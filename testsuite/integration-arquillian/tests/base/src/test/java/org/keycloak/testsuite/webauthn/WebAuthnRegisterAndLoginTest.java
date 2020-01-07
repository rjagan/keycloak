/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.webauthn;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnStrongRegisterFactory;
import org.keycloak.common.util.RandomString;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.webauthn.WebAuthnLoginPage;
import org.keycloak.testsuite.pages.webauthn.WebAuthnRegisterPage;
import org.keycloak.testsuite.WebAuthnAssume;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

public class WebAuthnRegisterAndLoginTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected WebAuthnLoginPage webAuthnLoginPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected WebAuthnRegisterPage webAuthnRegisterPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    private static final String ALL_ZERO_AAGUID = "00000000-0000-0000-0000-000000000000";

    private List<String> signatureAlgorithms;
    private String attestationConveyancePreference;
    private String authenticatorAttachment;
    private String requireResidentKey;
    private String rpEntityName;
    private String userVerificationRequirement;
    private String rpId;
    private int createTimeout;
    private boolean avoidSameAuthenticatorRegister;
    private List<String> acceptableAaguids;

    @Before
    public void verifyEnvironment() {
        WebAuthnAssume.assumeChrome(driver);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);
    }

    @Test
    public void registerUserSuccess() {
        String username = "registerUserSuccess";
        String password = "password";
        String email = "registerUserSuccess@email";

        try {
            RealmRepresentation rep = backupWebAuthnRealmSettings();
            rep.setWebAuthnPolicySignatureAlgorithms(Arrays.asList("ES256"));
            rep.setWebAuthnPolicyAttestationConveyancePreference("none");
            rep.setWebAuthnPolicyAuthenticatorAttachment("cross-platform");
            rep.setWebAuthnPolicyRequireResidentKey("No");
            rep.setWebAuthnPolicyRpId(null);
            rep.setWebAuthnPolicyUserVerificationRequirement("preferred");
            rep.setWebAuthnPolicyAcceptableAaguids(Arrays.asList(ALL_ZERO_AAGUID));
            testRealm().update(rep);

            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            String authenticatorLabel = RandomString.randomCode(24);
            registerPage.register("firstName", "lastName", email, username, password, password);

            // User was registered. Now he needs to register WebAuthn credential
            webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);

            appPage.assertCurrent();
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            appPage.openAccount();

            // confirm that registration is successfully completed
            String userId = events.expectRegister(username, email).assertEvent().getUserId();
            // confirm registration event
            EventRepresentation eventRep = events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
            .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                .assertEvent();
            String regPubKeyCredentialId = eventRep.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);
            //String regPubKeyCredentialAaguid = eventRep.getDetails().get("public_key_credential_aaguid");
            //String regPubKeyCredentialLabel = eventRep.getDetails().get("public_key_credential_label");

            // confirm login event
            String sessionId = events.expectLogin()
                .user(userId)
                .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, authenticatorLabel)
                .assertEvent().getSessionId();
            // confirm user registered
            assertUserRegistered(userId, username.toLowerCase(), email.toLowerCase());

            // logout by user
            appPage.logout();
            // confirm logout event
            events.expectLogout(sessionId)
                .user(userId)
                .assertEvent();

            // login by user
            loginPage.open();
            loginPage.login(username, password);

            // Confirm login on the WebAuthn login page
            webAuthnLoginPage.assertCurrent();
            webAuthnLoginPage.confirmWebAuthnLogin();

            appPage.assertCurrent();
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            appPage.openAccount();
            // confirm login event
            sessionId = events.expectLogin()
                .user(userId)
                .detail(WebAuthnConstants.PUBKEY_CRED_ID_ATTR, regPubKeyCredentialId)
//              .detail("web_authn_authenticator_user_verification_checked", Boolean.FALSE.toString())
                .assertEvent().getSessionId();

            // logout by user
            appPage.logout();
            // confirm logout event
            events.expectLogout(sessionId)
                .user(userId)
                .assertEvent();
        } finally {
            restoreWebAuthnRealmSettings();
        }
    }


    @Test
    public void testWebAuthnTwoFactorAndWebAuthnStrong() {

        // Change binding to browser-webauthn-strong. This is flow, which contains both "webauthn" and "webauthn-strong" authenticator
        RealmRepresentation realmRep = testRealm().toRepresentation();
        realmRep.setBrowserFlow("browser-webauthn-strong");
        testRealm().update(realmRep);

        //WaitUtils.pause(10000000);

        try {
            String userId = ApiUtil.findUserByUsername(testRealm(), "test-user@localhost").getId();

            // Login as test-user@localhost with password
            loginPage.open();
            loginPage.login("test-user@localhost", "password");

            // Register first requiredAction is needed. Use label "Label1"
            webAuthnRegisterPage.registerWebAuthnCredential("label1");

            // Register second requiredAction is needed. Use label "Label2". This will be for strong (passwordless) WebAuthn credential
            webAuthnRegisterPage.registerWebAuthnCredential("label2");

            appPage.assertCurrent();

            // Assert user is logged
            // confirm registration event
            EventRepresentation eventRep = events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                    .user(userId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnRegisterFactory.PROVIDER_ID)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, "label1")
                    .assertEvent();
            String regPubKeyCredentialId1 = eventRep.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

            eventRep = events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION)
                    .user(userId)
                    .detail(Details.CUSTOM_REQUIRED_ACTION, WebAuthnStrongRegisterFactory.PROVIDER_ID)
                    .detail(WebAuthnConstants.PUBKEY_CRED_LABEL_ATTR, "label2")
                    .assertEvent();
            String regPubKeyCredentialId2 = eventRep.getDetails().get(WebAuthnConstants.PUBKEY_CRED_ID_ATTR);

            String sessionId = events.expectLogin()
                    .user(userId)
                    .assertEvent().getSessionId();

            // Logout
            appPage.logout();
            events.expectLogout(sessionId)
                    .user(userId)
                    .assertEvent();

            // Assert user has 2 webauthn credentials. One of type "webauthn" and the other of type "webauthn-strong".
            List<CredentialRepresentation> rep = testRealm().users().get(userId).credentials();

            CredentialRepresentation webAuthnCredential1 = rep.stream()
                    .filter(credential -> WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(credential.getType()))
                    .findFirst().get();
            Assert.assertEquals("label1", webAuthnCredential1.getUserLabel());

            CredentialRepresentation webAuthnCredential2 = rep.stream()
                    .filter(credential -> WebAuthnCredentialModel.TYPE_STRONG.equals(credential.getType()))
                    .findFirst().get();
            Assert.assertEquals("label2", webAuthnCredential2.getUserLabel());

            // Assert user needs to authenticate first with "webauthn" during login
            loginPage.open();
            loginPage.login("test-user@localhost", "password");

            webAuthnLoginPage.assertCurrent();
            Assert.assertTrue(driver.getPageSource().contains(regPubKeyCredentialId1));
            webAuthnLoginPage.confirmWebAuthnLogin();

            // Assert user needs to authenticate also with "webauthn-strong"
            webAuthnLoginPage.assertCurrent();
            Assert.assertTrue(driver.getPageSource().contains(regPubKeyCredentialId2));
            webAuthnLoginPage.confirmWebAuthnLogin();

            // Assert user logged now
            appPage.assertCurrent();
            events.expectLogin()
                    .user(userId)
                    .assertEvent();

            // Remove webauthn credentials from the user
            testRealm().users().get(userId).removeCredential(webAuthnCredential1.getId());
            testRealm().users().get(userId).removeCredential(webAuthnCredential2.getId());
        } finally {
            // Revert binding to browser-webauthn
            realmRep.setBrowserFlow("browser-webauthn");
            testRealm().update(realmRep);
        }
    }

    private void assertUserRegistered(String userId, String username, String email) {
        UserRepresentation user = getUser(userId);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getCreatedTimestamp());
        // test that timestamp is current with 60s tollerance
        Assert.assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 60000);
        // test user info is set from form
        assertEquals(username.toLowerCase(), user.getUsername());
        assertEquals(email.toLowerCase(), user.getEmail());
        assertEquals("firstName", user.getFirstName());
        assertEquals("lastName", user.getLastName());
    }

    protected UserRepresentation getUser(String userId) {
        return testRealm().users().get(userId).toRepresentation();
    }

    private RealmRepresentation backupWebAuthnRealmSettings() {
        RealmRepresentation rep = testRealm().toRepresentation();
        signatureAlgorithms = rep.getWebAuthnPolicySignatureAlgorithms();
        attestationConveyancePreference = rep.getWebAuthnPolicyAttestationConveyancePreference();
        authenticatorAttachment = rep.getWebAuthnPolicyAuthenticatorAttachment();
        requireResidentKey = rep.getWebAuthnPolicyRequireResidentKey();
        rpEntityName = rep.getWebAuthnPolicyRpEntityName();
        userVerificationRequirement = rep.getWebAuthnPolicyUserVerificationRequirement();
        rpId = rep.getWebAuthnPolicyRpId();
        createTimeout = rep.getWebAuthnPolicyCreateTimeout();
        avoidSameAuthenticatorRegister = rep.isWebAuthnPolicyAvoidSameAuthenticatorRegister();
        acceptableAaguids = rep.getWebAuthnPolicyAcceptableAaguids();
        return rep;
    }

    public void restoreWebAuthnRealmSettings() {
        RealmRepresentation rep = testRealm().toRepresentation();
        rep.setWebAuthnPolicySignatureAlgorithms(signatureAlgorithms);
        rep.setWebAuthnPolicyAttestationConveyancePreference(attestationConveyancePreference);
        rep.setWebAuthnPolicyAuthenticatorAttachment(authenticatorAttachment);
        rep.setWebAuthnPolicyRequireResidentKey(requireResidentKey);
        rep.setWebAuthnPolicyRpEntityName(rpEntityName);
        rep.setWebAuthnPolicyUserVerificationRequirement(userVerificationRequirement);
        rep.setWebAuthnPolicyRpId(rpId);
        rep.setWebAuthnPolicyCreateTimeout(createTimeout);
        rep.setWebAuthnPolicyAvoidSameAuthenticatorRegister(avoidSameAuthenticatorRegister);
        rep.setWebAuthnPolicyAcceptableAaguids(acceptableAaguids);
        testRealm().update(rep);
    }

}
