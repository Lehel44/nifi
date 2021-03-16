package org.apache.nifi.snmp.processors;

import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.snmp.operations.SNMPTrapReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.*;

public class ListenTrapSNMP extends AbstractSNMPProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenTrapSNMP.class);

    public static final PropertyDescriptor listenHostname = new PropertyDescriptor.Builder()
            .name("snmp-trap-listen-hostname")
            .displayName("The hostname of the SNMP trap listener.")
            .description("The host where the processor listens to the trap messages")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor listenPort = new PropertyDescriptor.Builder()
            .name("snmp-trap-listen-port")
            .displayName("The port of the SNMP trap listener.")
            .description("The host where the processor listens to the trap messages")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("All FlowFiles that are received from the SNMP agent are routed to this relationship")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("All FlowFiles that cannot received from the SNMP agent are routed to this relationship")
            .build();

    private static final Set<Relationship> RELATIONSHIPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            REL_SUCCESS,
            REL_FAILURE
    )));

    private SNMPTrapReceiver trapReceiver;

    @OnScheduled
    @Override
    public void initSnmpClient(ProcessContext context) {
        String host = context.getProperty(listenHostname).toString();
        String port = context.getProperty(listenPort).toString();
        Snmp snmp = snmpContext.getSnmp();
        try {
            snmp.addTransportMapping(new DefaultUdpTransportMapping(new UdpAddress(host + "/" + port)));
            trapReceiver = new SNMPTrapReceiver(snmp);
        } catch (IOException e) {
            LOGGER.error("Could not initialize SNMP client.", e);
        }
    }

    @OnStopped
    public void stopSnmpClient(ProcessContext context) {
        try {
            trapReceiver.closeReceiver(snmpContext.getSnmp());
        } catch (IOException e) {
            LOGGER.error("Could not close trap receiver and SNMP client.", e);
        }
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) {
        // The processor passively listens to traps. See SnmpTrapReceiver::processPDU.
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return BASIC_PROPERTIES;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS;
    }
}
