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
import org.apache.nifi.snmp.exception.CreateSNMPClientException;
import org.apache.nifi.snmp.exception.SNMPClientInitializationException;
import org.apache.nifi.snmp.utils.SNMPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;

public final class SNMPClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SNMPClientFactory.class);
    private static final String DEFAULT_HOST = "0.0.0.0";
    private static final String DEFAULT_PORT = "0";

    public static Snmp createSnmpClient(final TargetConfiguration configuration, final String clientPort) {
        final int snmpVersion = configuration.getVersion();
        if (SnmpConstants.version3 == snmpVersion) {
            return createSnmpV3Client(configuration, clientPort);
        } else {
            return createSimpleSnmpClient(clientPort);
        }
    }

    private static Snmp createSimpleSnmpClient(final String clientPort) {
        final Snmp snmp;
        try {
            final String port = clientPort == null ? DEFAULT_PORT : clientPort;
            snmp = new Snmp(new DefaultUdpTransportMapping(new UdpAddress(DEFAULT_HOST + "/" + port)));
        } catch (IOException e) {
            final String errorMessage = "Creating SNMP client failed.";
            LOGGER.error(errorMessage, e);
            throw new CreateSNMPClientException(errorMessage);
        }
        initializeSnmpClient(snmp);
        return snmp;
    }

    private static Snmp createSnmpV3Client(final TargetConfiguration configuration, final String clientPort) {
        final Snmp snmp = createSimpleSnmpClient(clientPort);

        // If there's a USM instance associated with the MPv3 bound to this Snmp instance (like an agent running
        // on the same host) it is not null.
        if (snmp.getUSM() == null) {
            final OctetString localEngineId = new OctetString(MPv3.createLocalEngineID());
            final USM usm = new USM(SecurityProtocols.getInstance(), localEngineId, 0);
            SecurityModels.getInstance().addSecurityModel(usm);
        }

        final String username = configuration.getSecurityName();
        final String authProtocol = configuration.getAuthProtocol();
        final String authPassword = configuration.getAuthPassword();
        final String privacyProtocol = configuration.getPrivacyProtocol();
        final String privacyPassword = configuration.getPrivacyPassword();
        final OctetString authPasswordOctet = authPassword != null ? new OctetString(authPassword) : null;
        final OctetString privacyPasswordOctet = privacyPassword != null ? new OctetString(privacyPassword) : null;

        // Add user information.
        snmp.getUSM().addUser(
                new OctetString(username),
                new UsmUser(new OctetString(username), SNMPUtils.getAuth(authProtocol), authPasswordOctet,
                        SNMPUtils.getPriv(privacyProtocol), privacyPasswordOctet));

        initializeSnmpClient(snmp);
        return snmp;
    }

    private static void initializeSnmpClient(Snmp snmpClient) {
        try {
            snmpClient.listen();
        } catch (IOException e) {
            final String errorMessage = "Could not start SNMP client.";
            LOGGER.error(errorMessage, e);
            throw new SNMPClientInitializationException(errorMessage);
        }
    }

    private SNMPClientFactory() {
    }
}
