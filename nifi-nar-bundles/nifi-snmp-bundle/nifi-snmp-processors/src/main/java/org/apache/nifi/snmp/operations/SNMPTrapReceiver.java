package org.apache.nifi.snmp.operations;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.snmp.utils.SNMPUtils;
import org.snmp4j.*;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static org.apache.nifi.snmp.processors.ListenTrapSNMP.REL_SUCCESS;
import static org.apache.nifi.snmp.processors.ListenTrapSNMP.REL_FAILURE;

public class SNMPTrapReceiver implements CommandResponder {

    private final Snmp snmp;
    private final ProcessContext context;
    private final ProcessSession processSession;
    private final ComponentLog logger;

    public SNMPTrapReceiver(final Snmp snmp, final ProcessContext context, final ProcessSession processSession, final ComponentLog logger) throws IOException {
        this.snmp = snmp;
        this.context = context;
        this.processSession = processSession;
        this.logger = logger;
        snmp.addCommandResponder(this);
    }

    public void closeReceiver() {
        snmp.removeCommandResponder(this);
    }

    @Override
    public void processPdu(CommandResponderEvent event) {
        PDU pdu = event.getPDU();
        // check trap PDU, does it catch snmp get?
        if (pdu != null) {
            FlowFile flowFile = createFlowFile(context, processSession, pdu);
            processSession.getProvenanceReporter().receive(flowFile, event.getPeerAddress() + "/" + pdu.getRequestID());
            if (pdu.getErrorStatus() == PDU.noError) {
                processSession.transfer(flowFile, REL_SUCCESS);
            } else {
                processSession.transfer(flowFile, REL_FAILURE);
            }
        } else {
            logger.error("Get request timed out or parameters are incorrect.");
            context.yield();
        }

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

    private FlowFile createFlowFile(ProcessContext context, ProcessSession processSession, PDU pdu) {
        FlowFile flowFile = processSession.create();
        flowFile = SNMPUtils.updateFlowFileAttributesWithPduProperties(pdu, flowFile, processSession);
        //flowFile = SNMPUtils.addAttribute(SNMPUtils.SNMP_PROP_PREFIX + "textualOid", context.getProperty(TEXTUAL_OID).getValue(),
        //        flowFile, processSession);
        return flowFile;
    }

    public Snmp getSnmp() {
        return snmp;
    }
}
