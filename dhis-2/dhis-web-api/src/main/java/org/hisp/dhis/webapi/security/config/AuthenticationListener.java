/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetails;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * @author Henning Håkonsen
 */
@Slf4j
@Component
public class AuthenticationListener {
  @Autowired private SecurityService securityService;

  @Autowired private UserService userService;

  @Autowired private DhisConfigurationProvider config;

  @EventListener
  public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    Authentication auth = event.getAuthentication();
    String username = event.getAuthentication().getName();

    Object details = auth.getDetails();

    if (details != null
        && TwoFactorWebAuthenticationDetails.class.isAssignableFrom(details.getClass())) {
      TwoFactorWebAuthenticationDetails authDetails = (TwoFactorWebAuthenticationDetails) details;

      log.debug(String.format("Login attempt failed for remote IP: %s", authDetails.getIp()));
    }

    if (OAuth2LoginAuthenticationToken.class.isAssignableFrom(auth.getClass())) {
      OAuth2LoginAuthenticationToken authenticationToken = (OAuth2LoginAuthenticationToken) auth;
      DhisOidcUser principal = (DhisOidcUser) authenticationToken.getPrincipal();

      if (principal != null) {
        username = principal.getUser().getUsername();
      }

      WebAuthenticationDetails tokenDetails =
          (WebAuthenticationDetails) authenticationToken.getDetails();
      String remoteAddress = tokenDetails.getRemoteAddress();

      log.debug(String.format("OIDC login attempt failed for remote IP: %s", remoteAddress));
    }

    securityService.registerFailedLogin(username);
  }

  @EventListener({InteractiveAuthenticationSuccessEvent.class, AuthenticationSuccessEvent.class})
  public void handleAuthenticationSuccess(AbstractAuthenticationEvent event) {
    Authentication auth = event.getAuthentication();
    String username = event.getAuthentication().getName();

    Object details = auth.getDetails();

    if (TwoFactorWebAuthenticationDetails.class.isAssignableFrom(details.getClass())) {
      TwoFactorWebAuthenticationDetails authDetails = (TwoFactorWebAuthenticationDetails) details;

      log.debug(String.format("Login attempt succeeded for remote IP: %s", authDetails.getIp()));
    }

    if (OAuth2LoginAuthenticationToken.class.isAssignableFrom(auth.getClass())) {
      OAuth2LoginAuthenticationToken authenticationToken = (OAuth2LoginAuthenticationToken) auth;
      DhisOidcUser principal = (DhisOidcUser) authenticationToken.getPrincipal();
      username = principal.getUser().getUsername();

      WebAuthenticationDetails tokenDetails =
          (WebAuthenticationDetails) authenticationToken.getDetails();
      String remoteAddress = tokenDetails.getRemoteAddress();

      log.debug(String.format("OIDC login attempt succeeded for remote IP: %s", remoteAddress));
    }

    registerSuccessfulLogin(username);
  }

  private void registerSuccessfulLogin(String username) {
    User user = userService.getUserByUsername(username);

    boolean readOnly = config.isReadOnlyMode();

    if (Objects.nonNull(user) && !readOnly) {
      user.updateLastLogin();
      try {
        userService.updateUser(user);
      } catch (Exception e) {
        log.warn("Failed to update the user!", e);
      }
    }

    securityService.registerSuccessfulLogin(username);
  }
}
