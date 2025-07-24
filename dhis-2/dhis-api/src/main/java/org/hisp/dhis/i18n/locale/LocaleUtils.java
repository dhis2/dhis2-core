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
   * @param script , can be null.
   * @return a locale string.
   */
  public static String getLocaleString(String language, String country, String script) {
    if (language == null || language.isEmpty()) {
      throw new IllegalArgumentException("Language must not be null or empty");
    }

    Locale locale;

    if (script != null && !script.isEmpty()) {
      locale = new Locale.Builder()
          .setLanguage(language)
          .setRegion(country != null ? country : "")
          .setScript(script)
          .build();
    } else if (country == null || country.isEmpty()) {
      locale = new Locale(language);
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

    fallbacks.add(lang);

    if (!region.isEmpty()) {
      fallbacks.add(lang + SEP + region);
    }

    // Include script fallbacks
    if (!script.isEmpty()) {
      fallbacks.add(lang + SEP + region + SEP + script);
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
      String language = parts[0];
        String country = parts.length > 1 ? parts[1] : "";
        String script = parts.length > 2 ? parts[2] : "";
      if (parts.length == 3) {
        return new Locale.Builder().setLanguage(language).setRegion(country).setScript(script).build();
      } else if (parts.length == 2) {
        return new Locale.Builder().setLanguage(language).setRegion(country).build();
      } else {
        return new Locale.Builder().setLanguage(language).build();
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse locale: " + localeStr, e);
    }
  }


  public static String toUnderscoreFormat(Locale locale) {
    StringBuilder sb = new StringBuilder(locale.getLanguage());
    if (!locale.getScript().isEmpty()) {
      sb.append("_").append(locale.getCountry()).append("_").append(locale.getScript());
    } else if (!locale.getCountry().isEmpty()) {
      sb.append("_").append(locale.getCountry());
    }
    return sb.toString();
  }
}
