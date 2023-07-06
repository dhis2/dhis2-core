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
package org.hisp.dhis.webapi.security.apikey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenAttribute;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.security.apikey.IpAllowedList;
import org.hisp.dhis.security.apikey.MethodAllowedList;
import org.hisp.dhis.security.apikey.RefererAllowedList;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.core.log.LogMessage;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class Dhis2ApiTokenFilter extends OncePerRequestFilter {
  private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";

  private final ApiTokenResolver apiTokenResolver = new ApiTokenResolver();

  private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource =
      new WebAuthenticationDetailsSource();

  private final ApiTokenAuthManager apiTokenAuthManager;

  private final AuthenticationFailureHandler authenticationFailureHandler;

  private final AuthenticationEntryPoint authenticationEntryPoint;

  private final ApiTokenService apiTokenService;

  private final AuthenticationEventPublisher eventPublisher;

  public Dhis2ApiTokenFilter(
      ApiTokenService apiTokenService,
      ApiTokenAuthManager apiTokenAuthManager,
      AuthenticationEntryPoint authenticationEntryPoint,
      DefaultAuthenticationEventPublisher defaultAuthenticationEventPublisher) {
    this.apiTokenService = apiTokenService;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.apiTokenAuthManager = apiTokenAuthManager;
    this.authenticationFailureHandler =
        getAuthenticationFailureHandler(
            authenticationEntryPoint, defaultAuthenticationEventPublisher);

    this.eventPublisher = defaultAuthenticationEventPublisher;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String tokenKey;
    try {
      tokenKey = this.apiTokenResolver.resolve(request);
    } catch (OAuth2AuthenticationException invalid) {
      this.logger.debug(
          "Sending to authentication entry point since failed to resolve API token", invalid);
      this.authenticationEntryPoint.commence(request, response, invalid);
      return;
    }

    if (tokenKey == null) {
      this.logger.debug("Did not process request since did not find API token in header or body");
      filterChain.doFilter(request, response);
      return;
    }

    final String hashedKey = apiTokenService.hashKey(tokenKey);
    tokenKey = null;

    try {
      ApiTokenAuthenticationToken authenticationToken =
          (ApiTokenAuthenticationToken)
              apiTokenAuthManager.authenticate(new ApiTokenAuthenticationToken(hashedKey));

      // Set values unique to each request
      authenticationToken.setDetails(this.authenticationDetailsSource.buildDetails(request));

      validateRequestRules(request, authenticationToken.getToken());

      authenticationToken.setAuthenticated(true);

      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authenticationToken);
      SecurityContextHolder.setContext(context);

      if (this.logger.isDebugEnabled()) {
        this.logger.debug(
            LogMessage.format("Set SecurityContextHolder to %s", authenticationToken));
      }

      if (this.eventPublisher != null) {
        this.eventPublisher.publishAuthenticationSuccess(authenticationToken);
      }

      filterChain.doFilter(request, response);
    } catch (AuthenticationException failed) {
      SecurityContextHolder.clearContext();

      this.logger.debug("Failed to process authentication request", failed);
      this.authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
    }
  }

  private void validateRequestRules(HttpServletRequest request, ApiToken token) {
    final List<String> errors = new ArrayList<>();

    for (ApiTokenAttribute attribute : token.getAttributes()) {
      if (attribute instanceof IpAllowedList) {
        validateIp(request, errors, (IpAllowedList) attribute);
      }

      if (attribute instanceof RefererAllowedList) {
        validateReferer(request, errors, (RefererAllowedList) attribute);
      }

      if (attribute instanceof MethodAllowedList) {
        validateMethod(request, errors, (MethodAllowedList) attribute);
      }
    }

    if (!errors.isEmpty()) {
      throw new ApiTokenConstraintsValidationFailedException(errors.get(0));
    }
  }

  private void validateMethod(
      HttpServletRequest request, List<String> errors, MethodAllowedList attribute) {
    if (!attribute.getAllowedMethods().isEmpty()) {
      String method = request.getMethod();

      if (!attribute.getAllowedMethods().contains(method)) {
        errors.add("Failed to authenticate API token, request http method is not allowed.");
      }
    }
  }

  private void validateReferer(
      HttpServletRequest request, List<String> errors, RefererAllowedList attribute) {
    if (!attribute.getAllowedReferrers().isEmpty()) {
      String referrer = request.getHeader("referer");

      if (referrer == null
          || !attribute.getAllowedReferrers().contains(referrer.toLowerCase(Locale.ROOT))) {
        errors.add(
            "Failed to authenticate API token, request http referrer is missing or not allowed.");
      }
    }
  }

  private void validateIp(
      HttpServletRequest request, List<String> errors, IpAllowedList attribute) {
    if (!attribute.getAllowedIps().isEmpty()) {
      String requestRemoteAddr =
          ObjectUtils.firstNonNull(
              request.getHeader(HEADER_FORWARDED_FOR), request.getRemoteAddr());

      if (!attribute.getAllowedIps().contains(requestRemoteAddr)) {
        errors.add("Failed to authenticate API token, request ip address is not allowed.");
      }
    }
  }

  /**
   * Custom authentication failure handler needed for proper failure messaging with the
   * AuthenticationLoggerListener
   */
  private AuthenticationFailureHandler getAuthenticationFailureHandler(
      AuthenticationEntryPoint authenticationEntryPoint,
      DefaultAuthenticationEventPublisher defaultAuthenticationEventPublisher) {
    return (request, response, exception) -> {
      defaultAuthenticationEventPublisher.publishAuthenticationFailure(
          exception,
          new AbstractAuthenticationToken(null) {
            @Override
            public Object getCredentials() {
              return null;
            }

            @Override
            public Object getPrincipal() {
              return null;
            }
          });

      authenticationEntryPoint.commence(request, response, exception);
    };
  }
}
