package org.apache.nifi.snmp.processors;

import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.lifecycle.OnStopped;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessSessionFactory;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.snmp.operations.SNMPGetter;
import org.apache.nifi.snmp.operations.SNMPTrapReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ListenTrapSNMP extends AbstractSNMPProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenTrapSNMP.class);

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("All FlowFiles that are received from the SNMP agent are routed to this relationship")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("All FlowFiles that cannot received from the SNMP agent are routed to this relationship")
            .build();

    private static final List<PropertyDescriptor> PROPERTY_DESCRIPTORS = createPropertyList();

    private static final Set<Relationship> RELATIONSHIPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            REL_SUCCESS,
            REL_FAILURE
    )));

    private SNMPTrapReceiver trapReceiver;

    @OnScheduled
    @Override
    public void initSnmpClient(ProcessContext context) {
        super.initSnmpClient(context);
    }

    @OnStopped
    public void stopSnmpClient(ProcessContext context) {
        snmpContext.close();
        trapReceiver = null;
    }

    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) {
        if (Objects.isNull(trapReceiver)) {
            try {
                trapReceiver = new SNMPTrapReceiver(snmpContext.getSnmp(), context, session, getLogger());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTY_DESCRIPTORS;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS;
    }

    private static List<PropertyDescriptor> createPropertyList() {
        List<PropertyDescriptor> propertyDescriptors = new ArrayList<>();
        propertyDescriptors.addAll(BASIC_PROPERTIES);
        //propertyDescriptors.addAll(Arrays.asList(LISTEN_HOSTNAME, LISTEN_PORT));
        return Collections.unmodifiableList(propertyDescriptors);
    }
}
