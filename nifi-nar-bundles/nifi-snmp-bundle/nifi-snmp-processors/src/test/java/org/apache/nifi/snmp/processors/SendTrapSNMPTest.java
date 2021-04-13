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

import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSessionFactory;
import org.apache.nifi.remote.io.socket.NetworkUtils;
import org.apache.nifi.snmp.configuration.TrapConfiguration;
import org.apache.nifi.snmp.configuration.TrapConfigurationBuilder;
import org.apache.nifi.snmp.helper.SNMPTestUtils;
import org.apache.nifi.snmp.operations.SNMPTrapReceiver;
import org.apache.nifi.snmp.utils.SNMPUtils;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.TimeTicks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SendTrapSNMPTest {

    @Test
    public void testReceive() throws InterruptedException {

        final int trapSenderListenPort = NetworkUtils.availablePort();
        final int trapSenderAgentPort = NetworkUtils.availablePort();

        final TrapConfiguration configuration = createTrapConfiguration();

        // Create trap sender processor
        final SendTrapSNMP sendTrapSNMP = new SendTrapSNMP();
        final TestRunner runner = TestRunners.newTestRunner(sendTrapSNMP);
        setupTestRunner(runner, trapSenderListenPort, trapSenderAgentPort, configuration);

        final Snmp snmp = SNMPTestUtils.createSnmpClientWithPort(trapSenderAgentPort);


        // Create trap listener
        final ProcessContext processContext = runner.getProcessContext();
        final ProcessSessionFactory processSessionFactory = runner.getProcessSessionFactory();
        final ComponentLog logger = runner.getLogger();

        final SNMPTrapReceiver trapReceiver = new SNMPTrapReceiver(snmp, processContext, processSessionFactory, logger);
        trapReceiver.init();

        sendTrapSNMP.init(processContext);
        sendTrapSNMP.onTrigger(processContext, processSessionFactory);

        Thread.sleep(200);

        final MockFlowFile successFF = runner.getFlowFilesForRelationship(GetSNMP.REL_SUCCESS).get(0);
        assertNotNull(successFF);
        assertEquals("Success", successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + "errorStatusText"));
        assertEquals(configuration.getTrapOidKey().toString(), successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + SnmpConstants.snmpTrapOID + SNMPUtils.SNMP_PROP_DELIMITER + "6"));
        assertEquals(configuration.getTrapOidValue().toString(), successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + configuration.getTrapOidKey() + SNMPUtils.SNMP_PROP_DELIMITER + "4"));
        assertEquals(configuration.getManagerAddress(), successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + SnmpConstants.snmpTrapAddress + SNMPUtils.SNMP_PROP_DELIMITER + "64"));
        assertEquals(String.valueOf(new TimeTicks(configuration.getSysUptime())), successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + SnmpConstants.sysUpTime + SNMPUtils.SNMP_PROP_DELIMITER + "67"));
    }

    private TrapConfiguration createTrapConfiguration() {
        return new TrapConfigurationBuilder()
                .setEnterpriseOid("1.3.6.1.4.1.1824")
                .setAgentAddress("1.2.3.4")
                .setManagerAddress("1.2.3.5")
                .setGenericTrapType(PDUv1.ENTERPRISE_SPECIFIC)
                .setSpecificTrapType(2)
                .setTrapOidKey("1.3.6.1.4.1.1234.2.1.51")
                .setTrapOidValue("TrapOidValue")
                .setSysUptime(5000)
                .build();
    }

    private void setupTestRunner(final TestRunner runner, final int clientPort, final int agentPort, final TrapConfiguration configuration) {
        final String agentHost = "0.0.0.0";
        runner.setProperty(SendTrapSNMP.SNMP_CLIENT_PORT, String.valueOf(clientPort));
        runner.setProperty(SendTrapSNMP.AGENT_HOST, agentHost);
        runner.setProperty(SendTrapSNMP.AGENT_PORT, String.valueOf(agentPort));
        runner.setProperty(SendTrapSNMP.SNMP_COMMUNITY, "public");
        runner.setProperty(SendTrapSNMP.SNMP_VERSION, "SNMPv1");
        runner.setProperty(SendTrapSNMP.ENTERPRISE_OID, configuration.getEnterpriseOid());
        runner.setProperty(SendTrapSNMP.TRAP_OID, configuration.getTrapOidKey().toString());
        runner.setProperty(SendTrapSNMP.TRAP_OID_VALUE, configuration.getTrapOidValue().toString());
        runner.setProperty(SendTrapSNMP.GENERIC_TRAP_TYPE, String.valueOf(configuration.getGenericTrapType()));
        runner.setProperty(SendTrapSNMP.SPECIFIC_TRAP_TYPE, String.valueOf(configuration.getSpecificTrapType()));
        runner.setProperty(SendTrapSNMP.AGENT_ADDRESS, configuration.getAgentAddress());
        runner.setProperty(SendTrapSNMP.MANAGER_ADDRESS, configuration.getManagerAddress());
        runner.setProperty(SendTrapSNMP.SYSTEM_UPTIME, String.valueOf(configuration.getSysUptime()));
    }
}
