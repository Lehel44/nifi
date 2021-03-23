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

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.remote.io.socket.NetworkUtils;
import org.apache.nifi.snmp.context.SNMPClientFactory;
import org.apache.nifi.snmp.operations.SNMPTrapReceiver;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Ignore;
import org.junit.Test;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OID;

import java.io.IOException;

public class SendTrapSNMPTest {

    @Ignore("Found a bug in TestRunner, sometimes exits when debugging. Needs further investigation.")
    @Test
    public void testReceive() throws IOException, InterruptedException {

        int trapSenderClientPort = NetworkUtils.availablePort();
        String trapSenderAgentHost = "0.0.0.0";
        int trapSenderAgentPort = NetworkUtils.availablePort();

        String enterpriseOID = "1.3.6.1.4.1.1824";
        String agentAddress = "1.2.3.4";
        int genericTrapType = PDUv1.ENTERPRISE_SPECIFIC;
        int specificTrapType = 2;
        OID trapOID = new OID("1.3.6.1.4.1.1234.2.1.51");
        String managerAddress = "1.2.3.5";
        String trapOIDValue = "HAHAHA";
        int sysUpTime = 5000;

        SendTrapSNMP sendTrapSNMP = new SendTrapSNMP();
        TestRunner runner = TestRunners.newTestRunner(sendTrapSNMP);
        setupTestRunner(runner, trapSenderClientPort, trapSenderAgentHost, trapSenderAgentPort, enterpriseOID,
                trapOID, trapOIDValue, genericTrapType, specificTrapType, managerAddress, sysUpTime, agentAddress);

        Snmp snmp = SNMPClientFactory.createSnmpClient(String.valueOf(trapSenderAgentPort));
        snmp.listen();

        final ProcessContext processContext = runner.getProcessContext();
        final ProcessSession session = runner.getProcessSessionFactory().createSession();
        final ComponentLog logger = runner.getLogger();

        SNMPTrapReceiver trapReceiver = new SNMPTrapReceiver(snmp, processContext, session, logger);
        Thread.sleep(200);

        sendTrapSNMP.initSnmpClient(processContext);
        sendTrapSNMP.onTrigger(processContext, session);

        Thread.sleep(2000);

        final FlowFile flowFile = session.get();
        System.out.println("1");
        Thread.sleep(1000000);
    }

    private void setupTestRunner(TestRunner runner, int clientPort, String agentHost, int agentPort, String enterpriseOID,
                                 OID trapOID, String trapOIDValue, int genericTrapType, int specificTrapType, String managerAddress,
                                 int sysUpTime, String agentAddress) {
        runner.setProperty(SendTrapSNMP.SNMP_CLIENT_PORT, String.valueOf(clientPort));
        runner.setProperty(SendTrapSNMP.AGENT_HOST, String.valueOf(agentHost));
        runner.setProperty(SendTrapSNMP.AGENT_PORT, String.valueOf(agentPort));
        runner.setProperty(SendTrapSNMP.SNMP_COMMUNITY, "public");
        runner.setProperty(SendTrapSNMP.SNMP_VERSION, "SNMPv1");
        runner.setProperty(SendTrapSNMP.ENTERPRISE_OID, enterpriseOID);
        runner.setProperty(SendTrapSNMP.TRAP_OID, trapOID.toString());
        runner.setProperty(SendTrapSNMP.TRAP_OID_VALUE, trapOIDValue);
        runner.setProperty(SendTrapSNMP.GENERIC_TRAP_TYPE, String.valueOf(genericTrapType));
        runner.setProperty(SendTrapSNMP.SPECIFIC_TRAP_TYPE, String.valueOf(specificTrapType));
        runner.setProperty(SendTrapSNMP.MANAGER_ADDRESS, managerAddress);
        runner.setProperty(SendTrapSNMP.SYSTEM_UPTIME, String.valueOf(sysUpTime));
        runner.setProperty(SendTrapSNMP.AGENT_ADDRESS, agentAddress);
    }
}
