package org.apache.nifi.processors.hubspot;

public enum IncrementalFieldType {
    LAST_MODIFIED_DATE("lastmodifieddate"),
    HS_LAST_MODIFIED_DATE("hs_lastmodifieddate"),
    CREATE_DATE("createdate"),
    HS_CREATE_DATE("hs_createdate");

    final String value;

    IncrementalFieldType(String value) {
        this.value = value;
    }

    String getValue() {
        return value;
    }
}
