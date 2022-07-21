package org.apache.nifi.processors.hubspot.model;

import org.apache.nifi.components.AllowableValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CategoryNode implements Resource {

    private final String value;
    private final String displayName;
    private final String description;
    private final List<Resource> childNodes;

    public CategoryNode(final String value, final String displayName, final String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
        childNodes = new ArrayList<>();
    }

    public void addNode(final Resource resource) {
        childNodes.add(resource);
    }

    public void addNodes(final Resource... resources) {
        childNodes.addAll(Arrays.asList(resources));
    }

    public List<Resource> getChildNodes() {
        return childNodes;
    }

    @Override
    public AllowableValue getAllowableValue() {
        return new AllowableValue(value, displayName, description);
    }
}
