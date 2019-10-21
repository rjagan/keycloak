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
 *
 */

package org.keycloak.models.credential.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAuthnCredentialData {

    private final String aaguid;
    private final String credentialId;
    private long counter;

    @JsonCreator
    public WebAuthnCredentialData(@JsonProperty("aaguid") String aaguid,
                                  @JsonProperty("credentialId") String credentialId,
                                  @JsonProperty("counter") long counter) {
        this.aaguid = aaguid;
        this.credentialId = credentialId;
        this.counter = counter;
    }

    public String getAaguid() {
        return aaguid;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    @Override
    public String toString() {
        return "WebAuthnCredentialData { " +
                "aaguid='" + aaguid + '\'' +
                ", credentialId='" + credentialId + '\'' +
                ", counter=" + counter +
                " }";
    }
}
