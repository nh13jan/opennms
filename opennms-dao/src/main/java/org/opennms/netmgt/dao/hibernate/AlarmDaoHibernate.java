/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2012 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.dao.hibernate;

import java.util.Collections;
import java.util.List;

import org.opennms.netmgt.dao.api.AlarmDao;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.alarm.AlarmSummary;

/**
 * <p>AlarmDaoHibernate class.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public class AlarmDaoHibernate extends AbstractDaoHibernate<OnmsAlarm, Integer> implements AlarmDao {

	public AlarmDaoHibernate() {
		super(OnmsAlarm.class);
	}

    /** {@inheritDoc} */
    @Override
    public OnmsAlarm findByReductionKey(String reductionKey) {
        String hql = "from OnmsAlarm as alarms where alarms.reductionKey = ?";
        return super.findUnique(hql, reductionKey);
    }

    /** {@inheritDoc} */
    @Override
    public List<AlarmSummary> getNodeAlarmSummariesIncludeAcknowledgedOnes(List<Integer> nodeIds) {
        if (nodeIds.size() < 1) {
            return Collections.emptyList();
        }
        StringBuilder sql = new StringBuilder();
        //count(*) - count(alarm.alarmAckTime) counts only the unacknowledged alarms
        sql.append("SELECT DISTINCT new org.opennms.netmgt.model.alarm.AlarmSummary( node.id, node.label, min(alarm.lastEventTime), max(alarm.severity), (count(*) - count(alarm.alarmAckTime)) ) ");
        sql.append("FROM OnmsAlarm AS alarm ");
        sql.append ("LEFT JOIN alarm.node AS node ");
        sql.append("WHERE node.id IS NOT NULL AND alarm.severity > 3 ");

        // optional
        if (nodeIds.size() == 1) {
            sql.append("AND node.id = " + nodeIds.get(0) + " ");
        } else {
            sql.append("AND node.id in (");
            for (int i=0; i<nodeIds.size(); i++) {
                sql.append(nodeIds.get(i));
                if (i < nodeIds.size() -1) {
                    sql.append(",");
                }
            }
            sql.append(") ");
        }
        sql.append("GROUP BY node.id, node.label ");
        return findObjects(AlarmSummary.class, sql.toString());
    }

    /** {@inheritDoc} */
    @Override
    public List<AlarmSummary> getNodeAlarmSummaries() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT new org.opennms.netmgt.model.alarm.AlarmSummary(node.id, node.label, min(alarm.lastEventTime), max(alarm.severity), count(*)) ");
        sql.append("FROM OnmsAlarm AS alarm ");
        sql.append ("LEFT JOIN alarm.node AS node ");
        sql.append("WHERE node.id IS NOT NULL AND alarm.severity > 3 AND alarm.alarmAckTime IS NULL ");
        sql.append("GROUP BY node.id, node.label ");
        sql.append("ORDER BY min(alarm.lastEventTime) DESC, node.label ASC");
        return findObjects(AlarmSummary.class, sql.toString());
    }
}
