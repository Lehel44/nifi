package org.apache.nifi.dbcp.builder;

import java.util.concurrent.TimeUnit;

public enum DefaultDataSourceValues {

    MAX_WAIT_MILLIS("500", TimeUnit.MILLISECONDS),
    MAX_TOTAL_CONNECTIONS("8", null),
    MIN_IDLE("0", null),
    MAX_IDLE("8", null),
    MAX_CONN_LIFETIME("-1", null),
    EVICTION_RUN_PERIOD("-1", null),
    MIN_EVICTABLE_IDLE_TIME_MINS("30", TimeUnit.MINUTES),
    SOFT_MIN_EVICTABLE_IDLE_TIME("-1", null);


    private final String value;
    private final TimeUnit timeUnit;

    DefaultDataSourceValues(String value, TimeUnit timeUnit) {
        this.value = value;
        this.timeUnit = timeUnit;
    }

    public int getIntValue() {
        return Integer.parseInt(value);
    }

    public long getLongValue() {
        return Long.parseLong(value);
    }

    public String getDurationString() {
        if (timeUnit == null) {
            return "This value is not time type.";
        }
        return String.format("%s %s", value, timeUnit);
    }
}
