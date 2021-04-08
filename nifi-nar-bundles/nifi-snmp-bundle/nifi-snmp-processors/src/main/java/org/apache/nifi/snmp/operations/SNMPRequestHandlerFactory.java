package org.apache.nifi.snmp.operations;

import org.apache.nifi.snmp.configuration.TargetConfiguration;
import org.apache.nifi.snmp.context.SNMPClientFactory;
import org.apache.nifi.snmp.context.TargetFactory;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;

public class SNMPRequestHandlerFactory {

    public static SNMPRequestHandler createStandardRequestHandler(TargetConfiguration configuration, String clientPort) {
        Snmp snmpClient = SNMPClientFactory.createSnmpClient(configuration, clientPort);
        Target target = TargetFactory.createTarget(configuration);
        PDUFactory pduFactory = new DefaultPDUFactory();
        return new StandardSNMPRequestHandler(snmpClient, target, pduFactory);
    }

}
