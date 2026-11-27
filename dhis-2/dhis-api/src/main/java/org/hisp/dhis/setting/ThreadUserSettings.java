/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.setting;

import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUsername;
import static org.hisp.dhis.user.CurrentUserUtil.hasCurrentUser;

import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Manages the {@link ThreadLocal} state for {@link UserSettings} that cannot be included in a
 * non-public way in the interface.
 *
 * <p>The thread-state is backed by (initialized from) the {@link SessionUserSettings}.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadUserSettings {

  private static final ThreadLocal<UserSettings> REQUEST_CURRENT_USER_SETTINGS =
      new ThreadLocal<>();

  private static final UserSettings EMPTY = UserSettings.of(Map.of());

  @Nonnull
  static UserSettings get() {
    UserSettings settings = REQUEST_CURRENT_USER_SETTINGS.get();
    if (settings == null) {
      settings =
          hasCurrentUser() ? SessionUserSettings.get(getCurrentUsername()).orElse(EMPTY) : EMPTY;
      REQUEST_CURRENT_USER_SETTINGS.set(settings);
    }
    return settings;
  }

  /**
   * Removes the use {@link UserSettings} instance from the current thread. This happens at the end
   * of each request.
   */
  public static void clear() {
    REQUEST_CURRENT_USER_SETTINGS.remove();
  }

  /**
   * Returns the settings the current thread holds, so that they can be put back with {@link
   * #setHeld(UserSettings)} after a job ran on the thread. Unlike {@link
   * UserSettings#getCurrentSettings()} this does not load anything when no settings are held.
   *
   * @return the settings held for the current thread, or null when none are held
   */
  @CheckForNull
  public static UserSettings getHeld() {
    return REQUEST_CURRENT_USER_SETTINGS.get();
  }

  /**
   * Puts back the settings that {@link #getHeld()} returned before a job ran on the current thread.
   * This does not load anything.
   *
   * @param settings the settings to hold, null to hold none
   */
  public static void setHeld(@CheckForNull UserSettings settings) {
    if (settings == null) {
      clear();
    } else {
      REQUEST_CURRENT_USER_SETTINGS.set(settings);
    }
  }

  /**
   * Allows to overlay the current user's settings with overrides for the scope of the current
   * request. This does not change the user's setting in the session nor the settings stored in the
   * DB.
   *
   * @param settings the overrides to apply on top of the user's session held settings
   */
  public static void put(Map<String, String> settings) {
    REQUEST_CURRENT_USER_SETTINGS.set(get().withOverride(settings));
  }
}
