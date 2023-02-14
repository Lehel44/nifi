package org.apache.nifi.dbcp.builder;

import java.sql.Driver;
import java.util.function.Consumer;

public class Configuration {

    private final String userName;
    private final String password;
    private final Driver driver;

    Configuration(Builder builder) {
        userName = builder.userName;
        password = builder.password;
        driver = builder.driver;
    }

    abstract static class Builder<T extends Builder> {
        private final String userName;
        private final String password;
        private final Driver driver;

        Builder(String userName, String password, Driver driver) {
            this.userName = userName;
            this.password = password;
            this.driver = driver;
        }

        public T with(Consumer<T> consumer) {
            consumer.accept(self());
            return self();
        }

        abstract Configuration build();

        abstract T self();
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public Driver getDriver() {
        return driver;
    }
}
