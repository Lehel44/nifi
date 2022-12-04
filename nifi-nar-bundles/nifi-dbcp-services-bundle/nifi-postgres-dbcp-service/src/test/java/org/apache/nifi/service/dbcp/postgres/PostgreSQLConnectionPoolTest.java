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
package org.apache.nifi.service.dbcp.postgres;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.util.MockConfigurationContext;
import org.apache.nifi.util.file.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class PostgreSQLConnectionPoolTest {

    private static final String TEST_HOST_NAME = "testHostName";
    private static final String TEST_PORT = "55555";
    private static final String TEST_DATABASE_NAME = "testDatabaseName";
    private static final Path TEST_DRIVER_DIR = Paths.get("src/test/resources/testDriverDir");
    private static final Path TEST_RESOURCES_DIR = Paths.get("src/test/resources");

    PostgreSQLConnectionPool testConnectionPool;
    Map<PropertyDescriptor, String> propertyDescriptors;
    MockConfigurationContext mockConfigurationContext;

    @BeforeEach
    void init() {
        testConnectionPool = new PostgreSQLConnectionPool();
        propertyDescriptors = new HashMap<>();
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteFilesInDir(TEST_DRIVER_DIR.toFile(), null, null);
        Files.deleteIfExists(TEST_DRIVER_DIR);
        Files.deleteIfExists(TEST_RESOURCES_DIR);
    }

    @Test
    void testFullUrlModeReturnsDatabaseUrl() {
        final String expectedUrl = "testFullUrl";
        propertyDescriptors.put(PostgreSQLConnectionPool.CONNECTION_URL_FORMAT, JdbcUrlFormat.FULL_URL.getValue());
        propertyDescriptors.put(PostgreSQLConnectionPool.POSTGRES_DATABASE_URL, expectedUrl);
        mockConfigurationContext = new MockConfigurationContext(propertyDescriptors, null);

        final String uri = testConnectionPool.getUrl(mockConfigurationContext);

        assertEquals(expectedUrl, uri);
    }

    @Test
    void testParametersHostPortDb() {
        final String expectedUrl = String.format(JdbcUrlFormat.POSTGRESQL_HOST_PORT_DB_URI_TEMPLATE, TEST_HOST_NAME, TEST_PORT, TEST_DATABASE_NAME);
        propertyDescriptors.put(PostgreSQLConnectionPool.CONNECTION_URL_FORMAT, JdbcUrlFormat.PARAMETERS.getValue());
        propertyDescriptors.put(PostgreSQLConnectionPool.DATABASE_HOSTNAME, TEST_HOST_NAME);
        propertyDescriptors.put(PostgreSQLConnectionPool.DATABASE_PORT, TEST_PORT);
        propertyDescriptors.put(PostgreSQLConnectionPool.DATABASE_NAME, TEST_DATABASE_NAME);
        mockConfigurationContext = new MockConfigurationContext(propertyDescriptors, null);

        final String uri = testConnectionPool.getUrl(mockConfigurationContext);

        assertEquals(expectedUrl, uri);
    }

    @Test
    void testParametersHostPort() {
        final String expectedUrl = String.format(JdbcUrlFormat.POSTGRESQL_HOST_PORT_URI_TEMPLATE, TEST_HOST_NAME, TEST_PORT);
        propertyDescriptors.put(PostgreSQLConnectionPool.CONNECTION_URL_FORMAT, JdbcUrlFormat.PARAMETERS.getValue());
        propertyDescriptors.put(PostgreSQLConnectionPool.DATABASE_HOSTNAME, TEST_HOST_NAME);
        propertyDescriptors.put(PostgreSQLConnectionPool.DATABASE_PORT, TEST_PORT);
        mockConfigurationContext = new MockConfigurationContext(propertyDescriptors, null);

        final String uri = testConnectionPool.getUrl(mockConfigurationContext);

        assertEquals(expectedUrl, uri);
    }

    @Test
    void testParametersHostDb() {
        final String expectedUrl = String.format(JdbcUrlFormat.POSTGRESQL_HOST_DB_URI_TEMPLATE, TEST_HOST_NAME, TEST_DATABASE_NAME);
        propertyDescriptors.put(PostgreSQLConnectionPool.CONNECTION_URL_FORMAT, JdbcUrlFormat.PARAMETERS.getValue());
        propertyDescriptors.put(PostgreSQLConnectionPool.DATABASE_HOSTNAME, TEST_HOST_NAME);
        propertyDescriptors.put(PostgreSQLConnectionPool.DATABASE_NAME, TEST_DATABASE_NAME);
        mockConfigurationContext = new MockConfigurationContext(propertyDescriptors, null);

        final String uri = testConnectionPool.getUrl(mockConfigurationContext);

        assertEquals(expectedUrl, uri);
    }

    @Test
    void testParametersDb() {
        final String expectedUrl = String.format(JdbcUrlFormat.POSTGRESQL_DB_URI_TEMPLATE, TEST_DATABASE_NAME);
        propertyDescriptors.put(PostgreSQLConnectionPool.CONNECTION_URL_FORMAT, JdbcUrlFormat.PARAMETERS.getValue());
        propertyDescriptors.put(PostgreSQLConnectionPool.DATABASE_NAME, TEST_DATABASE_NAME);
        mockConfigurationContext = new MockConfigurationContext(propertyDescriptors, null);

        final String uri = testConnectionPool.getUrl(mockConfigurationContext);

        assertEquals(expectedUrl, uri);
    }

    @Test
    void testBlankParameters() {
        propertyDescriptors.put(PostgreSQLConnectionPool.CONNECTION_URL_FORMAT, JdbcUrlFormat.PARAMETERS.getValue());
        mockConfigurationContext = new MockConfigurationContext(propertyDescriptors, null);

        final String uri = testConnectionPool.getUrl(mockConfigurationContext);

        assertEquals(JdbcUrlFormat.POSTGRESQL_BASIC_URI, uri);
    }

    @Test
    void testDriverDirAndFileCreated() {
        PostgreSQLConnectionPool customTestConnectionPool = new PostgreSQLConnectionPool() {
            @Override
            Path getDriverDir() {
                return TEST_DRIVER_DIR;
            }

            @Override
            protected String getUrl(ConfigurationContext context) {
                return "https://driver-url.test";
            }
        };

        final String driverName = PostgresDriver.JDBC_DRIVER_42_5_1.getValue();
        propertyDescriptors.put(PostgreSQLConnectionPool.JDBC_DRIVER_VERSION, driverName);
        mockConfigurationContext = new MockConfigurationContext(propertyDescriptors, null);

        customTestConnectionPool.downloadDriver(mockConfigurationContext);

        assertTrue(Files.isDirectory(TEST_DRIVER_DIR));
        assertTrue(Files.exists(Paths.get(TEST_DRIVER_DIR.toString(), driverName)));
    }
}
