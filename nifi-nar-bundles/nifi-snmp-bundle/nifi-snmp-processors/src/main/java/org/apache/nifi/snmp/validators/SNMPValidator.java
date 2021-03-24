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
package org.apache.nifi.snmp.validators;

import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.snmp.configuration.TargetConfiguration;
import org.apache.nifi.snmp.utils.SNMPVersion;
import org.apache.nifi.util.StringUtils;
import org.snmp4j.security.SecurityLevel;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class SNMPValidator {

    private final TargetConfiguration targetConfiguration;
    private final List<ValidationResult> problems;

    public SNMPValidator(final TargetConfiguration targetConfiguration, final List<ValidationResult> problems) {
        this.targetConfiguration = targetConfiguration;
        this.problems = problems;
    }

    public Collection<ValidationResult> validate() {
        final boolean isVersion3 = SNMPVersion.SNMP_V3.getSnmpVersionDisplay().equals(targetConfiguration.getVersion());
        final boolean isSecurityNameInvalid = isInvalid(targetConfiguration.getSecurityName());
        final boolean isCommunityStringInvalid = isInvalid(targetConfiguration.getCommunityString());

        if (isVersion3 && isSecurityNameInvalid) {
            problems.add(new ValidationResult.Builder()
                    .input("SNMP Security Name")
                    .valid(false)
                    .explanation("SNMP Security Name must be set with SNMPv3.")
                    .build());
            checkSecurityLevel(targetConfiguration, problems);

        } else if (isCommunityStringInvalid) {
            problems.add(new ValidationResult.Builder()
                    .input("SNMP Community")
                    .valid(false)
                    .explanation("SNMP Community must be set with SNMPv1 and SNMPv2c.")
                    .build());
        }
        return problems;
    }

    private void checkSecurityLevel(TargetConfiguration targetConfiguration, List<ValidationResult> problems) {

        final boolean isAuthProtocolInvalid = isInvalid(targetConfiguration.getAuthProtocol());
        final boolean isAuthPasswordInvalid = isInvalid(targetConfiguration.getAuthPassword());
        final boolean isPrivacyProtocolInvalid = isInvalid(targetConfiguration.getPrivacyProtocol());
        final boolean isPrivacyPasswordInvalid = isInvalid(targetConfiguration.getPrivacyPassword());
        final SecurityLevel securityLevel = SecurityLevel.valueOf(targetConfiguration.getSecurityLevel());

        if (isAuthNoPrivSecurityLevelInvalid(securityLevel, isAuthProtocolInvalid, isAuthPasswordInvalid)) {
            problems.add(new ValidationResult.Builder()
                    .input("SNMP Security Level")
                    .valid(false)
                    .explanation("Authentication protocol and password must be set when using authNoPriv security level.")
                    .build());
        }

        if (isAuthPrivSecurityLevelInvalid(securityLevel, isAuthProtocolInvalid, isAuthPasswordInvalid, isPrivacyProtocolInvalid, isPrivacyPasswordInvalid)) {
            problems.add(new ValidationResult.Builder()
                    .input("SNMP Security Level")
                    .valid(false)
                    .explanation("All protocols and passwords must be set when using authPriv security level.")
                    .build());
        }
    }

    private boolean isInvalid(String property) {
        return Objects.isNull(property) || StringUtils.EMPTY.equals(property);
    }

    private boolean isAuthNoPrivSecurityLevelInvalid(final SecurityLevel securityLevel, final boolean isAuthProtocolInvalid, final boolean isAuthPasswordInvalid) {
        return SecurityLevel.authNoPriv == securityLevel && (isAuthProtocolInvalid || isAuthPasswordInvalid);
    }

    private boolean isAuthPrivSecurityLevelInvalid(final SecurityLevel securityLevel, final boolean isAuthProtocolInvalid, final boolean isAuthPasswordInvalid,
                                                   final boolean isPrivacyProtocolInvalid, final boolean isPrivacyPasswordInvalid) {
        return SecurityLevel.authPriv == securityLevel && (isAuthProtocolInvalid || isAuthPasswordInvalid || isPrivacyProtocolInvalid || isPrivacyPasswordInvalid);
    }

}
