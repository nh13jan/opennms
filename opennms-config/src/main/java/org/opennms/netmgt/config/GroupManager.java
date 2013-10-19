/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.OwnedInterval;
import org.opennms.core.utils.OwnedIntervalSequence;
import org.opennms.core.utils.Owner;
import org.opennms.core.xml.CastorUtils;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.config.groups.Group;
import org.opennms.netmgt.config.groups.Groupinfo;
import org.opennms.netmgt.config.groups.Groups;
import org.opennms.netmgt.config.groups.Header;
import org.opennms.netmgt.config.groups.Role;
import org.opennms.netmgt.config.groups.Roles;
import org.opennms.netmgt.config.groups.Schedule;
import org.opennms.netmgt.config.users.DutySchedule;
import org.opennms.netmgt.model.OnmsGroup;
import org.opennms.netmgt.model.OnmsGroupList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;


/**
 * <p>Abstract GroupManager class.</p>
 *
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 * @author <a href="mailto:brozow@opennms.org">Matt Brozowski</a>
 * @author <a href="mailto:ayres@net.orst.edu">Bill Ayres</a>
 * @author <a href="mailto:dj@gregor.com">DJ Gregor</a>
 */
public abstract class GroupManager {

    public static class OnmsGroupMapper {

        public Group map(final OnmsGroup inputGroup) {
            if (inputGroup == null) return null;
            final Group castorGroup = new Group();
            castorGroup.setName(inputGroup.getName());
            castorGroup.setComments(inputGroup.getComments());
            castorGroup.setUser(inputGroup.getUsers().toArray(EMPTY_STRING_ARRAY));
            return castorGroup;
        }

        public OnmsGroup map(final Group inputGroup) {
            if (inputGroup == null) return null;
            final OnmsGroup xmlGroup = new OnmsGroup(inputGroup.getName());
            xmlGroup.setComments(inputGroup.getComments());
            xmlGroup.setUsers(inputGroup.getUserCollection());
            return xmlGroup;
        }

        public Collection<OnmsGroup> map(final Collection<Group> inputGroups) {
            final Collection<OnmsGroup> xmlGroups = new ArrayList<OnmsGroup>();
            for (final Group eachGroup : inputGroups) {
                if (eachGroup == null) continue;
                xmlGroups.add(map(eachGroup));
            }
            return xmlGroups;
        }
    }

    public static class OnmsGroupListMapper {
        public OnmsGroupList map(Collection<OnmsGroup> groups) {
            final OnmsGroupList list = new OnmsGroupList();
            list.addAll(groups);
            list.setTotalCount(list.getCount());
            return list;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(GroupManager.class);
    private final ReadWriteUpdateLock m_lock = new ReentrantReadWriteUpdateLock();

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * The duty schedules for each group
     */
    protected static HashMap<String, List<DutySchedule>> m_dutySchedules;

    /**
     * A mapping of Group object by name
     */
    private Map<String, Group> m_groups;
    private Map<String, Role> m_roles;
    private Header m_oldHeader;

    /**
     * <p>parseXml</p>
     *
     * @param stream a {@link java.io.InputStream} object.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    protected void parseXml(final InputStream stream) throws MarshalException, ValidationException {
        m_lock.writeLock().lock();
        try {
            final Groupinfo groupinfo = CastorUtils.unmarshal(Groupinfo.class, stream);
            initializeGroupsAndRoles(groupinfo);
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    private void initializeGroupsAndRoles(final Groupinfo groupinfo) {
        final Groups groups = groupinfo.getGroups();
        m_groups = new LinkedHashMap<String, Group>();
        m_oldHeader = groupinfo.getHeader();
        for (final Group curGroup : groups.getGroupCollection()) {
            m_groups.put(curGroup.getName(), curGroup);
        }
        buildDutySchedules(m_groups);

        final Roles roles = groupinfo.getRoles();
        m_roles = new LinkedHashMap<String, Role>();
        if (roles != null) {
            for (final Role role : roles.getRoleCollection()) {
                m_roles.put(role.getName(), role);
            }
        }
    }

    /**
     * Set the groups data
     *
     * @param grp a {@link java.util.Map} object.
     */
    public void setGroups(final Map<String, Group> grp) {
        m_lock.writeLock().lock();
        try {
            m_groups = grp;
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /**
     * Get the groups
     *
     * @return a {@link java.util.Map} object.
     * @throws java.io.IOException if any.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    public Map<String, Group> getGroups() throws IOException, MarshalException, ValidationException {
        update();

        m_lock.readLock().lock();
        try {
            return Collections.unmodifiableMap(m_groups);
        } finally {
            m_lock.readLock().unlock();
        }
    }

    public OnmsGroupList getOnmsGroupList() throws MarshalException, ValidationException, IOException {
        final Map<String, Group> groups = getGroups();

        m_lock.readLock().lock();
        try {
            return new OnmsGroupListMapper().map(new OnmsGroupMapper().map(groups.values()));
        } finally {
            m_lock.readLock().unlock();
        }
    }

    public OnmsGroup getOnmsGroup(final String groupName) throws MarshalException, ValidationException, IOException {
        final Group castorGroup = getGroup(groupName);

        m_lock.updateLock().lock();
        try {
            if (castorGroup == null) return null;
            return new OnmsGroupMapper().map(castorGroup);
        } finally {
            m_lock.updateLock().unlock();
        }
    }

    public void save(final OnmsGroup group) throws Exception {
        Group castorGroup = getGroup(group.getName());

        m_lock.writeLock().lock();
        try {
            if (castorGroup == null) {
                castorGroup = new Group();
                castorGroup.setName(group.getName());
            }
            castorGroup.setComments(group.getComments());
            castorGroup.setUser(group.getUsers().toArray(EMPTY_STRING_ARRAY));

            saveGroup(group.getName(), castorGroup);
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /**
     * <p>update</p>
     *
     * @throws org.exolab.castor.xml.ValidationException if any.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws java.io.IOException if any.
     */
    public abstract void update() throws IOException, MarshalException, ValidationException;

    /**
     * Returns a boolean indicating if the group name appears in the xml file
     *
     * @return true if the group exists in the xml file, false otherwise
     * @param groupName a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    public boolean hasGroup(final String groupName) throws IOException, MarshalException, ValidationException {
        update();

        m_lock.readLock().lock();
        try {
            return m_groups.containsKey(groupName);
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>getGroupNames</p>
     *
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    public List<String> getGroupNames() throws IOException, MarshalException, ValidationException {
        update();

        m_lock.readLock().lock();
        try {
            return new ArrayList<String>(m_groups.keySet());
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * Get a group using its name
     *
     * @param name
     *            the name of the group to return
     * @return Group, the group specified by name
     * @throws java.io.IOException if any.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    public Group getGroup(final String name) throws IOException, MarshalException, ValidationException {
        update();

        m_lock.readLock().lock();
        try {
            return m_groups.get(name);
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>saveGroups</p>
     *
     * @throws java.lang.Exception if any.
     */
    public void saveGroups() throws Exception {
        m_lock.writeLock().lock();

        StringWriter writer = null;

        try {
            final Header header = m_oldHeader;
            if (header != null) header.setCreated(EventConstants.formatToString(new Date()));

            final Groups groups = new Groups();
            for (final Group grp : m_groups.values()) {
                groups.addGroup(grp);
            }

            final Roles roles = new Roles();
            for (final Role role : m_roles.values()) {
                roles.addRole(role);
            }

            final Groupinfo groupinfo = new Groupinfo();
            groupinfo.setGroups(groups);
            if (roles.getRoleCount() > 0) groupinfo.setRoles(roles);
            groupinfo.setHeader(header);

            m_oldHeader = header;

            // marshal to a string first, then write the string to the file. This
            // way the original configuration
            // isn't lost if the XML from the marshal is hosed.
            writer = new StringWriter();
            Marshaller.marshal(groupinfo, writer);
            saveXml(writer.toString());
        } finally {
            IOUtils.closeQuietly(writer);
            m_lock.writeLock().unlock();
        }
    }

    /**
     * Builds a mapping between groups and duty schedules. These are used when
     * determining to send a notice to a given group. This helps speed up the decision process.
     * @param groups the map of groups parsed from the XML configuration file
     */
    private static void buildDutySchedules(final Map<String, Group> groups) {
        m_dutySchedules = new HashMap<String, List<DutySchedule>>();

        for (final Map.Entry<String,Group> entry : groups.entrySet()) {
            final String key = entry.getKey();
            final Group curGroup = entry.getValue();

            if (curGroup.getDutyScheduleCount() > 0) {
                final List<DutySchedule> dutyList = new ArrayList<DutySchedule>();
                for (final String duty : curGroup.getDutyScheduleCollection()) {
                    dutyList.add(new DutySchedule(duty));
                }
                m_dutySchedules.put(key, dutyList);
            }
        }
    }

    /**
     * Determines if a group is on duty at a given time. If a group has no duty schedules
     * listed in the configuration file, that group is assumed to always be on duty.
     *
     * @param group the group whose duty schedule we want
     * @param time the time to check for a duty schedule
     * @return boolean, true if the group is on duty, false otherwise.
     * @throws java.io.IOException if any.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    public boolean isGroupOnDuty(final String group, final Calendar time) throws IOException, MarshalException, ValidationException {
        update();

        m_lock.readLock().lock();

        try {
            //if the group has no duty schedules then it is on duty
            if (!m_dutySchedules.containsKey(group)) {
                return true;
            }
            for (final DutySchedule curSchedule : m_dutySchedules.get(group)) {
                if (curSchedule.isInSchedule(time)) {
                    return true;
                }
            }
            return false;
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * Determines when a group is next on duty. If a group has no duty schedules
     * listed in the configuration file, that group is assumed to always be on duty.
     *
     * @param group the group whose duty schedule we want
     * @param time the time to check for a duty schedule
     * @return long, the time in milliseconds until the group is next on duty
     * @throws java.io.IOException if any.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    public long groupNextOnDuty(final String group, final Calendar time) throws IOException, MarshalException, ValidationException {
        update();

        m_lock.readLock().lock();
        try {
            long next = -1;
            //if the group has no duty schedules then it is on duty
            if (!m_dutySchedules.containsKey(group)) {
                return 0;
            }

            final List<DutySchedule> dutySchedules = m_dutySchedules.get(group);
            for (int i = 0; i < dutySchedules.size(); i++) {
                final DutySchedule curSchedule = dutySchedules.get(i);
                final long tempnext =  curSchedule.nextInSchedule(time);
                if( tempnext < next || next == -1 ) {
                    LOG.debug("isGroupOnDuty: On duty in {} millisec from schedule {}", i, tempnext);
                    next = tempnext;
                }
            }
            return next;
        } finally {
            m_lock.readLock().unlock();
        }
    } 

    /**
     * <p>saveXml</p>
     *
     * @param data a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    protected abstract void saveXml(String data) throws IOException;

    /**
     * Adds a new user and overwrites the "groups.xml"
     *
     * @param name a {@link java.lang.String} object.
     * @param details a {@link org.opennms.netmgt.config.groups.Group} object.
     * @throws java.lang.Exception if any.
     */
    public void saveGroup(final String name, final Group details) throws Exception {
        m_lock.writeLock().lock();

        try {
            if (name == null || details == null) {
                throw new Exception("GroupFactory:saveGroup  null");
            } else {
                m_groups.put(name, details);
            }
            saveGroups();
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /**
     * <p>saveRole</p>
     *
     * @param role a {@link org.opennms.netmgt.config.groups.Role} object.
     * @throws java.lang.Exception if any.
     */
    public void saveRole(final Role role) throws Exception {
        m_lock.writeLock().lock();
        try {
            m_roles.put(role.getName(), role);
            saveGroups();
        } finally {
            m_lock.writeLock().unlock();
        }
    }


    /**
     * Removes the user from the list of groups. Then overwrites to the
     * "groups.xml"
     *
     * @param name a {@link java.lang.String} object.
     * @throws java.lang.Exception if any.
     */
    public void deleteUser(final String name) throws Exception {
        m_lock.updateLock().lock();

        try {
            // Check if the user exists
            if (name != null && !name.equals("")) {
                m_lock.writeLock().lock();

                try {
                    // Remove the user in the group.
                    for (final Group group : m_groups.values()) {
                        group.removeUser(name);
                    }

                    for (final Role role : m_roles.values()) {
                        final Iterator<Schedule> s = role.getScheduleCollection().iterator();
                        while(s.hasNext()) {
                            final Schedule sched = s.next();
                            if (name.equals(sched.getName())) {
                                s.remove();
                            }
                        }
                    }

                    // Saves into "groups.xml" file
                    saveGroups();
                } finally {
                    m_lock.writeLock().unlock();
                }
            } else {
                throw new Exception("GroupFactory:delete Invalid user name:" + name);
            }
        } finally {
            m_lock.updateLock().unlock();
        }
    }

    /**
     * Removes the group from the list of groups. Then overwrites to the
     * "groups.xml"
     *
     * @param name a {@link java.lang.String} object.
     * @throws java.lang.Exception if any.
     */
    public void deleteGroup(final String name) throws Exception {
        m_lock.updateLock().lock();
        try {
            // Check if the group exists
            if (name != null && !name.equals("")) {
                if (m_groups.containsKey(name)) {
                    m_lock.writeLock().lock();
                    try {
                        // Remove the group.
                        m_groups.remove(name);
                        // Saves into "groups.xml" file
                        saveGroups();
                        return;
                    } finally {
                        m_lock.writeLock().unlock();
                    }
                }
            }

            // if we didn't match it, throw an error
            throw new Exception("Attempted to delete group " + name + " which does not exist!");
        } finally {
            m_lock.updateLock().unlock();
        }
    }

    /**
     * <p>deleteRole</p>
     *
     * @param name a {@link java.lang.String} object.
     * @throws java.lang.Exception if any.
     */
    public void deleteRole(final String name) throws Exception {
        m_lock.updateLock().lock();

        try {
            if (name != null && !name.equals("")) {
                if (m_roles.containsKey(name)) {
                    m_lock.writeLock().lock();
                    try {
                        m_roles.remove(name);
                        saveGroups();
                    } finally {
                        m_lock.writeLock().unlock();
                    }
                }
            }

            throw new Exception("Attempted to delete role " + name + " which does not exist!");
        } finally {
            m_lock.updateLock().unlock();
        }
    }


    /**
     * Renames the group from the list of groups. Then overwrites to the
     * "groups.xml"
     *
     * @param oldName a {@link java.lang.String} object.
     * @param newName a {@link java.lang.String} object.
     * @throws java.lang.Exception if any.
     */
    public void renameGroup(final String oldName, final String newName) throws Exception {
        m_lock.updateLock().lock();

        try {
            if (oldName != null && !oldName.equals("")) {
                if (m_groups.containsKey(oldName)) {
                    m_lock.writeLock().lock();
                    try {
                        final Group grp = m_groups.remove(oldName);
                        grp.setName(newName);
                        m_groups.put(newName, grp);

                        // Save into groups.xml and return
                        saveGroups();
                        return;
                    } finally {
                        m_lock.writeLock().unlock();
                    }
                }
            }

            // otherwise, if it wasn't renamed, throw an error
            throw new Exception("Attempted to rename group " + oldName + " to " + newName + ", but one or both don't exist!");
        } finally {
            m_lock.updateLock().unlock();
        }
    }

    /**
     * When this method is called group name is changed, so also is the
     * group name belonging to the view. Also overwrites the "groups.xml" file
     *
     * @param oldName a {@link java.lang.String} object.
     * @param newName a {@link java.lang.String} object.
     * @throws java.lang.Exception if any.
     */
    public void renameUser(final String oldName, final String newName) throws Exception {
        m_lock.updateLock().lock();

        try {
            // Get the old data
            if (oldName == null || newName == null || oldName == "" || newName == "") {
                throw new Exception("Group Factory: Rename user.. no value ");
            } else {
                final Map<String, Group> map = new LinkedHashMap<String, Group>();

                m_lock.writeLock().lock();
                try {
                    for (final Group group : m_groups.values()) {   
                        final ListIterator<String> userList = group.getUserCollection().listIterator();
                        while (userList.hasNext()) {
                            final String name = userList.next();
                            if (name.equals(oldName)) {
                                userList.set(newName); 
                            }
                        }
                        map.put(group.getName(), group);
                    }
    
                    m_groups.clear();
                    m_groups.putAll(map);
    
                    for (final Role role : m_roles.values()) {
                        for (final Schedule sched : role.getScheduleCollection()) {
                            if (oldName.equals(sched.getName())) {
                                sched.setName(newName);
                            }
                        }
                    }
    
                    saveGroups();
                } finally {
                    m_lock.writeLock().unlock();
                }
            }
        } finally {
            m_lock.updateLock().unlock();
        }
    }

    /**
     * <p>getRoleNames</p>
     *
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getRoleNames() {
        m_lock.readLock().lock();
        try {
            final Set<String> keys = m_roles.keySet();
            return keys.toArray(new String[keys.size()]);
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>getRoles</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    public Collection<Role> getRoles() {
        m_lock.readLock().lock();
        try {
            return m_roles.values();
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>getRole</p>
     *
     * @param roleName a {@link java.lang.String} object.
     * @return a {@link org.opennms.netmgt.config.groups.Role} object.
     */
    public Role getRole(final String roleName) {
        m_lock.readLock().lock();
        try {
            return (Role)m_roles.get(roleName);
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>userHasRole</p>
     *
     * @param userId a {@link java.lang.String} object.
     * @param roleid a {@link java.lang.String} object.
     * @return a boolean.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     * @throws java.io.IOException if any.
     */
    public boolean userHasRole(final String userId, final String roleid) throws MarshalException, ValidationException, IOException {
        update();
        final Role role = getRole(roleid);

        m_lock.readLock().lock();
        try {
            for (final Schedule sched : role.getScheduleCollection()) {
                if (userId.equals(sched.getName())) {
                    return true;
                }
            }
            return false;
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>getSchedulesForRoleAt</p>
     *
     * @param roleId a {@link java.lang.String} object.
     * @param time a {@link java.util.Date} object.
     * @return a {@link java.util.List} object.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     * @throws java.io.IOException if any.
     */
    public List<Schedule> getSchedulesForRoleAt(final String roleId, final Date time) throws MarshalException, ValidationException, IOException {
        update();
        final Role role = getRole(roleId);

        m_lock.readLock().lock();
        try {
            final List<Schedule> schedules = new ArrayList<Schedule>();
            for (final Schedule sched : role.getScheduleCollection()) {
                if (BasicScheduleUtils.isTimeInSchedule(time, BasicScheduleUtils.getGroupSchedule(sched))) {
                    schedules.add(sched);
                }
            }
            return schedules;
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>getUserSchedulesForRole</p>
     *
     * @param userId a {@link java.lang.String} object.
     * @param roleId a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     * @throws java.io.IOException if any.
     */
    public List<Schedule> getUserSchedulesForRole(final String userId, final String roleId) throws MarshalException, ValidationException, IOException {
        update();
        final Role role = getRole(roleId);

        m_lock.readLock().lock();
        try {
            final List<Schedule> scheds = new ArrayList<Schedule>();
            for (final Schedule sched : role.getScheduleCollection()) {
                if (userId.equals(sched.getName())) {
                    scheds.add(sched);
                }
            }
            return scheds;
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>isUserScheduledForRole</p>
     *
     * @param userId a {@link java.lang.String} object.
     * @param roleId a {@link java.lang.String} object.
     * @param time a {@link java.util.Date} object.
     * @return a boolean.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     * @throws java.io.IOException if any.
     */
    public boolean isUserScheduledForRole(final String userId, final String roleId, final Date time) throws MarshalException, ValidationException, IOException {
        update();
        final Role role = getRole(roleId);
        final List<Schedule> userSchedules = getUserSchedulesForRole(userId, roleId);

        m_lock.readLock().lock();
        try {
            for (final Schedule sched : userSchedules) {
                if (BasicScheduleUtils.isTimeInSchedule(time, BasicScheduleUtils.getGroupSchedule(sched))) {
                    return true;
                }
            }
    
            // if no user is scheduled then the supervisor is schedule by default 
            if (userId.equals(role.getSupervisor())) {
                for (final Schedule sched : role.getScheduleCollection()) {
                    if (BasicScheduleUtils.isTimeInSchedule(time, BasicScheduleUtils.getGroupSchedule(sched))) {
                        // we found another scheduled user
                        return false;
                    }
                }
                return true;
            }
            return false;
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>getRoleScheduleEntries</p>
     *
     * @param roleid a {@link java.lang.String} object.
     * @param start a {@link java.util.Date} object.
     * @param end a {@link java.util.Date} object.
     * @return a {@link org.opennms.core.utils.OwnedIntervalSequence} object.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     * @throws java.io.IOException if any.
     */
    public OwnedIntervalSequence getRoleScheduleEntries(final String roleid, final Date start, final Date end) throws MarshalException, ValidationException, IOException {
        update();
        final Role role = getRole(roleid);

        m_lock.updateLock().lock();
        try {
            final OwnedIntervalSequence schedEntries = new OwnedIntervalSequence();
            for (int i = 0; i < role.getScheduleCount(); i++) {
                final Schedule sched = (Schedule) role.getSchedule(i);
                final Owner owner = new Owner(roleid, sched.getName(), i);
                schedEntries.addAll(BasicScheduleUtils.getIntervalsCovering(start, end, BasicScheduleUtils.getGroupSchedule(sched), owner));
            }
    
            final OwnedIntervalSequence defaultEntries = new OwnedIntervalSequence(new OwnedInterval(start, end));
            defaultEntries.removeAll(schedEntries);
            final Owner supervisor = new Owner(roleid, role.getSupervisor());
            for (final OwnedInterval interval : defaultEntries) {
                interval.addOwner(supervisor);
            }
            schedEntries.addAll(defaultEntries);
            return schedEntries;
        } finally {
            m_lock.updateLock().unlock();
        }
    }

    /**
     * <p>findGroupsForUser</p>
     *
     * @param user a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<Group> findGroupsForUser(final String user) {
        final List<Group> groups = new ArrayList<Group>();

        m_lock.readLock().lock();
        try {
            for (final Group group : m_groups.values()) {
                if (group.getUserCollection().contains(user)) {
                    groups.add(group);
                }
            }

            return groups;
        } finally {
            m_lock.readLock().unlock();
        }
    }

}
