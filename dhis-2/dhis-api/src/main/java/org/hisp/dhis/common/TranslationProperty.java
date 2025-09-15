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

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.translation.Translation;

/**
 * Embedded property to be mapped to {@code translations} jsonb column in database. This class can
 * be declared in an Entity class as below. Note that the property name must be {@code translations}
 * so that it can be picked up by the {@code PropertyIntrospector}
 *
 * <p>{@code @Embedded} private TranslationProperty translations = new TranslationProperty();
 *
 * @author viet
 */
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class TranslationProperty implements Serializable {

  @Type(type = "jblTranslations")
  private Set<Translation> translations = new HashSet<>();

  /**
   * Cache for object translations, where the cache key is a combination of locale and translation
   * property, and value is the translated value.
   */
  @Transient
  protected final transient Map<String, String> translationCache = new ConcurrentHashMap<>();

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  public Set<Translation> getTranslations() {
    if (translations == null) {
      translations = new HashSet<>();
    }

    return translations;
  }

  /** Clears out cache when setting translations. */
  public void setTranslations(Set<Translation> translations) {
    this.translationCache.clear();
    this.translations = translations;
  }

  // -------------------------------------------------------------------------
  // Util methods
  // -------------------------------------------------------------------------

  /**
   * Get Translation value from {@code Set<Translation>} by given locale and translationKey
   *
   * @return Translation value if exists, otherwise return default value.
   */
  private String getTranslationValue(String locale, String translationKey, String defaultValue) {
    for (Translation translation : translations) {
      if (locale.equals(translation.getLocale())
          && translationKey.equals(translation.getProperty())
          && !StringUtils.isEmpty(translation.getValue())) {
        return translation.getValue();
      }
    }

    return defaultValue;
  }

  /**
   * Returns a translated value for this object for the given property. The current locale is read
   * from the user context.
   *
   * @param translationKey the translation key.
   * @param defaultValue the value to use if there are no translations.
   * @return a translated value.
   */
  public String getTranslation(String translationKey, String defaultValue) {
    Locale locale = UserSettings.getCurrentSettings().getUserDbLocale();

    final String defaultTranslation = defaultValue != null ? defaultValue.trim() : null;

    if (locale == null || translationKey == null || CollectionUtils.isEmpty(getTranslations())) {
      return defaultValue;
    }

    return translationCache.computeIfAbsent(
        Translation.getCacheKey(locale.toString(), translationKey),
        key -> getTranslationValue(locale.toString(), translationKey, defaultTranslation));
  }

  /**
   * Util method for casting translations from Object to TranslationProperty or Set depending on the
   * current call is serializing or deserializing.
   *
   * @param translations the translations object which can be either a TranslationProperty or a Set
   *     of Translation.
   * @return Set of Translation.
   */
  public static Set<Translation> fromObject(Object translations) {
    if (translations == null) {
      return Set.of();
    }
    Set<Translation> list;
    if (translations instanceof TranslationProperty translationProperty) {
      list = translationProperty.getTranslations();
    } else if (translations instanceof Set) {
      list = (Set<Translation>) translations;
    } else {
      throw new IllegalArgumentException("Object translations is invalid");
    }

    return list;
  }
}
