package org.apache.nifi.snmp.testagents;

import org.apache.nifi.snmp.helper.SNMPTestUtil;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.snmp.*;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.log.ConsoleLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.*;
import org.snmp4j.transport.TransportMappings;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class TestSnmpV2Agent extends BaseAgent {

    static {
        LogFactory.setLogFactory(new ConsoleLogFactory());
        //ConsoleLogAdapter.setDebugEnabled(true);
    }

    /**
     * address
     */
    private String address;
    /**
     * port
     */
    private int port;

    /**
     * constructor
     *
     * @param address address
     * @throws IOException IO Exception
     */
    public TestSnmpV2Agent(String address) throws IOException {
        super(new File("target/bootCounter2.agent"), new File("target/conf2.agent"),
                new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
        port = SNMPTestUtil.availablePort();
        this.address = address + "/" + port;
    }

    @Override
    protected void initTransportMappings() throws IOException {
        transportMappings = new TransportMapping[1];
        Address transportAddress = GenericAddress.parse(address);
        TransportMapping<? extends Address> transportMapping = TransportMappings.getInstance().createTransportMapping(transportAddress);
        transportMappings[0] = transportMapping;
    }

    public void start() throws IOException {
        init();
        addShutdownHook();
        getServer().addContext(new OctetString("public"));
        finishInit();
        run();
        sendColdStartNotification();
    }

    @Override
    protected void registerManagedObjects() {
    }

    public void registerManagedObject(ManagedObject mo) {
        try {
            server.register(mo, null);
        } catch (DuplicateRegistrationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void registerManagedObjects(ManagedObject... managedObjects) {
        Arrays.stream(managedObjects).forEach(this::registerManagedObject);
    }

    @Override
    protected void unregisterManagedObjects() {
    }

    protected void unregisterManagedObject(MOGroup moGroup) {
        moGroup.unregisterMOs(server, getContext(moGroup));
    }

    @Override
    protected void addUsmUser(USM usm) {

    }

    @Override
    protected void addNotificationTargets(SnmpTargetMIB targetMIB, SnmpNotificationMIB notificationMIB) {

    }

    @Override
    protected void addViews(VacmMIB vacmMIB) {
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c,
                new OctetString("cpublic"),
                new OctetString("v1v2group"),
                StorageType.nonVolatile);

        vacmMIB.addAccess(new OctetString("v1v2group"),
                new OctetString("public"),
                SecurityModel.SECURITY_MODEL_ANY,
                SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT,
                new OctetString("fullReadView"),
                new OctetString("fullWriteView"),
                new OctetString("fullNotifyView"),
                StorageType.nonVolatile);

        vacmMIB.addViewTreeFamily(new OctetString("fullReadView"),
                new OID("1.3"),
                new OctetString(),
                VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("fullWriteView"),
                new OID("1.3"),
                new OctetString(),
                VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("fullNotifyView"),
                new OID("1.3"),
                new OctetString(),
                VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
    }

    @Override
    protected void addCommunities(SnmpCommunityMIB communityMIB) {
        Variable[] com2sec = new Variable[]{
                new OctetString("public"), // community name
                new OctetString("cpublic"), // security name
                this.getAgent().getContextEngineID(), // local engine ID
                new OctetString("public"), // default context name
                new OctetString(), // transport tag
                new Integer32(StorageType.nonVolatile), // storage type
                new Integer32(RowStatus.active) // row status
        };
        final SnmpCommunityMIB.SnmpCommunityEntryRow row = communityMIB.getSnmpCommunityEntry().createRow(new OctetString("public2public")
                .toSubIndex(true), com2sec);
        communityMIB.getSnmpCommunityEntry().addRow(row);
    }

    public int getPort() {
        return port;
    }

}
