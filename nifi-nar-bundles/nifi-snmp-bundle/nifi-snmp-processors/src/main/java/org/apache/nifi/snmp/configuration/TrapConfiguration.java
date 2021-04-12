package org.apache.nifi.snmp.configuration;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

public class TrapConfiguration {

    private final String enterpriseOid;
    private final String agentAddress;
    private final String managerAddress;
    private final int genericTrapType;
    private final int specificTrapType;
    private final OID trapOidKey;
    private final OctetString trapOidValue;
    private final int sysUptime;

    public TrapConfiguration(final String enterpriseOid, final String agentAddress, final String managerAddress, final int genericTrapType,
                             final int specificTrapType, final OID trapOidKey, final OctetString trapOidValue, final int sysUptime) {
        this.enterpriseOid = enterpriseOid;
        this.agentAddress = agentAddress;
        this.managerAddress = managerAddress;
        this.genericTrapType = genericTrapType;
        this.specificTrapType = specificTrapType;
        this.trapOidKey = trapOidKey;
        this.trapOidValue = trapOidValue;
        this.sysUptime = sysUptime;
    }

    public String getEnterpriseOid() {
        return enterpriseOid;
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public String getManagerAddress() {
        return managerAddress;
    }

    public int getGenericTrapType() {
        return genericTrapType;
    }

    public int getSpecificTrapType() {
        return specificTrapType;
    }

    public OID getTrapOidKey() {
        return trapOidKey;
    }

    public OctetString getTrapOidValue() {
        return trapOidValue;
    }

    public int getSysUptime() {
        return sysUptime;
    }
}
