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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for {@link SignedJwtUserInfoLoader}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignedJwtUserInfoLoaderTest {

  private static final String USER_INFO_URI = "https://idp.test/userinfo";

  private static final MediaType APPLICATION_JWT = new MediaType("application", "jwt");

  @Mock private UserService userService;
  @Mock private JwkSourceCache jwkSourceCache;
  @Mock private OidcUserRequest userRequest;
  @Mock private OAuth2AccessToken accessToken;
  @Mock private OidcIdToken idToken;
  @Mock private ClientRegistration clientRegistration;
  @Mock private ClientRegistration.ProviderDetails providerDetails;
  @Mock private ClientRegistration.ProviderDetails.UserInfoEndpoint userInfoEndpoint;

  private RSAKey rsaJwk;
  private DhisOidcClientRegistration registration;
  private MockRestServiceServer idp;
  private SignedJwtUserInfoLoader loader;

  @BeforeEach
  void setUp() throws Exception {
    rsaJwk = new RSAKeyGenerator(2048).keyID("test-key").generate();
    when(jwkSourceCache.get("esignet", "https://idp.test/jwks"))
        .thenReturn(new ImmutableJWKSet<>(new JWKSet(rsaJwk.toPublicJWK())));

    when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
    when(userRequest.getAccessToken()).thenReturn(accessToken);
    when(userRequest.getIdToken()).thenReturn(idToken);
    when(accessToken.getTokenValue()).thenReturn("at-value");
    when(clientRegistration.getRegistrationId()).thenReturn("esignet");
    when(clientRegistration.getProviderDetails()).thenReturn(providerDetails);
    when(providerDetails.getUserInfoEndpoint()).thenReturn(userInfoEndpoint);
    when(userInfoEndpoint.getUri()).thenReturn(USER_INFO_URI);
    when(providerDetails.getJwkSetUri()).thenReturn("https://idp.test/jwks");

    registration =
        DhisOidcClientRegistration.builder()
            .clientRegistration(clientRegistration)
            .mappingClaimKey("sub")
            .userInfoResponseType(UserInfoResponseType.JWT)
            .userInfoJwsAlgorithm(JWSAlgorithm.RS256)
            .build();

    RestClient.Builder restClientBuilder = RestClient.builder();
    idp = MockRestServiceServer.bindTo(restClientBuilder).build();
    loader = new SignedJwtUserInfoLoader(userService, jwkSourceCache, restClientBuilder.build());
  }

  @Test
  void happyPathReturnsDhisOidcUser() throws Exception {
    idp.expect(requestTo(USER_INFO_URI))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer at-value"))
        .andExpect(header(HttpHeaders.ACCEPT, APPLICATION_JWT.toString()))
        .andRespond(withSuccess(signJwt(claims("user-123")), APPLICATION_JWT));

    User user = new User();
    user.setExternalAuth(true);
    when(userService.getUserByOpenId("user-123")).thenReturn(user);
    when(userService.createUserDetails(user)).thenReturn(UserDetails.fromUser(user));

    OidcUser result = loader.load(userRequest, registration);

    idp.verify();
    assertNotNull(result);
    assertEquals("user-123", result.getAttributes().get("sub"));
  }

  @Test
  void httpFailureRaisesInvalidUserInfoResponse() {
    idp.expect(requestTo(USER_INFO_URI)).andRespond(withServerError());

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("invalid_user_info_response", ex.getError().getErrorCode());
  }

  @Test
  void badSignatureRaisesJwtProcessingError() throws Exception {
    RSAKey other = new RSAKeyGenerator(2048).keyID("other").generate();
    respondWith(signJwt(claims("user-123"), other));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("jwt_processing_error", ex.getError().getErrorCode());
  }

  @Test
  void missingMappingClaimRaisesError() throws Exception {
    respondWith(signJwt(new JWTClaimsSet.Builder().issuer("idp").build()));

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("missing_mapping_claim", ex.getError().getErrorCode());
  }

  @Test
  void unknownUserRaisesError() throws Exception {
    respondWith(signJwt(claims("nobody")));
    when(userService.getUserByOpenId("nobody")).thenReturn(null);

    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> loader.load(userRequest, registration));
    assertEquals("user_not_found", ex.getError().getErrorCode());
  }

  @Test
  void disabledUserRaisesUserDisabled() throws Exception {
    respondWith(signJwt(claims("user-123")));
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

  private void respondWith(String jwt) {
    idp.expect(requestTo(USER_INFO_URI)).andRespond(withSuccess(jwt, APPLICATION_JWT));
  }

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
