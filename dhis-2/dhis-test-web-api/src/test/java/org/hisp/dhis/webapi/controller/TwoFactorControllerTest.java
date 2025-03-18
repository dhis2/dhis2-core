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

import static org.hisp.dhis.external.conf.ConfigurationKey.EMAIL_2FA_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.TOTP_2FA_ENABLED;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.security.twofa.TwoFactorAuthService;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.security.TwoFactorController;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.jboss.aerogear.security.otp.api.Base32.DecodingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link TwoFactorController}
 *
 * @author Jan Bernitt
 * @author Morten Svanæs
 */
@Slf4j
@Transactional
class TwoFactorControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private TwoFactorAuthService twoFactorAuthService;
  @Autowired private MessageSender emailMessageSender;
  @Autowired private DhisConfigurationProvider config;

  @AfterEach
  void tearDown() {
    emailMessageSender.clearMessages();
    config.getProperties().put(EMAIL_2FA_ENABLED.getKey(), "off");
    config.getProperties().put(TOTP_2FA_ENABLED.getKey(), "on");
  }

  @Test
  void testEnrollTOTP2FA()
      throws ChecksumException, NotFoundException, DecodingException, IOException, FormatException {
    User user = createUserAndInjectSecurityContext(false);

    assertStatus(HttpStatus.OK, POST("/2fa/enrollTOTP2FA"));
    User enrolledUser = userService.getUserByUsername(user.getUsername());
    assertNotNull(enrolledUser.getSecret());
    assertTrue(enrolledUser.getSecret().matches("^[a-zA-Z0-9]{32}$"));
    assertSame(TwoFactorType.ENROLLING_TOTP, enrolledUser.getTwoFactorType());

    HttpResponse res = GET("/2fa/qrCodeJson");
    assertStatus(HttpStatus.OK, res);
    assertNotNull(res.content());
    JsonMixed content = res.content();
    String base32Secret = content.getString("base32Secret").string();
    String base64QRImage = content.getString("base64QRImage").string();
    assertNotNull(base32Secret, "Base32 secret is null");
    assertFalse(base32Secret.isBlank(), "Base32 secret is blank");
    String codeFromQR = decodeBase64QRAndExtractBase32Secret(base64QRImage);
    assertEquals(base32Secret, codeFromQR);

    String code = new Totp(base32Secret).now();
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
    User enabledUser = userService.getUserByUsername(user.getUsername());
    assertSame(TwoFactorType.TOTP_ENABLED, enabledUser.getTwoFactorType());
  }

  private String decodeBase64QRAndExtractBase32Secret(String base64QRCode)
      throws IOException, NotFoundException, ChecksumException, FormatException, DecodingException {
    // Decode the base64 encoded QR code (PNG image)
    byte[] imageBytes = Base64.getDecoder().decode(base64QRCode);
    // Create the image object from the byte array
    BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
    assertNotNull(qrImage, "QR image could not be loaded");

    // Decode QR code URL from the image
    String qrCodeContent = decodeQRCode(qrImage);
    assertNotNull(qrCodeContent, "QR code content could not be decoded");

    // content looks like this: otpauth://totp/username?secret=base32secret&issuer=issuer
    String secret =
        qrCodeContent.substring(qrCodeContent.indexOf("?") + 8, qrCodeContent.indexOf("&"));

    // Extract the Base32-encoded secret bytes, check that it is 20 bytes long, which is 160 bits
    // of entropy, which is the RFC 4226 recommendation.
    byte[] decodedSecretBytes = Base32.decode(secret);
    assertEquals(20, decodedSecretBytes.length);

    return secret;
  }

  private String decodeQRCode(BufferedImage qrImage)
      throws ChecksumException, NotFoundException, FormatException {
    // Convert the BufferedImage to a ZXing binary bitmap source
    LuminanceSource source = new BufferedImageLuminanceSource(qrImage);
    assertNotNull(source, "QR image could not be converted to luminance source");
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    assertNotNull(bitmap, "QR image could not be converted to binary bitmap");
    // Use the ZXing's QRCodeReader to decode the QR code image
    QRCodeReader reader = new QRCodeReader();
    Map<DecodeHintType, Object> tmpHintsMap =
        new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
    tmpHintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    tmpHintsMap.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
    tmpHintsMap.put(DecodeHintType.PURE_BARCODE, Boolean.FALSE);
    Result code = reader.decode(bitmap, tmpHintsMap);
    return code.getText();
  }

  @Test
  void testEnrollEmail2FA() {
    config.getProperties().put(EMAIL_2FA_ENABLED.getKey(), "on");

    User user = createUserAndInjectSecurityContext(false);
    String emailAddress = "valid.x@email.com";
    user.setEmail(emailAddress);
    user.setVerifiedEmail(emailAddress);
    userService.encodeAndSetPassword(user, "Test123###...");

    assertStatus(HttpStatus.OK, POST("/2fa/enrollEmail2FA"));
    User enrolledUser = userService.getUserByUsername(user.getUsername());
    assertNotNull(enrolledUser.getSecret());
    assertTrue(enrolledUser.getSecret().matches("^[0-9]{6}\\|\\d+$"));
    assertSame(TwoFactorType.ENROLLING_EMAIL, enrolledUser.getTwoFactorType());

    List<OutboundMessage> messagesByEmail = emailMessageSender.getMessagesByEmail(emailAddress);
    assertFalse(messagesByEmail.isEmpty());
    String email = messagesByEmail.get(0).getText();

    // Extract the 6-digit code from the email
    String code = email.substring(email.indexOf("code:\n") + 6, email.indexOf("code:\n") + 12);
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
    User enabledUser = userService.getUserByUsername(user.getUsername());
    assertSame(TwoFactorType.EMAIL_ENABLED, enabledUser.getTwoFactorType());
  }

  @Test
  void testEnableTOTP2FA() throws ConflictException {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    twoFactorAuthService.enrollTOTP2FA(user.getUsername());
    switchToNewUser(user);

    String code = new Totp(user.getSecret()).now();
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
    User enabledUser = userService.getUserByUsername(user.getUsername());
    assertSame(TwoFactorType.TOTP_ENABLED, enabledUser.getTwoFactorType());
  }

  @Test
  void testEnableEmail2FA() throws ConflictException {
    config.getProperties().put(EMAIL_2FA_ENABLED.getKey(), "on");

    User user = createUserAndInjectSecurityContext(false);
    user.setEmail("valid.x@email.com");
    user.setVerifiedEmail("valid.x@email.com");
    userService.encodeAndSetPassword(user, "Test123###...");

    twoFactorAuthService.enrollEmail2FA(user.getUsername());

    User enrolledUser = userService.getUserByUsername(user.getUsername());
    String secret = enrolledUser.getSecret();
    assertNotNull(secret);
    String[] codeAndTTL = secret.split("\\|");
    // Check that the secret is a 6-digit code followed by a TTL in milliseconds
    assertTrue(codeAndTTL[0].matches("^[0-9]{6}$"));
    assertTrue(codeAndTTL[1].matches("^\\d+$"));

    String code = codeAndTTL[0];
    assertStatus(HttpStatus.OK, POST("/2fa/enabled", "{'code':'" + code + "'}"));
    User enabledUser = userService.getUserByUsername(user.getUsername());
    assertSame(TwoFactorType.EMAIL_ENABLED, enabledUser.getTwoFactorType());
  }

  @Test
  void testEnableTOTP2FAWrongCode() throws ConflictException {
    User user = createUserAndInjectSecurityContext(false);
    twoFactorAuthService.enrollTOTP2FA(user.getUsername());

    assertEquals(
        "Invalid 2FA code",
        POST("/2fa/enabled", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testQr2FAConflictMustDisableFirst() throws ConflictException {
    assertNull(getCurrentUser().getSecret());

    User user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    twoFactorAuthService.enrollTOTP2FA(user.getUsername());
    user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    assertNotNull(user.getSecret());

    String code = new Totp(user.getSecret()).now();
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));

    user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    assertNotNull(user.getSecret());
  }

  @Test
  void testEnable2FANotEnrolledFirst() {
    assertEquals(
        "User must start 2FA enrollment first",
        POST("/2fa/enable", "{'code':'wrong'}").error(HttpStatus.Series.CLIENT_ERROR).getMessage());
  }

  @Test
  void testDisableTOTP2FA() throws ConflictException {
    User user = createUserAndInjectSecurityContext(false);
    twoFactorAuthService.enrollTOTP2FA(user.getUsername());

    user = userService.getUserByUsername(user.getUsername());
    user.setTwoFactorType(user.getTwoFactorType().getEnabledType());
    userService.updateUser(user, new SystemUser());

    switchToNewUser(user);

    String code = new Totp(user.getSecret()).now();
    assertStatus(HttpStatus.OK, POST("/2fa/disable", "{'code':'" + code + "'}"));
  }

  @Test
  void testDisableEmail2FA() throws ConflictException {
    config.getProperties().put(EMAIL_2FA_ENABLED.getKey(), "on");

    User newUser = makeUser("Y", List.of("TEST"));
    newUser.setEmail("valid.y@email.com");
    newUser.setVerifiedEmail("valid.y@email.com");
    userService.addUser(newUser);
    twoFactorAuthService.enrollEmail2FA(newUser.getUsername());

    User user = userService.getUserByUsername(newUser.getUsername());
    user.setTwoFactorType(user.getTwoFactorType().getEnabledType());
    userService.updateUser(user, new SystemUser());

    switchToNewUser(newUser);

    User enabledUser = userService.getUserByUsername(newUser.getUsername());
    String secretAndTTL = enabledUser.getSecret();
    String code = secretAndTTL.split("\\|")[0];
    assertStatus(HttpStatus.OK, POST("/2fa/disable", "{'code':'" + code + "'}"));

    User disabledUser = userService.getUserByUsername(newUser.getUsername());
    assertNull(disabledUser.getSecret());
  }

  @Test
  void testDisable2FANotEnabled() {
    assertEquals(
        "User has not enabled 2FA",
        POST("/2fa/disable", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testDisable2FATooManyTimes() throws ConflictException {
    User user = createUserAndInjectSecurityContext(false);
    userService.addUser(user);
    twoFactorAuthService.enrollTOTP2FA(user.getUsername());

    String code = new Totp(user.getSecret()).now();
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
    assertStatus(HttpStatus.FORBIDDEN, POST("/2fa/disable", "{'code':'333333'}"));

    for (int i = 0; i < 3; i++) {
      assertWebMessage(
          "Forbidden",
          403,
          "ERROR",
          "Invalid 2FA code",
          POST("/2fa/disable", "{'code':'333333'}").content(HttpStatus.FORBIDDEN));
    }

    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Too many failed disable attempts. Please try again later",
        POST("/2fa/disable", "{'code':'333333'}").content(HttpStatus.CONFLICT));
  }

  @Test
  void testEnrollTOTP2FAWhenDisabled() {
    // Given TOTP 2FA is disabled in config
    config.getProperties().put(TOTP_2FA_ENABLED.getKey(), "off");

    User user = createUserAndInjectSecurityContext(false);

    // When trying to enroll
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "TOTP 2FA is not enabled",
        POST("/2fa/enrollTOTP2FA").content(HttpStatus.CONFLICT));

    // Then user should not be enrolled
    User updatedUser = userService.getUserByUsername(user.getUsername());
    assertEquals(TwoFactorType.NOT_ENABLED, updatedUser.getTwoFactorType());
  }

  @Test
  void testEnrollEmail2FAWhenDisabled() {
    // Given Email 2FA is disabled in config
    config.getProperties().put(EMAIL_2FA_ENABLED.getKey(), "off");

    User user = createUserAndInjectSecurityContext(false);
    user.setEmail("valid.x@email.com");
    user.setVerifiedEmail("valid.x@email.com");
    userService.encodeAndSetPassword(user, "Test123###...");

    // When trying to enroll
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Email based 2FA is not enabled",
        POST("/2fa/enrollEmail2FA").content(HttpStatus.CONFLICT));

    // Then user should not be enrolled
    User updatedUser = userService.getUserByUsername(user.getUsername());
    assertNull(updatedUser.getSecret());
    assertEquals(TwoFactorType.NOT_ENABLED, updatedUser.getTwoFactorType());
  }

  @Test
  void testLoginWithTOTP2FAWhenDisabled() {
    // Given user has TOTP 2FA enabled
    config.getProperties().put(TOTP_2FA_ENABLED.getKey(), "on");

    User user = createUserAndInjectSecurityContext(false);
    userService.encodeAndSetPassword(user, "Test123###...");

    assertStatus(HttpStatus.OK, POST("/2fa/enrollTOTP2FA"));
    String code = new Totp(user.getSecret()).now();
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
    assertEquals(TwoFactorType.TOTP_ENABLED, user.getTwoFactorType());

    // When TOTP 2FA is disabled in config
    config.getProperties().put(TOTP_2FA_ENABLED.getKey(), "off");

    // Then user should be able to log in without 2FA code
    assertStatus(
        HttpStatus.OK,
        POST(
            "/auth/login", "{'username':'" + user.getUsername() + "','password':'Test123###...'}"));
  }

  @Test
  void testLoginWithEmail2FAWhenDisabled() {
    // Given user has Email 2FA enabled
    config.getProperties().put(EMAIL_2FA_ENABLED.getKey(), "on");

    User user = createUserAndInjectSecurityContext(false);
    user.setEmail("valid.x@email.com");
    user.setVerifiedEmail("valid.x@email.com");
    userService.encodeAndSetPassword(user, "Test123###...");

    assertStatus(HttpStatus.OK, POST("/2fa/enrollEmail2FA"));
    User enrolledUser = userService.getUserByUsername(user.getUsername());
    String code = enrolledUser.getSecret().split("\\|")[0];
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
    assertEquals(TwoFactorType.EMAIL_ENABLED, user.getTwoFactorType());

    // When Email 2FA is disabled in config
    config.getProperties().put(EMAIL_2FA_ENABLED.getKey(), "off");

    // Then user should be able to log in without 2FA code
    assertStatus(
        HttpStatus.OK,
        POST(
            "/auth/login", "{'username':'" + user.getUsername() + "','password':'Test123###...'}"));
  }

  @Test
  void testDisable2FAWhenTypeDisabled() {
    // Given user has TOTP 2FA enabled
    config.getProperties().put(TOTP_2FA_ENABLED.getKey(), "on");

    User user = createUserAndInjectSecurityContext(false);

    assertStatus(HttpStatus.OK, POST("/2fa/enrollTOTP2FA"));
    String code = new Totp(user.getSecret()).now();
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
    assertEquals(TwoFactorType.TOTP_ENABLED, user.getTwoFactorType());

    // When TOTP 2FA is disabled in config
    config.getProperties().put(TOTP_2FA_ENABLED.getKey(), "off");

    // Then user should still be able to disable their 2FA
    assertStatus(HttpStatus.OK, POST("/2fa/disable", "{'code':'" + code + "'}"));

    User updatedUser = userService.getUserByUsername(user.getUsername());
    assertNull(updatedUser.getSecret());
    assertEquals(TwoFactorType.NOT_ENABLED, updatedUser.getTwoFactorType());
  }

  @Test
  void testChangeEmailWhenEmail2FAIsEnabledWithMeEndpoint() {
    // Given user has Email 2FA enabled
    config.getProperties().put(EMAIL_2FA_ENABLED.getKey(), "on");

    User user = createUserAndInjectSecurityContext(false);
    user.setEmail("valid.x@email.com");
    user.setVerifiedEmail("valid.x@email.com");
    userService.encodeAndSetPassword(user, "Test123###...");

    assertStatus(HttpStatus.OK, POST("/2fa/enrollEmail2FA"));
    User enrolledUser = userService.getUserByUsername(user.getUsername());
    String code = enrolledUser.getSecret().split("\\|")[0];
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
    assertEquals(TwoFactorType.EMAIL_ENABLED, user.getTwoFactorType());

    // When trying to change email
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Email address cannot be changed, when email-based 2FA is enabled, please disable 2FA first",
        PUT("/me", "{'email':'another@email.com'}").content(HttpStatus.CONFLICT));
  }

  @Test
  void testChangeEmailWhenEmail2FAIsEnabledWithUserEndpoint() {
    // Given user has Email 2FA enabled
    config.getProperties().put(EMAIL_2FA_ENABLED.getKey(), "on");

    User user = createUserAndInjectSecurityContext(false);
    user.setEmail("valid.x@email.com");
    user.setVerifiedEmail("valid.x@email.com");
    userService.encodeAndSetPassword(user, "Test123###...");

    assertStatus(HttpStatus.OK, POST("/2fa/enrollEmail2FA"));
    User enrolledUser = userService.getUserByUsername(user.getUsername());
    String code = enrolledUser.getSecret().split("\\|")[0];
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
    assertEquals(TwoFactorType.EMAIL_ENABLED, user.getTwoFactorType());

    switchContextToUser(getAdminUser());

    // When trying to change email
    JsonObject jsonUser = GET("/users/{id}", user.getUid()).content();
    String jsonUserString = jsonUser.toString();
    jsonUserString = jsonUserString.replace("valid.x@email.com", "another.email@com");

    JsonWebMessage msg =
        assertWebMessage(
            "Conflict",
            409,
            "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT("/users/" + user.getUid(), jsonUserString).content(HttpStatus.CONFLICT));

    msg.getResponse().find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E3052);
  }
}
