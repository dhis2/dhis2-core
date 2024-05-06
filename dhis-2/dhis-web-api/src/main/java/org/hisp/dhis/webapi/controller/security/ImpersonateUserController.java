/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.ImpersonatingUserDetailsChecker;
import org.hisp.dhis.webapi.controller.security.ImpersonateUserResponse.STATUS;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent;
import org.springframework.security.web.authentication.switchuser.SwitchUserAuthorityChanger;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.Tags({"user", "login"})
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@Slf4j
public class ImpersonateUserController {

  public static final String ROLE_PREVIOUS_ADMINISTRATOR = "ROLE_PREVIOUS_ADMINISTRATOR";

  private final DhisConfigurationProvider config;
  private final UserDetailsService userDetailsService;
  private final ApplicationEventPublisher eventPublisher;

  private UserDetailsChecker userDetailsChecker = new ImpersonatingUserDetailsChecker();
  private SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();
  private SwitchUserAuthorityChanger switchUserAuthorityChanger;
  private String switchAuthorityRole = ROLE_PREVIOUS_ADMINISTRATOR;
  private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource =
      new WebAuthenticationDetailsSource();
  private SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  @PostMapping("/impersonate")
  public ImpersonateUserResponse impersonateUser(
      HttpServletRequest request, HttpServletResponse response, @RequestParam String username) {

    boolean enabled = config.isEnabled(ConfigurationKey.SWITCH_USER_FEATURE_ENABLED);
    if (!enabled) {
      return ImpersonateUserResponse.builder()
          .status(ImpersonateUserResponse.STATUS.FEATURE_IS_DISABLED)
          .build();
    }
    if (!hasAllowListedIp(request.getRemoteAddr())) {
      return ImpersonateUserResponse.builder()
          .status(ImpersonateUserResponse.STATUS.IP_NOT_ALLOWED)
          .build();
    }

    try {
      Authentication targetUser = attemptSwitchUser(request, username);
      // update the current context to the new target user
      SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();

      context.setAuthentication(targetUser);
      this.securityContextHolderStrategy.setContext(context);
      log.debug("Set SecurityContextHolder to %s".formatted(targetUser));

      this.securityContextRepository.saveContext(context, request, response);

      return ImpersonateUserResponse.builder()
          .status(ImpersonateUserResponse.STATUS.IMPERSONATION_SUCCESS)
          .impersonatedUsername(username)
          .build();

    } catch (AuthenticationException ex) {
      log.warn("Failed to switch user", ex);
      return ImpersonateUserResponse.builder()
          .status(STATUS.GENERIC_FAILURE)
          .message(ex.getMessage())
          .build();
    }
  }

  @PostMapping("/impersonateExit")
  public ImpersonateUserResponse impersonateExit(
      HttpServletRequest request, HttpServletResponse response) {

    // get the original authentication object (if exists)
    Authentication originalUser = attemptExitUser(request);
    // update the current context back to the original user
    SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();

    context.setAuthentication(originalUser);
    this.securityContextHolderStrategy.setContext(context);

    log.debug("Set SecurityContextHolder to %s".formatted(originalUser));
    this.securityContextRepository.saveContext(context, request, response);

    return ImpersonateUserResponse.builder()
        .status(ImpersonateUserResponse.STATUS.IMPERSONATION_EXIT_SUCCESS)
        .impersonatedUsername(originalUser.getName())
        .build();
  }

  private Authentication attemptSwitchUser(HttpServletRequest request, String username)
      throws AuthenticationException {

    UsernamePasswordAuthenticationToken targetUserRequest;
    username = (username != null) ? username : "";
    log.debug("Attempting to switch to user [%s]".formatted(username));
    UserDetails targetUser = this.userDetailsService.loadUserByUsername(username);
    this.userDetailsChecker.check(targetUser);

    // OK, create the switch user token
    targetUserRequest = createSwitchUserToken(request, targetUser);

    // publish event
    if (this.eventPublisher != null) {
      this.eventPublisher.publishEvent(
          new AuthenticationSwitchUserEvent(
              this.securityContextHolderStrategy.getContext().getAuthentication(), targetUser));
    }

    return targetUserRequest;
  }

  private UsernamePasswordAuthenticationToken createSwitchUserToken(
      HttpServletRequest request, UserDetails targetUser) {
    UsernamePasswordAuthenticationToken targetUserRequest;
    // grant an additional authority that contains the original Authentication object
    // which will be used to 'exit' from the current switched user.
    Authentication currentAuthentication = getCurrentAuthentication(request);
    GrantedAuthority switchAuthority =
        new SwitchUserGrantedAuthority(this.switchAuthorityRole, currentAuthentication);
    // get the original authorities
    Collection<? extends GrantedAuthority> orig = targetUser.getAuthorities();

    // Allow subclasses to change the authorities to be granted
    if (this.switchUserAuthorityChanger != null) {
      orig =
          this.switchUserAuthorityChanger.modifyGrantedAuthorities(
              targetUser, currentAuthentication, orig);
    }
    // add the new switch user authority
    List<GrantedAuthority> newAuths = new ArrayList<>(orig);
    newAuths.add(switchAuthority);

    // create the new authentication token
    targetUserRequest =
        UsernamePasswordAuthenticationToken.authenticated(
            targetUser, targetUser.getPassword(), newAuths);
    // set details
    targetUserRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));

    return targetUserRequest;
  }

  private Authentication getCurrentAuthentication(HttpServletRequest request) {
    try {
      // SEC-1763. Check first if we are already switched.
      return attemptExitUser(request);
    } catch (AuthenticationCredentialsNotFoundException ex) {
      return this.securityContextHolderStrategy.getContext().getAuthentication();
    }
  }

  protected Authentication attemptExitUser(HttpServletRequest request)
      throws AuthenticationCredentialsNotFoundException {
    // need to check to see if the current user has a SwitchUserGrantedAuthority
    Authentication current = this.securityContextHolderStrategy.getContext().getAuthentication();
    if (current == null) {
      throw new AuthenticationCredentialsNotFoundException(
          "No current user associated with this request");
    }

    // check to see if the current user did actual switch to another user
    // if so, get the original source user, so we can switch back
    Authentication original = getSourceAuthentication(current);
    if (original == null) {
      log.debug("Failed to find original user");
      throw new AuthenticationCredentialsNotFoundException("Failed to find original user");
    }

    // get the source user details
    UserDetails originalUser = null;

    Object obj = original.getPrincipal();
    if (obj instanceof UserDetails userDetails) {
      originalUser = userDetails;
    }

    // publish event
    if (this.eventPublisher != null) {
      this.eventPublisher.publishEvent(new AuthenticationSwitchUserEvent(current, originalUser));
    }

    return original;
  }

  private Authentication getSourceAuthentication(Authentication current) {
    Authentication original = null;

    // iterate over granted authorities and find the 'switch user' authority
    Collection<? extends GrantedAuthority> authorities = current.getAuthorities();
    for (GrantedAuthority auth : authorities) {
      // check for switch user type of authority
      if (auth instanceof SwitchUserGrantedAuthority switchUserGrantedAuthority) {
        original = switchUserGrantedAuthority.getSource();
        log.debug("Found original switch user granted authority [%s]".formatted(original));
      }
    }
    return original;
  }

  private boolean hasAllowListedIp(String remoteAddr) {
    String property = config.getProperty(ConfigurationKey.SWITCH_USER_ALLOW_LISTED_IPS);
    for (String ip : property.split(",")) {
      if (ip.trim().equalsIgnoreCase(remoteAddr)) {
        return true;
      }
    }
    return false;
  }
}
