/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.snmp.utils;

import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.snmp.processors.GetSNMP;
import org.apache.nifi.util.MockProcessSession;
import org.apache.nifi.util.SharedSessionState;
import org.junit.Test;
import org.snmp4j.PDU;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link SNMPUtils}.
 */
public class SNMPUtilsTest {

    /**
     * Test for updating attributes of flow files with {@link PDU}
     */
    @Test
    public void validateUpdateFlowFileAttributes() {
        final GetSNMP processor = new GetSNMP();
        final ProcessSession processSession = new MockProcessSession(new SharedSessionState(processor, new AtomicLong()),
                processor);
        final FlowFile sourceFlowFile = processSession.create();

        final PDU pdu = new PDU();
        pdu.setErrorIndex(0);
        pdu.setErrorStatus(0);
        pdu.setType(4);

        final FlowFile f2 = SNMPUtils.updateFlowFileAttributesWithPduProperties(pdu, sourceFlowFile,
                processSession);

        assertEquals("0", f2.getAttributes().get(SNMPUtils.SNMP_PROP_PREFIX + "errorIndex"));
        assertEquals("0", f2.getAttributes().get(SNMPUtils.SNMP_PROP_PREFIX + "errorStatus"));
        assertEquals("4", f2.getAttributes().get(SNMPUtils.SNMP_PROP_PREFIX + "type"));
    }
}
