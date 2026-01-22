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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the DHIS2 {@link Locale}.
 *
 * @author Jan Bernitt
 */
class LocaleTest {

  @Test
  void testLocaleOf_Language() {
    assertLocaleEquals("en", Locale.of("en"));
    assertLocaleEquals("eng", Locale.of("eng"));
    assertLocaleEquals("id", Locale.of("in"));
    assertLocaleEquals("id", Locale.of("id"));
  }

  @Test
  void testLocaleOf_Language_Region() {
    assertLocaleEquals("en_US", Locale.of("en_US"));
    assertLocaleEquals("eng_US", Locale.of("eng_US"));
    assertLocaleEquals("en_840", Locale.of("en_840"));
    assertLocaleEquals("eng_840", Locale.of("eng_840"));
    assertLocaleEquals("id_ID", Locale.of("in_ID"));
    assertLocaleEquals("id_ID", Locale.of("id_ID"));

    // BCP47
    assertLocaleEquals("en_US", Locale.of("en-US"));
    assertLocaleEquals("eng_US", Locale.of("eng-US"));
    assertLocaleEquals("en_840", Locale.of("en-840"));
    assertLocaleEquals("eng_840", Locale.of("eng-840"));
    assertLocaleEquals("id_ID", Locale.of("in-ID"));
    assertLocaleEquals("id_ID", Locale.of("id-ID"));
  }

  @Test
  void testLocaleOf_Language_Region_Script() {
    assertLocaleEquals("en_US_Latn", Locale.of("en_US_Latn"));
    assertLocaleEquals("eng_US_Latn", Locale.of("eng_US_Latn"));
    assertLocaleEquals("en_840_Latn", Locale.of("en_840_Latn"));
    assertLocaleEquals("eng_840_Latn", Locale.of("eng_840_Latn"));
    assertLocaleEquals("id_ID_Latn", Locale.of("in_ID_Latn"));
    assertLocaleEquals("id_ID_Latn", Locale.of("id_ID_Latn"));

    // with #
    assertLocaleEquals("en_US_Latn", Locale.of("en_US_#Latn"));
    assertLocaleEquals("eng_US_Latn", Locale.of("eng_US_#Latn"));
    assertLocaleEquals("en_840_Latn", Locale.of("en_840_#Latn"));
    assertLocaleEquals("eng_840_Latn", Locale.of("eng_840_#Latn"));
    assertLocaleEquals("id_ID_Latn", Locale.of("in_ID_#Latn"));
    assertLocaleEquals("id_ID_Latn", Locale.of("id_ID_#Latn"));

    // BCP47
    assertLocaleEquals("en_US_Latn", Locale.of("en-Latn-US"));
    assertLocaleEquals("eng_US_Latn", Locale.of("eng-Latn-US"));
    assertLocaleEquals("en_840_Latn", Locale.of("en-Latn-840"));
    assertLocaleEquals("eng_840_Latn", Locale.of("eng-Latn-840"));
    assertLocaleEquals("id_ID_Latn", Locale.of("in-Latn-ID"));
    assertLocaleEquals("id_ID_Latn", Locale.of("id-Latn-ID"));
  }

  @Test
  void testLocaleOf() {
    List<String> valid =
        List.of(
            "en-US", // (English, United States)
            "en-GB", // (English, United Kingdom)
            "es-ES", // (Spanish, Spain)
            "fr-FR", // (French, France)
            "de-DE", // (German, Germany)
            "zh-CN", // (Chinese, China - Simplified)
            "zh-TW", // (Chinese, Taiwan - Traditional)
            "ja-JP", // (Japanese, Japan)
            "ko-KR", // (Korean, South Korea)
            "ru-RU", // (Russian, Russia)
            "ar-SA", // (Arabic, Saudi Arabia)
            "hi-IN", // (Hindi, India)
            "pt-BR", // (Portuguese, Brazil)
            "it-IT", // (Italian, Italy)

            // scripts
            "zh-Hans-CN", // (Chinese, Simplified script, China)
            "zh-Hant-TW", // (Chinese, Traditional script, Taiwan)
            "sr-Latn-RS", // (Serbian, Latin script, Serbia)
            "sr-Cyrl-RS", // (Serbian, Cyrillic script, Serbia)
            "az-Latn-AZ", // (Azerbaijani, Latin script, Azerbaijan)
            "az-Cyrl-AZ", // (Azerbaijani, Cyrillic script, Azerbaijan)
            "uz-Latn-UZ", // (Uzbek, Latin script, Uzbekistan)
            "uz-Cyrl-UZ" // (Uzbek, Cyrillic script, Uzbekistan)
            );
    for (String l : valid) assertDoesNotThrow(() -> Locale.of(l));
  }

  @Test
  void testLocaleOf_LanguageTooShort() {
    assertLocalThrows("Invalid locale: s", "s");
  }

  @Test
  void testLocaleOf_LanguageTooLong() {
    assertLocalThrows("Invalid locale: wham", "wham");
  }

  @Test
  void testLocaleOf_LanguageUpperCase() {
    assertLocalThrows("Invalid language code: US", "US");
  }

  @Test
  void testLocaleOf_RegionTooShort() {
    assertLocalThrows("Invalid region code: S", "en-S");
  }

  @Test
  void testLocaleOf_RegionTooLong() {
    assertLocalThrows("Invalid region code: WHAM", "en_WHAM");
  }

  @Test
  void testLocaleOf_RegionLowerCase() {
    assertLocalThrows("Invalid region code: en", "en-en");
  }

  @Test
  void testLocaleOf_RegionNoDash() {
    assertLocalThrows("Invalid locale: enxEN", "enxEN");
  }

  @Test
  void testLocaleOf_ScriptTooShort() {
    assertLocalThrows("Invalid script: Ltn", "en-EN-Ltn");
  }

  @Test
  void testLocaleOf_ScriptTooLong() {
    assertLocalThrows("Invalid script: Latin", "en_EN_Latin");
  }

  @Test
  void testLocaleOf_ScriptLowerCase() {
    assertLocalThrows("Invalid script: latn", "en-EN_latn");
  }

  @Test
  void testLocaleOf_ScriptUpperCase() {
    assertLocalThrows("Invalid script: LATN", "en-EN_LATN");
  }

  @Test
  void testLocaleOf_ScriptNoDash() {
    assertLocalThrows("Invalid region code: ENxLatn", "en_ENxLatn");
  }

  private void assertLocaleEquals(String expected, Locale actual) {
    assertEquals(expected, actual.toString());
    String[] parts = expected.split("_");
    assertEquals(parts[0], actual.language());
    if (parts.length >= 2) assertEquals(parts[1], actual.region());
    if (parts.length >= 3) assertEquals(parts[2], actual.script());
  }

  private void assertLocalThrows(String expected, String actual) {
    IllegalArgumentException ex =
        assertThrowsExactly(IllegalArgumentException.class, () -> Locale.of(actual));
    assertEquals(expected, ex.getMessage());
  }
}
