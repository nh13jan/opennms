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

package org.opennms.netmgt.eventd.adaptors.tcp;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.eventd.adaptors.EventHandler;
import org.opennms.netmgt.eventd.adaptors.EventHandlerMBeanProxy;
import org.opennms.netmgt.eventd.adaptors.EventReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;

/**
 * This class is the access point for the agents to hook into the event queue.
 * This fiber sets up an server socket that accepts incoming connections on the
 * configured port (port 5817 by default).
 *
 * When a connection is established a new thread is started to process the
 * socket connection. The event document is decoded and each of the events are
 * passed to the handlers. Based upon the action of the handlers an event receipt
 * is generated and sent to the remote client.
 *
 * @author <a href="mailto:weave@oculan.com">Brian Weaver </a>
 * @author <a href="http;//www.opennms.org">OpenNMS </a>
 */
public final class TcpEventReceiver implements EventReceiver, TcpEventReceiverMBean {
    private static final Logger LOG = LoggerFactory.getLogger(TcpEventReceiver.class);
    private final ReadWriteUpdateLock m_lock = new ReentrantReadWriteUpdateLock();

    /**
     * The value that defines unlimited events per connection.
     */
    public static final int UNLIMITED_EVENTS = -1;

    /**
     * The main server thread. This thread of execution is used to handle all
     * incoming connection requests.
     */
    private Thread m_worker;

    /**
     * The server socket
     */
    private TcpServer m_server;

    /**
     * The registered list of event handlers. Each incoming event will be
     * passed to all event handlers. The event handlers <em>MUST NOT</em>
     * modify the passed event.
     */
    private List<EventHandler> m_eventHandlers;

    /**
     * The fiber's status.
     */
    private volatile int m_status;

    /**
     * The TCP server to listen on
     */
    private int m_tcpPort;

    /**
     * The logging prefix
     */
    private String m_logPrefix;

    /**
     * The number of event records per connection.
     */
    private int m_recsPerConn;

    /**
     * The IP address that the TcpServer will listen on.  If null, bind to all
     * interfaces.
     */
    private String m_ipAddress;

    /**
     * Constructs a new TCP/IP event receiver on the default TCP/IP port. The
     * server socket allocation is delayed until the fiber is actually started.
     *
     * @throws java.net.UnknownHostException if any.
     */
    public TcpEventReceiver() throws UnknownHostException {
        this(TcpServer.TCP_PORT, TcpServer.DEFAULT_IP_ADDRESS);
    }

    /**
     * Constructs a new TCP/IP event receiver on the passed port. The server
     * socket allocation is delayed until the fiber is actually started.
     *
     * @param port
     *            The binding port for the TCP/IP server socket.
     * @param ipAddress TODO
     * @throws java.net.UnknownHostException if any.
     */
    public TcpEventReceiver(final int port, final String ipAddress) throws UnknownHostException {
        m_eventHandlers = new ArrayList<EventHandler>(3);
        m_status = START_PENDING;
        m_tcpPort = port;
        m_ipAddress = ipAddress;
        m_server = null;
        m_worker = null;
        m_logPrefix = null;
        m_recsPerConn = UNLIMITED_EVENTS;
    }

    /**
     * Allocates the server socket and starts up the server socket processor
     * thread. If an error occurs allocating the server socket or the Fiber is
     * in an erronous state then a
     * {@link java.lang.RuntimeException runtime exception}is thrown.
     *
     * @throws java.lang.reflect.UndeclaredThrowableException
     *             Thrown if an error occurs allocating the server socket.
     * @throws java.lang.RuntimeException
     *             Thrown if the fiber is in an erronous state or the underlying
     *             thread cannot be started.
     */
    @Override
    public void start() {
        m_lock.writeLock().lock();

        try {
            assertNotRunning();

            m_status = STARTING;
            try {
                final InetAddress address = "*".equals(m_ipAddress) ? null : InetAddressUtils.addr(m_ipAddress);
                m_server = new TcpServer(this, m_eventHandlers, m_tcpPort, address);
                if (m_logPrefix != null) {
                    m_server.setLogPrefix(m_logPrefix);
                }
                if (m_recsPerConn != UNLIMITED_EVENTS) {
                    m_server.setEventsPerConnection(m_recsPerConn);
                }
            } catch (final IOException e) {
                throw new UndeclaredThrowableException(e, "Error opening server socket: " + e);
            }
            m_worker = new Thread(m_server, "Event TCP Server[" + m_tcpPort + "]");

            try {
                m_worker.start();
            } catch (final RuntimeException e) {
                m_worker.interrupt();
                m_status = STOPPED;
                throw e;
            }

            m_status = RUNNING;
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /**
     * Stops the TCP/IP event receiver. This method will block until all the
     * children threads of this object are terminated and
     * {@link java.lang.Thread#join joined}.
     */
    @Override
    public void stop() {
        m_lock.updateLock().lock();

        try {
            if (m_status == STOPPED) {
                return;
            }

            m_lock.writeLock().lock();

            try {
                if (m_status == START_PENDING) {
                    m_status = STOPPED;
                    return;
                }

                m_status = STOP_PENDING;

                // Stop the main server thread then iterate over the connected threads
                try {
                    m_server.stop();
                } catch (InterruptedException e) {
                    LOG.warn("Thread Interrupted while attempting to join server socket thread", e);
                }
                m_server = null;
                m_worker = null;

                m_status = STOPPED;
            } finally {
                m_lock.writeLock().unlock();
            }
        } finally {
            m_lock.updateLock().unlock();
        }
    }

    /**
     * Returns the name of this Fiber.
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getName() {
        m_lock.readLock().lock();
        try {
            return "Event TCP Receiver[" + m_tcpPort + "]";
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * Returns the status of this Fiber.
     *
     * @return a int.
     */
    @Override
    public int getStatus() {
        m_lock.readLock().lock();
        try {
            return m_status;
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>getStatusText</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getStatusText() {
        m_lock.readLock().lock();
        try {
            return STATUS_NAMES[getStatus()];
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>status</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String status() {
        return getStatusText();
    }

    /**
     * Called when the fiber is initialized
     */
    @Override
    public void init() {
        // do nothing
    }

    /**
     * Called when the fiber is destroyed
     */
    @Override
    public void destroy() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     *
     * Adds a new event handler to receiver. When new events are received the
     * decoded event is passed to the handler.
     */
    @Override
    public void addEventHandler(final EventHandler handler) {
        m_lock.writeLock().lock();
        try {
            if (!m_eventHandlers.contains(handler)) {
                m_eventHandlers.add(handler);
            }
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Removes an event handler from the list of handler called when an event is
     * received. The handler is removed based upon the method
     * <code>equals()</code> inherieted from the <code>Object</code> class.
     */
    @Override
    public void removeEventHandler(final EventHandler handler) {
        m_lock.writeLock().lock();
        try {
            m_eventHandlers.remove(handler);
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /**
     * <p>getEventHandlers</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<EventHandler> getEventHandlers() {
        m_lock.readLock().lock();
        try {
            return m_eventHandlers;
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>setEventHandlers</p>
     *
     * @param eventHandlers a {@link java.util.List} object.
     */
    public void setEventHandlers(final List<EventHandler> eventHandlers) {
        m_lock.writeLock().lock();
        try {
            m_eventHandlers = eventHandlers;
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /**
     * <p>getIpAddress</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getIpAddress() {
        m_lock.readLock().lock();
        try {
            return m_ipAddress;
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /**
     * <p>setIpAddress</p>
     *
     * @param ipAddress a {@link java.lang.String} object.
     */
    public void setIpAddress(final String ipAddress) {
        m_lock.writeLock().lock();
        try {
            assertNotRunning();
            m_ipAddress = ipAddress;
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /**
     * <p>getPort</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    @Override
    public Integer getPort() {
        m_lock.readLock().lock();
        try {
            return m_tcpPort;
        } finally {
            m_lock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setPort(final Integer port) {
        m_lock.writeLock().lock();
        try {
            assertNotRunning();
            m_tcpPort = port;
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addEventHandler(final String name) throws MalformedObjectNameException, InstanceNotFoundException {
        m_lock.writeLock().lock();
        try {
            addEventHandler(new EventHandlerMBeanProxy(new ObjectName(name)));
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeEventHandler(final String name) throws MalformedObjectNameException, InstanceNotFoundException {
        m_lock.writeLock().lock();
        try {
            removeEventHandler(new EventHandlerMBeanProxy(new ObjectName(name)));
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setLogPrefix(final String prefix) {
        m_lock.writeLock().lock();
        try {
            m_logPrefix = prefix;
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * The number of event records a new connection is allowed to send before
     * the connection is terminated by the server. The connection is always
     * terminated after an event receipt is generated, if one is required.
     */
    @Override
    public void setEventsPerConnection(final Integer number) {
        m_lock.writeLock().lock();
        try {
            assertNotRunning();
            m_recsPerConn = number.intValue();
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    private void assertNotRunning() {
        Assert.state(m_status == START_PENDING || m_status == STOPPED, "The fiber is already running and cannot be modified or started");
    }
}
