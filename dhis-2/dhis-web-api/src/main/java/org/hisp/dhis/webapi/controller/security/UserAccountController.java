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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.badRequest;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.user.UserService.RECOVERY_LOCKOUT_MINS;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.auth.SelfRegistrationForm;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetails;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CredentialsInfo;
import org.hisp.dhis.user.PasswordValidationResult;
import org.hisp.dhis.user.PasswordValidationService;
import org.hisp.dhis.user.RecaptchaResponse;
import org.hisp.dhis.user.RestoreOptions;
import org.hisp.dhis.user.RestoreType;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
@OpenApi.Tags({"user"})
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@Slf4j
public class UserAccountController {

  private final UserService userService;
  private final SystemSettingManager systemSettingManager;
  private final PasswordValidationService passwordValidationService;
  private final ConfigurationService configurationService;
  private final TwoFactorAuthenticationProvider twoFactorAuthenticationProvider;

  private static final int MAX_LENGTH = 80;
  private static final int MAX_PHONE_NO_LENGTH = 30;

  @PostMapping("/forgotPassword")
  @ResponseStatus(HttpStatus.OK)
  public void forgotPassword(
      HttpServletRequest request, @RequestBody ForgotPasswordRequest forgotPasswordRequest)
      throws NotFoundException, ConflictException, ForbiddenException {

    String username = forgotPasswordRequest.getUsername();

    if (userService.isRecoveryLocked(username)) {
      throw new ForbiddenException(
          "The account recovery operation for the given user is temporarily locked due to too "
              + "many calls to this endpoint in the last '"
              + RECOVERY_LOCKOUT_MINS
              + "' minutes. Username:"
              + username);
    } else {
      userService.registerRecoveryAttempt(username);
    }

    User user = userService.getUserByUsername(username);

    if (user == null) {
      throw new NotFoundException("User does not exist: " + username);
    }

    ErrorCode errorCode = userService.validateRestore(user);

    if (errorCode != null) {
      throw new IllegalQueryException(errorCode);
    }

    if (!userService.sendRestoreOrInviteMessage(
        user, ContextUtils.getContextPath(request), RestoreOptions.RECOVER_PASSWORD_OPTION)) {
      throw new ConflictException("Account could not be recovered");
    }

    log.info("Recovery message sent for user: {}", username);
  }

  @PostMapping("/passwordReset")
  @ResponseStatus(HttpStatus.OK)
  public void resetPassword(@RequestBody ResetPasswordRequest resetRequest)
      throws ConflictException, BadRequestException {

    String token = resetRequest.getResetToken();
    String newPassword = resetRequest.getNewPassword();

    String[] idAndRestoreToken = userService.decodeEncodedTokens(token);
    String idToken = idAndRestoreToken[0];

    User user = userService.getUserByIdToken(idToken);
    if (user == null || idAndRestoreToken.length < 2) {
      throw new ConflictException("Account recovery failed");
    }
    String restoreToken = idAndRestoreToken[1];

    if (!systemSettingManager.accountRecoveryEnabled()) {
      throw new ConflictException("Account recovery is not enabled");
    }

    if (newPassword.trim().equals(user.getUsername())) {
      throw new BadRequestException("Password cannot be equal to username");
    }

    CredentialsInfo credentialsInfo =
        new CredentialsInfo(
            user.getUsername(), newPassword, StringUtils.trimToEmpty(user.getEmail()), false);

    PasswordValidationResult result = passwordValidationService.validate(credentialsInfo);

    if (!result.isValid()) {
      throw new BadRequestException(result.getErrorMessage());
    }

    boolean restoreSuccess =
        userService.restore(user, restoreToken, newPassword, RestoreType.RECOVER_PASSWORD);

    if (!restoreSuccess) {
      throw new BadRequestException("Account could not be restored");
    }

    log.info("Account restored for user: {}", user.getUsername());
  }

  @PostMapping("/register")
  public WebMessage register(
      @RequestBody SelfRegistrationForm registrationForm, HttpServletRequest request)
      throws BadRequestException, IOException {
    log.info("Self registration received");

    WebMessage validateCaptcha = validateCaptcha(registrationForm.getRecaptchaResponse(), request);
    if (validateCaptcha != null) {
      return validateCaptcha;
    }

    WebMessage validateInput = validateInput(registrationForm);
    if (validateInput != null) {
      return validateInput;
    }

    if (!configurationService.getConfiguration().selfRegistrationAllowed()) {
      return badRequest("User self registration is not allowed");
    }
    createAndAddSelfRegisteredUser(registrationForm, request);

    log.info("Self registration successful");
    return ok("Account created");
  }

  public WebMessage validateCaptcha(String recapResponse, HttpServletRequest request)
      throws IOException {
    if (!systemSettingManager.selfRegistrationNoRecaptcha()) {
      if (recapResponse == null) {
        log.warn("Recaptcha validation failed, null response received");
        return badRequest("Recaptcha validation failed.");
      }

      RecaptchaResponse recaptchaResponse =
          userService.verifyRecaptcha(recapResponse, request.getRemoteAddr());
      if (!recaptchaResponse.success()) {
        log.warn("Recaptcha validation failed: " + recaptchaResponse.getErrorCodes());
        return badRequest("Recaptcha validation failed: " + recaptchaResponse.getErrorCodes());
      }
    }

    return null;
  }

  private WebMessage validateInput(SelfRegistrationForm selfRegForm) throws BadRequestException {
    if (validateUserName(selfRegForm.getUsername()).get("response").equals("error")) {
      log.warn("Username validation failed");
      throw new BadRequestException("Username is not specified or invalid");
    }

    if (selfRegForm.getFirstName() == null
        || selfRegForm.getFirstName().trim().length() > MAX_LENGTH) {
      log.warn("First name validation failed");
      throw new BadRequestException("First name is not specified or invalid");
    }

    if (selfRegForm.getSurname() == null || selfRegForm.getSurname().trim().length() > MAX_LENGTH) {
      log.warn("Surname validation failed");
      throw new BadRequestException("Surname is not specified or invalid");
    }

    if (selfRegForm.getPassword() == null) {
      log.warn("Password validation failed");
      throw new BadRequestException("Password is not specified");
    }

    PasswordValidationResult passwordValidationResult =
        passwordValidationService.validate(
            new CredentialsInfo(
                selfRegForm.getUsername(),
                selfRegForm.getPassword(),
                selfRegForm.getEmail(),
                true));
    if (!passwordValidationResult.isValid()) {
      log.warn("Password validation failed");
      throw new BadRequestException(passwordValidationResult.getErrorMessage());
    }

    if (selfRegForm.getEmail() == null || !ValidationUtils.emailIsValid(selfRegForm.getEmail())) {
      log.warn("Email validation failed");
      throw new BadRequestException("Email is not specified or invalid");
    }

    if (selfRegForm.getPhoneNumber() == null
        || selfRegForm.getPhoneNumber().trim().length() > MAX_PHONE_NO_LENGTH) {
      log.warn("Phone number validation failed");
      throw new BadRequestException("Phone number is not specified or invalid");
    }

    return null;
  }

  private Map<String, String> validateUserName(String username) {
    boolean isNull = username == null;
    boolean usernameExists = userService.getUserByUsernameIgnoreCase(username) != null;
    boolean isValidSyntax = ValidationUtils.usernameIsValid(username, false);

    // Custom code required because of our hacked jQuery validation
    Map<String, String> result = new HashMap<>();
    if (isNull) {
      result.put("message", "Username is null");
    } else if (!isValidSyntax) {
      result.put("message", "Username is not valid");
    } else if (usernameExists) {
      result.put("message", "Username is already taken");
    }

    result.put("response", result.isEmpty() ? "success" : "error");

    if (result.get("response").equals("success")) {
      result.put("message", "");
    }

    return result;
  }

  private void createAndAddSelfRegisteredUser(
      SelfRegistrationForm selfRegForm, HttpServletRequest request) {
    UserRole userRole = configurationService.getConfiguration().getSelfRegistrationRole();
    OrganisationUnit orgUnit = configurationService.getConfiguration().getSelfRegistrationOrgUnit();

    User user = new User();
    user.setUsername(selfRegForm.getUsername());
    user.setFirstName(selfRegForm.getFirstName());
    user.setSurname(selfRegForm.getSurname());
    user.setEmail(selfRegForm.getEmail());
    user.setPhoneNumber(selfRegForm.getPhoneNumber());
    user.getOrganisationUnits().add(orgUnit);
    user.getDataViewOrganisationUnits().add(orgUnit);

    userService.encodeAndSetPassword(user, selfRegForm.getPassword());

    user.setSelfRegistered(true);
    user.getUserRoles().add(userRole);

    userService.addUser(user, new SystemUser());

    log.info("Created new user");

    authenticateUser(user, user.getUsername(), selfRegForm.getPassword(), request);
  }

  private void authenticateUser(
      User user, String username, String password, HttpServletRequest request) {
    Set<GrantedAuthority> authorities = getAuthorities(user.getUserRoles());
    authenticate(username, password, authorities, request);
  }

  private void authenticate(
      String username,
      String rawPassword,
      Collection<GrantedAuthority> authorities,
      HttpServletRequest request) {
    UsernamePasswordAuthenticationToken token =
        new UsernamePasswordAuthenticationToken(username, rawPassword, authorities);
    token.setDetails(new TwoFactorWebAuthenticationDetails(request));

    Authentication auth = twoFactorAuthenticationProvider.authenticate(token);

    SecurityContextHolder.getContext().setAuthentication(auth);

    HttpSession session = request.getSession();

    session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
  }

  private Set<GrantedAuthority> getAuthorities(Set<UserRole> userRoles) {
    Set<GrantedAuthority> auths = new HashSet<>();

    for (UserRole userRole : userRoles) {
      auths.addAll(getAuthorities(userRole));
    }

    return auths;
  }

  private Set<GrantedAuthority> getAuthorities(UserRole userRole) {
    Set<GrantedAuthority> auths = new HashSet<>();

    for (String auth : userRole.getAuthorities()) {
      auths.add(new SimpleGrantedAuthority(auth));
    }

    return auths;
  }
}
