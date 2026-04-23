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
package org.hisp.dhis.webapi.controller.security.oauth;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.condition.AuthorizationServerEnabledCondition;
import org.hisp.dhis.security.oauth2.dcr.OAuth2DcrService;
import org.hisp.dhis.security.oauth2.dcr.OAuth2DcrService.IatPair;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST controller for the DHIS2 device enrollment flow backing OAuth2 Dynamic Client Registration
 * (DCR) as defined by RFC 7591. A new device client is enrolled in two steps: the user first hits
 * {@code GET /api/auth/enrollDevice} to be redirected back to the device with a freshly minted
 * Initial Access Token (IAT); the device then presents that IAT as a Bearer credential to the
 * authorization server's {@code /connect/register} endpoint (provided by Spring Authorization
 * Server) which persists a new {@link org.hisp.dhis.security.oauth2.client.Dhis2OAuth2Client} row
 * configured with {@link
 * org.springframework.security.oauth2.core.ClientAuthenticationMethod#PRIVATE_KEY_JWT} and returns
 * the standard RFC 7591 registration response.
 *
 * <p>The DCR endpoint is implicitly enabled whenever {@code oauth2.server.enabled=on}, gated here
 * by {@link AuthorizationServerEnabledCondition}. The registration payload typically ships an
 * inline {@code jwks} (rather than a remote {@code jwks_uri}), because the main DCR client is the
 * DHIS2 Android Capture app, whose per-device RSA keypair is generated in the Android Keystore and
 * cannot be hosted at a public URL; the inline form is decoded at token-endpoint time by {@link
 * org.hisp.dhis.webapi.security.config.InlineJwksJwtClientAssertionDecoderFactory}. IATs are
 * single-use: once {@code /connect/register} consumes the underlying authorization, the IAT cannot
 * be replayed.
 *
 * <p>The primary client of this flow is the DHIS2 Android Capture app; the {@code /enrollDevice}
 * step below prepares the redirect that seeds it with an IAT.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/auth")
@Conditional(AuthorizationServerEnabledCondition.class)
public class OAuth2DynamicClientRegistrationController {

  @Autowired private SystemSettingsService systemSettingsService;
  @Autowired private OAuth2DcrService oAuth2DcrService;

  /**
   * Mints an Initial Access Token (IAT) via {@link OAuth2DcrService#createIat(String)} and
   * redirects the user-agent back to the caller-provided {@code redirectUri} with the IAT (and the
   * opaque {@code state}) attached as query parameters. The redirect shape mirrors the OAuth2
   * {@code /authorize} endpoint so that a device client can pick the IAT up the same way it would
   * pick up an authorization code.
   *
   * <p>Gated by two system-settings allowlists: the caller must belong to one of the user groups in
   * {@code deviceEnrollmentAllowedUserGroups} (HTTP 403 {@code forbidden_user} otherwise), and the
   * {@code redirectUri} must match an entry in {@code deviceEnrollmentRedirectAllowlist} (HTTP 400
   * {@code invalid_redirect_uri} otherwise). The response is marked non-cacheable before the
   * redirect is issued.
   *
   * @param redirectUri the redirect URI to send the IAT to; must match the redirect allowlist
   * @param state an opaque value that is echoed back to the client on the redirect
   * @param response the HTTP response used to emit either the error or the redirect
   * @throws IOException if writing the redirect or error response fails
   */
  @GetMapping("/enrollDevice")
  public void enroll(
      @RequestParam(value = "redirectUri") String redirectUri,
      @RequestParam(value = "state") String state,
      HttpServletResponse response)
      throws IOException {

    if (!hasValidUserGroupAuthorization()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "forbidden_user");
      return;
    }
    if (!isRedirectUriAllowed(redirectUri)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid_redirect_uri");
      return;
    }

    IatPair iaToken = oAuth2DcrService.createIat(redirectUri);

    // Avoid caching
    response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");

    String redirectUrl =
        UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("iat", iaToken.iatJwt())
            .queryParamIfPresent("state", java.util.Optional.ofNullable(state))
            .build(true)
            .toUriString();

    response.sendRedirect(response.encodeRedirectURL(redirectUrl));
  }

  /**
   * Checks whether the current user is allowed to enroll a device based on user group membership,
   * using the {@code deviceEnrollmentAllowedUserGroups} system setting as a comma-separated list of
   * group ids. If the setting is blank, all authenticated users are allowed.
   *
   * @return {@code true} if the caller may enroll a device, {@code false} otherwise
   */
  private boolean hasValidUserGroupAuthorization() {
    String allowedUserGroups =
        systemSettingsService.getCurrentSettings().getDeviceEnrollmentAllowedUserGroups();
    if (!allowedUserGroups.isBlank()) {
      Set<String> groupIds = CurrentUserUtil.getCurrentUserDetails().getUserGroupIds();
      boolean member = false;
      for (String token : allowedUserGroups.split(",")) {
        String trimmed = token.trim();
        if (trimmed.isEmpty()) continue;
        if (groupIds.contains(trimmed)) {
          member = true;
          break;
        }
      }
      return member;
    }
    return true;
  }

  /**
   * Checks whether the supplied redirect URI is permitted for device enrollment. Each non-blank
   * entry in the {@code deviceEnrollmentRedirectAllowlist} system setting is converted to a regex
   * via {@link TextUtils#createRegexFromGlob(String)} and matched case-insensitively against the
   * candidate URI. A blank or missing allowlist rejects all redirect URIs.
   *
   * @param redirectUri the redirect URI proposed by the caller
   * @return {@code true} if the redirect URI matches the allowlist, {@code false} otherwise
   */
  private boolean isRedirectUriAllowed(String redirectUri) {
    if (redirectUri == null || redirectUri.isBlank()) return false;
    String allowlist =
        systemSettingsService.getCurrentSettings().getDeviceEnrollmentRedirectAllowlist();
    if (!allowlist.isBlank()) {
      for (String entry : allowlist.split(",")) {
        String trimmed = entry.trim();
        if (trimmed.isEmpty()) continue;
        String regex = TextUtils.createRegexFromGlob(trimmed);
        if (Pattern.compile(regex).matcher(redirectUri.toLowerCase()).matches()) {
          return true;
        }
      }
      return false;
    }
    return false;
  }
}
