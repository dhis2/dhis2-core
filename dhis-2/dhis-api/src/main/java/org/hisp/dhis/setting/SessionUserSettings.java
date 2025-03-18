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
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Manages the {@link UserSettings} per session.
 *
 * <p>This state is managed from the outside by a service as this is based on spring events that
 * cannot be obtained statically.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SessionUserSettings {

  /** {@link UserSettings} for users with an active session */
  private static final Map<String, UserSettings> SESSION_USER_SETTINGS_BY_USERNAME =
      new ConcurrentHashMap<>();

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
