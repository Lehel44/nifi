package org.apache.nifi.snmp.context;

import org.apache.nifi.snmp.configuration.BasicConfiguration;
import org.apache.nifi.snmp.configuration.SecurityConfiguration;
import org.apache.nifi.snmp.processors.SnmpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;

public class SnmpContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnmpContext.class);

    private Snmp snmp;
    private AbstractTarget target;

    public static SnmpContext newInstance() {
        return new SnmpContext();
    }

    public void init(final BasicConfiguration basicConfiguration, final SecurityConfiguration securityConfiguration) {
        snmp = createSnmp();

        final String snmpVersion = securityConfiguration.getVersion();

        final int version = SnmpUtils.getSnmpVersion(snmpVersion);

        if (version == SnmpConstants.version3) {
            createUserTarget(basicConfiguration, securityConfiguration, snmp, version);
        } else {
            createCommunityTarget(basicConfiguration, securityConfiguration, version);
        }
    }

    public void close() {
        try {
            snmp.close();
        } catch (IOException e) {
            LOGGER.error("Could not close SNMP session.");
        }

    }

    private void createCommunityTarget(BasicConfiguration basicConfiguration, SecurityConfiguration securityConfiguration, int version) {
        target = new CommunityTarget();
        setupTargetBasicProperties(target, basicConfiguration, version);
        String community = securityConfiguration.getCommunityString();
        if (community != null) {
            ((CommunityTarget) target).setCommunity(new OctetString(community));
        }
    }

    private void createUserTarget(BasicConfiguration basicConfiguration, SecurityConfiguration securityConfiguration, Snmp snmp, int version) {
        final String username = securityConfiguration.getSecurityName();
        final String authProtocol = securityConfiguration.getAuthProtocol();
        final String authPassword = securityConfiguration.getAuthPassword();
        final String privacyProtocol = securityConfiguration.getPrivacyProtocol();
        final String privacyPassword = securityConfiguration.getPrivacyPassword();
        final OctetString authPasswordOctet = authPassword != null ? new OctetString(authPassword) : null;
        final OctetString privacyPasswordOctet = privacyPassword != null ? new OctetString(privacyPassword) : null;

        // Add user information.
        snmp.getUSM().addUser(
                new OctetString(username),
                new UsmUser(new OctetString(username), SnmpUtils.getAuth(authProtocol), authPasswordOctet,
                        SnmpUtils.getPriv(privacyProtocol), privacyPasswordOctet));

        target = new UserTarget();
        setupTargetBasicProperties(target, basicConfiguration, version);
        int securityLevel = SecurityLevel.valueOf(securityConfiguration.getSecurityLevel()).getSnmpValue();
        target.setSecurityLevel(securityLevel);

        final String securityName = securityConfiguration.getSecurityName();
        if (securityName != null) {
            target.setSecurityName(new OctetString(securityName));
        }
    }

    private Snmp createSnmp() {
        try {
            return new Snmp(new DefaultUdpTransportMapping());
        } catch (IOException e) {
            LOGGER.error("Could not create transport mapping", e);
            throw new RuntimeException("Transport mapping creation failure");
        }
    }

    private void setupTargetBasicProperties(AbstractTarget result, BasicConfiguration basicConfiguration, int version) {
        final String host = basicConfiguration.getHost();
        final int port = basicConfiguration.getPort();
        final int retries = basicConfiguration.getRetries();
        final int timeout = basicConfiguration.getTimeout();

        result.setVersion(version);
        result.setAddress(new UdpAddress(host + "/" + port));
        result.setRetries(retries);
        result.setTimeout(timeout);
    }

    public Snmp getSnmp() {
        return snmp;
    }

    public AbstractTarget getTarget() {
        return target;
    }
}
