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
package org.apache.nifi.snmp.configuration;

import org.apache.nifi.snmp.utils.SNMPVersion;

public class TargetConfiguration {

    private final String agentHost;
    private final String agentPort;
    private final int retries;
    private final int timeout;
    private final SNMPVersion version;
    private final String authProtocol;
    private final String authPassword;
    private final String privacyProtocol;
    private final String privacyPassword;
    private final String securityName;
    private final String securityLevel;
    private final String communityString;

    TargetConfiguration(final String agentHost,
                        final String agentPort,
                        final int retries,
                        final int timeout,
                        final SNMPVersion version,
                        final String authProtocol,
                        final String authPassword,
                        final String privacyProtocol,
                        final String privacyPassword,
                        final String securityName,
                        final String securityLevel,
                        final String communityString) {
        this.agentHost = agentHost;
        this.agentPort = agentPort;
        this.retries = retries;
        this.timeout = timeout;
        this.version = version;
        this.authProtocol = authProtocol;
        this.authPassword = authPassword;
        this.privacyProtocol = privacyProtocol;
        this.privacyPassword = privacyPassword;
        this.securityName = securityName;
        this.securityLevel = securityLevel;
        this.communityString = communityString;
    }

    public String getAgentHost() {
        return agentHost;
    }

    public String getAgentPort() {
        return agentPort;
    }

    public int getRetries() {
        return retries;
    }

    public int getTimeout() {
        return timeout;
    }

    public SNMPVersion getVersion() {
        return version;
    }

    public String getAuthProtocol() {
        return authProtocol;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public String getPrivacyProtocol() {
        return privacyProtocol;
    }

    public String getPrivacyPassword() {
        return privacyPassword;
    }

    public String getSecurityName() {
        return securityName;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public String getCommunityString() {
        return communityString;
    }
}
