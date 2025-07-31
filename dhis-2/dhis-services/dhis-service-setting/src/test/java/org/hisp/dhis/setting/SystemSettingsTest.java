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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsCacheTtlMode;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.jsontree.JsonBoolean;
import org.hisp.dhis.jsontree.JsonDate;
import org.hisp.dhis.jsontree.JsonInteger;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonPrimitive;
import org.hisp.dhis.jsontree.JsonString;
import org.junit.jupiter.api.Test;

/**
 * Tests the basics of {@link SystemSettings}.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
class SystemSettingsTest {

  private static final List<String> CONFIDENTIAL_KEYS =
      List.of("keyEmailPassword", "keyRemoteInstancePassword", "recaptchaSite", "recaptchaSecret");

  private static final List<String> TRANSLATABLE_KEYS =
      List.of(
          "applicationTitle",
          "keyApplicationIntro",
          "keyApplicationNotification",
          "keyApplicationFooter",
          "keyApplicationRightFooter",
          "loginPopup");

  @Test
  void testIsConfidential() {
    CONFIDENTIAL_KEYS.forEach(
        key ->
            assertTrue(
                SystemSettings.isConfidential(key), "%s should be confidential".formatted(key)));

    assertFalse(SystemSettings.isConfidential("keyEmailHostName"));
  }

  @Test
  void testIsTranslatable() {
    TRANSLATABLE_KEYS.forEach(
        key ->
            assertTrue(
                SystemSettings.isTranslatable(key), "%s should be translatable".formatted(key)));

    assertFalse(SystemSettings.isTranslatable("recaptchaSite"));
  }

  @Test
  void testKeysWithDefaults() {
    Set<String> keys = SystemSettings.keysWithDefaults();
    assertEquals(148, keys.size());
    // just check some at random
    assertTrue(keys.contains("syncSkipSyncForDataChangedBefore"));
    assertTrue(keys.contains("keyTrackerDashboardLayout"));
    assertTrue(keys.contains("experimentalAnalyticsSqlEngineEnabled"));
    assertTrue(keys.contains("notifierGistOverview"));
  }

  @Test
  void testToJson() {
    SystemSettings settings = SystemSettings.of(Map.of("applicationTitle", "Hello World"));
    JsonMap<JsonMixed> asJson = settings.toJson(false);
    // it does contain the set value
    JsonPrimitive stringValue = asJson.get("applicationTitle");
    assertTrue(stringValue.isString());
    assertEquals("Hello World", stringValue.as(JsonString.class).string());
    // but also all defaults (test some)
    JsonPrimitive intValue = asJson.get("keyParallelJobsInAnalyticsTableExport");
    assertTrue(intValue.isNumber());
    assertEquals(-1, intValue.as(JsonInteger.class).intValue());
    JsonPrimitive booleanValue = asJson.get("startModuleEnableLightweight");
    assertTrue(booleanValue.isBoolean());
    assertFalse(booleanValue.as(JsonBoolean.class).booleanValue());
    JsonString dateValue = asJson.get("keyLastMetaDataSyncSuccess");
    assertTrue(dateValue.isString());
    assertEquals(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()),
        dateValue.as(JsonDate.class).date());
    JsonString enumValue = asJson.get("keyCacheStrategy");
    assertTrue(enumValue.isString());
    assertEquals(CacheStrategy.CACHE_1_MINUTE, enumValue.parsed(CacheStrategy::valueOf));
    // except none of the confidential ones
    CONFIDENTIAL_KEYS.forEach(key -> assertFalse(asJson.exists(key)));
  }

  @Test
  void testKeys() {
    assertEquals(Set.of("a", "b"), SystemSettings.of(Map.of("a", "1", "b", "2")).keys());
  }

  @Test
  void testAsEnum() {
    assertEquals(
        AnalyticsCacheTtlMode.PROGRESSIVE,
        SystemSettings.of(Map.of("keyAnalyticsCacheTtlMode", "PROGRESSIVE"))
            .getAnalyticsCacheTtlMode());
  }

  @Test
  void testAsInt() {
    assertEquals(
        42,
        SystemSettings.of(Map.of("keyAnalyticsPeriodYearsOffset", "42"))
            .getAnalyticsPeriodYearsOffset());
  }

  @Test
  void testAsBoolean() {
    assertTrue(
        SystemSettings.of(Map.of("keyAcceptanceRequiredForApproval", "true"))
            .getAcceptanceRequiredForApproval());
  }

  @Test
  void testAsDate() {
    SystemSettings settings =
        SystemSettings.of(
            Map.of("keyLastSuccessfulResourceTablesUpdate", Settings.valueOf(new Date(123456L))));
    assertEquals(new Date(123456L), settings.getLastSuccessfulResourceTablesUpdate());
  }

  @Test
  void testAsLocale() {
    assertEquals(
        Locale.forLanguageTag("fr"), SystemSettings.of(Map.of("keyUiLocale", "fr")).getUiLocale());
  }

  @Test
  void testIsValid_Boolean() {
    SystemSettings settings = SystemSettings.of(Map.of());
    assertTrue(settings.isValid("keyEmailTls", "true"));
    assertTrue(settings.isValid("keyEmailTls", "false"));
    assertFalse(settings.isValid("keyEmailTls", "hello"));
    assertFalse(settings.isValid("keyEmailTls", "1"));
  }

  @Test
  void testIsValid_Int() {
    SystemSettings settings = SystemSettings.of(Map.of());
    assertTrue(settings.isValid("keyEmailPort", "1"));
    assertTrue(settings.isValid("keyEmailPort", "42"));
    assertTrue(settings.isValid("keyEmailPort", "-567"));
    assertFalse(settings.isValid("keyEmailPort", "hello"));
    assertFalse(settings.isValid("keyEmailPort", "true"));
  }

  @Test
  void testIsValid_Double() {
    SystemSettings settings = SystemSettings.of(Map.of());
    assertTrue(settings.isValid("factorDeviation", "1"));
    assertTrue(settings.isValid("factorDeviation", "42.5"));
    assertTrue(settings.isValid("factorDeviation", "-0.567"));
    assertFalse(settings.isValid("factorDeviation", "hello"));
    assertFalse(settings.isValid("factorDeviation", "true"));
  }

  @Test
  void testIsValid_Locale() {
    SystemSettings settings = SystemSettings.of(Map.of());
    assertTrue(settings.isValid("keyDbLocale", "fr"));
    assertTrue(settings.isValid("keyDbLocale", "de_DE"));
    assertFalse(settings.isValid("keyDbLocale", "hello"));
    assertFalse(settings.isValid("keyDbLocale", "true"));
    assertFalse(settings.isValid("keyDbLocale", "42"));
  }

  @Test
  void testIsValid_Enum() {
    SystemSettings settings = SystemSettings.of(Map.of());
    assertTrue(settings.isValid("keyCacheStrategy", "NO_CACHE"));
    assertTrue(settings.isValid("keyCacheStrategy", "CACHE_1_HOUR"));
    assertFalse(settings.isValid("keyCacheStrategy", "hello"));
    assertFalse(settings.isValid("keyCacheStrategy", "true"));
    assertFalse(settings.isValid("keyCacheStrategy", "42"));
  }

  @Test
  void testIsValid_Date() {
    SystemSettings settings = SystemSettings.of(Map.of());
    assertTrue(settings.isValid("keyLastMonitoringRun", "42789654"));
    String date = LocalDateTime.of(2020, 12, 12, 0, 0).toString();
    assertTrue(settings.isValid("keyLastMonitoringRun", date));
    assertFalse(settings.isValid("keyLastMonitoringRun", "hello"));
    assertFalse(settings.isValid("keyLastMonitoringRun", "true"));
  }

  @Test
  void testIsValid_Reset() {
    SystemSettings settings = SystemSettings.of(Map.of("keyCacheStrategy", "NO_CACHE"));
    assertEquals(CacheStrategy.NO_CACHE, settings.getCacheStrategy());
    assertTrue(settings.isValid("keyCacheStrategy", ""));
    assertTrue(settings.isValid("keyCacheStrategy", null));
  }

  @Test
  void testEmailIsConfigured() {
    SystemSettings settings = SystemSettings.of(Map.of());
    assertFalse(settings.isEmailConfigured());
    settings =
        SystemSettings.of(Map.of("keyEmailHostName", "localhost", "keyEmailUsername", "user"));
    assertTrue(settings.isEmailConfigured());
  }
}
