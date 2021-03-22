package org.apache.nifi.snmp.operations;

import org.apache.nifi.processor.exception.ProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;

public class SNMPTrapSender extends SNMPWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SNMPTrapSender.class);

    /**
     * Creates an instance of this worker and initializing it with {@link Snmp}
     * and {@link AbstractTarget} used by sub-classes to interact with SNMP agent.
     *
     * @param snmp   instance of {@link Snmp}
     * @param target instance of {@link AbstractTarget}
     */
    public SNMPTrapSender(Snmp snmp, AbstractTarget target) {
        super(snmp, target);
    }

    public ResponseEvent generateTrap(TimeTicks sysUpTime, String enterpriseOID, String agentAddress, int genericTrapType,
                                      int specificTrapType, OID trapOIDKey, String managerAddress, String trapOIDValue) {
        try {
            PDU pdu;
            if (target.getVersion() == SnmpConstants.version1) {
                // Automatically sets type to V1TRAP.
                PDUv1 pdu1 = new PDUv1();
                pdu1.setEnterprise(new OID(enterpriseOID));
                pdu1.setAgentAddress(new IpAddress(agentAddress));
                pdu1.setGenericTrap(genericTrapType);
                pdu1.setSpecificTrap(specificTrapType);
                pdu = pdu1;
            } else {
                PDU pdu2 = new PDU();
                pdu2.setType(PDU.TRAP);
                pdu = pdu2;
            }
            pdu.add(new VariableBinding(SnmpConstants.sysUpTime, sysUpTime));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, trapOIDKey));
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(managerAddress)));
            pdu.add(new VariableBinding(trapOIDKey, new OctetString(trapOIDValue)));

            return snmp.send(pdu, target);
        } catch (IOException e) {
            LOGGER.error("Failed to send trap from SNMP agent; {}", this, e);
            throw new ProcessException(e);
        }
    }
}
