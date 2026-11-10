/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.DisplayProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests how {@link SessionUserSettings} combines the settings stored for a user with the current
 * {@link SystemSettings}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class SessionUserSettingsTest {

  private static final String USERNAME = "session-user-settings-test";

  private final Map<String, String> systemSettings = new HashMap<>();
  private final Map<String, Map<String, String>> storedSettings = new HashMap<>();

  @BeforeEach
  void setUp() {
    SessionUserSettings.setSource(
        new SessionUserSettings.Source() {
          @Nonnull
          @Override
          public UserSettings getStoredSettings(@Nonnull String username) {
            return UserSettings.of(storedSettings.getOrDefault(username, Map.of()));
          }

          @Nonnull
          @Override
          public Map<String, String> getFallbacks() {
            return Map.copyOf(systemSettings);
          }
        });
  }

  @AfterEach
  void tearDown() {
    SessionUserSettings.clear(USERNAME);
    SessionUserSettings.setSource(null);
  }

  @Test
  void testGet_SystemSettingChangeIsVisibleInSession() {
    SessionUserSettings.put(USERNAME, UserSettings.of(Map.of()));

    systemSettings.put("keyUiLocale", "fr");
    assertEquals("fr", getEffective().getUserUiLocale().toString());

    systemSettings.put("keyUiLocale", "nb");
    assertEquals("nb", getEffective().getUserUiLocale().toString());

    systemSettings.remove("keyUiLocale");
    assertEquals("en", getEffective().getUserUiLocale().toString());
  }

  @Test
  void testGet_StoredValueWinsOverSystemSetting() {
    SessionUserSettings.put(USERNAME, UserSettings.of(Map.of("keyUiLocale", "de")));
    systemSettings.put("keyUiLocale", "fr");

    assertEquals("de", getEffective().getUserUiLocale().toString());
  }

  @Test
  void testGet_LoadsStoredSettingsWhenNotHeld() {
    // e.g. another session of the same user ended, or the session started on another server
    storedSettings.put(USERNAME, Map.of("keyUiLocale", "de"));
    systemSettings.put("keyAnalysisDisplayProperty", "shortName");

    UserSettings settings = getEffective();
    assertEquals("de", settings.getUserUiLocale().toString());
    assertEquals(DisplayProperty.SHORTNAME, settings.getUserAnalysisDisplayProperty());
  }

  private UserSettings getEffective() {
    return SessionUserSettings.get(USERNAME).orElseThrow();
  }
}
