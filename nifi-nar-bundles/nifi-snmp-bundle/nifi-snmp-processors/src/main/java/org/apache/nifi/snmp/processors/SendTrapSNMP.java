package org.apache.nifi.snmp.processors;

import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Processor;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.snmp.operations.SNMPTrapSender;

import java.util.*;

public class SendTrapSNMP extends AbstractSNMPProcessor {

    public static final PropertyDescriptor enterpriseOID = new PropertyDescriptor.Builder()
            .name("snmp-trap-enterprise-oid")
            .displayName("Enterprise OID")
            .description("Enterprise is the vendor identification (OID) for the network management sub-system that generated the trap")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor agentAddress = new PropertyDescriptor.Builder()
            .name("snmp-trap-agent-address")
            .displayName("Agent IP address")
            .description("The sender IP address")
            .required(false)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .build();

    public static final PropertyDescriptor genericTrapType = new PropertyDescriptor.Builder()
            .name("snmp-trap-generic-type")
            .displayName("Generic trap type")
            .description("Generic trap type is an integer in the range of 0 to 6. See Usage for details.")
            .required(true)
            .allowableValues("0", "1", "2", "3", "4", "5", "6")
            .build();

    public static final PropertyDescriptor specificTrapType = new PropertyDescriptor.Builder()
            .name("snmp-trap-specific-type")
            .displayName("Specific trap type")
            .description("Specific trap type is a number that further specifies the nature of the event that generated " +
                    "the trap in the case of traps of generic type 6 (enterpriseSpecific). The interpretation of this " +
                    "code is vendor-specific")
            .required(false)
            .addValidator(StandardValidators.INTEGER_VALIDATOR)
            .build();

    public static final PropertyDescriptor trapOID = new PropertyDescriptor.Builder()
            .name("snmp-trap-oid")
            .displayName("Trap OID")
            .description("The authoritative identification of the notification currently being sent")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor trapOIDValue = new PropertyDescriptor.Builder()
            .name("snmp-trap-oid-value")
            .displayName("Trap OID Value")
            .description("The value of the trap OID")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor managerAddress = new PropertyDescriptor.Builder()
            .name("snmp-trap-manager-address")
            .displayName("Manager (destination) IP address")
            .description("The address of the SNMP manager where the trap is being sent to.")
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

    private static final List<PropertyDescriptor> PROPERTY_DESCRIPTORS = createPropertyList();

    private static final Set<Relationship> RELATIONSHIPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            REL_SUCCESS,
            REL_FAILURE
    )));

    private SNMPTrapSender snmpTrapSender;

    @OnScheduled
    @Override
    public void initSnmpClient(ProcessContext context) {
        super.initSnmpClient(context);
        snmpTrapSender = new SNMPTrapSender(snmpContext.getSnmp(), snmpContext.getTarget());
    }

    /**
     * Delegate method to supplement
     * {@link #onTrigger(ProcessContext, ProcessSession)}. It is implemented by
     * sub-classes to perform {@link Processor} specific functionality.
     *
     * @param context        instance of {@link ProcessContext}
     * @param processSession instance of {@link ProcessSession}
     * @throws ProcessException Process exception
     */
    @Override
    public void onTrigger(ProcessContext context, ProcessSession processSession) {
        final String enterpriseOIDValue = context.getProperty(enterpriseOID).getValue();
        final String agentAddressValue = context.getProperty(agentAddress).getValue();
        final int genericTrapTypeValue = Integer.parseInt(context.getProperty(genericTrapType).getValue());
        final int specificTrapTypeValue = Integer.parseInt(context.getProperty(specificTrapType).getValue());
        final String trapOIDKey = context.getProperty(trapOID).getValue();
        final String managerAddressValue = context.getProperty(managerAddress).getValue();
        final String trapOIDValueValue = context.getProperty(trapOIDValue).getValue();

        snmpTrapSender.generateTrap(enterpriseOIDValue, agentAddressValue, genericTrapTypeValue, specificTrapTypeValue,
                trapOIDKey, managerAddressValue, trapOIDValueValue);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTY_DESCRIPTORS;
    }

    @Override
    public Set<Relationship> getRelationships() {
        return RELATIONSHIPS;
    }

    /**
     * Creates a list of the base class' and the current properties.
     *
     * @return a list of properties
     */
    private static List<PropertyDescriptor> createPropertyList() {
        List<PropertyDescriptor> propertyDescriptors = new ArrayList<>();
        propertyDescriptors.addAll(BASIC_PROPERTIES);
        propertyDescriptors.addAll(Arrays.asList(enterpriseOID, agentAddress, genericTrapType, specificTrapType,
                trapOID, managerAddress, trapOIDValue));
        return Collections.unmodifiableList(propertyDescriptors);
    }

}
