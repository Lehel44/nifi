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
import org.apache.nifi.snmp.exception.CloseSNMPClientException;
import org.apache.nifi.snmp.utils.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class StandardSNMPRequestHandler implements SNMPRequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardSNMPRequestHandler.class);
    private final Snmp snmpClient;
    private final Target target;

    @VisibleForTesting
    StandardSNMPRequestHandler(Snmp snmpClient, Target target) {
        this.snmpClient = snmpClient;
        this.target = target;
    }

    /**
     * Construct the PDU to perform the SNMP Get request and returns
     * the result in order to create the flow file.
     *
     * @return {@link ResponseEvent}
     */
    public ResponseEvent get(OID oid) {
        try {
            final PDUFactory pduFactory = new DefaultPDUFactory(PDU.GET);
            final PDU pdu = pduFactory.createPDU(target);
            pdu.add(new VariableBinding(oid));
            pdu.setType(PDU.GET);
            return snmpClient.get(pdu, target);
        } catch (IOException e) {
            LOGGER.error("Failed to get information from SNMP agent; {}", this, e);
            throw new ProcessException(e);
        }
    }

    /**
     * Perform a SNMP walk and returns the list of {@link TreeEvent}
     *
     * @return the list of {@link TreeEvent}
     */
    public List<TreeEvent> walk(OID oid) {
        TreeUtils treeUtils = new TreeUtils(snmpClient, new DefaultPDUFactory());
        return treeUtils.getSubtree(target, oid);
    }

    /**
     * Executes the SNMP set request and returns the response.
     *
     * @param pdu PDU to send
     * @return Response event
     * @throws IOException IO Exception
     */
    public ResponseEvent set(PDU pdu) throws IOException {
        return snmpClient.set(pdu, target);
    }


    public void sendTrap(TrapConfiguration configuration) {
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
            snmpClient.send(pdu, target);
        } catch (IOException e) {
            final String errorMessage = "Failed to send trap from SNMP agent: " + this.toString();
            LOGGER.error(errorMessage, e);
            throw new ProcessException(errorMessage, e);
        }
    }


    public void close() {
        try {
            snmpClient.close();
        } catch (IOException e) {
            final String errorMessage = "Could not close SNMP client.";
            LOGGER.error(errorMessage, e);
            throw new CloseSNMPClientException(errorMessage);
        }
    }

    public Target getTarget() {
        return target;
    }

    public Snmp getSnmpClient() {
        return snmpClient;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + snmpClient.toString();
    }

    private PDU createTrapPdu() {
        final PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);
        return pdu;
    }

    private PDU createV1Pdu(final TrapConfiguration configuration) {
        final PDUv1 pdu = new PDUv1();
        Optional.ofNullable(configuration.getEnterpriseOid()).map(OID::new).ifPresent(pdu::setEnterprise);
        Optional.ofNullable(configuration.getAgentAddress()).map(IpAddress::new).ifPresent(pdu::setAgentAddress);
        pdu.setGenericTrap(configuration.getGenericTrapType());
        pdu.setSpecificTrap(configuration.getSpecificTrapType());
        return pdu;
    }
}
