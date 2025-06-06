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

import static java.util.Calendar.DATE;
import static java.util.Calendar.MILLISECOND;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hisp.dhis.util.DateUtils.dateIsValid;
import static org.hisp.dhis.util.DateUtils.dateTimeIsValid;
import static org.hisp.dhis.util.DateUtils.getDate;
import static org.hisp.dhis.util.DateUtils.minusOneDay;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.util.DateUtils.plusOneDay;
import static org.hisp.dhis.util.DateUtils.safeParseDate;
import static org.hisp.dhis.util.DateUtils.toMediumDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.hisp.dhis.calendar.impl.NepaliCalendar;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class DateUtilsTest {
  private static final String NULL_STRING = null;
  private static final Date NULL_DATE = null;

  @Test
  void testDateIsValid() {
    assertTrue(dateIsValid("2000-01-01"));
    assertTrue(dateIsValid("1067-04-28"));
    assertFalse(dateIsValid("07-07-2000"));
    assertFalse(dateIsValid("2000-03-40"));
    assertFalse(dateIsValid("20d20-03-01"));
    assertTrue(dateIsValid("2014-01-01"));
    assertFalse(dateIsValid("2014-12-33"));
    assertFalse(dateIsValid("2014-13-32"));
    assertFalse(dateIsValid("2014-ab-cd"));
    assertFalse(dateIsValid("201-01-01"));
    assertFalse(dateIsValid("01-01-01"));
    assertFalse(dateIsValid("abcd-01-01"));
    assertFalse(dateIsValid("2017-04-31"));
    assertFalse(dateIsValid("2017-04-32"));
    assertFalse(dateIsValid("2016-09-31"));
    assertFalse(dateIsValid("0000-01-01"));
    assertTrue(dateIsValid("0001-01-01"));
    assertTrue(dateIsValid(NepaliCalendar.getInstance(), "2074-04-32"));
    assertFalse(dateIsValid(NepaliCalendar.getInstance(), "2074-03-32"));
    assertFalse(dateIsValid(NepaliCalendar.getInstance(), "2074-04-33"));
  }

  @Test
  void testDateTimeIsValid() {
    assertTrue(dateTimeIsValid("2021-08-30T13:35:18.985541Z"));
    assertTrue(dateTimeIsValid("2021-08-30T13:35:18.985541"));
    assertTrue(dateTimeIsValid("2021-08-30T13:35:18.8712Z"));
    assertTrue(dateTimeIsValid("2021-08-30T13:35:18.8712"));
    assertTrue(dateTimeIsValid("2000-01-01T10:00:00.000Z"));
    assertTrue(dateTimeIsValid("2021-08-30T13:35:18.985"));
    assertTrue(dateTimeIsValid("2000-01-01T10:00:00.000+05:30"));
    assertTrue(dateTimeIsValid("2000-01-01T10:00:00Z"));
    assertTrue(dateTimeIsValid("2000-01-01T10:00:00+05:30"));
    assertTrue(dateTimeIsValid("2000-01-01T10:00:00"));
    assertTrue(dateTimeIsValid("2000-01-01T10:00Z"));
    assertTrue(dateTimeIsValid("2000-01-01T10:00+05:30"));
    assertTrue(dateTimeIsValid("2000-01-01T10:00"));
    assertFalse(dateTimeIsValid("2000-01-01"));
    assertFalse(dateTimeIsValid("01-01-2000"));
    assertFalse(dateTimeIsValid("abcd"));
  }

  @Test
  void testDaysBetween() {
    Date dateA = new DateTime(2014, 3, 1, 0, 0).toDate();
    Date dateB = new DateTime(2014, 3, 7, 0, 0).toDate();
    assertEquals(6, DateUtils.daysBetween(dateA, dateB));
  }

  @Test
  void testIsToday() {
    java.util.Calendar cal = java.util.Calendar.getInstance();
    Date today = cal.getTime();
    cal.add(DATE, -1);
    Date yesterday = cal.getTime();
    cal.add(DATE, +2);
    Date tomorrow = cal.getTime();
    assertTrue(DateUtils.isToday(today));
    assertFalse(DateUtils.isToday(yesterday));
    assertFalse(DateUtils.isToday(tomorrow));
  }

  @Test
  void testMax() {
    Date date1 = new DateTime(2014, 5, 15, 3, 3).toDate();
    Date date2 = new DateTime(2014, 5, 18, 1, 1).toDate();
    Date date3 = null;
    Date date4 = null;
    assertEquals(date2, DateUtils.max(date1, date2));
    assertEquals(date2, DateUtils.max(date2, date1));
    assertEquals(date1, DateUtils.max(date1, date3));
    assertEquals(date1, DateUtils.max(date3, date1));
    assertNull(DateUtils.max(date3, date4));
  }

  @Test
  void testMaxCollection() {
    Date date1 = new DateTime(2014, 5, 15, 3, 3).toDate();
    Date date2 = new DateTime(2014, 5, 18, 1, 1).toDate();
    Date date3 = new DateTime(2014, 6, 10, 1, 1).toDate();
    Date date4 = null;
    Date date5 = null;
    Date date6 = null;
    assertEquals(date2, DateUtils.max(Sets.newHashSet(date1, date2, date4)));
    assertEquals(date2, DateUtils.max(Sets.newHashSet(date2, date1, date4)));
    assertEquals(date3, DateUtils.max(Sets.newHashSet(date1, date2, date3)));
    assertEquals(date3, DateUtils.max(Sets.newHashSet(date1, date2, date3)));
    assertEquals(date3, DateUtils.max(Sets.newHashSet(date3, date4, date5)));
    assertEquals(null, DateUtils.max(Sets.newHashSet(date4, date5, date6)));
    assertEquals(date1, DateUtils.max(Sets.newHashSet(date1, date5, date4)));
    assertNull(DateUtils.max(Sets.newHashSet(date4, date5, date6)));
  }

  @Test
  void testMin() {
    Date date1 = new DateTime(2014, 5, 15, 3, 3).toDate();
    Date date2 = new DateTime(2014, 5, 18, 1, 1).toDate();
    Date date3 = null;
    Date date4 = null;
    assertEquals(date1, DateUtils.min(date1, date2));
    assertEquals(date1, DateUtils.min(date2, date1));
    assertEquals(date1, DateUtils.min(date1, date3));
    assertEquals(date1, DateUtils.min(date3, date1));
    assertNull(DateUtils.max(date3, date4));
  }

  @Test
  void testMinCollection() {
    Date date1 = new DateTime(2014, 5, 15, 3, 3).toDate();
    Date date2 = new DateTime(2014, 5, 18, 1, 1).toDate();
    Date date3 = new DateTime(2014, 6, 10, 1, 1).toDate();
    Date date4 = null;
    Date date5 = null;
    Date date6 = null;
    assertEquals(date1, DateUtils.min(Sets.newHashSet(date1, date2, date4)));
    assertEquals(date1, DateUtils.min(Sets.newHashSet(date2, date1, date4)));
    assertEquals(date1, DateUtils.min(Sets.newHashSet(date1, date2, date3)));
    assertEquals(date1, DateUtils.min(Sets.newHashSet(date1, date2, date3)));
    assertEquals(date3, DateUtils.min(Sets.newHashSet(date3, date4, date5)));
    assertNull(DateUtils.min(Sets.newHashSet(date4, date5, date6)));
    assertEquals(date1, DateUtils.min(Sets.newHashSet(date1, date5, date4)));
    assertNull(DateUtils.max(Sets.newHashSet(date4, date5, date6)));
  }

  @Test
  void testGetPrettyInterval() {
    Date start = new DateTime(2014, 5, 18, 15, 10, 5, 12).toDate();
    Date end = new DateTime(2014, 5, 19, 11, 45, 42, 56).toDate();
    String interval = DateUtils.getPrettyInterval(start, end);
    assertNotNull(interval);
  }

  @Test
  void testToMediumDate() {
    assertEquals(
        new DateTime(2014, 5, 18, 0, 0, 0, 0).toDate(), DateUtils.toMediumDate("2014-05-18"));
    assertEquals(
        new DateTime(2015, 11, 3, 0, 0, 0, 0).toDate(), DateUtils.toMediumDate("2015-11-03"));
    assertNull(DateUtils.toMediumDate(NULL_STRING));
  }

  @Test
  void testGetInvalidMediumDate() {
    assertThrows(
        IllegalArgumentException.class, () -> DateUtils.toMediumDate("StringWhichIsNotADate"));
  }

  @Test
  void testToMediumDateString() {
    Date date = new DateTime(2014, 5, 18, 15, 10, 5, 12).toDate();
    assertEquals("2014-05-18", DateUtils.toMediumDate(date));
    assertNull(DateUtils.toMediumDate(NULL_DATE));
  }

  @Test
  void testToLongDateString() {
    Date date = new DateTime(2014, 5, 18, 15, 10, 5, 12).toDate();
    assertEquals("2014-05-18T15:10:05", DateUtils.toLongDate(date));
    assertNull(DateUtils.toLongDate(NULL_DATE));
  }

  @Test
  void testToHttpDateString() {
    Date date = new DateTime(2014, 5, 18, 15, 10, 5, 12).toDate();
    assertEquals("Sun, 18 May 2014 15:10:05 GMT", DateUtils.toHttpDateString(date));
    assertNull(DateUtils.toLongDate(NULL_DATE));
  }

  @Test
  void testGetDuration() {
    Duration s50 = DateUtils.getDuration("50s");
    Duration m20 = DateUtils.getDuration("20m");
    Duration h2 = DateUtils.getDuration("2h");
    Duration d14 = DateUtils.getDuration("14d");
    assertEquals(50, s50.getSeconds());
    assertEquals(1200, m20.getSeconds());
    assertEquals(7200, h2.getSeconds());
    assertEquals(1209600, d14.getSeconds());
    assertNull(DateUtils.getDuration("123x"));
    assertNull(DateUtils.getDuration("1y"));
    assertNull(DateUtils.getDuration("10ddd"));
  }

  @Test
  void testGetDate() {
    LocalDateTime time = LocalDateTime.of(2012, 1, 10, 10, 5);
    Date date = DateUtils.getDate(time);
    assertEquals(time.toInstant(ZoneOffset.UTC).toEpochMilli(), date.getTime());
  }

  @Test
  void testGetLatestDate() {
    Date jan3 = new DateTime(2019, 1, 3, 0, 0).toDate();
    Date mar21 = new DateTime(2019, 3, 21, 0, 0).toDate();
    Date aug17 = new DateTime(2019, 8, 17, 0, 0).toDate();
    assertEquals(aug17, DateUtils.getLatest(mar21, null, aug17, null, jan3, null));
    assertEquals(null, DateUtils.getLatest(null, null));
  }

  @Test
  void testParseIntoDSTGap() {
    Calendar cal = Calendar.getInstance();
    int year = 1985;
    int month = 4;
    int day = 14;
    String dateString =
        "" + year + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day;
    assertTrue(dateTimeIsValid(dateString + "T00:00"));
    Date dateParsed = parseDate(dateString);
    cal.setTime(dateParsed);
    assertEquals(year, cal.get(Calendar.YEAR));
    assertEquals(month, cal.get(Calendar.MONTH) + 1);
    assertEquals(day, cal.get(Calendar.DAY_OF_MONTH));
    Date mediumDateParsed = toMediumDate(dateString);
    assertEquals(dateParsed, mediumDateParsed);
  }

  @Test
  void testParseZuluDateOffset() {
    TimeZone timeZone = TimeZone.getTimeZone("UTC");
    Calendar cal = Calendar.getInstance(timeZone);
    int year = 1995;
    int month = 5;
    int day = 24;
    String dateString =
        ""
            + year
            + "-"
            + (month < 10 ? "0" : "")
            + month
            + "-"
            + (day < 10 ? "0" : "")
            + day
            + "T00:00Z";
    assertTrue(dateTimeIsValid(dateString));
    Date dateParsed = parseDate(dateString);
    cal.setTime(dateParsed);
    assertEquals(year, cal.get(Calendar.YEAR));
    assertEquals(month, cal.get(Calendar.MONTH) + 1);
    assertEquals(day, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
  }

  @Test
  void getNextDate() {
    Date dateA = new DateTime(2023, 4, 6, 15, 2, 24).toDate();
    Date dateB = new DateTime(2023, 4, 7, 3, 2, 35).toDate();
    assertEquals(new DateTime(2023, 4, 6, 19, 0, 0, 0).toDate(), DateUtils.getNextDate(19, dateA));
    assertEquals(new DateTime(2023, 4, 6, 21, 0, 0, 0).toDate(), DateUtils.getNextDate(21, dateA));
    assertEquals(new DateTime(2023, 4, 7, 4, 0, 0, 0).toDate(), DateUtils.getNextDate(4, dateA));
    assertEquals(new DateTime(2023, 4, 7, 15, 0, 0, 0).toDate(), DateUtils.getNextDate(15, dateA));
    assertEquals(new DateTime(2023, 4, 8, 2, 0, 0, 0).toDate(), DateUtils.getNextDate(2, dateB));
    assertEquals(new DateTime(2023, 4, 7, 5, 0, 0, 0).toDate(), DateUtils.getNextDate(5, dateB));
    assertEquals(new DateTime(2023, 4, 7, 17, 0, 0, 0).toDate(), DateUtils.getNextDate(17, dateB));
  }

  @Test
  void testCalculateDateFromUsingPositiveDays() {
    Date anyInitialDate = new Date();
    Date theNewDate = DateUtils.calculateDateFrom(anyInitialDate, 1, DATE);
    assertThat(theNewDate, is(greaterThan(anyInitialDate)));
  }

  @Test
  void testCalculateDateFromUsingNegativeDays() {
    Date anyInitialDate = new Date();
    Date theNewDate = DateUtils.calculateDateFrom(anyInitialDate, -1, DATE);
    assertThat(theNewDate, is(lessThan(anyInitialDate)));
  }

  @Test
  void testCalculateDateFromUsingPositiveMilis() {
    Date anyInitialDate = new Date();
    Date theNewDate = DateUtils.calculateDateFrom(anyInitialDate, 1, MILLISECOND);
    assertThat(theNewDate, is(greaterThan(anyInitialDate)));
  }

  @Test
  void testCalculateDateFromUsingNegativeMilis() {
    Date anyInitialDate = new Date();
    Date theNewDate = DateUtils.calculateDateFrom(anyInitialDate, -1, MILLISECOND);
    assertThat(theNewDate, is(lessThan(anyInitialDate)));
  }

  @Test
  void testParseDateSupportedDateFormats() {
    // Assert yyyyMMdd format
    Date date = parseDate("20031202");
    assertNotNull(date);
    LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2003, localDate.getYear());
    assertEquals(12, localDate.getMonthValue());
    assertEquals(02, localDate.getDayOfMonth());

    // Assert yyyy-MM-dd format
    date = parseDate("2005-10-12");
    assertNotNull(date);
    localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2005, localDate.getYear());
    assertEquals(10, localDate.getMonthValue());
    assertEquals(12, localDate.getDayOfMonth());

    // Assert yyyy-MM format
    date = parseDate("2022-05");
    assertNotNull(date);
    localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2022, localDate.getYear());
    assertEquals(5, localDate.getMonthValue());

    // Assert yyyy format
    date = parseDate("2023");
    assertNotNull(date);
    localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2023, localDate.getYear());
  }

  @Test
  void testParseDateInvalidDateFormats() {
    // Assert yyyy-MMM-dd INVALID format
    assertThrows(IllegalArgumentException.class, () -> parseDate("2025-110-12"));

    // Assert yyyy-MM-ddd INVALID format
    assertThrows(IllegalArgumentException.class, () -> parseDate("2025-11-120"));

    // Assert yyyyyMMddd INVALID format
    assertThrows(IllegalArgumentException.class, () -> parseDate("2025011120"));
  }

  @Test
  void testSafeParseDateSupportedDateFormats() {
    // Assert yyyyMMdd format
    Date date = safeParseDate("20031202");
    assertNotNull(date);
    LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2003, localDate.getYear());
    assertEquals(12, localDate.getMonthValue());
    assertEquals(2, localDate.getDayOfMonth());

    // Assert yyyy-MM-dd format
    date = safeParseDate("2005-10-12");
    assertNotNull(date);
    localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2005, localDate.getYear());
    assertEquals(10, localDate.getMonthValue());
    assertEquals(12, localDate.getDayOfMonth());

    // Assert yyyy-MM format
    date = safeParseDate("2022-05");
    assertNotNull(date);
    localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2022, localDate.getYear());
    assertEquals(5, localDate.getMonthValue());

    // Assert yyyy format
    date = safeParseDate("2023");
    assertNotNull(date);
    localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    assertEquals(2023, localDate.getYear());

    // Assert yyyy-MMM-dd INVALID format
    date = safeParseDate("2025-110-12");
    assertNull(date);

    // Assert yyyy-MM-ddd INVALID format
    date = safeParseDate("2025-11-120");
    assertNull(date);

    // Assert yyyyyMMddd INVALID format
    date = safeParseDate("2025011120");
    assertNull(date);
  }

  @Test
  void testSafeParseDateInvalidDateFormats() {
    // Assert yyyy-MMM-dd INVALID format
    Date date = safeParseDate("2025-110-12");
    assertNull(date);

    // Assert yyyy-MMM INVALID format
    date = safeParseDate("2025-111");
    assertNull(date);

    // Assert yyyyyMMddd INVALID format
    date = safeParseDate("2025011120");
    assertNull(date);
  }

  @Test
  void testDateTruncMonth() {
    Date dateA = getDate(2024, 3, 9);
    Date dateB = getDate(2024, 10, 20);

    assertEquals(getDate(2024, 3, 1), DateUtils.dateTruncMonth(dateA));
    assertEquals(getDate(2024, 10, 1), DateUtils.dateTruncMonth(dateB));
  }

  @Test
  void testPlusOneDay() {
    Date aDay = toMediumDate("2021-01-01");
    Date theDayAfter = toMediumDate("2021-01-02");
    assertThat(theDayAfter, is(plusOneDay(aDay)));
  }

  @Test
  void testPlusOneSqlDay() {
    java.sql.Date sqlDay = new java.sql.Date(toMediumDate("2021-01-01").getTime());
    Date theDayAfter = toMediumDate("2021-01-02");
    assertThat(theDayAfter, is(plusOneDay(sqlDay)));
  }

  @Test
  void testMinusOneDay() {
    Date aDay = toMediumDate("2021-01-01");
    Date theDayBefore = toMediumDate("2020-12-31");
    assertThat(theDayBefore, is(minusOneDay(aDay)));
  }

  @Test
  void testMinusOneSqlDay() {
    java.sql.Date sqlDay = new java.sql.Date(toMediumDate("2021-01-01").getTime());
    Date theDayBefore = toMediumDate("2020-12-31");
    assertThat(theDayBefore, is(minusOneDay(sqlDay)));
  }
}
