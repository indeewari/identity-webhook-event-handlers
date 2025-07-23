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

package org.wso2.identity.webhook.common.event.handler.api.constants;

/**
 * Constants class.
 */
public class Constants {

    /**
     * This enum represents the event schema types.
     */
    public enum EventSchema {
        WSO2,
        CAEP,
        RISC;

        EventSchema() {

        }
    }

    /**
     * This class represents the event channels.
     */
    public static class Channel {

        public static final String LOGIN_CHANNEL = "https://schemas.identity.wso2.org/events/login";
        public static final String CREDENTIAL_CHANGE_CHANNEL = "https://schemas.identity.wso2.org/events/credential";
        public static final String REGISTRATION_CHANNEL = "https://schemas.identity.wso2.org/events/registration";
        public static final String USER_OPERATION_CHANNEL = "https://schemas.identity.wso2.org/events/user";
        public static final String VERIFICATION_CHANNEL = "https://schemas.identity.wso2.org/events/verification";
        public static final String SESSION_CHANNEL = "https://schemas.identity.wso2.org/events/session";
        public static final String TOKEN_CHANNEL = "https://schemas.identity.wso2.org/events/token";
    }

    /**
     * This class represents the identity event types.
     */
    public static class Event {

        public static final String LOGIN_SUCCESS_EVENT = "https://schemas.identity.wso2.org/events/login/event-type/loginSuccess";
        public static final String LOGIN_FAILURE_EVENT = "https://schemas.identity.wso2.org/events/login/event-type/loginFailed";
        public static final String POST_UPDATE_USER_CREDENTIAL = "https://schemas.identity.wso2.org/events/credential/event-type/credentialUpdated";
        public static final String POST_REGISTRATION_SUCCESS_EVENT = "https://schemas.identity.wso2.org/events/registration/event-type/registrationSuccess";
        public static final String POST_REGISTRATION_FAILED_EVENT = "https://schemas.identity.wso2.org/events/registration/event-type/registrationFailed";
        public static final String POST_USER_CREATED_EVENT = "https://schemas.identity.wso2.org/events/user/event-type/userCreated";
        public static final String POST_UPDATE_USER_LIST_OF_ROLE_EVENT = "https://schemas.identity.wso2.org/events/group/event-type/groupUpdated";
        public static final String POST_DELETE_USER_EVENT = "https://schemas.identity.wso2.org/events/user/event-type/userDeleted";
        public static final String POST_UNLOCK_ACCOUNT_EVENT = "https://schemas.identity.wso2.org/events/user/event-type/userAccountUnlocked";
        public static final String POST_LOCK_ACCOUNT_EVENT = "https://schemas.identity.wso2.org/events/user/event-type/userAccountLocked";
        public static final String POST_USER_PROFILE_UPDATED_EVENT = "https://schemas.identity.wso2.org/events/user/event-type/userProfileUpdated";
        public static final String POST_ACCOUNT_ENABLE_EVENT = "https://schemas.identity.wso2.org/events/user/event-type/userEnabled";
        public static final String POST_ACCOUNT_DISABLE_EVENT = "https://schemas.identity.wso2.org/events/user/event-type/userDisabled";
        public static final String SESSION_CREATED_EVENT = "https://schemas.identity.wso2.org/events/session/event-type/sessionEstablished";
        public static final String SESSION_REVOKED_EVENT = "https://schemas.identity.wso2.org/events/session/event-type/sessionRevoked";
        public static final String SESSION_EXPIRED_EVENT = "https://schemas.identity.wso2.org/events/session/event-type/sessionExpired";
        public static final String SESSION_UPDATED_EVENT = "https://schemas.identity.wso2.org/events/session/event-type/sessionUpdated";
        public static final String SESSION_EXTENDED_EVENT = "https://schemas.identity.wso2.org/events/session/event-type/sessionExtended";
        public static final String SESSION_PRESENTED_EVENT = "https://schemas.identity.wso2.org/events/session/event-type/sessionPresented";
        public static final String TOKEN_ISSUED_EVENT = "https://schemas.identity.wso2.org/events/token/event-type/accessTokenIssued";
        public static final String TOKEN_REVOKED_EVENT = "https://schemas.identity.wso2.org/events/token/event-type/accessTokensRevoked";
        public static final String VERIFICATION_EVENT = "https://schemas.identity.wso2.org/events/user/event-type/verification";
    }
}
