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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Manages the {@link UserSettings} of users with an active session.
 *
 * <p>Only the settings stored for a user are held in memory. The {@link SystemSettings} fallbacks
 * are applied each time the effective settings are requested, so a change to a system setting is
 * visible right away to every user that has no value of their own for that setting.
 *
 * <p>This state is managed from the outside by a service as this is based on spring events and
 * stores that cannot be obtained statically. The service provides the {@link Source} to load the
 * stored settings of a user that is not held in memory, for example because another session of the
 * same user ended or because the session was started on another server.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SessionUserSettings {

  /** Provides the settings data that is not held in memory. */
  public interface Source {

    /**
     * @param username of the user
     * @return the settings stored for the user, without any fallbacks
     */
    @Nonnull
    UserSettings getStoredSettings(@Nonnull String username);

    /**
     * @return the current system settings that apply for any setting the user has not stored
     */
    @Nonnull
    Map<String, String> getFallbacks();
  }

  /** Stored {@link UserSettings} (without fallbacks) for users with an active session */
  private static final Map<String, UserSettings> STORED_USER_SETTINGS_BY_USERNAME =
      new ConcurrentHashMap<>();

  /**
   * The source of the running application context, set by its service on startup. Tests that run
   * several application contexts in one JVM set the source of their own context for each test.
   */
  private static final AtomicReference<Source> SOURCE = new AtomicReference<>();

  public static void setSource(@CheckForNull Source source) {
    SOURCE.set(source);
  }

  public static void clear(@Nonnull String username) {
    STORED_USER_SETTINGS_BY_USERNAME.remove(username);
  }

  /**
   * @param username of the user
   * @param settings the settings stored for the user, without fallbacks, or null to clear
   */
  public static void put(@Nonnull String username, @CheckForNull UserSettings settings) {
    if (settings == null) {
      clear(username);
    } else {
      STORED_USER_SETTINGS_BY_USERNAME.put(username, settings);
    }
  }

  /**
   * @param username of the user
   * @return the settings stored for the user (without fallbacks) if they are held in memory
   */
  public static Optional<UserSettings> getStored(@Nonnull String username) {
    return Optional.ofNullable(STORED_USER_SETTINGS_BY_USERNAME.get(username));
  }

  /**
   * @param username of the user
   * @return the effective settings of the user: the stored settings with the current system
   *     settings as fallback. Stored settings not held in memory are loaded from the {@link
   *     Source}. Empty only when no {@link Source} is set and no settings are held for the user.
   */
  public static Optional<UserSettings> get(@Nonnull String username) {
    Source src = SOURCE.get();
    UserSettings stored = STORED_USER_SETTINGS_BY_USERNAME.get(username);
    if (src == null) return Optional.ofNullable(stored);
    if (stored == null) {
      UserSettings loaded = src.getStoredSettings(username);
      UserSettings concurrent = STORED_USER_SETTINGS_BY_USERNAME.putIfAbsent(username, loaded);
      stored = concurrent == null ? loaded : concurrent;
    }
    return Optional.of(stored.withFallback(src.getFallbacks()));
  }
}
