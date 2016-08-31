package org.hisp.dhis.system.util;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.hisp.dhis.period.Period.DEFAULT_DATE_FORMAT;

/**
 * @author Lars Helge Overland
 */
public class DateUtils
{
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

    private static final DateTimeFormatter DATE_TIME_FORMATTER = ( new DateTimeFormatterBuilder() )
            .append(null, SUPPORTED_DATE_TIME_FORMAT_PARSERS).toFormatter();

    private static final String SEP = ", ";

    public static final PeriodFormatter DAY_SECOND_FORMAT = new PeriodFormatterBuilder()
        .appendDays().appendSuffix( " d" ).appendSeparator( SEP )
        .appendHours().appendSuffix( " h" ).appendSeparator( SEP )
        .appendMinutes().appendSuffix( " m" ).appendSeparator( SEP )
        .appendSeconds().appendSuffix( " s" ).appendSeparator( SEP ).toFormatter();

    //TODO replace with FastDateParser, SimpleDateFormat is not thread-safe

    /**
     * Used by web API and utility methods.
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    public static final SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat( TIMESTAMP_PATTERN );
    public static final SimpleDateFormat ACCESS_DATE_FORMAT = new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss" );
    public static final SimpleDateFormat HTTP_DATE_FORMAT = new SimpleDateFormat( "EEE, dd MMM yyyy HH:mm:ss" );

    public static final double DAYS_IN_YEAR = 365.0;

    private static final long MS_PER_DAY = 86400000;
    private static final long MS_PER_S = 1000;

    /**
     * Formats a Date to the Access date format.
     *
     * @param date the Date to parse.
     * @return a formatted date string.
     */
    public static String getAccessDateString( Date date )
    {
        return date != null ? ACCESS_DATE_FORMAT.format( date ) : null;
    }

    /**
     * Converts a Date to the GMT timezone and formats it to the format yyyy-MM-dd HH:mm:ssZ.
     *
     * @param date the Date to parse.
     * @return A formatted date string.
     */
    public static String getLongGmtDateString( Date date )
    {
        if ( date == null )
        {
            return null;
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
        simpleDateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return simpleDateFormat.format( date );
    }


    /**
     * Formats a Date to the format yyyy-MM-dd HH:mm:ss.
     *
     * @param date the Date to parse.
     * @return A formatted date string.
     */
    public static String getLongDateString( Date date )
    {
        return date != null ? LONG_DATE_FORMAT.format( date ) : null;
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
     * Formats a Date to the format YYYY-MM-DD.
     *
     * @param date the Date to parse.
     * @return A formatted date string. Null if argument is null.
     */
    public static String getMediumDateString( Date date )
    {
        final SimpleDateFormat format = new SimpleDateFormat();

        format.applyPattern( DEFAULT_DATE_FORMAT );

        return date != null ? format.format( date ) : null;
    }

    /**
     * Returns the latest of the two given dates.
     *
     * @param date1 the first date.
     * @param date2 the second date.
     * @return the latest of the two given dates.
     */
    public static Date max( Date date1, Date date2 )
    {
        if ( date1 == null )
        {
            return date2 != null ? date2 : null;
        }

        return date2 != null ? (date1.after( date2 ) ? date1 : date2) : date1;
    }

    /**
     * Returns the latest of the given dates.
     *
     * @param date the dates.
     * @return the latest of the given dates.
     */
    public static Date max( Date... date )
    {
        Date latest = null;

        for ( Date d : date )
        {
            latest = max( d, latest );
        }

        return latest;
    }

    /**
     * Formats a Date to the format YYYY-MM-DD.
     *
     * @param date         the Date to parse.
     * @param defaultValue the return value if the date argument is null.
     * @return A formatted date string. The defaultValue argument if date
     * argument is null.
     */
    public static String getMediumDateString( Date date, String defaultValue )
    {
        return date != null ? getMediumDateString( date ) : defaultValue;
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
        return HTTP_DATE_FORMAT.format( date ) + " GMT";
    }

    /**
     * Returns yesterday's date formatted according to the HTTP specification
     * standard date format.
     *
     * @param date the Date to format.
     * @return a formatted string.
     */
    public static String getExpiredHttpDateString()
    {
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.DAY_OF_YEAR, -1 );

        return getHttpDateString( cal.getTime() );
    }

    /**
     * Parses the given string into a Date using the default date format which is
     * yyyy-MM-dd. Returns null if the string cannot be parsed.
     *
     * @param dateString the date string.
     * @return a date.
     */
    public static Date getDefaultDate( String dateString )
    {
        try
        {
            return new SimpleDateFormat( DEFAULT_DATE_FORMAT ).parse( dateString );
        }
        catch ( Exception ex )
        {
            return null;
        }
    }

    /**
     * Parses a date from a String on the format YYYY-MM-DD.
     *
     * @param dateString the String to parse.
     * @return a Date based on the given String.
     */
    public static Date getMediumDate( String dateString )
    {
        try
        {
            final SimpleDateFormat format = new SimpleDateFormat();

            format.applyPattern( DEFAULT_DATE_FORMAT );

            return dateString != null && dateIsValid( dateString ) ? format.parse( dateString ) : null;
        }
        catch ( ParseException ex )
        {
            throw new RuntimeException( "Failed to parse medium date", ex );
        }
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

    private static final String DEFAULT_DATE_REGEX = "\\b\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-2])\\b";

    /**
     * This method checks whether the String inDate is a valid date following
     * the format "yyyy-MM-dd".
     *
     * @param dateString the string to be checked.
     * @return true/false depending on whether the string is a date according to the format "yyyy-MM-dd".
     */
    public static boolean dateIsValid( String dateString )
    {
        return dateString.matches( DEFAULT_DATE_REGEX );
    }

    /**
     * This method checks whether the String dateTimeString is a valid datetime following
     * the format "yyyy-MM-dd".
     *
     * @param dateTimeString the string to be checked.
     * @return true/false depending on whether the string is a valid datetime according to the format "yyyy-MM-dd".
     */
    public static boolean dateTimeIsValid(final String dateTimeString)
    {
        try
        {
            DATE_TIME_FORMATTER.parseDateTime(dateTimeString);
            return true;
        }
        catch( IllegalArgumentException ex )
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
     * This is a helper method for checking if the fromDate is later than the
     * toDate. This is necessary in case a user sends the dates with HTTP GET.
     *
     * @param fromDate
     * @param toDate
     * @return boolean
     */
    public static boolean checkDates( String fromDate, String toDate )
    {
        String formatString = DEFAULT_DATE_FORMAT;
        SimpleDateFormat sdf = new SimpleDateFormat( formatString );

        Date date1 = null;
        Date date2 = null;

        try
        {
            date1 = sdf.parse( fromDate );
            date2 = sdf.parse( toDate );
        }
        catch ( ParseException e )
        {
            return false; // The user hasn't specified any dates
        }

        return !date1.before( date2 );
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
        if ( dateString == null )
        {
            return null;
        }

        return DATE_FORMATTER.parseDateTime( dateString ).toDate();
    }
}
