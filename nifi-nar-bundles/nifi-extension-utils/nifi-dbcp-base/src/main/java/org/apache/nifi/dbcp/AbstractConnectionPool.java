package org.apache.nifi.dbcp;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.ConfigVerificationResult;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.controller.VerifiableControllerService;
import org.apache.nifi.dbcp.builder.BasicDataSourceConfiguration;
import org.apache.nifi.dbcp.builder.Configuration;
import org.apache.nifi.dbcp.builder.ExtendedConfiguration;
import org.apache.nifi.kerberos.KerberosCredentialsService;
import org.apache.nifi.kerberos.KerberosUserService;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.security.krb.KerberosAction;
import org.apache.nifi.security.krb.KerberosKeytabUser;
import org.apache.nifi.security.krb.KerberosLoginException;
import org.apache.nifi.security.krb.KerberosPasswordUser;
import org.apache.nifi.security.krb.KerberosUser;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.nifi.components.ConfigVerificationResult.Outcome.FAILED;
import static org.apache.nifi.components.ConfigVerificationResult.Outcome.SUCCESSFUL;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.DB_DRIVER_LOCATION;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.KERBEROS_CREDENTIALS_SERVICE;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.KERBEROS_PASSWORD;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.KERBEROS_PRINCIPAL;
import static org.apache.nifi.dbcp.OptionalDBCPProperties.KERBEROS_USER_SERVICE;

public abstract class AbstractConnectionPool extends AbstractControllerService implements DBCPService, VerifiableControllerService {

    protected volatile BasicDataSource dataSource;
    protected volatile KerberosUser kerberosUser;

    @Override
    public List<ConfigVerificationResult> verify(final ConfigurationContext context, final ComponentLog verificationLogger, final Map<String, String> variables) {
        List<ConfigVerificationResult> results = new ArrayList<>();

        KerberosUser kerberosUser = null;
        try {
            kerberosUser = getKerberosUser(context);
            if (kerberosUser != null) {
                results.add(new ConfigVerificationResult.Builder()
                        .verificationStepName("Configure Kerberos User")
                        .outcome(SUCCESSFUL)
                        .explanation("Successfully configured Kerberos user")
                        .build());
            }
        } catch (final Exception e) {
            verificationLogger.error("Failed to configure Kerberos user", e);
            results.add(new ConfigVerificationResult.Builder()
                    .verificationStepName("Configure Kerberos User")
                    .outcome(FAILED)
                    .explanation("Failed to configure Kerberos user: " + e.getMessage())
                    .build());
        }

        final BasicDataSource dataSource = new BasicDataSource();
        try {
            final BasicDataSourceConfiguration configuration = createDataSourceConfiguraton(context);
            configureDataSource(context, configuration);
            results.add(new ConfigVerificationResult.Builder()
                    .verificationStepName("Configure Data Source")
                    .outcome(SUCCESSFUL)
                    .explanation("Successfully configured data source")
                    .build());

            try (final Connection conn = getConnection(dataSource, kerberosUser)) {
                results.add(new ConfigVerificationResult.Builder()
                        .verificationStepName("Establish Connection")
                        .outcome(SUCCESSFUL)
                        .explanation("Successfully established Database Connection")
                        .build());
            } catch (final Exception e) {
                verificationLogger.error("Failed to establish Database Connection", e);
                results.add(new ConfigVerificationResult.Builder()
                        .verificationStepName("Establish Connection")
                        .outcome(FAILED)
                        .explanation("Failed to establish Database Connection: " + e.getMessage())
                        .build());
            }
        } catch (final Exception e) {
            String message = "Failed to configure Data Source.";
            if (e.getCause() instanceof ClassNotFoundException) {
                message += String.format("  Ensure changes to the '%s' property are applied before verifying",
                        DB_DRIVER_LOCATION.getDisplayName());
            }
            verificationLogger.error(message, e);
            results.add(new ConfigVerificationResult.Builder()
                    .verificationStepName("Configure Data Source")
                    .outcome(FAILED)
                    .explanation(message + ": " + e.getMessage())
                    .build());
        } finally {
            try {
                shutdown(dataSource, kerberosUser);
            } catch (final SQLException e) {
                verificationLogger.error("Failed to shut down data source", e);
            }
        }

        return results;
    }

    /**
     * Configures connection pool by creating an instance of the
     * {@link BasicDataSource} based on configuration provided with
     * {@link ConfigurationContext}.
     * <p>
     * This operation makes no guarantees that the actual connection could be
     * made since the underlying system may still go off-line during normal
     * operation of the connection pool.
     *
     * @param context the configuration context
     * @throws InitializationException if unable to create a database connection
     */
    @OnEnabled
    public void onConfigured(final ConfigurationContext context) throws InitializationException {
        kerberosUser = getKerberosUser(context);
        loginKerberos(kerberosUser);
        dataSource = new BasicDataSource();
        final BasicDataSourceConfiguration dataSourceConfiguraton = createDataSourceConfiguraton(context);
        configureDataSource(context, dataSourceConfiguraton);
    }

    private void loginKerberos(KerberosUser kerberosUser) throws InitializationException {
        if (kerberosUser != null) {
            try {
                kerberosUser.login();
            } catch (KerberosLoginException e) {
                throw new InitializationException("Unable to authenticate Kerberos principal", e);
            }
        }
    }

    protected abstract Driver getDriver(final String driverName, final String url);

    protected Configuration createDataSourceConfiguraton(final ConfigurationContext context) {
        final Driver driver = getDriver("driverName", "url");
        return new ExtendedConfiguration.Builder("userName", "password", driver)
                .with($ -> $.maxWaitMillis = 1000)
                .with($ -> $.maxConnLifetimeMillis = 2000)
                .with($ -> $.maxTotal = 1500)
                .build();
    }

    protected void configureDataSource(final ConfigurationContext context) {

        final ExtendedConfiguration configuration = (ExtendedConfiguration) createDataSourceConfiguraton(context);

        dataSource.setDriver(configuration.getDriver());
        dataSource.setMaxWaitMillis(configuration.getMaxWaitMillis());
        dataSource.setMaxTotal(configuration.getMaxTotal());
        dataSource.setMinIdle(configuration.getMinIdle());
        dataSource.setMaxIdle(configuration.getMaxIdle());
        dataSource.setMaxConnLifetimeMillis(configuration.getMaxConnLifetimeMillis());
        dataSource.setTimeBetweenEvictionRunsMillis(configuration.getTimeBetweenEvictionRunsMillis());
        dataSource.setMinEvictableIdleTimeMillis(configuration.getMinEvictableIdleTimeMillis());
        dataSource.setSoftMinEvictableIdleTimeMillis(configuration.getSoftMinEvictableIdleTimeMillis());

        final String validationQuery = configuration.getValidationQuery();
        if (StringUtils.isNotBlank(validationQuery)) {
            dataSource.setValidationQuery(validationQuery);
            dataSource.setTestOnBorrow(true);
        }

        dataSource.setUrl(configuration.getUrl());
        dataSource.setUsername(configuration.getUserName());
        dataSource.setPassword(configuration.getPassword());

        configureDataSourceWithDynamicProperties(context);

        getConnectionProperties(context).forEach(dataSource::addConnectionProperty);
    }

    protected void configureDataSourceWithDynamicProperties(final ConfigurationContext context) {
    }

    protected Map<String, String> getConnectionProperties(final ConfigurationContext context) {
        return new HashMap<>();
    }

    protected KerberosUser getKerberosUser(final ConfigurationContext context) {
        KerberosUser kerberosUser = null;
        final KerberosCredentialsService kerberosCredentialsService = context.getProperty(KERBEROS_CREDENTIALS_SERVICE).asControllerService(KerberosCredentialsService.class);
        final KerberosUserService kerberosUserService = context.getProperty(KERBEROS_USER_SERVICE).asControllerService(KerberosUserService.class);
        final String kerberosPrincipal = context.getProperty(KERBEROS_PRINCIPAL).evaluateAttributeExpressions().getValue();
        final String kerberosPassword = context.getProperty(KERBEROS_PASSWORD).getValue();

        if (kerberosUserService != null) {
            kerberosUser = kerberosUserService.createKerberosUser();
        } else if (kerberosCredentialsService != null) {
            kerberosUser = new KerberosKeytabUser(kerberosCredentialsService.getPrincipal(), kerberosCredentialsService.getKeytab());
        } else if (!StringUtils.isBlank(kerberosPrincipal) && !StringUtils.isBlank(kerberosPassword)) {
            kerberosUser = new KerberosPasswordUser(kerberosPrincipal, kerberosPassword);
        }
        return kerberosUser;
    }

    @Override
    public Connection getConnection() throws ProcessException {
        return getConnection(dataSource, kerberosUser);
    }

    private Connection getConnection(final BasicDataSource dataSource, final KerberosUser kerberosUser) {
        try {
            final Connection con;
            if (kerberosUser != null) {
                KerberosAction<Connection> kerberosAction = new KerberosAction<>(kerberosUser, dataSource::getConnection, getLogger());
                con = kerberosAction.execute();
            } else {
                con = dataSource.getConnection();
            }
            return con;
        } catch (final SQLException e) {
            // If using Kerberos,  attempt to re-login
            if (kerberosUser != null) {
                try {
                    getLogger().info("Error getting connection, performing Kerberos re-login");
                    kerberosUser.login();
                } catch (KerberosLoginException le) {
                    throw new ProcessException("Unable to authenticate Kerberos principal", le);
                }
            }
            throw new ProcessException(e);
        }
    }

    /**
     * Shutdown pool, close all open connections.
     * If a principal is authenticated with a KDC, that principal is logged out.
     *
     * @throws SQLException if there is an error while closing open connections
     */
    @OnDisabled
    public void shutdown() throws SQLException {
        try {
            shutdown(dataSource, kerberosUser);
        } finally {
            kerberosUser = null;
            dataSource = null;
        }
    }

    private void shutdown(final BasicDataSource dataSource, final KerberosUser kerberosUser) throws SQLException {
        try {
            if (kerberosUser != null) {
                kerberosUser.logout();
            }
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }


}
