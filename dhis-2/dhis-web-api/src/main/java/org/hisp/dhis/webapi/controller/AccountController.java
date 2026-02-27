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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.badRequest;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.forbidden;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.user.UserConstants.RECOVERY_LOCKOUT_MINS;
import static org.springframework.http.CacheControl.noStore;

import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.HashUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetails;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CredentialsInfo;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.PasswordValidationResult;
import org.hisp.dhis.user.PasswordValidationService;
import org.hisp.dhis.user.RecaptchaResponse;
import org.hisp.dhis.user.RestoreOptions;
import org.hisp.dhis.user.RestoreType;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserLookup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.HttpServletRequestPaths;
import org.hisp.dhis.webapi.webdomain.user.UserLookups;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:metadata"})
@Controller
@RequestMapping("/api/account")
@Slf4j
@AllArgsConstructor
public class AccountController {
  private static final int MAX_LENGTH = 80;

  private static final int MAX_PHONE_NO_LENGTH = 30;

  private final UserService userService;

  private final TwoFactorAuthenticationProvider twoFactorAuthenticationProvider;

  private final ConfigurationService configurationService;

  private final PasswordManager passwordManager;

  private final SystemSettingsProvider settingsProvider;

  private final PasswordValidationService passwordValidationService;

  @PostMapping("/recovery")
  @ResponseBody
  @Deprecated(forRemoval = true, since = "2.41")
  public WebMessage recoverAccount(
      @RequestParam String username, SystemSettings settings, HttpServletRequest request)
      throws WebMessageException {
    if (!settings.getAccountRecoveryEnabled()) {
      return conflict("Account recovery is not enabled");
    }

    handleRecoveryLock(username);

    User user = userService.getUserByUsername(username);

    if (user == null) {
      return conflict("User does not exist: " + username);
    }

    ErrorCode errorCode = userService.validateRestore(user);

    if (errorCode != null) {
      throw new IllegalQueryException(errorCode);
    }

    if (!userService.sendRestoreOrInviteMessage(
        user,
        HttpServletRequestPaths.getContextPath(request),
        RestoreOptions.RECOVER_PASSWORD_OPTION)) {
      return conflict("Account could not be recovered");
    }

    log.info("Recovery message sent for user: " + username);

    return ok("Recovery message sent");
  }

  private void handleRecoveryLock(String username) throws WebMessageException {
    if (userService.isRecoveryLocked(username)) {
      throw new WebMessageException(
          forbidden(
              "The account recovery operation for the given user is temporarily locked due to too "
                  + "many calls to this endpoint in the last '"
                  + RECOVERY_LOCKOUT_MINS
                  + "' minutes. Username:"
                  + username));
    } else {
      userService.registerRecoveryAttempt(username);
    }
  }

  @PostMapping("/restore")
  @ResponseBody
  @Deprecated(forRemoval = true, since = "2.41")
  public WebMessage restoreAccount(
      @RequestParam String token, @RequestParam String password, SystemSettings settings) {
    String[] idAndRestoreToken = userService.decodeEncodedTokens(token);
    String idToken = idAndRestoreToken[0];

    User user = userService.getUserByIdToken(idToken);
    if (user == null || idAndRestoreToken.length < 2) {
      return conflict("Account recovery failed");
    }

    String restoreToken = idAndRestoreToken[1];

    if (!settings.getAccountRecoveryEnabled()) {
      return conflict("Account recovery is not enabled");
    }

    if (password.trim().equals(user.getUsername())) {
      return badRequest("Password cannot be equal to username");
    }

    CredentialsInfo credentialsInfo =
        new CredentialsInfo(
            user.getUsername(), password, user.getEmail() != null ? user.getEmail() : "", false);

    PasswordValidationResult result = passwordValidationService.validate(credentialsInfo);

    if (!result.isValid()) {
      return badRequest(result.getErrorMessage());
    }

    boolean restoreSuccess =
        userService.restore(user, restoreToken, password, RestoreType.RECOVER_PASSWORD);

    if (!restoreSuccess) {
      return badRequest("Account could not be restored");
    }

    log.info("Account restored for user: " + user.getUsername());

    return ok("Account restored");
  }

  @PostMapping
  @ResponseBody
  @Deprecated(forRemoval = true, since = "2.41")
  public WebMessage createAccount(
      @RequestParam String username,
      @RequestParam String firstName,
      @RequestParam String surname,
      @RequestParam String password,
      @RequestParam String email,
      @RequestParam String phoneNumber,
      @RequestParam(required = false) String inviteUsername,
      @RequestParam(required = false) String inviteToken,
      @RequestParam(value = "g-recaptcha-response", required = false) String recapResponse,
      HttpServletRequest request)
      throws IOException {
    WebMessage validateCaptcha = validateCaptcha(recapResponse, request);
    if (validateCaptcha != null) {
      return validateCaptcha;
    }

    boolean invitedByEmail = !Strings.isNullOrEmpty(inviteUsername);
    if (invitedByEmail) {
      String[] idAndRestoreToken = userService.decodeEncodedTokens(inviteToken);
      String idToken = idAndRestoreToken[0];
      String restoreToken = idAndRestoreToken[1];
      boolean usernameChoice = userService.getRestoreOptions(restoreToken).isUsernameChoice();

      UserRegistration userRegistration =
          UserRegistration.builder()
              .username(StringUtils.trimToNull(usernameChoice ? username : inviteUsername))
              .firstName(StringUtils.trimToNull(firstName))
              .surname(StringUtils.trimToNull(surname))
              .password(StringUtils.trimToNull(password))
              .email(StringUtils.trimToNull(email))
              .phoneNumber(StringUtils.trimToNull(phoneNumber))
              .build();

      WebMessage validateInput = validateInput(userRegistration, usernameChoice);
      if (validateInput != null) {
        return validateInput;
      }

      User user = userService.getUserByIdToken(idToken);
      if (user == null) {
        return badRequest("Invitation link not valid");
      }

      if (!userService.canRestore(user, restoreToken, RestoreType.INVITE)) {
        return badRequest("Invitation code not valid");
      }

      if (!email.equals(user.getEmail())) {
        return badRequest("Email don't match invited email");
      }

      if (!userService.restore(user, restoreToken, password, RestoreType.INVITE)) {
        log.warn("Invite restore failed for: " + userRegistration.getUsername());
        return badRequest("Unable to create invited user account");
      }

      updateInvitedByEmailUser(user, userRegistration, request);
    } else // Self registration
    {
      UserRegistration userRegistration =
          UserRegistration.builder()
              .username(StringUtils.trimToNull(username))
              .firstName(StringUtils.trimToNull(firstName))
              .surname(StringUtils.trimToNull(surname))
              .password(StringUtils.trimToNull(password))
              .email(StringUtils.trimToNull(email))
              .phoneNumber(StringUtils.trimToNull(phoneNumber))
              .build();

      WebMessage validateInput = validateInput(userRegistration, true);
      if (validateInput != null) {
        return validateInput;
      }

      if (!configurationService.getConfiguration().selfRegistrationAllowed()) {
        return badRequest("User self registration is not allowed");
      }

      if (userService.getUserByUsername(username) != null) {
        return badRequest("Username already exists!");
      }

      createAndAddSelfRegisteredUser(userRegistration, request);
    }

    return ok("Account created");
  }

  WebMessage validateCaptcha(String recapResponse, HttpServletRequest request) throws IOException {
    if (!settingsProvider.getCurrentSettings().getSelfRegistrationNoRecaptcha()) {
      if (recapResponse == null) {
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

  private WebMessage validateInput(
      UserRegistration userRegistration, boolean validateUsernameExists) {
    if (validateUserName(userRegistration.getUsername(), validateUsernameExists)
        .get("response")
        .equals("error")) {
      return badRequest("Username is not specified or invalid");
    }

    if (userRegistration.getFirstName() == null
        || userRegistration.getFirstName().trim().length() > MAX_LENGTH) {
      return badRequest("First name is not specified or invalid");
    }

    if (userRegistration.getSurname() == null
        || userRegistration.getSurname().trim().length() > MAX_LENGTH) {
      return badRequest("Last name is not specified or invalid");
    }

    if (userRegistration.getPassword() == null) {
      return badRequest("Password is not specified");
    }

    PasswordValidationResult passwordValidationResult =
        passwordValidationService.validate(
            new CredentialsInfo(
                userRegistration.getUsername(),
                userRegistration.getPassword(),
                userRegistration.getEmail(),
                true));
    if (!passwordValidationResult.isValid()) {
      return badRequest(passwordValidationResult.getErrorMessage());
    }

    if (userRegistration.getEmail() == null
        || !ValidationUtils.emailIsValid(userRegistration.getEmail())) {
      return badRequest("Email is not specified or invalid");
    }

    if (userRegistration.getPhoneNumber() == null
        || userRegistration.getPhoneNumber().trim().length() > MAX_PHONE_NO_LENGTH) {
      return badRequest("Phone number is not specified or invalid");
    }

    return null;
  }

  @Data
  @Builder
  private static class UserRegistration {
    private final String username;

    private final String firstName;

    private final String surname;

    private final String password;

    private final String email;

    private final String phoneNumber;
  }

  private void createAndAddSelfRegisteredUser(
      UserRegistration userRegistration, HttpServletRequest request) {
    UserRole userRole = configurationService.getConfiguration().getSelfRegistrationRole();
    OrganisationUnit orgUnit = configurationService.getConfiguration().getSelfRegistrationOrgUnit();

    User user = new User();
    user.setUsername(userRegistration.getUsername());
    user.setFirstName(userRegistration.getFirstName());
    user.setSurname(userRegistration.getSurname());
    user.setEmail(userRegistration.getEmail());
    user.setPhoneNumber(userRegistration.getPhoneNumber());
    user.getOrganisationUnits().add(orgUnit);
    user.getDataViewOrganisationUnits().add(orgUnit);

    userService.encodeAndSetPassword(user, userRegistration.getPassword());

    user.setSelfRegistered(true);
    user.getUserRoles().add(userRole);

    userService.addUser(user, new SystemUser());

    log.info("Created user with username: " + user.getUsername());

    authenticateUser(user, user.getUsername(), userRegistration.getPassword(), request);
  }

  private void updateInvitedByEmailUser(
      User user, UserRegistration userRegistration, HttpServletRequest request) {
    user.setUsername(userRegistration.getUsername());
    user.setFirstName(userRegistration.getFirstName());
    user.setSurname(userRegistration.getSurname());
    user.setPhoneNumber(userRegistration.getPhoneNumber());
    user.setEmail(userRegistration.getEmail());
    user.setPhoneNumber(userRegistration.getPhoneNumber());

    boolean autoVerifyEmail = settingsProvider.getCurrentSettings().getAutoVerifyInvitedUserEmail();
    if (autoVerifyEmail && user.getEmail() != null) {
      user.setVerifiedEmail(user.getEmail());
    }

    userService.updateUser(user, new SystemUser());

    log.info("User " + user.getUsername() + " accepted invitation.");

    authenticateUser(user, user.getUsername(), userRegistration.getPassword(), request);
  }

  private void authenticateUser(
      User user, String username, String password, HttpServletRequest request) {
    Set<GrantedAuthority> authorities = getAuthorities(user.getUserRoles());
    authenticate(username, password, authorities, request);
  }

  @PostMapping("/password")
  public ResponseEntity<Map<String, String>> updatePassword(
      @RequestParam String oldPassword,
      @RequestParam String password,
      @RequestParam String username,
      HttpServletRequest request) {
    Map<String, String> result = new HashMap<>();

    if (username == null) {
      username = (String) request.getSession().getAttribute("username");
    }

    User user = userService.getUserByUsername(username);

    if (username == null) {
      result.put("status", "NON_EXPIRED");
      result.put("message", "Username is not valid, redirecting to login.");

      return ResponseEntity.badRequest().cacheControl(noStore()).body(result);
    }

    CredentialsInfo credentialsInfo =
        new CredentialsInfo(user.getUsername(), password, user.getEmail(), false);

    if (userService.userNonExpired(user)) {
      result.put("status", "NON_EXPIRED");
      result.put("message", "Account is not expired, redirecting to login.");

      return ResponseEntity.badRequest().cacheControl(noStore()).body(result);
    }

    if (!passwordManager.matches(oldPassword, user.getPassword())) {
      result.put("status", "NON_MATCHING_PASSWORD");
      result.put("message", "Old password is wrong, please correct and try again.");

      return ResponseEntity.badRequest().cacheControl(noStore()).body(result);
    }

    PasswordValidationResult passwordValidationResult =
        passwordValidationService.validate(credentialsInfo);

    if (!passwordValidationResult.isValid()) {
      result.put("status", "PASSWORD_INVALID");
      result.put("message", passwordValidationResult.getErrorMessage());

      return ResponseEntity.badRequest().cacheControl(noStore()).body(result);
    }

    if (password.trim().equals(username.trim())) {
      result.put("status", "PASSWORD_EQUAL_TO_USERNAME");
      result.put("message", "Password cannot be equal to username");

      return ResponseEntity.badRequest().cacheControl(noStore()).body(result);
    }

    userService.encodeAndSetPassword(user, password);
    userService.updateUser(user, new SystemUser());

    authenticate(username, password, getAuthorities(user.getUserRoles()), request);

    result.put("status", "OK");
    result.put("message", "Account was updated.");

    return ResponseEntity.ok().cacheControl(noStore()).body(result);
  }

  @GetMapping("/linkedAccounts")
  public @ResponseBody UserLookups getLinkedAccounts(@CurrentUser User currentUser) {
    List<UserLookup> linkedUserAccounts = userService.getLinkedUserAccounts(currentUser);
    return new UserLookups(linkedUserAccounts);
  }

  @GetMapping("/username")
  public ResponseEntity<Map<String, String>> validateUserNameGet(@RequestParam String username) {
    return ResponseEntity.ok().cacheControl(noStore()).body(validateUserName(username, true));
  }

  @PostMapping("/validateUsername")
  public ResponseEntity<Map<String, String>> validateUserNameGetPost(
      @RequestParam String username) {
    return ResponseEntity.ok().cacheControl(noStore()).body(validateUserName(username, true));
  }

  @GetMapping("/password")
  public ResponseEntity<Map<String, String>> validatePasswordGet(@RequestParam String password) {
    return ResponseEntity.ok().cacheControl(noStore()).body(validatePassword(password));
  }

  @PostMapping("/validatePassword")
  public ResponseEntity<Map<String, String>> validatePasswordPost(
      @RequestParam String password, HttpServletResponse response) {
    return ResponseEntity.ok().cacheControl(noStore()).body(validatePassword(password));
  }

  @PostMapping("/sendEmailVerification")
  @ResponseStatus(HttpStatus.CREATED)
  public void sendEmailVerification(@CurrentUser User currentUser, HttpServletRequest request)
      throws ConflictException {
    if (Strings.isNullOrEmpty(currentUser.getEmail())) {
      throw new ConflictException("User has no email set");
    }
    if (currentUser.isEmailVerified()) {
      throw new ConflictException("User has already verified the email address");
    }
    if (userService.getUserByVerifiedEmail(currentUser.getEmail()) != null) {
      throw new ConflictException(
          "The email the user is trying to verify is already verified by another account");
    }
    if (!settingsProvider.getCurrentSettings().isEmailConfigured()) {
      throw new ConflictException("System has no SMTP server configured");
    }

    // Generate a new email verification token and send it, we do this in two steps:
    // 1. Generate and save the token to the user
    // 2. Send the token to the user's email
    // This is because email delivery is unreliable can fail/respond false even if email is sent,
    // and true if email is not sent/received.
    String token = userService.generateAndSetNewEmailVerificationToken(currentUser);
    boolean successfullySent =
        userService.sendEmailVerificationToken(
            currentUser, token, HttpServletRequestPaths.getContextPath(request));

    if (!successfullySent) {
      throw new ConflictException(
          "Sorry, we couldn’t send your verification email. Please try again or contact support.");
    }
  }

  @GetMapping("/verifyEmail")
  public void verifyEmail(
      @RequestParam String token, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (userService.verifyEmail(token)) {
      response.sendRedirect(
          ContextUtils.getRootPath(request) + "/login/#/email-verification-success");
    } else {
      response.sendRedirect(
          ContextUtils.getRootPath(request) + "/login/#/email-verification-failure");
    }
  }

  @GetMapping("/listSessions")
  public @ResponseBody Map<String, String> listSessions(@CurrentUser UserDetails userDetails) {
    List<SessionInformation> sessionInformation = userService.listSessions(userDetails);
    return sessionInformation.stream()
        .collect(
            Collectors.toMap(
                s -> HashUtils.hashSHA1(s.getSessionId().getBytes()),
                s -> String.valueOf(s.isExpired())));
  }

  // ---------------------------------------------------------------------
  // Supportive methods
  // ---------------------------------------------------------------------

  private Map<String, String> validateUserName(String username, boolean validateIfExists) {
    boolean isNull = username == null;
    boolean usernameExists = userService.getUserByUsernameIgnoreCase(username) != null;
    boolean isValidSyntax = ValidationUtils.usernameIsValid(username, false);

    // Custom code required because of our hacked jQuery validation
    Map<String, String> result = new HashMap<>();
    if (isNull) {
      result.put("message", "Username is null");
    } else if (!isValidSyntax) {
      result.put("message", "Username is not valid");
    } else if (validateIfExists && usernameExists) {
      result.put("message", "Username is already taken");
    }

    result.put("response", result.isEmpty() ? "success" : "error");

    if (result.get("response").equals("success")) {
      result.put("message", "");
    }

    return result;
  }

  private Map<String, String> validatePassword(String password) {
    CredentialsInfo credentialsInfo =
        CredentialsInfo.builder().password(password).newUser(true).build();

    PasswordValidationResult passwordValidationResult =
        passwordValidationService.validate(credentialsInfo);

    // Custom code required because of our hacked jQuery validation

    Map<String, String> result = new HashMap<>();

    result.put("response", passwordValidationResult.isValid() ? "success" : "error");
    result.put(
        "message",
        passwordValidationResult.isValid() ? "" : passwordValidationResult.getErrorMessage());

    return result;
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
