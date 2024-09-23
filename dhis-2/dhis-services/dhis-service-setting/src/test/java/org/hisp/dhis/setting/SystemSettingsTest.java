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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsCacheTtlMode;
import org.hisp.dhis.jsontree.JsonInteger;
import org.hisp.dhis.jsontree.JsonMap;
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

  @Test
  void testIsConfidential() {

    CONFIDENTIAL_KEYS.forEach(
        key ->
            assertTrue(
                SystemSettings.isConfidential(key), "%s should be confidential".formatted(key)));

    assertFalse(SystemSettings.isConfidential("keyEmailHostName"));
  }

  @Test
  void testKeysWithDefaults() {
    Set<String> keys = SystemSettings.keysWithDefaults();
    assertEquals(135, keys.size());
    // just check some at random
    assertTrue(keys.contains("syncSkipSyncForDataChangedBefore"));
    assertTrue(keys.contains("keyTrackerDashboardLayout"));
  }

  @Test
  void testToJson() {
    SystemSettings settings = SystemSettings.of(Map.of("applicationTitle", "Hello World"));
    JsonMap<? extends JsonPrimitive> asJson = settings.toJson();
    // it does contain the custom value
    assertEquals("Hello World", asJson.get("applicationTitle").as(JsonString.class).string());
    // but also all defaults (test one)
    assertEquals(
        -1, asJson.get("keyParallelJobsInAnalyticsTableExport").as(JsonInteger.class).intValue());
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
}
