package org.apache.nifi.dbcp;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.resource.ResourceCardinality;
import org.apache.nifi.components.resource.ResourceType;
import org.apache.nifi.expression.AttributeExpression;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.kerberos.KerberosCredentialsService;
import org.apache.nifi.kerberos.KerberosUserService;
import org.apache.nifi.processor.util.StandardValidators;

public final class OptionalDBCPProperties {

    /**
     * Copied from {@link GenericObjectPoolConfig#DEFAULT_MIN_IDLE} in Commons-DBCP 2.7.0
     */
    protected static final String DEFAULT_MIN_IDLE = "0";
    /**
     * Copied from {@link GenericObjectPoolConfig#DEFAULT_MAX_IDLE} in Commons-DBCP 2.7.0
     */
    protected static final String DEFAULT_MAX_TOTAL_CONNECTIONS = "8";

    protected static final String DEFAULT_MAX_IDLE = DEFAULT_MAX_TOTAL_CONNECTIONS;
    /**
     * Copied from private variable {@link BasicDataSource#maxConnLifetimeMillis} in Commons-DBCP 2.7.0
     */
    protected static final String DEFAULT_MAX_CONN_LIFETIME = "-1";
    /**
     * Copied from {@link GenericObjectPoolConfig#DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS} in Commons-DBCP 2.7.0
     */
    protected static final String DEFAULT_EVICTION_RUN_PERIOD = String.valueOf(-1L);
    /**
     * Copied from {@link GenericObjectPoolConfig#DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS} in Commons-DBCP 2.7.0
     * and converted from 1800000L to "1800000 millis" to "30 mins"
     */
    protected static final String DEFAULT_MIN_EVICTABLE_IDLE_TIME_MINS = "30 mins";
    /**
     * Copied from {@link GenericObjectPoolConfig#DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS} in Commons-DBCP 2.7.0
     */
    protected static final String DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME = String.valueOf(-1L);
    protected static final String DEFAULT_MAX_WAIT_MILLIS = "500 millis";

    public static final PropertyDescriptor DB_DRIVERNAME = new PropertyDescriptor.Builder()
            .name("Database Driver Class Name")
            .description("Database driver class name")
            .defaultValue(null)
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor DB_DRIVER_LOCATION = new PropertyDescriptor.Builder()
            .name("database-driver-locations")
            .displayName("Database Driver Location(s)")
            .description("Comma-separated list of files/folders and/or URLs containing the driver JAR and its dependencies (if any). For example '/var/tmp/mariadb-java-client-1.1.7.jar'")
            .defaultValue(null)
            .required(false)
            .identifiesExternalResource(ResourceCardinality.MULTIPLE, ResourceType.FILE, ResourceType.DIRECTORY, ResourceType.URL)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .dynamicallyModifiesClasspath(true)
            .build();

    public static final PropertyDescriptor MAX_WAIT_TIME = new PropertyDescriptor.Builder()
            .name("Max Wait Time")
            .description("The maximum amount of time that the pool will wait (when there are no available connections) "
                    + " for a connection to be returned before failing, or -1 to wait indefinitely. ")
            .defaultValue(DEFAULT_MAX_WAIT_MILLIS)
            .required(true)
            .addValidator(DBCPValidator.CUSTOM_TIME_PERIOD_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .sensitive(false)
            .build();

    public static final PropertyDescriptor MAX_TOTAL_CONNECTIONS = new PropertyDescriptor.Builder()
            .name("Max Total Connections")
            .description("The maximum number of active connections that can be allocated from this pool at the same time, "
                    + " or negative for no limit.")
            .defaultValue(DEFAULT_MAX_TOTAL_CONNECTIONS)
            .required(true)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .sensitive(false)
            .build();

    public static final PropertyDescriptor VALIDATION_QUERY = new PropertyDescriptor.Builder()
            .name("Validation-query")
            .displayName("Validation query")
            .description("Validation query used to validate connections before returning them. "
                    + "When connection is invalid, it gets dropped and new valid connection will be returned. "
                    + "Note!! Using validation might have some performance penalty.")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor MIN_IDLE = new PropertyDescriptor.Builder()
            .displayName("Minimum Idle Connections")
            .name("dbcp-min-idle-conns")
            .description("The minimum number of connections that can remain idle in the pool without extra ones being " +
                    "created. Set to or zero to allow no idle connections.")
            .defaultValue(DEFAULT_MIN_IDLE)
            .required(false)
            .addValidator(StandardValidators.NON_NEGATIVE_INTEGER_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor MAX_IDLE = new PropertyDescriptor.Builder()
            .displayName("Max Idle Connections")
            .name("dbcp-max-idle-conns")
            .description("The maximum number of connections that can remain idle in the pool without extra ones being " +
                    "released. Set to any negative value to allow unlimited idle connections.")
            .defaultValue(DEFAULT_MAX_IDLE)
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor MAX_CONN_LIFETIME = new PropertyDescriptor.Builder()
            .displayName("Max Connection Lifetime")
            .name("dbcp-max-conn-lifetime")
            .description("The maximum lifetime in milliseconds of a connection. After this time is exceeded the " +
                    "connection will fail the next activation, passivation or validation test. A value of zero or less " +
                    "means the connection has an infinite lifetime.")
            .defaultValue(DEFAULT_MAX_CONN_LIFETIME)
            .required(false)
            .addValidator(DBCPValidator.CUSTOM_TIME_PERIOD_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor EVICTION_RUN_PERIOD = new PropertyDescriptor.Builder()
            .displayName("Time Between Eviction Runs")
            .name("dbcp-time-between-eviction-runs")
            .description("The number of milliseconds to sleep between runs of the idle connection evictor thread. When " +
                    "non-positive, no idle connection evictor thread will be run.")
            .defaultValue(DEFAULT_EVICTION_RUN_PERIOD)
            .required(false)
            .addValidator(DBCPValidator.CUSTOM_TIME_PERIOD_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor MIN_EVICTABLE_IDLE_TIME = new PropertyDescriptor.Builder()
            .displayName("Minimum Evictable Idle Time")
            .name("dbcp-min-evictable-idle-time")
            .description("The minimum amount of time a connection may sit idle in the pool before it is eligible for eviction.")
            .defaultValue(DEFAULT_MIN_EVICTABLE_IDLE_TIME_MINS)
            .required(false)
            .addValidator(DBCPValidator.CUSTOM_TIME_PERIOD_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor SOFT_MIN_EVICTABLE_IDLE_TIME = new PropertyDescriptor.Builder()
            .displayName("Soft Minimum Evictable Idle Time")
            .name("dbcp-soft-min-evictable-idle-time")
            .description("The minimum amount of time a connection may sit idle in the pool before it is eligible for " +
                    "eviction by the idle connection evictor, with the extra condition that at least a minimum number of" +
                    " idle connections remain in the pool. When the not-soft version of this option is set to a positive" +
                    " value, it is examined first by the idle connection evictor: when idle connections are visited by " +
                    "the evictor, idle time is first compared against it (without considering the number of idle " +
                    "connections in the pool) and then against this soft option, including the minimum idle connections " +
                    "constraint.")
            .defaultValue(DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME)
            .required(false)
            .addValidator(DBCPValidator.CUSTOM_TIME_PERIOD_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor KERBEROS_CREDENTIALS_SERVICE = new PropertyDescriptor.Builder()
            .name("kerberos-credentials-service")
            .displayName("Kerberos Credentials Service")
            .description("Specifies the Kerberos Credentials Controller Service that should be used for authenticating with Kerberos")
            .identifiesControllerService(KerberosCredentialsService.class)
            .required(false)
            .build();

    public static final PropertyDescriptor KERBEROS_USER_SERVICE = new PropertyDescriptor.Builder()
            .name("kerberos-user-service")
            .displayName("Kerberos User Service")
            .description("Specifies the Kerberos User Controller Service that should be used for authenticating with Kerberos")
            .identifiesControllerService(KerberosUserService.class)
            .required(false)
            .build();

    public static final PropertyDescriptor KERBEROS_PRINCIPAL = new PropertyDescriptor.Builder()
            .name("kerberos-principal")
            .displayName("Kerberos Principal")
            .description("The principal to use when specifying the principal and password directly in the processor for authenticating via Kerberos.")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .addValidator(StandardValidators.createAttributeExpressionLanguageValidator(AttributeExpression.ResultType.STRING))
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor KERBEROS_PASSWORD = new PropertyDescriptor.Builder()
            .name("kerberos-password")
            .displayName("Kerberos Password")
            .description("The password to use when specifying the principal and password directly in the processor for authenticating via Kerberos.")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .sensitive(true)
            .build();
}
