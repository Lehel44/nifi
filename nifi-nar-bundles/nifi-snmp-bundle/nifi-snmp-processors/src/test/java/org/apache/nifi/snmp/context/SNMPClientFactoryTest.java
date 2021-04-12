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

import org.apache.nifi.remote.io.socket.NetworkUtils;
import org.apache.nifi.snmp.configuration.TargetConfiguration;
import org.apache.nifi.snmp.configuration.TargetConfigurationBuilder;
import org.junit.Test;
import org.mockito.internal.util.collections.Iterables;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;

import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SNMPClientFactoryTest {

    private final TargetConfigurationBuilder configurationBuilder = new TargetConfigurationBuilder()
            .setAgentHost("1.2.3.4")
            .setAgentPort("12345")
            .setRetries(1)
            .setTimeout(1000)
            .setSecurityLevel("noAuthNoPriv")
            .setSecurityName("userName")
            .setAuthProtocol("SHA")
            .setAuthPassword("authPassword")
            .setPrivacyProtocol("DES")
            .setPrivacyPassword("privacyPassword")
            .setCommunityString("public");

    @Test
    public void testFactoryCreatesSnmpV1V2cClientWithCorrectTransportMapping() {
        String clientPort = String.valueOf(NetworkUtils.availablePort());
        TargetConfiguration configuration = configurationBuilder
                .setVersion(SNMPVersion.SNMP_V1)
                .build();

        Snmp snmp = SNMPClientFactory.createSnmpClient(configuration, clientPort);

        final Collection<TransportMapping> transportMappings = snmp.getMessageDispatcher().getTransportMappings();

        assertThat(transportMappings.size(), is(equalTo(1)));

        final TransportMapping transportMapping = Iterables.firstOf(transportMappings);
        final Address address = transportMapping.getListenAddress();

        assertThat(address, is(instanceOf(UdpAddress.class)));

        final UdpAddress udpAddress = (UdpAddress) address;
        final String actualPort = String.valueOf(udpAddress.getPort());
        final String actualHost = udpAddress.getInetAddress().getHostAddress();

        assertThat(actualPort, is(equalTo(clientPort)));
        assertThat("0.0.0.0", is(equalTo(actualHost)));
    }

    @Test
    public void testSnmpV3ClientWithoutCorrespondingAgentDoesNotHaveUSM() throws IOException {
        String clientPort = String.valueOf(NetworkUtils.availablePort());
        TargetConfiguration configuration = configurationBuilder
                .setVersion(SNMPVersion.SNMP_V3)
                .build();

        final Snmp snmpClient = SNMPClientFactory.createSnmpClient(configuration, clientPort);
        final UsmUser user = snmpClient.getUSM().getUserTable().getUser(new OctetString("userName")).getUsmUser();

        final OID usmHMACSHAAuthProtocol = new OID("1.3.6.1.6.3.10.1.1.3");
        final OID usmDESPrivProtocol = new OID("1.3.6.1.6.3.10.1.2.2");

        assertThat("userName", is(equalTo(user.getSecurityName().toString())));
        assertThat(usmHMACSHAAuthProtocol, is(equalTo(user.getAuthenticationProtocol())));
        assertThat("authPassword", is(equalTo(user.getAuthenticationPassphrase().toString())));
        assertThat(usmDESPrivProtocol, is(equalTo(user.getPrivacyProtocol())));
        assertThat("privacyPassword", is(equalTo(user.getPrivacyPassphrase().toString())));
        assertThat(3, is(equalTo(user.getSecurityModel())));
    }
}
