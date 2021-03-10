package org.apache.nifi.snmp.processors;

import org.apache.nifi.snmp.testagents.TestSnmpV1Agent;
import org.apache.nifi.snmp.testagents.TestSnmpV2Agent;
import org.apache.nifi.snmp.testagents.TestSnmpV3Agent;
import org.apache.nifi.snmp.utils.SnmpUtils;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.Snmp;
import org.snmp4j.agent.mo.DefaultMOFactory;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GetSnmpTest {

    private static TestSnmpV1Agent snmpV1Agent;
    private static final OID readOnlyOID1 = new OID("1.3.6.1.4.1.32437.1.5.1.4.2.0");
    private static final OID readOnlyOID2 = new OID("1.3.6.1.4.1.32437.1.5.1.4.3.0");
    private static final String OIDValue1 = "TestOID1";
    private static final String OIDValue2 = "TestOID2";

    @BeforeClass
    public static void setUp() throws IOException {
        snmpV1Agent = new TestSnmpV1Agent("0.0.0.0");
        snmpV1Agent.start();
        snmpV1Agent.registerManagedObjects(
                DefaultMOFactory.getInstance().createScalar(new OID(readOnlyOID1), MOAccessImpl.ACCESS_READ_ONLY, new OctetString(OIDValue1)),
                DefaultMOFactory.getInstance().createScalar(new OID(readOnlyOID2), MOAccessImpl.ACCESS_READ_ONLY, new OctetString(OIDValue2))
        );
    }

    @AfterClass
    public static void tearDown() {
        snmpV1Agent.stop();
    }

    @Test
    public void testSnmpV1Get() throws InterruptedException {
        TestRunner runner = getTestRunner(readOnlyOID1.toString(), String.valueOf(snmpV1Agent.getPort()), "GET");
        runner.run();
        Thread.sleep(200);
        final MockFlowFile successFF = runner.getFlowFilesForRelationship(GetSnmp.REL_SUCCESS).get(0);
        assertNotNull(successFF);
        assertEquals(OIDValue1, successFF.getAttribute(SnmpUtils.SNMP_PROP_PREFIX + readOnlyOID1.toString() + SnmpUtils.SNMP_PROP_DELIMITER + "4"));
    }

    @Test
    public void testSnmpV1Walk() throws InterruptedException {
        TestRunner runner = getTestRunner("1.3.6.1.4.1.32437", String.valueOf(snmpV1Agent.getPort()), "WALK");
        runner.run();
        Thread.sleep(200);
        final MockFlowFile successFF = runner.getFlowFilesForRelationship(GetSnmp.REL_SUCCESS).get(0);
        assertNotNull(successFF);
        assertEquals(OIDValue1, successFF.getAttribute(SnmpUtils.SNMP_PROP_PREFIX + readOnlyOID1.toString() + SnmpUtils.SNMP_PROP_DELIMITER + "4"));
        assertEquals(OIDValue2, successFF.getAttribute(SnmpUtils.SNMP_PROP_PREFIX + readOnlyOID2.toString() + SnmpUtils.SNMP_PROP_DELIMITER + "4"));
    }

    @Test
    public void testInvalidPduResultsInFailure() throws InterruptedException {
        TestRunner runner = getTestRunner("1.3.6.1.4.1.32437.0", String.valueOf(snmpV1Agent.getPort()), "GET");
        runner.run();
        Thread.sleep(200);
        final MockFlowFile failureFF = runner.getFlowFilesForRelationship(GetSnmp.REL_FAILURE).get(0);
        assertNotNull(failureFF);
        assertEquals("No such name", failureFF.getAttribute(SnmpUtils.SNMP_PROP_PREFIX + "errorStatusText"));
    }

    private TestRunner getTestRunner(String oid, String port, String strategy) {
        TestRunner runner = TestRunners.newTestRunner(GetSnmp.class);
        runner.setProperty(GetSnmp.OID, oid);
        runner.setProperty(GetSnmp.HOST, "127.0.0.1");
        runner.setProperty(GetSnmp.PORT, port);
        runner.setProperty(GetSnmp.SNMP_COMMUNITY, "public");
        runner.setProperty(GetSnmp.SNMP_VERSION, "SNMPv1");
        runner.setProperty(GetSnmp.SNMP_STRATEGY, strategy);
        return runner;
    }

}
