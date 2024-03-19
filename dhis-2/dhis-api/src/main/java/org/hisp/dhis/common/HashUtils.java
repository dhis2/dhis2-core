/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.common;

import com.google.common.hash.Hashing;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class HashUtils {
  private HashUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Calculates a SHA256 hash for the given input string.
   *
   * @param bytes the input string.
   * @return the hash.
   */
  public static String hashSHA256(@Nonnull byte[] bytes) {
    return Hashing.sha256().hashBytes(bytes).toString();
  }

  public static String hashSHA256(@Nonnull String str) {
    return hashSHA256(str.getBytes(StandardCharsets.UTF_8));
  }

  public static String hashSHA256(@Nonnull char[] chars) {
    byte[] bytes = extractBytesFromChar(chars);
    return hashSHA256(bytes);
  }

  /**
   * Calculates a SHA512 hash for the given input string.
   *
   * @param bytes the input string.
   * @return the hash.
   */
  public static String hashSHA512(@Nonnull byte[] bytes) {
    return Hashing.sha512().hashBytes(bytes).toString();
  }

  public static String hashSHA512(@Nonnull String str) {
    return hashSHA512(str.getBytes(StandardCharsets.UTF_8));
  }

  public static String hashSHA512(@Nonnull char[] chars) {
    byte[] bytes = extractBytesFromChar(chars);

    return hashSHA512(bytes);
  }

  /**
   * Creates a byte array from a char array.
   *
   * @param chars the char array
   * @return the byte array
   */
  private static byte[] extractBytesFromChar(char[] chars) {
    Charset charset = StandardCharsets.UTF_8;
    CharBuffer charBuffer = CharBuffer.wrap(chars);
    ByteBuffer byteBuffer = charset.encode(charBuffer);
    byte[] bytes = new byte[byteBuffer.remaining()];
    byteBuffer.get(bytes);
    return bytes;
  }

  /**
   * Try to check if the given string is a valid SHA-256 hash in hexadecimal format.
   *
   * @param str the string to check.
   * @return true if the string is a valid SHA-256 hash in hexadecimal format, false otherwise.
   */
  public static boolean isValidSHA256HexFormat(String str) {
    // Check if the string is a valid hexadecimal number
    String hexPattern = "^[0-9a-fA-F]+$";
    Pattern pattern = Pattern.compile(hexPattern);
    if (!pattern.matcher(str).matches()) {
      return false;
    }

    // SHA-256 in hexadecimal are exactly 64 characters long
    return str.length() == 64;
  }
}
