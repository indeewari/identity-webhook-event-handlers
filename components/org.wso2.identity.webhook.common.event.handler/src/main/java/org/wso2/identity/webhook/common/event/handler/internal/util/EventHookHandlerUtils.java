/*
 * Copyright (c) 2024-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.identity.webhook.common.event.handler.internal.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.MDC;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.UserIdNotFoundException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.publisher.api.model.EventPayload;
import org.wso2.carbon.identity.event.publisher.api.model.SecurityEventTokenPayload;
import org.wso2.carbon.identity.event.publisher.api.model.common.ComplexSubject;
import org.wso2.carbon.identity.event.publisher.api.model.common.SimpleSubject;
import org.wso2.carbon.identity.event.publisher.api.model.common.Subject;
import org.wso2.identity.webhook.common.event.handler.api.EventProfileManager;
import org.wso2.identity.webhook.common.event.handler.api.model.EventData;
import org.wso2.identity.webhook.common.event.handler.api.model.EventMetadata;
import org.wso2.identity.webhook.common.event.handler.internal.component.EventHookHandlerDataHolder;
import org.wso2.identity.webhook.common.event.handler.internal.constant.Constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils.CORRELATION_ID_MDC;

/**
 * Utility class for Event Handler Hooks.
 */
public class EventHookHandlerUtils {

    private static final Log log = LogFactory.getLog(EventHookHandlerUtils.class);

    private EventHookHandlerUtils() {

    }

    /**
     * Build the event data provider.
     *
     * @param event Event object.
     * @return Event data object.
     */
    public static EventData buildEventDataProvider(Event event) throws IdentityEventException {

        Map<String, Object> properties = validateAndGetProperties(event);

        Map<String, Object> params = extractParams(properties);
        AuthenticationContext context = extractAuthenticationContext(properties);
        AuthenticatorStatus status = extractAuthenticatorStatus(properties);
        HttpServletRequest request = extractRequest(params);

        String tenantDomain = resolveTenantDomain(context, params, properties);

        AuthenticatedUser authenticatedUser = extractAuthenticatedUser(params, context);
        String userId = resolveUserId(authenticatedUser, properties);

        SessionContext sessionContext = extractSessionContext(properties);

        return EventData.builder()
                .eventName(event.getEventName())
                .request(request)
                .eventParams(params)
                .properties(properties)
                .authenticationContext(context)
                .authenticatorStatus(status)
                .authenticatedUser(authenticatedUser)
                .sessionContext(sessionContext)
                .userId(userId)
                .tenantDomain(tenantDomain)
                .build();
    }

    /**
     * Retrieve the audience.
     *
     * @param eventPayload
     * @param eventUri     Event URI.
     * @return Audience string.
     */
    public static SecurityEventTokenPayload buildSecurityEventToken(EventPayload eventPayload, String eventUri)
            throws IdentityEventException {

        return buildSecurityEventToken(eventPayload, eventUri, null);
    }

    /**
     * Retrieve the audience.
     *
     * @param eventUri Event URI.
     * @return Audience string.
     */
    public static SecurityEventTokenPayload buildSecurityEventToken(EventPayload eventPayload,
                                                                    String eventUri, Subject subId)
            throws IdentityEventException {

        if (eventPayload == null) {
            throw new IdentityEventException("Invalid event payload input: Event payload input cannot be null.");
        }

        if (StringUtils.isEmpty(eventUri)) {
            throw new IdentityEventException("Invalid event URI input: Event URI input cannot be null or empty.");
        }

        Map<String, EventPayload> eventMap = new HashMap<>();
        eventMap.put(eventUri, eventPayload);

        return SecurityEventTokenPayload.builder()
                .iss(constructBaseURL())
                .iat(System.currentTimeMillis())
                .jti(UUID.randomUUID().toString())
                .rci(getCorrelationID())
                .subId(subId)
                .events(eventMap)
                .build();
    }

    /**
     * Get correlation id from the MDC.
     * If not then generate a random UUID, add it to MDC and return the UUID.
     *
     * @return Correlation id
     */
    public static String getCorrelationID() {

        return MDC.get(CORRELATION_ID_MDC);
    }

    /**
     * Get the tenant qualified URL.
     *
     * @return Tenant qualified URL.
     */
    public static String constructBaseURL() {

        try {
            ServiceURLBuilder builder = ServiceURLBuilder.create();
            return builder.build().getAbsolutePublicURL();
        } catch (URLBuilderException e) {
            log.debug("Error occurred while building the tenant qualified URL.", e);
        }
        return null;
    }

    /**
     * Get the EventMetadata for the given event profile and event name.
     *
     * @param eventProfile Event profile.
     * @param event        Event name.
     * @return EventMetadata if found, otherwise null.
     */
    public static EventMetadata getEventProfileManagerByProfile(String eventProfile, String event) {

        for (EventProfileManager manager : EventHookHandlerDataHolder.getInstance().getEventProfileManagers()) {
            EventMetadata metadata = manager.resolveEventMetadata(event);
            if (metadata != null && eventProfile.equals(metadata.getEventProfile())) {
                return metadata;
            }
        }
        return null;
    }

    // todo: Following methods are CAEP specific. These should move out from this common util.

    /**
     * Extracts the authenticated user from the event data.
     *
     * @param eventData Event data.
     * @return Authenticated user.
     * @throws IdentityEventException If an error occurs while extracting the authenticated user.
     */
    private static AuthenticatedUser extractAuthenticatedUser(EventData eventData) throws IdentityEventException {

        AuthenticatedUser authenticatedUser = eventData.getAuthenticatedUser();
        try {
            if (authenticatedUser == null) {
                authenticatedUser = (AuthenticatedUser) eventData.getEventParams().
                        get(Constants.EventDataProperties.USER);
            }
            return authenticatedUser;
        } catch (ClassCastException e) {
            throw new IdentityEventException("Error occurred while retrieving authenticated user", e);
        }
    }

    /**
     * Extracts the session ID from the event data.
     *
     * @param eventData Event data.
     * @return Session ID.
     * @throws IdentityEventException If an error occurs while extracting the session ID.
     */
    public static String extractSessionId(EventData eventData) {

        if (eventData.getEventParams().containsKey(Constants.EventDataProperties.SESSION_ID) &&
                eventData.getEventParams().get(Constants.EventDataProperties.SESSION_ID) != null) {
            return eventData.getEventParams().get(Constants.EventDataProperties.SESSION_ID).toString();
        } else if (eventData.getAuthenticationContext() != null) {
            return eventData.getAuthenticationContext().getSessionIdentifier();
        }
        return null;
    }

    /**
     * Extracts the subject from the event data.
     *
     * @param eventData Event data.
     * @return Subject.
     * @throws IdentityEventException If an error occurs while extracting the subject.
     */
    public static Subject extractSubjectFromEventData(EventData eventData) throws IdentityEventException {

        AuthenticatedUser authenticatedUser = extractAuthenticatedUser(eventData);
        String sessionId = extractSessionId(eventData);
        SimpleSubject user;
        try {
            user = SimpleSubject.createOpaqueSubject(authenticatedUser.getUserId());
        } catch (UserIdNotFoundException e) {
            throw new IdentityEventException("Error occurred while retrieving user id", e);
        }
        SimpleSubject tenant = SimpleSubject.createOpaqueSubject(String.valueOf(
                IdentityTenantUtil.getTenantId(authenticatedUser.getTenantDomain())));
        SimpleSubject session = SimpleSubject.createOpaqueSubject(sessionId);

        return ComplexSubject.builder()
                .tenant(tenant)
                .user(user)
                .session(session)
                .build();
    }

    public static Subject buildVerificationSubject(EventData eventData) throws IdentityEventException {

        Map<String, Object> params = eventData.getEventParams();
        String streamId = params.get(Constants.EventDataProperties.STREAM_ID) != null ?
                params.get(Constants.EventDataProperties.STREAM_ID).toString() : null;
        if (streamId == null) {
            throw new IdentityEventException("Stream ID cannot be null");
        }

        return SimpleSubject.createOpaqueSubject(streamId);

    }

    private static Map<String, Object> validateAndGetProperties(Event event) throws IdentityEventException {

        Map<String, Object> properties = event.getEventProperties();
        if (properties == null) {
            throw new IdentityEventException("Properties cannot be null");
        }
        return properties;
    }

    private static Map<String, Object> extractParams(Map<String, Object> properties) {

        // todo: remove logic that returns properties as parameters and using params field to get properties.
        //  Introduce a separate method to return all properties if event specific properties needs to be handled.

        return properties.containsKey(Constants.EventDataProperties.PARAMS) ?
                (Map<String, Object>) properties.get(Constants.EventDataProperties.PARAMS) : Collections.emptyMap();
    }

    private static AuthenticationContext extractAuthenticationContext(Map<String, Object> properties) {

        return properties.containsKey(Constants.EventDataProperties.CONTEXT) ?
                (AuthenticationContext) properties.get(Constants.EventDataProperties.CONTEXT) : null;
    }

    private static AuthenticatorStatus extractAuthenticatorStatus(Map<String, Object> properties) {

        return properties.containsKey(Constants.EventDataProperties.AUTHENTICATION_STATUS) ?
                (AuthenticatorStatus) properties.get(Constants.EventDataProperties.AUTHENTICATION_STATUS) : null;
    }

    private static HttpServletRequest extractRequest(Map<String, Object> params) {

        return params != null ? (HttpServletRequest) params.get(Constants.EventDataProperties.REQUEST) : null;
    }

    private static String resolveTenantDomain(AuthenticationContext context, Map<String, Object> params,
                                              Map<String, Object> properties) {

        if (context != null && StringUtils.isNotBlank(context.getLoginTenantDomain())) {
            return context.getLoginTenantDomain();
        }
        if (properties != null && properties.containsKey(IdentityEventConstants.EventProperty.TENANT_DOMAIN)) {
            return String.valueOf(properties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN));
        }
        return (params != null && params.containsKey(IdentityEventConstants.EventProperty.TENANT_DOMAIN)) ?
                String.valueOf(params.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN)) :
                PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    private static AuthenticatedUser extractAuthenticatedUser(Map<String, Object> params,
                                                              AuthenticationContext context) {

        if (params != null) {
            Object user = params.get(Constants.EventDataProperties.USER);
            if (user instanceof AuthenticatedUser) {
                AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
                if (context != null) {
                     /* todo: it's not a good practice to modify the authenticated user object.
                      Should remove this code and missing claims should be populated for the user
                      in event in a different way.*/
                    setLocalUserClaimsToAuthenticatedUser(authenticatedUser, context);
                }
                return authenticatedUser;
            }
        }
        return null;
    }

    private static String resolveUserId(AuthenticatedUser authenticatedUser, Map<String, Object> properties) {

        if (authenticatedUser != null) {
            try {
                return authenticatedUser.getUserId();
            } catch (UserIdNotFoundException e) {
                log.debug("User ID not found for the authenticated user: " + authenticatedUser.getUserName());
            }
        }

        if (properties != null && properties.containsKey(IdentityEventConstants.EventProperty.USER_ID)) {
            return (String) properties.get(IdentityEventConstants.EventProperty.USER_ID);
        }
        return null;
    }

    private static SessionContext extractSessionContext(Map<String, Object> properties) {

        return properties.containsKey(Constants.EventDataProperties.SESSION_CONTEXT) ?
                (SessionContext) properties.get(Constants.EventDataProperties.SESSION_CONTEXT) : null;
    }

    private static void setLocalUserClaimsToAuthenticatedUser(AuthenticatedUser authenticatedUser,
                                                              AuthenticationContext context) {

        Map<String, String> claimMappings = (Map<String, String>) context.getParameters()
                .get(Constants.SP_TO_CARBON_CLAIM_MAPPING);

        if (claimMappings == null) {
            log.debug("No local claim mappings found for the authenticated user from the context.");
            return;
        }

        Map<ClaimMapping, String> userAttributes = authenticatedUser.getUserAttributes();

        if (userAttributes == null) {
            userAttributes = new HashMap<>();
        }

        for (Map.Entry<ClaimMapping, String> entry : userAttributes.entrySet()) {

            ClaimMapping claimMapping = entry.getKey();
            Claim localClaim = claimMapping.getLocalClaim();

            if (claimMappings.containsKey(localClaim.getClaimUri())) {
                localClaim.setClaimUri(claimMappings.get(localClaim.getClaimUri()));
            }
        }
    }
}
