/*
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2009 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a derivative work, containing both original code, included code and modified
 * code that was published under the GNU General Public License. Copyrights for modified
 * and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Modifications:
 *
 * Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 *      OpenNMS Licensing       <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 *
 */

package org.opennms.sms.reflector.smsservice;

import org.smslib.USSDResponse;
import org.smslib.USSDSessionStatus;

/**
 * @author brozow
 *
 */
public class UssdResponse extends MobileMsgResponse {

    private String m_gatewayId;
    private USSDResponse m_msg;
    
    public UssdResponse(String gatewayId, USSDResponse msg, long receiveTime) {
    	super(receiveTime);
        m_gatewayId = gatewayId;
        m_msg = msg;
    }

    public String getGatewayId() {
        return m_gatewayId;
    }

    /**
     * @return the text
     */
    public String getText() {
        return m_msg.getContent();
    }
    
    
    /**
     * 
     */
    public USSDSessionStatus getSessionStatus() {
        return m_msg.getSessionStatus();
    }

    /**
     * @return
     */
    public USSDResponse getMessage() {
        return m_msg;
        
    }
    
    @Override
    public String toString() {
        return "" + m_msg;
    }



}
