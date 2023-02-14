package org.apache.nifi.dbcp.builder;

import java.sql.Driver;

public class ExtendedConfiguration extends Configuration {

    private final long maxWaitMillis;
    private final int maxTotal;
    private final int minIdle;
    private final int maxIdle;
    private final long maxConnLifetimeMillis;
    private final long timeBetweenEvictionRunsMillis;
    private final long minEvictableIdleTimeMillis;
    private final long softMinEvictableIdleTimeMillis;
    private final String validationQuery;

    public ExtendedConfiguration(Builder builder) {
        super(builder);
        this.maxWaitMillis = builder.maxWaitMillis;
        this.maxTotal = builder.maxTotal;
        this.minIdle = builder.minIdle;
        this.maxIdle = builder.maxIdle;
        this.maxConnLifetimeMillis = builder.maxConnLifetimeMillis;
        this.timeBetweenEvictionRunsMillis = builder.timeBetweenEvictionRunsMillis;
        this.minEvictableIdleTimeMillis = builder.minEvictableIdleTimeMillis;
        this.softMinEvictableIdleTimeMillis = builder.softMinEvictableIdleTimeMillis;
        this.validationQuery = builder.validationQuery;
    }

    public static class Builder extends Configuration.Builder<Builder> {

        public long maxWaitMillis;
        public int maxTotal;
        public int minIdle;
        public int maxIdle;
        public long maxConnLifetimeMillis;
        public long timeBetweenEvictionRunsMillis;
        public long minEvictableIdleTimeMillis;
        public long softMinEvictableIdleTimeMillis;
        public String validationQuery;

        public Builder(String userName, String password, Driver driver) {
            super(userName, password, driver);
        }

        @Override
        public Configuration build() {
            return null;
        }

        @Override
        Builder self() {
            return this;
        }
    }

    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public long getMaxConnLifetimeMillis() {
        return maxConnLifetimeMillis;
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public long getSoftMinEvictableIdleTimeMillis() {
        return softMinEvictableIdleTimeMillis;
    }

    public String getValidationQuery() {
        return validationQuery;
    }
}
