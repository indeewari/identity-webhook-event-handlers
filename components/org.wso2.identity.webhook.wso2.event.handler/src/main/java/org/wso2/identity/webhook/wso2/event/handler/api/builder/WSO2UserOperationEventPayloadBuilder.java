/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.identity.webhook.wso2.event.handler.api.builder;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.core.context.IdentityContext;
import org.wso2.carbon.identity.core.context.model.Flow;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.publisher.api.model.EventPayload;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.identity.webhook.common.event.handler.api.builder.UserOperationEventPayloadBuilder;
import org.wso2.identity.webhook.common.event.handler.api.constants.Constants;
import org.wso2.identity.webhook.common.event.handler.api.model.EventData;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.WSO2UserAccountEventPayload;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.WSO2UserCreatedEventPayload;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.WSO2UserGroupUpdateEventPayload;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.Group;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.Organization;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.User;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.UserClaim;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.UserStore;
import org.wso2.identity.webhook.wso2.event.handler.internal.util.WSO2PayloadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.USER_STORE_MANAGER;
import static org.wso2.identity.webhook.common.event.handler.internal.constant.Constants.PRE_DELETE_USER_ID;
import static org.wso2.identity.webhook.wso2.event.handler.internal.constant.Constants.SCIM2_USERS_ENDPOINT;

/**
 * WSO2 UserOperation Event Payload Builder.
 */
public class WSO2UserOperationEventPayloadBuilder implements UserOperationEventPayloadBuilder {

    @Override
    public EventPayload buildUserGroupUpdateEvent(EventData eventData) throws IdentityEventException {

        Organization tenant = WSO2PayloadUtils.buildTenant(eventData);
        UserStore userStore = WSO2PayloadUtils.buildUserStore(eventData);

        // todo: should remove retrieving user store manager as a property.
        //  Rather load user store managed from realm service.
        Group group = buildGroup(eventData);

        Flow flow = IdentityContext.getThreadLocalIdentityContext().getFlow();
        String initiatorType = null;
        if (flow != null) {
            initiatorType = flow.getInitiatingPersona().name();
        }

        return new WSO2UserGroupUpdateEventPayload.Builder()
                .initiatorType(initiatorType)
                .group(group)
                .tenant(tenant)
                .userStore(userStore)
                .build();
    }

    @Override
    public EventPayload buildUserDeleteEvent(EventData eventData) throws IdentityEventException {

        Organization tenant = WSO2PayloadUtils.buildTenant(eventData);
        UserStore userStore = WSO2PayloadUtils.buildUserStore(eventData);

        List<UserClaim> userClaims = new ArrayList<>();

        String userName =
                String.valueOf(eventData.getProperties().get(IdentityEventConstants.EventProperty.USER_NAME));
        Optional<UserClaim> userNameOptional =
                WSO2PayloadUtils.generateUserClaim(FrameworkConstants.USERNAME_CLAIM, userName,
                        eventData.getTenantDomain());
        userNameOptional.ifPresent(userClaims::add);

        if (eventData.getProperties().get("EMAIL_ADDRESS") != null) {
            String emailAddress = String.valueOf(eventData.getProperties().get("EMAIL_ADDRESS"));
            Optional<UserClaim> emailAddressOptional =
                    WSO2PayloadUtils.generateUserClaim(FrameworkConstants.EMAIL_ADDRESS_CLAIM, emailAddress,
                            eventData.getTenantDomain());
            emailAddressOptional.ifPresent(userClaims::add);
        }

        String userId;

        try {
            userId = String.valueOf(IdentityUtil.threadLocalProperties.get().get(PRE_DELETE_USER_ID));

            User deletedUser = new User();
            deletedUser.setId(userId);
            deletedUser.setRef(WSO2PayloadUtils.constructFullURLWithEndpoint(SCIM2_USERS_ENDPOINT) + "/" + userId);
            deletedUser.setClaims(userClaims);

            Flow flow = IdentityContext.getThreadLocalIdentityContext().getFlow();
            String initiatorType = null;
            if (flow != null) {
                initiatorType = flow.getInitiatingPersona().name();
            }

            return new WSO2UserAccountEventPayload.Builder()
                    .initiatorType(initiatorType)
                    .user(deletedUser)
                    .tenant(tenant)
                    .userStore(userStore)
                    .build();
        } finally {
            IdentityUtil.threadLocalProperties.get().remove(PRE_DELETE_USER_ID);
        }
    }

    @Override
    public EventPayload buildUserUnlockAccountEvent(EventData eventData) throws IdentityEventException {

        return this.buildUserAccountEvent(eventData);
    }

    private EventPayload buildUserAccountEvent(EventData eventData) throws IdentityEventException {

        Map<String, Object> properties = eventData.getProperties();
        String tenantDomain = eventData.getTenantDomain();

        Organization tenant = WSO2PayloadUtils.buildTenant(eventData);
        UserStore userStore = WSO2PayloadUtils.buildUserStore(eventData);

        AbstractUserStoreManager userStoreManager = (AbstractUserStoreManager) properties.get(USER_STORE_MANAGER);

        User user = new User();
        String userName =
                String.valueOf(eventData.getProperties().get(IdentityEventConstants.EventProperty.USER_NAME));
        enrichUser(userStoreManager, userName, user, tenantDomain);
        user.setRef(
                WSO2PayloadUtils.constructFullURLWithEndpoint(SCIM2_USERS_ENDPOINT) + "/" + user.getId());

        Flow flow = IdentityContext.getThreadLocalIdentityContext().getFlow();
        String initiatorType = null;
        if (flow != null) {
            initiatorType = flow.getInitiatingPersona().name();
        }

        return new WSO2UserAccountEventPayload.Builder()
                .user(user)
                .tenant(tenant)
                .userStore(userStore)
                .build();
    }

    private EventPayload buildUserEnableEvent(EventData eventData) throws IdentityEventException {

        Map<String, Object> properties = eventData.getProperties();
        String tenantDomain = String.valueOf(properties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN));

        Organization tenant = WSO2PayloadUtils.buildTenant(eventData);
        UserStore userStore = WSO2PayloadUtils.buildUserStore(eventData);

        String userId = String.valueOf(eventData.getProperties().get(IdentityEventConstants.EventProperty.USER_ID));
        User user = new User();
        user.setId(userId);

        AbstractUserStoreManager userStoreManager = null;
        Object userStoreManagerObj = properties.get(IdentityEventConstants.EventProperty.USER_STORE_MANAGER);
        if (userStoreManagerObj instanceof AbstractUserStoreManager) {
            userStoreManager = (AbstractUserStoreManager) properties.get(USER_STORE_MANAGER);
        }
        String userName =
                String.valueOf(eventData.getProperties().get(IdentityEventConstants.EventProperty.USER_NAME));
        enrichUser(userStoreManager, userName, user, tenantDomain);

        user.setRef(
                WSO2PayloadUtils.constructFullURLWithEndpoint(SCIM2_USERS_ENDPOINT) + "/" + user.getId());

        Flow flow = IdentityContext.getThreadLocalIdentityContext().getFlow();
        String initiatorType = null;
        if (flow != null) {
            initiatorType = flow.getInitiatingPersona().name();
        }

        return new WSO2UserAccountEventPayload.Builder()
                .initiatorType(initiatorType)
                .user(user)
                .tenant(tenant)
                .userStore(userStore)
                .build();
    }

    @Override
    public EventPayload buildUserLockAccountEvent(EventData eventData) throws IdentityEventException {

        return this.buildUserAccountEvent(eventData);
    }

    @Override
    public EventPayload buildUserProfileUpdateEvent(EventData eventData) throws IdentityEventException {

        Map<String, Object> properties = eventData.getProperties();
        String tenantDomain = String.valueOf(properties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN));
        String userId = String.valueOf(properties.get(IdentityEventConstants.EventProperty.USER_ID));

        Organization tenant = WSO2PayloadUtils.buildTenant(eventData);
        UserStore userStore = WSO2PayloadUtils.buildUserStore(eventData);

        List<UserClaim> addedClaims =
                populateClaims(properties, IdentityEventConstants.EventProperty.USER_CLAIMS_ADDED, tenantDomain);
        List<UserClaim> modifiedClaims =
                populateClaims(properties, IdentityEventConstants.EventProperty.USER_CLAIMS_MODIFIED, tenantDomain);
        List<UserClaim> deletedClaims =
                populateClaims(properties, IdentityEventConstants.EventProperty.USER_CLAIMS_DELETED, tenantDomain);
        List<UserClaim> additionalClaims = populateClaims(properties, "ADDITIONAL_USER_CLAIMS", tenantDomain);

        User user = new User();
        user.setId(userId);
        user.setRef(
                WSO2PayloadUtils.constructFullURLWithEndpoint(SCIM2_USERS_ENDPOINT) + "/" + user.getId());
        user.setAdditionalClaims(additionalClaims);
        user.setAddedClaims(addedClaims);
        user.setUpdatedClaims(modifiedClaims);
        user.setRemovedClaims(deletedClaims);

        Flow flow = IdentityContext.getThreadLocalIdentityContext().getFlow();
        String initiatorType = null;
        String action = null;
        if (flow != null) {
            initiatorType = flow.getInitiatingPersona().name();
            action = Optional.ofNullable(resolveAction(flow.getName()))
                    .map(Enum::name)
                    .orElse(null);
        }

        return new WSO2UserAccountEventPayload.Builder()
                .initiatorType(initiatorType)
                .action(action)
                .user(user)
                .tenant(tenant)
                .userStore(userStore)
                .build();
    }

    @Override
    public EventPayload buildUserAccountEnableEvent(EventData eventData) throws IdentityEventException {

        return this.buildUserEnableEvent(eventData);
    }

    @Override
    public EventPayload buildUserAccountDisableEvent(EventData eventData) throws IdentityEventException {

        return this.buildUserEnableEvent(eventData);
    }

    @Override
    public EventPayload buildUserCreatedEvent(EventData eventData) throws IdentityEventException {

        Map<String, Object> properties = eventData.getProperties();
        String tenantDomain = String.valueOf(properties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN));

        UserStore userStore = WSO2PayloadUtils.buildUserStore(eventData);

        User newUser = new User();
        WSO2PayloadUtils.enrichUser(properties, newUser, tenantDomain);

        if (StringUtils.isBlank(newUser.getId())) {
            // User set password flow for email invitation by admin.
            newUser = WSO2PayloadUtils.buildUser(eventData);
        }

        String tenantId = null;
        if (properties.get(IdentityEventConstants.EventProperty.TENANT_ID) != null) {
            tenantId = String.valueOf(properties.get(IdentityEventConstants.EventProperty.TENANT_ID));
        } else {
            RealmConfiguration realmConfiguration = WSO2PayloadUtils.getRealmConfigurationByTenantDomain(tenantDomain);
            if (realmConfiguration != null)
                tenantId = String.valueOf(realmConfiguration.getTenantId());
        }

        Organization organization = new Organization(tenantId, tenantDomain);
        Flow flow = IdentityContext.getThreadLocalIdentityContext().getFlow();
        String initiatorType = null;
        String action = null;
        if (flow != null) {
            initiatorType = flow.getInitiatingPersona().name();
            action = Optional.ofNullable(resolveAction(flow.getName()))
                    .map(Enum::name)
                    .orElse(null);
        }

        return new WSO2UserCreatedEventPayload.Builder()
                .initiatorType(initiatorType)
                .action(action)
                .user(newUser)
                .tenant(organization)
                .userStore(userStore)
                .build();
    }

    private List<UserClaim> populateClaims(Map<String, Object> properties, String userClaimKey, String tenantDomain) {

        if (properties != null && properties.get(userClaimKey) instanceof Map) {

            Map<String, String> userClaimsMap = (Map<String, String>) properties.get(userClaimKey);
            List<UserClaim> userClaims = new ArrayList<>();

            for (Map.Entry<String, String> entry : userClaimsMap.entrySet()) {
                Optional<UserClaim> userClaimOptional =
                        WSO2PayloadUtils.generateUserClaim(entry.getKey(), entry.getValue(), tenantDomain);
                userClaimOptional.ifPresent(userClaims::add);
            }
            return userClaims;
        }
        return null;
    }

    private List<User> buildUserList(AbstractUserStoreManager userStoreManager, Map<String, Object> properties,
                                     String userListPropertyName, String tenantDomain) throws IdentityEventException {

        List<User> users = new ArrayList<>();

        String[] domainQualifiedUsernames = (String[]) properties.get(userListPropertyName);
        if (domainQualifiedUsernames != null) {
            for (String domainQualifiedUsername : domainQualifiedUsernames) {
                User user = new User();
                enrichUser(userStoreManager, domainQualifiedUsername, user, tenantDomain);
                users.add(user);
            }
        }
        return users;
    }

    private static void enrichUser(UserStoreManager userStoreManager, String domainQualifiedUserName, User user,
                                   String tenantDomain)
            throws IdentityEventException {

        String userId;
        try {
            if (StringUtils.isEmpty(user.getId())) {
                userId = userStoreManager.getUserClaimValue(domainQualifiedUserName, FrameworkConstants.USER_ID_CLAIM,
                        UserCoreConstants.DEFAULT_PROFILE);
                user.setId(userId);
            }

            String emailAddress =
                    userStoreManager.getUserClaimValue(domainQualifiedUserName, FrameworkConstants.EMAIL_ADDRESS_CLAIM,
                            UserCoreConstants.DEFAULT_PROFILE);
            Optional<UserClaim> emailAddressUserOptional =
                    WSO2PayloadUtils.generateUserClaim(FrameworkConstants.EMAIL_ADDRESS_CLAIM, emailAddress,
                            tenantDomain);
            emailAddressUserOptional.ifPresent(user::addClaim);
        } catch (UserStoreException e) {
            throw new IdentityEventException(
                    "Error while extracting user claims for the user : " + domainQualifiedUserName, e);
        }
    }

    @Override
    public Constants.EventSchema getEventSchemaType() {

        return Constants.EventSchema.WSO2;
    }

    private Group buildGroup(EventData eventData) throws IdentityEventException {

        if (eventData == null || eventData.getProperties() == null) {
            return null;
        }

        Map<String, Object> properties = eventData.getProperties();

        org.wso2.carbon.user.core.common.Group groupFromUserStore;
        String groupName = String.valueOf(properties.get(IdentityEventConstants.EventProperty.ROLE_NAME));

        AbstractUserStoreManager userStoreManager =
                (AbstractUserStoreManager) WSO2PayloadUtils.getUserStoreManagerByTenantDomain(
                        eventData.getTenantDomain());
        try {
            groupFromUserStore = userStoreManager.getGroupByGroupName(groupName, null);
        } catch (UserStoreException e) {
            throw new IdentityEventException("Error while extracting group Id for the group Name: " + groupName, e);
        }
        String groupId = groupFromUserStore.getGroupID();
        String groupLocation = groupFromUserStore.getLocation();

        List<User> deletedUsers =
                buildUserList(userStoreManager, properties, IdentityEventConstants.EventProperty.DELETED_USERS,
                        eventData.getTenantDomain());
        List<User> addedUsers =
                buildUserList(userStoreManager, properties, IdentityEventConstants.EventProperty.NEW_USERS,
                        eventData.getTenantDomain());

        Group group = new Group();
        group.setName(groupName);
        group.setRef(groupLocation);
        group.setId(groupId);
        group.setRemovedUsers(deletedUsers);
        group.setAddedUsers(addedUsers);

        return group;
    }

    private UserOperationAction resolveAction(Flow.Name name) {

        if (name == null) {
            return null;
        }

        switch (name) {
            case PROFILE_UPDATE:
                return UserOperationAction.UPDATE;
            case USER_REGISTRATION_INVITE_WITH_PASSWORD:
            case INVITED_USER_REGISTRATION:
                return UserOperationAction.INVITE;
            case USER_REGISTRATION:
                return UserOperationAction.REGISTER;
            case JIT_PROVISION:
                return UserOperationAction.JUST_IN_TIME;
            default: {
                return null;
            }
        }
    }

    public enum UserOperationAction {
        INVITE, UPDATE, REGISTER, JUST_IN_TIME
    }
}
