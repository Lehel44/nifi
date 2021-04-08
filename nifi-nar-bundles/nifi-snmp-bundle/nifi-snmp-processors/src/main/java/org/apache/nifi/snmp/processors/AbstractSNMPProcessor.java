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

import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.snmp.configuration.TargetConfiguration;
import org.apache.nifi.snmp.configuration.TargetConfigurationBuilder;
import org.apache.nifi.snmp.logging.SLF4JLogFactory;
import org.apache.nifi.snmp.operations.StandardSNMPRequestHandler;
import org.apache.nifi.snmp.utils.SNMPVersion;
import org.apache.nifi.snmp.validators.SNMPValidator;
import org.snmp4j.log.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base processor that uses SNMP4J client API.
 * (http://www.snmp4j.org/)
 */
abstract class AbstractSNMPProcessor extends AbstractProcessor {

    static {
        LogFactory.setLogFactory(new SLF4JLogFactory());
    }

    // TODO-8325: Rename these back to original. Check why CLIENT PORT needed.
    // Shouldnt be required, rename it
    public static final PropertyDescriptor SNMP_CLIENT_PORT = new PropertyDescriptor.Builder()
            .name("snmp-client-port")
            .displayName("SNMP client port")
            .description("The processor runs an SNMP client on localhost. The port however can be specified")
            .required(true)
            .defaultValue("0")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor AGENT_HOST = new PropertyDescriptor.Builder()
            .name("snmp-hostname")
            .displayName("SNMP agent hostname")
            .description("Network address of SNMP Agent (e.g., localhost)")
            .required(true)
            .defaultValue("localhost")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor AGENT_PORT = new PropertyDescriptor.Builder()
            .name("snmp-port")
            .displayName("SNMP agent port")
            .description("Numeric value identifying the port of SNMP Agent (e.g., 161)")
            .required(true)
            .defaultValue("161")
            .addValidator(StandardValidators.PORT_VALIDATOR)
            .build();

    public static final PropertyDescriptor SNMP_VERSION = new PropertyDescriptor.Builder()
            .name("snmp-version")
            .displayName("SNMP Version")
            .description("SNMP Version to use")
            .required(true)
            .allowableValues("SNMPv1", "SNMPv2c", "SNMPv3")
            .defaultValue("SNMPv1")
            .build();

    public static final PropertyDescriptor SNMP_COMMUNITY = new PropertyDescriptor.Builder()
            .name("snmp-community")
            .displayName("SNMP Community (v1 & v2c)")
            .description("SNMP Community to use (e.g., public) for authentication")
            .required(false)
            .defaultValue("public")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .dependsOn(SNMP_VERSION, "SNMPv1", "SNMPv2c")
            .build();

    public static final PropertyDescriptor SNMP_SECURITY_LEVEL = new PropertyDescriptor.Builder()
            .name("snmp-security-level")
            .displayName("SNMP Security Level")
            .description("SNMP Security Level to use")
            .required(true)
            .allowableValues("noAuthNoPriv", "authNoPriv", "authPriv")
            .defaultValue("authPriv")
            .build();

    public static final PropertyDescriptor SNMP_SECURITY_NAME = new PropertyDescriptor.Builder()
            .name("snmp-security-name")
            .displayName("SNMP Security name / user name")
            .description("Security name used for SNMP exchanges")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor SNMP_AUTH_PROTOCOL = new PropertyDescriptor.Builder()
            .name("snmp-authentication-protocol")
            .displayName("SNMP Authentication Protocol")
            .description("SNMP Authentication Protocol to use")
            .required(false)
            .allowableValues("MD5", "SHA", "")
            .defaultValue("")
            .build();

    public static final PropertyDescriptor SNMP_AUTH_PASSWORD = new PropertyDescriptor.Builder()
            .name("snmp-authentication-passphrase")
            .displayName("SNMP Authentication pass phrase")
            .description("Pass phrase used for SNMP authentication protocol")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .sensitive(true)
            .build();

    public static final PropertyDescriptor SNMP_PRIVACY_PROTOCOL = new PropertyDescriptor.Builder()
            .name("snmp-private-protocol")
            .displayName("SNMP Privacy Protocol")
            .description("SNMP Privacy Protocol to use")
            .required(false)
            .allowableValues("DES", "3DES", "AES128", "AES192", "AES256", "")
            .defaultValue("")
            .build();

    public static final PropertyDescriptor SNMP_PRIVACY_PASSWORD = new PropertyDescriptor.Builder()
            .name("snmp-private-protocol-passphrase")
            .displayName("SNMP Privacy protocol pass phrase")
            .description("Pass phrase used for SNMP privacy protocol")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .sensitive(true)
            .build();

    public static final PropertyDescriptor SNMP_RETRIES = new PropertyDescriptor.Builder()
            .name("snmp-retries")
            .displayName("Number of retries")
            .description("Set the number of retries when requesting the SNMP Agent")
            .required(true)
            .defaultValue("0")
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();

    public static final PropertyDescriptor SNMP_TIMEOUT = new PropertyDescriptor.Builder()
            .name("snmp-timeout")
            .displayName("Timeout (ms)")
            .description("Set the timeout (in milliseconds) when requesting the SNMP Agent")
            .required(true)
            .defaultValue("5000")
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();

    protected volatile StandardSNMPRequestHandler standardSnmpRequestHandler;

    public void initSnmpClient(ProcessContext context) {
        // TODO: abstract method override in trap, separate descrption for processors, two diff property
        final String clientPort = context.getProperty(SNMP_CLIENT_PORT).getValue();

        SNMPVersion version = SNMPVersion.getEnumByDisplayName(context.getProperty(SNMP_VERSION).getValue());

        final TargetConfiguration configuration = new TargetConfigurationBuilder()
                .setAgentHost(context.getProperty(AGENT_HOST).getValue())
                .setAgentPort(context.getProperty(AGENT_PORT).toString())
                .setRetries(context.getProperty(SNMP_RETRIES).asInteger())
                .setTimeout(context.getProperty(SNMP_TIMEOUT).asInteger())
                .setVersion(version)
                .setAuthProtocol(context.getProperty(SNMP_AUTH_PROTOCOL).getValue())
                .setAuthPassword(context.getProperty(SNMP_AUTH_PASSWORD).getValue())
                .setPrivacyProtocol(context.getProperty(SNMP_PRIVACY_PROTOCOL).getValue())
                .setPrivacyPassword(context.getProperty(SNMP_PRIVACY_PASSWORD).getValue())
                .setSecurityName(context.getProperty(SNMP_SECURITY_NAME).getValue())
                .setSecurityLevel(context.getProperty(SNMP_SECURITY_LEVEL).getValue())
                .setCommunityString(context.getProperty(SNMP_COMMUNITY).getValue())
                .build();

        standardSnmpRequestHandler = new StandardSNMPRequestHandler(configuration, clientPort);
    }

    /**
     * Closes the current SNMP mapping.
     */
    @OnStopped
    public void close() {
        if (standardSnmpRequestHandler != null) {
            standardSnmpRequestHandler.close();
        }
    }

    /**
     * @see org.apache.nifi.components.AbstractConfigurableComponent#customValidate(org.apache.nifi.components.ValidationContext)
     */
    @Override
    protected Collection<ValidationResult> customValidate(ValidationContext validationContext) {
        final List<ValidationResult> problems = new ArrayList<>(super.customValidate(validationContext));

        SNMPVersion version = SNMPVersion.getEnumByDisplayName(validationContext.getProperty(SNMP_VERSION).getValue());

        final TargetConfiguration targetConfiguration = new TargetConfigurationBuilder()
                .setAgentHost(validationContext.getProperty(AGENT_HOST).getValue())
                .setAgentPort(validationContext.getProperty(AGENT_PORT).toString())
                .setRetries(validationContext.getProperty(SNMP_RETRIES).asInteger())
                .setTimeout(validationContext.getProperty(SNMP_TIMEOUT).asInteger())
                .setVersion(version)
                .setAuthProtocol(validationContext.getProperty(SNMP_AUTH_PROTOCOL).getValue())
                .setAuthPassword(validationContext.getProperty(SNMP_AUTH_PASSWORD).getValue())
                .setPrivacyProtocol(validationContext.getProperty(SNMP_PRIVACY_PROTOCOL).getValue())
                .setPrivacyPassword(validationContext.getProperty(SNMP_PRIVACY_PASSWORD).getValue())
                .setSecurityName(validationContext.getProperty(SNMP_SECURITY_NAME).getValue())
                .setSecurityLevel(validationContext.getProperty(SNMP_SECURITY_LEVEL).getValue())
                .setCommunityString(validationContext.getProperty(SNMP_COMMUNITY).getValue())
                .build();

        SNMPValidator snmpValidator = new SNMPValidator(targetConfiguration, problems);
        return snmpValidator.validate();
    }


}
