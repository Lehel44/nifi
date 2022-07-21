package org.apache.nifi.processors.hubspot.model;

import org.apache.nifi.components.DescribedValue;

public enum ApiType implements DescribedValue {

    ANALYTICS("analytics", "analytics", "analytics"),
    AUTOMATION("automation", "automation", "automation"),
    CMS("cms", "cms","cms");

    private final String value;
    private final String displayName;
    private final String description;

    ApiType(String value, String displayName, String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
