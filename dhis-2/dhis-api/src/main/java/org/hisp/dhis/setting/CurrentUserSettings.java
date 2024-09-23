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

import java.util.Map;
import javax.annotation.Nonnull;
import org.hisp.dhis.user.CurrentUserUtil;

/**
 * Manages the {@link ThreadLocal} state for {@link UserSettings} that cannot be included in a
 * non-public way in the interface.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
final class CurrentUserSettings {

  static final ThreadLocal<UserSettings> CURRENT_USER_SETTINGS = new ThreadLocal<>();

  @Nonnull
  static UserSettings getCurrentSettings() {
    UserSettings settings = CURRENT_USER_SETTINGS.get();
    if (settings == null) {
      settings =
          CurrentUserUtil.hasCurrentUser()
              ? CurrentUserUtil.getCurrentUserDetails().getUserSettings()
              : UserSettings.of(Map.of());
      CURRENT_USER_SETTINGS.set(settings);
    }
    return settings;
  }

  static void clearCurrentSettings() {
    CURRENT_USER_SETTINGS.remove();
  }

  static void overrideCurrentSettings(Map<String, String> settings) {
    CURRENT_USER_SETTINGS.set(getCurrentSettings().withOverlay(settings));
  }
}
