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

import static org.apache.nifi.snmp.utils.DefinedValues.AES128;
import static org.apache.nifi.snmp.utils.DefinedValues.AES192;
import static org.apache.nifi.snmp.utils.DefinedValues.AES256;
import static org.apache.nifi.snmp.utils.DefinedValues.AUTH_NO_PRIV;
import static org.apache.nifi.snmp.utils.DefinedValues.AUTH_PRIV;
import static org.apache.nifi.snmp.utils.DefinedValues.DES;
import static org.apache.nifi.snmp.utils.DefinedValues.DES3;
import static org.apache.nifi.snmp.utils.DefinedValues.NO_AUTH_NO_PRIV;
import static org.apache.nifi.snmp.utils.DefinedValues.NO_ENCRYPTION;
import static org.apache.nifi.snmp.utils.DefinedValues.SNMP_V1;
import static org.apache.nifi.snmp.utils.DefinedValues.SNMP_V2C;
import static org.apache.nifi.snmp.utils.DefinedValues.SNMP_V3;

/**
 * Base processor that uses SNMP4J client API.
 * (http://www.snmp4j.org/)
 */
abstract class AbstractSNMPProcessor extends AbstractProcessor {

    static {
        LogFactory.setLogFactory(new SLF4JLogFactory());
    }


    public static final PropertyDescriptor AGENT_HOST = new PropertyDescriptor.Builder()
            .name("snmp-hostname")
            .displayName("SNMP agent hostname")
            .description("Network address of SNMP Agent (e.g., localhost)")
            .required(true)
            .defaultValue("localhost")
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
            .defaultValue(SNMP_V1.getValue())
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
            .defaultValue(NO_AUTH_NO_PRIV.getValue())
            .dependsOn(SNMP_VERSION, SNMP_V3)
            .build();

    public static final PropertyDescriptor SNMP_SECURITY_NAME = new PropertyDescriptor.Builder()
            .name("snmp-security-name")
            .displayName("SNMP Security name / user name")
            .description("Security name used for SNMP exchanges")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .dependsOn(SNMP_VERSION, SNMP_V3)
            .build();

    public static final PropertyDescriptor SNMP_PRIVACY_PROTOCOL = new PropertyDescriptor.Builder()
            .name("snmp-private-protocol")
            .displayName("SNMP Privacy Protocol")
            .description("SNMP Privacy Protocol to use")
            .required(true)
            .allowableValues(DES, DES3, AES128, AES192, AES256, NO_ENCRYPTION)
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

    public static final PropertyDescriptor SNMP_AUTH_PROTOCOL = new PropertyDescriptor.Builder()
            .name("snmp-authentication-protocol")
            .displayName("SNMP Authentication Protocol")
            .description("SNMP Authentication Protocol to use")
            .required(true)
            .allowableValues("MD5", "SHA", "")
            .defaultValue("")
            .dependsOn(SNMP_SECURITY_LEVEL, AUTH_NO_PRIV, AUTH_PRIV)
            .build();

    public static final PropertyDescriptor SNMP_AUTH_PASSWORD = new PropertyDescriptor.Builder()
            .name("snmp-authentication-passphrase")
            .displayName("SNMP Authentication pass phrase")
            .description("Pass phrase used for SNMP authentication protocol")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .sensitive(true)
            .dependsOn(SNMP_SECURITY_LEVEL, AUTH_NO_PRIV, AUTH_PRIV)
            .build();

    public static final PropertyDescriptor SNMP_CLIENT_PORT = new PropertyDescriptor.Builder()
            .name("snmp-client-port")
            .displayName("SNMP client port")
            .description("The processor runs an SNMP manager on localhost. The port however can be specified")
            .required(false)
            .defaultValue("0")
            .addValidator(StandardValidators.PORT_VALIDATOR)
            .build();

    public static final PropertyDescriptor SNMP_RETRIES = new PropertyDescriptor.Builder()
            .name("snmp-retries")
            .displayName("Number of retries")
            .description("Set the number of retries when requesting the SNMP Agent")
            .required(false)
            .defaultValue("0")
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)
            .build();

    public static final PropertyDescriptor SNMP_TIMEOUT = new PropertyDescriptor.Builder()
            .name("snmp-timeout")
            .displayName("Timeout (ms)")
            .description("Set the timeout (in milliseconds) when requesting the SNMP Agent")
            .required(false)
            .defaultValue("5000")
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)
            .build();


    protected volatile SNMPRequestHandler snmpRequestHandler;

    public void initSnmpClient(ProcessContext context) {
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

        final String clientPort = context.getProperty(SNMP_CLIENT_PORT).toString();
        snmpRequestHandler = SNMPRequestHandlerFactory.createStandardRequestHandler(configuration, clientPort);
    }

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
