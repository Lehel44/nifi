package org.apache.nifi.snmp.exception;

public class SNMPException extends RuntimeException {

    public SNMPException(final String errorMessage) {
        super(errorMessage);
    }

}
