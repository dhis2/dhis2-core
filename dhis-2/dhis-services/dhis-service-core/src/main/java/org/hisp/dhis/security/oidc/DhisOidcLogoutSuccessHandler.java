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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class DhisOidcLogoutSuccessHandler implements LogoutSuccessHandler {

  private final DhisConfigurationProvider config;
  private final DhisOidcProviderRepository dhisOidcProviderRepository;
  private final UserService userService;

  private SimpleUrlLogoutSuccessHandler handler;

  @PostConstruct
  public void init() {
    if (config.isEnabled(OIDC_OAUTH2_LOGIN_ENABLED)) {
      setOidcLogoutUrl();
    } else {
      this.handler = new SimpleUrlLogoutSuccessHandler();
      this.handler.setDefaultTargetUrl("/");
    }
  }

  private void setOidcLogoutUrl() {
    String logoutUri = config.getPropertyOrDefault(OIDC_LOGOUT_REDIRECT_URL, "/");

    if (config.isEnabled(LINKED_ACCOUNTS_ENABLED)) {
      this.handler = new SimpleUrlLogoutSuccessHandler();
      this.handler.setDefaultTargetUrl(logoutUri);
    } else {
      OidcClientInitiatedLogoutSuccessHandler oidcHandler =
          new OidcClientInitiatedLogoutSuccessHandler(dhisOidcProviderRepository);
      oidcHandler.setPostLogoutRedirectUri(logoutUri);
      this.handler = oidcHandler;
      this.handler.setDefaultTargetUrl(logoutUri);
    }
  }

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    if (config.isEnabled(OIDC_OAUTH2_LOGIN_ENABLED) && config.isEnabled(LINKED_ACCOUNTS_ENABLED)) {
      handleLinkedAccountsLogout(request, response, authentication);
      return;
    }

    handler.onLogoutSuccess(request, response, authentication);
  }

  private void handleLinkedAccountsLogout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    String usernameToSwitchTo = request.getParameter("switch");
    String linkedAccountsLogoutUrl = config.getProperty(LINKED_ACCOUNTS_LOGOUT_URL);
    if (isNullOrEmpty(linkedAccountsLogoutUrl)) {
      // Fallback if not defined in config
      linkedAccountsLogoutUrl = "/";
    }

    if (isNullOrEmpty(usernameToSwitchTo)) {
      // No switch parameter present: redirect to linked_accounts.logout_url
      this.handler.setDefaultTargetUrl(linkedAccountsLogoutUrl);
    } else {
      // switch parameter present: switch accounts and then redirect to re-login URL
      String currentUsername = request.getParameter("current");
      if (!isNullOrEmpty(currentUsername)) {
        userService.setActiveLinkedAccounts(currentUsername, usernameToSwitchTo);
      }
      this.handler.setDefaultTargetUrl(config.getProperty(LINKED_ACCOUNTS_RELOGIN_URL));
    }

    handler.onLogoutSuccess(request, response, authentication);
  }
}
