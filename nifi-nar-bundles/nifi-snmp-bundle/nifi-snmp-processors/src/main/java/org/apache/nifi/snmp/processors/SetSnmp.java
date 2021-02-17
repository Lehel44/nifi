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
package org.apache.nifi.snmp.processors;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Performs a SNMP Set operation based on attributes of incoming FlowFile.
 * Upon each invocation of {@link #onTrigger(ProcessContext, ProcessSession)}
 * method, it will inspect attributes of FlowFile and look for attributes with
 * name formatted as "snmp$OID" to set the attribute value to this OID.
 */
@Tags({"snmp", "set", "oid"})
@InputRequirement(Requirement.INPUT_REQUIRED)
@CapabilityDescription("Based on incoming FlowFile attributes, the processor will execute SNMP Set requests." +
        " When founding attributes with name like snmp$<OID>, the processor will atempt to set the value of" +
        " attribute to the corresponding OID given in the attribute name")
public class SetSnmp extends AbstractSnmpProcessor<SnmpSetter> {

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("All FlowFiles that have been successfully used to perform SNMP Set are routed to this relationship")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("All FlowFiles that failed during the SNMP Set care routed to this relationship")
            .build();

    private static final Set<Relationship> RELATIONSHIPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            REL_SUCCESS,
            REL_FAILURE
    )));

    /**
     * @see AbstractSnmpProcessor#onTriggerSnmp(org.apache.nifi.processor.ProcessContext, org.apache.nifi.processor.ProcessSession)
     */
    @Override
    protected void onTriggerSnmp(ProcessContext context, ProcessSession processSession) {
        FlowFile flowFile = processSession.get();
        if (flowFile != null) {
            // Create the PDU object
            PDU pdu;
            if (this.snmpTarget.getVersion() == SnmpConstants.version3) {
                pdu = new ScopedPDU();
            } else {
                pdu = new PDU();
            }
            if (this.addVariables(pdu, flowFile.getAttributes())) {
                pdu.setType(PDU.SET);
                try {
                    ResponseEvent response = this.targetResource.set(pdu);
                    if (response.getResponse() == null) {
                        processSession.transfer(processSession.penalize(flowFile), REL_FAILURE);
                        this.getLogger().error("Set request timed out or parameters are incorrect.");
                        context.yield();
                    } else if (response.getResponse().getErrorStatus() == PDU.noError) {
                        flowFile = SnmpUtils.updateFlowFileAttributesWithPduProperties(pdu, flowFile, processSession);
                        processSession.transfer(flowFile, REL_SUCCESS);
                        processSession.getProvenanceReporter().send(flowFile, this.snmpTarget.getAddress().toString());
                    } else {
                        final String error = response.getResponse().getErrorStatusText();
                        flowFile = SnmpUtils.addAttribute(SnmpUtils.SNMP_PROP_PREFIX + "error", error, flowFile, processSession);
                        processSession.transfer(processSession.penalize(flowFile), REL_FAILURE);
                        this.getLogger().error("Failed while executing SNMP Set [{}] via " + this.targetResource + ". Error = {}", response.getRequest().getVariableBindings(), error);
                    }
                } catch (IOException e) {
                    processSession.transfer(processSession.penalize(flowFile), REL_FAILURE);
                    this.getLogger().error("Failed while executing SNMP Set via " + this.targetResource, e);
                    context.yield();
                }
            } else {
                processSession.transfer(processSession.penalize(flowFile), REL_FAILURE);
                this.getLogger().warn("No attributes found in the FlowFile to perform SNMP Set");
            }
        }
    }

    /**
     * Method to construct {@link VariableBinding} based on {@link FlowFile}
     * attributes in order to update the {@link PDU} that is going to be sent to
     * the SNMP Agent.
     *
     * @param pdu        {@link PDU} to be sent
     * @param attributes {@link FlowFile} attributes
     * @return true if at least one {@link VariableBinding} has been created, false otherwise
     */
    private boolean addVariables(PDU pdu, Map<String, String> attributes) {
        boolean result = false;
        for (Entry<String, String> attributeEntry : attributes.entrySet()) {
            if (attributeEntry.getKey().startsWith(SnmpUtils.SNMP_PROP_PREFIX)) {
                String[] splits = attributeEntry.getKey().split("\\" + SnmpUtils.SNMP_PROP_DELIMITER);
                String snmpPropName = splits[1];
                String snmpPropValue = attributeEntry.getValue();
                if (SnmpUtils.OID_PATTERN.matcher(snmpPropName).matches()) {
                    Variable var;
                    if (splits.length == 2) { // no SMI syntax defined
                        var = new OctetString(snmpPropValue);
                    } else {
                        int smiSyntax = Integer.parseInt(splits[2]);
                        var = this.stringToVariable(snmpPropValue, smiSyntax);
                    }
                    if (var != null) {
                        VariableBinding varBind = new VariableBinding(new OID(snmpPropName), var);
                        pdu.add(varBind);
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Method to create the variable from the attribute value and the given SMI syntax value
     *
     * @param value     attribute value
     * @param smiSyntax attribute SMI Syntax
     * @return variable
     */
    private Variable stringToVariable(String value, int smiSyntax) {
        Variable var = AbstractVariable.createFromSyntax(smiSyntax);
        try {
            if (var instanceof AssignableFromString) {
                ((AssignableFromString) var).setValue(value);
            } else if (var instanceof AssignableFromInteger) {
                ((AssignableFromInteger) var).setValue(Integer.parseInt(value));
            } else if (var instanceof AssignableFromLong) {
                ((AssignableFromLong) var).setValue(Long.parseLong(value));
            } else {
                this.getLogger().error("Unsupported conversion of [" + value + "] to " + var.getSyntaxString());
                var = null;
            }
        } catch (IllegalArgumentException e) {
            this.getLogger().error("Unsupported conversion of [" + value + "] to " + var.getSyntaxString(), e);
            var = null;
        }
        return var;
    }

    /**
     * @see org.apache.nifi.components.AbstractConfigurableComponent#getSupportedPropertyDescriptors()
     */
    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return BASIC_PROPERTIES;
    }

    /**
     * @see org.apache.nifi.processor.AbstractSessionFactoryProcessor#getRelationships()
     */
    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS;
    }

    /**
     * @see AbstractSnmpProcessor#finishBuildingTargetResource(org.apache.nifi.processor.ProcessContext)
     */
    @Override
    protected SnmpSetter finishBuildingTargetResource(ProcessContext context) {
        return new SnmpSetter(snmpContext.getSnmp(), snmpTarget);
    }

}
