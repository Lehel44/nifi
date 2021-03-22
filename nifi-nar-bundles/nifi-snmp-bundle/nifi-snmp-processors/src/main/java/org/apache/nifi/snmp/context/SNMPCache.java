package org.apache.nifi.snmp.context;

import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SNMPCache {

    private static final Map<String, Snmp> SNMP_ADDRESS_MAP = new HashMap<>();

    public static Snmp getOrCreate(String port) throws IOException {
        if (!SNMP_ADDRESS_MAP.containsKey(port)) {
            Snmp snmp = new Snmp(new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/" + port)));
            SNMP_ADDRESS_MAP.put(port, snmp);
            return snmp;
        } else {
            return SNMP_ADDRESS_MAP.get(port);
        }
    }

    private SNMPCache() {
    }

    public static void clearCache(String clientPort) {
        if (SNMP_ADDRESS_MAP.containsKey(clientPort)) {
            SNMP_ADDRESS_MAP.remove(clientPort);
        }
    }
}
