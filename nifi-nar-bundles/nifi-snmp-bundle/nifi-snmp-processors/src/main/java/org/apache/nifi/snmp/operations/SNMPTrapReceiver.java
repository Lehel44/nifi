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
package org.apache.nifi.snmp.operations;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessSessionFactory;
import org.apache.nifi.snmp.utils.SNMPUtils;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;

import static org.apache.nifi.snmp.processors.ListenTrapSNMP.REL_FAILURE;
import static org.apache.nifi.snmp.processors.ListenTrapSNMP.REL_SUCCESS;

public class SNMPTrapReceiver implements CommandResponder {

    private final Snmp snmp;
    private final ProcessContext context;
    private final ProcessSessionFactory processSessionFactory;
    private final ComponentLog logger;

    public SNMPTrapReceiver(final Snmp snmp, final ProcessContext context, final ProcessSessionFactory processSessionFactory, final ComponentLog logger) {
        this.snmp = snmp;
        this.context = context;
        this.processSessionFactory = processSessionFactory;
        this.logger = logger;
    }

    public void init() {
        snmp.addCommandResponder(this);
    }

    @Override
    public synchronized void processPdu(final CommandResponderEvent event) {
        final PDU pdu = event.getPDU();
        if (pdu != null) {
            final ProcessSession processSession = processSessionFactory.createSession();
            final FlowFile flowFile = createFlowFile(processSession, pdu);
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
    }

    private FlowFile createFlowFile(final ProcessSession processSession, final PDU pdu) {
        FlowFile flowFile = processSession.create();
        flowFile = SNMPUtils.updateFlowFileAttributesWithPduProperties(pdu, flowFile, processSession);
        return flowFile;
    }
}
