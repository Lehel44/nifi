package org.apache.nifi.snmp.configuration;

public class BasicConfiguration {

    private final int clientPort;
    private final String host;
    private final int port;
    private final int retries;
    private final int timeout;

    public BasicConfiguration(final int clientPort, final String host, final int port, final int retries, final int timeout) {
        this.clientPort = clientPort;
        this.host = host;
        this.port = port;
        this.retries = retries;
        this.timeout = timeout;
    }

    public int getClientPort() {
        return clientPort;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getRetries() {
        return retries;
    }

    public int getTimeout() {
        return timeout;
    }
}
