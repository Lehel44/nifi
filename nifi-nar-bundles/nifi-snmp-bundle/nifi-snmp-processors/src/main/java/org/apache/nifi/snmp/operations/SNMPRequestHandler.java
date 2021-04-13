package org.apache.nifi.snmp.operations;

import org.apache.nifi.snmp.configuration.TrapConfiguration;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.util.TreeEvent;

import java.io.IOException;
import java.util.List;

public interface SNMPRequestHandler {

    ResponseEvent get(final OID oid);

    List<TreeEvent> walk(final OID oid);

    ResponseEvent set(final PDU pdu) throws IOException;

    void sendTrap(final TrapConfiguration configuration);

    void close();

    Target getTarget();

    Snmp getSnmpClient();
}
