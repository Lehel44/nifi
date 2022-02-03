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
package org.apache.nifi.processors.salesforce;

import org.apache.nifi.annotation.behavior.Stateful;
import org.apache.nifi.annotation.behavior.TriggerSerially;
import org.apache.nifi.annotation.configuration.DefaultSchedule;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.components.state.Scope;
import org.apache.nifi.components.state.StateManager;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.scheduling.SchedulingStrategy;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Stateful(scopes = Scope.CLUSTER, description = "The processor holds the state")
@DefaultSchedule(strategy = SchedulingStrategy.TIMER_DRIVEN, period = "1 hour")
@TriggerSerially
public abstract class AbstractListSObjectsProcessor extends AbstractSalesForceProcessor {

    static final PropertyDescriptor SOBJECT_NAME = new PropertyDescriptor.Builder()
            .name("sobject-name")
            .displayName("SObject Name")
            .description("")
            .required(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .build();

    static final PropertyDescriptor START_DATE = new PropertyDescriptor.Builder()
            .name("start-date")
            .displayName("Start Date")
            .description("Date to start processing from in the following format:  . example: 2019-08-19T15:51:48+1:00")
            .addValidator(Validator.VALID)
            .required(false)
            .build();

    static final PropertyDescriptor END_DATE = new PropertyDescriptor.Builder()
            .name("end-date")
            .displayName("End Date")
            .required(false)
            .addValidator(Validator.VALID)
            .build();

    static final PropertyDescriptor POLL = new PropertyDescriptor.Builder()
            .name("poll")
            .displayName("Poll")
            .required(true)
            .allowableValues("true", "false")
            .defaultValue("false")
            .build();

    static final PropertyDescriptor POLL_INTERVAL = new PropertyDescriptor.Builder()
            .name("poll-interval")
            .displayName("Poll Interval")
            .required(false)
            .build();

    private String path;
    private String objectUrlPath;
    private String sObjectName;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxxxx").withZone(ZoneId.of("UTC"));
    private final Clock clock;

    public AbstractListSObjectsProcessor() {
        this(Clock.systemUTC());
    }

    public AbstractListSObjectsProcessor(Clock clock) {
        this.clock = clock;
    }

    protected abstract String processResult(ProcessSession session, String sObjectName, String objectUrlPath, String result);

    protected abstract String getListType();

    @OnScheduled
    public void setup(ProcessContext context) {
        String version = context.getProperty(API_VERSION).evaluateAttributeExpressions().getValue();
        sObjectName = context.getProperty(SOBJECT_NAME).getValue();
        path = getVersionedPath(version, "/sobjects/" + sObjectName + getListType());
        objectUrlPath = getVersionedPath(version, "/sobjects/" + sObjectName + "/");

    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        List<PropertyDescriptor> propertyDescriptors = new ArrayList<>(super.getSupportedPropertyDescriptors());
        propertyDescriptors.addAll(Arrays.asList(SOBJECT_NAME, START_DATE, END_DATE, POLL));
        return propertyDescriptors;
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        boolean poll = context.getProperty(POLL).asBoolean();
        String startDateString;
        String endDateString;
        if (poll) {
            try {
                OffsetDateTime startDateTime = getStartDate(context);
                OffsetDateTime endDateTime = startDateTime.plusDays(30);
                startDateString = startDateTime.format(formatter);
                endDateString = endDateTime.format(formatter);
            } catch (IOException e) {
                getLogger().error("Failed to restore processor state; yielding", e);
                context.yield();
                return;
            }
        } else {
            OffsetDateTime startDateTime = Optional.ofNullable(context.getProperty(START_DATE).getValue())
                    .map(date -> OffsetDateTime.parse(date, formatter))
                    .orElseGet(() -> clock.instant().atOffset(ZoneOffset.UTC));
            startDateString = startDateTime.format(formatter);
            OffsetDateTime endDateTime = Optional.ofNullable(context.getProperty(END_DATE).getValue())
                    .map(date -> OffsetDateTime.parse(date, formatter))
                    .orElseGet(() -> startDateTime.plusDays(30));

            endDateString = endDateTime.format(formatter);
        }


        Map<String, String> queryParams = new HashMap<>();

        queryParams.put("start", startDateString);
        queryParams.put("end", endDateString);
        String result = doGetRequest(path, queryParams);

        String lastDateCovered = processResult(session, sObjectName, objectUrlPath, result);
        persistState(context, lastDateCovered);
    }

    private void persistState(ProcessContext context, String lastDateCovered) {
        try {
            Map<String, String> state = new HashMap<>();
            state.put("lastDate", lastDateCovered);
            context.getStateManager().setState(state, Scope.CLUSTER);
        } catch (IOException e) {
            getLogger().error("Failed to save cluster-wide state. If NiFi is restarted, data duplication may occur", e);
        }
    }

    private OffsetDateTime getStartDate(ProcessContext context) throws IOException {
        OffsetDateTime resultDate;
        StateManager stateManager = context.getStateManager();
        String lastDate = stateManager.getState(Scope.CLUSTER).get("lastDate");
        if (lastDate == null) {
            String startDate = context.getProperty(START_DATE).getValue();
            if (startDate == null) {
                resultDate = clock.instant().atOffset(ZoneOffset.UTC);
            } else {
                resultDate = OffsetDateTime.parse(startDate, formatter);
            }
        } else {
            resultDate = OffsetDateTime.parse(lastDate, formatter);
        }

        return resultDate;
    }
}
