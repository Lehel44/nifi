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
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Processor;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.snmp.configuration.BasicConfiguration;
import org.apache.nifi.snmp.configuration.SecurityConfiguration;
import org.apache.nifi.snmp.context.SnmpContext;
import org.apache.nifi.snmp.validators.OidValidator;
import org.snmp4j.AbstractTarget;
import org.snmp4j.TransportMapping;

import java.util.*;

/**
 * Base processor that uses SNMP4J client API.
 * (http://www.snmp4j.org/)
 *
 * @param <T> the type of {@link SnmpWorker}. Please see {@link SnmpSetter}
 *            and {@link SnmpGetter}
 */
abstract class AbstractSnmpProcessor<T extends SnmpWorker> extends AbstractProcessor {

    // Property to define the host of the SNMP agent.
    public static final PropertyDescriptor HOST = new PropertyDescriptor.Builder()
            .name("snmp-hostname")
            .displayName("Hostname")
            .description("Network address of SNMP Agent (e.g., localhost)")
            .required(true)
            .defaultValue("localhost")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    // Property to define the port of the SNMP agent.
    public static final PropertyDescriptor PORT = new PropertyDescriptor.Builder()
            .name("snmp-port")
            .displayName("Port")
            .description("Numeric value identifying the port of SNMP Agent (e.g., 161)")
            .required(true)
            .defaultValue("161")
            .addValidator(StandardValidators.PORT_VALIDATOR)
            .build();

    // Property to define SNMP version.
    public static final PropertyDescriptor SNMP_VERSION = new PropertyDescriptor.Builder()
            .name("snmp-version")
            .displayName("SNMP Version")
            .description("SNMP Version to use")
            .required(true)
            .allowableValues("SNMPv1", "SNMPv2c", "SNMPv3")
            .defaultValue("SNMPv1")
            .build();

    // Property to define SNMP community.
    public static final PropertyDescriptor SNMP_COMMUNITY = new PropertyDescriptor.Builder()
            .name("snmp-community")
            .displayName("SNMP Community (v1 & v2c)")
            .description("SNMP Community to use (e.g., public). The SNMP Community string" +
                    " is like a user id or password that allows access to a router's or " +
                    "other device's statistics.")
            .required(false)
            .defaultValue("public")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    // Property to define SNMP security level.
    public static final PropertyDescriptor SNMP_SECURITY_LEVEL = new PropertyDescriptor.Builder()
            .name("snmp-security-level")
            .displayName("SNMP Security Level")
            .description("SNMP Security Level to use")
            .required(true)
            .allowableValues("noAuthNoPriv", "authNoPriv", "authPriv")
            .defaultValue("authPriv")
            .build();

    // Property to define SNMP security name.
    public static final PropertyDescriptor SNMP_SECURITY_NAME = new PropertyDescriptor.Builder()
            .name("snmp-security-name")
            .displayName("SNMP Security name / user name")
            .description("Security name used for SNMP exchanges")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    // Property to define SNMP authentication protocol.
    public static final PropertyDescriptor SNMP_AUTH_PROTOCOL = new PropertyDescriptor.Builder()
            .name("snmp-authentication-protocol")
            .displayName("SNMP Authentication Protocol")
            .description("SNMP Authentication Protocol to use")
            .required(true)
            .allowableValues("MD5", "SHA", "")
            .defaultValue("")
            .build();

    // Property to define SNMP authentication password.
    public static final PropertyDescriptor SNMP_AUTH_PASSWORD = new PropertyDescriptor.Builder()
            .name("snmp-authentication-passphrase")
            .displayName("SNMP Authentication pass phrase")
            .description("Pass phrase used for SNMP authentication protocol")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .sensitive(true)
            .build();

    // Property to define SNMP private protocol.
    public static final PropertyDescriptor SNMP_PRIVACY_PROTOCOL = new PropertyDescriptor.Builder()
            .name("snmp-privacy-protocol")
            .displayName("SNMP Privacy Protocol")
            .description("SNMP Privacy Protocol to use")
            .required(true)
            .allowableValues("DES", "3DES", "AES128", "AES192", "AES256", "")
            .defaultValue("")
            .build();

    // Property to define SNMP private password.
    public static final PropertyDescriptor SNMP_PRIVACY_PASSWORD = new PropertyDescriptor.Builder()
            .name("snmp-privacy-protocol-passphrase")
            .displayName("SNMP Privacy protocol pass phrase")
            .description("Pass phrase used for SNMP privacy protocol")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .sensitive(true)
            .build();

    // Property to define the number of SNMP retries when requesting the SNMP Agent.
    public static final PropertyDescriptor SNMP_RETRIES = new PropertyDescriptor.Builder()
            .name("snmp-retries")
            .displayName("Number of retries")
            .description("Set the number of retries when requesting the SNMP Agent")
            .required(true)
            .defaultValue("0")
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();

    // Property to define the timeout when requesting the SNMP Agent.
    public static final PropertyDescriptor SNMP_TIMEOUT = new PropertyDescriptor.Builder()
            .name("snmp-timeout")
            .displayName("Timeout (ms)")
            .description("Set the timeout (in milliseconds) when requesting the SNMP Agent")
            .required(true)
            .defaultValue("5000")
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();

    protected static final List<PropertyDescriptor> BASIC_PROPERTIES = Collections.unmodifiableList(Arrays.asList(
            HOST,
            PORT,
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

    protected volatile AbstractTarget snmpTarget;
    protected volatile TransportMapping transportMapping;
    protected volatile SnmpContext snmpContext;
    protected volatile T targetResource;

    /**
     * Builds target resource upon first invocation and delegates to the
     * implementation of {@link #onTriggerSnmp(ProcessContext, ProcessSession)} method for
     * further processing.
     */
    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) {

        final BasicConfiguration basicConfiguration = new BasicConfiguration(
                context.getProperty(HOST).getValue(),
                context.getProperty(PORT).asInteger(),
                context.getProperty(SNMP_RETRIES).asInteger(),
                context.getProperty(SNMP_TIMEOUT).asInteger()
        );

        final SecurityConfiguration securityConfiguration = new SecurityConfiguration(
                context.getProperty(SNMP_VERSION).getValue(),
                context.getProperty(SNMP_SECURITY_NAME).getValue(),
                context.getProperty(SNMP_AUTH_PROTOCOL).getValue(),
                context.getProperty(SNMP_AUTH_PASSWORD).getValue(),
                context.getProperty(SNMP_PRIVACY_PROTOCOL).getValue(),
                context.getProperty(SNMP_PRIVACY_PASSWORD).getValue(),
                context.getProperty(SNMP_SECURITY_LEVEL).getValue(),
                context.getProperty(SNMP_COMMUNITY).getValue()
        );

        snmpContext = SnmpContext.newInstance();
        snmpContext.init(basicConfiguration, securityConfiguration);
        onTriggerSnmp(context, session);
    }

    /**
     * Closes the current SNMP mapping.
     */
    @OnStopped
    public void close() {
        try {
            if (targetResource != null) {
                targetResource.close();
            }
        } catch (Exception e) {
            getLogger().error("Failure while closing target resource {}", targetResource, e);
        }
        targetResource = null;

        snmpContext.close();
    }

    /**
     * @see org.apache.nifi.components.AbstractConfigurableComponent#customValidate(org.apache.nifi.components.ValidationContext)
     */
    @Override
    protected Collection<ValidationResult> customValidate(ValidationContext validationContext) {
        final List<ValidationResult> problems = new ArrayList<>(super.customValidate(validationContext));

        final SecurityConfiguration securityConfiguration = new SecurityConfiguration(
                validationContext.getProperty(SNMP_VERSION).getValue(),
                validationContext.getProperty(SNMP_SECURITY_NAME).getValue(),
                validationContext.getProperty(SNMP_AUTH_PROTOCOL).getValue(),
                validationContext.getProperty(SNMP_AUTH_PASSWORD).getValue(),
                validationContext.getProperty(SNMP_PRIVACY_PROTOCOL).getValue(),
                validationContext.getProperty(SNMP_PRIVACY_PASSWORD).getValue(),
                validationContext.getProperty(SNMP_SECURITY_LEVEL).getValue(),
                validationContext.getProperty(SNMP_COMMUNITY).getValue()
        );

        OidValidator oidValidator = new OidValidator(securityConfiguration, problems);
        return oidValidator.validate();
    }

    /**
     * Delegate method to supplement
     * {@link #onTrigger(ProcessContext, ProcessSession)}. It is implemented by
     * sub-classes to perform {@link Processor} specific functionality.
     *
     * @param context instance of {@link ProcessContext}
     * @param session instance of {@link ProcessSession}
     * @throws ProcessException Process exception
     */
    protected abstract void onTriggerSnmp(ProcessContext context, ProcessSession session);

    /**
     * Delegate method to supplement building of target {@link SnmpWorker} (see
     * {@link SnmpSetter} or {@link SnmpGetter}) and is implemented by
     * sub-classes.
     *
     * @param context instance of {@link ProcessContext}
     * @return new instance of {@link SnmpWorker}
     */
    protected abstract T finishBuildingTargetResource(ProcessContext context);
}
