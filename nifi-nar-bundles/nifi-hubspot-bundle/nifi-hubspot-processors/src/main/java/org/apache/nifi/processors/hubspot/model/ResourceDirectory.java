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
package org.apache.nifi.processors.hubspot.model;

import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.processors.hubspot.rest.RestUriType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ResourceDirectory {

    private ResourceDirectory() {
    }

    private static final Map<ResourceType, List<HubSpotResource>> resourceMap;

    static {
        resourceMap = new EnumMap<>(ResourceType.class);
        resourceMap.put(ResourceType.ACCESS, getAccessResources());
        resourceMap.put(ResourceType.ANALYTICS, getAnalyticsResources());
        resourceMap.put(ResourceType.BILLING, getBillingResources());
        resourceMap.put(ResourceType.CUSTOMERS, getCustomerResources());
        resourceMap.put(ResourceType.DISCOUNTS, getDiscountResources());
        resourceMap.put(ResourceType.EVENTS, getEventResources());
        resourceMap.put(ResourceType.INVENTORY, getInventoryResources());
        resourceMap.put(ResourceType.MARKETING_EVENT, getMarketingEventResources());
        resourceMap.put(ResourceType.METAFIELDS, getMetafieldResources());
        resourceMap.put(ResourceType.ONLINE_STORE, getOnlineStoreResources());
        resourceMap.put(ResourceType.ORDERS, getOrderResources());
        resourceMap.put(ResourceType.PLUS, getPlusResources());
        resourceMap.put(ResourceType.PRODUCT, getProductResources());
        resourceMap.put(ResourceType.SALES_CHANNELS, getSalesChannelResources());
        resourceMap.put(ResourceType.SHIPPING_AND_FULFILLMENTS, getShippingAndFulfillmentResources());
        resourceMap.put(ResourceType.STORE_PROPERTIES, getStorePropertyResources());
        resourceMap.put(ResourceType.TENDER_TRANSACTIONS, getTenderTransactionResources());
    }

    private static List<HubSpotResource> getAccessResources() {
        final HubSpotResource accessScope = HubSpotResource.withUriPath(
                "access_scopes",
                "Access Scope",
                "The AccessScope resource allows you to retrieve the permissions that a merchant has granted to an app.",
                RestUriType.OAUTH,
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource storefrontAccessToken = HubSpotResource.newInstance(
                "storefront_access_tokens",
                "Storefront Access Token",
                "Storefront access tokens are used to delegate unauthenticated access scopes to clients that need to access the unautheticated Storefront API.",
                IncrementalLoadingParameter.NONE
        );

        return Collections.unmodifiableList(Arrays.asList(accessScope, storefrontAccessToken));
    }

    private static List<HubSpotResource> getAnalyticsResources() {
        return Collections.singletonList(HubSpotResource.newInstance(
                "reports",
                "Report",
                "Reports to query",
                IncrementalLoadingParameter.UPDATED_AT_MIN
                ));
    }

    private static List<HubSpotResource> getBillingResources() {
        final HubSpotResource applicationCharge = HubSpotResource.newInstance(
                "application_charges",
                "Application Charge",
                "The ApplicationCharge resource facilitates one-time charges.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource applicationCredit = HubSpotResource.newInstance(
                "application_credits",
                "Application Credit",
                "The ApplicationCredit resource is used to issue credits to merchants that can be used towards future app purchases in Shopify.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource recurringApplicationCharge = HubSpotResource.newInstance(
                "recurring_application_charges",
                "Recurring Application Charge",
                "The RecurringApplicationCharge resource facilitates a fixed-value, 30-day recurring charge.",
                IncrementalLoadingParameter.NONE
        );

        return Collections.unmodifiableList(Arrays.asList(applicationCharge, applicationCredit, recurringApplicationCharge));
    }

    private static List<HubSpotResource> getCustomerResources() {
        final HubSpotResource customer = HubSpotResource.newInstance(
                "customers",
                "Customers",
                "The Customer resource stores information about a shop's customers, such as their contact details," +
                        " their order history, and whether they've agreed to receive email marketing.",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        final HubSpotResource customerSavedSearch = HubSpotResource.newInstance(
                "customer_saved_searches",
                "Customer Saved Searches",
                "A customer saved search is a search query that represents a group of customers defined by the shop owner.",
                IncrementalLoadingParameter.NONE
        );
        return Collections.unmodifiableList(Arrays.asList(customer, customerSavedSearch));
    }

    private static List<HubSpotResource> getDiscountResources() {
        final HubSpotResource priceRule = HubSpotResource.newInstance(
                "price_rules",
                "Price Rules",
                "The PriceRule resource can be used to get discounts using conditions",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        return Collections.singletonList(priceRule);
    }

    private static List<HubSpotResource> getEventResources() {
        final HubSpotResource event = HubSpotResource.newInstance(
                "events",
                "Events",
                "Events are generated by some Shopify resources when certain actions are completed," +
                        " such as the creation of an article, the fulfillment of an order, or the addition of a product.",
                IncrementalLoadingParameter.CREATED_AT_MIN
        );
        return Collections.singletonList(event);
    }

    private static List<HubSpotResource> getInventoryResources() {
        final HubSpotResource inventoryLevel = HubSpotResource.newInstance(
                "inventory_levels",
                "Inventory Levels",
                "An inventory level represents the quantities of an inventory item for a location.",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        final HubSpotResource location = HubSpotResource.newInstance(
                "locations",
                "Locations",
                "A location represents a geographical location where your stores, pop-up stores, headquarters, and warehouses exist.",
                IncrementalLoadingParameter.NONE
        );
        return Collections.unmodifiableList(Arrays.asList(inventoryLevel, location));
    }

    private static List<HubSpotResource> getMarketingEventResources() {
        final HubSpotResource metafield = HubSpotResource.newInstance(
                "marketing_events",
                "Marketing Events",
                "Marketing events represent actions taken by your app, on behalf of the merchant, to market products, collections," +
                        " discounts, pages, blog posts, and other features.",
                IncrementalLoadingParameter.NONE
        );
        return Collections.singletonList(metafield);
    }

    private static List<HubSpotResource> getMetafieldResources() {
        final HubSpotResource metafield = HubSpotResource.newInstance(
                "metafields",
                "Metafields",
                "Metafields are a flexible way to attach additional information to a Shopify resource (e.g. Product, Collection, etc.).",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        return Collections.singletonList(metafield);
    }

    private static List<HubSpotResource> getOnlineStoreResources() {
        final HubSpotResource blog = HubSpotResource.newInstance(
                "blogs",
                "Blogs",
                "Shopify shops come with a built-in blogging engine, allowing a shop to have one or more blogs.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource comment = HubSpotResource.newInstance(
                "comments",
                "Comments",
                "A comment is a reader's response to an article in a blog.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource page = HubSpotResource.newInstance(
                "pages",
                "Pages",
                "Shopify stores come with a tool for creating basic HTML web pages.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource redirect = HubSpotResource.newInstance(
                "redirects",
                "Redirects",
                "A redirect causes a visitor on a specific path on the shop's site to be automatically sent to a different location.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource scriptTag = HubSpotResource.newInstance(
                "script_tags",
                "Script Tags",
                "The ScriptTag resource represents remote JavaScript code that is loaded into the pages of a shop's storefront or the order status page of checkout.",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        final HubSpotResource theme = HubSpotResource.newInstance(
                "themes",
                "Themes",
                "A theme controls the look and feel of a Shopify online store.",
                IncrementalLoadingParameter.NONE
        );
        return Collections.unmodifiableList(Arrays.asList(blog, comment, page, redirect, scriptTag, theme));
    }

    private static List<HubSpotResource> getOrderResources() {
        final HubSpotResource abandonedCheckouts = HubSpotResource.newInstance(
                "checkouts",
                "Abandoned Checkouts",
                "A checkout is considered abandoned after the customer has added contact information, but before the customer has completed their purchase.",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        final HubSpotResource draftOrders = HubSpotResource.newInstance(
                "draft_orders",
                "Draft Orders",
                "Merchants can use draft orders to create orders on behalf of their customers.",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        final HubSpotResource orders = HubSpotResource.newInstance(
                "orders",
                "Orders",
                "An order is a customer's request to purchase one or more products from a shop.",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        return Collections.unmodifiableList(Arrays.asList(abandonedCheckouts, draftOrders, orders));
    }

    private static List<HubSpotResource> getPlusResources() {
        final HubSpotResource giftCard = HubSpotResource.newInstance(
                "gift_cards",
                "Gift Cards",
                "A gift card is an alternative payment method. Each gift card has a unique code that is entered during checkout.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource user = HubSpotResource.newInstance(
                "users",
                "Users",
                "The User resource contains information about staff on a Shopify shop, including staff permissions.",
                IncrementalLoadingParameter.NONE
        );
        return Collections.unmodifiableList(Arrays.asList(giftCard, user));
    }

    private static List<HubSpotResource> getProductResources() {
        final HubSpotResource collect = HubSpotResource.newInstance(
                "collects",
                "Collects",
                "Collects are meant for managing the relationship between products and custom collections.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource customCollection = HubSpotResource.newInstance(
                "custom_collections",
                "Custom Collections",
                "A custom collection is a grouping of products that a merchant can create to make their store easier to browse. ",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        final HubSpotResource product = HubSpotResource.newInstance(
                "products",
                "Products",
                "Get products in a merchant's store ",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        final HubSpotResource smartCollection = HubSpotResource.newInstance(
                "smart_collections",
                "Smart Collections",
                "A smart collection is a grouping of products defined by rules that are set by the merchant.",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        return Collections.unmodifiableList(Arrays.asList(collect, customCollection, product, smartCollection));
    }

    private static List<HubSpotResource> getSalesChannelResources() {
        final HubSpotResource collectionListing = HubSpotResource.newInstance(
                "collection_listings",
                "Collection Listings",
                "A CollectionListing resource represents a product collection that a merchant has made available to your sales channel.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource mobilePlatformApplication = HubSpotResource.newInstance(
                "mobile_platform_applications",
                "Mobile Platform Applications",
                "You can use the MobilePlatformApplication resource to enable shared web credentials for Shopify iOS apps, as well as to" +
                        " create iOS universal link or Android app link verification endpoints for merchant Shopify iOS or Android apps.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource productListing = HubSpotResource.newInstance(
                "product_listings",
                "Product Listings",
                "A ProductListing resource represents a Product which is available to your sales channel.",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );

        final HubSpotResource resourceFeedback = HubSpotResource.newInstance(
                "resource_feedbacks",
                "Resource Feedbacks",
                "The ResourceFeedback resource lets an app report the status of shops and their resources. ",
                IncrementalLoadingParameter.NONE
        );
        return Collections.unmodifiableList(Arrays.asList(collectionListing, mobilePlatformApplication, productListing, resourceFeedback));
    }

    private static List<HubSpotResource> getShippingAndFulfillmentResources() {
        final HubSpotResource carrierServices = HubSpotResource.newInstance(
                "carrier_services",
                "Carrier Services",
                "A carrier service (also known as a carrier calculated service or shipping service) provides real-time shipping rates to Shopify.",
                IncrementalLoadingParameter.NONE
        );
        return Collections.singletonList(carrierServices);
    }

    private static List<HubSpotResource> getStorePropertyResources() {
        final HubSpotResource country = HubSpotResource.newInstance(
                "countries",
                "Countries",
                "The Country resource represents the tax rates applied to orders from the different countries where a shop sells its products.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource currency = HubSpotResource.newInstance(
                "currencies",
                "Currencies",
                "Merchants who use Shopify Payments can allow customers to pay in their local currency on the online store.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource policy = HubSpotResource.newInstance(
                "policies",
                "Policies",
                "Policy resource can be used to access the policies that a merchant has configured for their shop, such as their refund and privacy policies.",
                IncrementalLoadingParameter.NONE
        );
        final HubSpotResource shippingZone = HubSpotResource.newInstance(
                "shipping_zones",
                "Shipping Zones",
                "ShippingZone resource can be used to view shipping zones and their countries, provinces, and shipping rates.",
                IncrementalLoadingParameter.UPDATED_AT_MIN
        );
        final HubSpotResource shop = HubSpotResource.newInstance(
                "shop",
                "Shop",
                "The Shop resource is a collection of general business and store management settings and information about the store.",
                IncrementalLoadingParameter.NONE
        );
        return Collections.unmodifiableList(Arrays.asList(country, currency, policy, shippingZone, shop));
    }

    private static List<HubSpotResource> getTenderTransactionResources() {
        final HubSpotResource tenderTransaction = HubSpotResource.newInstance(
                "tender_transactions",
                "Tender Transactions",
                "Each tender transaction represents money passing between the merchant and a customer.",
                IncrementalLoadingParameter.PROCESSED_AT_MIN
        );
        return Collections.singletonList(tenderTransaction);
    }

    public static AllowableValue[] getCategories() {
        return resourceMap.keySet().stream().map(ResourceType::getAllowableValue).toArray(AllowableValue[]::new);
    }

    public static List<HubSpotResource> getResources(final ResourceType key) {
        return resourceMap.get(key);
    }

    public static AllowableValue[] getResourcesAsAllowableValues(final ResourceType key) {
        return getResources(key).stream().map(HubSpotResource::getAllowableValue).toArray(AllowableValue[]::new);
    }

    public static HubSpotResource getResourceTypeDto(final ResourceType key, final String value) {
        return getResources(key).stream()
                .filter(s -> s.getValue().equals(value))
                .findFirst()
                .get();
    }
}
