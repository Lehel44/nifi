package org.apache.nifi.snmp.operations;


import org.apache.nifi.snmp.context.SNMPCache;
import org.apache.nifi.snmp.processors.GetSNMP;
import org.apache.nifi.snmp.processors.ListenTrapSNMP;
import org.apache.nifi.snmp.utils.SNMPUtilsTest;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.TimeTicks;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class SNMPTrapReceiverTest {

    @Test
    public void testReceive() throws IOException, InterruptedException {

        //Snmp snmp = new Snmp(new DefaultUdpTransportMapping(new UdpAddress("127.0.0.1/10221")));
        CommunityTarget target = SNMPUtilsTest.createCommTarget("public", "127.0.0.1/10221", SnmpConstants.version1);

        TestRunner runner = getTestRunner();
        runner.run(1, false);
        Thread.sleep(200);

        String enterpriseOID = "1.3.6.1.4.1.1824";
        String agentAddress = "1.2.3.4";
        int genericTrapType = PDUv1.ENTERPRISE_SPECIFIC;
        int specificTrapType = 2;
        OID trapOID = SnmpConstants.snmpTrapOID;
        String managerAddress = "1.2.3.5";
        String trapOIDValue = "HAHAHA";
        TimeTicks sysUpTime = new TimeTicks(5000);

        //SNMPTrapReceiver trapReceiver = new SNMPTrapReceiver(snmp);

        Snmp snmp = SNMPCache.getOrCreate("10221");

        try (SNMPTrapSender trapSender = new SNMPTrapSender(snmp, target)) {

            final ResponseEvent response = trapSender.generateTrap(sysUpTime, enterpriseOID, agentAddress, genericTrapType,
                    specificTrapType, trapOID, managerAddress, trapOIDValue);
            Thread.sleep(3000);
            final MockFlowFile successFF = runner.getFlowFilesForRelationship(GetSNMP.REL_SUCCESS).get(0);
            assertNotNull(successFF);
            //assertEquals(OIDValue1, successFF.getAttribute(SNMPUtils.SNMP_PROP_PREFIX + readOnlyOID1.toString() + SNMPUtils.SNMP_PROP_DELIMITER + "4"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private TestRunner getTestRunner() {
        TestRunner runner = TestRunners.newTestRunner(ListenTrapSNMP.class);
        runner.setProperty(ListenTrapSNMP.SNMP_COMMUNITY, "public");
        runner.setProperty(ListenTrapSNMP.SNMP_VERSION, "SNMPv1");
        runner.setProperty(ListenTrapSNMP.SNMP_CLIENT_PORT, "10221");
        return runner;
    }

}
