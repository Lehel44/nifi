package org.apache.nifi.snmp.configuration;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.util.StringUtils;

public class SecurityConfiguration {

    private static final String SNMP_V3 = "SNMPv3";

    private final boolean isVersion3;
    private final boolean isAuthProtocolValid;
    private final boolean isAuthPasswordSet;
    private final boolean isPrivateProtocolValid;
    private final boolean isPrivatePasswordSet;
    private final boolean isSecurityNameSet;
    private final SecurityLevel securityLevel;
    private final boolean isCommunityStringSet;

    public SecurityConfiguration(final ValidationContext validationContext,
                                 final PropertyDescriptor snmpVersion,
                                 final PropertyDescriptor snmpSecurityName,
                                 final PropertyDescriptor snmpAuthProtocol,
                                 final PropertyDescriptor snmpAuthPassword,
                                 final PropertyDescriptor snmpPrivateProtocol,
                                 final PropertyDescriptor snmpPrivatePassword,
                                 final PropertyDescriptor snmpSecurityLevel,
                                 final PropertyDescriptor snmpCommunityString) {

        this.isVersion3 = SNMP_V3.equals(validationContext.getProperty(snmpVersion).getValue());
        this.isSecurityNameSet = validationContext.getProperty(snmpSecurityName).isSet();
        this.isAuthProtocolValid = !StringUtils.EMPTY.equals(validationContext.getProperty(snmpAuthProtocol).getValue());
        this.isAuthPasswordSet = validationContext.getProperty(snmpAuthPassword).isSet();
        this.isPrivateProtocolValid = !StringUtils.EMPTY.equals(validationContext.getProperty(snmpPrivateProtocol).getValue());
        this.isPrivatePasswordSet = validationContext.getProperty(snmpPrivatePassword).isSet();
        this.securityLevel = SecurityLevel.valueOf(validationContext.getProperty(snmpSecurityLevel).getValue());
        this.isCommunityStringSet = validationContext.getProperty(snmpCommunityString).isSet();
    }

    public static String getSnmpV3() {
        return SNMP_V3;
    }

    public boolean isVersion3() {
        return isVersion3;
    }

    public boolean isAuthProtocolValid() {
        return isAuthProtocolValid;
    }

    public boolean isAuthPasswordSet() {
        return isAuthPasswordSet;
    }

    public boolean isPrivacyProtocolValid() {
        return isPrivateProtocolValid;
    }

    public boolean isPrivacyPasswordSet() {
        return isPrivatePasswordSet;
    }

    public boolean isSecurityNameSet() {
        return isSecurityNameSet;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public boolean isCommunityStringSet() {
        return isCommunityStringSet;
    }
}
