/*
 * Copyright (c) 2004-2022, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jan Bernitt
 */
@TestInstance(Lifecycle.PER_CLASS)
class SystemSettingsServiceTest extends PostgresIntegrationTestBase {

  @Autowired private SystemSettingsService settingsService;

  @BeforeEach
  void setUp() {
    // just to be sure since the test itself is not transactional
    settingsService.deleteAll(settingsService.getCurrentSettings().keys());
    // this is required since creating a user to test with indirectly initializes
    // the settings in the current thread
    settingsService.clearCurrentSettings();
  }

  @Test
  void testSaveSystemSettings() throws Exception {
    settingsService.putAll(Map.of("keyUiLocale", "de", "keyDbLocale", "fr"));

    SystemSettings settings = settingsService.getCurrentSettings();
    assertEquals(Set.of("keyUiLocale", "keyDbLocale"), settings.keys());
    assertEquals(Map.of("keyUiLocale", "de", "keyDbLocale", "fr"), settings.toMap());
  }

  @Test
  void testSaveSystemSetting_Date() {
    settingsService.put("lastSuccessfulDataStatistics", new Date(123456L));

    SystemSettings settings = settingsService.getCurrentSettings();
    assertEquals(new Date(123456L), settings.asDate("lastSuccessfulDataStatistics", new Date(0L)));
  }

  @Test
  void testSaveSystemSetting_Boolean() {
    settingsService.put("keyEmailTls", true);

    SystemSettings settings = settingsService.getCurrentSettings();
    assertTrue(settings.asBoolean("keyEmailTls", false));
  }

  @Test
  void testSaveSystemSetting_Int() {
    settingsService.put("maxPasswordLength", 42);

    SystemSettings settings = settingsService.getCurrentSettings();
    assertEquals(42, settings.asInt("maxPasswordLength", -1));
  }

  @Test
  void testSaveSystemSetting_Locale() {
    settingsService.put("keyUiLocale", Locale.forLanguageTag("en-GB"));

    SystemSettings settings = settingsService.getCurrentSettings();
    assertEquals(Locale.forLanguageTag("en-GB"), settings.asLocale("keyUiLocale", Locale.FRENCH));
  }

  @Test
  void testSaveSystemSetting_Double() {
    settingsService.put("factorDeviation", 42.5d);

    SystemSettings settings = settingsService.getCurrentSettings();
    assertEquals(42.5d, settings.asDouble("factorDeviation", -1));
  }

  @Test
  void testDeleteSystemSettings() throws Exception {
    Map<String, String> allSettings =
        Map.ofEntries(
            Map.entry("keyCustomJs", "JS"),
            Map.entry("keyCustomCss", "CSS"),
            Map.entry("keyStyle", "style"));
    settingsService.putAll(allSettings);

    Set<String> deletedKeys = Set.of("keyCustomJs", "keyStyle");
    settingsService.deleteAll(deletedKeys);

    SystemSettings settings = settingsService.getCurrentSettings();
    assertEquals(Set.of("keyCustomCss"), settings.keys());
  }
}
