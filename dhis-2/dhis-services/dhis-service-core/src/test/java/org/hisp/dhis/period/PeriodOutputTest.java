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
package org.hisp.dhis.period;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.translation.Translation;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PeriodOutput}.
 *
 * <p>All output uses {@code excludeNullMembers: true}, so:
 *
 * <ul>
 *   <li>null string fields are omitted from the output rather than rendered as {@code null}
 *   <li>null {@code Integer} fields (e.g. {@code frequencyOrder}) are omitted
 *   <li>null or empty {@code translations} / {@code relativePeriods} are omitted entirely, not
 *       rendered as {@code []} / {@code {}}
 *   <li>if {@code type} is null, the {@code name} member is not written at all
 * </ul>
 *
 * <p>The {@code locale} value in the output is {@code Locale.toString()}, which may not be the
 * literal {@code "en"} depending on the {@code Locale} implementation — the assertions use {@code
 * LOCALE.toString()} in the expected string so they track the actual representation.
 */
class PeriodOutputTest {

  private static final Locale LOCALE = Locale.of("en");

  // ====== toJson(PeriodTypes.Output) — wrapper object ======

  @Test
  void testToJson_EmptyEntries() {
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of());
    assertEquals("{\"locale\":\"" + LOCALE + "\",\"periodTypes\":[]}", toJson(types));
  }

  @Test
  void testToJson_SingleFullEntry() {
    PeriodTypes.Output types =
        new PeriodTypes.Output(LOCALE, List.of(entry(PeriodTypeEnum.BI_MONTHLY)));
    assertEquals(
        """
        {"locale":"%s","periodTypes":[{"name":"BiMonthly","isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":1,"defaultName":"Default Name","label":"Label","displayLabel":"Display Label","displayName":"Display Name"}]}"""
            .formatted(LOCALE),
        toJson(types));
  }

  @Test
  void testToJson_MultipleEntries() {
    PeriodTypes.Output types =
        new PeriodTypes.Output(
            LOCALE,
            List.of(
                entry(PeriodTypeEnum.DAILY),
                entry(PeriodTypeEnum.MONTHLY),
                entry(PeriodTypeEnum.YEARLY)));
    assertEquals(
        """
        {"locale":"%s","periodTypes":[{"name":"Daily","isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":1,"defaultName":"Default Name","label":"Label","displayLabel":"Display Label","displayName":"Display Name"},{"name":"Monthly","isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":1,"defaultName":"Default Name","label":"Label","displayLabel":"Display Label","displayName":"Display Name"},{"name":"Yearly","isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":1,"defaultName":"Default Name","label":"Label","displayLabel":"Display Label","displayName":"Display Name"}]}"""
            .formatted(LOCALE),
        toJson(types));
  }

  @Test
  void testToJson_EntryWithNullType_OmitsName() {
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of(entry(null)));
    assertEquals(
        """
        {"locale":"%s","periodTypes":[{"isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":1,"defaultName":"Default Name","label":"Label","displayLabel":"Display Label","displayName":"Display Name"}]}"""
            .formatted(LOCALE),
        toJson(types));
  }

  @Test
  void testToJson_EntryAllNull_RendersEmptyObject() {
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of(emptyEntry()));
    assertEquals("{\"locale\":\"" + LOCALE + "\",\"periodTypes\":[{}]}", toJson(types));
  }

  @Test
  void testToJson_EntryWithNullFrequencyOrder_OmitsFrequencyOrder() {
    PeriodTypes.PeriodTypeEntry.Output e =
        new PeriodTypes.PeriodTypeEntry.Output(
            PeriodTypeEnum.DAILY,
            "Display Name",
            "P1M",
            "yyyyMM",
            null, // frequencyOrder
            "Default Name",
            "Label",
            "Display Label",
            null,
            null);
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of(e));
    assertEquals(
        """
        {"locale":"%s","periodTypes":[{"name":"Daily","isoDuration":"P1M","isoFormat":"yyyyMM","defaultName":"Default Name","label":"Label","displayLabel":"Display Label","displayName":"Display Name"}]}"""
            .formatted(LOCALE),
        toJson(types));
  }

  // ====== toJson(PeriodTypes.Output) — translations ======

  @Test
  void testToJson_EntryWithTranslations() {
    List<Translation> translations =
        List.of(
            new Translation(LOCALE, "name", "Nom"),
            new Translation(LOCALE, "description", "Description"));
    PeriodTypes.PeriodTypeEntry.Output e =
        new PeriodTypes.PeriodTypeEntry.Output(
            PeriodTypeEnum.MONTHLY,
            "Monthly",
            "P1M",
            "yyyyMM",
            5,
            "Monthly",
            "Monthly",
            "Monthly",
            translations,
            null);
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of(e));
    assertEquals(
        """
        {"locale":"%s","periodTypes":[{"name":"Monthly","isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":5,"defaultName":"Monthly","label":"Monthly","displayLabel":"Monthly","displayName":"Monthly","translations":[{"locale":"%s","property":"name","value":"Nom"},{"locale":"%s","property":"description","value":"Description"}]}]}"""
            .formatted(LOCALE, LOCALE, LOCALE),
        toJson(types));
  }

  @Test
  void testToJson_EntryWithEmptyTranslations_OmitsTranslations() {
    PeriodTypes.PeriodTypeEntry.Output e =
        new PeriodTypes.PeriodTypeEntry.Output(
            PeriodTypeEnum.DAILY,
            "d",
            "P1D",
            "yyyyMMdd",
            1,
            "d",
            "d",
            "d",
            List.of(), // empty, not null
            null);
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of(e));
    assertEquals(
        """
        {"locale":"%s","periodTypes":[{"name":"Daily","isoDuration":"P1D","isoFormat":"yyyyMMdd","frequencyOrder":1,"defaultName":"d","label":"d","displayLabel":"d","displayName":"d"}]}"""
            .formatted(LOCALE),
        toJson(types));
  }

  // ====== toJson(PeriodTypes.Output) — relativePeriods ======

  @Test
  void testToJson_EntryWithRelativePeriods() {
    Map<RelativePeriodEnum, PeriodTypes.Labels> relativePeriods = new LinkedHashMap<>();
    relativePeriods.put(
        RelativePeriodEnum.TODAY,
        new PeriodTypes.Labels(
            "Today Default",
            "Today Label",
            List.of(),
            "Today Display Label",
            "Today Display Name"));
    PeriodTypes.PeriodTypeEntry.Output e =
        new PeriodTypes.PeriodTypeEntry.Output(
            PeriodTypeEnum.DAILY,
            "Daily",
            "P1D",
            "yyyyMMdd",
            1,
            "Daily",
            "Daily",
            "Daily",
            null,
            relativePeriods);
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of(e));
    assertEquals(
        """
        {"locale":"%s","periodTypes":[{"name":"Daily","isoDuration":"P1D","isoFormat":"yyyyMMdd","frequencyOrder":1,"defaultName":"Daily","label":"Daily","displayLabel":"Daily","displayName":"Daily","relativePeriods":{"TODAY":{"defaultName":"Today Default","label":"Today Label","displayLabel":"Today Display Label","displayName":"Today Display Name"}}}]}"""
            .formatted(LOCALE),
        toJson(types));
  }

  @Test
  void testToJson_EntryWithRelativePeriodsPreservesOrder() {
    Map<RelativePeriodEnum, PeriodTypes.Labels> relativePeriods = new LinkedHashMap<>();
    relativePeriods.put(RelativePeriodEnum.TODAY, labels("Today"));
    relativePeriods.put(RelativePeriodEnum.YESTERDAY, labels("Yesterday"));
    relativePeriods.put(RelativePeriodEnum.THIS_YEAR, labels("This Year"));
    PeriodTypes.PeriodTypeEntry.Output e =
        new PeriodTypes.PeriodTypeEntry.Output(
            PeriodTypeEnum.DAILY,
            "Daily",
            "P1D",
            "yyyyMMdd",
            1,
            "Daily",
            "Daily",
            "Daily",
            null,
            relativePeriods);
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of(e));
    String json = toJson(types);
    int today = json.indexOf("\"TODAY\":");
    int yesterday = json.indexOf("\"YESTERDAY\":");
    int thisYear = json.indexOf("\"THIS_YEAR\":");
    org.junit.jupiter.api.Assertions.assertTrue(today >= 0, json);
    org.junit.jupiter.api.Assertions.assertTrue(today < yesterday, json);
    org.junit.jupiter.api.Assertions.assertTrue(yesterday < thisYear, json);
  }

  @Test
  void testToJson_EntryWithRelativePeriodsAndTranslations() {
    Map<RelativePeriodEnum, PeriodTypes.Labels> relativePeriods = new LinkedHashMap<>();
    relativePeriods.put(
        RelativePeriodEnum.TODAY,
        new PeriodTypes.Labels(
            "Today Default",
            "Today Label",
            List.of(new Translation(LOCALE, "label", "Aujourd'hui")),
            "Today Display Label",
            "Today Display Name"));
    PeriodTypes.PeriodTypeEntry.Output e =
        new PeriodTypes.PeriodTypeEntry.Output(
            PeriodTypeEnum.DAILY,
            "Daily",
            "P1D",
            "yyyyMMdd",
            1,
            "Daily",
            "Daily",
            "Daily",
            null,
            relativePeriods);
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of(e));
    assertEquals(
        """
        {"locale":"%s","periodTypes":[{"name":"Daily","isoDuration":"P1D","isoFormat":"yyyyMMdd","frequencyOrder":1,"defaultName":"Daily","label":"Daily","displayLabel":"Daily","displayName":"Daily","relativePeriods":{"TODAY":{"defaultName":"Today Default","label":"Today Label","displayLabel":"Today Display Label","displayName":"Today Display Name","translations":[{"locale":"%s","property":"label","value":"Aujourd'hui"}]}}}]}"""
            .formatted(LOCALE, LOCALE),
        toJson(types));
  }

  @Test
  void testToJson_EntryWithEmptyRelativePeriods_OmitsRelativePeriods() {
    PeriodTypes.PeriodTypeEntry.Output e =
        new PeriodTypes.PeriodTypeEntry.Output(
            PeriodTypeEnum.DAILY,
            "Daily",
            "P1D",
            "yyyyMMdd",
            1,
            "Daily",
            "Daily",
            "Daily",
            null,
            new LinkedHashMap<>()); // empty, not null
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of(e));
    String json = toJson(types);
    org.junit.jupiter.api.Assertions.assertFalse(json.contains("relativePeriods"), json);
  }

  // ====== toJsonArray(PeriodTypes.Output) — bare array ======

  @Test
  void testToJsonArray_EmptyEntries() {
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of());
    assertEquals("[]", toJsonArray(types));
  }

  @Test
  void testToJsonArray_SingleEntry() {
    PeriodTypes.Output types =
        new PeriodTypes.Output(LOCALE, List.of(entry(PeriodTypeEnum.WEEKLY)));
    assertEquals(
        """
        [{"name":"Weekly","isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":1,"defaultName":"Default Name","label":"Label","displayLabel":"Display Label","displayName":"Display Name"}]""",
        toJsonArray(types));
  }

  @Test
  void testToJsonArray_MultipleEntries() {
    PeriodTypes.Output types =
        new PeriodTypes.Output(
            LOCALE, List.of(entry(PeriodTypeEnum.DAILY), entry(PeriodTypeEnum.YEARLY)));
    assertEquals(
        """
        [{"name":"Daily","isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":1,"defaultName":"Default Name","label":"Label","displayLabel":"Display Label","displayName":"Display Name"},{"name":"Yearly","isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":1,"defaultName":"Default Name","label":"Label","displayLabel":"Display Label","displayName":"Display Name"}]""",
        toJsonArray(types));
  }

  @Test
  void testToJsonArray_NoLocaleWrapper() {
    // The array form drops the outer {"locale":...,"periodTypes":[...]} wrapper entirely
    PeriodTypes.Output types = new PeriodTypes.Output(LOCALE, List.of(entry(PeriodTypeEnum.DAILY)));
    String json = toJsonArray(types);
    org.junit.jupiter.api.Assertions.assertFalse(json.contains("\"locale\""), json);
    org.junit.jupiter.api.Assertions.assertFalse(json.contains("\"periodTypes\""), json);
  }

  // ====== toJson(PeriodTypeEntry.Output) — single entry ======

  @Test
  void testToJsonSingleEntry_Full() {
    assertEquals(
        """
        {"name":"BiMonthly","isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":1,"defaultName":"Default Name","label":"Label","displayLabel":"Display Label","displayName":"Display Name"}""",
        toJson(entry(PeriodTypeEnum.BI_MONTHLY)));
  }

  @Test
  void testToJsonSingleEntry_AllNull() {
    assertEquals("{}", toJson(emptyEntry()));
  }

  @Test
  void testToJsonSingleEntry_WithTranslations() {
    PeriodTypes.PeriodTypeEntry.Output e =
        new PeriodTypes.PeriodTypeEntry.Output(
            PeriodTypeEnum.MONTHLY,
            "Monthly",
            "P1M",
            "yyyyMM",
            5,
            "Monthly",
            "Monthly",
            "Monthly",
            List.of(new Translation(LOCALE, "name", "Mois")),
            null);
    assertEquals(
        """
        {"name":"Monthly","isoDuration":"P1M","isoFormat":"yyyyMM","frequencyOrder":5,"defaultName":"Monthly","label":"Monthly","displayLabel":"Monthly","displayName":"Monthly","translations":[{"locale":"%s","property":"name","value":"Mois"}]}"""
            .formatted(LOCALE),
        toJson(e));
  }

  @Test
  void testToJsonSingleEntry_WithRelativePeriods() {
    Map<RelativePeriodEnum, PeriodTypes.Labels> relativePeriods = new LinkedHashMap<>();
    relativePeriods.put(RelativePeriodEnum.THIS_YEAR, labels("This Year"));
    PeriodTypes.PeriodTypeEntry.Output e =
        new PeriodTypes.PeriodTypeEntry.Output(
            PeriodTypeEnum.YEARLY,
            "Yearly",
            "P1Y",
            "yyyy",
            6,
            "Yearly",
            "Yearly",
            "Yearly",
            null,
            relativePeriods);
    assertEquals(
        """
        {"name":"Yearly","isoDuration":"P1Y","isoFormat":"yyyy","frequencyOrder":6,"defaultName":"Yearly","label":"Yearly","displayLabel":"Yearly","displayName":"Yearly","relativePeriods":{"THIS_YEAR":{"defaultName":"This Year Default","label":"This Year Label","displayLabel":"This Year Display Label","displayName":"This Year Display Name"}}}""",
        toJson(e));
  }

  // ====== helpers ======

  private static PeriodTypes.PeriodTypeEntry.Output entry(PeriodTypeEnum type) {
    return new PeriodTypes.PeriodTypeEntry.Output(
        type,
        "Display Name",
        "P1M",
        "yyyyMM",
        1,
        "Default Name",
        "Label",
        "Display Label",
        null,
        null);
  }

  private static PeriodTypes.PeriodTypeEntry.Output emptyEntry() {
    return new PeriodTypes.PeriodTypeEntry.Output(
        null, null, null, null, null, null, null, null, null, null);
  }

  private static PeriodTypes.Labels labels(String prefix) {
    return new PeriodTypes.Labels(
        prefix + " Default",
        prefix + " Label",
        List.of(),
        prefix + " Display Label",
        prefix + " Display Name");
  }

  private static String toJson(PeriodTypes.Output types) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PeriodOutput.toJson(types, out);
    return out.toString(StandardCharsets.UTF_8);
  }

  private static String toJsonArray(PeriodTypes.Output types) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PeriodOutput.toJsonArray(types, out);
    return out.toString(StandardCharsets.UTF_8);
  }

  private static String toJson(PeriodTypes.PeriodTypeEntry.Output entry) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PeriodOutput.toJson(entry, out);
    return out.toString(StandardCharsets.UTF_8);
  }
}
