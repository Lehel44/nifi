package org.apache.nifi.snmp.operations;

import org.snmp4j.PDU;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.util.TreeEvent;

import java.io.IOException;
import java.util.List;

public interface SNMPRequestHandler {

    ResponseEvent get(OID oid);

    List<TreeEvent> walk(OID oid);

    ResponseEvent set(PDU pdu) throws IOException;

    void close();

    Target getTarget();
}
