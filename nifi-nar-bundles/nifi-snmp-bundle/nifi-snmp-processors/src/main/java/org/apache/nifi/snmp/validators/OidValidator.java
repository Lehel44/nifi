package org.apache.nifi.snmp.validators;


import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.snmp.configuration.SecurityConfiguration;

import java.util.Collection;
import java.util.List;

public class OidValidator {

    private final SecurityConfiguration securityConfiguration;
    private final List<ValidationResult> problems;

    public OidValidator(final SecurityConfiguration securityConfiguration, final List<ValidationResult> problems) {
        this.securityConfiguration = securityConfiguration;
        this.problems = problems;
    }

    public Collection<ValidationResult> validate() {

        if (securityConfiguration.isVersion3()) {
            if (!securityConfiguration.isSecurityNameSet()) {
                problems.add(new ValidationResult.Builder()
                        .input("SNMP Security Name")
                        .valid(false)
                        .explanation("SNMP Security Name must be set with SNMPv3.")
                        .build());
            }

            checkSecurityLevel(securityConfiguration, problems);
        } else {
            final boolean isCommunitySet = securityConfiguration.isCommunityStringSet();
            if (!isCommunitySet) {
                problems.add(new ValidationResult.Builder()
                        .input("SNMP Community")
                        .valid(false)
                        .explanation("SNMP Community must be set with SNMPv1 and SNMPv2c.")
                        .build());
            }
        }

        return problems;
    }

    private void checkSecurityLevel(SecurityConfiguration securityConfiguration, List<ValidationResult> problems) {
        final boolean isAuthProtocolValid = securityConfiguration.isAuthProtocolValid();
        final boolean isAuthPasswordSet = securityConfiguration.isAuthPasswordSet();
        final boolean isPrivacyProtocolValid = securityConfiguration.isPrivacyProtocolValid();
        final boolean isPrivacyPasswordSet = securityConfiguration.isPrivacyPasswordSet();

        switch (securityConfiguration.getSecurityLevel()) {
            case AUTH_NO_PRIV:
                if (!isAuthProtocolValid || !isAuthPasswordSet) {
                    problems.add(new ValidationResult.Builder()
                            .input("SNMP Security Level")
                            .valid(false)
                            .explanation("Authentication protocol and password must be set when using authNoPriv security level.")
                            .build());
                }
                break;
            case AUTH_PRIV:
                if (!isAuthProtocolValid || !isAuthPasswordSet || !isPrivacyProtocolValid || !isPrivacyPasswordSet) {
                    problems.add(new ValidationResult.Builder()
                            .input("SNMP Security Level")
                            .valid(false)
                            .explanation("All protocols and passwords must be set when using authPriv security level.")
                            .build());
                }
                break;
            case NO_AUTH_NO_PRIV:
            default:
                break;
        }
    }

}
