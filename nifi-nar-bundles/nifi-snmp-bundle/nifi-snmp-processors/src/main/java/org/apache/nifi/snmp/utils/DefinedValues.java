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

import org.apache.nifi.components.AllowableValue;

public final class DefinedValues {

    // SNMP versions
    public static final AllowableValue SNMP_V1 = new AllowableValue("SNMPv1", "v1", "SNMP version 1");
    public static final AllowableValue SNMP_V2C = new AllowableValue("SNMPv2c", "v2c", "SNMP version 2c");
    public static final AllowableValue SNMP_V3 = new AllowableValue("SNMPv3", "v3", "SNMP version 3 with improved security");

    // SNMPv3 security
    public static final AllowableValue NO_AUTH_NO_PRIV = new AllowableValue("noAuthNoPriv", "No authentication or encryption",
            "No authentication or encryption");
    public static final AllowableValue AUTH_NO_PRIV = new AllowableValue("authNoPriv", "Authentication without encryption",
            "Authentication without encryption");
    public static final AllowableValue AUTH_PRIV = new AllowableValue("authPriv", "Authentication and encryption",
            "Authentication and encryption");

    // SNMPv3 encryption
    public static final AllowableValue DES = new AllowableValue("DES", "DES", "Symmetric-key algorithm for the encryption of digital data");
    public static final AllowableValue DES3 = new AllowableValue("3DES", "3DES", "Symmetric-key block cipher, which applies the DES cipher algorithm three times to each data block");

    private static final String AES_DESCRIPTION = "AES is a symmetric algorithm which uses the same 128, 192, or 256 bit key for both encryption and decryption (the security of an AES system increases exponentially with key length)";

    public static final AllowableValue AES128 = new AllowableValue("AES128", "AES128", AES_DESCRIPTION);
    public static final AllowableValue AES192 = new AllowableValue("AES192", "AES192", AES_DESCRIPTION);
    public static final AllowableValue AES256 = new AllowableValue("AES256", "AES256", AES_DESCRIPTION);
    public static final AllowableValue NO_ENCRYPTION = new AllowableValue("", "No encryption protocol", "Sends SNMP requests without encryption");

    private DefinedValues() {
    }
}
