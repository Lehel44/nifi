package org.apache.nifi.dbcp;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;

public final class DBCPProperties {

    public static final PropertyDescriptor DATABASE_URL = new PropertyDescriptor.Builder()
            .name("Database Connection URL")
            .description("A database connection URL used to connect to a database. May contain database system name, host, port, database name and some parameters."
                    + " The exact syntax of a database connection URL is specified by your DBMS.")
            .defaultValue(null)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor DB_USER = new PropertyDescriptor.Builder()
            .name("Database User")
            .description("Database user name")
            .defaultValue(null)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();

    public static final PropertyDescriptor DB_PASSWORD = new PropertyDescriptor.Builder()
            .name("Password")
            .description("The password for the database user")
            .defaultValue(null)
            .required(false)
            .sensitive(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .build();



}
