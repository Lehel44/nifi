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
package org.apache.nifi.snmp.helper;

import org.apache.nifi.snmp.exception.CreateSNMPClientException;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;

public class SNMPTestUtils {

    /**
     * Method to create an instance of SNMP.
     *
     * @return instance of SNMP
     * @throws IOException IO Exception
     */
    public static Snmp createSnmpClient() throws IOException {
        final DefaultUdpTransportMapping transportMapping = new DefaultUdpTransportMapping();
        transportMapping.listen();
        return new Snmp(transportMapping);
    }

    public static Snmp createSnmpClientWithPort(final int clientPort) {
        final Snmp snmp;
        try {
            snmp = new Snmp(new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0" + "/" + clientPort)));
            snmp.listen();
        } catch (IOException e) {
            final String errorMessage = "Creating SNMP client failed.";
            throw new CreateSNMPClientException(errorMessage);
        }
        return snmp;
    }

    /**
     * Method to create community target.
     *
     * @param community community name
     * @param address   address
     * @param version   SNMP version
     * @return community target
     */
    public static CommunityTarget createCommTarget(final String community, final String address, final int version) {
        final CommunityTarget target = new CommunityTarget();
        target.setVersion(version);
        target.setCommunity(new OctetString(community));
        target.setAddress(new UdpAddress(address));
        target.setRetries(0);
        target.setTimeout(500);
        return target;
    }

    /**
     * Method to create a user target
     *
     * @param address       address
     * @param securityLevel security level
     * @param securityName  security name
     * @return user target
     */
    public static UserTarget createUserTarget(final String address, final int securityLevel, final String securityName) {
        final UserTarget target = new UserTarget();
        target.setVersion(SnmpConstants.version3);
        target.setSecurityLevel(securityLevel);
        target.setSecurityName(new OctetString(securityName));
        target.setAddress(new UdpAddress(address));
        target.setRetries(0);
        target.setTimeout(500);
        return target;
    }

    /**
     * Method to prepare user target and to add id in the User Based Security Model of the given SNMP instance
     *
     * @param snmp          SNMP instance
     * @param address       address
     * @param securityLevel security level
     * @param securityName  security name
     * @param auth          authentication protocol
     * @param priv          private protocol
     * @param authPwd       authentication password
     * @param privPwd       private password
     * @return user target
     */
    public static UserTarget prepareUser(final Snmp snmp, final String address, final int securityLevel, final String securityName,
                                         final OID auth, final OID priv, final String authPwd, final String privPwd) {
        snmp.getUSM().removeAllUsers();
        final OctetString aPwd = authPwd != null ? new OctetString(authPwd) : null;
        final OctetString pPwd = privPwd != null ? new OctetString(privPwd) : null;
        snmp.getUSM().addUser(new OctetString(securityName), new UsmUser(new OctetString(securityName), auth, aPwd, priv, pPwd));
        return createUserTarget(address, securityLevel, securityName);
    }

}
