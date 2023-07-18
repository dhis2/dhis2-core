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
package org.hisp.dhis.security;

import static org.hisp.dhis.feedback.ErrorCode.E3026;
import static org.hisp.dhis.feedback.ErrorCode.E3028;
import static org.hisp.dhis.user.UserService.TWO_FACTOR_CODE_APPROVAL_PREFIX;

import com.google.common.base.Strings;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.LongValidator;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.user.User;
import org.jboss.aerogear.security.otp.Totp;

/**
 * @author Henning Håkonsen
 * @author Morten Svanæs
 */
@Slf4j
public class TwoFactoryAuthenticationUtils {
  private TwoFactoryAuthenticationUtils() {
    throw new IllegalStateException("Utility class");
  }

  private static final String APP_NAME_PREFIX = "DHIS 2 ";

  /**
   * Generate QR code in PNG format based on given qrContent.
   *
   * @param qrContent content to be used for generating the QR code.
   * @param width width of the generated PNG image.
   * @param height height of the generated PNG image.
   * @return PNG image as byte array.
   */
  public static byte[] generateQRCode(
      String qrContent, int width, int height, Consumer<ErrorCode> errorCode) {
    try {
      BitMatrix bitMatrix =
          new MultiFormatWriter()
              .encode(
                  new String(qrContent.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                  BarcodeFormat.QR_CODE,
                  width,
                  height);

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (WriterException | IOException e) {
      log.error(e.getMessage(), e);
      errorCode.accept(E3026);
      return ArrayUtils.EMPTY_BYTE_ARRAY;
    }
  }

  /**
   * Generate QR content based on given appName and {@link User}
   *
   * @param appName app name to be used for generating QR content.
   * @param user {@link User} which the QR Code is generated for.
   * @return a String which can be used for generating a QR code by calling method {@link
   *     TwoFactoryAuthenticationUtils#generateQRCode(String, int, int, Consumer)}
   */
  public static String generateQrContent(String appName, User user, Consumer<ErrorCode> errorCode) {
    String secret = user.getSecret();

    if (Strings.isNullOrEmpty(secret)) {
      errorCode.accept(E3028);
    }

    secret = removeApprovalPrefix(secret);

    String app = (APP_NAME_PREFIX + StringUtils.stripToEmpty(appName)).replace(" ", "%20");

    return String.format(
        "otpauth://totp/%s:%s?secret=%s&issuer=%s", app, user.getUsername(), secret, app);
  }

  /**
   * Verifies that the secret for the given user matches the given code.
   *
   * @param code the code.
   * @param secret
   * @return true if the user secret matches the given code, false if not.
   */
  public static boolean verify(String code, String secret) {
    if (Strings.isNullOrEmpty(secret)) {
      throw new IllegalArgumentException("User must have a secret");
    }

    if (!LongValidator.getInstance().isValid(code)) {
      return false;
    }

    secret = removeApprovalPrefix(secret);

    Totp totp = new Totp(secret);
    try {
      return totp.verify(code);
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  private static String removeApprovalPrefix(String secret) {
    if (secret.startsWith(TWO_FACTOR_CODE_APPROVAL_PREFIX)) {
      secret = secret.substring(TWO_FACTOR_CODE_APPROVAL_PREFIX.length());
    }
    return secret;
  }
}
