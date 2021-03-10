package org.apache.nifi.snmp.operations;

import org.apache.nifi.processor.exception.ProcessException;
import org.snmp4j.AbstractTarget;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;

import java.io.IOException;

public class SnmpTrapSender extends SnmpWorker {

    private static final String trapOid = ".1.3.6.1.2.1.1.6";
    private static final String ipAddress = "127.0.0.1";

    /**
     * Creates an instance of this worker and initializing it with {@link Snmp}
     * and {@link AbstractTarget} used by sub-classes to interact with SNMP agent.
     *
     * @param snmp   instance of {@link Snmp}
     * @param target instance of {@link AbstractTarget}
     */
    protected SnmpTrapSender(Snmp snmp, AbstractTarget target) {
        super(snmp, target);
    }

    public ResponseEvent generateTrap() {
        try {
            PDU pdu;
            if (target.getVersion() == SnmpConstants.version1) {
                // Automatically sets type to V1TRAP.
                PDUv1 pdu1 = new PDUv1();
                pdu1.setEnterprise(new OID("1.3.6.1.4.1.1824"));
                pdu1.setAgentAddress(new IpAddress("10.0.0.7"));     //SET THIS. This is the sender address
                pdu1.setSpecificTrap(5);
                pdu1.setGenericTrap(23);
                pdu = pdu1;
            } else {
                PDU pdu2 = new PDU();
                pdu2.setType(PDU.TRAP);
                pdu2.setRequestID(new Integer32(123));
                pdu = pdu2;
            }
            pdu.add(new VariableBinding(SnmpConstants.sysUpTime));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(trapOid)));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(
                    ipAddress)));
            pdu.add(new VariableBinding(new OID(trapOid), new OctetString("Major")));

            return snmp.get(pdu, target);
        } catch (IOException e) {
            LOGGER.error("Failed to get information from SNMP agent; {}", this, e);
            throw new ProcessException(e);
        }
    }
}
