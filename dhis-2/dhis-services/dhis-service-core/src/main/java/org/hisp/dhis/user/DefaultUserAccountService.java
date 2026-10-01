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
package org.hisp.dhis.user;

import static org.hisp.dhis.user.UserConstants.MAX_LENGTH_NAME;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.auth.RegistrationParams;
import org.hisp.dhis.common.auth.UserInviteParams;
import org.hisp.dhis.common.auth.UserRegistrationParams;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.HiddenNotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetails;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultUserAccountService implements UserAccountService {

  /**
   * Valid bcrypt hash (of a random string, same cost as the configured encoder) used to spend one
   * password verification on the unknown-username path of {@link #updateExpiredPassword}, so that
   * unknown and known usernames respond in similar time (no enumeration via timing).
   */
  private static final String TIMING_EQUALIZATION_HASH =
      "$2a$10$4TXauPu06PhCTK8Up3oHi.0Y7SXLeu8ISJ6jq1GYpaaQOsSL5FOxG";

  private final UserService userService;
  private final ConfigurationService configService;
  private final TwoFactorAuthenticationProvider twoFactorAuthProvider;
  private final SystemSettingsProvider settingsProvider;
  private final PasswordValidationService passwordValidationService;
  private final DhisConfigurationProvider configProvider;
  private final PasswordManager passwordManager;

  @Override
  public void validateUserRegistration(RegistrationParams params, String remoteAddress)
      throws BadRequestException, IOException {
    log.info("Validating user info");
    validateCaptcha(params.getRecaptchaResponse(), remoteAddress);
    validateUserRegistration(params);

    if (!configService.getConfiguration().selfRegistrationAllowed()) {
      throw new BadRequestException("User self registration is not allowed");
    }
  }

  @Override
  public void validateInvitedUser(RegistrationParams params, String remoteIpAddress)
      throws BadRequestException, IOException {
    validateCaptcha(params.getRecaptchaResponse(), remoteIpAddress);
    validateInvitedUser(params);
  }

  @Override
  @Transactional
  public void registerUser(UserRegistrationParams params, HttpServletRequest request) {
    UserRole userRole = configService.getConfiguration().getSelfRegistrationRole();
    OrganisationUnit orgUnit = configService.getConfiguration().getSelfRegistrationOrgUnit();

    User user = new User();
    user.setUsername(params.getUsername());
    user.setFirstName(params.getFirstName());
    user.setSurname(params.getSurname());
    user.setEmail(params.getEmail());
    user.getOrganisationUnits().add(orgUnit);
    user.getDataViewOrganisationUnits().add(orgUnit);

    userService.encodeAndSetPassword(user, params.getPassword());
    user.setSelfRegistered(true);
    user.getUserRoles().add(userRole);

    userService.addUser(user, new SystemUser());
    log.info("Created new user");

    authenticate(user.getUsername(), params.getPassword(), user.getAuthorities(), request);
  }

  @Override
  @Transactional
  public void confirmUserInvite(UserInviteParams params, HttpServletRequest request)
      throws BadRequestException {
    validateInvitedUser(params);

    User user = validateRestoreLinkAndToken(params);
    user.setUsername(params.getUsername());
    user.setFirstName(params.getFirstName());
    user.setSurname(params.getSurname());

    boolean autoVerifyEmail = settingsProvider.getCurrentSettings().getAutoVerifyInvitedUserEmail();
    if (autoVerifyEmail && user.getEmail() != null) {
      user.setVerifiedEmail(user.getEmail());
    }

    userService.encodeAndSetPassword(user, params.getPassword());
    userService.updateUser(user, new SystemUser());
    log.info("User invitation accepted");

    authenticate(user.getUsername(), params.getPassword(), user.getAuthorities(), request);
  }

  @Override
  @Transactional
  public void forgotPassword(String emailOrUsername)
      throws HiddenNotFoundException, ConflictException, ForbiddenException {
    if (!settingsProvider.getCurrentSettings().getAccountRecoveryEnabled()) {
      throw new ConflictException("Account recovery is not enabled");
    }

    String baseUrl = configProvider.getServerBaseUrl();
    if (StringUtils.isEmpty(baseUrl)) {
      throw new ConflictException("Server base URL is not configured");
    }

    User user = getUserByEmailOrUsername(emailOrUsername);

    checkRecoveryLock(user.getUsername());

    ErrorCode errorCode = userService.validateRestore(user);
    if (errorCode != null) {
      log.warn("Validate email restore failed: {}", errorCode);
      throw new HiddenNotFoundException("Validate failed: " + errorCode);
    }

    if (!userService.sendRestoreOrInviteMessage(
        user, baseUrl, RestoreOptions.RECOVER_PASSWORD_OPTION)) {
      throw new ConflictException("Account could not be recovered");
    }

    log.info("Forgot email was sent to user: {}", user.getUsername());
  }

  @Override
  @Transactional
  public void resetPassword(String token, String newPassword)
      throws ConflictException, BadRequestException {
    if (!settingsProvider.getCurrentSettings().getAccountRecoveryEnabled()) {
      throw new ConflictException("Account recovery is not enabled");
    }

    if (StringUtils.isBlank(token)) {
      throw new BadRequestException("Token is required");
    }
    if (StringUtils.isBlank(newPassword)) {
      throw new BadRequestException("New password is required");
    }

    String[] idAndRestoreToken = userService.decodeEncodedTokens(token);
    String idToken = idAndRestoreToken[0];

    User user = userService.getUserByIdToken(idToken);
    if (user == null || idAndRestoreToken.length < 2 || user.isExternalAuth()) {
      throw new ConflictException("Account recovery failed");
    }

    String restoreToken = idAndRestoreToken[1];

    validateNewPassword(user, newPassword);

    if (!userService.restore(user, restoreToken, newPassword, RestoreType.RECOVER_PASSWORD)) {
      throw new BadRequestException(
          "Account could not be restored for user: " + user.getUsername());
    }

    log.info("Password was reset for user: {}", user.getUsername());
  }

  @Override
  @Transactional
  public void updateExpiredPassword(String username, String oldPassword, String newPassword)
      throws BadRequestException, ForbiddenException {
    if (StringUtils.isBlank(username)
        || StringUtils.isBlank(oldPassword)
        || StringUtils.isBlank(newPassword)) {
      throw new BadRequestException("Username, old password and new password are required");
    }

    User user = userService.getUserByUsername(username);
    if (user == null) {
      // Generic message on purpose: do not reveal whether the username exists. Spend one password
      // verification (result deliberately ignored) so this path takes as long as the known-user
      // path below and the timing does not reveal it either.
      passwordManager.matches(oldPassword, TIMING_EQUALIZATION_HASH);
      throw new BadRequestException("Invalid username or password");
    }

    // Throttle repeated attempts against a single account (brute-force protection), reusing the
    // account-recovery lockout. Registers an attempt and rejects once the threshold is exceeded.
    checkRecoveryLock(user.getUsername());

    // Caller must know the current (expired) password. Checked before the expiry guard below so
    // that "Account is not expired" can only be observed by someone who knows the password.
    if (!passwordManager.matches(oldPassword, user.getPassword())) {
      throw new BadRequestException("Invalid username or password");
    }

    // This self-service path is ONLY for expired accounts.
    if (userService.userNonExpired(user)) {
      throw new BadRequestException("Account is not expired");
    }

    // The old password was verified against the stored hash above, so plain equality is enough.
    if (newPassword.equals(oldPassword)) {
      throw new BadRequestException("New password must be different from the old password");
    }

    validateNewPassword(user, newPassword);

    userService.encodeAndSetPassword(user, newPassword);
    userService.updateUser(user, new SystemUser());

    log.info("Expired password updated for user: {}", user.getUsername());
  }

  /** Rejects a new password that is equal to the username or fails the password policy. */
  private void validateNewPassword(User user, String newPassword) throws BadRequestException {
    if (newPassword.trim().equals(user.getUsername().trim())) {
      throw new BadRequestException("Password cannot be equal to username");
    }

    PasswordValidationResult result =
        passwordValidationService.validate(
            CredentialsInfo.builder()
                .username(user.getUsername())
                .password(newPassword)
                .email(StringUtils.trimToEmpty(user.getEmail()))
                .newUser(false)
                .build());
    if (!result.isValid()) {
      throw new BadRequestException(result.getErrorMessage());
    }
  }

  /** Registers a recovery attempt and rejects once the account has exceeded the allowed rate. */
  private void checkRecoveryLock(String username) throws ForbiddenException {
    if (userService.isRecoveryLocked(username)) {
      throw new ForbiddenException(
          "The account recovery operation for the given user is temporarily locked due to too "
              + "many calls to this endpoint in the last '"
              + UserConstants.RECOVERY_LOCKOUT_MINS
              + "' minutes. Username:"
              + username);
    }
    userService.registerRecoveryAttempt(username);
  }

  private User getUserByEmailOrUsername(String emailOrUsername) throws HiddenNotFoundException {
    User user;
    if (ValidationUtils.emailIsValid(emailOrUsername)) {
      user = userService.getUserByEmail(emailOrUsername);
    } else {
      user = userService.getUserByUsername(emailOrUsername);
    }
    if (user == null) {
      throw new HiddenNotFoundException("User does not exist: " + emailOrUsername);
    }
    return user;
  }

  private User validateRestoreLinkAndToken(UserInviteParams params) throws BadRequestException {
    String[] idAndRestoreToken = userService.decodeEncodedTokens(params.getToken());
    String idToken = idAndRestoreToken[0];
    String restoreToken = idAndRestoreToken[1];

    User user = userService.getUserByIdToken(idToken);
    if (user == null) {
      throw new BadRequestException("Invitation link not valid");
    }
    if (!userService.canRestore(user, restoreToken, RestoreType.INVITE)) {
      throw new BadRequestException("Invitation code not valid");
    }
    if (!userService.restore(user, restoreToken, params.getPassword(), RestoreType.INVITE)) {
      log.warn("Invite restore failed");
      throw new BadRequestException("Unable to update invited user account");
    }
    return user;
  }

  private void validateUserRegistration(RegistrationParams params) throws BadRequestException {
    validateUserName(params.getUsername());
    validateFirstName(params.getFirstName());
    validateSurname(params.getSurname());
    validatePassword(params);
    validateEmail(params.getEmail());
  }

  /**
   * Validating an invited user does not currently include validating the username or email as we
   * assume these are valid already when the admin initially sets the invited user up.
   *
   * @param params to validate
   * @throws BadRequestException validation error
   */
  private void validateInvitedUser(RegistrationParams params) throws BadRequestException {
    validateFirstName(params.getFirstName());
    validateSurname(params.getSurname());
    validatePassword(params);
  }

  private void validateCaptcha(String recapResponse, String remoteAddress)
      throws BadRequestException, IOException {
    if (!settingsProvider.getCurrentSettings().getSelfRegistrationNoRecaptcha()) {
      if (recapResponse == null) {
        log.warn("Recaptcha validation failed, null response received");
        throw new BadRequestException("Recaptcha validation failed.");
      }

      RecaptchaResponse recaptchaResponse =
          userService.verifyRecaptcha(recapResponse, remoteAddress);
      if (!recaptchaResponse.success()) {
        log.warn("Recaptcha validation failed: " + recaptchaResponse.getErrorCodes());
        throw new BadRequestException(
            "Recaptcha validation failed: " + recaptchaResponse.getErrorCodes());
      }
    }
  }

  public void authenticate(
      String username,
      String rawPassword,
      Collection<GrantedAuthority> authorities,
      HttpServletRequest request) {
    UsernamePasswordAuthenticationToken token =
        new UsernamePasswordAuthenticationToken(username, rawPassword, authorities);
    token.setDetails(new TwoFactorWebAuthenticationDetails(request));
    Authentication auth = twoFactorAuthProvider.authenticate(token);
    SecurityContextHolder.getContext().setAuthentication(auth);
    HttpSession session = request.getSession();
    session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
  }

  private void validateUserName(String username) throws BadRequestException {
    boolean isNull = username == null;
    boolean usernameExists = userService.getUserByUsernameIgnoreCase(username) != null;
    boolean isValidSyntax = ValidationUtils.usernameIsValid(username, false);

    if (isNull || !isValidSyntax || usernameExists) {
      log.warn("Username validation failed");
      throw new BadRequestException("Username is not specified or invalid");
    }
  }

  private void validateFirstName(String firstName) throws BadRequestException {
    if (firstName == null || firstName.trim().length() > MAX_LENGTH_NAME) {
      log.warn("First name validation failed");
      throw new BadRequestException("First name is not specified or invalid");
    }
  }

  private void validateSurname(String surname) throws BadRequestException {
    if (surname == null || surname.trim().length() > MAX_LENGTH_NAME) {
      log.warn("Surname validation failed");
      throw new BadRequestException("Surname is not specified or invalid");
    }
  }

  private void validateEmail(String email) throws BadRequestException {
    if (email == null || !ValidationUtils.emailIsValid(email)) {
      log.warn("Email validation failed");
      throw new BadRequestException("Email is not specified or invalid");
    }
  }

  private void validatePassword(RegistrationParams params) throws BadRequestException {
    if (params.getPassword() == null) {
      log.warn("Password validation failed");
      throw new BadRequestException("Password is not specified");
    }

    PasswordValidationResult passwordValidationResult =
        passwordValidationService.validate(
            new CredentialsInfo(
                params.getUsername(), params.getPassword(), params.getEmail(), true));
    if (!passwordValidationResult.isValid()) {
      log.warn("Password validation failed");
      throw new BadRequestException(passwordValidationResult.getErrorMessage());
    }
  }
}
