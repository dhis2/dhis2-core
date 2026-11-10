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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.http.HttpClientAdapter.HttpResponse;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the {@link UserSettingsController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class UserSettingsControllerTest extends H2ControllerIntegrationTestBase {

  @AfterEach
  void resetSettings() {
    DELETE("/userSettings/keyUiLocale");
    DELETE("/systemSettings/keyUiLocale");
  }

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

  @Test
  void testGetAllUserSettings_NoFallbackReturnsNullForUnset() {
    assertStatus(HttpStatus.OK, POST("/systemSettings/keyUiLocale?value=fr"));

    JsonObject settings = GET("/userSettings?useFallback=false").content();
    UserSettings.keysWithDefaults()
        .forEach(key -> assertTrue(settings.get(key).exists(), "expected " + key));
    assertTrue(settings.get("keyUiLocale").isNull(), "unset setting must not report a default");

    // an explicit value equal to the default is still reported as set
    assertStatus(HttpStatus.OK, POST("/userSettings/keyUiLocale?value=en"));
    assertEquals(
        "en",
        GET("/userSettings?useFallback=false&key=keyUiLocale")
            .content()
            .getString("keyUiLocale")
            .string());
  }

  /** The short form of a key (without the {@code key} prefix) refers to the same setting. */
  @ParameterizedTest
  @ValueSource(strings = {"keyUiLocale", "uiLocale"})
  void testGetAllUserSettings_NoFallbackByKey(String key) {
    assertStatus(HttpStatus.OK, POST("/systemSettings/keyUiLocale?value=fr"));

    JsonObject settings = GET("/userSettings?useFallback=false&key=" + key).content();
    assertEquals(List.of(key), settings.names());
    assertTrue(settings.get(key).isNull(), "unset setting must not report a default");

    assertStatus(HttpStatus.OK, POST("/userSettings/keyUiLocale?value=de"));
    settings = GET("/userSettings?useFallback=false&key=" + key).content();
    assertEquals("de", settings.getString(key).string());
  }

  @Test
  void testGetUserSettingByKey_NoFallbackHasNoValueForUnset() {
    assertStatus(HttpStatus.OK, POST("/systemSettings/keyUiLocale?value=fr"));
    assertEquals("fr", GET("/userSettings/keyUiLocale").content("text/plain"));

    HttpResponse unset = GET("/userSettings/keyUiLocale?useFallback=false");
    assertEquals(HttpStatus.OK, unset.status());
    assertEquals("", unset.content("text/plain"), "unset setting must not report a default");

    // an explicit value equal to the default is still reported as set
    assertStatus(HttpStatus.OK, POST("/userSettings/keyUiLocale?value=en"));
    assertEquals("en", GET("/userSettings/keyUiLocale?useFallback=false").content("text/plain"));
  }

  @Test
  void testSystemSettingChangeAppliesToSessionWithoutReLogin() {
    assertEquals("en", getEffectiveUiLocale());

    assertStatus(HttpStatus.OK, POST("/systemSettings/keyUiLocale?value=fr"));
    assertEquals("fr", getEffectiveUiLocale());

    assertStatus(HttpStatus.OK, POST("/systemSettings/keyUiLocale?value=nb"));
    assertEquals("nb", getEffectiveUiLocale());
  }

  @Test
  void testSystemSettingChangeDoesNotOverrideUserValue() {
    assertStatus(HttpStatus.OK, POST("/userSettings/keyUiLocale?value=de"));
    assertStatus(HttpStatus.OK, POST("/systemSettings/keyUiLocale?value=fr"));
    assertEquals("de", getEffectiveUiLocale());

    // "Use system default"
    DELETE("/userSettings/keyUiLocale");
    assertEquals("fr", getEffectiveUiLocale());
  }

  /** Asserts both sources apps read agree, then returns the UI locale they report. */
  private String getEffectiveUiLocale() {
    String fromUserSettings = GET("/userSettings/keyUiLocale").content("text/plain");
    String fromMe =
        GET("/me?fields=settings[keyUiLocale]")
            .content()
            .getObject("settings")
            .getString("keyUiLocale")
            .string();
    assertEquals(fromUserSettings, fromMe, "/userSettings and /me disagree");
    return fromMe;
  }
}
