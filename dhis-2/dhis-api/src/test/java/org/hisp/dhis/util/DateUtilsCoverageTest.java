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
package org.hisp.dhis.util;

import static org.hisp.dhis.util.DateUtils.getDate;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.util.DateUtils.toLongGmtDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Coverage-focused tests for {@link DateUtils}.
 *
 * <p>The default timezone is forced to UTC for the duration of this class so exact string outputs
 * are deterministic and safe to assert. These tests exist primarily to lock in current behaviour
 * before migrating away from Joda-Time.
 */
@ResourceLock("dateutils-tz")
class DateUtilsCoverageTest {

  private static TimeZone originalTz;

  @BeforeAll
  static void setUp() {
    originalTz = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    DateTimeZone.setDefault(DateTimeZone.UTC);
  }

  @AfterAll
  static void tearDown() {
    TimeZone.setDefault(originalTz);
    DateTimeZone.setDefault(DateTimeZone.forTimeZone(originalTz));
  }

  // -------------------------------------------------------------------
  // Constants
  // -------------------------------------------------------------------

  @Test
  void testConstants() {
    assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS", DateUtils.ISO8601_NO_TZ_PATTERN);
    assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSSZ", DateUtils.ISO8601_PATTERN);
    assertEquals(365.0, DateUtils.DAYS_IN_YEAR);
    assertEquals(86400L, DateUtils.SECONDS_PER_DAY);
    assertNotNull(DateUtils.DAY_SECOND_FORMAT);
  }

  // -------------------------------------------------------------------
  // ISO 8601 / timestamp formatting
  // -------------------------------------------------------------------

  @Test
  void testToIso8601() {
    Date date = getDate(2024, 3, 15, 14, 30);
    String result = DateUtils.toIso8601(date);
    assertNotNull(result);
    assertTrue(result.startsWith("2024-03-15T14:30:00.000"), "Got: " + result);
    assertTrue(result.endsWith("+0000") || result.endsWith("Z"), "Got: " + result);
    assertNull(DateUtils.toIso8601(null));
  }

  @Test
  void testToIso8601NoTz() {
    Date date = getDate(2024, 3, 15, 14, 30);
    assertEquals("2024-03-15T14:30:00.000", DateUtils.toIso8601NoTz(date));
    assertNull(DateUtils.toIso8601NoTz(null));
  }

  @Test
  void testToLongDateWithMillis() {
    Date date = getDate(2024, 3, 15, 14, 30);
    assertEquals("2024-03-15T14:30:00.000", DateUtils.toLongDateWithMillis(date));
    assertNull(DateUtils.toLongDateWithMillis(null));
  }

  @Test
  void testToLongDateNoT() {
    Date date = getDate(2024, 3, 15, 14, 30);
    assertEquals("2024-03-15 14:30:00.000", DateUtils.toLongDateNoT(date));
    assertNull(DateUtils.toLongDateNoT(null));
  }

  @Test
  void testGetLongDate() {
    String result = DateUtils.getLongDate();
    assertNotNull(result);
    assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"), "Got: " + result);
  }

  @Test
  void testGetMediumDateString() {
    String result = DateUtils.getMediumDateString();
    assertNotNull(result);
    assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"), "Got: " + result);
  }

  // -------------------------------------------------------------------
  // getDate(int...) constructors
  // -------------------------------------------------------------------

  @Test
  void testGetDateYMD() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(getDate(2024, 3, 15));
    assertEquals(2024, cal.get(Calendar.YEAR));
    assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
    assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal.get(Calendar.MINUTE));
    assertEquals(0, cal.get(Calendar.SECOND));
    assertEquals(0, cal.get(Calendar.MILLISECOND));
  }

  @Test
  void testGetDateYMDHM() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(getDate(2024, 3, 15, 14, 30));
    assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(30, cal.get(Calendar.MINUTE));
    assertEquals(0, cal.get(Calendar.SECOND));
  }

  // -------------------------------------------------------------------
  // Epoch / days arithmetic
  // -------------------------------------------------------------------

  @Test
  void testGetEpoch() {
    assertEquals(0L, DateUtils.getEpoch().getTime());
  }

  @Test
  void testGetDays() {
    assertEquals(0L, DateUtils.getDays(DateUtils.getEpoch()));
    assertEquals(1L, DateUtils.getDays(new Date(86400000L)));
    assertEquals(10L, DateUtils.getDays(new Date(10 * 86400000L)));
  }

  @Test
  void testGetDaysBetween() {
    Date start = new Date(0L);
    Date end = new Date(10 * 86400000L);
    assertEquals(10L, DateUtils.getDays(start, end));
    assertEquals(-10L, DateUtils.getDays(end, start));
  }

  @Test
  void testGetDaysInclusive() {
    Date start = new Date(0L);
    Date end = new Date(10 * 86400000L);
    assertEquals(11L, DateUtils.getDaysInclusive(start, end));
    assertEquals(1L, DateUtils.getDaysInclusive(start, start));
  }

  @Test
  void testGetDaysInclusiveSameDay() {
    Date start = getDate(2024, 3, 15);
    Date end = new Date(start.getTime() + 86399999L); // end of same day
    assertEquals(0L, DateUtils.getDays(start, end));
    assertEquals(1L, DateUtils.getDaysInclusive(start, end));
  }

  @Test
  void testMonthsBetween() {
    Date jan15 = getDate(2024, 1, 15);
    Date apr15 = getDate(2024, 4, 15);
    Date apr14 = getDate(2024, 4, 14);
    assertEquals(3, DateUtils.monthsBetween(jan15, apr15));
    assertEquals(-3, DateUtils.monthsBetween(apr15, jan15));
    assertEquals(0, DateUtils.monthsBetween(jan15, jan15));
    assertEquals(2, DateUtils.monthsBetween(jan15, apr14));
  }

  @Test
  void testDaysSince1900() {
    // From 1900-01-01 to 2000-01-01: 100 years, 24 leap years (1900 is not leap)
    assertEquals(36524, DateUtils.daysSince1900(getDate(2000, 1, 1)));
    assertEquals(0, DateUtils.daysSince1900(getDate(1900, 1, 1)));
  }

  @Test
  void testAddDays() {
    Date start = getDate(2024, 3, 15);
    assertEquals(getDate(2024, 3, 16), DateUtils.addDays(start, 1));
    assertEquals(getDate(2024, 3, 14), DateUtils.addDays(start, -1));
    assertEquals(getDate(2024, 3, 20), DateUtils.addDays(start, 5));
    assertEquals(start, DateUtils.addDays(start, 0));
  }

  @Test
  void testAddDaysFractional() {
    Date start = getDate(2024, 3, 15);
    // 0.5 day = 12 hours
    Date result = DateUtils.addDays(start, 0.5);
    assertEquals(start.getTime() + 12 * 3600 * 1000L, result.getTime());
  }

  @Test
  void testGetDateForTomorrow() {
    Date before = new Date();
    Date tomorrow = DateUtils.getDateForTomorrow(10);
    Date after = new Date();

    Calendar cal = Calendar.getInstance();
    cal.setTime(tomorrow);
    assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));

    // Must be between 10h and 58h in the future (allowing for the non-zeroed
    // minute/second fields).
    long diff = tomorrow.getTime() - before.getTime();
    assertTrue(diff > 0, "Tomorrow must be in the future");
    assertTrue(diff < 58L * 3600 * 1000, "Tomorrow must be within ~48h, got: " + diff);
    assertTrue(tomorrow.getTime() <= after.getTime() + 58L * 3600 * 1000);
  }

  @Test
  void testGetSecondsUntilTomorrow() {
    long seconds = DateUtils.getSecondsUntilTomorrow(0);
    assertTrue(seconds > 0, "Got: " + seconds);
    assertTrue(seconds <= 24 * 3600, "Got: " + seconds);
  }

  // -------------------------------------------------------------------
  // between / strictlyBetween
  // -------------------------------------------------------------------

  @Test
  void testBetweenInclusive() {
    Date start = getDate(2024, 3, 1);
    Date end = getDate(2024, 3, 10);
    assertTrue(DateUtils.between(getDate(2024, 3, 5), start, end));
    assertTrue(DateUtils.between(start, start, end));
    assertTrue(DateUtils.between(end, start, end));
    assertFalse(DateUtils.between(getDate(2024, 2, 28), start, end));
    assertFalse(DateUtils.between(getDate(2024, 3, 11), start, end));
  }

  @Test
  void testBetweenDegenerateRanges() {
    Date start = getDate(2024, 3, 5);
    assertFalse(DateUtils.between(start, start, start)); // start == end
    assertFalse(DateUtils.between(start, start, getDate(2024, 3, 1))); // end before start
  }

  @Test
  void testStrictlyBetween() {
    Date start = getDate(2024, 3, 1);
    Date end = getDate(2024, 3, 10);
    assertTrue(DateUtils.strictlyBetween(getDate(2024, 3, 5), start, end));
    assertFalse(DateUtils.strictlyBetween(start, start, end));
    assertFalse(DateUtils.strictlyBetween(end, start, end));
    assertFalse(DateUtils.strictlyBetween(getDate(2024, 2, 28), start, end));
    assertFalse(DateUtils.strictlyBetween(getDate(2024, 3, 11), start, end));
  }

  @Test
  void testStrictlyBetweenDegenerateRanges() {
    Date start = getDate(2024, 3, 5);
    assertFalse(DateUtils.strictlyBetween(start, start, start));
    assertFalse(DateUtils.strictlyBetween(start, start, getDate(2024, 3, 1)));
  }

  // -------------------------------------------------------------------
  // min/max varargs variants
  // -------------------------------------------------------------------

  @Test
  void testGetEarliest() {
    Date d1 = getDate(2024, 3, 15);
    Date d2 = getDate(2024, 3, 10);
    Date d3 = getDate(2024, 3, 20);

    assertEquals(d2, DateUtils.getEarliest(d1, d2, d3));
    assertEquals(d2, DateUtils.getEarliest(d3, d2, d1));
    assertEquals(d1, DateUtils.getEarliest(d1));
    assertEquals(d1, DateUtils.getEarliest(d1, null));
    assertNull(DateUtils.getEarliest());
    assertNull(DateUtils.getEarliest((Date) null, (Date) null));
  }

  @Test
  void testGetLatest() {
    Date d1 = getDate(2024, 3, 15);
    Date d2 = getDate(2024, 3, 10);
    Date d3 = getDate(2024, 3, 20);

    assertEquals(d3, DateUtils.getLatest(d1, d2, d3));
    assertEquals(d3, DateUtils.getLatest(d3, d2, d1));
    assertEquals(d1, DateUtils.getLatest(d1));
    assertEquals(d1, DateUtils.getLatest(null, d1));
    assertNull(DateUtils.getLatest());
    assertNull(DateUtils.getLatest((Date) null, (Date) null));
  }

  @Test
  void testMaxMinEmptyCollections() {
    assertNull(DateUtils.max(Collections.emptyList()));
    assertNull(DateUtils.min(Collections.emptyList()));
    assertNull(DateUtils.max(Collections.singletonList(null)));
    assertNull(DateUtils.min(Collections.singletonList(null)));
  }

  // -------------------------------------------------------------------
  // getPrettyInterval
  // -------------------------------------------------------------------

  @Test
  void testGetPrettyIntervalTwoDays() {
    Date start = new Date(0L);
    Date end = new Date(2 * 86400000L);
    assertEquals("48 h", DateUtils.getPrettyInterval(start, end));
  }

  @Test
  void testGetPrettyIntervalFullFields() {
    long diff =
        2 * 86400000L // 2 days
            + 3 * 3600000L // 3 hours
            + 15 * 60000L // 15 minutes
            + 30 * 1000L; // 30 seconds
    Date start = new Date(0L);
    Date end = new Date(diff);
    // 51 h = 48 h (2 days) + 3 h
    assertEquals("51 h, 15 m, 30 s", DateUtils.getPrettyInterval(start, end));
  }

  @Test
  void testGetPrettyIntervalNulls() {
    assertNull(DateUtils.getPrettyInterval(null, new Date()));
    assertNull(DateUtils.getPrettyInterval(new Date(), null));
    assertNull(DateUtils.getPrettyInterval(null, null));
  }

  // -------------------------------------------------------------------
  // getSqlDateString / removeTimeStamp
  // -------------------------------------------------------------------

  @Test
  void testGetSqlDateString() {
    assertEquals("2024-03-05", DateUtils.getSqlDateString(getDate(2024, 3, 5)));
    assertEquals("2024-12-25", DateUtils.getSqlDateString(getDate(2024, 12, 25)));
    assertEquals("2024-01-01", DateUtils.getSqlDateString(getDate(2024, 1, 1)));
  }

  @Test
  void testRemoveTimeStamp() {
    Date date = getDate(2024, 3, 15, 14, 30);
    assertEquals(getDate(2024, 3, 15), DateUtils.removeTimeStamp(date));
    assertNull(DateUtils.removeTimeStamp(null));
  }

  // -------------------------------------------------------------------
  // parseDateEndOfTheDay
  // -------------------------------------------------------------------

  @Test
  void testParseDateEndOfTheDayNull() {
    assertNull(DateUtils.parseDateEndOfTheDay(null));
    assertNull(DateUtils.parseDateEndOfTheDay(""));
  }

  @Test
  void testParseDateEndOfTheDayDateOnly() {
    Date date = DateUtils.parseDateEndOfTheDay("2024-03-15");
    assertNotNull(date);
    LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    assertEquals(2024, ldt.getYear());
    assertEquals(3, ldt.getMonthValue());
    assertEquals(15, ldt.getDayOfMonth());
    assertEquals(23, ldt.getHour());
    assertEquals(59, ldt.getMinute());
    assertEquals(59, ldt.getSecond());
    assertEquals(999, ldt.getNano() / 1_000_000);
  }

  @Test
  void testParseDateEndOfTheDayWithTime() {
    Date date = DateUtils.parseDateEndOfTheDay("2024-03-15T14:30:45");
    assertNotNull(date);
    LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    assertEquals(2024, ldt.getYear());
    assertEquals(3, ldt.getMonthValue());
    assertEquals(15, ldt.getDayOfMonth());
    assertEquals(14, ldt.getHour());
    assertEquals(30, ldt.getMinute());
    assertEquals(45, ldt.getSecond());
  }

  // -------------------------------------------------------------------
  // parseDate across the supported formats
  // -------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2024-03-15T14:30:45.123456789Z",
        "2024-03-15T14:30:45.123456Z",
        "2024-03-15T14:30:45.123Z",
        "2024-03-15T14:30:45Z",
        "2024-03-15T14:30Z",
        "2024-03-15T14Z",
        "2024-03-15T14:30:45.123",
        "2024-03-15T14:30:45",
        "2024-03-15T14:30",
        "2024-03-15T14",
        "2024-03-15 14:30:45Z"
      })
  void testParseDateFormats(String input) {
    Date date = parseDate(input);
    assertNotNull(date, "Failed to parse: " + input);
  }

  @Test
  void testParseDateWithVariousOffsets() {
    // 2024-03-15T14:30:45+05:30 == 2024-03-15T09:00:45Z
    Date date = parseDate("2024-03-15T14:30:45+05:30");
    assertEquals(Instant.parse("2024-03-15T09:00:45Z").toEpochMilli(), date.getTime());

    // 2024-03-15T14:30:45-08:00 == 2024-03-15T22:30:45Z
    date = parseDate("2024-03-15T14:30:45-08:00");
    assertEquals(Instant.parse("2024-03-15T22:30:45Z").toEpochMilli(), date.getTime());

    // 2024-03-15T14:30:45Z is UTC
    date = parseDate("2024-03-15T14:30:45Z");
    assertEquals(Instant.parse("2024-03-15T14:30:45Z").toEpochMilli(), date.getTime());
  }

  @Test
  void testParseDateDateOnlyVariants() {
    // yyyyMMdd
    Date date = parseDate("20240315");
    LocalDate ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2024, ld.getYear());
    assertEquals(3, ld.getMonthValue());
    assertEquals(15, ld.getDayOfMonth());

    // yyyy-MM
    date = parseDate("2024-03");
    ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2024, ld.getYear());
    assertEquals(3, ld.getMonthValue());
    assertEquals(1, ld.getDayOfMonth());

    // yyyy
    date = parseDate("2024");
    ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2024, ld.getYear());
    assertEquals(1, ld.getMonthValue());
    assertEquals(1, ld.getDayOfMonth());
  }

  // -------------------------------------------------------------------
  // getDuration / nowMinusDuration
  // -------------------------------------------------------------------

  @Test
  void testGetDurationNullAndInvalid() {
    assertNull(DateUtils.getDuration(null));
    assertNull(DateUtils.getDuration(""));
    assertNull(DateUtils.getDuration("abc"));
    assertNull(DateUtils.getDuration("123"));
    assertNull(DateUtils.getDuration("10x"));
    assertNull(DateUtils.getDuration("1y"));
    assertNull(DateUtils.getDuration("10ddd"));
  }

  @Test
  void testGetDurationValid() {
    assertEquals(Duration.ofSeconds(50), DateUtils.getDuration("50s"));
    assertEquals(Duration.ofMinutes(20), DateUtils.getDuration("20m"));
    assertEquals(Duration.ofHours(2), DateUtils.getDuration("2h"));
    assertEquals(Duration.ofDays(14), DateUtils.getDuration("14d"));
    assertEquals(Duration.ofSeconds(0), DateUtils.getDuration("0s"));
  }

  @Test
  void testNowMinusDuration() {
    long before = System.currentTimeMillis();
    Date oneHourAgo = DateUtils.nowMinusDuration("1h");
    long after = System.currentTimeMillis();
    long midpoint = (before + after) / 2;
    long expected = midpoint - 3600000L;
    assertTrue(
        Math.abs(oneHourAgo.getTime() - expected) < 2000,
        "Expected ~" + expected + " got " + oneHourAgo.getTime());
  }

  // -------------------------------------------------------------------
  // Conversions (Timestamp, SQL Date, OffsetDateTime, Instant)
  // -------------------------------------------------------------------

  @Test
  void testAsTimestamp() {
    Date date = getDate(2024, 3, 15, 14, 30);
    Timestamp ts = DateUtils.asTimestamp(date);
    assertEquals(date.getTime(), ts.getTime());
  }

  @Test
  void testAsSqlDate() {
    Date date = getDate(2024, 3, 15, 14, 30);
    java.sql.Date sqlDate = DateUtils.asSqlDate(date);
    assertEquals(date.getTime(), sqlDate.getTime());
  }

  @Test
  void testOffSetDateTimeFrom() {
    Date date = getDate(2024, 3, 15, 14, 30);
    OffsetDateTime odt = DateUtils.offSetDateTimeFrom(date);
    assertEquals(ZoneOffset.UTC, odt.getOffset());
    assertEquals(2024, odt.getYear());
    assertEquals(3, odt.getMonthValue());
    assertEquals(15, odt.getDayOfMonth());
    assertEquals(14, odt.getHour());
    assertEquals(30, odt.getMinute());
    assertEquals(0, odt.getSecond());
  }

  @Test
  void testFromInstant() {
    Instant instant = Instant.ofEpochMilli(1710500000000L);
    Date date = DateUtils.fromInstant(instant);
    assertNotNull(date);
    assertEquals(instant.toEpochMilli(), date.getTime());
    assertNull(DateUtils.fromInstant(null));
  }

  @Test
  void testInstantFromDate() {
    Date date = new Date(1710500000000L);
    assertEquals(date.toInstant(), DateUtils.instantFromDate(date));
    assertNull(DateUtils.instantFromDate(null));
  }

  @Test
  void testInstantFromEpoch() {
    Instant instant = DateUtils.instantFromEpoch(1710500000000L);
    assertEquals(1710500000000L, instant.toEpochMilli());
  }

  @Test
  void testInstantFromDateAsString() {
    Instant instant = DateUtils.instantFromDateAsString("2024-03-15T14:30:45.000Z");
    assertEquals(Instant.parse("2024-03-15T14:30:45.000Z"), instant);
  }

  @Test
  void testInstantFromDateAsStringNull() {
    assertNull(DateUtils.instantFromDateAsString(null));
    assertNull(DateUtils.instantFromDateAsString(""));
  }

  // -------------------------------------------------------------------
  // dateTruncMonth with time-of-day (current test only covers midnight)
  // -------------------------------------------------------------------

  @Test
  void testDateTruncMonthWithTimeOfDay() {
    Date date = getDate(2024, 3, 15, 14, 30);
    Calendar cal = Calendar.getInstance();
    cal.setTime(DateUtils.dateTruncMonth(date));
    assertEquals(2024, cal.get(Calendar.YEAR));
    assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
    assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal.get(Calendar.MINUTE));
    assertEquals(0, cal.get(Calendar.SECOND));
    assertEquals(0, cal.get(Calendar.MILLISECOND));
  }

  // -------------------------------------------------------------------
  // isToday edge cases
  // -------------------------------------------------------------------

  @Test
  void testIsTodayBoundaries() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    assertTrue(DateUtils.isToday(cal.getTime()), "Midnight today must be today");

    cal.set(Calendar.HOUR_OF_DAY, 23);
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.SECOND, 59);
    cal.set(Calendar.MILLISECOND, 999);
    assertTrue(DateUtils.isToday(cal.getTime()), "End of day today must be today");

    cal.add(Calendar.DAY_OF_YEAR, -1);
    assertFalse(DateUtils.isToday(cal.getTime()), "Yesterday end-of-day must not be today");

    cal.add(Calendar.DAY_OF_YEAR, +2);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    assertFalse(DateUtils.isToday(cal.getTime()), "Tomorrow midnight must not be today");
  }

  // -------------------------------------------------------------------
  // safeParseDate null / empty
  // -------------------------------------------------------------------

  @Test
  void testSafeParseDateNullOrEmpty() {
    assertNull(DateUtils.safeParseDate(null));
    assertNull(DateUtils.safeParseDate(""));
  }

  // -------------------------------------------------------------------
  // parseDate contract for definitely-invalid input
  // -------------------------------------------------------------------

  @Test
  void testParseDateRejectsGarbage() {
    assertThrows(IllegalArgumentException.class, () -> parseDate("not-a-date"));
    assertThrows(IllegalArgumentException.class, () -> parseDate("2024/03/15"));
    assertThrows(IllegalArgumentException.class, () -> parseDate("2024-99-99"));
  }

  // -------------------------------------------------------------------
  // LocalDateTime -> Date uses UTC (already covered upstream, but the
  // contract is worth documenting explicitly alongside the other tests)
  // -------------------------------------------------------------------

  @Test
  void testGetDateFromLocalDateTimeUsesUtc() {
    LocalDateTime ldt = LocalDateTime.of(2024, 3, 15, 14, 30, 45);
    Date date = DateUtils.getDate(ldt);
    assertEquals(Instant.parse("2024-03-15T14:30:45Z").toEpochMilli(), date.getTime());
  }

  // -------------------------------------------------------------------
  // getDate(int...) + parse round-trip
  // -------------------------------------------------------------------

  @Test
  void testGetDateParseRoundTrip() {
    Date date = getDate(2024, 3, 15, 14, 30);
    String formatted = DateUtils.toLongDate(date);
    Date reparsed = parseDate(formatted);
    assertEquals(date, reparsed);
  }

  // -------------------------------------------------------------------
  // toMediumDate(String) contract
  // -------------------------------------------------------------------

  @Test
  void testToMediumDateStringNull() {
    assertNull(DateUtils.toMediumDate((String) null));
    assertNull(DateUtils.toMediumDate(""));
  }

  // -------------------------------------------------------------------
  // LocalTime.MAX round-trip sanity for parseDateEndOfTheDay
  // -------------------------------------------------------------------

  @Test
  void testParseDateEndOfTheDayIsLaterThanStartOfDay() {
    Date endOfDay = DateUtils.parseDateEndOfTheDay("2024-03-15");
    Date startOfDay = parseDate("2024-03-15T00:00:00");
    assertTrue(endOfDay.after(startOfDay));
    long diffMillis = endOfDay.getTime() - startOfDay.getTime();
    assertEquals(86399999L, diffMillis); // 23:59:59.999
  }

  @Test
  void testGetPrettyIntervalZero() {
    Date d = new Date(0L);
    System.out.println("ZERO: [" + DateUtils.getPrettyInterval(d, d) + "]");
  }

  @Test
  void testGetPrettyIntervalSecondsOnly() {
    Date start = new Date(0L);
    Date end = new Date(5 * 1000L);
    System.out.println("5s: [" + DateUtils.getPrettyInterval(start, end) + "]");
  }

  @Test
  void testGetPrettyIntervalExactlyOneHour() {
    Date start = new Date(0L);
    Date end = new Date(3600 * 1000L);
    System.out.println("1h: [" + DateUtils.getPrettyInterval(start, end) + "]");
  }

  @Test
  void testGetPrettyIntervalExactlyOneWeek() {
    Date start = new Date(0L);
    Date end = new Date(7 * 86400000L);
    System.out.println("7d: [" + DateUtils.getPrettyInterval(start, end) + "]");
  }

  @Test
  void testGetPrettyIntervalThirtyDays() {
    Date start = new Date(0L);
    Date end = new Date(30 * 86400000L);
    System.out.println("30d: [" + DateUtils.getPrettyInterval(start, end) + "]");
  }

  @Test
  void testGetPrettyIntervalOneYear() {
    Date start = new Date(0L);
    Date end = new Date(365L * 86400000L);
    System.out.println("365d: [" + DateUtils.getPrettyInterval(start, end) + "]");
  }

  @Test
  void toLongGmtDate_preEpoch() {
    assertLongGmtDateEquals("1969-12-31T00:00:00.000+0000", new Date(-86_400_000L));
  }

  @Test
  void toLongGmtDate_yearOne() {
    // proleptic Gregorian; w4 pads to 4 digits, so year 1 renders as 0001 on both sides
    assertLongGmtDateEquals("0001-01-01T00:00:00.000+0000", new Date(-62_135_596_800_000L));
  }

  @Test
  void toLongGmtDate_centuryBoundary() {
    // leap day in a century year, exercises the civil_from_days math end-to-end
    assertLongGmtDateEquals("2000-02-29T12:34:56.789+0000", new Date(951_827_696_789L));
  }

  @Test
  void toLongGmtDate_midnightMillis() {
    assertLongGmtDateEquals("2024-01-01T00:00:00.000+0000", new Date(1_704_067_200_000L));
  }

  @Test
  void toLongGmtDate_endOfDay() {
    assertLongGmtDateEquals("2024-12-31T23:59:59.999+0000", new Date(1_735_689_599_999L));
  }

  /**
   * Differential sweep across a representative set of epochs: pre-1970, leap days, year boundaries,
   * max millis/second/hour/minute, and a few "normal" mid-year dates. Every case here is verified
   * against the original Joda implementation. If any of these fail, the hand-rolled renderer has
   * drifted.
   */
  @ParameterizedTest(name = "epoch={0}")
  @ValueSource(
      longs = {
        0L, // 1970-01-01
        -1L, // 1969-12-31T23:59:59.999 — max millis below epoch
        -86_400_000L, // 1969-12-31
        -1_000_000_000_000L, // 1938-04-24
        410_244_000_000L, // 1983-01-01
        951_782_400_000L, // 2000-02-29T00:00
        951_868_800_000L, // 2000-03-01T00:00
        1_682_899_200_000L, // 2023-05-01
        1_704_067_200_000L, // 2024-01-01
        1_710_468_000_000L, // 2024-03-15
        1_717_632_000_000L, // 2024-06-06
        1_735_689_599_999L, // 2024-12-31T23:59:59.999
        1_735_689_600_000L, // 2025-01-01
      })
  void toLongGmtDate_differential(long epochMillis) {
    assertLongGmtDateMatchesJoda(new Date(epochMillis));
  }

  @Test
  void testToLongGmtDate() {
    assertLongGmtDateEquals("2024-03-15T14:30:00.000+0000", getDate(2024, 3, 15, 14, 30));
    assertLongGmtDateEquals(null, null);
  }

  /**
   * Reference implementation of {@code toLongGmtDate} using the pre-refactor Joda path. Used only
   * for differential testing while Joda is still present in {@link DateUtils}; when the last Joda
   * formatter is removed from the main class, delete this block together with its callers and rely
   * on the explicit expected strings alone.
   */
  private static final DateTimeFormatter JODA_LONG_GMT_FORMAT =
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZoneUTC();

  private static String jodaLongGmtDate(Date date) {
    return date == null ? null : JODA_LONG_GMT_FORMAT.print(new DateTime(date));
  }

  /**
   * Asserts that both {@link DateUtils#toLongGmtDate(Date)} and {@link
   * DateUtils#toLongGmtDateText(Date)} produce the given expected string, that the CharSequence
   * view is a faithful 1:1 projection (length + charAt), and that both agree with the pre-refactor
   * Joda reference. The Joda comparison is transitional; the explicit expected string is what
   * survives the removal of Joda.
   */
  private static void assertLongGmtDateEquals(String expected, Date date) {
    // String variant
    String actual = toLongGmtDate(date);
    assertEquals(expected, actual, "toLongGmtDate");

    // CharSequence variant
    CharSequence text = DateUtils.toLongGmtDateText(date);
    if (expected == null) {
      assertNull(text, "toLongGmtDateText");
    } else {
      assertNotNull(text, "toLongGmtDateText");
      assertEquals(expected.length(), text.length(), "toLongGmtDateText.length()");
      for (int i = 0; i < expected.length(); i++) {
        assertEquals(expected.charAt(i), text.charAt(i), "toLongGmtDateText.charAt(" + i + ")");
      }
      assertEquals(expected, text.toString(), "toLongGmtDateText.toString()");
    }

    // Differential check against the original Joda formatter
    assertEquals(jodaLongGmtDate(date), actual, "toLongGmtDate vs Joda reference");
    if (expected != null) {
      assertEquals(jodaLongGmtDate(date), text.toString(), "toLongGmtDateText vs Joda reference");
    }
  }

  /** Lighter differential check for the broad parameterized sweep. */
  private static void assertLongGmtDateMatchesJoda(Date date) {
    String joda = jodaLongGmtDate(date);
    assertEquals(joda, toLongGmtDate(date), "toLongGmtDate vs Joda");
    CharSequence text = DateUtils.toLongGmtDateText(date);
    assertEquals(joda, text == null ? null : text.toString(), "toLongGmtDateText vs Joda");
  }
}
