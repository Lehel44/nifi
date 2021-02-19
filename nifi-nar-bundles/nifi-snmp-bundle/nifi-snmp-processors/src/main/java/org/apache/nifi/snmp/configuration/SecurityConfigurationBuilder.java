package org.apache.nifi.snmp.configuration;

public class SecurityConfigurationBuilder {
    private String version;
    private String authProtocol;
    private String authPassword;
    private String privacyProtocol;
    private String privacyPassword;
    private String securityName;
    private String securityLevel;
    private String communityString;

    public SecurityConfigurationBuilder setVersion(String version) {
        this.version = version;
        return this;
    }

    public SecurityConfigurationBuilder setAuthProtocol(String authProtocol) {
        this.authProtocol = authProtocol;
        return this;
    }

    public SecurityConfigurationBuilder setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
        return this;
    }

    public SecurityConfigurationBuilder setPrivacyProtocol(String privacyProtocol) {
        this.privacyProtocol = privacyProtocol;
        return this;
    }

    public SecurityConfigurationBuilder setPrivacyPassword(String privacyPassword) {
        this.privacyPassword = privacyPassword;
        return this;
    }

    public SecurityConfigurationBuilder setSecurityName(String securityName) {
        this.securityName = securityName;
        return this;
    }

    public SecurityConfigurationBuilder setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
        return this;
    }

    public SecurityConfigurationBuilder setCommunityString(String communityString) {
        this.communityString = communityString;
        return this;
    }

    public SecurityConfiguration createSecurityConfiguration() {
        return new SecurityConfiguration(version, authProtocol, authPassword, privacyProtocol, privacyPassword, securityName, securityLevel, communityString);
    }
}