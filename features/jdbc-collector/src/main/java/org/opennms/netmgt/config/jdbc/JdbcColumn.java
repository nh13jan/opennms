/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2012 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.config.jdbc;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang.builder.CompareToBuilder;

@XmlRootElement(name="column")
public class JdbcColumn implements Serializable, Comparable<JdbcColumn> {
    private static final long serialVersionUID = -2982266671178613278L;

    @XmlAttribute(name="name", required=true)
    private String m_columnName;
    
    @XmlAttribute(name="data-source-name", required=false)
    private String m_dataSourceName;
    
    @XmlAttribute(name="type", required=true)    
    private String m_dataType;
    
    @XmlAttribute(name="alias", required=true)
    private String m_alias;
    
    @XmlTransient
    public String getColumnName() {
        return m_columnName;
    }
    
    public void setColumnName(String columnName) {
        m_columnName = columnName;
    }
    
    @XmlTransient
    public String getDataSourceName() {
        return m_dataSourceName;
    }
    
    public void setDataSourceName(String dataSourceName) {
        m_dataSourceName = dataSourceName;
    }
    
    @XmlTransient
    public String getDataType() {
        return m_dataType;
    }
    
    public void setDataType(String dataType) {
        m_dataType = dataType;
    }
    
    
    @XmlTransient
    public String getAlias() {
        return m_alias;
    }

    public void setAlias(String alias) {
        m_alias = alias;
    }

    @Override
    public int compareTo(JdbcColumn obj) {
        return new CompareToBuilder()
            .append(getColumnName(), obj.getColumnName())
            .append(getDataSourceName(), obj.getDataSourceName())
            .append(getDataType(), obj.getDataType())
            .append(getAlias(), obj.getAlias())
            .toComparison();
    }

    @Override
    public int hashCode() {
        final int prime = 883;
        int result = 1;
        result = prime * result + ((m_alias == null) ? 0 : m_alias.hashCode());
        result = prime * result + ((m_columnName == null) ? 0 : m_columnName.hashCode());
        result = prime * result + ((m_dataSourceName == null) ? 0 : m_dataSourceName.hashCode());
        result = prime * result + ((m_dataType == null) ? 0 : m_dataType.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof JdbcColumn)) return false;
        final JdbcColumn other = (JdbcColumn) obj;
        if (m_alias == null) {
            if (other.m_alias != null) return false;
        } else if (!m_alias.equals(other.m_alias)) {
            return false;
        }
        if (m_columnName == null) {
            if (other.m_columnName != null) return false;
        } else if (!m_columnName.equals(other.m_columnName)) {
            return false;
        }
        if (m_dataSourceName == null) {
            if (other.m_dataSourceName != null) return false;
        } else if (!m_dataSourceName.equals(other.m_dataSourceName)) {
            return false;
        }
        if (m_dataType == null) {
            if (other.m_dataType != null) return false;
        } else if (!m_dataType.equals(other.m_dataType)) {
            return false;
        }
        return true;
    }
    
}
