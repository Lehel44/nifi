package org.apache.nifi.processors.hubspot.model;

import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.DescribedValue;
import org.apache.nifi.components.PropertyDescriptor;

import java.util.ArrayList;
import java.util.List;

public class ResourceNode implements Resource {

    private final String uri;
    private final String value;
    private final String displayName;
    private final String description;
    private final List<PropertyDescriptor> propertyDescriptors;

    public ResourceNode(String uri, String value, String displayName, String description) {
        this.uri = uri;
        this.value = value;
        this.displayName = displayName;
        this.description = description;
        propertyDescriptors = new ArrayList<>();
    }

    @Override
    public AllowableValue getAllowableValue() {
        return new AllowableValue(value, displayName, description);
    }

    public void addDescriptor(final PropertyDescriptor propertyDescriptor) {
        propertyDescriptors.add(propertyDescriptor);
    }

    public List<PropertyDescriptor> getPropertyDescriptors() {
        return propertyDescriptors;
    }
}
