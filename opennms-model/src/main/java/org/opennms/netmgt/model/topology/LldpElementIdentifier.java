package org.opennms.netmgt.model.topology;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

public final class LldpElementIdentifier extends ElementIdentifier {

	public static class LldpChassisIdSubType extends AbstractType 
	implements Serializable{
	
	    /**
		 * 
		 */
		private static final long serialVersionUID = 3997412402508473063L;
		/*
	     * LldpChassisIdSubtype ::= TEXTUAL-CONVENTION
	    STATUS      current
	    DESCRIPTION
	            "This TC describes the source of a chassis identifier.

	            The enumeration 'chassisComponent(1)' represents a chassis
	            identifier based on the value of entPhysicalAlias object
	            (defined in IETF RFC 2737) for a chassis component (i.e.,
	            an entPhysicalClass value of 'chassis(3)').

	            The enumeration 'interfaceAlias(2)' represents a chassis
	            identifier based on the value of ifAlias object (defined in
	            IETF RFC 2863) for an interface on the containing chassis.

	            The enumeration 'portComponent(3)' represents a chassis
	            identifier based on the value of entPhysicalAlias object
	            (defined in IETF RFC 2737) for a port or backplane
	            component (i.e., entPhysicalClass value of 'port(10)' or
	            'backplane(4)'), within the containing chassis.

	            The enumeration 'macAddress(4)' represents a chassis
	            identifier based on the value of a unicast source address
	            (encoded in network byte order and IEEE 802.3 canonical bit
	            order), of a port on the containing chassis as defined in
	            IEEE Std 802-2001.

	            The enumeration 'networkAddress(5)' represents a chassis
	            identifier based on a network address, associated with
	            a particular chassis.  The encoded address is actually
	            composed of two fields.  The first field is a single octet,
	            representing the IANA AddressFamilyNumbers value for the
	            specific address type, and the second field is the network
	            address value.

	            The enumeration 'interfaceName(6)' represents a chassis
	            identifier based on the value of ifName object (defined in
	            IETF RFC 2863) for an interface on the containing chassis.

	            The enumeration 'local(7)' represents a chassis identifier
	            based on a locally defined value."
	    SYNTAX  INTEGER {
	            chassisComponent(1),
	            interfaceAlias(2),
	            portComponent(3),
	            macAddress(4),
	            networkAddress(5),
	            interfaceName(6),
	            local(7)
	    }

	     */
	    public static final int LLDP_CHASSISID_SUBTYPE_CHASSISCOMPONENT = 1;
	    public static final int LLDP_CHASSISID_SUBTYPE_INTERFACEALIAS = 2;
	    public static final int LLDP_CHASSISID_SUBTYPE_PORTCOMPONENT = 3;
	    public static final int LLDP_CHASSISID_SUBTYPE_MACADDRESS = 4;
	    public static final int LLDP_CHASSISID_SUBTYPE_NETWORKADDRESS = 5;
	    public static final int LLDP_CHASSISID_SUBTYPE_INTERFACENAME = 6;
	    public static final int LLDP_CHASSISID_SUBTYPE_LOCAL = 7;

	    public static LldpChassisIdSubType CHASSISCOMPONENT = new LldpChassisIdSubType(LLDP_CHASSISID_SUBTYPE_CHASSISCOMPONENT);
	    public static LldpChassisIdSubType INTERFACEALIAS = new LldpChassisIdSubType(LLDP_CHASSISID_SUBTYPE_INTERFACEALIAS);
	    public static LldpChassisIdSubType PORTCOMPONENT = new LldpChassisIdSubType(LLDP_CHASSISID_SUBTYPE_PORTCOMPONENT);
	    public static LldpChassisIdSubType MACADDRESS = new LldpChassisIdSubType(LLDP_CHASSISID_SUBTYPE_MACADDRESS);
	    public static LldpChassisIdSubType NETWORKADDRESS = new LldpChassisIdSubType(LLDP_CHASSISID_SUBTYPE_NETWORKADDRESS);
	    public static LldpChassisIdSubType INTERFACENAME = new LldpChassisIdSubType(LLDP_CHASSISID_SUBTYPE_INTERFACENAME);
	    public static LldpChassisIdSubType LOCAL = new LldpChassisIdSubType(LLDP_CHASSISID_SUBTYPE_LOCAL);

	    
	    public LldpChassisIdSubType(Integer chassisIdsubtype) {
	    	super(chassisIdsubtype);
	    }

	    protected static final Map<Integer, String> s_typeMap = new HashMap<Integer, String>();

        static {
        	s_typeMap.put(1, "chassisComponent" );
        	s_typeMap.put(2, "interfaceAlias" );
        	s_typeMap.put(3, "portComponent" );
        	s_typeMap.put(4, "macAddress" );
        	s_typeMap.put(5, "networkAddress" );
        	s_typeMap.put(6, "interfaceName" );
        	s_typeMap.put(7, "local" );
        }

        /**
         * <p>ElementIdentifierTypeString</p>
         *
         * @return a {@link java.lang.String} object.
         */
        /**
         */
        public static String getTypeString(Integer code) {
            if (s_typeMap.containsKey(code))
                    return s_typeMap.get( code);
            return null;
        }


        @Override
        public boolean equals(Object o) {
            if (o instanceof LldpChassisIdSubType) {
                return m_type.intValue() == ((LldpChassisIdSubType)o).m_type.intValue();
            }
            return false;
        }

        public static LldpChassisIdSubType get(Integer code) {
            if (code == null)
                throw new IllegalArgumentException("Cannot create LldpChassisIdSubType from null code");
            switch (code) {
            case LLDP_CHASSISID_SUBTYPE_CHASSISCOMPONENT: 	return CHASSISCOMPONENT;
            case LLDP_CHASSISID_SUBTYPE_INTERFACEALIAS: 	return INTERFACEALIAS;
            case LLDP_CHASSISID_SUBTYPE_PORTCOMPONENT: 		return PORTCOMPONENT;
            case LLDP_CHASSISID_SUBTYPE_MACADDRESS: 		return MACADDRESS;
            case LLDP_CHASSISID_SUBTYPE_NETWORKADDRESS: 	return NETWORKADDRESS;
            case LLDP_CHASSISID_SUBTYPE_INTERFACENAME: 		return INTERFACENAME;
            case LLDP_CHASSISID_SUBTYPE_LOCAL: 				return LOCAL;
            default:
                throw new IllegalArgumentException("Cannot create LldpChassisIdSubType from code "+code);
            }
        }		

	}

	private final String m_lldpSysname;
    
    private final String m_lldpChassisId;
    private final LldpChassisIdSubType m_lldpChassisIdSubType;

    public LldpElementIdentifier(String lldpChassisId, String sysname, Integer subtype) {
		super(ElementIdentifierType.LLDP);
		m_lldpChassisId=lldpChassisId;
		m_lldpSysname=sysname;
		m_lldpChassisIdSubType=LldpChassisIdSubType.get(subtype);
	}

	public LldpChassisIdSubType getLldpChassisIdSubType() {
		return m_lldpChassisIdSubType;
	}


	public String getLldpSysname() {
		return m_lldpSysname;
	}


	public String getLldpChassisId() {
		return m_lldpChassisId;
	}

	@Override
	public boolean equals(ElementIdentifier elementIdentifier) {
		if (elementIdentifier instanceof LldpElementIdentifier) {
			LldpElementIdentifier a = (LldpElementIdentifier)elementIdentifier;
			if (getLldpChassisId().equals(a.getLldpChassisId()) &&
				getLldpChassisIdSubType().equals(a.getLldpChassisIdSubType()) &&
				getLldpSysname().equals(a.getLldpSysname()))
				return true;
		}
			
		return false;
	}
 
	/**
	 * <p>toString</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String toString() {
		return new ToStringBuilder(this)
			.append("lldpChassisSubType", m_lldpChassisIdSubType)
			.append("lldpChassisId", m_lldpChassisId)
			.append("lldpSysName", m_lldpSysname)
			.append("lastPoll", m_lastPoll)
			.toString();
	}

}