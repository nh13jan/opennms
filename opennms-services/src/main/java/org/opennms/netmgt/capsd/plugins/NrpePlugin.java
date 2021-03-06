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

package org.opennms.netmgt.capsd.plugins;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.Map;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.ParameterMap;
import org.opennms.core.utils.SocketUtils;
import org.opennms.netmgt.capsd.AbstractPlugin;
import org.opennms.netmgt.poller.nrpe.CheckNrpe;
import org.opennms.netmgt.poller.nrpe.NrpePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <P>
 * This class is designed to be used by the capabilities daemon to test for the
 * existance of an TCP server on remote interfaces. The class implements the
 * Plugin interface that allows it to be used along with other plugins by the
 * daemon.
 * </P>
 *
 * @author <a href="mailto:mike@opennms.org">Mike</a>
 * @author <a href="mailto:weave@oculan.com">Weaver</a>
 * @author <a href="http://www.opennms.org">OpenNMS</a>
 */
public final class NrpePlugin extends AbstractPlugin {
    
    
    private static final Logger LOG = LoggerFactory.getLogger(NrpePlugin.class);

    /**
     * The protocol supported by the plugin
     */
    private final static String PROTOCOL_NAME = "NRPE";

    /**
     * Default number of retries for TCP requests
     */
    private final static int DEFAULT_RETRY = 0;

    /**
     * Default timeout (in milliseconds) for TCP requests
     */
    private final static int DEFAULT_TIMEOUT = 5000; // in milliseconds
    
    /**
     * Default whether to use SSL
     */
    private final static boolean DEFAULT_USE_SSL = true;
    
    /**
     * List of cipher suites to use when talking SSL to NRPE, which uses anonymous DH
     */
    private static final String[] ADH_CIPHER_SUITES = new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"};
    
    /**
     * Whether to use SSL for this instantiation
     */
    private boolean m_useSsl = DEFAULT_USE_SSL;

    /**
     * <P>
     * Test to see if the passed host-port pair is the endpoint for a TCP
     * server. If there is a TCP server at that destination then a value of true
     * is returned from the method. Otherwise a false value is returned to the
     * caller. In order to return true the remote host must generate a banner
     * line which contains the text from the bannerMatch argument.
     * </P>
     * 
     * @param host
     *            The remote host to connect to.
     * @param port
     *            The remote port on the host.
     * @param bannerResult
     *            Banner line generated by the remote host must contain this
     *            text.
     * 
     * @return True if a connection is established with the host and the banner
     *         line contains the bannerMatch text.
     */
    private boolean isServer(InetAddress host, int port, String command, int padding, int retries, int timeout, RE regex, StringBuffer bannerResult) {
        boolean isAServer = false;
        for (int attempts = 0; attempts <= retries && !isAServer; attempts++) {
            Socket socket = null;
            try {
                // create a connected socket
                //
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), timeout);
                socket = wrapSocket(socket, host.toString(), port);
                socket.setSoTimeout(timeout);
                LOG.debug("NrpePlugin: connected to host: {} on port: {}", host, port);
				
				NrpePacket p = new NrpePacket(NrpePacket.QUERY_PACKET, (short) 0,
						command);
				byte[] b = p.buildPacket(padding);
				OutputStream o = socket.getOutputStream();
				o.write(b);

				NrpePacket response = NrpePacket.receivePacket(socket.getInputStream(), padding);
				if (response.getResultCode() == 0) {
                    isAServer = true;
				} else if (response.getResultCode() <= 2) {
						String response_msg = response.getBuffer();
						RE r = new RE("OK|WARNING|CRITICAL");
						if (r.match(response_msg)) {
							isAServer = true;
						} else {
							LOG.info("received 1-2 return code, {}, with message: {}", response.getResultCode(), response.getBuffer());
							isAServer = false;
							break;
						}
				} else {
						LOG.info("received 3+ return code, {}, with message: {}", response.getResultCode(), response.getBuffer());
                        isAServer = false;
						break;
                }

				/*
                // If banner matching string is null or wildcard ("*") then we
                // only need to test connectivity and we've got that!
                //
                if (regex == null) {
                    isAServer = true;
                } else {
                    // get a line reader
                    //
                    BufferedReader lineRdr = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Read the server's banner line ouptput and validate it
                    // against
                    // the bannerMatch parameter to determine if this interface
                    // supports the
                    // service.
                    //
                    String response = lineRdr.readLine();
                    if (regex.match(response)) {
                        if (log.isDebugEnabled())
                            log.debug("isServer: matching response=" + response);
                        isAServer = true;
                        if (bannerResult != null)
                            bannerResult.append(response);
                    } else {
                        // Got a response but it didn't match...no need to
                        // attempt retries
                        isAServer = false;
                        if (log.isDebugEnabled())
                            log.debug("isServer: NON-matching response=" + response);
                        break;
                    }
                }
                */
            } catch (ConnectException e) {
                // Connection refused!! Continue to retry.
                //
                LOG.debug("NrpePlugin: Connection refused to {}:{}", InetAddressUtils.str(host), port);
                isAServer = false;
            } catch (NoRouteToHostException e) {
                // No Route to host!!!
                //
                e.fillInStackTrace();
                LOG.info("NrpePlugin: Could not connect to host {}, no route to host", InetAddressUtils.str(host), e);
                isAServer = false;
                throw new UndeclaredThrowableException(e);
            } catch (InterruptedIOException e) {
                // This is an expected exception
                //
                LOG.debug("NrpePlugin: did not connect to host within timeout: {} attempt: {}", timeout, attempts);
                isAServer = false;
            } catch (IOException e) {
                LOG.info("NrpePlugin: An expected I/O exception occured connecting to host {} on port {}", InetAddressUtils.str(host), port, e);
                isAServer = false;
            } catch (Throwable t) {
                isAServer = false;
                LOG.warn("NrpePlugin: An undeclared throwable exception was caught connecting to host {} on port {}", InetAddressUtils.str(host), port, t);
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                }
            }
        }

        //
        // return the success/failure of this
        // attempt to contact an ftp server.
        //
        return isAServer;
    }

    /**
     * Returns the name of the protocol that this plugin checks on the target
     * system for support.
     *
     * @return The protocol name for this plugin.
     */
    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * Returns true if the protocol defined by this plugin is supported. If the
     * protocol is not supported then a false value is returned to the caller.
     */
    @Override
    public boolean isProtocolSupported(InetAddress address) {
        throw new UnsupportedOperationException("Undirected TCP checking not supported");
    }

    /**
     * {@inheritDoc}
     *
     * Returns true if the protocol defined by this plugin is supported. If the
     * protocol is not supported then a false value is returned to the caller.
     * The qualifier map passed to the method is used by the plugin to return
     * additional information by key-name. These key-value pairs can be added to
     * service events if needed.
     */
    @Override
    public boolean isProtocolSupported(InetAddress address, Map<String, Object> qualifiers) {
        int retries = DEFAULT_RETRY;
        int timeout = DEFAULT_TIMEOUT;
        int port = -1;
		int padding = -1;

        String banner = null;
        String match = null;
		String command = null;

        if (qualifiers != null) {
	        command = ParameterMap.getKeyedString(qualifiers, "command", NrpePacket.HELLO_COMMAND);
	        port = ParameterMap.getKeyedInteger(qualifiers, "port", CheckNrpe.DEFAULT_PORT);
	        padding = ParameterMap.getKeyedInteger(qualifiers, "padding", NrpePacket.DEFAULT_PADDING);
            retries = ParameterMap.getKeyedInteger(qualifiers, "retry", DEFAULT_RETRY);
            timeout = ParameterMap.getKeyedInteger(qualifiers, "timeout", DEFAULT_TIMEOUT);
            banner = ParameterMap.getKeyedString(qualifiers, "banner", null);
            match = ParameterMap.getKeyedString(qualifiers, "match", null);
            m_useSsl = ParameterMap.getKeyedBoolean(qualifiers, "usessl", DEFAULT_USE_SSL);
        }

        try {
            StringBuffer bannerResult = null;
            RE regex = null;
            if (match == null && (banner == null || banner.equals("*"))) {
                regex = null;
            } else if (match != null) {
                regex = new RE(match);
                bannerResult = new StringBuffer();
            } else if (banner != null) {
                regex = new RE(banner);
                bannerResult = new StringBuffer();
            }

            boolean result = isServer(address, port, command, padding, retries, timeout, regex, bannerResult);
            if (result && qualifiers != null) {
                if (bannerResult != null && bannerResult.length() > 0)
                    qualifiers.put("banner", bannerResult.toString());
            }

            return result;
        } catch (RESyntaxException e) {
            throw new java.lang.reflect.UndeclaredThrowableException(e);
        }
    }
    
    /**
     * <p>wrapSocket</p>
     *
     * @param socket a {@link java.net.Socket} object.
     * @param hostAddress a {@link java.lang.String} object.
     * @param hostPort a int.
     * @return a {@link java.net.Socket} object.
     * @throws java.lang.Exception if any.
     */
    protected Socket wrapSocket(Socket socket, String hostAddress, int hostPort) throws Exception {
    	if (! m_useSsl) {
		LOG.debug("Parameter 'usessl' is unset or false, not using SSL");
    		return socket;
    	} else {
		LOG.debug("Parameter 'usessl' is true, using SSL");
    		return SocketUtils.wrapSocketInSslContext(socket, ADH_CIPHER_SUITES);
    	}
    }
}
