package org.wso2.identity.webhook.common.event.handler.internal.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.bean.context.MessageContext;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.bean.IdentityEventMessageContext;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.identity.event.common.publisher.model.EventPayload;
import org.wso2.identity.event.common.publisher.model.SecurityEventTokenPayload;
import org.wso2.identity.webhook.common.event.handler.api.builder.TokensEventPayloadBuilder;
import org.wso2.identity.webhook.common.event.handler.api.constants.EventSchema;
import org.wso2.identity.webhook.common.event.handler.api.model.EventData;
import org.wso2.identity.webhook.common.event.handler.internal.config.EventPublisherConfig;
import org.wso2.identity.webhook.common.event.handler.internal.constant.Constants;
import org.wso2.identity.webhook.common.event.handler.internal.util.EventConfigManager;
import org.wso2.identity.webhook.common.event.handler.internal.util.EventHookHandlerUtils;
import org.wso2.identity.webhook.common.event.handler.internal.util.PayloadBuilderFactory;

public class TokensEventHookHandler extends AbstractEventHandler {

    private static final Log log = LogFactory.getLog(TokensEventHookHandler.class);
    private final EventConfigManager eventConfigManager;

    public TokensEventHookHandler(EventConfigManager eventConfigManager) {

        this.eventConfigManager = eventConfigManager;
    }

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        EventData eventData = EventHookHandlerUtils.buildEventDataProvider(event);

        TokensEventPayloadBuilder payloadBuilder = PayloadBuilderFactory
                .getTokensEventPayloadBuilder(EventSchema.WSO2);
        EventPublisherConfig tokensEventPublisherConfig;
        try {

            String tenantDomain =
                    String.valueOf(eventData.getEventParams().get(IdentityEventConstants.EventProperty.TENANT_DOMAIN));

            tokensEventPublisherConfig =
                    eventConfigManager.getEventPublisherConfigForTenant(tenantDomain, event.getEventName());

            EventPayload eventPayload;
            String eventUri;

            if ((IdentityEventConstants.EventName.SESSION_TERMINATE.name().equals(event.getEventName())) &&
                    tokensEventPublisherConfig.isPublishEnabled()) {
                eventPayload = payloadBuilder.buildAccessTokenRevokeEvent(eventData);
                eventUri =
                        eventConfigManager.getEventUri(
                                Constants.EventHandlerKey.WSO2.POST_TOKEN_REVOKE_EVENT);
                SecurityEventTokenPayload securityEventTokenPayload = EventHookHandlerUtils
                        .buildSecurityEventToken(eventPayload, eventUri);
                EventHookHandlerUtils.publishEventPayload(securityEventTokenPayload, tenantDomain, eventUri);
            }
        } catch (IdentityEventException e) {
            log.debug("Error while retrieving event publisher configuration for tenant.", e);
        }
    }

    @Override
    public String getName() {

        return Constants.TOKENS_EVENT_HOOK_NAME;
    }

    @Override
    public boolean canHandle(MessageContext messageContext) throws IdentityRuntimeException {

        IdentityEventMessageContext identityContext = (IdentityEventMessageContext) messageContext;
        String eventName = identityContext.getEvent().getEventName();

        boolean canHandle = isSupportedEvent(eventName);
        if (canHandle) {
            log.debug(eventName + " event can be handled.");
        } else {
            log.debug(eventName + " event cannot be handled.");
        }
        return canHandle;
    }

    private boolean isSupportedEvent(String eventName) {

        return IdentityEventConstants.EventName.SESSION_TERMINATE.name().equals(eventName);
    }
}
