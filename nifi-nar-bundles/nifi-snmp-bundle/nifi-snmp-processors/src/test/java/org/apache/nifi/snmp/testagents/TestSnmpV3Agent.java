package org.apache.nifi.snmp.testagents;

import org.apache.nifi.snmp.helper.SNMPTestUtil;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.snmp.*;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.log.ConsoleLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.TransportMappings;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class TestSnmpV3Agent extends BaseAgent {

    static {
        LogFactory.setLogFactory(new ConsoleLogFactory());
        //ConsoleLogAdapter.setDebugEnabled(true);
    }

    private final String address;
    private final int port;

    public TestSnmpV3Agent(final String address) {
        super(new File("target/bootCounter3.agent"), new File("target/conf3.agent"),
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
        UsmUser user = new UsmUser(new OctetString("SHA"),
                AuthSHA.ID,
                new OctetString("SHAAuthPassword"),
                null,
                null);
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("SHADES"),
                AuthSHA.ID,
                new OctetString("SHADESAuthPassword"),
                PrivDES.ID,
                new OctetString("SHADESPrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("MD5DES"),
                AuthMD5.ID,
                new OctetString("MD5DESAuthPassword"),
                PrivDES.ID,
                new OctetString("MD5DESPrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
        user = new UsmUser(new OctetString("SHAAES128"),
                AuthSHA.ID,
                new OctetString("SHAAES128AuthPassword"),
                PrivAES128.ID,
                new OctetString("SHAAES128PrivPassword"));
        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
    }

    @Override
    protected void addNotificationTargets(SnmpTargetMIB targetMIB, SnmpNotificationMIB notificationMIB) {

    }

    @Override
    protected void addViews(VacmMIB vacmMIB) {
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("SHA"),
                new OctetString("v3-auth-no-priv-group"),
                StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("SHADES"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("MD5DES"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM,
                new OctetString("SHAAES128"),
                new OctetString("v3group"),
                StorageType.nonVolatile);
        vacmMIB.addAccess(new OctetString("v3group"), new OctetString(),
                SecurityModel.SECURITY_MODEL_USM,
                SecurityLevel.AUTH_PRIV,
                MutableVACM.VACM_MATCH_EXACT,
                new OctetString("fullReadView"),
                new OctetString("fullWriteView"),
                new OctetString("fullNotifyView"),
                StorageType.nonVolatile);
        vacmMIB.addAccess(new OctetString("v3-auth-no-priv-group"), new OctetString(),
                SecurityModel.SECURITY_MODEL_USM,
                SecurityLevel.AUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT,
                new OctetString("fullReadView"),
                new OctetString("fullWriteView"),
                new OctetString("fullNotifyView"),
                StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("fullNotifyView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
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
