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
package org.apache.nifi.dbcp;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.controller.VerifiableControllerService;
import org.apache.nifi.dbcp.builder.BasicDataSourceConfiguration;
import org.apache.nifi.expression.AttributeExpression;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.nifi.dbcp.DBCPProperties.DATABASE_URL;
import static org.apache.nifi.dbcp.DBCPProperties.DB_PASSWORD;
import static org.apache.nifi.dbcp.DBCPProperties.DB_USER;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.DB_DRIVERNAME;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.EVICTION_RUN_PERIOD;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.MAX_CONN_LIFETIME;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.MAX_IDLE;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.MAX_TOTAL_CONNECTIONS;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.MAX_WAIT_TIME;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.MIN_EVICTABLE_IDLE_TIME;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.MIN_IDLE;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.SOFT_MIN_EVICTABLE_IDLE_TIME;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.VALIDATION_QUERY;

/**
 * Abstract base class for Database Connection Pooling Services using Apache Commons DBCP as the underlying connection pool implementation.
 */
public abstract class AbstractDBCPConnectionPool extends AbstractConnectionPool implements DBCPService, VerifiableControllerService {
    /**
     * Property Name Prefix for Sensitive Dynamic Properties
     */
    protected static final String SENSITIVE_PROPERTY_PREFIX = "SENSITIVE.";

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {
        final PropertyDescriptor.Builder builder = new PropertyDescriptor.Builder()
                .name(propertyDescriptorName)
                .required(false)
                .dynamic(true)
                .addValidator(StandardValidators.createAttributeExpressionLanguageValidator(AttributeExpression.ResultType.STRING, true))
                .addValidator(StandardValidators.ATTRIBUTE_KEY_PROPERTY_NAME_VALIDATOR);

        if (propertyDescriptorName.startsWith(SENSITIVE_PROPERTY_PREFIX)) {
            builder.sensitive(true).expressionLanguageSupported(ExpressionLanguageScope.NONE);
        } else {
            builder.expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY);
        }

        return builder.build();
    }

    @Override
    protected BasicDataSourceConfiguration createDataSourceConfiguraton(final ConfigurationContext context) {

        final String dbUrl = getUrl(context);
        final String driverName = context.getProperty(DB_DRIVERNAME).evaluateAttributeExpressions().getValue();
        final Driver driver = getDriver(driverName, dbUrl);

        final String userName = context.getProperty(DB_USER).evaluateAttributeExpressions().getValue();
        final String password = context.getProperty(DB_PASSWORD).evaluateAttributeExpressions().getValue();
        final String validationQuery = context.getProperty(VALIDATION_QUERY).evaluateAttributeExpressions().getValue();

        final PropertyValue maxTotal = context.getProperty(MAX_TOTAL_CONNECTIONS);
        final PropertyValue maxWaitMillis = context.getProperty(MAX_WAIT_TIME);
        final PropertyValue minIdle = context.getProperty(MIN_IDLE);
        final PropertyValue maxIdle = context.getProperty(MAX_IDLE);
        final PropertyValue maxConnLifetimeMillis = context.getProperty(MAX_CONN_LIFETIME);
        final PropertyValue timeBetweenEvictionRunsMillis = context.getProperty(EVICTION_RUN_PERIOD);
        final PropertyValue minEvictableIdleTimeMillis = context.getProperty(MIN_EVICTABLE_IDLE_TIME);
        final PropertyValue softMinEvictableIdleTimeMillis = context.getProperty(SOFT_MIN_EVICTABLE_IDLE_TIME);

        return new BasicDataSourceConfiguration.Builder(dbUrl, driver, userName, password)
                .maxTotal(maxTotal)
                .maxWaitMillis(maxWaitMillis)
                .minIdle(minIdle)
                .maxIdle(maxIdle)
                .maxConnLifetimeMillis(maxConnLifetimeMillis)
                .timeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis)
                .minEvictableIdleTimeMillis(minEvictableIdleTimeMillis)
                .softMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis)
                .validationQuery(validationQuery)
                .build();

    }

    @Override
    protected void configureDataSourceWithDynamicProperties(final ConfigurationContext context) {
        final List<PropertyDescriptor> dynamicProperties = context.getProperties()
                .keySet()
                .stream()
                .filter(PropertyDescriptor::isDynamic)
                .collect(Collectors.toList());

        dynamicProperties.forEach(descriptor -> {
            final PropertyValue propertyValue = context.getProperty(descriptor);
            if (descriptor.isSensitive()) {
                final String propertyName = StringUtils.substringAfter(descriptor.getName(), SENSITIVE_PROPERTY_PREFIX);
                dataSource.addConnectionProperty(propertyName, propertyValue.getValue());
            } else {
                dataSource.addConnectionProperty(descriptor.getName(), propertyValue.evaluateAttributeExpressions().getValue());
            }
        });
    }

    protected String getUrl(ConfigurationContext context) {
        return context.getProperty(DATABASE_URL).evaluateAttributeExpressions().getValue();
    }

    protected Driver getDriver(final String driverName, final String url) {
        final Class<?> clazz;

        try {
            clazz = Class.forName(driverName);
        } catch (final ClassNotFoundException e) {
            throw new ProcessException("Driver class " + driverName + " is not found", e);
        }

        try {
            return DriverManager.getDriver(url);
        } catch (final SQLException e) {
            // In case the driver is not registered by the implementation, we explicitly try to register it.
            try {
                final Driver driver = (Driver) clazz.newInstance();
                DriverManager.registerDriver(driver);
                return DriverManager.getDriver(url);
            } catch (final SQLException e2) {
                throw new ProcessException("No suitable driver for the given Database Connection URL", e2);
            } catch (final IllegalAccessException | InstantiationException e2) {
                throw new ProcessException("Creating driver instance is failed", e2);
            }
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[id=" + getIdentifier() + "]";
    }
}
