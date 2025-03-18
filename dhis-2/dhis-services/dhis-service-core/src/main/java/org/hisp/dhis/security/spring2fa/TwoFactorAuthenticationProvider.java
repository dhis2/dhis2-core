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
package org.hisp.dhis.security.spring2fa;

import static org.hisp.dhis.security.twofa.TwoFactorAuthService.TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME;
import static org.hisp.dhis.security.twofa.TwoFactorAuthUtils.isValid2FACode;

import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.security.ForwardedIpAwareWebAuthenticationDetails;
import org.hisp.dhis.security.twofa.TwoFactorAuthService;
import org.hisp.dhis.security.twofa.TwoFactorType;
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
  private TwoFactorAuthService twoFactorAuthService;
  private DhisConfigurationProvider configurationProvider;

  @Autowired
  public TwoFactorAuthenticationProvider(
      @Qualifier("userDetailsService") UserDetailsService detailsService,
      PasswordEncoder passwordEncoder,
      @Lazy UserService userService,
      @Lazy TwoFactorAuthService twoFactorAuthService,
      DhisConfigurationProvider configurationProvider) {

    this.userService = userService;
    this.twoFactorAuthService = twoFactorAuthService;
    this.configurationProvider = configurationProvider;
    setUserDetailsService(detailsService);
    setPasswordEncoder(passwordEncoder);
  }

  @Override
  public Authentication authenticate(Authentication auth) throws AuthenticationException {
    final String username = auth.getName();
    final Object details = auth.getDetails();

    // Extract the forwarded IP if available
    final String ip =
        details instanceof ForwardedIpAwareWebAuthenticationDetails fwd ? fwd.getIp() : "";

    log.debug("Login attempt: {}", username);

    // Check for temporary lockout
    checkLockout(username, ip);

    // Authenticate via the parent method (which calls UserDetailsService#loadUserByUsername())
    Authentication result = super.authenticate(auth);
    UserDetails userDetails = (UserDetails) result.getPrincipal();

    // Validate that the user is not configured for external auth only
    checkExternalAuth(userDetails, username);

    // If the user’s role requires 2FA enrollment but they haven’t set it up, throw an exception.
    checkTwoFactorEnrolment(userDetails);

    // Handle two-factor authentication validations.
    checkTwoFactorAuthentication(auth, userDetails);

    // Return a new authentication token with the user details.
    return new UsernamePasswordAuthenticationToken(
        userDetails, result.getCredentials(), result.getAuthorities());
  }

  private void checkLockout(String username, String ip) {
    if (userService.isLocked(username)) {
      log.warn("Temporary lockout for user: '{}'", username);
      throw new LockedException(String.format("IP is temporarily locked: %s", ip));
    }
  }

  private void checkExternalAuth(UserDetails userDetails, String username) {
    if (userDetails.isExternalAuth()) {
      log.info(
          "User has external authentication enabled, password login attempt aborted: '{}'",
          username);
      throw new BadCredentialsException(
          "Invalid login method, user is using external authentication");
    }
  }

  private void checkTwoFactorEnrolment(UserDetails userDetails) {
    boolean has2FARestriction =
        userDetails.hasAnyRestrictions(Set.of(TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME));
    if (!userDetails.isTwoFactorEnabled() && has2FARestriction) {
      throw new TwoFactorAuthenticationEnrolmentException(
          "User must setup two-factor authentication first before logging in");
    }
  }

  private void checkTwoFactorAuthentication(Authentication auth, UserDetails userDetails) {
    if (!userDetails.isTwoFactorEnabled()) {
      return; // Nothing to do if 2FA is not enabled.
    }

    // Ensure the authentication details are from a form-based 2FA login
    if (!(auth.getDetails() instanceof TwoFactorWebAuthenticationDetails authDetails)) {
      throw new PreAuthenticatedCredentialsNotFoundException(
          "User has 2FA enabled, but attempted to authenticate with a non-form based login method: "
              + userDetails.getUsername());
    }

    // Only validate the 2FA code if the configured type is enabled.
    if (isTwoFactorTypeEnabled(userDetails.getTwoFactorType())) {
      validate2FACode(authDetails.getCode(), userDetails);
    }
  }

  private boolean isTwoFactorTypeEnabled(TwoFactorType type) {
    return switch (type) {
      case EMAIL_ENABLED -> configurationProvider.isEnabled(ConfigurationKey.EMAIL_2FA_ENABLED);
      case TOTP_ENABLED -> configurationProvider.isEnabled(ConfigurationKey.TOTP_2FA_ENABLED);
      default -> false;
    };
  }

  private void validate2FACode(@CheckForNull String code, @Nonnull UserDetails userDetails) {
    TwoFactorType type = userDetails.getTwoFactorType();

    // For email-based 2FA, if no code is provided, trigger sending the email code.
    if (type == TwoFactorType.EMAIL_ENABLED && StringUtils.isBlank(code)) {
      sendEmail2FACode(userDetails);
      // Inform the caller that the email code has been sent.
      throw new TwoFactorCodeSentException(ErrorCode.E3051.getMessage(), type);
    }

    // If the code is blank (null, empty, or only whitespace), reject the login.
    if (StringUtils.isBlank(code)) {
      throw new TwoFactorAuthenticationException(ErrorCode.E3023.getMessage(), type);
    }

    // Validate the provided 2FA code.
    if (!isValid2FACode(type, code, userDetails.getSecret())) {
      throw new TwoFactorAuthenticationException(ErrorCode.E3023.getMessage(), type);
    }
    // If no exception is thrown, the 2FA code is valid.
  }

  private void sendEmail2FACode(UserDetails userDetails) {
    try {
      twoFactorAuthService.sendEmail2FACode(userDetails.getUsername());
    } catch (ConflictException e) {
      throw new TwoFactorAuthenticationException(
          ErrorCode.E3049.getMessage(), TwoFactorType.EMAIL_ENABLED);
    }
  }
}
