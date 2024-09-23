/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.setting;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;

/**
 * @author Jan Bernitt (refactored version)
 */
public interface SystemSettingsService extends SystemSettingsProvider {

  /** Called at the start of a new request to ensure fresh view of the settings */
  void clearCurrentSettings();

  void saveSystemSetting(@Nonnull String key, @CheckForNull Serializable value);

  /**
   * Saves the given system setting key and value.
   *
   * @param settings the new values, null or empty values delete the setting
   */
  void saveSystemSettings(@Nonnull Map<String, String> settings);

  /**
   * Deletes the system setting with the given name.
   *
   * @param names of the system setting to delete
   */
  void deleteSystemSettings(@Nonnull Set<String> names);

  /**
   * Saves the translation for given setting key and locale if given setting key is translatable. If
   * the translation string contains an empty string, the translation for given locale and key is
   * removed.
   *
   * @param key of the related setting
   * @param locale locale of the translation (should be a language tag)
   * @param translation translation text, null or empty to delete
   */
  void saveSystemSettingTranslation(
      @Nonnull String key, @Nonnull String locale, @CheckForNull String translation)
      throws ForbiddenException, BadRequestException;

  /**
   * Returns the translation for given setting key and locale or empty Optional if no translation is
   * available or setting key is not translatable.
   *
   * @param key SettingKey
   * @param locale Locale of required translation
   * @return The Optional with the actual translation or empty Optional
   */
  Optional<String> getSystemSettingTranslation(@Nonnull String key, @Nonnull String locale);
}
