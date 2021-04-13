package org.apache.nifi.snmp.operations;

import org.apache.nifi.snmp.configuration.TargetConfiguration;
import org.apache.nifi.snmp.context.SNMPClientFactory;
import org.apache.nifi.snmp.context.TargetFactory;
import org.snmp4j.Snmp;
import org.snmp4j.Target;

public final class SNMPRequestHandlerFactory {

    public static SNMPRequestHandler createStandardRequestHandler(final TargetConfiguration configuration, final String clientPort) {
        final Snmp snmpClient = SNMPClientFactory.createSnmpClient(configuration, clientPort);
        final Target target = TargetFactory.createTarget(configuration);
        return new StandardSNMPRequestHandler(snmpClient, target);
    }

    private SNMPRequestHandlerFactory() {
    }

}
