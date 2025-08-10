/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.identity.webhook.wso2.event.handler.internal.model;

import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.AccessToken;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.Application;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.Organization;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.Reason;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.Tenant;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.User;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.UserStore;

import java.util.List;

/**
 * Represents the payload for the WSO2 Token Revoked event.
 * This class encapsulates details about the revoked access tokens and other related information.
 */
public class WSO2TokenRevokedEventPayload extends WSO2BaseEventPayload {

    private List<AccessToken> accessTokens;
    private Reason reason;
    private String action;
    private List<Application> application;

    public List<AccessToken> getAccessTokens() {

        return accessTokens;
    }

    public Reason getReason() {

        return reason;
    }

    public String getAction() {

        return action;
    }

    public List<Application> getApplications() {

        return application;
    }

    private WSO2TokenRevokedEventPayload(Builder builder) {

        this.initiatorType = builder.initiatorType;
        this.tenant = builder.tenant;
        this.userStore = builder.userStore;
        this.user = builder.user;
        this.accessTokens = builder.accessTokens;
        this.reason = builder.reason;
        this.action = builder.action;
        this.organization = builder.organization;
        this.application = builder.applications;
    }

    /**
     * Builder for the WSO2TokenRevokedEventPayload.
     */
    public static class Builder {

        private String initiatorType;
        private Tenant tenant;
        private UserStore userStore;
        private User user;
        private List<AccessToken> accessTokens;
        private Reason reason;
        private String action;
        private Organization organization;
        private List<Application> applications;

        public Builder initiatorType(String initiatorType) {

            this.initiatorType = initiatorType;
            return this;
        }

        public Builder tenant(Tenant tenant) {

            this.tenant = tenant;
            return this;
        }

        public Builder userStore(UserStore userStore) {

            this.userStore = userStore;
            return this;
        }

        public Builder user(User user) {

            this.user = user;
            return this;
        }

        public Builder accessTokens(List<AccessToken> accessTokens) {

            this.accessTokens = accessTokens;
            return this;
        }

        public Builder applications(List<Application> applications) {

            this.applications = applications;
            return this;
        }

        public Builder reason(Reason reason) {

            this.reason = reason;
            return this;
        }

        public Builder action(String action) {

            this.action = action;
            return this;
        }

        public Builder organization(Organization organization) {

            this.organization = organization;
            return this;
        }

        public WSO2TokenRevokedEventPayload build() {

            return new WSO2TokenRevokedEventPayload(this);
        }
    }
}
