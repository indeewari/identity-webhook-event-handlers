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

package org.wso2.identity.webhook.wso2.event.handler.api.builder;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.identity.core.context.IdentityContext;
import org.wso2.carbon.identity.core.context.model.Flow;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.publisher.api.model.EventPayload;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.config.RealmConfiguration;
import org.wso2.identity.webhook.common.event.handler.api.constants.Constants.EventSchema;
import org.wso2.identity.webhook.common.event.handler.api.model.EventData;
import org.wso2.identity.webhook.wso2.event.handler.internal.component.WSO2EventHookHandlerDataHolder;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.WSO2BaseEventPayload;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.WSO2RegistrationFailureEventPayload;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.WSO2RegistrationSuccessEventPayload;
import org.wso2.identity.webhook.wso2.event.handler.internal.model.common.UserClaim;
import org.wso2.identity.webhook.wso2.event.handler.internal.util.CommonTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.USER_STORE_MANAGER;
import static org.wso2.identity.webhook.wso2.event.handler.internal.constant.Constants.FIRST_NAME_CLAIM_URI;
import static org.wso2.identity.webhook.wso2.event.handler.internal.constant.Constants.LAST_NAME_CLAIM_URI;
import static org.wso2.identity.webhook.wso2.event.handler.internal.constant.Constants.LOCATION_CLAIM;
import static org.wso2.identity.webhook.wso2.event.handler.internal.constant.Constants.SCIM2_USERS_ENDPOINT;
import static org.wso2.identity.webhook.wso2.event.handler.internal.util.TestUtils.closeMockedIdentityTenantUtil;
import static org.wso2.identity.webhook.wso2.event.handler.internal.util.TestUtils.closeMockedServiceURLBuilder;
import static org.wso2.identity.webhook.wso2.event.handler.internal.util.TestUtils.mockIdentityTenantUtil;
import static org.wso2.identity.webhook.wso2.event.handler.internal.util.TestUtils.mockServiceURLBuilder;
import static org.wso2.identity.webhook.wso2.event.handler.internal.util.WSO2PayloadUtils.constructFullURLWithEndpoint;

public class WSO2RegistrationEventPayloadBuilderTest {

    private static final int TENANT_ID = -1234;
    private static final String TENANT_DOMAIN = "example.com";
    private static final String TEST_USER_ID = "22e46698-7fa7-4497-90fc-e12864e30b77";
    private static final String TEST_USER_EMAIL = "tom@gmail.com";
    private static final String FIRST_NAME = "Tom";
    private static final String LAST_NAME = "Hanks";
    private static final String DOMAIN_QUALIFIED_TEST_USER_NAME = "DEFAULT/tom";
    private static final String FAILURE_MESSAGE = "InvalidOperation Invalid operation. User store is read only";
    public static final String DEFAULT_USER_STORE = "DEFAULT";

    @Mock
    private EventData mockEventData;

    @Mock
    private RealmConfiguration realmConfiguration;

    @Mock
    private AbstractUserStoreManager userStoreManager;

    @InjectMocks
    private WSO2RegistrationEventPayloadBuilder payloadBuilder;

    @Mock
    private ClaimMetadataManagementService claimMetadataManagementService;

    private MockedStatic<FrameworkUtils> frameworkUtils;

    @BeforeClass
    public void setup() throws Exception {

        MockitoAnnotations.openMocks(this);
        WSO2EventHookHandlerDataHolder.getInstance().setClaimMetadataManagementService(claimMetadataManagementService);

        when(realmConfiguration.getUserStoreProperty(
                UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME)).thenReturn(DEFAULT_USER_STORE);
        when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfiguration);

        mockServiceURLBuilder();
        mockIdentityTenantUtil();

        frameworkUtils = mockStatic(FrameworkUtils.class);
        frameworkUtils.when(FrameworkUtils::getMultiAttributeSeparator).thenReturn(",");

        CommonTestUtils.initPrivilegedCarbonContext();
    }

    @AfterClass
    public void teardown() {

        closeMockedServiceURLBuilder();
        closeMockedIdentityTenantUtil();
        Mockito.reset(realmConfiguration, claimMetadataManagementService, userStoreManager);
        PrivilegedCarbonContext.endTenantFlow();
        frameworkUtils.close();
    }

    @Test
    public void testTestGetEventSchemaType() {

        assertEquals(payloadBuilder.getEventSchemaType(), EventSchema.WSO2);
    }

    @Test
    public void testBuildRegistrationSuccessEvent() throws IdentityEventException {

        Map<String, Object> properties = new HashMap<>();
        properties.put(IdentityEventConstants.EventProperty.TENANT_ID, TENANT_ID);
        properties.put(IdentityEventConstants.EventProperty.TENANT_DOMAIN, TENANT_DOMAIN);
        properties.put(USER_STORE_MANAGER, userStoreManager);
        properties.put(IdentityEventConstants.EventProperty.USER_NAME, DOMAIN_QUALIFIED_TEST_USER_NAME);

        Map<String, String> claims = new HashMap<>();
        claims.put(FrameworkConstants.EMAIL_ADDRESS_CLAIM, TEST_USER_EMAIL);
        claims.put(FrameworkConstants.USER_ID_CLAIM, TEST_USER_ID);
        claims.put(FIRST_NAME_CLAIM_URI, FIRST_NAME);
        claims.put(LAST_NAME_CLAIM_URI, LAST_NAME);

        properties.put(IdentityEventConstants.EventProperty.USER_CLAIMS, claims);

        when(mockEventData.getProperties()).thenReturn(properties);
        when(mockEventData.getTenantDomain()).thenReturn(TENANT_DOMAIN);

        IdentityContext.getThreadLocalIdentityContext().setFlow(new Flow.Builder()
                .name(Flow.Name.USER_REGISTRATION)
                .initiatingPersona(Flow.InitiatingPersona.ADMIN)
                .build());

        EventPayload eventPayload = payloadBuilder.buildRegistrationSuccessEvent(mockEventData);
        assertCommonFields((WSO2BaseEventPayload) eventPayload);

        WSO2RegistrationSuccessEventPayload userAccountEventPayload =
                (WSO2RegistrationSuccessEventPayload) eventPayload;
        // Assert the user account event payload
        assertNotNull(userAccountEventPayload.getUser());
        assertEquals(userAccountEventPayload.getUser().getId(), TEST_USER_ID);
        assertEquals(userAccountEventPayload.getUser().getRef(),
                constructFullURLWithEndpoint(SCIM2_USERS_ENDPOINT) + "/" + TEST_USER_ID);
        assertNotNull(userAccountEventPayload.getAction());
        assertEquals(userAccountEventPayload.getAction(),
                WSO2RegistrationEventPayloadBuilder.RegistrationAction.REGISTER.name());
        assertNotNull(userAccountEventPayload.getUser().getClaims());
        assertEquals(userAccountEventPayload.getUser().getClaims().size(), 3);

        List<UserClaim> userClaims = userAccountEventPayload.getUser().getClaims();
        Map<String, Object> userClaimsMap = userClaims.stream()
                .collect(java.util.stream.Collectors.toMap(UserClaim::getUri, UserClaim::getValue));

        assertNotNull(userClaimsMap.get(FrameworkConstants.EMAIL_ADDRESS_CLAIM));
        assertEquals(userClaimsMap.get(FrameworkConstants.EMAIL_ADDRESS_CLAIM), TEST_USER_EMAIL);

        assertNotNull(userClaimsMap.get(FIRST_NAME_CLAIM_URI));
        assertEquals(userClaimsMap.get(FIRST_NAME_CLAIM_URI), FIRST_NAME);

        assertNotNull(userClaimsMap.get(LAST_NAME_CLAIM_URI));
        assertEquals(userClaimsMap.get(LAST_NAME_CLAIM_URI), LAST_NAME);

        IdentityContext.destroyCurrentContext();
    }

    private static void assertCommonFields(WSO2BaseEventPayload wso2BaseEventPayload) {

        assertNotNull(wso2BaseEventPayload);

        assertNotNull(wso2BaseEventPayload.getInitiatorType());
        assertEquals(wso2BaseEventPayload.getInitiatorType(), Flow.InitiatingPersona.ADMIN.name());

        assertNotNull(wso2BaseEventPayload.getTenant());
        assertEquals(wso2BaseEventPayload.getTenant().getName(), TENANT_DOMAIN);

        assertNotNull(wso2BaseEventPayload.getUserStore());
        assertEquals(wso2BaseEventPayload.getUserStore().getId(), "REVGQVVMVA==");
        assertEquals(wso2BaseEventPayload.getUserStore().getName(), DEFAULT_USER_STORE);
    }

    @Test
    public void testGetEventSchemaType() {

        assertEquals(payloadBuilder.getEventSchemaType(), EventSchema.WSO2);
    }

    @Test
    public void testBuildRegistrationFailureEvent() throws IdentityEventException {

        Map<String, Object> properties = new HashMap<>();
        properties.put(IdentityEventConstants.EventProperty.TENANT_ID, TENANT_ID);
        properties.put(IdentityEventConstants.EventProperty.TENANT_DOMAIN, TENANT_DOMAIN);
        properties.put(IdentityEventConstants.EventProperty.ERROR_MESSAGE, FAILURE_MESSAGE);
        properties.put(IdentityEventConstants.EventProperty.USER_STORE_DOMAIN, DEFAULT_USER_STORE);
        properties.put(USER_STORE_MANAGER, userStoreManager);
        properties.put(IdentityEventConstants.EventProperty.USER_NAME, DOMAIN_QUALIFIED_TEST_USER_NAME);

        Map<String, String> claims = new HashMap<>();
        claims.put(FrameworkConstants.EMAIL_ADDRESS_CLAIM, TEST_USER_EMAIL);
        claims.put(FIRST_NAME_CLAIM_URI, FIRST_NAME);
        claims.put(LAST_NAME_CLAIM_URI, LAST_NAME);
        claims.put(LOCATION_CLAIM, constructFullURLWithEndpoint(SCIM2_USERS_ENDPOINT) + "/" + TEST_USER_ID);

        properties.put(IdentityEventConstants.EventProperty.USER_CLAIMS, claims);

        when(mockEventData.getProperties()).thenReturn(properties);

        IdentityContext.getThreadLocalIdentityContext().setFlow(new Flow.Builder()
                .name(Flow.Name.USER_REGISTRATION)
                .initiatingPersona(Flow.InitiatingPersona.ADMIN)
                .build());

        EventPayload eventPayload = payloadBuilder.buildRegistrationFailureEvent(mockEventData);
        assertCommonFields((WSO2BaseEventPayload) eventPayload);

        WSO2RegistrationFailureEventPayload userRegistrationFailurePayload =
                (WSO2RegistrationFailureEventPayload) eventPayload;
        // Assert the user account event payload
        assertNotNull(userRegistrationFailurePayload.getUser());
        assertEquals(userRegistrationFailurePayload.getUser().getId(), TEST_USER_ID);
        assertEquals(userRegistrationFailurePayload.getUser().getRef(),
                constructFullURLWithEndpoint(SCIM2_USERS_ENDPOINT) + "/" + TEST_USER_ID);
        assertNotNull(userRegistrationFailurePayload.getAction());
        assertEquals(userRegistrationFailurePayload.getAction(),
                WSO2RegistrationEventPayloadBuilder.RegistrationAction.REGISTER.name());

        assertNotNull(userRegistrationFailurePayload.getReason());
        assertNotNull(userRegistrationFailurePayload.getReason().getDescription());

        assertEquals(userRegistrationFailurePayload.getReason().getDescription(), FAILURE_MESSAGE);

        assertNotNull(userRegistrationFailurePayload.getUser().getClaims());
        assertEquals(userRegistrationFailurePayload.getUser().getClaims().size(), 3);

        List<UserClaim> userClaims = userRegistrationFailurePayload.getUser().getClaims();
        Map<String, Object> userClaimsMap = userClaims.stream()
                .collect(java.util.stream.Collectors.toMap(UserClaim::getUri, UserClaim::getValue));

        assertNotNull(userClaimsMap.get(FrameworkConstants.EMAIL_ADDRESS_CLAIM));
        assertEquals(userClaimsMap.get(FrameworkConstants.EMAIL_ADDRESS_CLAIM), TEST_USER_EMAIL);

        assertNotNull(userClaimsMap.get(FIRST_NAME_CLAIM_URI));
        assertEquals(userClaimsMap.get(FIRST_NAME_CLAIM_URI), FIRST_NAME);

        assertNotNull(userClaimsMap.get(LAST_NAME_CLAIM_URI));
        assertEquals(userClaimsMap.get(LAST_NAME_CLAIM_URI), LAST_NAME);

        IdentityContext.destroyCurrentContext();
    }
}
