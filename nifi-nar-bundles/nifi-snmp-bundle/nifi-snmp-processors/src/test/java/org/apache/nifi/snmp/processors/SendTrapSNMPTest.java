package org.apache.nifi.snmp.processors;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.snmp.context.SNMPCache;
import org.apache.nifi.snmp.helper.SNMPTestUtil;
import org.apache.nifi.snmp.operations.SNMPTrapReceiver;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OID;

import java.io.IOException;

public class SendTrapSNMPTest {
    @Test
    public void testReceive() throws IOException, InterruptedException {

        int port = SNMPTestUtil.availablePort();

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
        setupTestRunner(runner, port, enterpriseOID, trapOID, trapOIDValue, genericTrapType, specificTrapType, managerAddress, sysUpTime);

        Snmp snmp = SNMPCache.getOrCreate(String.valueOf(port));
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
    }

    private void setupTestRunner(TestRunner runner, int port, String enterpriseOID, OID trapOID, String trapOIDValue, int genericTrapType, int specificTrapType, String managerAddress, int sysUpTime) {
        runner.setProperty(SendTrapSNMP.SNMP_COMMUNITY, "public");
        runner.setProperty(SendTrapSNMP.SNMP_VERSION, "SNMPv1");
        runner.setProperty(SendTrapSNMP.SNMP_CLIENT_PORT, String.valueOf(port));
        runner.setProperty(SendTrapSNMP.enterpriseOID, enterpriseOID);
        runner.setProperty(SendTrapSNMP.trapOID, trapOID.toString());
        runner.setProperty(SendTrapSNMP.trapOIDValue, trapOIDValue);
        runner.setProperty(SendTrapSNMP.genericTrapType, String.valueOf(genericTrapType));
        runner.setProperty(SendTrapSNMP.specificTrapType, String.valueOf(specificTrapType));
        runner.setProperty(SendTrapSNMP.managerAddress, managerAddress);
        runner.setProperty(SendTrapSNMP.sysUpTime, String.valueOf(sysUpTime));
        runner.setProperty(SendTrapSNMP.AGENT_PORT, String.valueOf(port));
    }
}
