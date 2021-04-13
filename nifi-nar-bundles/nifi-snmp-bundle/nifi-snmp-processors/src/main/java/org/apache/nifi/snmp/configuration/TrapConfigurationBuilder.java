package org.apache.nifi.snmp.configuration;

import org.apache.nifi.util.StringUtils;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

public class TrapConfigurationBuilder {
    private String enterpriseOid;
    private String agentAddress;
    private String managerAddress;
    private int genericTrapType;
    private int specificTrapType;
    private OID trapOidKey;
    private OctetString trapOidValue;
    private int sysUptime;

    public TrapConfigurationBuilder setEnterpriseOid(String enterpriseOid) {
        this.enterpriseOid = enterpriseOid;
        return this;
    }

    public TrapConfigurationBuilder setAgentAddress(String agentAddress) {
        this.agentAddress = agentAddress;
        return this;
    }

    public TrapConfigurationBuilder setManagerAddress(String managerAddress) {
        this.managerAddress = managerAddress;
        return this;
    }

    public TrapConfigurationBuilder setGenericTrapType(int genericTrapType) {
        this.genericTrapType = genericTrapType;
        return this;
    }

    public TrapConfigurationBuilder setSpecificTrapType(int specificTrapType) {
        this.specificTrapType = specificTrapType;
        return this;
    }

    public TrapConfigurationBuilder setTrapOidKey(String trapOidKey) {
        if (StringUtils.isNotEmpty(trapOidKey)) {
            this.trapOidKey = new OID(trapOidKey);
        }
        return this;
    }

    public TrapConfigurationBuilder setTrapOidValue(String trapOidValue) {
        if (StringUtils.isNotEmpty(trapOidValue)) {
            this.trapOidValue = new OctetString(trapOidValue);
        }
        return this;
    }

    public TrapConfigurationBuilder setSysUptime(int sysUptime) {
        this.sysUptime = sysUptime;
        return this;
    }

    public TrapConfiguration build() {
        return new TrapConfiguration(enterpriseOid, agentAddress, managerAddress, genericTrapType, specificTrapType, trapOidKey, trapOidValue, sysUptime);
    }
}