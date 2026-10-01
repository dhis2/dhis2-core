/*
 * Copyright (c) 2004-2026, University of Oslo
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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.URI;
import java.net.http.HttpClient;
import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Loads the OIDC user for providers whose UserInfo endpoint responds with a signed JWT ({@code
 * application/jwt}) instead of JSON, for example MOSIP eSignet. Spring Security's {@link
 * OidcUserService} only parses JSON, so {@link DhisOidcUserService} delegates here when the
 * registration's {@link UserInfoResponseType} is {@link UserInfoResponseType#JWT}.
 *
 * <p>The UserInfo JWT is fetched with {@code Accept: application/jwt} and its signature is verified
 * against the IdP's JWKS ({@code jwk_uri}) with the registered JWS algorithm. The verified claims
 * are combined with the ID token into an {@link OidcUser}, as {@link OidcUserService} does for a
 * JSON response, so the DHIS2 user mapping in {@link DhisOidcUserService} is the same for both.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
public class SignedJwtUserInfoLoader {

  /** Connect timeout for the IdP UserInfo request, same as the preceding token exchange. */
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

  /** Read timeout for the IdP UserInfo request, same as the preceding token exchange. */
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  private static final MediaType APPLICATION_JWT = new MediaType("application", "jwt");

  private final JwkSourceCache jwkSourceCache;
  private final RestClient restClient;

  @Autowired
  SignedJwtUserInfoLoader(JwkSourceCache jwkSourceCache) {
    this(jwkSourceCache, buildRestClient());
  }

  SignedJwtUserInfoLoader(JwkSourceCache jwkSourceCache, RestClient restClient) {
    this.jwkSourceCache = jwkSourceCache;
    this.restClient = restClient;
  }

  /**
   * Builds the {@link RestClient} for the UserInfo request the same way {@link
   * DhisAuthorizationCodeTokenResponseClient} builds the one for the token exchange: pinned to the
   * JDK {@link HttpClient} with explicit connect and read timeouts, so a hung IdP UserInfo endpoint
   * cannot block a login request thread indefinitely.
   */
  private static RestClient buildRestClient() {
    JdkClientHttpRequestFactory requestFactory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
    requestFactory.setReadTimeout(READ_TIMEOUT);
    return RestClient.builder().requestFactory(requestFactory).build();
  }

  /**
   * Fetches and verifies the signed UserInfo JWT and combines its claims with the ID token.
   *
   * @param userRequest the OIDC user request produced after the code-for-token exchange
   * @param registration the DHIS2 registration of the provider
   * @return the OIDC user holding the ID token and the verified UserInfo claims
   * @throws OAuth2AuthenticationException if the UserInfo JWT cannot be fetched or verified
   */
  public OidcUser loadUser(OidcUserRequest userRequest, DhisOidcClientRegistration registration) {
    JWSAlgorithm jwsAlgorithm = registration.getUserInfoJwsAlgorithm();
    if (jwsAlgorithm == null) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("configuration_error"),
          "userInfoJwsAlgorithm is required when userInfoResponseType is JWT");
    }
    ClientRegistration clientRegistration = userRequest.getClientRegistration();
    ProviderDetails providerDetails = clientRegistration.getProviderDetails();
    String jwt =
        fetchJwt(providerDetails.getUserInfoEndpoint().getUri(), userRequest.getAccessToken());
    JWTClaimsSet claims =
        verify(
            jwt,
            jwsAlgorithm,
            clientRegistration.getRegistrationId(),
            providerDetails.getJwkSetUri());
    return new DefaultOidcUser(
        List.of(),
        userRequest.getIdToken(),
        new OidcUserInfo(claims.toJSONObject()),
        IdTokenClaimNames.SUB);
  }

  private String fetchJwt(String userInfoUri, OAuth2AccessToken accessToken) {
    String body;
    try {
      body =
          restClient
              .get()
              .uri(URI.create(userInfoUri))
              .headers(headers -> headers.setBearerAuth(accessToken.getTokenValue()))
              .accept(APPLICATION_JWT)
              .retrieve()
              .body(String.class);
    } catch (RestClientException ex) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("invalid_user_info_response"),
          "Failed to fetch UserInfo response: " + ex.getMessage(),
          ex);
    }
    if (body == null || body.isBlank()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("invalid_user_info_response"), "Empty UserInfo JWT response");
    }
    return body;
  }

  private JWTClaimsSet verify(
      String jwt, JWSAlgorithm jwsAlgorithm, String registrationId, String idpJwkSetUri) {
    JWKSource<SecurityContext> keySource = jwkSourceCache.get(registrationId, idpJwkSetUri);
    ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(jwsAlgorithm, keySource));
    try {
      return processor.process(jwt, null);
    } catch (BadJOSEException | JOSEException | ParseException ex) {
      log.debug("UserInfo JWT verification failed for registration {}", registrationId, ex);
      throw new OAuth2AuthenticationException(
          new OAuth2Error("jwt_processing_error"),
          "Failed to verify UserInfo JWT: " + ex.getMessage(),
          ex);
    }
  }
}
