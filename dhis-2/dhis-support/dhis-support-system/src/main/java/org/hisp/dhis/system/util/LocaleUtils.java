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
package org.hisp.dhis.system.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author Oyvind Brucker
 */
public class LocaleUtils {

  private LocaleUtils() {}

  private static final String SEP = "_";

  /**
   * Creates a Locale object based on the input string.
   *
   * @param localeStr String to parse
   * @return A locale object or null if not valid
   */
  public static Locale getLocale(String localeStr) {
    if (localeStr == null || localeStr.trim().isEmpty()) {
      return null;
    }

    try {
      if (localeStr.contains("-")) {
        // BCP 47: en-US, uz-Cyrl-UZ
        return Locale.forLanguageTag(localeStr);
      }

      // Legacy format: en_US, uz_UZ_Cyrl
      String[] parts = localeStr.split("_");
      Locale.Builder builder = new Locale.Builder();

      if (parts.length > 0) builder.setLanguage(parts[0]);
      if (parts.length > 1) builder.setRegion(parts[1]);
      if (parts.length > 2) builder.setScript(parts[2]);

      return builder.build();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid locale string: " + localeStr, e);
    }
  }

  /**
   * Createa a locale string based on the given language, country and script.
   *
   * @param language the language, cannot be null.
   * @param country the country, can be null.
   * @param script the script of the language, can be null.
   * @return a locale string.
   */
  public static String getLocaleString(String language, String country, String script) {
    Locale locale;

    if (script != null && !script.isEmpty()) {
      locale =
          new Locale.Builder().setLanguage(language).setRegion(country).setScript(script).build();
    } else {
      locale = new Locale.Builder().setLanguage(language).setRegion(country).build();
    }

    return locale.toLanguageTag();
  }

  /**
   * Creates a list of locales of all possible specifities based on the given Locale. As an example,
   * for the given locale "en_UK", the locales "en" and "en_UK" are returned. Additionally, if the
   * locale has a script, it will also return "en_Cyrl" if the script is "Cyrl". The order of the
   * list is from most specific to least specific.
   *
   * @param locale the Locale.
   * @return a list of locale strings.
   */
  public static List<String> getLocaleFallbacks(Locale locale) {
    Set<String> fallbacks = new LinkedHashSet<>();
    String lang = locale.getLanguage();
    String region = locale.getCountry();
    String script = locale.getScript();
    String variant = locale.getVariant();

    if (!script.isEmpty() && !region.isEmpty()) {
      fallbacks.add(lang + "_" + region + "_" + script);
      fallbacks.add(lang + "_" + script + "_" + region);
    }

    if (!region.isEmpty()) {
      fallbacks.add(lang + "_" + region);
    }

    if (!script.isEmpty()) {
      fallbacks.add(lang + "_" + script);
    }

    fallbacks.add(lang);

    if (!variant.isEmpty()) {
      fallbacks.add(locale.toString());
    }

    return new ArrayList<>(fallbacks);
  }
}
