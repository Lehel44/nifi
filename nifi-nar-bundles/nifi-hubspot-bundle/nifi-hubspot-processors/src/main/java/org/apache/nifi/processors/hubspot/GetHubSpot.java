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
package org.apache.nifi.processors.hubspot;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.PrimaryNodeOnly;
import org.apache.nifi.annotation.behavior.Stateful;
import org.apache.nifi.annotation.behavior.TriggerSerially;
import org.apache.nifi.annotation.behavior.TriggerWhenEmpty;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.state.Scope;
import org.apache.nifi.components.state.StateMap;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.hubspot.model.IncrementalLoadingParameter;
import org.apache.nifi.processors.hubspot.model.ResourceDirectory;
import org.apache.nifi.processors.hubspot.model.ResourceType;
import org.apache.nifi.processors.hubspot.model.HubSpotResource;
import org.apache.nifi.processors.hubspot.rest.HubSpotRestService;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@PrimaryNodeOnly
@TriggerSerially
@TriggerWhenEmpty
@InputRequirement(Requirement.INPUT_FORBIDDEN)
@Tags({"hubspot"})
@Stateful(scopes = Scope.CLUSTER, description = "For some resources the processors support incremental loading. The list of the resources with the supported parameters" +
        "can be found in additional details. State is stored across the cluster so that this Processor can be run on Primary Node only and if a new Primary Node is" +
        " selected, the new node can pick up where the previous node left off, without duplicating the data.")
@CapabilityDescription("Retrieves object from a custom Shopify store.")
public class GetHubSpot extends AbstractProcessor {

    static final PropertyDescriptor API_URL = new PropertyDescriptor.Builder()
            .name("hubspot-url")
            .displayName("URL")
            .description("")
            .required(true)
            .addValidator(StandardValidators.URL_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    static final PropertyDescriptor ACCESS_TOKEN = new PropertyDescriptor.Builder()
            .name("hubspot-admin-api-access-token")
            .displayName("Admin API Access Token")
            .description("")
            .required(true)
            .sensitive(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    static final PropertyDescriptor API_VERSION = new PropertyDescriptor.Builder()
            .name("hubspot-api-version")
            .displayName("API Version")
            .description("")
            .required(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .defaultValue("2022-07")
            .build();

    static final PropertyDescriptor RESOURCE_TYPE = new PropertyDescriptor.Builder()
            .name("hubspot-resource-type")
            .displayName("Resource Type")
            .description("HubSpot resource type")
            .required(true)
            .allowableValues(ResourceDirectory.getCategories())
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .build();

    static final PropertyDescriptor READ_TIMEOUT = new PropertyDescriptor.Builder()
            .name("read-timeout")
            .displayName("Read Timeout")
            .description("Maximum time allowed for reading a response from the Salesforce REST API")
            .required(true)
            .defaultValue("15 s")
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("For FlowFiles created as a result of a successful query.")
            .build();

    private static final Map<ResourceType, PropertyDescriptor> propertyMap = new EnumMap<>(ResourceType.class);
    private static final List<PropertyDescriptor> PROPERTY_DESCRIPTORS = createPropertyDescriptors();

    private static List<PropertyDescriptor> createPropertyDescriptors() {
        final List<PropertyDescriptor> resourceDescriptors = Arrays.stream(ResourceType.values())
                .map(resourceType -> {
                    final PropertyDescriptor resourceDescriptor = new PropertyDescriptor.Builder()
                            .name(resourceType.getValue())
                            .displayName(resourceType.getDisplayName())
                            .description(resourceType.getDescription())
                            .required(true)
                            .dependsOn(RESOURCE_TYPE, resourceType.getValue())
                            .allowableValues(ResourceDirectory.getResourcesAsAllowableValues(resourceType))
                            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
                            .build();
                    propertyMap.put(resourceType, resourceDescriptor);
                    return resourceDescriptor;
                })
                .collect(Collectors.toList());
        final List<PropertyDescriptor> propertyDescriptors = new ArrayList<>(Arrays.asList(
                API_URL,
                ACCESS_TOKEN,
                API_VERSION,
                RESOURCE_TYPE
        ));
        propertyDescriptors.addAll(resourceDescriptors);
        propertyDescriptors.add(READ_TIMEOUT);
        return Collections.unmodifiableList(propertyDescriptors);
    }

    private volatile HubSpotRestService hubSpotRestService;
    private volatile HubSpotResource hubSpotResource;
    private volatile String resourceName;

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        final String apiVersion = context.getProperty(API_VERSION).getValue();
        final String baseUrl = context.getProperty(API_URL).getValue();
        final String accessToken = context.getProperty(ACCESS_TOKEN).getValue();

        final String category = context.getProperty(RESOURCE_TYPE).getValue();
        final ResourceType resourceType = ResourceType.valueOf(category);
        resourceName = context.getProperty(propertyMap.get(resourceType)).getValue();

        hubSpotResource = ResourceDirectory.getResourceTypeDto(resourceType, resourceName);

        hubSpotRestService = new HubSpotRestService(
                apiVersion,
                baseUrl,
                accessToken,
                resourceName,
                hubSpotResource.getUri(),
                hubSpotResource.getIncrementalLoadingParameter(),
                context.getProperty(READ_TIMEOUT).evaluateAttributeExpressions().asTimePeriod(TimeUnit.MILLISECONDS).intValue()
        );
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTY_DESCRIPTORS;
    }

    @Override
    public Set<Relationship> getRelationships() {
        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        return relationships;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        StateMap state;
        try {
            state = context.getStateManager().getState(Scope.CLUSTER);
        } catch (IOException e) {
            throw new ProcessException("State retrieval failed", e);
        }

        final String fromDateTime = state.get(resourceName);

        FlowFile flowFile = session.create();

        flowFile = session.write(flowFile, rawOut -> {
            try (BufferedOutputStream out = new BufferedOutputStream(rawOut)) {
                IOUtils.copyLarge(hubSpotRestService.getHubSpotObjects(fromDateTime), out);
            }
        });

        session.transfer(flowFile, REL_SUCCESS);

        Map<String, String> newState = new HashMap<>(state.toMap());
        if (hubSpotResource.getIncrementalLoadingParameter() != IncrementalLoadingParameter.NONE) {
            newState.put(hubSpotRestService.getResourceName(), Instant.now().toString());
            updateState(context, newState);
        }
    }

    private void updateState(ProcessContext context, Map<String, String> newState) {
        try {
            context.getStateManager().setState(newState, Scope.CLUSTER);
        } catch (IOException e) {
            throw new ProcessException("Last Age Filter state update failed", e);
        }
    }
}
