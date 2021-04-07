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
package org.apache.nifi.snmp.operations;

import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.snmp.helper.SNMPTestUtils;
import org.apache.nifi.snmp.testagents.TestSNMPV1Agent;
import org.apache.nifi.snmp.testagents.TestSNMPV2Agent;
import org.apache.nifi.snmp.testagents.TestSNMPV3Agent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.Snmp;
import org.snmp4j.UserTarget;
import org.snmp4j.agent.mo.DefaultMOFactory;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TreeEvent;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SNMPGetterTest {

    private static final String LOCALHOST = "0.0.0.0";
    private static final OID readOnlyOID1 = new OID("1.3.6.1.4.1.32437.1.5.1.4.2.0");
    private static final OID readOnlyOID2 = new OID("1.3.6.1.4.1.32437.1.5.1.4.3.0");
    private static final String OIDValue1 = "TestOID1";
    private static final String OIDValue2 = "TestOID2";

    private static TestSNMPV1Agent snmpV1Agent;
    private static TestSNMPV2Agent snmpV2Agent;
    private static TestSNMPV3Agent snmpV3Agent;


    @BeforeClass
    public static void setUp() throws IOException {
        snmpV1Agent = new TestSNMPV1Agent(LOCALHOST);
        snmpV1Agent.start();
        snmpV1Agent.registerManagedObjects(
                DefaultMOFactory.getInstance().createScalar(new OID(readOnlyOID1), MOAccessImpl.ACCESS_READ_ONLY, new OctetString(OIDValue1)),
                DefaultMOFactory.getInstance().createScalar(new OID(readOnlyOID2), MOAccessImpl.ACCESS_READ_ONLY, new OctetString(OIDValue2))
        );
        snmpV2Agent = new TestSNMPV2Agent(LOCALHOST);
        snmpV2Agent.start();
        snmpV2Agent.registerManagedObjects(
                DefaultMOFactory.getInstance().createScalar(new OID(readOnlyOID1), MOAccessImpl.ACCESS_READ_ONLY, new OctetString(OIDValue1)),
                DefaultMOFactory.getInstance().createScalar(new OID(readOnlyOID2), MOAccessImpl.ACCESS_READ_ONLY, new OctetString(OIDValue2))
        );
        snmpV3Agent = new TestSNMPV3Agent(LOCALHOST);
        snmpV3Agent.start();
        snmpV3Agent.registerManagedObjects(
                DefaultMOFactory.getInstance().createScalar(new OID(readOnlyOID1), MOAccessImpl.ACCESS_READ_ONLY, new OctetString(OIDValue1))
        );
    }


    @AfterClass
    public static void tearDown() {
        snmpV1Agent.stop();
        snmpV2Agent.stop();
        snmpV3Agent.stop();
    }

    @Test
    public void testSuccessfulSnmpV1Get() throws IOException {
        ResponseEvent response = getResponseEvent(snmpV1Agent.getPort(), SnmpConstants.version1);
        assertEquals(OIDValue1, response.getResponse().get(0).getVariable().toString());

    }

    @Test
    public void testSuccessfulSnmpV1Walk() throws IOException {
        final List<TreeEvent> responseEvents = getTreeEvents(snmpV1Agent.getPort(), SnmpConstants.version1);
        assertEquals(OIDValue1, responseEvents.get(0).getVariableBindings()[0].getVariable().toString());
        assertEquals(OIDValue2, responseEvents.get(1).getVariableBindings()[0].getVariable().toString());

    }

    @Test
    public void testSuccessfulSnmpV2Get() throws IOException {
        ResponseEvent response = getResponseEvent(snmpV2Agent.getPort(), SnmpConstants.version2c);
        assertEquals(OIDValue1, response.getResponse().get(0).getVariable().toString());

    }

    @Test
    public void testSuccessfulSnmpV2Walk() throws IOException {
        final List<TreeEvent> responseEvents = getTreeEvents(snmpV2Agent.getPort(), SnmpConstants.version2c);
        final VariableBinding[] variableBindings = responseEvents.get(0).getVariableBindings();
        assertEquals(OIDValue1, variableBindings[0].getVariable().toString());
        assertEquals(OIDValue2, variableBindings[1].getVariable().toString());

    }

    @Test
    public void testSuccessfulSnmpV3Get() throws IOException {
        SNMPRequestHandler snmpRequestHandler = getSnmpV3Getter("SHA", "SHAAuthPassword");
        ResponseEvent response = snmpRequestHandler.get(readOnlyOID1);
        assertEquals(OIDValue1, response.getResponse().get(0).getVariable().toString());

    }

    @Test
    public void testUnauthorizedUserSnmpV3GetReturnsNull() throws IOException {
        SNMPRequestHandler snmpRequestHandler = getSnmpV3Getter("FakeUserName", "FakeAuthPassword");
        ResponseEvent response = snmpRequestHandler.get(readOnlyOID1);
        assertEquals("Null", response.getResponse().get(0).getVariable().toString());

    }

    @Test
    public void testSnmpV1GetTimeoutReturnsNull() throws IOException {
        Snmp snmp = SNMPTestUtils.createSnmp();
        CommunityTarget target = SNMPTestUtils.createCommTarget("public", "127.0.0.2/" + snmpV1Agent.getPort(), SnmpConstants.version1);
        SNMPRequestHandler snmpRequestHandler = new SNMPRequestHandler(snmp, target);
        ResponseEvent response = snmpRequestHandler.get(readOnlyOID1);
        assertNull(response.getResponse());
    }

    @Test(expected = ProcessException.class)
    public void testSnmpV1GetWithInvalidTargetThrowsException() throws IOException {
        Snmp snmp = SNMPTestUtils.createSnmp();
        CommunityTarget target = SNMPTestUtils.createCommTarget("public", LOCALHOST + "/" + snmpV1Agent.getPort(), -1);
        SNMPRequestHandler snmpRequestHandler = new SNMPRequestHandler(snmp, target);
        snmpRequestHandler.get(readOnlyOID1);

    }

    private ResponseEvent getResponseEvent(int port, int version) throws IOException {
        Snmp snmp = SNMPTestUtils.createSnmp();
        CommunityTarget target = SNMPTestUtils.createCommTarget("public", LOCALHOST + "/" + port, version);
        SNMPRequestHandler snmpRequestHandler = new SNMPRequestHandler(snmp, target);
        return snmpRequestHandler.get(readOnlyOID1);
    }


    private SNMPRequestHandler getSnmpV3Getter(String sha, String shaAuthPassword) throws IOException {
        SNMP4JSettings.setForwardRuntimeExceptions(true);
        Snmp snmp = SNMPTestUtils.createSnmp();
        final UserTarget userTarget = SNMPTestUtils.prepareUser(snmp, LOCALHOST + "/" + snmpV3Agent.getPort(), SecurityLevel.AUTH_NOPRIV,
                sha, AuthSHA.ID, null, shaAuthPassword, null);
        return new SNMPRequestHandler(snmp, userTarget);
    }

    private List<TreeEvent> getTreeEvents(int port, int version) throws IOException {
        Snmp snmp = SNMPTestUtils.createSnmp();
        CommunityTarget target = SNMPTestUtils.createCommTarget("public", LOCALHOST + "/" + port, version);
        SNMPRequestHandler snmpRequestHandler = new SNMPRequestHandler(snmp, target);
        return snmpRequestHandler.walk(new OID("1.3.6.1.4.1.32437"));
    }
}
