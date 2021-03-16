package org.apache.nifi.snmp.operations;

import org.snmp4j.*;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

public class SNMPTrapReceiver implements CommandResponder {

    public SNMPTrapReceiver(Snmp snmp) throws IOException {
        snmp.listen();
        snmp.addCommandResponder(this);
    }

    public void closeReceiver(Snmp snmp) throws IOException {
        snmp.removeCommandResponder(this);
        snmp.close();
    }

    @Override
    public void processPdu(CommandResponderEvent event) {
        PDU pdu = event.getPDU();
        if (pdu.getType() == PDU.V1TRAP) {

            PDUv1 pduV1 = (PDUv1) pdu;
            System.out.println();
            System.out.println("===== NEW SNMP 1 TRAP RECEIVED ====");
            System.out.println("agentAddr " + pduV1.getAgentAddress().toString());
            System.out.println("enterprise " + pduV1.getEnterprise().toString());
            System.out.println("timeStam " + pduV1.getTimestamp());
            System.out.println("genericTrap " + pduV1.getGenericTrap());
            System.out.println("specificTrap " + pduV1.getSpecificTrap());
            System.out.println("snmpVersion " + PDU.V1TRAP);
            System.out.println("communityString " + new String(event.getSecurityName()));

        } else if (pdu.getType() == PDU.TRAP) {
            System.out.println();
            System.out.println("===== NEW SNMP 2/3 TRAP RECEIVED ====");

            System.out.println("errorStatus " + pdu.getErrorStatus());
            System.out.println("errorIndex " + pdu.getErrorIndex());
            System.out.println("requestID " + pdu.getRequestID());
            System.out.println("snmpVersion " + PDU.TRAP);
            System.out.println("communityString " + new String(event.getSecurityName()));

        }

        Vector<? extends VariableBinding> varBinds = pdu.getVariableBindings();
        if (varBinds != null && !varBinds.isEmpty()) {
            Iterator<? extends VariableBinding> varIter = varBinds.iterator();

            StringBuilder resultset = new StringBuilder();
            resultset.append("-----");
            while (varIter.hasNext()) {
                VariableBinding vb = varIter.next();

                String syntaxstr = vb.getVariable().getSyntaxString();
                int syntax = vb.getVariable().getSyntax();
                System.out.println("OID: " + vb.getOid());
                System.out.println("Value: " + vb.getVariable());
                System.out.println("syntaxstring: " + syntaxstr);
                System.out.println("syntax: " + syntax);
                System.out.println("------");
            }


        }
        System.out.println("==== TRAP END ===");
        System.out.println("");
    }

}
