package org.apache.nifi.snmp.utils;

import org.snmp4j.mp.SnmpConstants;

import java.util.Arrays;


public enum SNMPVersion {
    SNMP_V1("SNMPv1", SnmpConstants.version1),
    SNMP_V2C("SNMPv2c", SnmpConstants.version2c),
    SNMP_V3("SNMPv3", SnmpConstants.version3);

    private final String snmpVersionDisplay;
    private final int snmpVersionNumber;


    SNMPVersion(final String snmpVersionDisplay, final int snmpVersionNumber) {
        this.snmpVersionDisplay = snmpVersionDisplay;
        this.snmpVersionNumber = snmpVersionNumber;
    }

    public static int getSnmpVersionNumberByDisplay(String display) {
        final SNMPVersion snmpVersion = Arrays.stream(SNMPVersion.values())
                .filter(s -> s.snmpVersionDisplay.equals(display))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Invalid SNMP verison"));
        return snmpVersion.getSnmpVersionNumber();
    }

    public String getSnmpVersionDisplay() {
        return snmpVersionDisplay;
    }

    public int getSnmpVersionNumber() {
        return snmpVersionNumber;
    }
}
