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

import java.beans.PropertyVetoException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.db.BaseConnectionFactory;
import org.opennms.netmgt.config.opennmsDataSources.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import snaq.db.DBPoolDataSource;

/**
 * <p>C3P0ConnectionFactory class.</p>
 */
public class DBPoolConnectionFactory extends BaseConnectionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DBPoolConnectionFactory.class);

	private DBPoolDataSource m_dataSource;

    public DBPoolConnectionFactory(final JdbcDataSource ds) throws MarshalException, ValidationException, PropertyVetoException, SQLException {
    	super(ds);
    }

    @Override
    protected void initializePool(final JdbcDataSource dataSource) throws SQLException {
    	m_dataSource = new DBPoolDataSource();
    	m_dataSource.setName(dataSource.getName());
    	m_dataSource.setDriverClassName(dataSource.getClassName());
    	m_dataSource.setUrl(dataSource.getUrl());
    	m_dataSource.setUser(dataSource.getUserName());
    	m_dataSource.setPassword(dataSource.getPassword());
    }

    @Override
    public Connection getConnection() throws SQLException {
    	return m_dataSource.getConnection();
    }

    @Override
    public String getUrl() {
    	return m_dataSource.getUrl();
    }

    @Override
    public void setUrl(final String url) {
    	validateJdbcUrl(url);
    	m_dataSource.setUrl(url);
    }

    @Override
    public String getUser() {
    	return m_dataSource.getUser();
    }

    @Override
    public void setUser(final String user) {
    	m_dataSource.setUser(user);
    }

    @Override
    public DataSource getDataSource() {
    	return m_dataSource;
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
    	return m_dataSource.getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
    	return m_dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(final PrintWriter out) throws SQLException {
        m_dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(final int seconds) {
        m_dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return m_dataSource.getLoginTimeout();
    }

    /** {@inheritDoc} */
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("getParentLogger not supported");
    }

    @Override
    public void close() {
    	super.close();
    	LOG.info("Closing DBPool pool.");
    	m_dataSource.release();
    }

    @Override
	public void setIdleTimeout(final int idleTimeout) {
		m_dataSource.setIdleTimeout(idleTimeout);
	}

    @Override
	public void setMinPool(final int minPool) {
		m_dataSource.setMinPool(minPool);
	}

    @Override
	public void setMaxPool(final int maxPool) {
		m_dataSource.setMaxPool(maxPool);
	}

    @Override
	public void setMaxSize(final int maxSize) {
		m_dataSource.setMaxSize(maxSize);
	}
}
