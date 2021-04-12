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
import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.snmp.configuration.TargetConfiguration;
import org.apache.nifi.snmp.configuration.TargetConfigurationBuilder;
import org.apache.nifi.snmp.logging.SLF4JLogFactory;
import org.apache.nifi.snmp.operations.SNMPRequestHandler;
import org.apache.nifi.snmp.operations.SNMPRequestHandlerFactory;
import org.apache.nifi.snmp.utils.SNMPUtils;
import org.snmp4j.log.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base processor that uses SNMP4J client API.
 * (http://www.snmp4j.org/)
 */
abstract class AbstractSNMPProcessor extends AbstractProcessor {

    protected static final String DEFAULT_PORT = "0";

    static {
        LogFactory.setLogFactory(new SLF4JLogFactory());
    }

    private static final AllowableValue SNMP_V1 = new AllowableValue("SNMPv1", "v1",
            "SNMP version 1");

    private static final AllowableValue SNMP_V2C = new AllowableValue("SNMPv2c", "v2c",
            "SNMP version 2c");

    private static final AllowableValue SNMP_V3 = new AllowableValue("SNMPv3", "v3",
            "SNMP version 3 with improved security");

    private static final AllowableValue NO_AUTH_NO_PRIV = new AllowableValue("noAuthNoPriv", "No authentication or encryption",
            "No authentication or encryption");

    private static final AllowableValue AUTH_NO_PRIV = new AllowableValue("authNoPriv", "Authentication without encryption",
            "Authentication without encryption");

    private static final AllowableValue AUTH_PRIV = new AllowableValue("authPriv", "Authentication and encryption",
            "Authentication and encryption");

    public static final PropertyDescriptor SNMP_CLIENT_PORT = new PropertyDescriptor.Builder()
            .name("snmp-client-port")
            .displayName("SNMP client port")
            .description("The processor runs an SNMP client on localhost. The port however can be specified")
            .required(false)
            .defaultValue("0")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor AGENT_HOST = new PropertyDescriptor.Builder()
            .name("snmp-hostname")
            .displayName("SNMP agent hostname")
            .description("Network address of SNMP Agent (e.g., localhost)")
            .required(true)
            .defaultValue("127.0.0.1")
            .addValidator(StandardValidators.NETWORK_ADDRESS_VALIDATOR)
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
            .allowableValues(SNMP_V1, SNMP_V2C, SNMP_V3)
            .defaultValue("SNMPv1")
            .build();

    public static final PropertyDescriptor SNMP_COMMUNITY = new PropertyDescriptor.Builder()
            .name("snmp-community")
            .displayName("SNMP Community (v1 & v2c)")
            .description("SNMP Community to use (e.g., public) for authentication")
            .required(true)
            .defaultValue("public")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .dependsOn(SNMP_VERSION, SNMP_V1, SNMP_V2C)
            .build();

    public static final PropertyDescriptor SNMP_SECURITY_LEVEL = new PropertyDescriptor.Builder()
            .name("snmp-security-level")
            .displayName("SNMP Security Level")
            .description("SNMP Security Level to use")
            .required(true)
            .allowableValues(NO_AUTH_NO_PRIV, AUTH_NO_PRIV, AUTH_PRIV)
            .defaultValue("authPriv")
            .build();

    public static final PropertyDescriptor SNMP_SECURITY_NAME = new PropertyDescriptor.Builder()
            .name("snmp-security-name")
            .displayName("SNMP Security name / user name")
            .description("Security name used for SNMP exchanges")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .dependsOn(SNMP_VERSION, SNMP_V3)
            .build();

    public static final PropertyDescriptor SNMP_AUTH_PROTOCOL = new PropertyDescriptor.Builder()
            .name("snmp-authentication-protocol")
            .displayName("SNMP Authentication Protocol")
            .description("SNMP Authentication Protocol to use")
            .required(false)
            .allowableValues("MD5", "SHA", "")
            .defaultValue("")
            .dependsOn(SNMP_SECURITY_LEVEL, AUTH_NO_PRIV, AUTH_PRIV)
            .build();

    public static final PropertyDescriptor SNMP_AUTH_PASSWORD = new PropertyDescriptor.Builder()
            .name("snmp-authentication-passphrase")
            .displayName("SNMP Authentication pass phrase")
            .description("Pass phrase used for SNMP authentication protocol")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .sensitive(true)
            .dependsOn(SNMP_SECURITY_LEVEL, AUTH_NO_PRIV, AUTH_PRIV)
            .build();

    public static final PropertyDescriptor SNMP_PRIVACY_PROTOCOL = new PropertyDescriptor.Builder()
            .name("snmp-private-protocol")
            .displayName("SNMP Privacy Protocol")
            .description("SNMP Privacy Protocol to use")
            // TODO check all
            .required(true)
            .allowableValues("DES", "3DES", "AES128", "AES192", "AES256", "")
            .defaultValue("")
            .dependsOn(SNMP_SECURITY_LEVEL, AUTH_PRIV)
            .build();

    public static final PropertyDescriptor SNMP_PRIVACY_PASSWORD = new PropertyDescriptor.Builder()
            .name("snmp-private-protocol-passphrase")
            .displayName("SNMP Privacy protocol pass phrase")
            .description("Pass phrase used for SNMP privacy protocol")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .sensitive(true)
            .dependsOn(SNMP_SECURITY_LEVEL, AUTH_PRIV)
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

    protected volatile SNMPRequestHandler snmpRequestHandler;

    public void initSnmpClient(ProcessContext context) {
        // TODO: abstract method override in trap, separate descrption for processors, two diff property
        final int version = SNMPUtils.getVersion(context.getProperty(SNMP_VERSION).getValue());

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

        snmpRequestHandler = SNMPRequestHandlerFactory.createStandardRequestHandler(configuration, getClientPort());
    }

    protected abstract String getClientPort();

    /**
     * Closes the current SNMP mapping.
     */
    @OnStopped
    public void close() {
        if (snmpRequestHandler != null) {
            snmpRequestHandler.close();
        }
    }
}
