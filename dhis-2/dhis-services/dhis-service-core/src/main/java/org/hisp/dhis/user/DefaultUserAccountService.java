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
package org.hisp.dhis.user;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.auth.SelfRegistrationForm;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetails;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * @author david mackessy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultUserAccountService implements UserAccountService {

  private final UserService userService;
  private final ConfigurationService configService;
  private final TwoFactorAuthenticationProvider twoFactorAuthProvider;
  private final SystemSettingManager systemSettingManager;
  private final PasswordValidationService passwordValidationService;

  private static final int MAX_LENGTH = 80;
  private static final int MAX_PHONE_NO_LENGTH = 30;

  @Override
  public void createAndAddSelfRegisteredUser(
      SelfRegistrationForm selfRegForm, HttpServletRequest request) {
    UserRole userRole = configService.getConfiguration().getSelfRegistrationRole();
    OrganisationUnit orgUnit = configService.getConfiguration().getSelfRegistrationOrgUnit();

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

    authenticate(user.getUsername(), selfRegForm.getPassword(), user.getAuthorities(), request);
  }

  @Override
  public void validateUserFormInfo(SelfRegistrationForm selfRegForm, HttpServletRequest request)
      throws BadRequestException, IOException {
    validateCaptcha(selfRegForm.getRecaptchaResponse(), request);
    validateInput(selfRegForm);

    if (!configService.getConfiguration().selfRegistrationAllowed()) {
      throw new BadRequestException("User self registration is not allowed");
    }
  }

  private void validateInput(SelfRegistrationForm selfRegForm) throws BadRequestException {
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
  }

  @Override
  public void validateCaptcha(String recapResponse, HttpServletRequest request)
      throws BadRequestException, IOException {
    if (!systemSettingManager.selfRegistrationNoRecaptcha()) {
      if (recapResponse == null) {
        log.warn("Recaptcha validation failed, null response received");
        throw new BadRequestException("Recaptcha validation failed.");
      }

      RecaptchaResponse recaptchaResponse =
          userService.verifyRecaptcha(recapResponse, request.getRemoteAddr());
      if (!recaptchaResponse.success()) {
        log.warn("Recaptcha validation failed: " + recaptchaResponse.getErrorCodes());
        throw new BadRequestException(
            "Recaptcha validation failed: " + recaptchaResponse.getErrorCodes());
      }
    }
  }

  @Override
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
}
