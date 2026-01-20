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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.auth.UserInviteParams;
import org.hisp.dhis.common.auth.UserRegistrationParams;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.HiddenNotFoundException;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CredentialsInfo;
import org.hisp.dhis.user.PasswordValidationResult;
import org.hisp.dhis.user.PasswordValidationService;
import org.hisp.dhis.user.RestoreOptions;
import org.hisp.dhis.user.RestoreType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccountService;
import org.hisp.dhis.user.UserConstants;
import org.hisp.dhis.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class is responsible for handling user account related operations such as password reset and
 * account creation.
 *
 * <p>These operations are migrated from the AccountController and adapted to the new LoginApp and
 * JSON input.
 *
 * <p>Operations migrated here, should be deprecated in the AccountController and will be removed in
 * the future.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class UserAccountController {

  private final UserService userService;
  private final UserAccountService userAccountService;
  private final PasswordValidationService passwordValidationService;
  private final DhisConfigurationProvider configurationProvider;

  @PostMapping("/forgotPassword")
  @ResponseStatus(HttpStatus.OK)
  public void forgotPassword(@RequestBody ForgotPasswordRequest request, SystemSettings settings)
      throws HiddenNotFoundException, ConflictException, ForbiddenException {

    if (!settings.getAccountRecoveryEnabled()) {
      throw new ConflictException("Account recovery is not enabled");
    }

    String baseUrl = configurationProvider.getServerBaseUrl();
    if (StringUtils.isEmpty(baseUrl)) {
      throw new ConflictException("Server base URL is not configured");
    }

    User user = getUser(request.getEmailOrUsername());

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

  @PostMapping("/passwordReset")
  @ResponseStatus(HttpStatus.OK)
  public void resetPassword(@RequestBody ResetPasswordRequest request, SystemSettings settings)
      throws ConflictException, BadRequestException {

    if (!settings.getAccountRecoveryEnabled()) {
      throw new ConflictException("Account recovery is not enabled");
    }

    String token = request.getToken();
    String newPassword = request.getNewPassword();

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

    if (newPassword.trim().equals(user.getUsername())) {
      throw new BadRequestException("Password cannot be equal to username");
    }

    CredentialsInfo credentialsInfo =
        CredentialsInfo.builder()
            .username(user.getUsername())
            .password(newPassword)
            .email(StringUtils.trimToEmpty(user.getEmail()))
            .newUser(false)
            .build();

    PasswordValidationResult result = passwordValidationService.validate(credentialsInfo);
    if (!result.isValid()) {
      throw new BadRequestException(result.getErrorMessage());
    }

    if (!userService.restore(user, restoreToken, newPassword, RestoreType.RECOVER_PASSWORD)) {
      throw new BadRequestException(
          "Account could not be restored for user: " + user.getUsername());
    }

    log.info("Password was reset for user: {}", user.getUsername());
  }

  @PostMapping("/registration")
  @ResponseStatus(HttpStatus.CREATED)
  public WebMessage registerUser(
      @RequestBody UserRegistrationParams params, HttpServletRequest request)
      throws BadRequestException, IOException {
    log.info("Self registration received");

    userAccountService.validateUserRegistration(params, request.getRemoteAddr());
    userAccountService.registerUser(params, request);

    log.info("User registration successful");
    return created("Account created");
  }

  @PostMapping("/invite")
  @ResponseStatus(HttpStatus.OK)
  public WebMessage invite(@RequestBody UserInviteParams params, HttpServletRequest request)
      throws BadRequestException, IOException {
    log.info("Invite registration received");

    userAccountService.validateInvitedUser(params, request.getRemoteAddr());
    userAccountService.confirmUserInvite(params, request);

    log.info("Invite confirmation successful");
    return ok("Account updated");
  }

  private void checkRecoveryLock(String username) throws ForbiddenException {
    if (userService.isRecoveryLocked(username)) {
      throw new ForbiddenException(
          "The account recovery operation for the given user is temporarily locked due to too "
              + "many calls to this endpoint in the last '"
              + UserConstants.RECOVERY_LOCKOUT_MINS
              + "' minutes. Username:"
              + username);
    } else {
      userService.registerRecoveryAttempt(username);
    }
  }

  private User getUser(String emailOrUsername) throws HiddenNotFoundException {
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
}
