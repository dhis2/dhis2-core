package org.hisp.dhis.calendar.impl;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.calendar.AbstractCalendar;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateInterval;
import org.hisp.dhis.calendar.DateIntervalType;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.calendar.exception.InvalidCalendarParametersException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.chrono.ISOChronology;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Hans Jacobson <jacobson.hans@gmail.com>
 */
@Component
public class PersianCalendar extends AbstractCalendar
{
    private static final DateTimeUnit START_PERSIAN = new DateTimeUnit( 1353, 1, 1, java.util.Calendar.THURSDAY );

    private static final DateTimeUnit START_ISO = new DateTimeUnit( 1974, 3, 21, java.util.Calendar.THURSDAY, true );

    private static final DateTimeUnit STOP_PERSIAN = new DateTimeUnit( 1418, 12, 29, java.util.Calendar.MONDAY );

    private static final DateTimeUnit STOP_ISO = new DateTimeUnit( 2040, 3, 19, java.util.Calendar.MONDAY, true );

    private static final Calendar SELF = new PersianCalendar();

    public static Calendar getInstance()
    {
        return SELF;
    }

    @Override
    public String name()
    {
        return "persian";
    }

    @Override
    public DateTimeUnit toIso( DateTimeUnit dateTimeUnit )
    {

        if ( dateTimeUnit.getYear() >= START_ISO.getYear() && dateTimeUnit.getYear() <= STOP_ISO.getYear() )
        {
            return new DateTimeUnit( dateTimeUnit.getYear(), dateTimeUnit.getMonth(), dateTimeUnit.getDay(),
                dateTimeUnit.getDayOfWeek(), true );
        }

        if ( dateTimeUnit.getYear() > STOP_PERSIAN.getYear() ||
            dateTimeUnit.getYear() < START_PERSIAN.getYear() )
        {
            throw new InvalidCalendarParametersException(
                "Illegal PERSIAN year, must be between " + START_PERSIAN.getYear() + " and " +
                    STOP_PERSIAN.getYear() + ", was given " + dateTimeUnit.getYear() );
        }

        DateTime dateTime = START_ISO.toJodaDateTime();

        int totalDays = 0;

        for ( int year = START_PERSIAN.getYear(); year < dateTimeUnit.getYear(); year++ )
        {
            totalDays += getYearTotal( year );
        }

        for ( int month = START_PERSIAN.getMonth(); month < dateTimeUnit.getMonth(); month++ )
        {
            totalDays += getDaysFromMap( dateTimeUnit.getYear(), month );
        }

        totalDays += dateTimeUnit.getDay() - START_PERSIAN.getDay();

        dateTime = dateTime.plusDays( totalDays );

        return new DateTimeUnit( DateTimeUnit.fromJodaDateTime( dateTime ), true );
    }

    @Override
    public DateTimeUnit fromIso( Date date )
    {
        return fromIso( DateTimeUnit.fromJdkDate( date ) );
    }

    @Override
    public DateTimeUnit fromIso( DateTimeUnit dateTimeUnit )
    {
        if ( dateTimeUnit.getYear() >= START_PERSIAN.getYear() &&
            dateTimeUnit.getYear() <= STOP_PERSIAN.getYear() )
        {
            return new DateTimeUnit( dateTimeUnit.getYear(), dateTimeUnit.getMonth(), dateTimeUnit.getDay(),
                dateTimeUnit.getDayOfWeek(), false );
        }

        if ( dateTimeUnit.getYear() < START_ISO.getYear() || dateTimeUnit.getYear() > STOP_ISO.getYear() )
        {
            throw new InvalidCalendarParametersException(
                "Illegal ISO year, must be between " + START_ISO.getYear() + " and " + STOP_ISO.getYear() +
                    ", was given " + dateTimeUnit.getYear() );
        }

        DateTime start = START_ISO.toJodaDateTime();
        DateTime end = dateTimeUnit.toJodaDateTime();

        return plusDays( START_PERSIAN, Days.daysBetween( start, end ).getDays() );
    }

    @Override
    public DateInterval toInterval( DateTimeUnit dateTimeUnit, DateIntervalType type, int offset, int length )
    {
        switch ( type )
        {
            case ISO8601_YEAR:
                return toYearIsoInterval( dateTimeUnit, offset, length );
            case ISO8601_MONTH:
                return toMonthIsoInterval( dateTimeUnit, offset, length );
            case ISO8601_WEEK:
                return toWeekIsoInterval( dateTimeUnit, offset, length );
            case ISO8601_DAY:
                return toDayIsoInterval( dateTimeUnit, offset, length );
            default:
                return null;
        }
    }

    @Override
    public int daysInYear( int year )
    {
        return getYearTotal( year );
    }

    @Override
    public int daysInMonth( int year, int month )
    {
        int newYear = year;

        if ( year > START_ISO.getYear() )
        {

            if ( month < 4 )
            {
                newYear = year - 622;
            }
            else
            {
                newYear = year - 621;
            }
        }

        return getDaysFromMap( newYear, month );
    }

    @Override
    public int weeksInYear( int year )
    {
        DateTime dateTime = new DateTime( year, 1, 1, 0, 0,
            ISOChronology.getInstance( DateTimeZone.getDefault() ) );
        return dateTime.weekOfWeekyear().getMaximumValue();
    }

    @Override
    public int isoWeek( DateTimeUnit dateTimeUnit )
    {
        DateTime dateTime = toIso( dateTimeUnit )
            .toJodaDateTime( ISOChronology.getInstance( DateTimeZone.getDefault() ) );
        return dateTime.getWeekOfWeekyear();
    }

    @Override
    public int week( DateTimeUnit dateTimeUnit )
    {
        return isoWeek( dateTimeUnit );
    }

    @Override
    public int isoWeekday( DateTimeUnit dateTimeUnit )
    {

        DateTime dateTime = toIso( dateTimeUnit )
            .toJodaDateTime( ISOChronology.getInstance( DateTimeZone.getDefault() ) );
        return dateTime.getDayOfWeek();
    }

    @Override
    public int weekday( DateTimeUnit dateTimeUnit )
    {

        int dayOfWeek = (isoWeekday( dateTimeUnit ) + 1);

        if ( dayOfWeek > 7 )
        {
            return 1;
        }

        return dayOfWeek;
    }

    @Override
    public String nameOfMonth( int month )
    {
        if ( month > DEFAULT_I18N_MONTH_NAMES.length || month <= 0 )
        {
            return null;
        }

        return "persian." + DEFAULT_I18N_MONTH_NAMES[month - 1];
    }

    @Override
    public String shortNameOfMonth( int month )
    {
        if ( month > DEFAULT_I18N_MONTH_SHORT_NAMES.length || month <= 0 )
        {
            return null;
        }

        return "persian." + DEFAULT_I18N_MONTH_SHORT_NAMES[month - 1];
    }

    @Override
    public String nameOfDay( int day )
    {
        if ( day > DEFAULT_I18N_DAY_NAMES.length || day <= 0 )
        {
            return null;
        }

        return "persian." + DEFAULT_I18N_DAY_NAMES[day - 1];
    }

    @Override
    public String shortNameOfDay( int day )
    {
        if ( day > DEFAULT_I18N_DAY_SHORT_NAMES.length || day <= 0 )
        {
            return null;
        }

        return "persian." + DEFAULT_I18N_DAY_SHORT_NAMES[day - 1];
    }

    @Override
    public DateTimeUnit minusYears( DateTimeUnit dateTimeUnit, int years )
    {
        DateTimeUnit result = new DateTimeUnit( dateTimeUnit.getYear() - years, dateTimeUnit.getMonth(),
            dateTimeUnit.getDay(), dateTimeUnit.getDayOfWeek() );
        updateDateUnit( result );

        return result;
    }

    @Override
    public DateTimeUnit minusMonths( DateTimeUnit dateTimeUnit, int months )
    {
        DateTimeUnit result = new DateTimeUnit( dateTimeUnit );
        int newMonths = months;

        while ( newMonths != 0 )
        {
            result.setMonth( result.getMonth() - 1 );

            if ( result.getMonth() < 1 )
            {
                result.setMonth( monthsInYear() );
                result.setYear( result.getYear() - 1 );
            }

            newMonths--;
        }

        updateDateUnit( result );

        return result;
    }

    @Override
    public DateTimeUnit minusWeeks( DateTimeUnit dateTimeUnit, int weeks )
    {
        return minusDays( dateTimeUnit, weeks * daysInWeek() );
    }

    @Override
    public DateTimeUnit minusDays( DateTimeUnit dateTimeUnit, int days )
    {
        int curYear = dateTimeUnit.getYear();
        int curMonth = dateTimeUnit.getMonth();
        int curDay = dateTimeUnit.getDay();
        int dayOfWeek = dateTimeUnit.getDayOfWeek();
        int newDays = days;

        while ( newDays != 0 )
        {
            curDay--;

            if ( curDay == 0 )
            {
                curMonth--;

                if ( curMonth == 0 )
                {
                    curYear--;
                    curMonth = 12;
                }

                curDay = getDaysFromMap( curYear, curMonth );
            }

            dayOfWeek--;

            if ( dayOfWeek == 0 )
            {
                dayOfWeek = 7;
            }

            newDays--;
        }

        return new DateTimeUnit( curYear, curMonth, curDay, dayOfWeek );
    }

    @Override
    public DateTimeUnit plusYears( DateTimeUnit dateTimeUnit, int years )
    {
        DateTimeUnit result = new DateTimeUnit( dateTimeUnit.getYear() + years, dateTimeUnit.getMonth(),
            dateTimeUnit.getDay(), dateTimeUnit.getDayOfWeek() );
        updateDateUnit( result );

        return result;
    }

    @Override
    public DateTimeUnit plusMonths( DateTimeUnit dateTimeUnit, int months )
    {
        DateTimeUnit result = new DateTimeUnit( dateTimeUnit );
        int newMonths = months;

        while ( newMonths != 0 )
        {
            result.setMonth( result.getMonth() + 1 );

            if ( result.getMonth() > monthsInYear() )
            {
                result.setMonth( 1 );
                result.setYear( result.getYear() + 1 );
            }

            newMonths--;
        }

        updateDateUnit( result );

        return result;
    }

    @Override
    public DateTimeUnit plusWeeks( DateTimeUnit dateTimeUnit, int weeks )
    {
        return plusDays( dateTimeUnit, weeks * daysInWeek() );
    }

    @Override
    public DateTimeUnit plusDays( DateTimeUnit dateTimeUnit, int days )
    {
        int curYear = dateTimeUnit.getYear();
        int curMonth = dateTimeUnit.getMonth();
        int curDay = dateTimeUnit.getDay();
        int dayOfWeek = dateTimeUnit.getDayOfWeek();
        int newDays = days;

        while ( newDays != 0 )
        {
            if ( curDay < getDaysFromMap( curYear, curMonth ) )
            {
                curDay++;
            }
            else
            {
                curMonth++;
                curDay = 1;
                if ( curMonth == 13 )
                {
                    curMonth = 1;
                    curYear++;
                }
            }
            newDays--;
            dayOfWeek++;
            if ( dayOfWeek == 8 )
            {
                dayOfWeek = 1;
            }
        }

        return new DateTimeUnit( curYear, curMonth, curDay, dayOfWeek, false );
    }

    @Override
    public DateTimeUnit isoStartOfYear( int year )
    {
        int day = 21;
        int[] twenties = new int[]{ 1375, 1379, 1383, 1387, 1391, 1395, 1399, 1403, 1407, 1408, 1411, 1412,
            1415, 1416, 1419 };

        if ( contains( twenties, year ) )
        {
            day = 20;
        }

        return new DateTimeUnit( year + 621, 3, day, java.util.Calendar.FRIDAY, true );
    }

    //---------------------------------------------------------------------------------------------
    // Helpers
    //---------------------------------------------------------------------------------------------

    private int getYearTotal( int year )
    {

        if ( CONVERSION_MAP.get( year ) == null )
        {
            throw new InvalidCalendarParametersException(
                "Illegal PERSIAN year, must be between " + START_PERSIAN.getYear() + " and " +
                    STOP_PERSIAN.getYear() + " was given " + year );
        }

        if ( CONVERSION_MAP.get( year )[0] == 0 )
        {
            for ( int j = 1; j <= 12; j++ )
            {
                CONVERSION_MAP.get( year )[0] += CONVERSION_MAP.get( year )[j];
            }
        }

        return CONVERSION_MAP.get( year )[0];
    }

    private int getDaysFromMap( int year, int month )
    {

        if ( CONVERSION_MAP.get( year ) == null )
        {
            throw new InvalidCalendarParametersException(
                "Illegal PERSIAN year, must be between " + START_PERSIAN.getYear() + " and " +
                    STOP_PERSIAN.getYear() + ", was given " + year );
        }

        return CONVERSION_MAP.get( year )[month];
    }

    private DateInterval toYearIsoInterval( DateTimeUnit dateTimeUnit, int offset, int length )
    {
        DateTimeUnit from = new DateTimeUnit( dateTimeUnit );

        if ( offset > 0 )
        {
            from = plusYears( from, offset );
        }
        else if ( offset < 0 )
        {
            from = minusYears( from, -offset );
        }

        DateTimeUnit to = new DateTimeUnit( from );
        to = plusYears( to, length );
        to = minusDays( to, length );

        from = toIso( from );
        to = toIso( to );

        return new DateInterval( from, to, DateIntervalType.ISO8601_YEAR );
    }

    private DateInterval toMonthIsoInterval( DateTimeUnit dateTimeUnit, int offset, int length )
    {
        DateTimeUnit from = new DateTimeUnit( dateTimeUnit );

        if ( offset > 0 )
        {
            from = plusMonths( from, offset );
        }
        else if ( offset < 0 )
        {
            from = minusMonths( from, -offset );
        }

        DateTimeUnit to = new DateTimeUnit( from );
        to = plusMonths( to, length );
        to = minusDays( to, 1 );

        from = toIso( from );
        to = toIso( to );

        return new DateInterval( from, to, DateIntervalType.ISO8601_MONTH );
    }

    private DateInterval toWeekIsoInterval( DateTimeUnit dateTimeUnit, int offset, int length )
    {
        DateTimeUnit from = new DateTimeUnit( dateTimeUnit );

        if ( offset > 0 )
        {
            from = plusWeeks( from, offset );
        }
        else if ( offset < 0 )
        {
            from = minusWeeks( from, -offset );
        }

        DateTimeUnit to = new DateTimeUnit( from );
        to = plusWeeks( to, length );
        to = minusDays( to, 1 );

        from = toIso( from );
        to = toIso( to );

        return new DateInterval( from, to, DateIntervalType.ISO8601_WEEK );
    }

    private DateInterval toDayIsoInterval( DateTimeUnit dateTimeUnit, int offset, int length )
    {
        DateTimeUnit from = new DateTimeUnit( dateTimeUnit );

        if ( offset >= 0 )
        {
            from = plusDays( from, offset );
        }
        else if ( offset < 0 )
        {
            from = minusDays( from, -offset );
        }

        DateTimeUnit to = new DateTimeUnit( from );
        to = plusDays( to, length );

        from = toIso( from );
        to = toIso( to );

        return new DateInterval( from, to, DateIntervalType.ISO8601_DAY );
    }

    private boolean contains( final int[] arr, final int key )
    {
        return Arrays.stream( arr ).anyMatch( i -> i == key );
    }

    // check if day is more than current maximum for month, don't overflow, just set to maximum
    // set day of week
    private void updateDateUnit( DateTimeUnit result )
    {
        int dm = getDaysFromMap( result.getYear(), result.getMonth() );

        if ( result.getDay() > dm )
        {
            result.setDay( dm );
        }

        result.setDayOfWeek( weekday( result ) );
    }


    //------------------------------------------------------------------------------------------------------------
    // Conversion map for Persian calendar
    //
    //------------------------------------------------------------------------------------------------------------

    /**
     * Map that gives an array of month lengths based on Persian year lookup.
     * Index 1 - 12 is used for months, index 0 is used to give year total (lazy calculated).
     */
    private static final Map<Integer, int[]> CONVERSION_MAP = new HashMap<>();

    static
    {
        CONVERSION_MAP.put( 1353, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1354, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1355, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1356, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1357, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1358, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1359, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1360, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1361, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1362, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1363, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1364, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1365, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1366, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1367, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1368, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1369, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1370, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1371, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1372, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1373, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1374, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1375, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1376, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1377, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1378, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1379, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1380, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1381, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1382, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1383, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1384, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1385, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1386, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1387, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1388, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1389, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1390, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1391, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1392, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1393, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1394, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1395, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1396, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1397, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1398, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1399, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1400, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1401, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1402, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1403, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1404, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1405, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1406, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1407, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1408, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1409, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1410, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1411, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1412, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1413, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1414, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1415, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1416, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30 } );
        CONVERSION_MAP.put( 1417, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1418, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
        CONVERSION_MAP.put( 1419, new int[]{ 0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29 } );
    }

}