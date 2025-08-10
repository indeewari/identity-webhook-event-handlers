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

package org.wso2.identity.webhook.common.event.handler.internal.component;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.event.publisher.api.service.EventPublisherService;
import org.wso2.identity.webhook.common.event.handler.api.builder.CredentialEventPayloadBuilder;
import org.wso2.identity.webhook.common.event.handler.api.builder.LoginEventPayloadBuilder;
import org.wso2.identity.webhook.common.event.handler.api.builder.RegistrationEventPayloadBuilder;
import org.wso2.identity.webhook.common.event.handler.api.builder.SessionEventPayloadBuilder;
import org.wso2.identity.webhook.common.event.handler.api.builder.TokenEventPayloadBuilder;
import org.wso2.identity.webhook.common.event.handler.api.builder.UserOperationEventPayloadBuilder;
import org.wso2.identity.webhook.common.event.handler.api.constants.Constants;

import static org.mockito.Mockito.when;

/**
 * Test class for EventHookHandlerServiceComponent.
 */
public class EventHookHandlerServiceComponentTest {

    @InjectMocks
    private EventHookHandlerServiceComponent serviceComponent;

    @Mock
    private ComponentContext componentContext;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private CredentialEventPayloadBuilder credentialBuilder;

    @Mock
    private SessionEventPayloadBuilder sessionBuilder;

    @Mock
    private LoginEventPayloadBuilder loginBuilder;

    @Mock
    private UserOperationEventPayloadBuilder userOperationBuilder;

    @Mock
    private RegistrationEventPayloadBuilder registrationBuilder;

    @Mock
    private ConfigurationManager configurationManager;

    @Mock
    private EventPublisherService eventPublisherService;

    @Mock
    private TokenEventPayloadBuilder tokenEventPayloadBuilder;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
    }

    @Test
    public void testAddAndRemoveCredentialEventPayloadBuilder() {

        when(credentialBuilder.getEventSchemaType()).thenReturn(Constants.EventSchema.WSO2);
        serviceComponent.addCredentialEventPayloadBuilder(credentialBuilder);
        serviceComponent.removeCredentialEventPayloadBuilder(credentialBuilder);
    }

    @Test
    public void testAddAndRemoveSessionEventPayloadBuilder() {

        when(sessionBuilder.getEventSchemaType()).thenReturn(Constants.EventSchema.WSO2);
        serviceComponent.addSessionEventPayloadBuilder(sessionBuilder);
        serviceComponent.removeSessionEventPayloadBuilder(sessionBuilder);
    }

    @Test
    public void testAddAndRemoveLoginEventPayloadBuilder() {

        when(loginBuilder.getEventSchemaType()).thenReturn(Constants.EventSchema.WSO2);
        serviceComponent.addLoginEventPayloadBuilder(loginBuilder);
        serviceComponent.removeLoginEventPayloadBuilder(loginBuilder);
    }

    @Test
    public void testAddAndRemoveUserOperationEventPayloadBuilder() {

        when(userOperationBuilder.getEventSchemaType()).thenReturn(Constants.EventSchema.WSO2);
        serviceComponent.addUserOperationEventPayloadBuilder(userOperationBuilder);
        serviceComponent.removeUserOperationEventPayloadBuilder(userOperationBuilder);
    }

    @Test
    public void testRegisterAndUnregisterConfigurationManager() {

        serviceComponent.registerConfigurationManager(configurationManager);
        serviceComponent.unregisterConfigurationManager(configurationManager);
    }

    @Test
    public void testSetAndUnsetEventPublisherService() {

        serviceComponent.setEventPublisherService(eventPublisherService);
        serviceComponent.unsetEventPublisherService(eventPublisherService);
    }

    @Test
    public void testAddAndRemoveRegistrationEventPayloadBuilder() {

        when(registrationBuilder.getEventSchemaType()).thenReturn(Constants.EventSchema.WSO2);
        serviceComponent.addRegistrationEventPayloadBuilder(registrationBuilder);
        serviceComponent.removeRegistrationEventPayloadBuilder(registrationBuilder);
    }

    @Test
    public void testAddAndRemoveTokenEventPayloadBuilder() {

        when(tokenEventPayloadBuilder.getEventSchemaType()).thenReturn(Constants.EventSchema.WSO2);
        serviceComponent.addTokenEventPayloadBuilder(tokenEventPayloadBuilder);
        serviceComponent.removeTokenEventPayloadBuilder(tokenEventPayloadBuilder);
    }
}
