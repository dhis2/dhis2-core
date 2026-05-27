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
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Loads OIDC userinfo from providers that respond with a signed JWT ({@code application/jwt})
 * instead of plain JSON. Used when the registration's {@link UserInfoResponseType} is {@link
 * UserInfoResponseType#JWT} (e.g. MOSIP eSignet).
 *
 * <p>The flow is: GET userinfo with {@code Accept: application/jwt} → verify the signature against
 * the IdP's JWKS using the registered JWS algorithm → extract the configured mapping claim →
 * resolve the local DHIS2 user. The principal-name attribute remains {@link IdTokenClaimNames#SUB}
 * so audit logs keyed off {@code sub} match the JSON path.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
public class SignedJwtUserInfoLoader {

  private static final int CONNECT_TIMEOUT_MS = 5_000;
  private static final int READ_TIMEOUT_MS = 10_000;

  private final UserService userService;
  private final JwkSourceCache jwkSourceCache;
  private final RestTemplate restTemplate;

  @Autowired
  SignedJwtUserInfoLoader(UserService userService, JwkSourceCache jwkSourceCache) {
    this(userService, jwkSourceCache, createRestTemplate());
  }

  SignedJwtUserInfoLoader(
      UserService userService, JwkSourceCache jwkSourceCache, RestTemplate restTemplate) {
    this.userService = userService;
    this.jwkSourceCache = jwkSourceCache;
    this.restTemplate = restTemplate;
  }

  private static RestTemplate createRestTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
    factory.setReadTimeout(READ_TIMEOUT_MS);
    return new RestTemplate(factory);
  }

  /**
   * Fetches, verifies and maps a signed-JWT userinfo response to a {@link DhisOidcUser}.
   *
   * @param userRequest the OIDC user request produced after the code-for-token exchange
   * @param reg the DHIS2 client registration carrying JWT-specific metadata
   * @return a {@link DhisOidcUser} bound to the resolved DHIS2 user
   * @throws OAuth2AuthenticationException if the JWT cannot be fetched, verified or mapped to a
   *     valid DHIS2 user
   */
  public OidcUser load(OidcUserRequest userRequest, DhisOidcClientRegistration reg) {
    if (reg.getUserInfoJwsAlgorithm() == null) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("configuration_error"),
          "userInfoJwsAlgorithm is required when userInfoResponseType is JWT");
    }
    var providerDetails = userRequest.getClientRegistration().getProviderDetails();
    String userInfoUri = providerDetails.getUserInfoEndpoint().getUri();
    String idpJwkSetUri = providerDetails.getJwkSetUri();
    String jwt = fetchJwt(userRequest, userInfoUri);
    JWTClaimsSet claims =
        verify(jwt, reg, userRequest.getClientRegistration().getRegistrationId(), idpJwkSetUri);
    String mappingValue = requireMappingClaim(claims, reg);
    User user = resolveExternalAuthUser(userService, mappingValue, reg.getMappingClaimKey());
    UserDetails details = userService.createUserDetails(user);
    return new DhisOidcUser(
        details, claims.toJSONObject(), IdTokenClaimNames.SUB, userRequest.getIdToken());
  }

  private String fetchJwt(OidcUserRequest userRequest, String userInfoUri) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(userRequest.getAccessToken().getTokenValue());
    headers.setAccept(List.of(MediaType.valueOf("application/jwt")));
    HttpEntity<String> entity = new HttpEntity<>("", headers);
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(userInfoUri, HttpMethod.GET, entity, String.class);
      String body = response.getBody();
      if (body == null || body.isBlank()) {
        throw new OAuth2AuthenticationException(
            new OAuth2Error("invalid_user_info_response"), "Empty UserInfo JWT response");
      }
      return body;
    } catch (RestClientException ex) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("invalid_user_info_response"),
          "Failed to fetch UserInfo response: " + ex.getMessage(),
          ex);
    }
  }

  private JWTClaimsSet verify(
      String jwt, DhisOidcClientRegistration reg, String registrationId, String idpJwkSetUri) {
    try {
      ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
      JWKSource<SecurityContext> keySource = jwkSourceCache.get(registrationId, idpJwkSetUri);
      JWSKeySelector<SecurityContext> selector =
          new JWSVerificationKeySelector<>(reg.getUserInfoJwsAlgorithm(), keySource);
      processor.setJWSKeySelector(selector);
      return processor.process(jwt, null);
    } catch (BadJOSEException | JOSEException | java.text.ParseException ex) {
      log.debug("UserInfo JWT verification failed for registration {}", registrationId, ex);
      throw new OAuth2AuthenticationException(
          new OAuth2Error("jwt_processing_error"),
          "Failed to verify UserInfo JWT: " + ex.getMessage(),
          ex);
    }
  }

  private String requireMappingClaim(JWTClaimsSet claims, DhisOidcClientRegistration reg) {
    String mappingClaimKey = reg.getMappingClaimKey();
    Map<String, Object> claimsMap = claims.toJSONObject();
    Object value = claimsMap.get(mappingClaimKey);
    if (!(value instanceof String s) || s.isBlank()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("missing_mapping_claim"),
          "Mapping claim '" + mappingClaimKey + "' missing or empty in UserInfo JWT");
    }
    return s;
  }

  static User resolveExternalAuthUser(
      UserService userService, String mappingValue, String mappingClaimKey) {
    User user = userService.getUserByOpenId(mappingValue);
    if (user == null || !user.isExternalAuth()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("user_not_found"),
          "No external-auth DHIS2 user found for mapping claim '"
              + mappingClaimKey
              + "'='"
              + mappingValue
              + "'");
    }
    if (user.isDisabled() || !user.isAccountNonExpired()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("user_disabled"), "DHIS2 user is disabled or expired");
    }
    return user;
  }
}
