package org.hisp.dhis.calendar.impl;

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

import org.hisp.dhis.calendar.AbstractCalendar;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateInterval;
import org.hisp.dhis.calendar.DateIntervalType;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.chrono.ISOChronology;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class NepaliCalendar extends AbstractCalendar
{
    private static final DateTimeUnit START_NEPAL = new DateTimeUnit( 2000, 1, 1, java.util.Calendar.WEDNESDAY );

    private static final DateTimeUnit START_ISO = new DateTimeUnit( 1943, 4, 14, java.util.Calendar.WEDNESDAY, true );

    private static final Calendar SELF = new NepaliCalendar();

    public static Calendar getInstance()
    {
        return SELF;
    }

    @Override
    public String name()
    {
        return "nepali";
    }

    @Override
    public DateTimeUnit toIso( DateTimeUnit dateTimeUnit )
    {
        if ( dateTimeUnit.isIso8601() )
        {
            return dateTimeUnit;
        }

        DateTime dateTime = START_ISO.toJodaDateTime();

        int totalDays = 0;
        
        for ( int year = START_NEPAL.getYear(); year < dateTimeUnit.getYear(); year++ )
        {
            totalDays += getYearTotal( year );
        }

        for ( int month = START_NEPAL.getMonth(); month < dateTimeUnit.getMonth(); month++ )
        {
            totalDays += CONVERSION_MAP.get( dateTimeUnit.getYear() )[month];
        }

        totalDays += dateTimeUnit.getDay() - START_NEPAL.getDay();

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
        DateTime start = START_ISO.toJodaDateTime();
        DateTime end = dateTimeUnit.toJodaDateTime();

        return plusDays( START_NEPAL, Days.daysBetween( start, end ).getDays() );
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
        }

        return null;
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

        if ( offset > 0 )
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

    @Override
    public int daysInYear( int year )
    {
        return getYearTotal( year );
    }

    @Override
    public int daysInMonth( int year, int month )
    {
        return CONVERSION_MAP.get( year )[month];
    }

    @Override
    public int weeksInYear( int year )
    {
        DateTime dateTime = new DateTime( year, 1, 1, 0, 0, ISOChronology.getInstance( DateTimeZone.getDefault() ) );
        return dateTime.weekOfWeekyear().getMaximumValue();
    }

    @Override
    public int isoWeek( DateTimeUnit dateTimeUnit )
    {
        DateTime dateTime = toIso( dateTimeUnit ).toJodaDateTime( ISOChronology.getInstance( DateTimeZone.getDefault() ) );
        return dateTime.getWeekyear();
    }

    @Override
    public int week( DateTimeUnit dateTimeUnit )
    {
        return isoWeek( dateTimeUnit );
    }

    @Override
    public int isoWeekday( DateTimeUnit dateTimeUnit )
    {
        DateTime dateTime = toIso( dateTimeUnit ).toJodaDateTime( ISOChronology.getInstance( DateTimeZone.getDefault() ) );
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

        return "nepali." + DEFAULT_I18N_MONTH_NAMES[month - 1];
    }

    @Override
    public String shortNameOfMonth( int month )
    {
        if ( month > DEFAULT_I18N_MONTH_SHORT_NAMES.length || month <= 0 )
        {
            return null;
        }

        return "nepali." + DEFAULT_I18N_MONTH_SHORT_NAMES[month - 1];
    }

    @Override
    public String nameOfDay( int day )
    {
        if ( day > DEFAULT_I18N_DAY_NAMES.length || day <= 0 )
        {
            return null;
        }

        return "nepali." + DEFAULT_I18N_DAY_NAMES[day - 1];
    }

    @Override
    public String shortNameOfDay( int day )
    {
        if ( day > DEFAULT_I18N_DAY_SHORT_NAMES.length || day <= 0 )
        {
            return null;
        }

        return "nepali." + DEFAULT_I18N_DAY_SHORT_NAMES[day - 1];
    }

    private int getYearTotal( int year )
    {
        // if year total index is uninitialized, calculate and set in array
        if ( CONVERSION_MAP.get( year )[0] == 0 )
        {
            for ( int j = 1; j <= 12; j++ )
            {
                CONVERSION_MAP.get( year )[0] += CONVERSION_MAP.get( year )[j];
            }
        }

        return CONVERSION_MAP.get( year )[0];
    }

    @Override
    public DateTimeUnit minusYears( DateTimeUnit dateTimeUnit, int years )
    {
        DateTimeUnit result = new DateTimeUnit( dateTimeUnit.getYear() - years, dateTimeUnit.getMonth(), dateTimeUnit.getDay(), dateTimeUnit.getDayOfWeek() );
        updateDateUnit( result );

        return result;
    }

    @Override
    public DateTimeUnit minusMonths( DateTimeUnit dateTimeUnit, int months )
    {
        DateTimeUnit result = new DateTimeUnit( dateTimeUnit );

        while ( months != 0 )
        {
            result.setMonth( result.getMonth() - 1 );

            if ( result.getMonth() < 1 )
            {
                result.setMonth( monthsInYear() );
                result.setYear( result.getYear() - 1 );
            }

            months--;
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

        while ( days != 0 )
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

                curDay = CONVERSION_MAP.get( curYear )[curMonth];
            }

            dayOfWeek--;

            if ( dayOfWeek == 0 )
            {
                dayOfWeek = 7;
            }

            days--;
        }

        return new DateTimeUnit( curYear, curMonth, curDay, dayOfWeek );
    }

    @Override
    public DateTimeUnit plusYears( DateTimeUnit dateTimeUnit, int years )
    {
        DateTimeUnit result = new DateTimeUnit( dateTimeUnit.getYear() + years, dateTimeUnit.getMonth(), dateTimeUnit.getDay(), dateTimeUnit.getDayOfWeek() );
        updateDateUnit( result );

        return result;
    }

    @Override
    public DateTimeUnit plusMonths( DateTimeUnit dateTimeUnit, int months )
    {
        DateTimeUnit result = new DateTimeUnit( dateTimeUnit );

        while ( months != 0 )
        {
            result.setMonth( result.getMonth() + 1 );

            if ( result.getMonth() > monthsInYear() )
            {
                result.setMonth( 1 );
                result.setYear( result.getYear() + 1 );
            }

            months--;
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

        while ( days != 0 )
        {
            // days in month
            int dm = CONVERSION_MAP.get( curYear )[curMonth];

            curDay++;

            if ( curDay > dm )
            {
                curMonth++;
                curDay = 1;
            }

            if ( curMonth > 12 )
            {
                curYear++;
                curMonth = 1;
            }

            dayOfWeek++;

            if ( dayOfWeek > 7 )
            {
                dayOfWeek = 1;
            }

            days--;
        }

        return new DateTimeUnit( curYear, curMonth, curDay, dayOfWeek );
    }

    @Override
    public DateTimeUnit isoStartOfYear( int year )
    {
        return START_ISO;
    }

    // check if day is more than current maximum for month, don't overflow, just set to maximum
    // set day of week
    private void updateDateUnit( DateTimeUnit result )
    {
        int dm = CONVERSION_MAP.get( result.getYear() )[result.getMonth()];

        if ( result.getDay() > dm )
        {
            result.setDay( dm );
        }

        result.setDayOfWeek( weekday( result ) );
    }

    //------------------------------------------------------------------------------------------------------------
    // Conversion map for Nepali calendar
    //
    // Based on map from:
    // http://forjavaprogrammers.blogspot.com/2012/06/how-to-convert-english-date-to-nepali.html
    //------------------------------------------------------------------------------------------------------------

    /**
     * Map that gives an array of month lengths based on Nepali year lookup.
     * Index 1 - 12 is used for months, index 0 is used to give year total (lazy calculated).
     */
    private static final Map<Integer, int[]> CONVERSION_MAP = new HashMap<>();

    static
    {
        CONVERSION_MAP.put( 2000, new int[]{ 0, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2001, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2002, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2003, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2004, new int[]{ 0, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2005, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2006, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2007, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2008, new int[]{ 0, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31 } );
        CONVERSION_MAP.put( 2009, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2010, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2011, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2012, new int[]{ 0, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2013, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2014, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2015, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2016, new int[]{ 0, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2017, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2018, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2019, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2020, new int[]{ 0, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2021, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2022, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30 } );
        CONVERSION_MAP.put( 2023, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2024, new int[]{ 0, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2025, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2026, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2027, new int[]{ 0, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2028, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2029, new int[]{ 0, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2030, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2031, new int[]{ 0, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2032, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2033, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2034, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2035, new int[]{ 0, 30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31 } );
        CONVERSION_MAP.put( 2036, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2037, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2038, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2039, new int[]{ 0, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2040, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2041, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2042, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2043, new int[]{ 0, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2044, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2045, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2046, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2047, new int[]{ 0, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2048, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2049, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30 } );
        CONVERSION_MAP.put( 2050, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2051, new int[]{ 0, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2052, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2053, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30 } );
        CONVERSION_MAP.put( 2054, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2055, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2056, new int[]{ 0, 31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2057, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2058, new int[]{ 0, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2059, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2060, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2061, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2062, new int[]{ 0, 30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2063, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2064, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2065, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2066, new int[]{ 0, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31 } );
        CONVERSION_MAP.put( 2067, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2068, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2069, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2070, new int[]{ 0, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2071, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2072, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2073, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2074, new int[]{ 0, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2075, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2076, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30 } );
        CONVERSION_MAP.put( 2077, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2078, new int[]{ 0, 31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2079, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2080, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30 } );
        CONVERSION_MAP.put( 2081, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2082, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2083, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2084, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2085, new int[]{ 0, 30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2086, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2087, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2088, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2089, new int[]{ 0, 30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31 } );
        CONVERSION_MAP.put( 2090, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2091, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2092, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
        CONVERSION_MAP.put( 2093, new int[]{ 0, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31 } );
        CONVERSION_MAP.put( 2094, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2095, new int[]{ 0, 31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 29 } );
        CONVERSION_MAP.put( 2096, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 32 } );
        CONVERSION_MAP.put( 2097, new int[]{ 0, 31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2098, new int[]{ 0, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2099, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30 } );
        CONVERSION_MAP.put( 2100, new int[]{ 0, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31 } );
    }
}
