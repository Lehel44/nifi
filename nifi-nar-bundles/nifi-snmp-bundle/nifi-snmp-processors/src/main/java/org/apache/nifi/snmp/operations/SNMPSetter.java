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
package org.apache.nifi.snmp.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;

import java.io.IOException;

/**
 * Extension of {@link SNMPRequest} to perform SNMP Set requests.
 */
public final class SNMPSetter extends SNMPRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SNMPSetter.class);

    /**
     * Creates an instance of this setter.
     *
     * @param snmp   instance of {@link Snmp}
     * @param target instance of {@link AbstractTarget} to request
     */
    public SNMPSetter(Snmp snmp, AbstractTarget target) {
        super(snmp, target);
        LOGGER.info("Successfully initialized SNMP Setter");
    }

    /**
     * Executes the SNMP set request and returns the response.
     *
     * @param pdu PDU to send
     * @return Response event
     * @throws IOException IO Exception
     */
    public ResponseEvent set(PDU pdu) throws IOException {
        return snmp.set(pdu, target);
    }

}
