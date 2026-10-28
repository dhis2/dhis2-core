/*
 * Copyright (c) 2004-2026, University of Oslo
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
import org.hisp.dhis.jsontree.Appender;
import org.hisp.dhis.jsontree.Text;

/**
 * A OpenAPI specific utility for generating HTML.
 *
 * <p>While similar methods are available in common utility frameworks these are not suitable for
 * three main reasons: 1. They tend to do more than needed and wanted 2. They often are not
 * optimized for speed 3. The OpenAPI rendering implementation should be extractable to a library
 * with no 3rd party dependencies
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
   * HTML-escapes the provided text preserving any HTML entities that are contained, appending the
   * result to the given {@link Appender}.
   *
   * <p>Unescaped content is written in contiguous runs via {@link
   * Appender#appendEncoded(CharSequence, int, int)}. Writing runs rather than individual characters
   * is required for correctness on byte-oriented sinks: a surrogate pair split across two
   * single-char {@code appendEncoded(char)} calls is encoded as two {@code U+FFFD} because neither
   * half is a complete codepoint on its own.
   *
   * @param text "unsafe" text that should be inserted into HTML as "plain" text
   * @param to the target to append the escaped text to
   */
  public static void appendEscaped(@CheckForNull Text text, Appender to) {
    if (text == null || text.isEmpty()) return;
    int len = text.length();
    int runStart = 0;
    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);
      switch (c) {
        case '<' -> {
          if (i > runStart) to.appendEncoded(text, runStart, i);
          to.appendAscii("&lt;", 0, 4);
          runStart = i + 1;
        }
        case '>' -> {
          if (i > runStart) to.appendEncoded(text, runStart, i);
          to.appendAscii("&gt;", 0, 4);
          runStart = i + 1;
        }
        case '"' -> {
          if (i > runStart) to.appendEncoded(text, runStart, i);
          to.appendAscii("&quot;", 0, 6);
          runStart = i + 1;
        }
        case '\'' -> {
          if (i > runStart) to.appendEncoded(text, runStart, i);
          to.appendAscii("&#039;", 0, 6);
          runStart = i + 1;
        }
        case '&' -> {
          if (i > runStart) to.appendEncoded(text, runStart, i);
          int j = findEndOfHtmlEntity(text, i);
          if (j < 0) {
            to.appendAscii("&amp;", 0, 5);
            runStart = i + 1;
          } else {
            // preserve the entity as-is
            to.appendAscii(text, i, j);
            i = j - 1; // compensate for the loop increment
            runStart = j;
          }
        }
        default -> {
          /* part of the pending run */
        }
      }
    }
    if (runStart < len) to.appendEncoded(text, runStart, len);
  }

  private static int findEndOfHtmlEntity(Text text, int i) {
    int len = text.length();
    if (i + 3 >= len) return -1; // can't be
    int j = i + 1;
    // scan for HTML entity to preserve it
    if (text.charAt(j) == '#') {
      j++; // skip beyond the #
      int k = min(j + 6, len);
      if (text.charAt(j) == 'x') {
        // &#x[0-9A-Fa-f]{1,5};
        j++; // skip the x
        while (j < k && isHexDigit(text.charAt(j))) j++;
      } else {
        // &#[0-9]{1,5};
        while (j < k && isDigit(text.charAt(j))) j++;
      }
    } else {
      // &[a-zA-Z]{1,24};
      int k = min(j + 31, len); // maximum name length is 24
      while (j < k && isLetter(text.charAt(j))) j++;
    }
    if (j < len && text.charAt(j) == ';' && j - i > 3) return j + 1;
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
}
