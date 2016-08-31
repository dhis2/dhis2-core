package org.hisp.dhis.calendar;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.Date;
import java.util.List;

/**
 * Generic interface for representing a Calendar.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @see DateTimeUnit
 * @see DateInterval
 * @see DateIntervalType
 */
public interface Calendar
{
    /**
     * Name of this calendar.
     *
     * @return Name of calendar.
     */
    String name();

    /**
     * Date format for this calendar
     *
     * @return Default date format
     */
    String getDateFormat();

    /**
     * Set date format for this calendar
     *
     * @param dateFormat Date format to use for this calendar
     */
    void setDateFormat( String dateFormat );

    /**
     * Formats dateUnit using dateFormat
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @return Default date format
     * @see #getDateFormat()
     */
    String formattedDate( DateTimeUnit dateTimeUnit );

    /**
     * Formats dateUnit using supplied date format
     *
     * @param dateFormat   Date format to use
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @return Default date format
     * @see #getDateFormat()
     */
    String formattedDate( String dateFormat, DateTimeUnit dateTimeUnit );

    /**
     * Formats dateUnit using dateFormat and ISO 8601
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @return Default date format
     * @see #getDateFormat()
     */
    String formattedIsoDate( DateTimeUnit dateTimeUnit );

    /**
     * Convert local calendar to an ISO 8601 DateUnit.
     *
     * @param year  Local year
     * @param month Local month
     * @param day   Local day
     * @return DateUnit representing local date in ISO 8601
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">http://en.wikipedia.org/wiki/ISO_8601</a>
     */
    DateTimeUnit toIso( int year, int month, int day );

    /**
     * Convert local calendar to an ISO 8601 DateUnit.
     *
     * @param date Date formatted using default date format
     * @return DateUnit representing local date in ISO 8601
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">http://en.wikipedia.org/wiki/ISO_8601</a>
     */
    DateTimeUnit toIso( String date );

    /**
     * Convert local calendar to an ISO 8601 DateUnit.
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @return DateUnit representing local date in ISO 8601
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">http://en.wikipedia.org/wiki/ISO_8601</a>
     */
    DateTimeUnit toIso( DateTimeUnit dateTimeUnit );

    /**
     * Convert from ISO 8601 to local DateUnit.
     *
     * @param year  ISO 8601 year
     * @param month ISO 8601 month
     * @param day   ISO 8601 day
     * @return DateUnit representing ISO 8601 in local
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">http://en.wikipedia.org/wiki/ISO_8601</a>
     */
    DateTimeUnit fromIso( int year, int month, int day );

    /**
     * Convert from ISO 8601 to local DateUnit.
     *
     * @param dateTimeUnit DateUnit representing ISO 8601 year, month, day
     * @return DateUnit representing ISO 8601 in local
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">http://en.wikipedia.org/wiki/ISO_8601</a>
     */
    DateTimeUnit fromIso( DateTimeUnit dateTimeUnit );

    /**
     * Convert from ISO 8601 Jdk Date to local DateUnit.
     *
     * @param date Date to convert from
     * @return DateUnit representing ISO 8601 in local
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">http://en.wikipedia.org/wiki/ISO_8601</a>
     */
    DateTimeUnit fromIso( Date date );

    /**
     * Gets interval of type based on DateUnit
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param type         Interval type to get
     * @param offset       Offset to start at, can be negative of positive
     * @param length       How many periods to asks for, i.e. type = MONTH, length = 2, two months
     * @return Interval for interval type based on dateUnit
     * @see DateIntervalType
     */
    DateInterval toInterval( DateTimeUnit dateTimeUnit, DateIntervalType type, int offset, int length );

    /**
     * Gets interval of type based on DateUnit using default options, 0 for offset, 1 for length
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param type         Interval type to get
     * @return Interval for interval type based on dateUnit
     * @see DateIntervalType
     */
    DateInterval toInterval( DateTimeUnit dateTimeUnit, DateIntervalType type );

    /**
     * Gets interval of type based on today's date
     *
     * @param type Interval type to get
     * @return Interval for interval type based on dateUnit
     * @see DateIntervalType
     */
    DateInterval toInterval( DateIntervalType type );

    /**
     * Gets interval of type based on today's date
     *
     * @param type   Interval type to get
     * @param offset Offset to start at, can be negative of positive
     * @param length How many periods to asks for, i.e. type = MONTH, length = 2, two months
     * @return Interval for interval type based on dateUnit
     * @see DateIntervalType
     */
    DateInterval toInterval( DateIntervalType type, int offset, int length );

    /**
     * Gets interval of type based on DateUnit
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param type         Interval type to get
     * @param offset       Offset to start at, can be negative of positive
     * @param length       How many periods to asks for, i.e. type = MONTH, length = 2, two months
     * @param periods      How many periods to generate
     * @return Interval for interval type based on dateUnit
     * @see DateIntervalType
     */
    List<DateInterval> toIntervals( DateTimeUnit dateTimeUnit, DateIntervalType type, int offset, int length, int periods );

    /**
     * Gets current date as local DateUnit
     *
     * @return Today date as local DateUnit
     */
    DateTimeUnit today();

    /**
     * Gets the number of months in a calendar year.
     *
     * @return Number of months in a year
     */
    int monthsInYear();

    /**
     * Gets the number of days in a calendar week.
     *
     * @return Number of days in a week
     */
    int daysInWeek();

    /**
     * Gets the number of days in a calendar year.
     *
     * @return Number of days in this calendar year
     */
    int daysInYear( int year );

    /**
     * Gets the number of days in a calendar year/month.
     *
     * @return Number of days in this calendar year/month
     */
    int daysInMonth( int year, int month );

    /**
     * Gets the number of weeks in a calendar year.
     *
     * @return Number of weeks in this calendar year
     */
    int weeksInYear( int year );

    /**
     * Gets week number using local DateUnit, week number is calculated based on
     * ISO 8601 week numbers
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @return Week number
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">http://en.wikipedia.org/wiki/ISO_8601</a>
     * @see <a href="http://en.wikipedia.org/wiki/ISO_week_date">http://en.wikipedia.org/wiki/ISO_week_date</a>
     */
    int isoWeek( DateTimeUnit dateTimeUnit );

    /**
     * Returns week number using local DateUnit, week number is calculated based on local calendar.
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @return Week number
     */
    int week( DateTimeUnit dateTimeUnit );

    /**
     * Gets the ISO 8601 weekday for this local DateUnit, using ISO 8601 day numbering,
     * 1=Monday => 7=Sunday.
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @return Weekday number
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">http://en.wikipedia.org/wiki/ISO_8601</a>
     * @see <a href="http://en.wikipedia.org/wiki/ISO_week_date">http://en.wikipedia.org/wiki/ISO_week_date</a>
     */
    int isoWeekday( DateTimeUnit dateTimeUnit );

    /**
     * Gets the local weekday for this local DateUnit, using ISO 8601 day numbering,
     * 1=Monday => 7=Sunday.
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @return Weekday number
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">http://en.wikipedia.org/wiki/ISO_8601</a>
     * @see <a href="http://en.wikipedia.org/wiki/ISO_week_date">http://en.wikipedia.org/wiki/ISO_week_date</a>
     */
    int weekday( DateTimeUnit dateTimeUnit );

    /**
     * Gets the (untranslated) I18n key for local month
     *
     * @param month Month to fetch key for
     * @return I18n Key for this month
     * @see <a href="http://en.wikipedia.org/wiki/Internationalization_and_localization">http://en.wikipedia.org/wiki/Internationalization_and_localization</a>
     */
    String nameOfMonth( int month );

    /**
     * Gets the (untranslated) I18n short key for local month
     *
     * @param month Month to fetch key for
     * @return I18n Key for this month
     * @see <a href="http://en.wikipedia.org/wiki/Internationalization_and_localization">http://en.wikipedia.org/wiki/Internationalization_and_localization</a>
     */
    String shortNameOfMonth( int month );

    /**
     * Gets the (untranslated) I18n key for local day
     *
     * @param day Day to fetch key for
     * @return I18n Key for this day
     * @see <a href="http://en.wikipedia.org/wiki/Internationalization_and_localization">http://en.wikipedia.org/wiki/Internationalization_and_localization</a>
     */
    String nameOfDay( int day );

    /**
     * Gets the (untranslated) I18n short key for local day
     *
     * @param day Day to fetch key for
     * @return I18n Key for this day
     * @see <a href="http://en.wikipedia.org/wiki/Internationalization_and_localization">http://en.wikipedia.org/wiki/Internationalization_and_localization</a>
     */
    String shortNameOfDay( int day );

    /**
     * Returns a new dateUnit with specified number of days added
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param days         Days to add
     * @return dateUnit + days
     */
    DateTimeUnit plusDays( DateTimeUnit dateTimeUnit, int days );

    /**
     * Returns a new dateUnit with specified number of days subtracted
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param days         Days to subtract
     * @return dateUnit - days
     */
    DateTimeUnit minusDays( DateTimeUnit dateTimeUnit, int days );

    /**
     * Returns a new dateUnit with specified number of weeks added
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param weeks        Weeks to add
     * @return dateUnit + weeks
     */
    DateTimeUnit plusWeeks( DateTimeUnit dateTimeUnit, int weeks );

    /**
     * Returns a new dateUnit with specified number of weeks subtracted
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param weeks        Weeks to subtract
     * @return dateUnit - weeks
     */
    DateTimeUnit minusWeeks( DateTimeUnit dateTimeUnit, int weeks );

    /**
     * Returns a new dateUnit with specified number of months added
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param months       Months to add
     * @return dateUnit + months
     */
    DateTimeUnit plusMonths( DateTimeUnit dateTimeUnit, int months );

    /**
     * Returns a new dateUnit with specified number of months subtracted
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param months       Months to subtract
     * @return dateUnit - months
     */
    DateTimeUnit minusMonths( DateTimeUnit dateTimeUnit, int months );

    /**
     * Returns a new dateUnit with specified number of years added
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param years        Years to add
     * @return dateUnit + years
     */
    DateTimeUnit plusYears( DateTimeUnit dateTimeUnit, int years );

    /**
     * Returns a new dateUnit with specified number of years subtracted
     *
     * @param dateTimeUnit DateUnit representing local year, month, day
     * @param years        Years to subtract
     * @return dateUnit - years
     */
    DateTimeUnit minusYears( DateTimeUnit dateTimeUnit, int years );

    /**
     * Is this calendar based on ISO 8601 (Iso8601 / Gregorian)
     *
     * @return is iso8601 based
     */
    boolean isIso8601();

    /**
     * Get start of year as month/day, important especially for Nepali calendar
     *
     * @return
     */
    DateTimeUnit isoStartOfYear( int year );
}
