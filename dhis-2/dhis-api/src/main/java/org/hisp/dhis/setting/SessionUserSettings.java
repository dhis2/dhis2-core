/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUsername;
import static org.hisp.dhis.user.CurrentUserUtil.hasCurrentUser;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Manages the {@link ThreadLocal} state for {@link UserSettings} that cannot be included in a
 * non-public way in the interface.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
public final class SessionUserSettings {

  private SessionUserSettings() {
    throw new UnsupportedOperationException("util");
  }

  private static final ThreadLocal<UserSettings> REQUEST_CURRENT_USER_SETTINGS =
      new ThreadLocal<>();

  /** {@link UserSettings} for users with an active session */
  private static final Map<String, UserSettings> SESSION_USER_SETTINGS_BY_USERNAME =
      new ConcurrentHashMap<>();

  private static final UserSettings EMPTY = UserSettings.of(Map.of());

  @Nonnull
  static UserSettings getCurrentUserSettings() {
    UserSettings settings = REQUEST_CURRENT_USER_SETTINGS.get();
    if (settings == null) {
      settings =
          hasCurrentUser()
              ? SESSION_USER_SETTINGS_BY_USERNAME.getOrDefault(
                  getCurrentUsername(), UserSettings.of(Map.of()))
              : EMPTY;
      REQUEST_CURRENT_USER_SETTINGS.set(settings);
    }
    return settings;
  }

  /**
   * Removes the use {@link UserSettings} instance from the current thread. This happens at the end
   * of each request.
   */
  public static void clearCurrentUserSettings() {
    REQUEST_CURRENT_USER_SETTINGS.remove();
  }

  /**
   * Allows to overlay the current user's settings with overrides for the scope of the current
   * request. This does not change the user's setting in the session nor the settings stored in the
   * DB.
   *
   * @param settings the overrides to apply on top of the user's session held settings
   */
  public static void overrideCurrentUserSettings(Map<String, String> settings) {
    REQUEST_CURRENT_USER_SETTINGS.set(getCurrentUserSettings().withOverride(settings));
  }

  public static void clear(@Nonnull String username) {
    SESSION_USER_SETTINGS_BY_USERNAME.remove(username);
  }

  public static void put(@Nonnull String username, @CheckForNull UserSettings settings) {
    if (settings == null) {
      clear(username);
    } else {
      SESSION_USER_SETTINGS_BY_USERNAME.put(username, settings);
    }
  }

  public static Optional<UserSettings> get(@Nonnull String username) {
    UserSettings settings = SESSION_USER_SETTINGS_BY_USERNAME.get(username);
    return settings == null ? Optional.empty() : Optional.of(settings);
  }
}
