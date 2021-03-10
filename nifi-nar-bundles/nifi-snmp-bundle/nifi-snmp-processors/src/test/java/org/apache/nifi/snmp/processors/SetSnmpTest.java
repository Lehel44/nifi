package org.apache.nifi.snmp.processors;

import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.snmp.operations.SnmpGetter;
import org.apache.nifi.snmp.testagents.TestSnmpV1Agent;
import org.apache.nifi.snmp.utils.SnmpUtils;
import org.apache.nifi.util.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.agent.mo.DefaultMOFactory;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SetSnmpTest {

    private static TestSnmpV1Agent snmpV1Agent;
    private static final OID testOID = new OID("1.3.6.1.4.1.32437.1.5.1.4.2.0");
    private static final String testOIDValue = "TestOID";

    @BeforeClass
    public static void setUp() throws IOException {
        snmpV1Agent = new TestSnmpV1Agent("0.0.0.0");
        snmpV1Agent.start();
        snmpV1Agent.registerManagedObjects(
                DefaultMOFactory.getInstance().createScalar(new OID(testOID), MOAccessImpl.ACCESS_READ_WRITE, new OctetString(testOIDValue))
        );
    }

    @AfterClass
    public static void tearDown() {
        snmpV1Agent.stop();
    }

    @Test
    public void testSnmpV1Set() throws InterruptedException {

        TestRunner runner = getTestRunner(String.valueOf(snmpV1Agent.getPort()));
        runner.run();
        Thread.sleep(200);
        final MockFlowFile successFF = runner.getFlowFilesForRelationship(SetSnmp.REL_SUCCESS).get(0);
        assertNotNull(successFF);
        assertEquals(testOIDValue, successFF.getAttribute(SnmpUtils.SNMP_PROP_PREFIX + testOID.toString() + SnmpUtils.SNMP_PROP_DELIMITER + "4"));
    }

    @Test
    public void testSnmpV1SetEmptyFlowFileResultsInFailure() throws InterruptedException {

        TestRunner runner = getTestRunnerWithEmptyFlowFile(String.valueOf(snmpV1Agent.getPort()));
        runner.run();
        Thread.sleep(200);
        final MockFlowFile failureFF = runner.getFlowFilesForRelationship(SetSnmp.REL_FAILURE).get(0);
        assertNotNull(failureFF);
    }

    @Test
    public void testSnmpSetWithInvalidAddressResultsInFailure() throws InterruptedException {

        TestRunner runner = getTestRunnerWithInvalidHost();
        runner.run();
        Thread.sleep(200);
        final MockFlowFile failureFF = runner.getFlowFilesForRelationship(SetSnmp.REL_FAILURE).get(0);
        assertNotNull(failureFF);
    }

    @Test
    public void testSnmpSetWithInvalidOIDResultsInFailure() throws InterruptedException {

        TestRunner runner = getTestRunnerWithInvalidOID();
        runner.run();
        Thread.sleep(200);
        final MockFlowFile failureFF = runner.getFlowFilesForRelationship(SetSnmp.REL_FAILURE).get(0);
        assertNotNull(failureFF);
    }

    private TestRunner getTestRunner(String port) {
        SetSnmp processor = new SetSnmp();
        TestRunner runner = TestRunners.newTestRunner(processor);
        MockFlowFile ff = new MockFlowFile(123);
        final Map<String, String> attributes = ff.getAttributes();
        Map<String, String> newAttributes = new HashMap<>(attributes);
        newAttributes.put("snmp$1.3.6.1.4.1.32437.1.5.1.4.2.0$4", testOIDValue);
        ff.putAttributes(newAttributes);
        runner.enqueue(ff);
        runner.setProperty(GetSnmp.HOST, "127.0.0.1");
        runner.setProperty(GetSnmp.PORT, port);
        runner.setProperty(GetSnmp.SNMP_COMMUNITY, "public");
        runner.setProperty(GetSnmp.SNMP_VERSION, "SNMPv1");
        return runner;
    }

    private TestRunner getTestRunnerWithEmptyFlowFile(String port) {
        SetSnmp processor = new SetSnmp();
        TestRunner runner = TestRunners.newTestRunner(processor);
        runner.enqueue(new MockFlowFile(123));
        runner.setProperty(GetSnmp.HOST, "127.0.0.1");
        runner.setProperty(GetSnmp.PORT, port);
        runner.setProperty(GetSnmp.SNMP_COMMUNITY, "public");
        runner.setProperty(GetSnmp.SNMP_VERSION, "SNMPv1");
        return runner;
    }

    private TestRunner getTestRunnerWithInvalidHost() {
        SetSnmp processor = new SetSnmp();
        TestRunner runner = TestRunners.newTestRunner(processor);
        MockFlowFile ff = new MockFlowFile(123);
        final Map<String, String> attributes = ff.getAttributes();
        Map<String, String> newAttributes = new HashMap<>(attributes);
        newAttributes.put("snmp$1.3.6.1.4.1.32437.1.5.1.4.2.0$4", testOIDValue);
        ff.putAttributes(newAttributes);
        runner.enqueue(ff);
        runner.setProperty(GetSnmp.HOST, "127.0.0.2");
        runner.setProperty(GetSnmp.PORT, "1234");
        runner.setProperty(GetSnmp.SNMP_COMMUNITY, "public");
        runner.setProperty(GetSnmp.SNMP_VERSION, "SNMPv1");
        return runner;
    }

    private TestRunner getTestRunnerWithInvalidOID() {
        SetSnmp processor = new SetSnmp();
        TestRunner runner = TestRunners.newTestRunner(processor);
        MockFlowFile ff = new MockFlowFile(123);
        final Map<String, String> attributes = ff.getAttributes();
        Map<String, String> newAttributes = new HashMap<>(attributes);
        newAttributes.put("snmp$1.3.6.1.4.1.32437.1.5.1.4.213.0$4", testOIDValue);
        ff.putAttributes(newAttributes);
        runner.enqueue(ff);
        runner.setProperty(GetSnmp.HOST, "127.0.0.1");
        runner.setProperty(GetSnmp.PORT, String.valueOf(snmpV1Agent.getPort()));
        runner.setProperty(GetSnmp.SNMP_COMMUNITY, "public");
        runner.setProperty(GetSnmp.SNMP_VERSION, "SNMPv1");
        return runner;
    }

}
