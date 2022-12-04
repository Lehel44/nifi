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

import java.util.Arrays;
import org.apache.nifi.components.DescribedValue;

public enum PostgresDriver implements DescribedValue {

    JDBC_DRIVER_42_5_1("postgres-driver-from-8.2.jar",
            "PostgreSQL 8.2 and newer",
            "PostgreSQL JDBC 4.2 (JRE 8+) Driver 42.5.1 for PostgreSQL 8.2 and newer versions",
            "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.5.1/postgresql-42.5.1.jar",
            "42.5.1"),
    JDBC_DRIVER_9_4_1212("postgres-driver-to-8.2.jar",
            "PostgreSQL older than 8.2",
            "Java JDBC 4.2 (JRE 8+) driver 9.4.1212 for PostgreSQL databases older than 8.2",
            "https://repo1.maven.org/maven2/org/postgresql/postgresql/9.4.1212/postgresql-9.4.1212.jar", "9.4.1212");

    private final String value;
    private final String displayName;
    private final String description;
    private final String url;
    private final String version;
    PostgresDriver(String value, String displayName, String description, String url, String version) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
        this.url = url;
        this.version = version;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getVersion() {
        return version;
    }

    public static PostgresDriver forName(final String value) {
        return Arrays.stream(PostgresDriver.values())
                .filter(s -> s.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid PostgresDriver type: " + value));
    }
}
