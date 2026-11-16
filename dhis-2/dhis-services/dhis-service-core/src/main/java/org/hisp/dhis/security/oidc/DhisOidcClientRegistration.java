/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * DHIS2 wrapper around Spring Security's {@link ClientRegistration} that bundles the Spring
 * registration with DHIS2-specific metadata needed to operate as an OpenID Connect Relying Party
 * (RP) against an external OIDC Identity Provider (IdP).
 *
 * <p>Each registered instance corresponds to one OIDC provider configured in {@code dhis.conf}
 * (e.g. {@code google}, {@code azure.0}, {@code wso2}, or any generic provider under {@code
 * oidc.provider.<id>.*}), and becomes a login button on the DHIS2 web login page when {@code
 * oidc.oauth2.login.enabled=on}. The auto-registered {@code dhis2-internal} provider is the
 * exception: it is used by the Android Capture app's authorization-code flow against DHIS2 as
 * authorization server and is not rendered on the web login page.
 *
 * <p>Instances are produced by the provider builders (e.g. {@code GoogleProvider}, {@code
 * AzureAdProvider}, {@code Wso2Provider}, {@code GenericOidcProviderBuilder}, {@code
 * Dhis2InternalOidcProvider}) and registered in the {@link DhisOidcProviderRepository}, which
 * exposes them to Spring's {@code oauth2Login} filter chain.
 *
 * <p>Fields:
 *
 * <ul>
 *   <li>{@code clientRegistration}: the underlying Spring {@link ClientRegistration} describing
 *       client id/secret, authorization/token/userinfo/JWK endpoints, scopes, and client
 *       authentication method.
 *   <li>{@code mappingClaimKey}: the ID-token/userinfo claim DHIS2 uses to look up the local user
 *       (default {@code email} for external providers; {@code username} for the internal DHIS2
 *       provider).
 *   <li>{@code loginIcon}, {@code loginIconPadding}, {@code loginText}: rendering hints for the
 *       login button on the DHIS2 web login page.
 *   <li>{@code jwk}, {@code rsaPublicKey}, {@code keyId}, {@code jwkSetUrl}: per-provider signing
 *       material used when DHIS2 authenticates to the IdP's token endpoint with a {@code
 *       private_key_jwt} assertion. Loaded from {@code oidc.provider.<id>.keystore_path}, {@code
 *       keystore_password}, {@code key_alias}, {@code key_password}. Distinct from the
 *       authorization-server signing keystore ({@code oauth2.server.jwt.keystore.*}).
 *   <li>{@code visibleOnLoginPage}: whether this registration renders a button on the web login
 *       page; {@code false} for the internal DHIS2 provider.
 *   <li>{@code externalClients}: client ids and secrets for additional clients authorized to
 *       present tokens issued by the same IdP (used when validating inbound bearer tokens).
 * </ul>
 *
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

  /**
   * Selects how DHIS2 consumes this provider's userinfo response. Defaults to {@link
   * UserInfoResponseType#JSON}, preserving the historical Spring Security behaviour for every
   * existing provider.
   */
  @Builder.Default
  private final UserInfoResponseType userInfoResponseType = UserInfoResponseType.JSON;

  /**
   * JWS algorithm used to verify the signed userinfo JWT. Only consulted when {@link
   * #userInfoResponseType} is {@link UserInfoResponseType#JWT}.
   */
  @CheckForNull private final JWSAlgorithm userInfoJwsAlgorithm;

  @Builder.Default private final boolean visibleOnLoginPage = true;

  @Builder.Default private final Map<String, Map<String, String>> externalClients = new HashMap<>();

  /**
   * Returns the set of client ids associated with this registration: the primary client id from the
   * underlying {@link ClientRegistration} plus every client id declared for the configured external
   * clients.
   *
   * @return unmodifiable set of client ids accepted for this provider
   */
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
