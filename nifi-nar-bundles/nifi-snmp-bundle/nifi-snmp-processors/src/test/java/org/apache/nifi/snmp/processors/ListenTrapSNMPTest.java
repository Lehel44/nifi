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
package org.apache.nifi.snmp.processors;

import org.apache.nifi.remote.io.socket.NetworkUtils;
import org.apache.nifi.snmp.context.SNMPClientFactory;
import org.apache.nifi.snmp.helper.SNMPTestUtils;
import org.apache.nifi.snmp.operations.SNMPTrapSender;
import org.apache.nifi.snmp.utils.SNMPUtils;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.TimeTicks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ListenTrapSNMPTest {

    @Test
    public void testReceive() throws InterruptedException {

        int port = NetworkUtils.availablePort();
        CommunityTarget target = SNMPTestUtils.createCommTarget("public", "127.0.0.1/" + port, SnmpConstants.version2c);

        TestRunner runner = getTestRunner(port);
        runner.run(1, false);
        Thread.sleep(200);

        String enterpriseOID = "1.3.6.1.4.1.1824";
        String agentAddress = "1.2.3.4";
        int genericTrapType = PDUv1.ENTERPRISE_SPECIFIC;
        int specificTrapType = 2;
        OID trapOID = new OID("1.3.6.1.4.1.1234.2.1.51");
        String managerAddress = "1.2.3.5";
        String trapOIDValue = "TrapOidValue";
        TimeTicks sysUpTime = new TimeTicks(5000);

        Snmp snmp = SNMPClientFactory.createSnmpClient(String.valueOf(NetworkUtils.availablePort()));

        try {
            SNMPTrapSender trapSender = new SNMPTrapSender(snmp, target);
            trapSender.generateTrap(sysUpTime, enterpriseOID, agentAddress, genericTrapType, specificTrapType, trapOID, managerAddress, trapOIDValue);
            // TODO-3328: Countdownlatch
            Thread.sleep(200);
            final MockFlowFile successFF = runner.getFlowFilesForRelationship(GetSNMP.REL_SUCCESS).get(0);
            assertNotNull(successFF);
            assertEquals("Success", successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + "errorStatusText"));
            assertEquals(trapOID.toString(), successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + SnmpConstants.snmpTrapOID + SNMPUtils.SNMP_PROP_DELIMITER + "6"));
            assertEquals(managerAddress, successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + SnmpConstants.snmpTrapAddress + SNMPUtils.SNMP_PROP_DELIMITER + "64"));
            assertEquals(sysUpTime.toString(), successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + SnmpConstants.sysUpTime + SNMPUtils.SNMP_PROP_DELIMITER + "67"));
            assertEquals(trapOIDValue, successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + trapOID + SNMPUtils.SNMP_PROP_DELIMITER + "4"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private TestRunner getTestRunner(int port) {
        TestRunner runner = TestRunners.newTestRunner(ListenTrapSNMP.class);
        runner.setProperty(ListenTrapSNMP.SNMP_COMMUNITY, "public");
        runner.setProperty(ListenTrapSNMP.SNMP_VERSION, "SNMPv1");
        runner.setProperty(ListenTrapSNMP.SNMP_CLIENT_PORT, String.valueOf(port));
        return runner;
    }

}
