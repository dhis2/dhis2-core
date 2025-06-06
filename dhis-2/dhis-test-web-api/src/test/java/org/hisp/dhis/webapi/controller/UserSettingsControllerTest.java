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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link UserSettingsController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class UserSettingsControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testGetAllUserSettings() {
    JsonObject settings = GET("/userSettings").content();
    assertTrue(settings.isObject());
    UserSettings.keysWithDefaults()
        .forEach(key -> assertTrue(settings.get(key).exists(), "expected " + key));
  }

  @Test
  void testGetAllUserSettings_ByKey() {
    JsonObject settings = GET("/userSettings?key=keyMessageSmsNotification").content();
    assertTrue(settings.isObject());
    assertEquals(1, settings.size());
    assertEquals(List.of("keyMessageSmsNotification"), settings.names());

    settings = GET("/userSettings?key=keyMessageSmsNotification&key=keyUiLocale").content();
    assertTrue(settings.isObject());
    assertEquals(2, settings.size());
    assertEquals(Set.of("keyMessageSmsNotification", "keyUiLocale"), Set.copyOf(settings.names()));
  }

  @Test
  void testGetUserSettingByKey() {
    assertEquals("en", GET("/userSettings/keyUiLocale").content("text/plain"));
  }
}
