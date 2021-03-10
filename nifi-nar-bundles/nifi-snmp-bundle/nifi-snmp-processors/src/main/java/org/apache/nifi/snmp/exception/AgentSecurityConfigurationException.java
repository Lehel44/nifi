package org.apache.nifi.snmp.exception;

public class AgentSecurityConfigurationException extends RuntimeException {

    public AgentSecurityConfigurationException(String errorMessage) {
        super(errorMessage);
    }
}
