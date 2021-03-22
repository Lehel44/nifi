package org.apache.nifi.snmp.configuration;

public class BasicConfiguration {

    private final String clientPort;
    private final String agentHost;
    private final String agentPort;
    private final int retries;
    private final int timeout;

    public BasicConfiguration(final String clientPort, final String agentHost, final String agentPort, final int retries, final int timeout) {
        this.clientPort = clientPort;
        this.agentHost = agentHost;
        this.agentPort = agentPort;
        this.retries = retries;
        this.timeout = timeout;
    }

    public String getClientPort() {
        return clientPort;
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
}
