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
package org.hisp.dhis.security;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.CurrentUserDetails;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.session.SessionFixationProtectionEvent;
import org.springframework.util.ClassUtils;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class AuthenticationLoggerListener
    implements ApplicationListener<AbstractAuthenticationEvent> {
  private final HashFunction sessionIdHasher = Hashing.sha256();

  public void onApplicationEvent(AbstractAuthenticationEvent event) {
    if (!log.isWarnEnabled()) {
      return;
    }

    if (SessionFixationProtectionEvent.class.isAssignableFrom(event.getClass())
        || InteractiveAuthenticationSuccessEvent.class.isAssignableFrom(event.getClass())) {
      return;
    }

    logAuthenticationEvent(event);
  }

  private void logAuthenticationEvent(AbstractAuthenticationEvent event) {
    String eventClassName =
        String.format("Authentication event: %s; ", ClassUtils.getShortName(event.getClass()));
    Authentication authentication = event.getAuthentication();

    String authName = StringUtils.firstNonEmpty(authentication.getName(), "");
    String ipAddress = "";
    String sessionId = "";
    String exceptionMessage = "";

    if (event instanceof AbstractAuthenticationFailureEvent) {
      exceptionMessage =
          "exception: " + ((AbstractAuthenticationFailureEvent) event).getException().getMessage();
    } else if (authentication.getDetails() instanceof ForwardedIpAwareWebAuthenticationDetails) {
      ForwardedIpAwareWebAuthenticationDetails authDetails =
          (ForwardedIpAwareWebAuthenticationDetails) authentication.getDetails();
      ipAddress = String.format("ip: %s; ", authDetails.getIp());
      sessionId = hashSessionId(authDetails.getSessionId());
    } else if (authentication instanceof OAuth2LoginAuthenticationToken) {
      OAuth2LoginAuthenticationToken authenticationToken =
          (OAuth2LoginAuthenticationToken) authentication;
      authName = getUsernameFromPrincipal(authenticationToken.getPrincipal());
      WebAuthenticationDetails oauthDetails =
          (WebAuthenticationDetails) authenticationToken.getDetails();
      ipAddress = String.format("ip: %s; ", oauthDetails.getRemoteAddress());
      sessionId = hashSessionId(oauthDetails.getSessionId());
    } else if (event.getSource() instanceof OAuth2AuthenticationToken) {
      OAuth2AuthenticationToken authenticationToken = (OAuth2AuthenticationToken) event.getSource();
      authName = getUsernameFromPrincipal(authenticationToken.getPrincipal());
    } else if (event.getSource() instanceof ApiTokenAuthenticationToken) {
      ApiTokenAuthenticationToken authenticationToken =
          (ApiTokenAuthenticationToken) event.getSource();
      CurrentUserDetails principal = authenticationToken.getPrincipal();
      if (principal != null) {
        authName = principal.getUsername();
      }
    }

    String usernamePrefix =
        Strings.isNullOrEmpty(authName) ? "" : String.format("username: %s; ", authName);
    String msg =
        TextUtils.removeNonEssentialChars(
            eventClassName + usernamePrefix + ipAddress + sessionId + exceptionMessage);
    log.info(StringUtils.removeEnd(msg.stripTrailing(), ";"));
  }

  private String getUsernameFromPrincipal(Object principal) {
    if (principal instanceof DhisOidcUser) {
      return ((DhisOidcUser) principal).getUsername();
    }
    return "";
  }

  private String hashSessionId(String sessionId) {
    if (sessionId == null) {
      return "";
    }
    String s = sessionIdHasher.newHasher().putString(sessionId, Charsets.UTF_8).hash().toString();
    return String.format("sessionId: %s; ", s);
  }
}
