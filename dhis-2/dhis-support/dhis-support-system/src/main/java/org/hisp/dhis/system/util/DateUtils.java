package org.hisp.dhis.system.util;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Lars Helge Overland
 */
public class DateUtils
{
    private static DateTimeFormatter ISO8601 = DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );

    private static DateTimeFormatter ISO8601_NO_TZ = DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSS" );

    private static final DateTimeParser[] SUPPORTED_DATE_FORMAT_PARSERS = {
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSS" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ssZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mmZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HHZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm:ssZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM" ).getParser(),
        DateTimeFormat.forPattern( "yyyy" ).getParser()
    };

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
        .append( null, SUPPORTED_DATE_FORMAT_PARSERS ).toFormatter();

    private static final DateTimeParser[] SUPPORTED_DATE_TIME_FORMAT_PARSERS = {
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ssZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mmZ" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss" ).getParser(),
        DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm" ).getParser()
    };

    private static final DateTimeFormatter DATE_TIME_FORMAT = (new DateTimeFormatterBuilder())
        .append( null, SUPPORTED_DATE_TIME_FORMAT_PARSERS ).toFormatter();

    public static final PeriodFormatter DAY_SECOND_FORMAT = new PeriodFormatterBuilder()
        .appendDays().appendSuffix( " d" ).appendSeparator( ", " )
        .appendHours().appendSuffix( " h" ).appendSeparator( ", " )
        .appendMinutes().appendSuffix( " m" ).appendSeparator( ", " )
        .appendSeconds().appendSuffix( " s" ).appendSeparator( ", " ).toFormatter();

    private static final DateTimeFormatter MEDIUM_DATE_FORMAT = DateTimeFormat.forPattern( "yyyy-MM-dd" );
    private static final DateTimeFormatter LONG_DATE_FORMAT = DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss" );
    private static final DateTimeFormatter HTTP_DATE_FORMAT = DateTimeFormat.forPattern( "EEE, dd MMM yyyy HH:mm:ss 'GMT'" ).withLocale( Locale.ENGLISH );
    private static final DateTimeFormatter TIMESTAMP_UTC_TZ_FORMAT = DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ).withZoneUTC();

    private static final double DAYS_IN_YEAR = 365.0;
    private static final long MS_PER_DAY = 86400000;
    private static final long MS_PER_S = 1000;

    private static final Pattern DURATION_PATTERN = Pattern.compile( "^(\\d+)(d|h|m|s)$" );

    private static final Map<String, ChronoUnit> TEMPORAL_MAP = ImmutableMap.of(
        "d", ChronoUnit.DAYS, "h", ChronoUnit.HOURS, "m", ChronoUnit.MINUTES, "s", ChronoUnit.SECONDS );

    /**
     * Returns date formatted as ISO 8601
     */
    public static String getIso8601( Date date )
    {
        return date != null ? ISO8601.print( new DateTime( date ) ) : null;
    }

    /**
     * Returns date formatted as ISO 8601, without any TZ info
     */
    public static String getIso8601NoTz( Date date )
    {
        return date != null ? ISO8601_NO_TZ.print( new DateTime( date ) ) : null;
    }

    /**
     * Converts a Date to the GMT timezone and formats it to the format yyyy-MM-dd HH:mm:ssZ.
     *
     * @param date the Date to parse.
     * @return A formatted date string.
     */
    public static String getLongGmtDateString( Date date )
    {
        return date != null ? TIMESTAMP_UTC_TZ_FORMAT.print( new DateTime( date ) ) : null;
    }

    /**
     * Formats a Date to the format yyyy-MM-dd HH:mm:ss.
     *
     * @param date the Date to parse.
     * @return A formatted date string.
     */
    public static String getLongDateString( Date date )
    {
        return date != null ? LONG_DATE_FORMAT.print( new DateTime( date ) ) : null;
    }

    /**
     * Formats a Date to the format yyyy-MM-dd HH:mm:ss.
     *
     * @return A formatted date string.
     */
    public static String getLongDateString()
    {
        return getLongDateString( Calendar.getInstance().getTime() );
    }

    /**
     * Formats a Date to the format yyyy-MM-dd.
     *
     * @param date the Date to parse.
     * @return A formatted date string. Null if argument is null.
     */
    public static String getMediumDateString( Date date )
    {
        return date != null ? MEDIUM_DATE_FORMAT.print( new DateTime( date ) ) : null;
    }

    /**
     * Formats the current Date to the format YYYY-MM-DD.
     *
     * @return A formatted date string.
     */
    public static String getMediumDateString()
    {
        return getMediumDateString( Calendar.getInstance().getTime() );
    }

    /**
     * Formats a Date according to the HTTP specification standard date format.
     *
     * @param date the Date to format.
     * @return a formatted string.
     */
    public static String getHttpDateString( Date date )
    {
        return date != null ? (HTTP_DATE_FORMAT.print( new DateTime( date ) )) : null;
    }

    /**
     * Returns the latest of the two given dates.
     *
     * @param date1 the first date, can be null.
     * @param date2 the second date, can be null.
     * @return the latest of the two given dates.
     */
    public static Date max( Date date1, Date date2 )
    {
        if ( date1 == null )
        {
            return date2;
        }

        return date2 != null ? (date1.after( date2 ) ? date1 : date2) : date1;
    }

    /**
     * Returns the latest of the given dates.
     *
     * @param date the collection of dates.
     * @return the latest of the given dates.
     */
    public static Date max( Collection<Date> dates )
    {
        Date latest = null;

        for ( Date d : dates )
        {
            latest = max( d, latest );
        }

        return latest;
    }

    /**
     * Returns the earliest of the two given dates.
     *
     * @param date1 the first date, can be null.
     * @param date2 the second date, can be null.
     * @return the latest of the two given dates.
     */
    public static Date min( Date date1, Date date2 )
    {
        if ( date1 == null )
        {
            return date2;
        }

        return date2 != null ? (date1.before( date2 ) ? date1 : date2) : date1;
    }

    /**
     * Returns the earliest of the given dates.
     *
     * @param date the collection of dates.
     * @return the earliest of the given dates.
     */
    public static Date min( Collection<Date> dates )
    {
        Date earliest = null;

        for ( Date d : dates )
        {
            earliest = min( d, earliest );
        }

        return earliest;
    }

    /**
     * Parses a date from a String on the format YYYY-MM-DD. Returns null if the
     * given string is null.
     *
     * @param dateString the String to parse.
     * @return a Date based on the given String.
     * @throws IllegalArgumentException if the given string is invalid.
     */
    public static Date getMediumDate( String string )
    {
        return string != null ? MEDIUM_DATE_FORMAT.parseDateTime( string ).toDate() : null;
    }

    /**
     * Tests if the given base date is between the given start date and end
     * date, including the dates themselves.
     *
     * @param baseDate  the date used as base for the test.
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return <code>true</code> if the base date is between the start date
     * and end date, <code>false</code> otherwise.
     */
    public static boolean between( Date baseDate, Date startDate, Date endDate )
    {
        if ( startDate.equals( endDate ) || endDate.before( startDate ) )
        {
            return false;
        }

        if ( (startDate.before( baseDate ) || startDate.equals( baseDate ))
            && (endDate.after( baseDate ) || endDate.equals( baseDate )) )
        {
            return true;
        }

        return false;
    }

    /**
     * Tests if the given base date is strictly between the given start date and
     * end date.
     *
     * @param baseDate  the date used as base for the test.
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return <code>true</code> if the base date is between the start date
     * and end date, <code>false</code> otherwise.
     */
    public static boolean strictlyBetween( Date baseDate, Date startDate, Date endDate )
    {
        if ( startDate.equals( endDate ) || endDate.before( startDate ) )
        {
            return false;
        }

        if ( startDate.before( baseDate ) && endDate.after( baseDate ) )
        {
            return true;
        }

        return false;
    }

    /**
     * Returns the number of days since 01/01/1970. The value is rounded off to
     * the floor value and does not take daylight saving time into account.
     *
     * @param date the date.
     * @return number of days since Epoch.
     */
    public static long getDays( Date date )
    {
        return date.getTime() / MS_PER_DAY;
    }

    /**
     * Returns the number of days between the start date (inclusive) and end
     * date (exclusive). The value is rounded off to the floor value and does
     * not take daylight saving time into account.
     *
     * @param startDate the start-date.
     * @param endDate   the end-date.
     * @return the number of days between the start and end-date.
     */
    public static long getDays( Date startDate, Date endDate )
    {
        return (endDate.getTime() - startDate.getTime()) / MS_PER_DAY;
    }

    /**
     * Returns the number of days between the start date (inclusive) and end
     * date (inclusive). The value is rounded off to the floor value and does
     * not take daylight saving time into account.
     *
     * @param startDate the start-date.
     * @param endDate   the end-date.
     * @return the number of days between the start and end-date.
     */
    public static long getDaysInclusive( Date startDate, Date endDate )
    {
        return getDays( startDate, endDate ) + 1;
    }

    /**
     * Calculates the number of days between the start and end-date. Note this
     * method is taking daylight saving time into account and has a performance
     * overhead.
     *
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return the number of days between the start and end date.
     */
    public static int daysBetween( Date startDate, Date endDate )
    {
        final Days days = Days.daysBetween( new DateTime( startDate ), new DateTime( endDate ) );

        return days.getDays();
    }

    /**
     * Calculates the number of months between the start and end-date. Note this
     * method is taking daylight saving time into account and has a performance
     * overhead.
     *
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return the number of months between the start and end date.
     */
    public static int monthsBetween( Date startDate, Date endDate )
    {
        final Months days = Months.monthsBetween( new DateTime( startDate ), new DateTime( endDate ) );

        return days.getMonths();
    }

    /**
     * Calculates the number of days between Epoch and the given date.
     *
     * @param date the date.
     * @return the number of days between Epoch and the given date.
     */
    public static int daysSince1900( Date date )
    {
        final Calendar calendar = Calendar.getInstance();

        calendar.clear();
        calendar.set( 1900, 0, 1 );

        return daysBetween( calendar.getTime(), date );
    }

    /**
     * Returns Epoch date, ie. 01/01/1970.
     *
     * @return Epoch date, ie. 01/01/1970.
     */
    public static Date getEpoch()
    {
        final Calendar calendar = Calendar.getInstance();

        calendar.clear();
        calendar.set( 1970, 0, 1 );

        return calendar.getTime();
    }

    /**
     * Returns a date formatted in ANSI SQL.
     *
     * @param date the Date.
     * @return a date String.
     */
    public static String getSqlDateString( Date date )
    {
        Calendar cal = Calendar.getInstance();

        cal.setTime( date );

        int year = cal.get( Calendar.YEAR );
        int month = cal.get( Calendar.MONTH ) + 1;
        int day = cal.get( Calendar.DAY_OF_MONTH );

        String yearString = String.valueOf( year );
        String monthString = month < 10 ? "0" + month : String.valueOf( month );
        String dayString = day < 10 ? "0" + day : String.valueOf( day );

        return yearString + "-" + monthString + "-" + dayString;
    }

    private static final String DEFAULT_DATE_REGEX = "\\b(?<year>\\d{4})-(?<month>0[1-9]|1[0-2])-(?<day>0[1-9]|[1-2][0-9]|3[0-2])\\b";

    private static final Pattern DEFAULT_DATE_REGEX_PATTERN = Pattern.compile( DEFAULT_DATE_REGEX );

    /**
     * This method checks whether the String inDate is a valid date following
     * the format "yyyy-MM-dd".
     *
     * @param dateString the string to be checked.
     * @return true/false depending on whether the string is a date according to the format "yyyy-MM-dd".
     */
    public static boolean dateIsValid( String dateString )
    {
        return dateIsValid( PeriodType.getCalendar(), dateString );
    }

    /**
     * This method checks whether the String inDate is a valid date following
     * the format "yyyy-MM-dd".
     *
     * @param calendar   Calendar to be used
     * @param dateString the string to be checked.
     * @return true/false depending on whether the string is a date according to the format "yyyy-MM-dd".
     */
    public static boolean dateIsValid( org.hisp.dhis.calendar.Calendar calendar, String dateString )
    {
        Matcher matcher = DEFAULT_DATE_REGEX_PATTERN.matcher( dateString );

        if ( !matcher.matches() )
        {
            return false;
        }

        DateTimeUnit dateTime = new DateTimeUnit(
            Integer.valueOf( matcher.group( "year" ) ),
            Integer.valueOf( matcher.group( "month" ) ),
            Integer.valueOf( matcher.group( "day" ) )
        );

        return calendar.isValid( dateTime );
    }

    /**
     * This method checks whether the String dateTimeString is a valid datetime following
     * the format "yyyy-MM-dd".
     *
     * @param dateTimeString the string to be checked.
     * @return true/false depending on whether the string is a valid datetime according to the format "yyyy-MM-dd".
     */
    public static boolean dateTimeIsValid( final String dateTimeString )
    {
        try
        {
            DATE_TIME_FORMAT.parseDateTime( dateTimeString );
            return true;
        }
        catch ( IllegalArgumentException ex )
        {
            return false;
        }
    }

    /**
     * Returns the number of seconds until the next day at the given hour.
     *
     * @param hour the hour.
     * @return number of seconds.
     */
    public static long getSecondsUntilTomorrow( int hour )
    {
        Date date = getDateForTomorrow( hour );
        return (date.getTime() - new Date().getTime()) / MS_PER_S;
    }

    /**
     * Returns a date set to tomorrow at the given hour.
     *
     * @param hour the hour.
     * @return a date.
     */
    public static Date getDateForTomorrow( int hour )
    {
        Calendar cal = PeriodType.createCalendarInstance();
        cal.add( Calendar.DAY_OF_YEAR, 1 );
        cal.set( Calendar.HOUR_OF_DAY, hour );
        return cal.getTime();
    }

    /**
     * This method adds days to a date
     *
     * @param date the date.
     * @param days the number of days to add.
     */
    public static Date getDateAfterAddition( Date date, int days )
    {
        Calendar cal = Calendar.getInstance();

        cal.setTime( date );
        cal.add( Calendar.DATE, days );

        return cal.getTime();
    }

    /**
     * Returns the annualization factor for the given indicator and start-end date interval.
     */
    public static double getAnnualizationFactor( Indicator indicator, Date startDate, Date endDate )
    {
        double factor = 1.0;

        if ( indicator.isAnnualized() )
        {
            final int daysInPeriod = DateUtils.daysBetween( startDate, endDate ) + 1;

            factor = DAYS_IN_YEAR / daysInPeriod;
        }

        return factor;
    }

    /**
     * Sets the name property of each period based on the given I18nFormat.
     */
    public static List<Period> setNames( List<Period> periods, I18nFormat format )
    {
        for ( Period period : periods )
        {
            if ( period != null )
            {
                period.setName( format.formatPeriod( period ) );
            }
        }

        return periods;
    }

    /**
     * Returns a pretty string representing the interval between the given
     * start and end dates using a day, month, second format.
     *
     * @param start the start date.
     * @param end   the end date.
     * @return a string, or null if the given start or end date is null.
     */
    public static String getPrettyInterval( Date start, Date end )
    {
        if ( start == null || end == null )
        {
            return null;
        }

        long diff = end.getTime() - start.getTime();

        return DAY_SECOND_FORMAT.print( new org.joda.time.Period( diff ) );
    }

    /**
     * Returns a pretty string representing the interval between the given
     * start and end dates using a day, month, second format.
     *
     * @param ms the number of milliseconds in the interval.
     * @return a string, or null if the given start or end date is null.
     */
    public static String getPrettyInterval( long ms )
    {
        return DAY_SECOND_FORMAT.print( new org.joda.time.Period( ms ) );
    }

    /**
     * Parses the given string into a Date using the supported date formats.
     * Returns null if the string cannot be parsed.
     *
     * @param dateString the date string.
     * @return a date.
     */
    public static Date parseDate( final String dateString )
    {
        if ( StringUtils.isEmpty( dateString ) )
        {
            return null;
        }

        return DATE_FORMATTER.parseDateTime( dateString ).toDate();
    }

    /**
     * Creates a {@link java.util.Date} from the given {@link java.time.LocalDateTime}
     * based on the UTC time zone.
     *
     * @param time the LocalDateTime.
     * @return a Date.
     */
    public static Date getDate( LocalDateTime time )
    {
        Instant instant = time.toInstant( ZoneOffset.UTC );

        return Date.from( instant );
    }

    /**
     * Return the current date minus the duration specified by the given string.
     *
     * @param duration the duration string, see {@link DateUtils.getDuration}.
     * @return a Date.
     */
    public static Date nowMinusDuration( String duration )
    {
        Duration dr = DateUtils.getDuration( duration );

        LocalDateTime time = LocalDateTime.now().minus( dr );

        return DateUtils.getDate( time );
    }

    /**
     * Parses the given string into a {@link java.time.Duration} object. The
     * string syntax is [amount][unit]. The supported units are:
     * <p>
     * <ul>
     * <li>"d": Days</li>
     * <li>"h": Hours</li>
     * <li>"m": Minutes</li>
     * <li>"s": Seconds</li>
     * </ul>
     *
     * @param duration the duration string, an example describing 12 days is "12d".
     * @return a Duration object, or null if the duration string is invalid.
     */
    public static Duration getDuration( String duration )
    {
        Matcher matcher = DURATION_PATTERN.matcher( duration );

        if ( !matcher.find() )
        {
            return null;
        }

        long amount = Long.valueOf( matcher.group( 1 ) );
        String unit = matcher.group( 2 );

        ChronoUnit chronoUnit = TEMPORAL_MAP.get( unit );

        if ( chronoUnit == null )
        {
            return null;
        }

        return Duration.of( amount, chronoUnit );
    }
    
    /**
     * Converts the given {@link Date} to a {@link Timestamp}.
     *  
     * @param date the date to convert.
     * @return a time stamp.
     */
    public static Timestamp asTimestamp( Date date )
    {
        return new Timestamp( date.getTime() );
    }

    /**
     * Converts the given {@link Date} to a {@link java.sql.Date}.
     *  
     * @param date the date to convert.
     * @return a date.
     */
    public static java.sql.Date asSqlDate( Date date )
    {
        return new java.sql.Date( date.getTime() );
    }
}
