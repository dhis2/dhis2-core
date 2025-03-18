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
package org.hisp.dhis.common;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * @author bobj
 */
public class CodeGenerator {

  private CodeGenerator() {
    throw new IllegalStateException("Utility class");
  }

  /*
   * The secure random number generator used by this class to create secure
   * random-based codes. It is in a holder class to defer initialization until
   * needed.
   */
  public static class SecureRandomHolder {
    static final SecureRandom GENERATOR = new SecureRandom();
  }

  public static final String NUMERIC_CHARS = "0123456789";

  public static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  public static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";

  public static final String LETTERS = LOWERCASE_LETTERS + UPPERCASE_LETTERS;

  public static final String ALPHANUMERIC_CHARS = NUMERIC_CHARS + LETTERS;

  public static final int UID_CODE_SIZE = 11;

  public static final String UID_REGEXP = "^[a-zA-Z][a-zA-Z0-9]{10}$";

  public static final Pattern UID_PATTERN = Pattern.compile(UID_REGEXP);

  /**
   * The minimum length of a random alphanumeric string, with the first character always being a
   * letter. We want to have at least 256 bits of entropy.
   *
   * <p>Alphanumeric char = log2(62) = 5.95
   *
   * <p>Letter only char = log2(52) = 5.7
   *
   * <p>256 - 5.7 (1st char) / 5.95 bits ≈ 42.1 characters ≈ 43 characters + 1 (1st char) = 44
   * characters
   *
   * <p>We add one extra character to ensure we have 256 bits of entropy.
   */
  public static final int SECURE_RANDOM_TOKEN_MIN_SIZE = 44;

  /**
   * Generates a string of random alphanumeric characters to the following rules:
   *
   * <ul>
   *   <li>Alphanumeric characters only.
   *   <li>First character is alphabetic.
   * </ul>
   *
   * @return a code.
   */
  private static char[] generateRandomAlphanumericCode(int codeSize, java.util.Random r) {
    char[] randomChars = new char[codeSize];

    // First char should be a letter
    randomChars[0] = LETTERS.charAt(r.nextInt(LETTERS.length()));

    for (int i = 1; i < codeSize; ++i) {
      randomChars[i] = ALPHANUMERIC_CHARS.charAt(r.nextInt(ALPHANUMERIC_CHARS.length()));
    }

    return randomChars;
  }

  /**
   * Generates a string of random alphanumeric characters. Uses a {@link ThreadLocalRandom} instance
   * and is considered non-secure and should not be used for security purposes.
   *
   * @param codeSize the number of characters in the code.
   * @return the code.
   */
  public static String generateCode(int codeSize) {
    ThreadLocalRandom r = ThreadLocalRandom.current();
    return new String(generateRandomAlphanumericCode(codeSize, r));
  }

  /**
   * Generates a UID according to the following rules:
   *
   * <ul>
   *   <li>Alphanumeric characters only.
   *   <li>Exactly 11 characters long.
   *   <li>First character is alphabetic.
   * </ul>
   *
   * @return a UID.
   */
  public static String generateUid() {
    return generateCode(UID_CODE_SIZE);
  }

  /**
   * Generates a UID from a timestamp. This is only meant to be used to "fake" the presence of an
   * independent UID for backwards compatibility when the actual unique value is a timestamp.
   *
   * <p>The algorithm uses the lowest 8 bit as a selector to shift through the letter alphabets so
   * that even small increments in time result in completely different UIDs. The rest of the bits
   * are used in groups of 5 bits for each of the 11 characters as an offset into the 26 letters or
   * 10 digits alphabet. Since 5 bits give 0-31 the potential overflow is remembered in the carry to
   * influence later characters.
   *
   * @param timestamp a UNIX timestamp
   * @return a UID that is likely to be unique for the timestamp
   */
  public static String generateUid(long timestamp) {
    int low8 = (int) (timestamp & 0xFF); // 8 bits
    // 11 x 5 bits to get a number 0-31 => 55 bits
    // = 63 bits
    long v = timestamp >> 8; // cut off 8 low bits
    char[] uid = new char[11];
    char c0 = 'a';
    int carry = 0;
    for (int i = 0; i < 11; i++) {
      int offset = low8 & (1 << (i % 8));
      int index = (int) (v & 0b11111);
      index += offset;
      index ^= carry;
      if (offset % 2 == 0) {
        c0 =
            switch (c0) {
              case 'a' -> 'A';
              case 'A' -> '0';
              default -> 'a';
            };
      }
      int n = c0 == '0' ? 10 : 26;
      char c = (char) (c0 + (index % n));
      uid[i] = c;
      v >>= 5; // move to next 5 bits
      carry = Math.max(0, index - n);
    }
    return new String(uid);
  }

  /**
   * Generates a string of random alphanumeric characters. Uses a {@link SecureRandom} instance and
   * is slower than {@link #generateCode(int)}, this should be used for security-related purposes
   * only.
   *
   * @param codeSize the number of characters in the code.
   * @return the code.
   */
  public static char[] generateSecureRandomCode(int codeSize) {
    SecureRandom sr = SecureRandomHolder.GENERATOR;
    return generateRandomAlphanumericCode(codeSize, sr);
  }

  public static byte[] generateSecureRandomBytes(int length) {
    SecureRandom sr = SecureRandomHolder.GENERATOR;
    byte[] bytes = new byte[length];
    sr.nextBytes(bytes);
    return bytes;
  }

  /**
   * Generates a string of random numeric characters.
   *
   * @param length the number of characters in the code.
   * @return the code.
   */
  public static char[] generateSecureRandomNumber(int length) {
    char[] digits = new char[length];
    SecureRandom sr = SecureRandomHolder.GENERATOR;
    for (int i = 0; i < length; i++) {
      digits[i] = (char) ('0' + sr.nextInt(10));
    }
    return digits;
  }

  /**
   * Generates a random secure token.
   *
   * <p>The token is generated using {@link SecureRandom} and should be used for security-related
   * purposes only.
   *
   * @return a token.
   */
  public static String getRandomSecureToken() {
    SecureRandom sr = SecureRandomHolder.GENERATOR;
    return new String(generateRandomAlphanumericCode(SECURE_RANDOM_TOKEN_MIN_SIZE, sr));
  }

  /**
   * Tests whether the given code is a valid UID.
   *
   * @param code the code to validate.
   * @return true if the code is valid.
   */
  public static boolean isValidUid(String code) {
    return code != null && UID_PATTERN.matcher(code).matches();
  }
}
