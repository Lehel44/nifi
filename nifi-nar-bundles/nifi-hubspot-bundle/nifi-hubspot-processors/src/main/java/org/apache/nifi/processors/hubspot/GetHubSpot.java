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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.PrimaryNodeOnly;
import org.apache.nifi.annotation.behavior.Stateful;
import org.apache.nifi.annotation.behavior.TriggerSerially;
import org.apache.nifi.annotation.behavior.TriggerWhenEmpty;
import org.apache.nifi.annotation.configuration.DefaultSettings;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.AllowableValue;
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
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.web.client.api.HttpResponseEntity;
import org.apache.nifi.web.client.api.HttpResponseStatus;
import org.apache.nifi.web.client.api.HttpUriBuilder;
import org.apache.nifi.web.client.provider.api.WebClientServiceProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@PrimaryNodeOnly
@TriggerSerially
@TriggerWhenEmpty
@InputRequirement(Requirement.INPUT_FORBIDDEN)
@Tags({"hubspot"})
@CapabilityDescription("Retrieves JSON data from a private HubSpot application."
        + " Configuring the Result Limit property enables incremental retrieval of results. When this property is set the processor will"
        + " retrieve new records. This processor is intended to be run on the Primary Node only.")
@Stateful(scopes = Scope.CLUSTER, description = "When the 'Limit' attribute is set, the paging cursor is saved after executing a request."
        + " Only the objects after the paging cursor will be retrieved. The maximum number of retrieved objects is the 'Limit' attribute.")
@DefaultSettings(yieldDuration = "10 sec")
public class GetHubSpot extends AbstractProcessor {

    static final AllowableValue CREATE_DATE = new AllowableValue("createDate", "Create Date", "The time of the field was created");
    static final AllowableValue LAST_MODIFIED_DATE = new AllowableValue("lastModifiedDate", "Last Modified Date",
            "The time of the field was last modified");

    static final PropertyDescriptor OBJECT_TYPE = new PropertyDescriptor.Builder()
            .name("object-type")
            .displayName("Object Type")
            .description("The HubSpot Object Type requested")
            .required(true)
            .allowableValues(HubSpotObjectType.class)
            .build();

    static final PropertyDescriptor ACCESS_TOKEN = new PropertyDescriptor.Builder()
            .name("access-token")
            .displayName("Access Token")
            .description("Access Token to authenticate requests")
            .required(true)
            .sensitive(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.NONE)
            .build();

    static final PropertyDescriptor RESULT_LIMIT = new PropertyDescriptor.Builder()
            .name("result-limit")
            .displayName("Result Limit")
            .description("The maximum number of results to request for each invocation of the Processor")
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .required(false)
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    static final PropertyDescriptor IS_INCREMENTAL = new PropertyDescriptor.Builder()
            .name("is-incremental")
            .displayName("Incremental Loading")
            .description("The processor can incrementally load the queried objects so that each object is queried exactly once." +
                    " For each query, the processor queries objects which were created or modified after the previous run time" +
                    " but before the current time.")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .build();

    static final PropertyDescriptor INCREMENTAL_FIELD = new PropertyDescriptor.Builder()
            .name("incremental-field")
            .displayName("Incremental Field")
            .description("The field that the Processor uses to perform incremental loading")
            .required(true)
            .allowableValues(CREATE_DATE, LAST_MODIFIED_DATE)
            .defaultValue(LAST_MODIFIED_DATE.getValue())
            .dependsOn(IS_INCREMENTAL, "true")
            .build();

    static final PropertyDescriptor WEB_CLIENT_SERVICE_PROVIDER = new PropertyDescriptor.Builder()
            .name("web-client-service-provider")
            .displayName("Web Client Service Provider")
            .description("Controller service for HTTP client operations")
            .identifiesControllerService(WebClientServiceProvider.class)
            .required(true)
            .build();

    static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("For FlowFiles created as a result of a successful HTTP request.")
            .build();

    private static final String API_BASE_URI = "api.hubapi.com";
    private static final String HTTPS = "https";
    private static final String CURSOR_PARAMETER = "after";
    private static final String LIMIT_PARAMETER = "limit";
    private static final int TOO_MANY_REQUESTS = 429;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JsonFactory JSON_FACTORY = OBJECT_MAPPER.getFactory();
    private static final String EMPTY_JSON = "{}";
    private static final Map<String, HubSpotObjectType> objectTypeLookupMap = createObjectTypeLookupMap();

    private static Map<String, HubSpotObjectType> createObjectTypeLookupMap() {
        return Arrays.stream(HubSpotObjectType.values())
                .collect(Collectors.toMap(HubSpotObjectType::getValue, Function.identity()));
    }

    private volatile WebClientServiceProvider webClientServiceProvider;

    private static final List<PropertyDescriptor> PROPERTY_DESCRIPTORS = Collections.unmodifiableList(Arrays.asList(
            OBJECT_TYPE,
            ACCESS_TOKEN,
            RESULT_LIMIT,
            IS_INCREMENTAL,
            INCREMENTAL_FIELD,
            WEB_CLIENT_SERVICE_PROVIDER
    ));

    private static final Set<Relationship> RELATIONSHIPS = Collections.singleton(REL_SUCCESS);
    private AtomicInteger total = new AtomicInteger(-1);

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        webClientServiceProvider = context.getProperty(WEB_CLIENT_SERVICE_PROVIDER).asControllerService(WebClientServiceProvider.class);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTY_DESCRIPTORS;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final String accessToken = context.getProperty(ACCESS_TOKEN).getValue();
        final String endpoint = context.getProperty(OBJECT_TYPE).getValue();

        final StateMap state = getStateMap(context);
        final URI uri = getBaseUri(context);

        final HttpResponseEntity response = getHttpResponseEntity(context, accessToken, uri);

        if (response.statusCode() == HttpResponseStatus.OK.getCode()) {
            FlowFile flowFile = session.create();
            flowFile = session.write(flowFile, parseHttpResponse(context, endpoint, state, response));
            if (total.get() > 0) {
                session.transfer(flowFile, REL_SUCCESS);
            } else {
                getLogger().debug("Empty response when requested HubSpot endpoint: [{}]", endpoint);
                session.remove(flowFile);
            }
        } else if (response.statusCode() == TOO_MANY_REQUESTS) {
            context.yield();
            throw new ProcessException(String.format("Rate limit exceeded, yielding before retrying request. HTTP %d error for requested URI [%s]", response.statusCode(), uri));
        } else {
            final String responseBody = getResponseBodyAsString(context, response, uri);
            getLogger().warn("HTTP {} error for requested URI [{}] with response [{}]", response.statusCode(), uri, responseBody);
        }
    }

    private String getResponseBodyAsString(final ProcessContext context, final HttpResponseEntity response, final URI uri) {
        try {
            return IOUtils.toString(response.body(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            context.yield();
            throw new UncheckedIOException(String.format("Reading HTTP response body for requested URI [%s] failed", uri), e);
        }
    }

    private OutputStreamCallback parseHttpResponse(ProcessContext context, String endpoint, StateMap state, HttpResponseEntity response) {
        return out -> {
            try (final JsonParser jsonParser = JSON_FACTORY.createParser(response.body());
                 final JsonGenerator jsonGenerator = JSON_FACTORY.createGenerator(out, JsonEncoding.UTF8)) {
                while (jsonParser.nextToken() != null) {
                    if (jsonParser.getCurrentToken() == JsonToken.FIELD_NAME && jsonParser.getCurrentName()
                            .equals("total")) {
                        jsonParser.nextToken();
                        total.set(jsonParser.getIntValue());
                    }
                    if (jsonParser.getCurrentToken() == JsonToken.FIELD_NAME && jsonParser.getCurrentName()
                            .equals("results")) {
                        jsonParser.nextToken();
                        jsonGenerator.copyCurrentStructure(jsonParser);
                    }
//                    final String fieldName = jsonParser.getCurrentName();
//                    if (CURSOR_PARAMETER.equals(fieldName)) {
//                        jsonParser.nextToken();
//                        Map<String, String> newStateMap = new HashMap<>(state.toMap());
//                        newStateMap.put(endpoint, jsonParser.getText());
//                        updateState(context, newStateMap);
//                        break;
//                    }
                }
            }
        };
    }

    private URI getBaseUri(final ProcessContext context) {
        final String path = context.getProperty(OBJECT_TYPE).getValue();
        return webClientServiceProvider.getHttpUriBuilder()
                .scheme(HTTPS)
                .host(API_BASE_URI)
                .encodedPath(path + "/search")
                .build();
    }

    private HttpResponseEntity getHttpResponseEntity(final ProcessContext context, final String accessToken, final URI uri) {
        final InputStreamConverter converter = new InputStreamConverter(createIncrementalFilters(context));
        return webClientServiceProvider.getWebClientService()
                .post()
                .uri(uri)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .body(converter.getInputStreamFromJson(), OptionalLong.of(converter.getByteSize()))
                .retrieve();
    }

    private String createIncrementalFilters(final ProcessContext context) {
        final String objectType = context.getProperty(OBJECT_TYPE).getValue();
        final HubSpotObjectType hubSpotObjectType = objectTypeLookupMap.get(objectType);
        final String incrementalFieldName = context.getProperty(INCREMENTAL_FIELD).getValue();
        final String incrementalKey = String.format("%s - %s", objectType, incrementalFieldName);

        final ObjectNode root = OBJECT_MAPPER.createObjectNode();
        final boolean isLimitSet = context.getProperty(RESULT_LIMIT).evaluateAttributeExpressions().isSet();
        if (isLimitSet) {
            final String limit = context.getProperty(RESULT_LIMIT).getValue();
            root.put("limit", limit);
        }

        final boolean isIncremental = context.getProperty(IS_INCREMENTAL).asBoolean();
        if (isIncremental) {

            String hubspotSpecificIncrementalFieldName = null;
            if (incrementalFieldName.equals(CREATE_DATE.getValue())) {
                hubspotSpecificIncrementalFieldName = hubSpotObjectType.getCreateDateType().getValue();
            } else if (incrementalFieldName.equals(LAST_MODIFIED_DATE.getValue())) {
                hubspotSpecificIncrementalFieldName = hubSpotObjectType.getLastModifiedDateType().getValue();
            }

            final StateMap state = getStateMap(context);
            final String lastExecutionTime = state.get(incrementalKey);

            final ArrayNode filters = OBJECT_MAPPER.createArrayNode();

            if (lastExecutionTime != null) {
                final ObjectNode greaterThanFilterNode = OBJECT_MAPPER.createObjectNode();
                greaterThanFilterNode.put("propertyName", hubspotSpecificIncrementalFieldName);
                greaterThanFilterNode.put("operator", "GT");
                greaterThanFilterNode.put("value", lastExecutionTime);
                filters.add(greaterThanFilterNode);
            }

            final long now = Instant.now().toEpochMilli();

            final ObjectNode lessThanFilterNode = OBJECT_MAPPER.createObjectNode();
            lessThanFilterNode.put("propertyName", hubspotSpecificIncrementalFieldName);
            lessThanFilterNode.put("operator", "LT");
            lessThanFilterNode.put("value", now);
            filters.add(lessThanFilterNode);

            root.set("filters", filters);

            final HashMap<String, String> stateMap = new HashMap<>(state.toMap());
            stateMap.put(incrementalKey, String.valueOf(now));
            updateState(context, stateMap);
        }
        return root.toString();
    }

    private StateMap getStateMap(final ProcessContext context) {
        final StateMap stateMap;
        try {
            stateMap = context.getStateManager().getState(Scope.CLUSTER);
        } catch (IOException e) {
            throw new ProcessException("State retrieval failed", e);
        }
        return stateMap;
    }

    private void updateState(ProcessContext context, Map<String, String> newState) {
        try {
            context.getStateManager().setState(newState, Scope.CLUSTER);
        } catch (IOException e) {
            throw new ProcessException("Page cursor update failed", e);
        }
    }
}
