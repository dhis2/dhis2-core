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
package org.hisp.dhis.webapi.webdomain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.i18n.locale.LocaleUtils;

/**
 * Class that represents a Locale for the web
 *
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class WebLocale {
  @JsonProperty private final String locale;

  @JsonProperty private final String languageTag;

  @JsonProperty private final String name;

  @JsonProperty private final String displayName;

  /**
   * @param locale any locale, used to display the locale property, and name in its own language
   * @param userLocale a user-specific locale used to format the language in the displayName
   *     property
   * @return A WebLocale instance
   */
  public static WebLocale fromLocale(Locale locale, Locale userLocale) {
    String localeStr = LocaleUtils.toUnderscoreFormat(locale);
    String languageTag = locale.toLanguageTag();

    if (!locale.getScript().isEmpty()) {
      String name = buildLocaleDisplay(locale, locale);
      String displayName = buildLocaleDisplay(locale, userLocale);
      return new WebLocale(localeStr, languageTag, name, displayName);
    }

    return new WebLocale(
        localeStr, languageTag, locale.getDisplayName(locale), locale.getDisplayName(userLocale));
  }

  private static String buildLocaleDisplay(Locale target, Locale displayLocale) {
    String language = target.getDisplayLanguage(displayLocale);
    String country = target.getDisplayCountry(displayLocale);
    String script = target.getScript();

    StringBuilder sb = new StringBuilder(language);

    if (!country.isEmpty() || !script.isEmpty()) {
      sb.append(" (");
      if (!country.isEmpty()) {
        sb.append(country);
        if (!script.isEmpty()) {
          sb.append(", ");
        }
      }
      if (!script.isEmpty()) {
        sb.append(script);
      }
      sb.append(")");
    }

    return sb.toString();
  }
}
