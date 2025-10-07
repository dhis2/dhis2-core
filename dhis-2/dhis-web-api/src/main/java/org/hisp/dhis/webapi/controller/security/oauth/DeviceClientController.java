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
import org.hisp.dhis.security.oauth2.dcr.OAuth2DcrService;
import org.hisp.dhis.security.oauth2.dcr.OAuth2DcrService.IatPair;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Controller for enrolling devices using OAuth2 Dynamic Client Registration (DCR) (RFC 7591). A new
 * device client can be enrolled by obtaining an initial access token (IAT) using the custom
 * '/enroll' endpoint.
 *
 * <p>The IAT is then used to register a new client at the /connect/register (RFC 7591) endpoint of
 * the authorization server.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/deviceClients")
public class DeviceClientController {

  @Autowired private SystemSettingsService systemSettingsService;
  @Autowired private OAuth2DcrService oAuth2DcrService;

  /**
   * Enroll a new device client, create an initial access token (IAT) and then redirect to the
   * specified redirect URI with the IAT and state as query parameters, similar to the OAuth2
   * '/authorize' endpoint.
   *
   * @param redirectUri the redirect URI to send the IAT to
   * @param state an opaque value that will be returned to the client
   * @param response the HTTP response
   * @throws IOException if an I/O error occurs
   */
  @GetMapping("/enroll")
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
   * Check if the current user is authorized based on user group membership.
   *
   * <p>Default: allow all authenticated users regardless of group membership.
   *
   * @return true if the user is authorized, false otherwise
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
   * Check if the provided redirect URI is allowed based on an allowlist from system settings.
   *
   * <p>If no allowlist is configured, the following rules apply:
   *
   * @param redirectUri the redirect URI to check
   * @return true if the redirect URI is allowed, false otherwise
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
        if (Pattern.compile(regex).matcher(redirectUri).matches()) {
          return true;
        }
      }
      return false;
    }

    String lower = redirectUri.toLowerCase();
    if (lower.startsWith("dhis2-android://")) return true;
    if (lower.startsWith("https://")) return true;
    if (lower.startsWith("http://localhost")) return true;
    return false;
  }
}
