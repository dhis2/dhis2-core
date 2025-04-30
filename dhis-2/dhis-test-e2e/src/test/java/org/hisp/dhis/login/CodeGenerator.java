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
package org.hisp.dhis.login;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author bobj
 */
public class CodeGenerator {

  private CodeGenerator() {
    throw new IllegalStateException("Utility class");
  }

  public static final String NUMERIC_CHARS = "0123456789";

  public static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  public static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";

  public static final String LETTERS = LOWERCASE_LETTERS + UPPERCASE_LETTERS;

  public static final String ALPHANUMERIC_CHARS = NUMERIC_CHARS + LETTERS;

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
   * and is considered non-secure and should not be used for security.
   *
   * @param codeSize the number of characters in the code.
   * @return the code.
   */
  public static String generateCode(int codeSize) {
    ThreadLocalRandom r = ThreadLocalRandom.current();
    return new String(generateRandomAlphanumericCode(codeSize, r));
  }
}
