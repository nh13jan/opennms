/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2012 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.dao.api;

import org.springframework.dao.DataAccessException;

/**
 * <p>EventdServiceManager interface.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public interface EventdServiceManager {
    /**
     * Lookup the service ID for a specific service by name.
     *
     * @return service ID for the given service name or -1 if not found
     * @exception DataAccessException if there is an error accessing the database
     * @param serviceName a {@link java.lang.String} object.
     * @throws org.springframework.dao.DataAccessException if any.
     */
    public abstract int getServiceId(String serviceName) throws DataAccessException;

    /**
     * Synchronize the in-memory cache with the service table in the database.
     */
    public abstract void dataSourceSync();
}
