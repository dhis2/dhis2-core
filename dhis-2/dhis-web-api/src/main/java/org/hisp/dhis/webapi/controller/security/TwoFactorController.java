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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.security.twofa.TwoFactorAuthService;
import org.hisp.dhis.security.twofa.TwoFactorAuthUtils;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Henning Håkonsen
 * @author Morten Svanaes
 */
@Slf4j
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/2fa")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@AllArgsConstructor
public class TwoFactorController {
  private final UserService userService;
  private final TwoFactorAuthService twoFactorAuthService;

  @PostMapping(value = "/enrollTOTP2FA")
  @ResponseStatus(HttpStatus.OK)
  public WebMessage enrollTOTP2FA(@CurrentUser User currentUser, SystemSettings settings)
      throws ConflictException {
    if (!settings.getTOTP2FAEnabled()) {
      throw new ConflictException(ErrorCode.E3046);
    }
    if (currentUser.getTwoFactorType().isEnabled()) {
      throw new ConflictException(ErrorCode.E3022);
    }

    twoFactorAuthService.enrollTOTP2FA(currentUser);

    return ok(
        "The user has enrolled in TOTP 2FA, call the QR code endpoint to continue the process");
  }

  @PostMapping(value = "/enrollEmail2FA")
  @ResponseStatus(HttpStatus.OK)
  public WebMessage enrollEmail2FA(
      @CurrentUser User currentUser, SystemSettings settings, HttpServletRequest request)
      throws ConflictException {
    if (!settings.getEmail2FAEnabled()) {
      throw new ConflictException(ErrorCode.E3045);
    }
    if (currentUser.getTwoFactorType().isEnabled()) {
      throw new ConflictException(ErrorCode.E3022);
    }
    if (!userService.isEmailVerified(currentUser)) {
      throw new ConflictException(ErrorCode.E3043);
    }

    twoFactorAuthService.enrollEmail2FA(currentUser);

    return ok(
        "The user has enrolled in email-based 2FA, a code was generated and sent successfully to the user's email");
  }

  /**
   * Shows the already generated QR code for the user to scan.
   *
   * @throws IOException
   * @throws ConflictException
   */
  @OpenApi.Response(byte[].class)
  @GetMapping(
      value = {"/showQRCodeAsImage"},
      produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void showQRCodeAsImage(
      @CurrentUser User currentUser, HttpServletResponse response, SystemSettings settings)
      throws IOException, ConflictException {
    if (!settings.getTOTP2FAEnabled()) {
      throw new ConflictException(ErrorCode.E3046);
    }
    if (!currentUser.getTwoFactorType().equals(TwoFactorType.ENROLLING_TOTP)) {
      throw new ConflictException(ErrorCode.E3047);
    }

    writeQRCodeToResponse(currentUser, response, settings);
  }

  /**
   * Shows the already generated QR code for the user to scan as a JSON object. Where the QR code is
   * represented as a base64 encoded string. And the secret encoded in Base32.
   *
   * @throws IOException
   * @throws ConflictException
   */
  @GetMapping(
      value = {"/showQRCodeAsJson"},
      produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public QRCode showQRCodeAsText(
      @CurrentUser User currentUser, HttpServletResponse response, SystemSettings settings)
      throws ConflictException {
    if (!settings.getTOTP2FAEnabled()) {
      throw new ConflictException(ErrorCode.E3046);
    }
    if (!currentUser.getTwoFactorType().equals(TwoFactorType.ENROLLING_TOTP)) {
      throw new ConflictException(ErrorCode.E3047);
    }
    byte[] qrCode = generateQRCodeBytes(currentUser, settings);

    return new QRCode(currentUser.getSecret(), Base64.getEncoder().encodeToString(qrCode));
  }

  public record QRCode(@JsonProperty String base32Secret, @JsonProperty String base64QRImage) {}

  /**
   * Enrolls the user in TOTP 2FA and generates a QR code for the user to scan.
   *
   * @throws IOException
   * @throws ConflictException
   */
  @OpenApi.Response(byte[].class)
  @GetMapping(
      value = {"/qrCode"},
      produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Deprecated(forRemoval = true, since = "2.42")
  public void generateQRCode(
      @CurrentUser User currentUser, HttpServletResponse response, SystemSettings settings)
      throws IOException, ConflictException {
    if (!settings.getTOTP2FAEnabled()) {
      throw new ConflictException(ErrorCode.E3044);
    }
    if (currentUser.isTwoFactorEnabled()) {
      throw new ConflictException(ErrorCode.E3022);
    }

    twoFactorAuthService.enrollTOTP2FA(currentUser);

    writeQRCodeToResponse(currentUser, response, settings);
  }

  private static void writeQRCodeToResponse(
      User currentUser, HttpServletResponse response, SystemSettings settings)
      throws IOException, ConflictException {
    byte[] qrCode = generateQRCodeBytes(currentUser, settings);

    response.getOutputStream().write(qrCode);
  }

  private static byte[] generateQRCodeBytes(User currentUser, SystemSettings settings)
      throws ConflictException {
    List<ErrorCode> errorCodes = new ArrayList<>();

    String totpURL =
        TwoFactorAuthUtils.generateTOTP2FAURL(
            settings.getApplicationTitle(),
            currentUser.getSecret(),
            currentUser.getUsername(),
            errorCodes::add);

    if (!errorCodes.isEmpty()) {
      throw new ConflictException(errorCodes.get(0));
    }

    byte[] qrCode = TwoFactorAuthUtils.generateQRCode(totpURL, 200, 200, errorCodes::add);

    if (!errorCodes.isEmpty()) {
      throw new ConflictException(errorCodes.get(0));
    }
    return qrCode;
  }

  @GetMapping(
      value = "/enabled",
      consumes = {"text/*", "application/*"})
  @ResponseStatus(HttpStatus.OK)
  public boolean isEnabled(@CurrentUser(required = true) User currentUser) {
    return currentUser.isTwoFactorEnabled();
  }

  /**
   * Enable 2FA authentication for the current user, if the user is the enrollment mode and the code
   * is valid.
   *
   * @param body The body of the request.
   * @param currentUser This is the user that is currently logged in.
   */
  @PostMapping(
      value = {"/enabled", "/enable"},
      consumes = {"text/*", "application/*"})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage enable(
      @RequestBody Map<String, String> body, @CurrentUser(required = true) User currentUser)
      throws ConflictException, ForbiddenException {
    if (currentUser.isTwoFactorEnabled()) {
      throw new ConflictException(ErrorCode.E3022);
    }
    if (!currentUser.getTwoFactorType().isEnrolling()) {
      throw new ConflictException(ErrorCode.E3029);
    }

    String code = body.get("code");

    if (twoFactorAuthService.isInvalid2FACode(currentUser, code)) {
      log.error("1. Invalid 2FA code for user: {} code: {}", currentUser.getUsername(), code);
      throw new ForbiddenException(ErrorCode.E3023);
    }

    twoFactorAuthService.enable2FA(currentUser, code);

    return ok("Two factor authentication was enabled successfully");
  }

  /**
   * Disable 2FA authentication for the current user.
   *
   * @param body The body of the request.
   * @param currentUser This is the user that is currently logged in.
   */
  @PostMapping(
      value = {"/disabled", "/disable"},
      consumes = {"text/*", "application/*"})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage disable(
      @RequestBody Map<String, String> body, @CurrentUser(required = true) User currentUser)
      throws ForbiddenException, ConflictException {
    if (!currentUser.isTwoFactorEnabled()) {
      throw new ConflictException(ErrorCode.E3031.getMessage());
    }
    if (userService.is2FADisableEndpointLocked(currentUser.getUsername())) {
      throw new ConflictException(ErrorCode.E3042.getMessage());
    }

    String code = body.get("code");

    if (twoFactorAuthService.isInvalid2FACode(currentUser, code)) {
      userService.registerFailed2FADisableAttempt(currentUser.getUsername());
      throw new ForbiddenException(ErrorCode.E3023);
    }

    twoFactorAuthService.disable2FA(currentUser, code);

    return ok("Two factor authentication was disabled successfully");
  }
}
