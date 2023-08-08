/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.security.oidc;

import static org.hisp.dhis.security.oidc.provider.AbstractOidcProvider.CLIENT_ID;

import com.nimbusds.jose.jwk.JWK;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Data
@Builder
public class DhisOidcClientRegistration {
  private final ClientRegistration clientRegistration;

  private final String mappingClaimKey;

  private final String loginIcon;

  private final String loginIconPadding;

  private final String loginText;

  private final JWK jwk;

  private final RSAPublicKey rsaPublicKey;

  private final String keyId;

  private final String jwkSetUrl;

  @Builder.Default private final Map<String, Map<String, String>> externalClients = new HashMap<>();

  public Collection<String> getClientIds() {
    Set<String> allExternalClientIds =
        externalClients.entrySet().stream()
            .flatMap(e -> e.getValue().entrySet().stream())
            .filter(e -> e.getKey().contains(CLIENT_ID))
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());

    allExternalClientIds.add(clientRegistration.getClientId());
    return Collections.unmodifiableSet(allExternalClientIds);
  }
}
