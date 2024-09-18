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

import org.hisp.dhis.setting.UserSettings;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * The main interface for working with user settings. Implementation need to get the current user
 * from {@link CurrentUserUtil}.
 *
 * @author Torgeir Lorange Ostby
 */
public interface UserSettingService {

  /**
   * Saves the key/value pair as a user setting connected to the currently logged in user.
   *
   * @param key the user setting key.
   * @param value the setting value.
   */
  void saveUserSetting(String key, String value);

  /**
   * Saves the name/value pair as a user setting connected to user.
   *
   * @param key the user setting key.
   * @param value the setting value.
   * @param user the user.
   */
  void saveUserSetting(String key, String value, UserDetails user);

  /**
   * Updates all non-null settings in the {@link UserSettingsDto} collection.
   *
   * @param settings settings to store, maybe null
   * @param user owner/target of the settings
   */
  void saveUserSettings(UserSettingsDto settings, UserDetails user);

  /**
   * Deletes a UserSetting.
   *
   * @param key the UserSetting to delete.
   */
  void deleteUserSetting(String key);

  /**
   * Deletes the user setting with the given name for the given user.
   *
   * @param key the user setting key.
   * @param user the user.
   */
  void deleteUserSetting(String key, UserDetails user);

  /**
   * Note that a user's setting including fallbacks can and should be accessed via {@link
   * UserSettings#getCurrentSettings()} for the current user or for any other particular user use
   * {@link UserDetails#getUserSettings()}.
   *
   * <p>This method is just suitable when building composed settings with fallbacks or when fallback
   * explicitly are not wanted, e.g. to make a copy of a user's settings.
   *
   * @param user the user for whom to fetch settings
   * @return the user's setting as stored in DB without any fallbacks applied
   */
  UserSettings getSettings(UserDetails user);

  /**
   * Returns the value of the user setting specified by the given name.
   *
   * @param key the user setting key.
   * @return the value corresponding to the named user setting, or null if there is no match.
   */
  Serializable getUserSetting(UserSettingKey key);

  /**
   * Returns the value of the user setting specified by the given name.
   *
   * @param key the user setting key.
   * @param username the user.
   * @return the value corresponding to the named user setting, or null if there is no match.
   */
  Serializable getUserSetting(UserSettingKey key, String username);

  /**
   * Retrieves UserSettings for the given User.
   *
   * @param user the User.
   * @return a List of UserSettings.
   */
  UserSettingsDto getUserSettings(String username);

  /**
   * Returns all specified user settings. If any user settings have not been set, system settings
   * will be used as a fallback.
   *
   * @param userSettingKeys the set of user settings to retrieve
   * @return a map of setting names and their values
   */
  Map<String, Serializable> getUserSettingsWithFallbackByUserAsMap(
      User user, Set<UserSettingKey> userSettingKeys, boolean useFallback);

  /**
   * Returns all user settings for currently logged in user. Setting will not be included in map if
   * its value is null.
   *
   * @return a map of setting names and their values
   */
  Map<String, Serializable> getUserSettingsAsMap(User user);
}
