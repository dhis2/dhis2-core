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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.unauthorized;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.security.TwoFactoryAuthenticationUtils;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Håkonsen
 * @author Morten Svanaes
 */
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/2fa")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@AllArgsConstructor
public class TwoFactorController {
  private final UserService defaultUserService;

  /**
   * @deprecated Use {@link #generateQRCode}.
   */
  @Deprecated(since = "2.39")
  @GetMapping(value = "/qr", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  @ResponseBody
  public Map<String, String> getQrCode(@CurrentUser UserDetails currentUser)
      throws WebMessageException {
    if (currentUser == null) {
      throw new WebMessageException(conflict(ErrorCode.E3027.getMessage(), ErrorCode.E3027));
    }

    return Map.of("url", "url");
  }

  @OpenApi.Response(byte[].class)
  @GetMapping(value = "/qrCode", produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void generateQRCode(
      @CurrentUser User currentUser, HttpServletResponse response, SystemSettings settings)
      throws IOException, WebMessageException {
    if (currentUser == null) {
      throw new WebMessageException(conflict(ErrorCode.E3027.getMessage(), ErrorCode.E3027));
    }

    if (currentUser.isTwoFactorEnabled()
        && !UserService.hasTwoFactorSecretForApproval(currentUser)) {
      throw new WebMessageException(conflict(ErrorCode.E3022.getMessage(), ErrorCode.E3022));
    }

    defaultUserService.generateTwoFactorOtpSecretForApproval(currentUser);

    String appName = settings.getApplicationTitle();

    List<ErrorCode> errorCodes = new ArrayList<>();

    String qrContent =
        TwoFactoryAuthenticationUtils.generateQrContent(appName, currentUser, errorCodes::add);

    if (!errorCodes.isEmpty()) {
      throw new WebMessageException(conflict(errorCodes.get(0).getMessage(), errorCodes.get(0)));
    }

    byte[] qrCode =
        TwoFactoryAuthenticationUtils.generateQRCode(qrContent, 200, 200, errorCodes::add);

    if (!errorCodes.isEmpty()) {
      throw new WebMessageException(conflict(errorCodes.get(0).getMessage(), errorCodes.get(0)));
    }

    OutputStream outputStream = response.getOutputStream();
    outputStream.write(qrCode);
  }

  @GetMapping(
      value = "/enabled",
      consumes = {"text/*", "application/*"})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public boolean isEnabled(@CurrentUser(required = true) User currentUser) {
    return currentUser.isTwoFactorEnabled()
        && !UserService.hasTwoFactorSecretForApproval(currentUser);
  }

  /**
   * Enable 2FA authentication for the current user.
   *
   * @param body The body of the request.
   * @param currentUser This is the user that is currently logged in.
   */
  @PostMapping(
      value = "/enabled",
      consumes = {"text/*", "application/*"})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public WebMessage enable(
      @RequestBody Map<String, String> body, @CurrentUser(required = true) User currentUser)
      throws WebMessageException {
    String code = body.get("code");

    if (!currentUser.isTwoFactorEnabled()
        || !UserService.hasTwoFactorSecretForApproval(currentUser)) {
      throw new WebMessageException(conflict(ErrorCode.E3029.getMessage(), ErrorCode.E3029));
    }

    if (!verifyCode(code, currentUser)) {
      return unauthorized(ErrorCode.E3023.getMessage());
    }

    defaultUserService.enableTwoFa(currentUser, code);

    return ok("Two factor authentication was enabled successfully");
  }

  /**
   * Disable 2FA authentication for the current user.
   *
   * @param body The body of the request.
   * @param currentUser This is the user that is currently logged in.
   */
  @PostMapping(
      value = "/disabled",
      consumes = {"text/*", "application/*"})
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public WebMessage disable(
      @RequestBody Map<String, String> body, @CurrentUser(required = true) User currentUser)
      throws WebMessageException {
    String code = body.get("code");

    if (!currentUser.isTwoFactorEnabled()) {
      throw new WebMessageException(conflict(ErrorCode.E3031.getMessage(), ErrorCode.E3031));
    }

    if (defaultUserService.twoFaDisableIsLocked(currentUser.getUsername())) {
      throw new WebMessageException(conflict(ErrorCode.E3042.getMessage(), ErrorCode.E3042));
    }

    if (!verifyCode(code, currentUser)) {
      defaultUserService.registerFailed2FADisableAttempt(currentUser.getUsername());
      return unauthorized(ErrorCode.E3023.getMessage());
    }

    defaultUserService.disableTwoFa(currentUser, code);

    return ok("Two factor authentication was disabled successfully");
  }

  private static boolean verifyCode(String code, User currentUser) {
    if (currentUser == null) {
      throw new BadCredentialsException(ErrorCode.E3025.getMessage());
    }

    return TwoFactoryAuthenticationUtils.verify(code, currentUser.getSecret());
  }
}
