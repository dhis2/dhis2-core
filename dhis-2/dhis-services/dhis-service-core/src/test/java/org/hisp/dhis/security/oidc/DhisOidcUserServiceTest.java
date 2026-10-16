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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Unit tests for {@link DhisOidcUserService}: the userinfo response type of the provider selects
 * how the OIDC user is loaded, and the same DHIS2 user mapping applies to both response types.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class DhisOidcUserServiceTest {

  private static final String SUBJECT = "psut-4711";

  private static final String EMAIL = "user@dhis2.org";

  @Mock private UserService userService;

  @Mock private SignedJwtUserInfoLoader signedJwtUserInfoLoader;

  private DhisOidcProviderRepository providers;

  private DhisOidcUserService dhisOidcUserService;

  @BeforeEach
  void setUp() {
    providers = new DhisOidcProviderRepository();
    dhisOidcUserService = new DhisOidcUserService(userService, providers, signedJwtUserInfoLoader);
  }

  @Test
  void jsonProviderUserIsLoadedBySpringSecurity() {
    // without a UserInfo endpoint Spring Security builds the OIDC user from the ID token alone
    OidcUserRequest userRequest =
        userRequest(register(UserInfoResponseType.JSON), Map.of("email", EMAIL));
    User user = externalAuthUser();
    when(userService.getUserByOpenId(EMAIL)).thenReturn(user);
    when(userService.createUserDetails(user)).thenReturn(UserDetails.fromUser(user));

    OidcUser oidcUser = dhisOidcUserService.loadUser(userRequest);

    assertEquals("oidcuser", assertInstanceOf(DhisOidcUser.class, oidcUser).getUsername());
    verifyNoInteractions(signedJwtUserInfoLoader);
  }

  @Test
  void jwtProviderUserIsMappedFromSignedUserInfo() {
    DhisOidcClientRegistration registration = register(UserInfoResponseType.JWT);
    OidcUserRequest userRequest = userRequest(registration, Map.of());
    jwtUserInfoContains(userRequest, registration, Map.of("sub", SUBJECT, "email", EMAIL));
    User user = externalAuthUser();
    when(userService.getUserByOpenId(EMAIL)).thenReturn(user);
    when(userService.createUserDetails(user)).thenReturn(UserDetails.fromUser(user));

    OidcUser oidcUser = dhisOidcUserService.loadUser(userRequest);

    DhisOidcUser dhisOidcUser = assertInstanceOf(DhisOidcUser.class, oidcUser);
    assertEquals("oidcuser", dhisOidcUser.getUsername());
    assertEquals(SUBJECT, dhisOidcUser.getName());
    assertEquals(EMAIL, dhisOidcUser.getAttributes().get("email"));
  }

  @Test
  void jwtProviderUserWithoutMappingClaimIsRejected() {
    DhisOidcClientRegistration registration = register(UserInfoResponseType.JWT);
    OidcUserRequest userRequest = userRequest(registration, Map.of());
    jwtUserInfoContains(userRequest, registration, Map.of("sub", SUBJECT));

    assertLoginFails(userRequest, "could_not_map_oidc_user_to_dhis2_user");
    verify(userService, never()).getUserByOpenId(anyString());
  }

  @Test
  void userNotFlaggedForExternalAuthIsRejected() {
    DhisOidcClientRegistration registration = register(UserInfoResponseType.JWT);
    OidcUserRequest userRequest = userRequest(registration, Map.of());
    jwtUserInfoContains(userRequest, registration, Map.of("sub", SUBJECT, "email", EMAIL));
    User user = externalAuthUser();
    user.setExternalAuth(false);
    when(userService.getUserByOpenId(EMAIL)).thenReturn(user);

    assertLoginFails(userRequest, "could_not_map_oidc_user_to_dhis2_user");
  }

  @Test
  void disabledUserIsRejected() {
    DhisOidcClientRegistration registration = register(UserInfoResponseType.JWT);
    OidcUserRequest userRequest = userRequest(registration, Map.of());
    jwtUserInfoContains(userRequest, registration, Map.of("sub", SUBJECT, "email", EMAIL));
    User user = externalAuthUser();
    user.setDisabled(true);
    when(userService.getUserByOpenId(EMAIL)).thenReturn(user);

    assertLoginFails(userRequest, "user_disabled");
  }

  private void assertLoginFails(OidcUserRequest userRequest, String expectedErrorCode) {
    OAuth2AuthenticationException ex =
        assertThrows(
            OAuth2AuthenticationException.class, () -> dhisOidcUserService.loadUser(userRequest));
    assertEquals(expectedErrorCode, ex.getError().getErrorCode());
  }

  private void jwtUserInfoContains(
      OidcUserRequest userRequest,
      DhisOidcClientRegistration registration,
      Map<String, Object> userInfoClaims) {
    when(signedJwtUserInfoLoader.loadUser(userRequest, registration))
        .thenReturn(
            new DefaultOidcUser(
                List.of(),
                userRequest.getIdToken(),
                new OidcUserInfo(userInfoClaims),
                IdTokenClaimNames.SUB));
  }

  /** Registers a provider without UserInfo endpoint, mapping DHIS2 users by {@code email}. */
  private DhisOidcClientRegistration register(UserInfoResponseType userInfoResponseType) {
    DhisOidcClientRegistration registration =
        DhisOidcClientRegistration.builder()
            .clientRegistration(
                ClientRegistration.withRegistrationId("idp")
                    .clientId("dhis2-client")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/oauth2/code/{registrationId}")
                    .scope("openid", "email")
                    .authorizationUri("https://idp.test/authorize")
                    .tokenUri("https://idp.test/token")
                    .jwkSetUri("https://idp.test/jwks")
                    .userNameAttributeName(IdTokenClaimNames.SUB)
                    .build())
            .mappingClaimKey("email")
            .userInfoResponseType(userInfoResponseType)
            .build();
    providers.addRegistration(registration);
    return registration;
  }

  private static OidcUserRequest userRequest(
      DhisOidcClientRegistration registration, Map<String, Object> idTokenClaims) {
    Instant now = Instant.now();
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            now,
            now.plusSeconds(60),
            Set.of("openid", "email"));
    OidcIdToken idToken =
        OidcIdToken.withTokenValue("id-token")
            .issuer("https://idp.test")
            .subject(SUBJECT)
            .audience(List.of("dhis2-client"))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .claims(claims -> claims.putAll(idTokenClaims))
            .build();
    return new OidcUserRequest(registration.getClientRegistration(), accessToken, idToken);
  }

  private static User externalAuthUser() {
    User user = new User();
    user.setUsername("oidcuser");
    user.setExternalAuth(true);
    return user;
  }
}
