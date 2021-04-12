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

public class TargetConfigurationBuilder {
    private String agentHost;
    private String agentPort;
    private int retries;
    private int timeout;
    private int version;
    private String authProtocol;
    private String authPassword;
    private String privacyProtocol;
    private String privacyPassword;
    private String securityName;
    private String securityLevel;
    private String communityString;

    public TargetConfigurationBuilder setAgentHost(String agentHost) {
        this.agentHost = agentHost;
        return this;
    }

    public TargetConfigurationBuilder setAgentPort(String agentPort) {
        this.agentPort = agentPort;
        return this;
    }

    public TargetConfigurationBuilder setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public TargetConfigurationBuilder setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public TargetConfigurationBuilder setVersion(int version) {
        this.version = version;
        return this;
    }

    public TargetConfigurationBuilder setAuthProtocol(String authProtocol) {
        this.authProtocol = authProtocol;
        return this;
    }

    public TargetConfigurationBuilder setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
        return this;
    }

    public TargetConfigurationBuilder setPrivacyProtocol(String privacyProtocol) {
        this.privacyProtocol = privacyProtocol;
        return this;
    }

    public TargetConfigurationBuilder setPrivacyPassword(String privacyPassword) {
        this.privacyPassword = privacyPassword;
        return this;
    }

    public TargetConfigurationBuilder setSecurityName(String securityName) {
        this.securityName = securityName;
        return this;
    }

    public TargetConfigurationBuilder setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
        return this;
    }

    public TargetConfigurationBuilder setCommunityString(String communityString) {
        this.communityString = communityString;
        return this;
    }

    public TargetConfiguration build() {
        validate();
        return new TargetConfiguration(agentHost, agentPort, retries, timeout, version, authProtocol, authPassword, privacyProtocol, privacyPassword, securityName, securityLevel, communityString);
    }

    private void validate() {
        boolean isValid = agentHost != null && agentPort != null && securityLevel != null;
        if (!isValid) {
            throw new IllegalStateException("Required properties are not set.");
        }
    }
}