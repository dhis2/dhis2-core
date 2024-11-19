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
package org.hisp.dhis.security.spring2fa;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.security.ForwardedIpAwareWebAuthenticationDetails;
import org.hisp.dhis.security.TwoFactoryAuthenticationUtils;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.stereotype.Component;

/**
 * @author Henning Håkonsen
 * @author Morten Svanæs
 */
@Slf4j
@Component
public class TwoFactorAuthenticationProvider extends DaoAuthenticationProvider {
  private UserService userService;

  @Autowired
  public TwoFactorAuthenticationProvider(
      @Qualifier("userDetailsService") UserDetailsService detailsService,
      PasswordEncoder passwordEncoder,
      @Lazy UserService userService) {

    this.userService = userService;
    setUserDetailsService(detailsService);
    setPasswordEncoder(passwordEncoder);
  }

  @Override
  public Authentication authenticate(Authentication auth) throws AuthenticationException {
    String username = auth.getName();
    String ip = "";

    if (auth.getDetails() instanceof ForwardedIpAwareWebAuthenticationDetails details) {
      ip = details.getIp();
    }

    log.debug("Login attempt: {}", username);

    // If enabled, temporarily block user with too many failed attempts
    if (userService.isLocked(username)) {
      log.debug("Temporary lockout for user: '{}' and IP: {}", username, ip);
      throw new LockedException(String.format("IP is temporarily locked: %s", ip));
    }

    // Calls the UserDetailsService#loadUserByUsername(), to create the UserDetails object,
    // after password is validated.
    Authentication result = super.authenticate(auth);
    UserDetails principal = (UserDetails) result.getPrincipal();

    // Prevents other authentication methods (e.g. OAuth2/LDAP),
    // to use password login.
    if (principal.isExternalAuth()) {
      log.info(
          "User is using external authentication, password login attempt aborted: '{}'", username);
      throw new BadCredentialsException(
          "Invalid login method, user is using external authentication");
    }

    validateTwoFactor(principal, auth.getDetails());

    return new UsernamePasswordAuthenticationToken(
        principal, result.getCredentials(), result.getAuthorities());
  }

  private void validateTwoFactor(UserDetails userDetails, Object details) {
    // If the user has 2FA enabled and tries to authenticate with HTTP Basic or OAuth
    if (userDetails.isTwoFactorEnabled()
        && !(details instanceof TwoFactorWebAuthenticationDetails)) {
      throw new PreAuthenticatedCredentialsNotFoundException(
          "User has 2FA enabled, but attempted to authenticate with a non-form based login method: "
              + userDetails.getUsername());
    }

    // If the user requires 2FA, and it's not enabled/provisioned, redirect to
    // the enrolment page, (via the CustomAuthFailureHandler)
    if (userService.hasTwoFactorRoleRestriction(userDetails) && !userDetails.isTwoFactorEnabled()) {
      throw new TwoFactorAuthenticationEnrolmentException(
          "User must setup two factor authentication");
    }

    if (userDetails.isTwoFactorEnabled()) {
      TwoFactorWebAuthenticationDetails authDetails = (TwoFactorWebAuthenticationDetails) details;
      if (authDetails == null) {
        log.info("Missing authentication details in authentication request");
        throw new PreAuthenticatedCredentialsNotFoundException(
            "Missing authentication details in authentication request");
      }

      validateTwoFactorCode(
          StringUtils.deleteWhitespace(authDetails.getCode()), userDetails.getUsername());
    }
  }

  private void validateTwoFactorCode(String code, String username) {
    User user = userService.getUserByUsername(username);

    code = StringUtils.deleteWhitespace(code);

    if (!TwoFactoryAuthenticationUtils.verify(code, user.getSecret())) {
      log.debug("Two-factor authentication failure for user: '{}'", user.getUsername());

      if (UserService.hasTwoFactorSecretForApproval(user)) {
        userService.resetTwoFactor(user, new SystemUser());
        throw new TwoFactorAuthenticationEnrolmentException("Invalid verification code");
      } else {
        throw new TwoFactorAuthenticationException("Invalid verification code");
      }
    } else if (UserService.hasTwoFactorSecretForApproval(user)) {
      userService.approveTwoFactorSecret(user, new SystemUser());
    }
  }
}
