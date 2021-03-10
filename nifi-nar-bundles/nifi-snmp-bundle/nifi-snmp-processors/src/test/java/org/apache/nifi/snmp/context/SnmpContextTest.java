package org.apache.nifi.snmp.context;

import org.apache.nifi.snmp.configuration.BasicConfiguration;
import org.apache.nifi.snmp.configuration.SecurityConfiguration;
import org.apache.nifi.snmp.configuration.SecurityConfigurationBuilder;
import org.apache.nifi.snmp.testagents.TestSnmpV3Agent;
import org.junit.Test;
import org.snmp4j.CommunityTarget;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;

import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class SnmpContextTest {

    private static final BasicConfiguration basicConfiguration = new BasicConfiguration("localhost", 3333, 1, 1000);

    @Test
    public void testSnmpV1CreatesCommunityTarget() {
        SnmpContext snmpContext = SnmpContext.newInstance();
        SecurityConfiguration securityConfiguration = new SecurityConfigurationBuilder()
                .setVersion("SNMPv1")
                .setSecurityLevel("noAuthNoPriv")
                .setSecurityName("userName")
                .setAuthProtocol("SHA")
                .setAuthPassword("authPassword")
                .setPrivacyProtocol("DES")
                .setPrivacyPassword("privacyPassword")
                .setCommunityString("public")
                .createSecurityConfiguration();

        snmpContext.init(basicConfiguration, securityConfiguration);

        assertThat(snmpContext.getTarget(), instanceOf(CommunityTarget.class));
    }

    @Test
    public void testSnmpV2cCreatesCommunityTarget() {
        SnmpContext snmpContext = SnmpContext.newInstance();
        SecurityConfiguration securityConfiguration = new SecurityConfigurationBuilder()
                .setVersion("SNMPv2c")
                .setSecurityLevel("noAuthNoPriv")
                .setSecurityName("userName")
                .setAuthProtocol("SHA")
                .setAuthPassword("authPassword")
                .setPrivacyProtocol("DES")
                .setPrivacyPassword("privacyPassword")
                .setCommunityString("public")
                .createSecurityConfiguration();

        snmpContext.init(basicConfiguration, securityConfiguration);

        assertThat(snmpContext.getTarget(), instanceOf(CommunityTarget.class));
    }

    @Test
    public void testSnmpV3CreatesUserTarget() throws IOException {

        TestSnmpV3Agent snmpV3Agent = new TestSnmpV3Agent("0.0.0.0");
        snmpV3Agent.start();

        SnmpContext snmpContext = SnmpContext.newInstance();
        SecurityConfiguration securityConfiguration = new SecurityConfigurationBuilder()
                .setVersion("SNMPv3")
                .setSecurityLevel("authNoPriv")
                .setSecurityName("SHA")
                .setAuthProtocol("SHA")
                .setAuthPassword("authPassword")
                .setPrivacyProtocol("DES")
                .setPrivacyPassword("privacyPassword")
                .setCommunityString("public")
                .createSecurityConfiguration();

        snmpContext.init(basicConfiguration, securityConfiguration);

        assertThat(snmpContext.getTarget(), instanceOf(UserTarget.class));

        snmpV3Agent.stop();
    }

    @Test
    public void testResourcesClosed() {
        SnmpContext snmpContext = SnmpContext.newInstance();
        SecurityConfiguration securityConfiguration = new SecurityConfigurationBuilder()
                .setVersion("SNMPv2c")
                .setSecurityLevel("noAuthNoPriv")
                .setSecurityName("userName")
                .setAuthProtocol("SHA")
                .setAuthPassword("authPassword")
                .setPrivacyProtocol("DES")
                .setPrivacyPassword("privacyPassword")
                .setCommunityString("public")
                .createSecurityConfiguration();

        snmpContext.init(basicConfiguration, securityConfiguration);
        snmpContext.close();

        final Collection<TransportMapping> transportMappings = snmpContext.getSnmp().getMessageDispatcher().getTransportMappings();

        boolean isAllClosed = transportMappings.stream().noneMatch(TransportMapping::isListening);

        assertTrue(isAllClosed);
    }
}
