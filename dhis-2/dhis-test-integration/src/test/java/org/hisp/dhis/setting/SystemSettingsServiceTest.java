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
package org.hisp.dhis.setting;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
    settingsService.deleteSystemSettings(settingsService.getCurrentSettings().keys());
    // this is required since creating a user to test with indirectly initializes
    // the settings in the current thread
    settingsService.clearCurrentSettings();
  }

  @Test
  void testSaveSystemSettings() {
    settingsService.saveSystemSettings(Map.of("k0", "v0", "k1", "v1"));

    SystemSettings settings = settingsService.getCurrentSettings();
    assertEquals(Set.of("k0", "k1"), settings.keys());
    assertEquals(Map.of("k0", "v0", "k1", "v1"), settings.toMap());
  }

  @Test
  void testSaveSystemSetting_Date() {
    settingsService.saveSystemSetting("keyDate", new Date(123456L));

    SystemSettings settings = settingsService.getCurrentSettings();
    assertEquals(new Date(123456L), settings.asDate("keyDate", new Date(0L)));
  }

  @Test
  void testSaveSystemSetting_Boolean() {
    settingsService.saveSystemSetting("keyBoolean", true);

    SystemSettings settings = settingsService.getCurrentSettings();
    assertTrue(settings.asBoolean("keyBoolean", false));
  }

  @Test
  void testSaveSystemSetting_Int() {
    settingsService.saveSystemSetting("keyInt", 42);

    SystemSettings settings = settingsService.getCurrentSettings();
    assertEquals(42, settings.asInt("keyInt", -1));
  }

  @Test
  void testSaveSystemSetting_Locale() {
    settingsService.saveSystemSetting("keyLocale", Locale.forLanguageTag("en-GB"));

    SystemSettings settings = settingsService.getCurrentSettings();
    assertEquals(Locale.forLanguageTag("en-GB"), settings.asLocale("keyLocale", Locale.FRENCH));
  }

  @Test
  void testDeleteSystemSettings() {
    Map<String, String> allSettings =
        IntStream.range(0, 20)
            .mapToObj(String::valueOf)
            .collect(toMap(i -> "key" + i, i -> "value" + i));
    settingsService.saveSystemSettings(allSettings);

    Set<String> deletedKeys =
        Stream.of(1, 4, 5, 7, 9, 12, 16, 32, 64).map(i -> "key" + i).collect(toSet());
    settingsService.deleteSystemSettings(deletedKeys);

    SystemSettings settings = settingsService.getCurrentSettings();
    Set<String> expectedKeys =
        Stream.of(0, 2, 3, 6, 8, 10, 11, 13, 14, 15, 17, 18, 19)
            .map(i -> "key" + i)
            .collect(toSet());
    assertEquals(expectedKeys, settings.keys());
  }
}
