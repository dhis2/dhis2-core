/*
 * Copyright (c) 2004-2023, University of Oslo
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

import com.nimbusds.jose.jwk.JWK;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Service;

/**
 * Spring {@link OAuth2AccessTokenResponseClient} used by the DHIS2 Relying Party during the
 * authorization-code token exchange against an external OIDC Identity Provider.
 *
 * <p>This client supports both client authentication modes: standard {@code client_secret_*}
 * (basic, post, JWT) and {@code private_key_jwt}. When the matched {@link ClientRegistration}
 * declares {@link ClientAuthenticationMethod#PRIVATE_KEY_JWT}, the {@link
 * NimbusJwtClientAuthenticationParametersConverter} adds a JWT client assertion signed with the
 * per-provider key loaded from {@link DhisOidcClientRegistration} ({@code jwk}, {@code jwkSetUrl});
 * for all other methods the converter contributes nothing and the delegate falls back to the
 * standard client-credentials parameters.
 *
 * <p>Since Spring Security 7.0 removed the {@code RequestEntity}-converter /{@code RestOperations}
 * token-client infrastructure, the HTTP exchange is delegated to {@link
 * RestClientAuthorizationCodeTokenResponseClient}, which provides the default {@code RestClient},
 * message converters and OAuth2 error handling.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Service
@RequiredArgsConstructor
public class DhisAuthorizationCodeTokenResponseClient
    implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {
  private final DhisOidcProviderRepository clientRegistrations;

  private RestClientAuthorizationCodeTokenResponseClient delegate;

  /**
   * Builds the delegate token-response client and registers the {@code private_key_jwt} parameters
   * converter (which self-gates on the client registration's authentication method).
   */
  @PostConstruct
  public void init() {
    Function<ClientRegistration, JWK> jwkResolver =
        clientRegistration -> {
          if (clientRegistration
              .getClientAuthenticationMethod()
              .equals(ClientAuthenticationMethod.PRIVATE_KEY_JWT)) {
            DhisOidcClientRegistration clientReg =
                clientRegistrations.getDhisOidcClientRegistration(
                    clientRegistration.getRegistrationId());
            return clientReg.getJwk();
          }
          return null;
        };

    Consumer<
            NimbusJwtClientAuthenticationParametersConverter.JwtClientAuthenticationContext<
                OAuth2AuthorizationCodeGrantRequest>>
        jwtClientAssertionCustomizer =
            context -> {
              ClientRegistration clientRegistration =
                  context.getAuthorizationGrantRequest().getClientRegistration();
              DhisOidcClientRegistration dhisOidcClientRegistration =
                  clientRegistrations.getDhisOidcClientRegistration(
                      clientRegistration.getRegistrationId());
              context.getHeaders().jwkSetUrl(dhisOidcClientRegistration.getJwkSetUrl());
            };

    NimbusJwtClientAuthenticationParametersConverter<OAuth2AuthorizationCodeGrantRequest>
        parametersConverter = new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver);
    parametersConverter.setJwtClientAssertionCustomizer(jwtClientAssertionCustomizer);

    RestClientAuthorizationCodeTokenResponseClient tokenResponseClient =
        new RestClientAuthorizationCodeTokenResponseClient();
    tokenResponseClient.addParametersConverter(parametersConverter);
    this.delegate = tokenResponseClient;
  }

  /**
   * Exchanges an authorization code for an access token (and an ID token) at the IdP's token
   * endpoint.
   *
   * @param authorizationCodeGrantRequest the authorization-code grant request
   * @return the parsed token response from the IdP
   */
  @Override
  public OAuth2AccessTokenResponse getTokenResponse(
      @Nonnull OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
    return delegate.getTokenResponse(authorizationCodeGrantRequest);
  }
}
