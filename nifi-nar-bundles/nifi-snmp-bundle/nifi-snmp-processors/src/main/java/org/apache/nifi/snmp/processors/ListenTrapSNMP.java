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
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.snmp.configuration.TargetConfiguration;
import org.apache.nifi.snmp.configuration.TargetConfigurationBuilder;
import org.apache.nifi.snmp.configuration.TrapConfiguration;
import org.apache.nifi.snmp.context.SNMPClientFactory;
import org.apache.nifi.snmp.operations.SNMPTrapReceiver;
import org.apache.nifi.snmp.utils.SNMPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Receiving data from configured SNMP agent which, upon each invocation of
 * {@link #onTrigger(ProcessContext, ProcessSession)} method, will construct a
 * {@link FlowFile} containing in its properties the information retrieved.
 * The output {@link FlowFile} won't have any content.
 */
@Tags({"snmp", "listen", "trap"})
@InputRequirement(InputRequirement.Requirement.INPUT_FORBIDDEN)
@CapabilityDescription("Receives information from SNMP Agent and outputs a FlowFile with information in attributes and without any content")
@WritesAttribute(attribute = SNMPUtils.SNMP_PROP_PREFIX + "*", description = "Attributes retrieved from the SNMP response. It may include:"
                + " snmp$errorIndex, snmp$errorStatus, snmp$errorStatusText, snmp$nonRepeaters, snmp$requestID, snmp$type, snmp$variableBindings")
public class ListenTrapSNMP extends AbstractSNMPProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenTrapSNMP.class);

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
            SNMP_TIMEOUT
    ));

    private static final Set<Relationship> RELATIONSHIPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            REL_SUCCESS,
            REL_FAILURE
    )));

    private volatile SNMPTrapReceiver trapReceiver;

    @OnScheduled
    @Override
    public void initSnmpClient(ProcessContext context) {
        final String clientPort = context.getProperty(SNMP_CLIENT_PORT).getValue();

        final TargetConfiguration configuration = new TargetConfigurationBuilder()
                .setVersion(context.getProperty(SNMP_VERSION).getValue())
                .setAuthProtocol(context.getProperty(SNMP_AUTH_PROTOCOL).getValue())
                .setAuthPassword(context.getProperty(SNMP_AUTH_PASSWORD).getValue())
                .setPrivacyProtocol(context.getProperty(SNMP_PRIVACY_PROTOCOL).getValue())
                .setPrivacyPassword(context.getProperty(SNMP_PRIVACY_PASSWORD).getValue())
                .setSecurityName(context.getProperty(SNMP_SECURITY_NAME).getValue())
                .setSecurityLevel(context.getProperty(SNMP_SECURITY_LEVEL).getValue())
                .createSecurityConfiguration();

        snmpClient = SNMPClientFactory.createSnmpClient(configuration, clientPort);
        try {
            snmpClient.listen();
        } catch (IOException e) {
            final String errorMessage = "Could not initialize SNMP client.";
            LOGGER.error(errorMessage, e);
            throw new ProcessException(errorMessage, e);
        }
    }

    @OnStopped
    public void stopSnmpClient(ProcessContext context) {
        trapReceiver = null;
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) {
        if (Objects.isNull(trapReceiver)) {
            trapReceiver = new SNMPTrapReceiver(snmpClient, context, session, getLogger());
        }
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
