package org.apache.nifi.dbcp;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.nifi.controller.ConfigurationContext;

public class BasicDataSourceBuilder {

    /**
     * Copied from {@link GenericObjectPoolConfig#DEFAULT_MIN_IDLE} in Commons-DBCP 2.7.0
     */
    private static final String DEFAULT_MIN_IDLE = "0";
    /**
     * Copied from {@link GenericObjectPoolConfig#DEFAULT_MAX_IDLE} in Commons-DBCP 2.7.0
     */
    private static final String DEFAULT_MAX_TOTAL_CONNECTIONS = "8";

    private static final String DEFAULT_MAX_IDLE = DEFAULT_MAX_TOTAL_CONNECTIONS;
    /**
     * Copied from private variable {@link BasicDataSource#maxConnLifetimeMillis} in Commons-DBCP 2.7.0
     */
    private static final String DEFAULT_MAX_CONN_LIFETIME = "-1";
    /**
     * Copied from {@link GenericObjectPoolConfig#DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS} in Commons-DBCP 2.7.0
     */
    private static final String DEFAULT_EVICTION_RUN_PERIOD = String.valueOf(-1L);
    /**
     * Copied from {@link GenericObjectPoolConfig#DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS} in Commons-DBCP 2.7.0
     * and converted from 1800000L to "1800000 millis" to "30 mins"
     */
    protected static final String DEFAULT_MIN_EVICTABLE_IDLE_TIME_MINS = "30 mins";
    /**
     * Copied from {@link GenericObjectPoolConfig#DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS} in Commons-DBCP 2.7.0
     */
    private static final String DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME = String.valueOf(-1L);
    private static final String DEFAULT_MAX_WAIT_MILLIS = "500 millis";

    private final ConfigurationContext context;

}
