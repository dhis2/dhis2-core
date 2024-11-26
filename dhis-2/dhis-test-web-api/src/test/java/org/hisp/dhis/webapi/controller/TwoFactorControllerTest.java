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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
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
import java.util.List;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.security.twofa.TwoFactorAuthService;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
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
  @Autowired private SystemSettingsService systemSettingsService;
  @Autowired private MessageSender emailMessageSender;

  @AfterEach
  void tearDown() {
    emailMessageSender.clearMessages();
    systemSettingsService.put("email2FAEnabled", "false");
  }

  @Test
  void testEnrollTOTP2FA()
      throws ChecksumException, NotFoundException, DecodingException, IOException, FormatException {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);

    switchToNewUser(user);

    assertStatus(HttpStatus.OK, POST("/2fa/enrollTOTP2FA"));

    User enrolledUser = userService.getUserByUsername(user.getUsername());
    assertNotNull(enrolledUser.getSecret());
    assertTrue(enrolledUser.getSecret().matches("^[a-zA-Z0-9]{32}$"));
    assertSame(TwoFactorType.ENROLLING_TOTP, enrolledUser.getTwoFactorType());

    HttpResponse res = GET("/2fa/showQRCodeAsJson");
    assertStatus(HttpStatus.OK, res);
    assertNotNull(res.content());

    JsonMixed content = res.content();
    String base32Secret = content.getString("base32Secret").string();
    String base64QRImage = content.getString("base64QRImage").string();

    String codeFromQR = decodeBase64QRAndExtractBase32Secret(base64QRImage);

    assertEquals(base32Secret, codeFromQR);

    String code = new Totp(base32Secret).now();
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
  }

  private String decodeBase64QRAndExtractBase32Secret(String base64QRCode)
      throws IOException, NotFoundException, ChecksumException, FormatException, DecodingException {

    byte[] imageBytes = Base64.getDecoder().decode(base64QRCode);

    // Read the image from the decoded bytes
    BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
    assertNotNull(qrImage, "QR image could not be loaded");

    // Decode QR code
    String qrCodeContent = decodeQRCode(qrImage);
    assertNotNull(qrCodeContent, "QR code content could not be decoded");

    // content look like this: otpauth://totp/username?secret=base32secret&issuer=issuer
    String secret =
        qrCodeContent.substring(qrCodeContent.indexOf("?") + 8, qrCodeContent.indexOf("&"));

    // Extract the Base32-encoded secret bytes, check that it is 20 bytes long, which is in 160 bits
    // of entropy, which is the RFC 4226 recommendation.
    byte[] decodedSecretBytes = Base32.decode(secret);
    assertEquals(20, decodedSecretBytes.length);

    return secret;
  }

  private String decodeQRCode(BufferedImage qrImage)
      throws ChecksumException, NotFoundException, FormatException {
    // Convert the BufferedImage to a ZXing binary bitmap source
    LuminanceSource source = new BufferedImageLuminanceSource(qrImage);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

    // Use the QRCodeReader to decode the QR code
    QRCodeReader qrCodeReader = new QRCodeReader();
    Result result = qrCodeReader.decode(bitmap);

    return result.getText();
  }

  @Test
  void testEnrollEmail2FA() {
    systemSettingsService.put("email2FAEnabled", "true");

    User user = makeUser("X", List.of("TEST"));
    String emailAddress = "valid.x@email.com";
    user.setEmail(emailAddress);
    user.setVerifiedEmail(emailAddress);
    userService.addUser(user);

    switchToNewUser(user);

    assertStatus(HttpStatus.OK, POST("/2fa/enrollEmail2FA"));

    User enrolledUser = userService.getUserByUsername(user.getUsername());
    assertNotNull(enrolledUser.getSecret());
    assertTrue(enrolledUser.getSecret().matches("^[0-9]{6}\\|\\d+$"));
    assertSame(TwoFactorType.ENROLLING_EMAIL, enrolledUser.getTwoFactorType());

    List<OutboundMessage> messagesByEmail = emailMessageSender.getMessagesByEmail(emailAddress);
    for (OutboundMessage message : messagesByEmail) {
      log.error("message: " + message.getText());
    }
    assertFalse(messagesByEmail.isEmpty());
    String email = messagesByEmail.get(0).getText();
    String code = email.substring(email.indexOf("code:\n") + 6, email.indexOf("code:\n") + 12);

    log.error("email: " + email);

    List<User> allUsers = userService.getAllUsers();
    for (User allUser : allUsers) {
      log.error("User: " + allUser.getUsername() + " - " + allUser.getSecret());
    }

    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
  }

  @Test
  void testEnableTOTP2FA() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    twoFactorAuthService.enrollTOTP2FA(user);

    switchToNewUser(user);

    String code = new Totp(user.getSecret()).now();
    assertStatus(HttpStatus.OK, POST("/2fa/enable", "{'code':'" + code + "'}"));
  }

  @Test
  void testEnableEmail2FA() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    user.setVerifiedEmail("valid.x@email.com");
    userService.addUser(user);
    twoFactorAuthService.enrollEmail2FA(user);

    switchToNewUser(user);

    User enrolledUser = userService.getUserByUsername(user.getUsername());
    String secret = enrolledUser.getSecret();
    assertNotNull(secret);
    String[] codeAndTTL = secret.split("\\|");
    String code = codeAndTTL[0];

    assertStatus(HttpStatus.OK, POST("/2fa/enabled", "{'code':'" + code + "'}"));
  }

  @Test
  void testEnableTOTP2FAWrongCode() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    twoFactorAuthService.enrollTOTP2FA(user);

    switchToNewUser(user);

    assertEquals(
        "Invalid 2FA code",
        POST("/2fa/enabled", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testQr2FAConflictMustDisableFirst() {
    assertNull(getCurrentUser().getSecret());

    User user = userService.getUser(CurrentUserUtil.getCurrentUserDetails().getUid());
    twoFactorAuthService.enrollTOTP2FA(user);

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
  void testDisableTOTP2FA() {
    User newUser = makeUser("Y", List.of("TEST"));
    newUser.setEmail("valid.y@email.com");

    userService.addUser(newUser);
    twoFactorAuthService.enrollTOTP2FA(newUser);
    twoFactorAuthService.setEnabled2FA(newUser, new SystemUser());

    switchToNewUser(newUser);

    String code = new Totp(newUser.getSecret()).now();

    assertStatus(HttpStatus.OK, POST("/2fa/disable", "{'code':'" + code + "'}"));
  }

  @Test
  void testDisableEmail2FA() {
    User newUser = makeUser("Y", List.of("TEST"));
    newUser.setEmail("valid.y@email.com");
    newUser.setVerifiedEmail("valid.y@email.com");

    userService.addUser(newUser);
    twoFactorAuthService.enrollEmail2FA(newUser);
    twoFactorAuthService.setEnabled2FA(newUser, new SystemUser());

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
        "Two factor authentication is not enabled",
        POST("/2fa/disable", "{'code':'wrong'}")
            .error(HttpStatus.Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testDisable2FATooManyTimes() {
    User user = makeUser("X", List.of("TEST"));
    user.setEmail("valid.x@email.com");
    userService.addUser(user);
    twoFactorAuthService.enrollTOTP2FA(user);

    switchToNewUser(user);

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
}
