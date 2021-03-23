/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
