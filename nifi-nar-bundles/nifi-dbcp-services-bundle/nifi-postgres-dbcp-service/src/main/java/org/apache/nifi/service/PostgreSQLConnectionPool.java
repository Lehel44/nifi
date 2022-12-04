package org.apache.nifi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.RequiresInstanceClassLoading;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.dbcp.AbstractDBCPConnectionPool;
import org.apache.nifi.dbcp.KerberosContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.kerberos.KerberosCredentialsService;
import org.apache.nifi.kerberos.KerberosUserService;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.nifi.service.JdbcUrlFormat.FULL_URL;
import static org.apache.nifi.service.JdbcUrlFormat.PARAMETERS;
import static org.apache.nifi.service.JdbcUrlFormat.POSTGRESQL_BASIC_URI;

/**
 * Implementation of Database Connection Pooling Service for PostgreSQL with built-in JDBC driver.
 * Apache DBCP is used for connection pooling functionality.
 */
@Tags({"postgres", "postgresql", "dbcp", "jdbc", "database", "connection", "pooling", "store"})
@CapabilityDescription("Provides PostgreSQL Connection Pooling Service. Connections can be asked from pool and returned after usage.")
@RequiresInstanceClassLoading
public class PostgreSQLConnectionPool extends AbstractDBCPConnectionPool {

    public static final PropertyDescriptor CONNECTION_URL_FORMAT = new PropertyDescriptor.Builder()
            .name("connection-url-format")
            .displayName("Connection URL Format")
            .description("The format of the connection URL.")
            .allowableValues(JdbcUrlFormat.class)
            .required(true)
            .defaultValue(FULL_URL.getValue())
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

    public static final PropertyDescriptor KERBEROS_AUTH = new PropertyDescriptor.Builder()
            .name("kerberos-auth")
            .displayName("Kerberos Authentication")
            .description("Use Kerberos authentication.")
            .allowableValues("true", "false")
            .defaultValue("false")
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor POSTGRES_KERBEROS_USER_SERVICE = new PropertyDescriptor.Builder()
            .fromPropertyDescriptor(KERBEROS_USER_SERVICE)
            .dependsOn(KERBEROS_AUTH, "true")
            .build();

    public static final PropertyDescriptor POSTGRES_KERBEROS_CREDENTIALS_SERVICE = new PropertyDescriptor.Builder()
            .fromPropertyDescriptor(KERBEROS_CREDENTIALS_SERVICE)
            .dependsOn(KERBEROS_AUTH, "true")
            .build();

    public static final PropertyDescriptor POSTGRES_KERBEROS_PRINCIPAL = new PropertyDescriptor.Builder()
            .fromPropertyDescriptor(KERBEROS_PRINCIPAL)
            .dependsOn(KERBEROS_AUTH, "true")
            .build();

    public static final PropertyDescriptor POSTGRES_KERBEROS_PASSWORD = new PropertyDescriptor.Builder()
            .fromPropertyDescriptor(KERBEROS_PASSWORD)
            .dependsOn(KERBEROS_AUTH, "true")
            .build();

    private static final List<PropertyDescriptor> PROPERTIES = Collections.unmodifiableList(Arrays.asList(
            POSTGRES_DATABASE_URL,
            DATABASE_NAME,
            DATABASE_HOSTNAME,
            DATABASE_PORT,
            KERBEROS_AUTH,
            POSTGRES_KERBEROS_USER_SERVICE,
            POSTGRES_KERBEROS_CREDENTIALS_SERVICE,
            POSTGRES_KERBEROS_PRINCIPAL,
            POSTGRES_KERBEROS_PASSWORD,
            DB_USER,
            DB_PASSWORD,
            MAX_WAIT_TIME,
            MAX_TOTAL_CONNECTIONS,
            VALIDATION_QUERY,
            MIN_IDLE,
            MAX_IDLE,
            MAX_CONN_LIFETIME,
            EVICTION_RUN_PERIOD,
            MIN_EVICTABLE_IDLE_TIME,
            SOFT_MIN_EVICTABLE_IDLE_TIME
    ));

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTIES;
    }

    @Override
    protected Collection<ValidationResult> customValidate(ValidationContext context) {
        final List<ValidationResult> results = new ArrayList<>();

        final boolean isKerberosAuth = context.getProperty(KERBEROS_AUTH).asBoolean();
        if (isKerberosAuth) {
            final boolean kerberosPrincipalProvided = !StringUtils.isBlank(context.getProperty(POSTGRES_KERBEROS_PRINCIPAL).evaluateAttributeExpressions().getValue());
            final boolean kerberosPasswordProvided = !StringUtils.isBlank(context.getProperty(POSTGRES_KERBEROS_PASSWORD).getValue());

            if (kerberosPrincipalProvided && !kerberosPasswordProvided) {
                results.add(new ValidationResult.Builder()
                        .subject(POSTGRES_KERBEROS_PASSWORD.getDisplayName())
                        .valid(false)
                        .explanation("a password must be provided for the given principal")
                        .build());
            }

            if (kerberosPasswordProvided && !kerberosPrincipalProvided) {
                results.add(new ValidationResult.Builder()
                        .subject(POSTGRES_KERBEROS_PRINCIPAL.getDisplayName())
                        .valid(false)
                        .explanation("a principal must be provided for the given password")
                        .build());
            }

            final KerberosCredentialsService kerberosCredentialsService = context.getProperty(POSTGRES_KERBEROS_CREDENTIALS_SERVICE).asControllerService(KerberosCredentialsService.class);
            final KerberosUserService kerberosUserService = context.getProperty(POSTGRES_KERBEROS_USER_SERVICE).asControllerService(KerberosUserService.class);

            if (kerberosCredentialsService != null && (kerberosPrincipalProvided || kerberosPasswordProvided)) {
                results.add(new ValidationResult.Builder()
                        .subject(POSTGRES_KERBEROS_CREDENTIALS_SERVICE.getDisplayName())
                        .valid(false)
                        .explanation("kerberos principal/password and kerberos credential service cannot be configured at the same time")
                        .build());
            }

            if (kerberosUserService != null && (kerberosPrincipalProvided || kerberosPasswordProvided)) {
                results.add(new ValidationResult.Builder()
                        .subject(POSTGRES_KERBEROS_USER_SERVICE.getDisplayName())
                        .valid(false)
                        .explanation("kerberos principal/password and kerberos user service cannot be configured at the same time")
                        .build());
            }

            if (kerberosUserService != null && kerberosCredentialsService != null) {
                results.add(new ValidationResult.Builder()
                        .subject(POSTGRES_KERBEROS_USER_SERVICE.getDisplayName())
                        .valid(false)
                        .explanation("kerberos user service and kerberos credential service cannot be configured at the same time")
                        .build());
            }
        }
        return results;
    }

    @Override
    protected Driver getDriver(final String driverName, final String url) {
        try {
            Class.forName(org.postgresql.Driver.class.getName());
            return DriverManager.getDriver(url);
        } catch (Exception e) {
            throw new ProcessException("PostgreSQL driver unavailable or incompatible connection URL", e);
        }
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

    @Override
    protected KerberosContext getKerberosContext(ConfigurationContext context) {
        final KerberosCredentialsService kerberosCredentialsService = context.getProperty(POSTGRES_KERBEROS_CREDENTIALS_SERVICE).asControllerService(KerberosCredentialsService.class);
        final KerberosUserService kerberosUserService = context.getProperty(POSTGRES_KERBEROS_USER_SERVICE).asControllerService(KerberosUserService.class);
        final String kerberosPrincipal = context.getProperty(POSTGRES_KERBEROS_PRINCIPAL).getValue();
        final String kerberosPassword = context.getProperty(POSTGRES_KERBEROS_PASSWORD).getValue();

        return new KerberosContext(kerberosCredentialsService, kerberosUserService, kerberosPrincipal, kerberosPassword);
    }
}