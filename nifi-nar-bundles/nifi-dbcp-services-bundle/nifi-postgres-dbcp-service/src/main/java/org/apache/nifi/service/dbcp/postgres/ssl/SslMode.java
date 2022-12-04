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
package org.apache.nifi.service.dbcp.postgres.ssl;

import org.apache.nifi.components.DescribedValue;

import java.util.Arrays;

public enum SslMode implements DescribedValue {

    DISABLE(org.postgresql.jdbc.SslMode.DISABLE.value, org.postgresql.jdbc.SslMode.DISABLE.name(),
            "No security and no overhead for encryption."),
    ALLOW(org.postgresql.jdbc.SslMode.ALLOW.value, org.postgresql.jdbc.SslMode.ALLOW.name(),
            "Security and encryption with overhead if the server requires."),
    PREFER(
            org.postgresql.jdbc.SslMode.PREFER.value, org.postgresql.jdbc.SslMode.PREFER.name(),
            "Security and encryption if the server supports it."),
    REQUIRE(org.postgresql.jdbc.SslMode.REQUIRE.value, org.postgresql.jdbc.SslMode.REQUIRE.name(),
            "Security and encryption without guaranty that the network will always connect to the right server."),
    VERIFY_CA(org.postgresql.jdbc.SslMode.VERIFY_CA.value, org.postgresql.jdbc.SslMode.VERIFY_CA.name(),
            "Security and encryption with guaranty for connecting to a trusted server."),
    VERIFY_FULL(org.postgresql.jdbc.SslMode.VERIFY_FULL.value, org.postgresql.jdbc.SslMode.VERIFY_FULL.name(),
            "Security and encryption with guaranty to connect to the specified server and that it's trusted.");

    private final String value;
    private final String displayName;
    private final String description;

    SslMode(final String value, final String displayName, final String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
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

    public static SslMode forName(final String value) {
        return Arrays.stream(SslMode.values())
                .filter(s -> s.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid SSLMode type: " + value));
    }
}
