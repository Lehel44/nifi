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

import org.apache.nifi.processor.exception.ProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;

public class SNMPTrapSender extends SNMPRequest {

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
            final String errorMessage = "Failed to send trap from SNMP agent: " + this.toString();
            LOGGER.error(errorMessage, e);
            throw new ProcessException(errorMessage, e);
        }
    }
}
