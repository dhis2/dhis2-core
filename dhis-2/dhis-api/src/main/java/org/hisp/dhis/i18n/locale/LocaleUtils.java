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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocaleUtils {

  private LocaleUtils() {
    // Utility class, no instantiation
  }

  private static final String SEP = "_";

  /**
   * Create a locale string based on the given language, country and script.
   *
   * @param language the language, cannot be null.
   * @param country the country, can be null.
   * @param variant , can be null.
   * @return a locale string.
   */
  public static String getLocaleString(String language, String country, String variant) {
    Locale locale;

    if (variant != null && !variant.isEmpty()) {
      locale = new Locale(language, country, variant);
    } else {
      locale = new Locale(language, country);
    }

    return toUnderscoreFormat(locale);
  }

  /**
   * Creates a list of locales of all possible specifities based on the given Locale. As an example,
   * for the given locale "en_UK", the locales "en" and "en_UK" are returned.
   *
   * @param locale the Locale.
   * @return a list of locale strings.
   */
  public static List<String> getLocaleFallbacks(Locale locale) {
    List<String> fallbacks = new ArrayList<>();
    String lang = locale.getLanguage();
    String region = locale.getCountry();
    String script = locale.getScript();
    String variant = locale.getVariant();

    fallbacks.add(lang);

    if (!region.isEmpty()) {
      fallbacks.add(lang + SEP + region);
    }

    // Include script fallbacks
    if (!script.isEmpty()) {
      fallbacks.add(lang + SEP + script);

      if (!region.isEmpty()) {
        fallbacks.add(lang + SEP + region + SEP + script);
        fallbacks.add(lang + SEP + script + SEP + region);
      }
    }

    // Legacy fallback using variant
    if (!variant.isEmpty()) {
      fallbacks.add(locale.toString());
    }

    return fallbacks;
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
      String[] parts = localeStr.split("_");
      if (parts.length == 3) {
        return new Locale(parts[0], parts[1], parts[2]);
      } else if (parts.length == 2) {
        return new Locale(parts[0], parts[1]);
      } else {
        return new Locale(parts[0]);
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse locale: " + localeStr, e);
    }
  }

  // Note that in the context of DHIS2, the underscore format is used for locale strings.
  // It may be the same as Locale.toString() for some locales, but it is not guaranteed.
  // For supported locales, (language + country + variant) is used.
  public static String toUnderscoreFormat(Locale locale) {
    StringBuilder sb = new StringBuilder(locale.getLanguage());

    if (!locale.getVariant().isEmpty()) {
      sb.append("_").append(locale.getCountry()).append("_").append(locale.getVariant());
    } else if (!locale.getCountry().isEmpty()) {
      sb.append("_").append(locale.getCountry());
    }
    return sb.toString();
  }
}
