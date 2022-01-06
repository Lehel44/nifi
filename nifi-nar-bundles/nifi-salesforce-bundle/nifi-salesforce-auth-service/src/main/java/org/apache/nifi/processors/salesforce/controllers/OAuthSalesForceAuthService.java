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
package org.apache.nifi.processors.salesforce.controllers;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyDescriptor.Builder;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.util.StandardValidators;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

public class OAuthSalesForceAuthService extends AbstractControllerService implements SalesForceAuthService {

    static final PropertyDescriptor LOGIN_URL = new Builder()
            .name("login-url")
            .displayName("Login Url")
            .description("")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    static final PropertyDescriptor USERNAME = new Builder()
            .name("username")
            .displayName("Username")
            .description("")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    static final PropertyDescriptor PASSWORD = new Builder()
            .name("password")
            .displayName("Password")
            .description("")
            .required(true)
            .sensitive(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    static final PropertyDescriptor CLIENT_ID = new Builder()
            .name("client-id")
            .displayName("Client Id")
            .description("")
            .required(true)
            .sensitive(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    static final PropertyDescriptor CLIENT_SECRET = new Builder()
            .name("client-secret")
            .displayName("Client Secret")
            .description("")
            .required(true)
            .sensitive(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private String loginUrl;
    private String username;
    private String password;
    private String clientId;
    private String clientSecret;

    private String instanceUrl;
    private String accessToken;


    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return Arrays.asList(LOGIN_URL, USERNAME, PASSWORD, CLIENT_ID, CLIENT_SECRET);
    }

    @Override
    public void renew() {
        accessToken = null;
        instanceUrl = null;
        requestAccessToken();
    }

    @Override
    public String getToken() {
        return accessToken;
    }

    @Override
    public String getInstanceUrl() {
        return instanceUrl;
    }

    @OnEnabled
    public void onEnabled(final ConfigurationContext configContext) {
        loginUrl = configContext.getProperty(LOGIN_URL).getValue();
        username = configContext.getProperty(USERNAME).getValue();
        password = configContext.getProperty(PASSWORD).getValue();
        clientId = configContext.getProperty(CLIENT_ID).getValue();
        clientSecret = configContext.getProperty(CLIENT_SECRET).getValue();
        renew();
    }

    private void requestAccessToken() {
        OkHttpClient okHttpClient = new OkHttpClient();

        FormBody formBody = new FormBody.Builder()
                .add("grant_type", "password")
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("username", username)
                .add("password", password)
                .build();
        Request request = new Request.Builder()
                .url(loginUrl + "/services/oauth2/token")
                .post(formBody)
                .build();

        getLogger().debug("Calling login URL: " + request.url());
        try (Response response = okHttpClient.newCall(request).execute()) {
            getLogger().trace("auth response: " + response);
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    String responseString = response.body().string();
                    try (JsonReader reader = Json.createReader(new StringReader(responseString))) {
                        JsonObject jsonObject = reader.readObject();
                        accessToken = jsonObject.getString("access_token");
                        instanceUrl = jsonObject.getString("instance_url");
                    }
                }
            } else if (response.code() == 400 || response.code() == 401) {
                throw new RuntimeException("Authentication error: " + (response.body() == null ? "Unknown" : response.body().string()));
            } else {
                throw new RuntimeException("Invalid response: " + response.toString());
            }

        } catch (IOException e) {
            throw new RuntimeException("Error happened during token request.", e);
        }
    }
}
