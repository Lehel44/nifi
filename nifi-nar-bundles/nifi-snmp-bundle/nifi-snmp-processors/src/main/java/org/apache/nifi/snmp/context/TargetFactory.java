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
package org.apache.nifi.snmp.context;

import org.apache.nifi.snmp.configuration.TargetConfiguration;
import org.apache.nifi.snmp.utils.SNMPUtils;
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;

import java.util.Optional;

public class TargetFactory {

    public static AbstractTarget createTarget(TargetConfiguration configuration) {
        final int snmpVersion = SNMPUtils.getSnmpVersion(configuration.getVersion());
        if (SnmpConstants.version3 == snmpVersion) {
            return createUserTarget(configuration);
        } else {
            return createCommunityTarget(configuration);
        }
    }

    private static UserTarget createUserTarget(TargetConfiguration configuration) {
        UserTarget userTarget = new UserTarget();
        setupTargetBasicProperties(userTarget, configuration);

        int securityLevel = SecurityLevel.valueOf(configuration.getSecurityLevel()).getSnmpValue();
        userTarget.setSecurityLevel(securityLevel);

        final String securityName = configuration.getSecurityName();
        Optional.ofNullable(securityName).map(OctetString::new).ifPresent(userTarget::setSecurityName);

        return userTarget;
    }

    private static CommunityTarget createCommunityTarget(TargetConfiguration configuration) {
        CommunityTarget communityTarget = new CommunityTarget();
        setupTargetBasicProperties(communityTarget, configuration);
        String community = configuration.getCommunityString();

        Optional.ofNullable(community).map(OctetString::new).ifPresent(communityTarget::setSecurityName);

        return communityTarget;
    }

    private static void setupTargetBasicProperties(AbstractTarget target, TargetConfiguration configuration) {
        final int snmpVersion = SNMPUtils.getSnmpVersion(configuration.getVersion());
        final String host = configuration.getAgentHost();
        final String port = configuration.getAgentPort();
        final int retries = configuration.getRetries();
        final int timeout = configuration.getTimeout();

        target.setVersion(snmpVersion);
        target.setAddress(new UdpAddress(host + "/" + port));
        target.setRetries(retries);
        target.setTimeout(timeout);
    }

    private TargetFactory() {
    }
}
