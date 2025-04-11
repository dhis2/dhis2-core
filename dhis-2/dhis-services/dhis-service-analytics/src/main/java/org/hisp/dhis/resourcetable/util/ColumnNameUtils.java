/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.resourcetable.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class ColumnNameUtils {

  private static final int MAX_COLUMN_NAME_LENGTH = 127;
  private static final char REPLACEMENT_CHAR = '_';

  /**
   * Transforms a given string into a valid column name according to specific rules.
   *
   * <p>Rules for a valid column name:
   *
   * <ul>
   *   <li>Begins with an ASCII single-byte alphabetic character (a-z, A-Z) or underscore (_).
   *   <li>Subsequent characters can be ASCII single-byte alphanumeric characters (a-z, A-Z, 0-9) or
   *       underscores (_).
   *   <li>Length must be between 1 and 127 bytes (characters in this ASCII context).
   *   <li>Contains no quotation marks ('", `) or spaces.
   * </ul>
   *
   * Invalid characters (including spaces and quotes) are replaced with underscores. If the
   * resulting name doesn't start with a valid character, an underscore is prepended. If the input
   * is null, blank, or results in an empty string after processing, "" is returned. The result is
   * truncated to 127 characters if necessary.
   *
   * @param columnName The input string to transform.
   * @return A valid column name string, conforming to the specified rules.
   */
  public static String toValidColumnName(String columnName) {

    if (StringUtils.isBlank(columnName)) {
      return "";
    }

    StringBuilder validNameBuilder = new StringBuilder(columnName.length());
    boolean firstCharProcessed = false;

    // Iterate and replace invalid characters, keeping track of the first valid one
    for (char c : columnName.toCharArray()) {
      boolean isValid;
      if (!firstCharProcessed) {
        // Rule for the very first character added to the builder
        isValid = isValidFirstChar(c);
        if (isValid || isValidSubsequentChar(c)) {
          firstCharProcessed = true;
          validNameBuilder.append(c);
        } else {
          // Completely invalid char at the start, replace but don't mark as processed yet
          // So the *next* valid char can become the firstCharCandidate
          // We only append replacement if builder is not exceeding max length potential
          if (validNameBuilder.length() < MAX_COLUMN_NAME_LENGTH) {
            validNameBuilder.append(REPLACEMENT_CHAR);
          }
        }
      } else {
        isValid = isValidSubsequentChar(c);
        if (isValid) {
          validNameBuilder.append(c);
        } else {
          validNameBuilder.append(REPLACEMENT_CHAR);
        }
      }
      // Early exit if builder already reached max length during iteration
      if (validNameBuilder.length() >= MAX_COLUMN_NAME_LENGTH) {
        break;
      }
    }

    // Handle cases where loop finished without finding any valid character or builder is empty
    if (validNameBuilder.isEmpty()) {
      return "";
    }

    return getString(validNameBuilder);
  }

  private static String getString(StringBuilder validNameBuilder) {
    String potentiallyValidName = validNameBuilder.toString();

    // Check if the first character of the resulting string is valid (letter or underscore)
    // It might be a digit if the original string started with digits or invalid chars followed by
    // digits.
    if (!isValidFirstChar(potentiallyValidName.charAt(0))) {
      // Prepend underscore if first char is not a letter or underscore (must be a digit)
      potentiallyValidName = REPLACEMENT_CHAR + potentiallyValidName;
    }

    // 5. Enforce maximum length (truncate if necessary after potential prepending)
    if (potentiallyValidName.length() > MAX_COLUMN_NAME_LENGTH) {
      potentiallyValidName = potentiallyValidName.substring(0, MAX_COLUMN_NAME_LENGTH);
    }
    return potentiallyValidName;
  }

  /**
   * Checks if a character is a valid first character for a column name. (ASCII single-byte
   * alphabetic character or underscore)
   *
   * @param c The character to check.
   * @return true if valid, false otherwise.
   */
  private static boolean isValidFirstChar(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  /**
   * Checks if a character is a valid subsequent character for a column name. (ASCII single-byte
   * alphanumeric character or underscore)
   *
   * @param c The character to check.
   * @return true if valid, false otherwise.
   */
  private static boolean isValidSubsequentChar(char c) {
    // Includes checks from isValidFirstChar plus digits
    return isValidFirstChar(c) || (c >= '0' && c <= '9');
  }
}
