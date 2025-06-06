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
package org.hisp.dhis.common;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.translation.Translation;

public interface TranslatableMetadataObject {

  Set<Translation> getTranslations();

  Map<String, String> getTranslationCache();

  // -------------------------------------------------------------------------
  // Util methods
  // -------------------------------------------------------------------------

  /**
   * Returns a translated value for this object for the given property. The current locale is read
   * from the user context.
   *
   * @param translationKey the translation key.
   * @param defaultValue the value to use if there are no translations.
   * @return a translated value.
   */
  default String getTranslation(String translationKey, String defaultValue) {
    Locale locale = UserSettings.getCurrentSettings().getUserDbLocale();

    final String defaultTranslation = defaultValue != null ? defaultValue.trim() : null;

    if (locale == null || translationKey == null || CollectionUtils.isEmpty(getTranslations())) {
      return defaultValue;
    }

    return getTranslationCache()
        .computeIfAbsent(
            Translation.getCacheKey(locale.toString(), translationKey),
            key -> getTranslationValue(locale.toString(), translationKey, defaultTranslation));
  }

  /**
   * Get Translation value from {@code Set<Translation>} by given locale and translationKey
   *
   * @return Translation value if exists, otherwise return default value.
   */
  private String getTranslationValue(String locale, String translationKey, String defaultValue) {
    for (Translation translation : getTranslations()) {
      if (locale.equals(translation.getLocale())
          && translationKey.equals(translation.getProperty())
          && !StringUtils.isEmpty(translation.getValue())) {
        return translation.getValue();
      }
    }

    return defaultValue;
  }
}
