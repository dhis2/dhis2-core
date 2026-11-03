/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.security.config;

import static org.hisp.dhis.security.oauth2.OAuth2Constants.IAT_REDIRECT_URL_CLAIM;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.oidc.OidcClientRegistration;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcClientRegistrationAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcClientRegistrationAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcClientRegistrationAuthenticationValidator;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Registration policy for OAuth2 Dynamic Client Registration ({@code POST /connect/register}),
 * applied after Spring Authorization Server's default {@link
 * OidcClientRegistrationAuthenticationValidator}.
 *
 * <p>Initial Access Tokens (IATs) are issued by {@code GET /api/auth/enrollDevice} for device
 * enrollment, so a registration must match the client the DHIS2 Android Capture app registers: an
 * {@code authorization_code} client, optionally with {@code refresh_token}, that authenticates with
 * {@code private_key_jwt} and only uses the redirect URI the IAT was issued for. Other
 * registrations are rejected with an RFC 7591 error before the client is converted or persisted.
 * The IAT subject owns the new client, so it must be an active user.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
final class DcrRegistrationPolicyValidator
    implements Consumer<OidcClientRegistrationAuthenticationContext> {

  private static final String ERROR_URI =
      "https://openid.net/specs/openid-connect-registration-1_0.html#RegistrationError";

  private static final String INVALID_CLIENT_METADATA = "invalid_client_metadata";

  private static final Set<String> ALLOWED_GRANT_TYPES =
      Set.of(
          AuthorizationGrantType.AUTHORIZATION_CODE.getValue(),
          AuthorizationGrantType.REFRESH_TOKEN.getValue());

  private final UserService userService;

  DcrRegistrationPolicyValidator(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void accept(OidcClientRegistrationAuthenticationContext context) {
    OidcClientRegistrationAuthenticationToken authentication = context.getAuthentication();
    Jwt initialAccessToken = getInitialAccessToken(authentication);
    checkSubjectIsActiveUser(initialAccessToken);

    OidcClientRegistration registration = authentication.getClientRegistration();
    checkRedirectUris(registration, initialAccessToken);
    checkGrantTypes(registration);
    checkResponseTypes(registration);
    checkTokenEndpointAuthMethod(registration);
    checkNoPostLogoutRedirectUris(registration);
  }

  private static Jwt getInitialAccessToken(
      OidcClientRegistrationAuthenticationToken authentication) {
    if (authentication.getPrincipal() instanceof AbstractOAuth2TokenAuthenticationToken<?> bearer
        && bearer.getToken() instanceof Jwt jwt) {
      return jwt;
    }
    throw error(OAuth2ErrorCodes.INVALID_TOKEN, "The initial access token must be a JWT");
  }

  /** Same account checks as a login: enabled, not locked out, account and password not expired. */
  private void checkSubjectIsActiveUser(Jwt initialAccessToken) {
    String username = initialAccessToken.getSubject();
    UserDetails user = username == null ? null : userService.createUserDetailsByUsername(username);
    if (user == null
        || !user.isEnabled()
        || !user.isAccountNonLocked()
        || !user.isAccountNonExpired()
        || !user.isCredentialsNonExpired()) {
      throw error(
          OAuth2ErrorCodes.INVALID_TOKEN, "The initial access token subject is not an active user");
    }
  }

  /**
   * The only allowed redirect URI is the one the IAT was issued for: {@code /api/auth/enrollDevice}
   * checked it against the {@code deviceEnrollmentRedirectAllowlist} and sent the IAT there.
   */
  private static void checkRedirectUris(
      OidcClientRegistration registration, Jwt initialAccessToken) {
    String issuedFor = initialAccessToken.getClaimAsString(IAT_REDIRECT_URL_CLAIM);
    if (!StringUtils.hasText(issuedFor)
        || !List.of(issuedFor).equals(registration.getRedirectUris())) {
      throw error(
          OAuth2ErrorCodes.INVALID_REDIRECT_URI,
          "redirect_uris must be exactly the redirect URI the initial access token was issued for");
    }
  }

  /** Spring AS registers {@code authorization_code} when {@code grant_types} is omitted. */
  private static void checkGrantTypes(OidcClientRegistration registration) {
    List<String> grantTypes = registration.getGrantTypes();
    if (CollectionUtils.isEmpty(grantTypes)) {
      return;
    }
    if (!grantTypes.contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
        || !ALLOWED_GRANT_TYPES.containsAll(grantTypes)) {
      throw error(
          INVALID_CLIENT_METADATA,
          "grant_types must be authorization_code, optionally with refresh_token");
    }
  }

  private static void checkResponseTypes(OidcClientRegistration registration) {
    List<String> responseTypes = registration.getResponseTypes();
    if (!CollectionUtils.isEmpty(responseTypes)
        && !List.of(OAuth2AuthorizationResponseType.CODE.getValue()).equals(responseTypes)) {
      throw error(INVALID_CLIENT_METADATA, "response_types must be code");
    }
  }

  /**
   * Spring AS issues a client secret for every other method, including an omitted one, which falls
   * back to {@code client_secret_basic}.
   */
  private static void checkTokenEndpointAuthMethod(OidcClientRegistration registration) {
    if (!ClientAuthenticationMethod.PRIVATE_KEY_JWT
        .getValue()
        .equals(registration.getTokenEndpointAuthenticationMethod())) {
      throw error(INVALID_CLIENT_METADATA, "token_endpoint_auth_method must be private_key_jwt");
    }
  }

  private static void checkNoPostLogoutRedirectUris(OidcClientRegistration registration) {
    if (!CollectionUtils.isEmpty(registration.getPostLogoutRedirectUris())) {
      throw error(INVALID_CLIENT_METADATA, "post_logout_redirect_uris are not supported");
    }
  }

  private static OAuth2AuthenticationException error(String errorCode, String description) {
    return new OAuth2AuthenticationException(new OAuth2Error(errorCode, description, ERROR_URI));
  }
}
