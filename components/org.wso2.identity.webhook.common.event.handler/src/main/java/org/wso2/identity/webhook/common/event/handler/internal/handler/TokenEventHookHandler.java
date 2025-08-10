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

package org.wso2.identity.webhook.common.event.handler.internal.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.publisher.api.exception.EventPublisherException;
import org.wso2.carbon.identity.event.publisher.api.model.EventContext;
import org.wso2.carbon.identity.event.publisher.api.model.EventPayload;
import org.wso2.carbon.identity.event.publisher.api.model.SecurityEventTokenPayload;
import org.wso2.carbon.identity.webhook.metadata.api.model.Channel;
import org.wso2.carbon.identity.webhook.metadata.api.model.EventProfile;
import org.wso2.identity.webhook.common.event.handler.api.builder.TokenEventPayloadBuilder;
import org.wso2.identity.webhook.common.event.handler.api.model.EventData;
import org.wso2.identity.webhook.common.event.handler.api.model.EventMetadata;
import org.wso2.identity.webhook.common.event.handler.internal.component.EventHookHandlerDataHolder;
import org.wso2.identity.webhook.common.event.handler.internal.constant.Constants;
import org.wso2.identity.webhook.common.event.handler.internal.util.EventHookHandlerUtils;
import org.wso2.identity.webhook.common.event.handler.internal.util.PayloadBuilderFactory;

import java.util.List;
import java.util.Objects;

/**
 * TokenEventHookHandler class.
 * This class is a placeholder for handling token-related events.
 * Currently, it does not contain any methods or properties.
 */
public class TokenEventHookHandler extends AbstractEventHandler {

    private static final Log log = LogFactory.getLog(TokenEventHookHandler.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        EventData eventData = EventHookHandlerUtils.buildEventDataProvider(event);

        try {
            List<EventProfile> eventProfileList =
                    EventHookHandlerDataHolder.getInstance().getWebhookMetadataService().getSupportedEventProfiles();
            if (eventProfileList.isEmpty()) {
                log.warn("No event profiles found in the webhook metadata service. Skipping token event handling.");
                return;
            }
            for (EventProfile eventProfile : eventProfileList) {
                handleEventForProfile(event, eventData, eventProfile);
            }
        } catch (Exception e) {
            log.warn("Error while retrieving token event publisher configuration for tenant.", e);
        }
    }

    @Override
    public String getName() {

        return Constants.TOKEN_EVENT_HOOK_NAME;
    }

    private void handleEventForProfile(Event event, EventData eventData, EventProfile eventProfile)
            throws IdentityEventException, EventPublisherException {

        // Prepare schema, payload builder, and event metadata
        org.wso2.identity.webhook.common.event.handler.api.constants.Constants.EventSchema schema =
                org.wso2.identity.webhook.common.event.handler.api.constants.Constants.EventSchema.valueOf(
                        eventProfile.getProfile());
        TokenEventPayloadBuilder payloadBuilder = PayloadBuilderFactory.getTokenEventPayloadBuilder(schema);

        if (payloadBuilder == null) {
            log.debug("Skipping token event handling for profile " + eventProfile.getProfile());
            return;
        }

        EventMetadata eventMetadata = EventHookHandlerUtils.getEventProfileManagerByProfile(
                eventProfile.getProfile(), event.getEventName());
        if (eventMetadata == null) {
            log.debug("No event metadata found for event: " + event.getEventName() +
                    " in profile: " + eventProfile.getProfile());
            return;
        }

        // Get channel and event URI
        Channel tokenChannel = eventProfile.getChannels().stream()
                .filter(channel -> eventMetadata.getChannel().equals(channel.getUri()))
                .findFirst()
                .orElse(null);
        if (tokenChannel == null) {
            log.debug("No channel found for token event profile: " + eventProfile.getProfile());
            return;
        }

        String eventUri = tokenChannel.getEvents().stream()
                .filter(channelEvent -> Objects.equals(eventMetadata.getEvent(), channelEvent.getEventUri()))
                .findFirst()
                .map(org.wso2.carbon.identity.webhook.metadata.api.model.Event::getEventUri)
                .orElse(null);

        // Publish for current accessing org
        String tenantDomain = eventData.getTenantDomain();
        publishEvent(tenantDomain, tokenChannel, eventUri, eventProfile.getProfile(),
                payloadBuilder, eventData, event.getEventName());
    }

    private void publishEvent(String tenantDomain, Channel tokenChannel, String eventUri, String eventProfileName,
                              TokenEventPayloadBuilder payloadBuilder, EventData eventData, String eventName)
            throws IdentityEventException, EventPublisherException {

        EventContext eventContext = EventContext.builder()
                .tenantDomain(tenantDomain)
                .eventUri(tokenChannel.getUri())
                .eventProfileName(eventProfileName)
                .eventProfileVersion(Constants.EVENT_PROFILE_VERSION)
                .build();

        if (!EventHookHandlerDataHolder.getInstance().getEventPublisherService().canHandleEvent(eventContext)) {
            return;
        }

        EventPayload eventPayload;
        if ((IdentityEventConstants.Event.TOKEN_REVOKED.equals(eventName))) {
            eventPayload = payloadBuilder.buildAccessTokenRevokeEvent(eventData);
        } else if (IdentityEventConstants.Event.TOKEN_ISSUED.equals(eventName)) {
            eventPayload = payloadBuilder.buildAccessTokenIssueEvent(eventData);
        } else {
            throw new IdentityRuntimeException("Unsupported event type: " + eventName);
        }

        log.debug("Publishing token event: " + eventName + " for tenant: " + tenantDomain +
                " with event URI: " + eventUri + " and profile: " + eventProfileName);
        SecurityEventTokenPayload securityEventTokenPayload =
                EventHookHandlerUtils.buildSecurityEventToken(eventPayload, eventUri);
        EventHookHandlerDataHolder.getInstance().getEventPublisherService()
                .publish(securityEventTokenPayload, eventContext);
    }
}
