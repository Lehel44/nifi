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
import org.apache.nifi.snmp.configuration.TrapConfiguration;
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
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;
import java.util.Optional;

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

    public ResponseEvent sendTrap(TrapConfiguration configuration) {
        try {
            PDU pdu;
            if (target.getVersion() == SnmpConstants.version1) {
                pdu = createV1Pdu(configuration);
            } else {
                pdu = createTrapPdu();
            }
            pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks(configuration.getSysUptime())));
            if (configuration.getTrapOidKey() != null && configuration.getTrapOidValue() != null) {
                pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(configuration.getTrapOidKey())));
                pdu.add(new VariableBinding(configuration.getTrapOidKey(), configuration.getTrapOidValue()));
            }
            Optional.ofNullable(configuration.getManagerAddress())
                    .map(IpAddress::new)
                    .map(ipAddress -> new VariableBinding(SnmpConstants.snmpTrapAddress, ipAddress))
                    .ifPresent(pdu::add);
            return snmp.send(pdu, target);
        } catch (IOException e) {
            final String errorMessage = "Failed to send trap from SNMP agent: " + this.toString();
            LOGGER.error(errorMessage, e);
            throw new ProcessException(errorMessage, e);
        }
    }

    private PDU createTrapPdu() {
        PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);
        return pdu;
    }

    private PDU createV1Pdu(TrapConfiguration configuration) {
        PDUv1 pdu = new PDUv1();
        Optional.ofNullable(configuration.getEnterpriseOid()).map(OID::new).ifPresent(pdu::setEnterprise);
        Optional.ofNullable(configuration.getAgentAddress()).map(IpAddress::new).ifPresent(pdu::setAgentAddress);
        pdu.setGenericTrap(configuration.getGenericTrapType());
        pdu.setSpecificTrap(configuration.getSpecificTrapType());
        return pdu;
    }
}
