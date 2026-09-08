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
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.auth.UserInviteParams;
import org.hisp.dhis.common.auth.UserRegistrationParams;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.HiddenNotFoundException;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccountService;
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

  private final UserAccountService userAccountService;

  @PostMapping("/forgotPassword")
  @ResponseStatus(HttpStatus.OK)
  public void forgotPassword(@RequestBody ForgotPasswordRequest request)
      throws HiddenNotFoundException, ConflictException, ForbiddenException {
    userAccountService.forgotPassword(request.getEmailOrUsername());
  }

  @PostMapping("/passwordReset")
  @ResponseStatus(HttpStatus.OK)
  public void resetPassword(@RequestBody ResetPasswordRequest request)
      throws ConflictException, BadRequestException {
    userAccountService.resetPassword(request.getToken(), request.getNewPassword());
  }

  /**
   * Self-service change of an <b>expired</b> password, see {@link
   * UserAccountService#updateExpiredPassword}. This replaced the removed legacy form-param {@code
   * POST /api/account/password}. Reachable without authentication ({@code permitAll}) since a user
   * with an expired password cannot log in; the service flow is self-guarding.
   */
  @PostMapping("/updatePassword")
  @ResponseStatus(HttpStatus.OK)
  public WebMessage updatePassword(@RequestBody UpdatePasswordRequest request)
      throws BadRequestException, ForbiddenException {
    userAccountService.updateExpiredPassword(
        request.getUsername(), request.getOldPassword(), request.getNewPassword());
    return ok("Password updated");
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
}
