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
package org.hisp.dhis.user;

import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.setting.UserSettings;

/**
 * The main interface for working with user settings.
 *
 * @author Jan Bernitt
 */
public interface UserSettingService {

  /**
   * Note that a user's setting including fallbacks can and should be accessed via {@link
   * UserSettings#getCurrentSettings()} for the current user or for any other particular user use
   * {@link UserDetails#getUserSettings()}.
   *
   * <p>This method is just suitable when building composed settings with fallbacks or when fallback
   * explicitly are not wanted, e.g. to make a copy of a user's settings.
   *
   * @param username the user for whom to fetch settings
   * @return the user's setting as stored in DB **without** any fallbacks applied. If no such user
   *     exists an empty {@link UserSettings} is returned.
   */
  UserSettings getSettings(@Nonnull String username);

  /**
   * Saves the key/value pair as a user setting for the current user
   *
   * @param key the user setting key.
   * @param value the setting value, null or empty to delete
   */
  void saveUserSetting(@Nonnull String key, @CheckForNull String value);

  /**
   * Saves the name/value pair as a user setting connected to user.
   *
   * @param key the user setting key.
   * @param value the setting value, null or empty to delete
   * @param username owner/target of the settings update
   */
  void saveUserSetting(@Nonnull String key, @CheckForNull String value, @Nonnull String username)
      throws NotFoundException;

  /**
   * Updates all settings in the provided collection.
   *
   * @param settings settings to store, null or empty values are deleted
   * @param username owner/target of the settings update
   */
  void saveUserSettings(@Nonnull Map<String, String> settings, @Nonnull String username)
      throws NotFoundException;

  /**
   * Deletes all settings of a user. If no such user exists this has no effect.
   *
   * @param username owner/target of the settings deletion
   */
  void deleteAllUserSettings(@Nonnull String username);
}
