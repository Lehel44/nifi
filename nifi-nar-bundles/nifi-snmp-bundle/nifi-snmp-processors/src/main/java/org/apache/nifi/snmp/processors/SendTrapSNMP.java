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

import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Processor;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.snmp.operations.SNMPTrapSender;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.TimeTicks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SendTrapSNMP extends AbstractSNMPProcessor {

    public static final PropertyDescriptor ENTERPRISE_OID = new PropertyDescriptor.Builder()
            .name("snmp-trap-enterprise-oid")
            .displayName("Enterprise OID")
            .description("Enterprise is the vendor identification (OID) for the network management sub-system that generated the trap")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor AGENT_ADDRESS = new PropertyDescriptor.Builder()
            .name("snmp-trap-agent-address")
            .displayName("Agent IP address")
            .description("The sender IP address")
            .required(false)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .build();

    public static final PropertyDescriptor GENERIC_TRAP_TYPE = new PropertyDescriptor.Builder()
            .name("snmp-trap-generic-type")
            .displayName("Generic trap type")
            .description("Generic trap type is an integer in the range of 0 to 6. See Usage for details.")
            .required(true)
            .allowableValues("0", "1", "2", "3", "4", "5", "6")
            .build();

    public static final PropertyDescriptor SPECIFIC_TRAP_TYPE = new PropertyDescriptor.Builder()
            .name("snmp-trap-specific-type")
            .displayName("Specific trap type")
            .description("Specific trap type is a number that further specifies the nature of the event that generated " +
                    "the trap in the case of traps of generic type 6 (enterpriseSpecific). The interpretation of this " +
                    "code is vendor-specific")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();

    public static final PropertyDescriptor TRAP_OID = new PropertyDescriptor.Builder()
            .name("snmp-trap-oid")
            .displayName("Trap OID")
            .description("The authoritative identification of the notification currently being sent")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor TRAP_OID_VALUE = new PropertyDescriptor.Builder()
            .name("snmp-trap-oid-value")
            .displayName("Trap OID Value")
            .description("The value of the trap OID")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor MANAGER_ADDRESS = new PropertyDescriptor.Builder()
            .name("snmp-trap-manager-address")
            .displayName("Manager (destination) IP address")
            .description("The address of the SNMP manager where the trap is being sent to.")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SYSTEM_UPTIME = new PropertyDescriptor.Builder()
            .name("snmp-trap-sysUpTime")
            .displayName("System uptime")
            .description("The system uptime.")
            .required(true)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("All FlowFiles that are received from the SNMP agent are routed to this relationship")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("All FlowFiles that cannot received from the SNMP agent are routed to this relationship")
            .build();

    protected static final List<PropertyDescriptor> PROPERTY_DESCRIPTORS = Collections.unmodifiableList(Arrays.asList(
            SNMP_CLIENT_PORT,
            AGENT_HOST,
            AGENT_PORT,
            SNMP_VERSION,
            SNMP_COMMUNITY,
            SNMP_SECURITY_LEVEL,
            SNMP_SECURITY_NAME,
            SNMP_AUTH_PROTOCOL,
            SNMP_AUTH_PASSWORD,
            SNMP_PRIVACY_PROTOCOL,
            SNMP_PRIVACY_PASSWORD,
            SNMP_RETRIES,
            SNMP_TIMEOUT,
            ENTERPRISE_OID,
            AGENT_ADDRESS,
            GENERIC_TRAP_TYPE,
            SPECIFIC_TRAP_TYPE,
            TRAP_OID,
            MANAGER_ADDRESS,
            TRAP_OID_VALUE,
            SYSTEM_UPTIME
    ));

    private static final Set<Relationship> RELATIONSHIPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            REL_SUCCESS,
            REL_FAILURE
    )));

    private SNMPTrapSender snmpTrapSender;

    @OnScheduled
    @Override
    public void initSnmpClient(ProcessContext context) {
        super.initSnmpClient(context);
        snmpTrapSender = new SNMPTrapSender(snmpClient, target);
    }

    /**
     * Delegate method to supplement
     * {@link #onTrigger(ProcessContext, ProcessSession)}. It is implemented by
     * sub-classes to perform {@link Processor} specific functionality.
     *
     * @param context        instance of {@link ProcessContext}
     * @param processSession instance of {@link ProcessSession}
     * @throws ProcessException Process exception
     */
    @Override
    public void onTrigger(ProcessContext context, ProcessSession processSession) {
        final String enterpriseOIDValue = context.getProperty(ENTERPRISE_OID).getValue();
        final String agentAddressValue = context.getProperty(AGENT_ADDRESS).getValue();
        final int genericTrapTypeValue = Integer.parseInt(context.getProperty(GENERIC_TRAP_TYPE).getValue());
        final int specificTrapTypeValue = Integer.parseInt(context.getProperty(SPECIFIC_TRAP_TYPE).getValue());
        final String trapOIDKey = context.getProperty(TRAP_OID).getValue();
        final String managerAddressValue = context.getProperty(MANAGER_ADDRESS).getValue();
        final String trapOIDValueValue = context.getProperty(TRAP_OID_VALUE).getValue();
        final int sysUpTimeValue = context.getProperty(SYSTEM_UPTIME).asInteger();

        snmpTrapSender.generateTrap(new TimeTicks(sysUpTimeValue), enterpriseOIDValue, agentAddressValue, genericTrapTypeValue, specificTrapTypeValue,
                new OID(trapOIDKey), managerAddressValue, trapOIDValueValue);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTY_DESCRIPTORS;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS;
    }

}
