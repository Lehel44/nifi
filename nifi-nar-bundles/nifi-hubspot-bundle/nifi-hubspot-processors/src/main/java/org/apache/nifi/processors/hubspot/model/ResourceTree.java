package org.apache.nifi.processors.hubspot.model;

import org.apache.nifi.components.PropertyDescriptor;

import java.util.List;

public class ResourceTree {


    public CategoryNode createTree() {

        final CategoryNode rootNode = new CategoryNode("root", "root", "root");

        addCompaniesApi(rootNode);
        addContactsApi(rootNode);

        return rootNode;
    }

    private void addCompaniesApi(CategoryNode rootNode) {
        final CategoryNode companiesApi = new CategoryNode(
                "hubspot-companies-api",
                "Companies API",
                "In HubSpot, company records store information about a business or organization."
        );

        final ResourceNode allCompanies = new ResourceNode(
                "/companies/v2/companies/paged",
                "hubspot-companies-all",
                "All Companies",
                "This endpoint is used to get all of the companies in a HubSpot account."
                );
        final ResourceNode recentlyModifiedCompanies = new ResourceNode(
                "/companies/v2/companies/recent/modified",
                "hubspot-companies-recently-modified",
                "Recently Modified Companies",
                "Returns a list of all companies sorted by the date the companies were most recently modified or created."
        );
        final ResourceNode recentlyCreatedCompanies = new ResourceNode(
                "/companies/v2/companies/recent/created",
                "hubspot-companies-recently-modified",
                "Recently Created Companies",
                "Returns a list of all companies sorted by the date the companies were created."
        );

        companiesApi.addNodes(allCompanies, recentlyModifiedCompanies, recentlyCreatedCompanies);
        rootNode.addNode(companiesApi);
    }

    private void addContactsApi(CategoryNode rootNode) {
        final CategoryNode contactsApi = new CategoryNode(
                "hubspot-contacts-api",
                "Contacts API",
                "In HubSpot, contacts store information about an individual. From marketing automation to smart content," +
                        "the lead-specific data found in contact records helps users leverage much of HubSpot's functionality."
        );

        final ResourceNode allContacts = new ResourceNode(
                "/contacts/v1/lists/all/contacts/all",
                "hubspot-contacts-all",
                "All Contacts",
                "For a given account, return all contacts that have been created in the account."
        );
        final ResourceNode recentlyModifiedContacts = new ResourceNode(
                "/contacts/v1/lists/recently_updated/contacts/recent",
                "hubspot-contacts-recently-modified",
                "Recently Modified Contacts",
                "The get recently updated and created contacts endpoint is used to return information about all contacts for a" +
                        " given account that were updated or created in the last 30 days."
        );
        final ResourceNode recentlyCreatedContacts = new ResourceNode(
                "/contacts/v1/lists/all/contacts/recent",
                "hubspot-contacts-recently-modified",
                "Recently Created Contacts",
                "For a given account, return all contacts that have been recently created."
        );

        contactsApi.addNodes(allContacts, recentlyModifiedContacts, recentlyCreatedContacts);
        rootNode.addNode(contactsApi);
    }

    public List<PropertyDescriptor> generatePropertyDescriptors() {

    }
}
