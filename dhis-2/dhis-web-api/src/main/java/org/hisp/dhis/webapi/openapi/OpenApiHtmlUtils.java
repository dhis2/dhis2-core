/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.openapi;

import static java.lang.Math.min;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A OpenAPI specific utility for generating HTML.
 *
 * <p>While similar methods are available in common utility frameworks these are not suitable for
 * three main reasons: 1. They tend to do more than needed and wanted 2. They often are not
 * optimized for speed 3. The OpenAPI rendering implementation should be extractable to a library
 * with no 3rd party dependencies
 *
 * <p>When generating big HTML documents from big JSON OpenAPI specifications the stripping and
 * escaping of text occurs quite frequent and has to be
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OpenApiHtmlUtils {

  /**
   * Simply removes any characters that cannot occur in an HTML element or attribute value.
   *
   * @param value as used for an HTML element attribute
   * @return the value without any characters that need escaping in a HTML attribute value
   */
  @Nonnull
  static String stripHtml(@CheckForNull String value) {
    if (value == null) return "";
    if (value.isBlank()) return value;
    StringBuilder escaped = new StringBuilder(value.length());
    value
        .chars()
        .forEach(
            c -> {
              switch (c) {
                case '<', '>', '"', '\'', '&' -> {}
                default -> escaped.append((char) c);
              }
            });
    return escaped.toString();
  }

  /**
   * HTML-escapes the provided text preserving any HTML entities that are contained. That means they
   * are not escaped again, so they keep working as HTML entities.
   *
   * @param text "unsafe" text that should be inserted into HTML as "plain" text
   * @return the equivalent HTML text
   */
  @Nonnull
  public static String escapeHtml(@CheckForNull String text) {
    if (text == null) return "";
    if (text.isBlank()) return text;
    if (text.indexOf('&') < 0) return escapeHtmlFast(text);
    char[] chars = text.toCharArray();
    int len = chars.length;
    StringBuilder escaped = new StringBuilder(nextDivisibleBy64(len));
    for (int i = 0; i < len; i++) {
      char c = chars[i];
      switch (c) {
        case '<' -> escaped.append("&lt;");
        case '>' -> escaped.append("&gt;");
        case '"' -> escaped.append("&quot;");
        case '\'' -> escaped.append("&#039;");
        case '&' -> {
          int j = findEndOfHtmlEntity(chars, i);
          if (j < 0) {
            escaped.append("&amp;");
          } else {
            // this is a HTML entity, keep it
            escaped.append(chars, i, j - i);
            i = j - 1; // as i is increased at the end of the loop
          }
        }
        default -> escaped.append((char) c);
      }
    }
    return escaped.toString();
  }

  private static int findEndOfHtmlEntity(char[] chars, int i) {
    int len = chars.length;
    if (i + 3 >= len) return -1; // can't be
    int j = i + 1;
    // scan for HTML entity to preserve it
    if (chars[j] == '#') {
      j++; // skip beyond the #
      int k = min(j + 6, len);
      if (chars[j] == 'x') {
        // &#x[0-9A-Fa-f]{1,5};
        j++; // skip the x
        while (j < k && isHexDigit(chars[j])) j++;
      } else {
        // &#[0-9]{1,5};
        while (j < k && isDigit(chars[j])) j++;
      }
    } else {
      // &[a-zA-Z]{1,24};
      int k = min(j + 31, len); // maximum name length is 24
      while (j < k && isLetter(chars[j])) j++;
    }
    if (j < chars.length && chars[j] == ';' && j - i > 3) return j + 1;
    return -1;
  }

  private static boolean isLetter(char c) {
    return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isHexDigit(char c) {
    return isDigit(c) || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
  }

  /**
   * Escapes by replacing characters in a stream writing to a new buffer. This does not preserve use
   * HTML entities so should only be used when no "&" is in the input should those be preserved.
   *
   * @param str unescaped text
   * @return HTML escaped text
   */
  @Nonnull
  private static String escapeHtmlFast(String str) {
    StringBuilder escaped = new StringBuilder(nextDivisibleBy64(str.length()));
    str.chars()
        .forEach(
            c -> {
              switch (c) {
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#039;");
                case '&' -> escaped.append("&amp;");
                default -> escaped.append((char) c);
              }
            });
    return escaped.toString();
  }

  /**
   * When escaping we want some room for potential occurrence of escaped characters as the escaped
   * string is longer. It makes sense to allocate a buffer in 64 size steps, so we use the next
   * length divisible by 64.
   */
  private static int nextDivisibleBy64(int n) {
    return n + (64 - (n % 64));
  }
}
