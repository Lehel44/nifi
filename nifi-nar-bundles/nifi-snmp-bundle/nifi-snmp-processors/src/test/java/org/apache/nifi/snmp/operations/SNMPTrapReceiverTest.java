package org.apache.nifi.snmp.operations;


import org.apache.nifi.snmp.utils.SNMPUtilsTest;
import org.junit.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;

public class SNMPTrapReceiverTest {

    @Test
    public void testReceive() throws IOException {

        Snmp snmp = new Snmp(new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/10333")));
        CommunityTarget target = SNMPUtilsTest.createCommTarget("public", "127.0.0.1/10333", SnmpConstants.version1);

        String enterpriseOID = "1.3.6.1.4.1.1824";
        String agentAddress = "127.0.0.1";
        int genericTrapType = PDUv1.ENTERPRISE_SPECIFIC;
        int specificTrapType = 2;
        String trapOID = ".1.3.6.1.2.1.1.6";
        String managerAddress = "127.0.0.1";
        String trapOIDValue = "HAHAHA";

        SNMPTrapReceiver trapReceiver = new SNMPTrapReceiver(snmp);

        try (SNMPTrapSender trapSender = new SNMPTrapSender(snmp, target)) {

            final ResponseEvent response = trapSender.generateTrap(enterpriseOID, agentAddress, genericTrapType,
                    specificTrapType, trapOID, managerAddress, trapOIDValue);
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
