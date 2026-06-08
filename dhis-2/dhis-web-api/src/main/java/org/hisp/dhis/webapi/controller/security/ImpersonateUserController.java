/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.security.Authorities.F_IMPERSONATE_USER;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.security.ImpersonatingUserDetailsChecker;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for impersonating users. Most of the code is copied from
 * org.springframework.security.web.authentication.switchuser.SwitchUserFilter
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:support"})
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Conditional(value = UserImpersonationEnabledCondition.class)
public class ImpersonateUserController {

  private final DhisConfigurationProvider config;
  private final UserDetailsService userDetailsService;
  private final ApplicationEventPublisher eventPublisher;

  private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource =
      new WebAuthenticationDetailsSource();
  private final UserDetailsChecker userDetailsChecker = new ImpersonatingUserDetailsChecker();
  private final SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  @RequiresAuthority(anyOf = F_IMPERSONATE_USER)
  @PostMapping("/impersonate")
  public ImpersonateUserResponse impersonateUser(
      HttpServletRequest request, HttpServletResponse response, @RequestParam String username)
      throws ForbiddenException, NotFoundException {

    validateAllowed(request);

    try {
      username = (username != null) ? username : "";
      UserDetails targetUser = this.userDetailsService.loadUserByUsername(username);
      this.userDetailsChecker.check(targetUser);

      // remember the impersonator Authentication object in session
      // which will be used to 'exit' from the current switched user.
      Authentication impersonator = getEffectiveImpersonatorUserAuth(request);
      request.getSession().setAttribute("ORIGINAL_AUTH", impersonator);

      // create the new authentication token
      UsernamePasswordAuthenticationToken targetUserRequest =
          UsernamePasswordAuthenticationToken.authenticated(
              targetUser, targetUser.getPassword(), targetUser.getAuthorities());

      targetUserRequest.setDetails(authenticationDetailsSource.buildDetails(request));

      if (this.eventPublisher != null) {
        this.eventPublisher.publishEvent(
            new AuthenticationSwitchUserEvent(
                this.securityContextHolderStrategy.getContext().getAuthentication(), targetUser));
      }

      SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
      context.setAuthentication(targetUserRequest);
      this.securityContextHolderStrategy.setContext(context);
      this.securityContextRepository.saveContext(context, request, response);

      return ImpersonateUserResponse.builder()
          .status(ImpersonateUserResponse.STATUS.IMPERSONATION_SUCCESS)
          .username(username)
          .build();

    } catch (UsernameNotFoundException ex) {
      throw new NotFoundException("Username not found: %s".formatted(username));

    } catch (AuthenticationException ex) {
      throw new ForbiddenException("Forbidden, reason: %s".formatted(ex.getMessage()));
    }
  }

  private void validateAllowed(HttpServletRequest request) throws ForbiddenException {
    String remoteAddr = request.getRemoteAddr();
    boolean enabled = config.isEnabled(ConfigurationKey.SWITCH_USER_FEATURE_ENABLED);
    if (!enabled) {
      log.error(
          "Impersonation attempt when feature is disabled, from username: {}, IP address: {}",
          getEffectiveImpersonatorUserAuth(request).getName(),
          remoteAddr);
      throw new ForbiddenException(
          "Forbidden, user not allowed to impersonate user, feature disabled");
    }
    if (!hasAllowListedIp(remoteAddr, config)) {
      log.error(
          "Impersonation attempt from non allow-listed IP address: {}, username: {}",
          remoteAddr,
          getEffectiveImpersonatorUserAuth(request).getName());
      throw new ForbiddenException(
          "Forbidden, user not allowed to impersonate user from IP: %s".formatted(remoteAddr));
    }
  }

  @PostMapping("/impersonateExit")
  public ImpersonateUserResponse impersonateExit(
      HttpServletRequest request,
      HttpServletResponse response,
      @CurrentUser org.hisp.dhis.user.UserDetails userDetails)
      throws ForbiddenException, BadRequestException {

    validateAllowed(request);

    Authentication impersonator = getImpersonatorUserAuth(request);
    if (impersonator == null)
      throw new BadRequestException(
          "User not impersonating anyone, user: %s".formatted(userDetails.getUsername()));

    request.getSession().removeAttribute("ORIGINAL_AUTH");
    // publish event
    if (this.eventPublisher != null) {
      Authentication impersonated = securityContextHolderStrategy.getContext().getAuthentication();
      this.eventPublisher.publishEvent(
          new AuthenticationSwitchUserEvent(
              impersonated, (UserDetails) impersonator.getPrincipal()));
    }

    // update the current context back to the original user
    SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(impersonator);
    this.securityContextHolderStrategy.setContext(context);
    this.securityContextRepository.saveContext(context, request, response);

    return ImpersonateUserResponse.builder()
        .status(ImpersonateUserResponse.STATUS.IMPERSONATION_EXIT_SUCCESS)
        .username(impersonator.getName())
        .build();
  }

  @Nonnull
  private Authentication getEffectiveImpersonatorUserAuth(HttpServletRequest request) {
    Authentication impersonator = getImpersonatorUserAuth(request);
    if (impersonator != null) return impersonator;
    Authentication current = securityContextHolderStrategy.getContext().getAuthentication();
    if (current == null)
      throw new AuthenticationCredentialsNotFoundException(
          "No current user associated with this request");
    return current;
  }

  private static Authentication getImpersonatorUserAuth(HttpServletRequest request) {
    return (Authentication) request.getSession().getAttribute("ORIGINAL_AUTH");
  }

  public static boolean hasAllowListedIp(String remoteAddr, DhisConfigurationProvider config) {
    String property = config.getProperty(ConfigurationKey.SWITCH_USER_ALLOW_LISTED_IPS);
    for (String ip : property.split(",")) {
      if (ip.trim().equalsIgnoreCase(remoteAddr)) {
        return true;
      }
    }
    return false;
  }
}
