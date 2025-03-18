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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
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
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
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
  private final TwoFactorAuthService twoFactorAuthService;

  @PostMapping(value = "/enrollTOTP2FA")
  @ResponseStatus(HttpStatus.OK)
  public WebMessage enrollTOTP2FA(@CurrentUser User currentUser) throws ConflictException {
    twoFactorAuthService.enrollTOTP2FA(currentUser.getUsername());
    return ok(
        "The user has enrolled in TOTP 2FA, call the QR code endpoint to continue the process");
  }

  @PostMapping(value = "/enrollEmail2FA")
  @ResponseStatus(HttpStatus.OK)
  public WebMessage enrollEmail2FA(@CurrentUser User currentUser) throws ConflictException {
    twoFactorAuthService.enrollEmail2FA(currentUser.getUsername());
    return ok(
        "The user has enrolled in email-based 2FA, a code was generated and sent successfully to the user's email");
  }

  /**
   * Returns generated QR code for the user to scan.
   *
   * @throws IOException
   * @throws ConflictException
   */
  @OpenApi.Response(byte[].class)
  @GetMapping(
      value = {"/qrCodePng"},
      produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void qrCodePng(@CurrentUser User currentUser, HttpServletResponse response)
      throws IOException, ConflictException {
    byte[] qrCode = twoFactorAuthService.generateQRCode(currentUser);
    response.getOutputStream().write(qrCode);
  }

  /**
   * Shows the generated QR code for the user to scan as a JSON object. Where the QR code (PNG
   * image) is represented as a base64 encoded string. And the secret encoded in Base32.
   *
   * @throws ConflictException
   */
  @GetMapping(
      value = {"/qrCodeJson"},
      produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public QRCode qrCodeJson(@CurrentUser User currentUser) throws ConflictException {
    byte[] qrCode = twoFactorAuthService.generateQRCode(currentUser);
    return new QRCode(currentUser.getSecret(), Base64.getEncoder().encodeToString(qrCode));
  }

  public record QRCode(@JsonProperty String base32Secret, @JsonProperty String base64QRImage) {}

  /**
   * Enrolls the user in TOTP 2FA and generates a QR code for the user to scan.
   *
   * @throws IOException The QR code could not be generated.
   * @throws ConflictException The user is already enrolled in 2FA.
   */
  @OpenApi.Response(byte[].class)
  @GetMapping(
      value = {"/qrCode"},
      produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Deprecated(forRemoval = true, since = "2.42")
  public void generateQRCode(@CurrentUser User currentUser, HttpServletResponse response)
      throws IOException, ConflictException {
    twoFactorAuthService.enrollTOTP2FA(currentUser.getUsername());
    byte[] qrCode = twoFactorAuthService.generateQRCode(currentUser);
    response.getOutputStream().write(qrCode);
  }

  @GetMapping(
      value = "/enabled",
      consumes = {"text/*", "application/*"})
  @ResponseStatus(HttpStatus.OK)
  public boolean isEnabled(@CurrentUser(required = true) User currentUser) {
    return currentUser.isTwoFactorEnabled();
  }

  /**
   * Enable 2FA authentication for the current user if the user is the enrollment mode and the code
   * is valid.
   *
   * @param body The body of the request.
   * @param currentUser The user currently logged in.
   */
  @PostMapping(
      value = {"/enabled", "/enable"},
      consumes = {"text/*", "application/*"})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage enable(
      @RequestBody Map<String, String> body, @CurrentUser(required = true) UserDetails currentUser)
      throws ForbiddenException, ConflictException {
    String code = body.get("code");
    if (Strings.isNullOrEmpty(code)) {
      throw new ConflictException(ErrorCode.E3050);
    }
    twoFactorAuthService.enable2FA(currentUser.getUsername(), code, currentUser);
    return ok("2FA was enabled successfully");
  }

  /**
   * Disable 2FA authentication for the current user.
   *
   * @param body The body of the request.
   * @param currentUser The user currently logged in.
   */
  @PostMapping(
      value = {"/disabled", "/disable"},
      consumes = {"text/*", "application/*"})
  @ResponseStatus(HttpStatus.OK)
  public WebMessage disable(
      @RequestBody Map<String, String> body, @CurrentUser(required = true) UserDetails currentUser)
      throws ForbiddenException, ConflictException {
    String code = body.get("code");
    twoFactorAuthService.disable2FA(currentUser.getUsername(), code);
    return ok("2FA was disabled successfully");
  }
}
