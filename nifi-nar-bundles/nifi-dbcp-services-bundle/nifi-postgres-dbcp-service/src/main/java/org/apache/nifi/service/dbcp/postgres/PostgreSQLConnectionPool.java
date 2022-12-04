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

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.DynamicProperties;
import org.apache.nifi.annotation.behavior.DynamicProperty;
import org.apache.nifi.annotation.behavior.SupportsSensitiveDynamicProperties;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnRemoved;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.dbcp.AbstractDBCPConnectionPool;
import org.apache.nifi.dbcp.DriverShim;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.security.util.TlsConfiguration;
import org.apache.nifi.service.dbcp.postgres.ssl.NiFiSslSocketFactory;
import org.apache.nifi.service.dbcp.postgres.ssl.SslMode;
import org.apache.nifi.ssl.SSLContextService;
import org.apache.nifi.util.NiFiProperties;
import org.apache.nifi.util.file.classloader.ClassLoaderUtils;
import org.postgresql.PGProperty;

import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.nifi.service.dbcp.postgres.JdbcUrlFormat.FULL_URL;
import static org.apache.nifi.service.dbcp.postgres.JdbcUrlFormat.PARAMETERS;
import static org.apache.nifi.service.dbcp.postgres.JdbcUrlFormat.POSTGRESQL_BASIC_URI;

/**
 * Implementation of Database Connection Pooling Service for PostgreSQL with built-in JDBC drivers. Apache DBCP is used
 * for connection pooling functionality.
 */
@Tags({"postgres", "postgresql", "dbcp", "jdbc", "database", "connection", "pooling", "store"})
@CapabilityDescription("Provides PostgreSQL Connection Pooling Service with built-in JDBC driver." +
        " Connections can be requested from the pool and returned once they have been used.")
@SupportsSensitiveDynamicProperties
@DynamicProperties({
        @DynamicProperty(name = "JDBC property name",
                value = "JDBC property value",
                expressionLanguageScope = ExpressionLanguageScope.VARIABLE_REGISTRY,
                description = "JDBC driver property name and value applied to JDBC connections."),
        @DynamicProperty(name = "SENSITIVE.JDBC property name",
                value = "JDBC property value",
                expressionLanguageScope = ExpressionLanguageScope.NONE,
                description = "JDBC driver property name prefixed with 'SENSITIVE.' handled as a sensitive property.")
})
public class PostgreSQLConnectionPool extends AbstractDBCPConnectionPool {

    private static final String DRIVERS_DIR_BASE = "DriverTmpDir";
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    public static final PropertyDescriptor JDBC_DRIVER_VERSION = new PropertyDescriptor.Builder()
            .name("jdbc-driver")
            .displayName("JDBC Driver Version")
            .description("The JDBC driver version.")
            .required(true)
            .allowableValues(PostgresDriver.class)
            .build();

    public static final PropertyDescriptor CONNECTION_URL_FORMAT = new PropertyDescriptor.Builder()
            .name("connection-url-format")
            .displayName("Connection URL Format")
            .description("The format of the connection URL.")
            .allowableValues(JdbcUrlFormat.class)
            .required(true)
            .defaultValue(PARAMETERS.getValue())
            .build();

    public static final PropertyDescriptor POSTGRES_DATABASE_URL = new PropertyDescriptor.Builder()
            .fromPropertyDescriptor(DATABASE_URL)
            .dependsOn(CONNECTION_URL_FORMAT, FULL_URL)
            .build();

    public static final PropertyDescriptor DATABASE_NAME = new PropertyDescriptor.Builder()
            .name("database-name")
            .displayName("Database Name")
            .description("The name of the database to connect.")
            .required(false)
            .dependsOn(CONNECTION_URL_FORMAT, PARAMETERS)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor DATABASE_HOSTNAME = new PropertyDescriptor.Builder()
            .name("database-hostname")
            .displayName("Database Hostname")
            .description("The hostname of the database to connect.")
            .required(false)
            .dependsOn(CONNECTION_URL_FORMAT, PARAMETERS)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor DATABASE_PORT = new PropertyDescriptor.Builder()
            .name("database-port")
            .displayName("Database Port")
            .description("The port of the database to connect.")
            .required(false)
            .dependsOn(CONNECTION_URL_FORMAT, PARAMETERS)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor SSL_MODE = new PropertyDescriptor.Builder()
            .name("ssl-mode")
            .displayName("SSL Mode")
            .description("The SSL mode to use when connection to the database.")
            .required(true)
            .allowableValues(SslMode.class)
            .defaultValue(SslMode.DISABLE.getValue())
            .build();

    public static final PropertyDescriptor SSL_CONTEXT_SERVICE = new PropertyDescriptor.Builder()
            .name("SSL Context Service")
            .description("If specified, indicates the SSL Context Service that is used to communicate with the "
                    + "remote server. If not specified, communications will not be encrypted")
            .required(true)
            .identifiesControllerService(SSLContextService.class)
            .dependsOn(SSL_MODE, SslMode.ALLOW, SslMode.PREFER, SslMode.REQUIRE, SslMode.VERIFY_CA, SslMode.VERIFY_FULL)
            .build();

    private static final List<PropertyDescriptor> PROPERTIES = Collections.unmodifiableList(Arrays.asList(
            JDBC_DRIVER_VERSION,
            CONNECTION_URL_FORMAT,
            POSTGRES_DATABASE_URL,
            DATABASE_NAME,
            DATABASE_HOSTNAME,
            DATABASE_PORT,
            DB_USER,
            DB_PASSWORD,
            KERBEROS_USER_SERVICE,
            SSL_MODE,
            SSL_CONTEXT_SERVICE
    ));

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTIES;
    }

    @Override
    protected void downloadDriver(final ConfigurationContext context) {
        final String value = context.getProperty(JDBC_DRIVER_VERSION).getValue();
        final Path driverDir = getDriverDir();
        final Path driverPath = Paths.get(driverDir.toString(), value);
        if (Files.exists(driverPath)) {
            return;
        }
        try {
            Files.createDirectories(driverDir);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Could not create directories at path [%s]", driverDir), e);
        }
        final PostgresDriver driver = PostgresDriver.forName(value);
        final URL url = getUrl(driver);
        try (final ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
             final FileOutputStream fileOutputStream = new FileOutputStream(driverPath.toString())) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not download driver", e);
        }
    }

    @Override
    protected Driver getDriver(final String driverName, final String url) {
        final String value = getConfigurationContext().getProperty(JDBC_DRIVER_VERSION).getValue();
        final String version = PostgresDriver.forName(value).getVersion();
        final String driverPath = Paths.get(TMP_DIR, DRIVERS_DIR_BASE, getIdentifier(), value).toString();

        for (Enumeration<Driver> driver = DriverManager.getDrivers(); driver.hasMoreElements(); ) {
            final Driver currentDriver = driver.nextElement();
            DriverShim driverShim = null;
            if (currentDriver instanceof DriverShim) {
                driverShim = (DriverShim) currentDriver;
            }
            if (driverShim != null && driverShim.getVersion().equals(version)) {
                try {
                    return DriverManager.getDriver(url);
                } catch (SQLException e) {
                    throw new RuntimeException("No suitable driver for the given Database Connection URL", e);
                }
            }
        }
        final ClassLoader customClassLoader;
        try {
            customClassLoader = ClassLoaderUtils.getCustomClassLoader(driverPath, Thread.currentThread().getContextClassLoader(), getJarFilenameFilter());
            final Driver driver = (Driver) Class.forName("org.postgresql.Driver", true, customClassLoader).newInstance();
            DriverManager.registerDriver(new DriverShim(driver, version));
            return driver;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Map<String, String> getConnectionProperties(ConfigurationContext context) {
        final Map<String, String> connectionProperties = new HashMap<>();
        SslMode sslMode = SslMode.forName(context.getProperty(SSL_MODE).getValue());
        if (sslMode == SslMode.DISABLE) {
            return connectionProperties;
        }
        final SSLContextService sslContextService = getConfigurationContext().getProperty(SSL_CONTEXT_SERVICE).asControllerService(SSLContextService.class);
        final TlsConfiguration tlsConfiguration = sslContextService.createTlsConfiguration();

        connectionProperties.put(PGProperty.SSL.getName(), "true");
        connectionProperties.put(PGProperty.SSL_FACTORY.getName(), NiFiSslSocketFactory.class.getName());
        connectionProperties.put(PGProperty.SSL_MODE.getName(), sslMode.getValue());

        connectionProperties.put(NiFiProperties.SECURITY_KEYSTORE, tlsConfiguration.getTruststorePath());
        connectionProperties.put(NiFiProperties.SECURITY_KEYSTORE_PASSWD, tlsConfiguration.getTruststorePassword());
        connectionProperties.put(NiFiProperties.SECURITY_KEYSTORE_TYPE, tlsConfiguration.getTruststoreType().getType());
        connectionProperties.put(NiFiProperties.SECURITY_TRUSTSTORE, tlsConfiguration.getTruststorePath());
        connectionProperties.put(NiFiProperties.SECURITY_TRUSTSTORE_PASSWD, tlsConfiguration.getTruststorePassword());
        connectionProperties.put(NiFiProperties.SECURITY_TRUSTSTORE_TYPE, tlsConfiguration.getTruststoreType().getType());

        return connectionProperties;
    }

    @Override
    protected String getUrl(ConfigurationContext context) {
        final JdbcUrlFormat jdbcUrlFormat = JdbcUrlFormat.forName(context.getProperty(CONNECTION_URL_FORMAT).getValue());
        if (jdbcUrlFormat == FULL_URL) {
            return context.getProperty(POSTGRES_DATABASE_URL).evaluateAttributeExpressions().getValue();
        } else if (jdbcUrlFormat == PARAMETERS) {
            final String databaseName = context.getProperty(DATABASE_NAME).getValue();
            final String hostname = context.getProperty(DATABASE_HOSTNAME).getValue();
            final String port = context.getProperty(DATABASE_PORT).getValue();

            if (StringUtils.isNoneBlank(databaseName, hostname, port)) {
                return String.format(JdbcUrlFormat.POSTGRESQL_HOST_PORT_DB_URI_TEMPLATE, hostname, port, databaseName);
            } else if (StringUtils.isNoneBlank(hostname, port) && StringUtils.isBlank(databaseName)) {
                return String.format(JdbcUrlFormat.POSTGRESQL_HOST_PORT_URI_TEMPLATE, hostname, port);
            } else if (StringUtils.isNoneBlank(hostname, databaseName) && StringUtils.isBlank(port)) {
                return String.format(JdbcUrlFormat.POSTGRESQL_HOST_DB_URI_TEMPLATE, hostname, databaseName);
            } else if (StringUtils.isNotBlank(databaseName) && StringUtils.isAllBlank(hostname, port)) {
                return String.format(JdbcUrlFormat.POSTGRESQL_DB_URI_TEMPLATE, databaseName);
            } else if (StringUtils.isAllBlank(databaseName, hostname, port)) {
                return POSTGRESQL_BASIC_URI;
            }
        }
        throw new IllegalArgumentException("Invalid JDBC URI format");
    }

    @OnRemoved
    public void tearDown() throws SQLException {
        for (Enumeration<Driver> drivers = DriverManager.getDrivers(); drivers.hasMoreElements(); ) {
            DriverManager.deregisterDriver(drivers.nextElement());
        }
    }

    Path getDriverDir() {
        return Paths.get(TMP_DIR, DRIVERS_DIR_BASE, getIdentifier());
    }

    private URL getUrl(final PostgresDriver driver) {
        final String url = driver.getUrl();
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format("Invalid Postgres JDBC Driver URL: %s", url), e);
        }
    }

    private FilenameFilter getJarFilenameFilter() {
        return (dir, name) -> (name != null && name.endsWith(".jar"));
    }
}