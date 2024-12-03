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
package org.hisp.dhis.security.twofa;

import static org.hisp.dhis.feedback.ErrorCode.E3026;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
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
public class TwoFactorAuthUtils {
  private TwoFactorAuthUtils() {
    throw new IllegalStateException("Utility class");
  }

  private static final String APP_NAME_PREFIX = "DHIS 2 ";
  private static final Pattern PIPE_SPLIT_PATTERN = Pattern.compile("\\|");

  /**
   * Generate QR code in PNG format based on given qrContent.
   *
   * @param qrContent content to be used for generating the QR code.
   * @param width width of the generated PNG image.
   * @param height height of the generated PNG image.
   * @return PNG image as a byte array.
   */
  public static byte[] generateQRCode(
      @Nonnull String qrContent, int width, int height, @Nonnull Consumer<ErrorCode> errorCode) {
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
      log.warn(e.getMessage(), e);
      errorCode.accept(E3026);
      return ArrayUtils.EMPTY_BYTE_ARRAY;
    }
  }

  /**
   * Generate TOTP URL-based appName and {@link User}, this is used as input to the TOTP generator.
   */
  public static String generateTOTP2FAURL(
      @Nonnull String appName, @Nonnull String secret, @Nonnull String username) {
    String app = (APP_NAME_PREFIX + StringUtils.stripToEmpty(appName)).replace(" ", "%20");
    return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", app, username, secret, app);
  }

  /**
   * Validate the 2FA code based on the given type.
   *
   * @param type {@link TwoFactorType}
   * @param code 2FA code
   * @param secret 2FA secret
   * @return true if the code is valid, false otherwise.
   */
  public static boolean isValid2FACode(
      @Nonnull TwoFactorType type, @Nonnull String code, @Nonnull String secret) {
    if (TwoFactorType.TOTP == type || TwoFactorType.ENROLLING_TOTP == type) {
      return TwoFactorAuthUtils.verifyTOTP2FACode(code, secret);
    } else if (TwoFactorType.EMAIL == type || TwoFactorType.ENROLLING_EMAIL == type) {
      return TwoFactorAuthUtils.verifyEmail2FACode(code, secret);
    }
    return false;
  }

  /**
   * Verify the email based2FA code.
   *
   * @param code 2FA code
   * @param secretAndTTL secret and TTL string separated by a pipe character.
   * @return true if the code is valid, false otherwise.
   */
  public static boolean verifyEmail2FACode(@Nonnull String code, @Nonnull String secretAndTTL) {
    String[] parts = PIPE_SPLIT_PATTERN.split(secretAndTTL);
    String secret = parts[0];
    long ttl = Long.parseLong(parts[1]);
    if (ttl < System.currentTimeMillis()) {
      return false;
    }
    return code.equals(secret);
  }

  /**
   * Verify the TOTP 2FA code.
   *
   * @param code 2FA code
   * @param secret 2FA secret
   * @return true if the code is valid, false otherwise.
   */
  public static boolean verifyTOTP2FACode(@Nonnull String code, @Nonnull String secret) {
    if (!LongValidator.getInstance().isValid(code)) {
      return false;
    }
    Totp totp = new Totp(secret);
    return totp.verify(code);
  }
}
