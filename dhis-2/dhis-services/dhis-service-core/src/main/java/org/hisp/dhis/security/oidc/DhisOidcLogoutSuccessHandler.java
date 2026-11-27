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

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.hisp.dhis.external.conf.ConfigurationKey.LINKED_ACCOUNTS_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.LINKED_ACCOUNTS_LOGOUT_URL;
import static org.hisp.dhis.external.conf.ConfigurationKey.LINKED_ACCOUNTS_RELOGIN_URL;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_LOGOUT_REDIRECT_URL;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_OAUTH2_LOGIN_ENABLED;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Redirects to the OIDC provider on logout, or to a configured local or linked-account destination.
 * An allowlisted mobile callback takes precedence. Only an authenticated OIDC POST can select
 * another linked account before redirecting to {@code linked_accounts.relogin_url}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class DhisOidcLogoutSuccessHandler implements LogoutSuccessHandler {

  private final DhisConfigurationProvider config;
  private final DhisOidcProviderRepository dhisOidcProviderRepository;
  private final UserService userService;
  private final SystemSettingsProvider settingsProvider;

  private SimpleUrlLogoutSuccessHandler handler;

  /** Configured only for linked accounts. Never change redirect targets during a request. */
  private SimpleUrlLogoutSuccessHandler linkedAccountsReloginHandler;

  @PostConstruct
  public void init() {
    if (!config.isEnabled(OIDC_OAUTH2_LOGIN_ENABLED)) {
      handler = redirectTo("/");
    } else if (config.isEnabled(LINKED_ACCOUNTS_ENABLED)) {
      handler = redirectTo(config.getProperty(LINKED_ACCOUNTS_LOGOUT_URL));
      linkedAccountsReloginHandler = redirectTo(config.getProperty(LINKED_ACCOUNTS_RELOGIN_URL));
    } else {
      String logoutUri = config.getPropertyOrDefault(OIDC_LOGOUT_REDIRECT_URL, "/");
      OidcClientInitiatedLogoutSuccessHandler oidcHandler =
          new OidcClientInitiatedLogoutSuccessHandler(dhisOidcProviderRepository);
      oidcHandler.setPostLogoutRedirectUri(logoutUri);
      oidcHandler.setDefaultTargetUrl(logoutUri);
      handler = oidcHandler;
    }
  }

  private static SimpleUrlLogoutSuccessHandler redirectTo(String url) {
    SimpleUrlLogoutSuccessHandler redirectHandler = new SimpleUrlLogoutSuccessHandler();
    // Fallback if not defined in config
    redirectHandler.setDefaultTargetUrl(isNullOrEmpty(url) ? "/" : url);
    return redirectHandler;
  }

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    if (redirectToMobileCallback(request, response)) {
      return;
    }

    if (linkedAccountsReloginHandler != null && switchLinkedAccount(request, authentication)) {
      linkedAccountsReloginHandler.onLogoutSuccess(request, response, authentication);
      return;
    }

    handler.onLogoutSuccess(request, response, authentication);
  }

  /**
   * Redirects a logout request that arrives without an authenticated session, e.g. when the session
   * already expired before the Android SDK's logout URL was opened. Redirects to the validated
   * {@code redirect_uri} mobile callback if present, otherwise to the login page. Does not log out,
   * write any account state or call an identity provider.
   */
  public void redirectAfterAnonymousLogout(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (!redirectToMobileCallback(request, response)) {
      response.sendRedirect(request.getContextPath() + "/login/");
    }
  }

  /**
   * Support custom redirect URI for mobile app deep links (e.g., dhis2oauth://oauth). Validated
   * against the device enrollment redirect allowlist to prevent open redirects.
   *
   * @return true if the request was redirected to the callback
   */
  private boolean redirectToMobileCallback(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String redirectUri = request.getParameter("redirect_uri");
    if (!isNullOrEmpty(redirectUri) && isRedirectUriAllowed(redirectUri)) {
      response.sendRedirect(redirectUri);
      return true;
    }
    return false;
  }

  /** Spring has already cleared the thread-local context; use the principal captured at logout. */
  private boolean switchLinkedAccount(HttpServletRequest request, Authentication authentication) {
    if (!"POST".equals(request.getMethod())) {
      return false;
    }
    if (!(authentication instanceof OAuth2AuthenticationToken token)
        || !token.isAuthenticated()
        || !(token.getPrincipal() instanceof DhisOidcUser oidcUser)) {
      return false;
    }
    String usernameToSwitchTo = request.getParameter("switch");
    if (isNullOrEmpty(usernameToSwitchTo)) {
      return false;
    }

    DhisOidcClientRegistration registration =
        dhisOidcProviderRepository.getDhisOidcClientRegistration(
            token.getAuthorizedClientRegistrationId());
    if (registration == null) {
      return false;
    }

    if (!(oidcUser.getAttributes().get(registration.getMappingClaimKey()) instanceof String openId)
        || openId.isBlank()) {
      return false;
    }

    User actingUser = userService.getUserByUsername(oidcUser.getUsername());
    if (actingUser == null || !openId.equals(actingUser.getOpenId())) {
      return false;
    }

    User target = userService.getUserByUsername(usernameToSwitchTo);
    if (target == null || target.isDisabled() || !openId.equals(target.getOpenId())) {
      return false;
    }

    userService.setActiveLinkedAccounts(actingUser.getUsername(), target.getUsername());
    return true;
  }

  /**
   * Check if the provided redirect URI is allowed based on the device enrollment redirect allowlist
   * from system settings. Uses case-insensitive glob-to-regex matching, consistent with {@link
   * org.hisp.dhis.commons.util.TextUtils#createRegexFromGlob}.
   *
   * @param redirectUri the redirect URI to check
   * @return true if the redirect URI matches an entry in the allowlist, false otherwise
   */
  private boolean isRedirectUriAllowed(String redirectUri) {
    if (redirectUri == null || redirectUri.isBlank()) return false;
    String allowlist = settingsProvider.getCurrentSettings().getDeviceEnrollmentRedirectAllowlist();
    if (allowlist == null || allowlist.isBlank()) return false;
    for (String entry : allowlist.split(",")) {
      String trimmed = entry.trim();
      if (trimmed.isEmpty()) continue;
      String regex = TextUtils.createRegexFromGlob(trimmed);
      if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(redirectUri).matches()) {
        return true;
      }
    }
    return false;
  }
}
