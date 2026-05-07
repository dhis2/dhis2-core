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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link SignedJwtUserInfoLoader}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignedJwtUserInfoLoaderTest {

  @Mock private UserService userService;
  @Mock private RestTemplate restTemplate;
  @Mock private OidcUserRequest userRequest;
  @Mock private OAuth2AccessToken accessToken;
  @Mock private OidcIdToken idToken;
  @Mock private ClientRegistration clientRegistration;
  @Mock private ClientRegistration.ProviderDetails providerDetails;
  @Mock private ClientRegistration.ProviderDetails.UserInfoEndpoint userInfoEndpoint;

  /** Stub for {@link JwkSourceCache}: returns the correct public JWK source by default. */
  private JwkSourceCache jwkSourceCacheStub;

  private RSAKey rsaJwk;
  private DhisOidcClientRegistration registration;
  private SignedJwtUserInfoLoader loader;

  @BeforeEach
  void setUp() throws Exception {
    rsaJwk = new RSAKeyGenerator(2048).keyID("test-key").generate();
    JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(rsaJwk.toPublicJWK()));

    // Hand-rolled stub: avoids Byte Buddy inline-mock limitations on Java 21
    // for concrete Spring @Component classes.
    jwkSourceCacheStub =
        new JwkSourceCache() {
          @Override
          public JWKSource<SecurityContext> get(String registrationId, String jwkSetUri) {
            return source;
          }
        };

    when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
    when(userRequest.getAccessToken()).thenReturn(accessToken);
    when(userRequest.getIdToken()).thenReturn(idToken);
    when(accessToken.getTokenValue()).thenReturn("at-value");
    when(clientRegistration.getRegistrationId()).thenReturn("esignet");
    when(clientRegistration.getProviderDetails()).thenReturn(providerDetails);
    when(providerDetails.getUserInfoEndpoint()).thenReturn(userInfoEndpoint);
    when(userInfoEndpoint.getUri()).thenReturn("https://idp.test/userinfo");
    when(providerDetails.getJwkSetUri()).thenReturn("https://idp.test/jwks");

    registration =
        DhisOidcClientRegistration.builder()
            .clientRegistration(clientRegistration)
            .mappingClaimKey("sub")
            .userInfoResponseType(UserInfoResponseType.JWT)
            .userInfoJwsAlgorithm(JWSAlgorithm.RS256)
            .build();

    loader = new SignedJwtUserInfoLoader(userService, jwkSourceCacheStub, restTemplate);
  }

  @Test
  void happyPathReturnsDhisOidcUser() throws Exception {
    String jwt = signJwt(claims("user-123"));
    when(restTemplate.exchange(
            eq("https://idp.test/userinfo"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(ResponseEntity.ok(jwt));

    User user = new User();
    user.setExternalAuth(true);
    when(userService.getUserByOpenId("user-123")).thenReturn(user);
    when(userService.createUserDetails(user)).thenReturn(UserDetails.fromUser(user));

    OidcUser result = loader.load(userRequest, registration);

    assertNotNull(result);
    assertEquals("user-123", result.getAttributes().get("sub"));
  }

  @Test
  void httpFailureRaisesInvalidUserInfoResponse() {
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new RestClientException("boom"));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("invalid_user_info_response", ex.getError().getErrorCode());
  }

  @Test
  void badSignatureRaisesJwtProcessingError() throws Exception {
    RSAKey other = new RSAKeyGenerator(2048).keyID("other").generate();
    String jwt = signJwt(claims("user-123"), other);
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jwt));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("jwt_processing_error", ex.getError().getErrorCode());
  }

  @Test
  void missingMappingClaimRaisesError() throws Exception {
    JWTClaimsSet noSub = new JWTClaimsSet.Builder().issuer("idp").build();
    String jwt = signJwt(noSub);
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jwt));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("missing_mapping_claim", ex.getError().getErrorCode());
  }

  @Test
  void unknownUserRaisesError() throws Exception {
    String jwt = signJwt(claims("nobody"));
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jwt));
    when(userService.getUserByOpenId("nobody")).thenReturn(null);

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("user_not_found", ex.getError().getErrorCode());
  }

  @Test
  void disabledUserRaisesUserDisabled() throws Exception {
    String jwt = signJwt(claims("user-123"));
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jwt));
    User user = new User();
    user.setExternalAuth(true);
    user.setDisabled(true);
    when(userService.getUserByOpenId("user-123")).thenReturn(user);

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("user_disabled", ex.getError().getErrorCode());
  }

  // helpers

  private JWTClaimsSet claims(String sub) {
    return new JWTClaimsSet.Builder()
        .subject(sub)
        .issuer("idp")
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plusSeconds(60)))
        .build();
  }

  private String signJwt(JWTClaimsSet claims) throws JOSEException {
    return signJwt(claims, rsaJwk);
  }

  private String signJwt(JWTClaimsSet claims, RSAKey signingKey) throws JOSEException {
    SignedJWT signed =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
    signed.sign(new RSASSASigner(signingKey));
    return signed.serialize();
  }
}
