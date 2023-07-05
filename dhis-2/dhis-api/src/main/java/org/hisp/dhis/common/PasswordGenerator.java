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

import java.security.SecureRandom;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class PasswordGenerator {
  private PasswordGenerator() {
    throw new IllegalStateException("Utility class");
  }

  public static final String NUMERIC_CHARS = "0123456789";

  public static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  public static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";

  public static final String LETTERS = LOWERCASE_LETTERS + UPPERCASE_LETTERS;

  public static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;':,./<>?©®™¢£¥€";

  public static final String NUMERIC_AND_SPECIAL_CHARS = NUMERIC_CHARS + SPECIAL_CHARS;

  public static final String ALL_CHARS = NUMERIC_CHARS + LETTERS + SPECIAL_CHARS;

  /**
   * Generates a random password with the given size, has to be minimum 8 characters long.
   *
   * <p>The password will contain at least one digit, one special character, one uppercase letter
   * and one lowercase letter.
   *
   * <p>The password will not contain more than 2 consecutive letters to avoid generating words.
   *
   * <p>Passwords should always be greater than 4 characters, since the first 4 has predetermined
   * lesser character sets. To avoid using it in production with size set to 4 or less, we enforce
   * minimum size of 8, which is the default password minimum length in DHIS2.
   *
   * @param size the size of the password.
   * @return a random password.
   */
  public static char[] generateValidPassword(int size) {
    if (size < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters long");
    }

    char[] chars = new char[size];

    chars[0] = generateCharacter(NUMERIC_CHARS);
    chars[1] = generateCharacter(SPECIAL_CHARS);
    chars[2] = generateCharacter(UPPERCASE_LETTERS);
    chars[3] = generateCharacter(LOWERCASE_LETTERS);

    int c = 2; // the last 2 characters are letters
    for (int i = 4; i < size; ++i) {
      if (c >= 2) {
        // After 2 consecutive letters, the next character should be a number or a special character
        chars[i] = generateCharacter(NUMERIC_AND_SPECIAL_CHARS);
        c = 0;
      } else {
        chars[i] = generateCharacter(ALL_CHARS);
        if (LETTERS.indexOf(chars[i]) >= 0) {
          // If the character is a letter, increment the counter
          c++;
        } else {
          // If the character is not a letter, reset the counter
          c = 0;
        }
      }
    }

    return chars;
  }

  private static char generateCharacter(String str) {
    SecureRandom sr = CodeGenerator.SecureRandomHolder.GENERATOR;
    return str.charAt(sr.nextInt(str.length()));
  }

  public static boolean containsDigit(char[] chars) {
    for (char c : chars) {
      if (c >= '0' && c <= '9') {
        return true;
      }
    }
    return false;
  }

  public static boolean containsSpecialCharacter(char[] chars) {
    for (char c : chars) {
      if (SPECIAL_CHARS.indexOf(c) >= 0) {
        return true;
      }
    }
    return false;
  }

  public static boolean containsUppercaseCharacter(char[] chars) {
    for (char c : chars) {
      if (c >= 'A' && c <= 'Z') {
        return true;
      }
    }
    return false;
  }

  public static boolean containsLowercaseCharacter(char[] chars) {
    for (char c : chars) {
      if (c >= 'a' && c <= 'z') {
        return true;
      }
    }
    return false;
  }
}
