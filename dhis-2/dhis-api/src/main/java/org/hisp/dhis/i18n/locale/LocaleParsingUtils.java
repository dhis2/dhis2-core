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
package org.hisp.dhis.i18n.locale;

import java.util.Locale;

public class LocaleParsingUtils {

  private LocaleParsingUtils() {
    // Utility class, no instantiation
  }

  /**
   * Parses a locale string in either legacy underscore format (e.g., "en_US") or BCP 47 format
   * (e.g., "en-US") and returns a Locale object. If the legacy format is used, it can also include
   * a script (e.g., "en_US_Latn").
   *
   * @param localeStr the locale string to parse
   * @return a Locale object representing the parsed locale
   */
  public static Locale parse(String localeStr) {
    if (localeStr == null || localeStr.isBlank()) {
      return Locale.getDefault();
    }
    try {
      // Legacy style with underscores
      if (localeStr.contains("_")) {
        // Try legacy style with script
        String[] parts = localeStr.split("_");
        if (parts.length == 3) {
          return new Locale.Builder()
              .setLanguage(parts[0])
              .setRegion(parts[1])
              .setScript(parts[2])
              .build();
        } else if (parts.length == 2) {
          return new Locale(parts[0], parts[1]);
        } else {
          return new Locale(parts[0]);
        }
      } else {
        // BCP 47 style with hyphens
        return Locale.forLanguageTag(localeStr);
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse locale: " + localeStr, e);
    }
  }
}
