/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.forms.login.LoginFormsProvider;
<<<<<<< HEAD
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
=======
>>>>>>> db8e53edc5... multi-factor cherry-pick2

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class PasswordForm extends UsernamePasswordForm {

    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        return validatePassword(context, context.getUser(), formData, false);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
<<<<<<< HEAD
        Response challengeResponse = context.form().createLoginPassword();
=======
        Response challengeResponse = context.form().setAuthContext(context).createLoginPassword();
>>>>>>> db8e53edc5... multi-factor cherry-pick2
        context.challenge(challengeResponse);
    }

    @Override
<<<<<<< HEAD
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // never called
        return getCredentialProvider(session).isConfiguredFor(realm, user, getType(session));
    }

    @Override
=======
>>>>>>> db8e53edc5... multi-factor cherry-pick2
    public boolean requiresUser() {
        return true;
    }

    @Override
    protected Response createLoginForm(LoginFormsProvider form) {
        return form.createLoginPassword();
    }
}
