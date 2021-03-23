package org.apache.nifi.snmp.validators;

import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;

import java.util.regex.Pattern;

public class OIDValidator {

    public static final Pattern OID_PATTERN = Pattern.compile("[0-9+.]*");

    public static final Validator SNMP_OID_VALIDATOR = (subject, input, context) -> {
        final ValidationResult.Builder builder = new ValidationResult.Builder();
        builder.subject(subject).input(input);
        if (context.isExpressionLanguageSupported(subject) && context.isExpressionLanguagePresent(input)) {
            return builder.valid(true).explanation("Contains Expression Language").build();
        }
        try {
            if (OID_PATTERN.matcher(input).matches()) {
                builder.valid(true);
            } else {
                builder.valid(false).explanation(input + "is not a valid OID");
            }
        } catch (final IllegalArgumentException e) {
            builder.valid(false).explanation(e.getMessage());
        }
        return builder.build();
    };

}
