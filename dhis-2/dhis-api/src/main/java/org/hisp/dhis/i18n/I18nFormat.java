package org.hisp.dhis.i18n;

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

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;

/**
 * @author Pham Thi Thuy
 * @author Nguyen Dang Quang
 * @version $Id: I18nFormat.java 2974 2007-03-03 22:11:13Z torgeilo $
 */
public class I18nFormat
{
    private static final DecimalFormat FORMAT_VALUE = new DecimalFormat( "#.#" ); // Fixed for now
    private static final String EMPTY = "";
    private static final String NAN = "NaN";
    
    private static final String INVALID_DATE = "Invalid date format";

    public static final String FORMAT_DATE = "yyyy-MM-dd";
    public static final String FORMAT_TIME = "HH:mm";
    public static final String FORMAT_DATETIME = "yyyy-MM-dd HH:mm";

    private ResourceBundle resourceBundle;

    public I18nFormat( ResourceBundle resourceBundle )
    {
        this.resourceBundle = resourceBundle;
    }

    /**
     * Constructor should only be used for testing purposes. Use
     * I18nManager.getI18nFormat for normal use.
     */
    public I18nFormat()
    {
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    private DateFormatSymbols dateFormatSymbols;

    public void init()
    {
        String[] months = { "month.january", "month.february", "month.march", "month.april", "month.may", "month.june",
            "month.july", "month.august", "month.september", "month.october", "month.november", "month.december" };
        String[] shortMonths = { "month.short.january", "month.short.february", "month.short.march",
            "month.short.april", "month.short.may", "month.short.june", "month.short.july", "month.short.august",
            "month.short.september", "month.short.october", "month.short.november", "month.short.december" };
        String[] weekdays = { "weekday.sunday", "weekday.monday", "weekday.tuesday", "weekday.wednesday",
            "weekday.thursday", "weekday.friday", "weekday.saturday" };
        String[] shortWeekdays = { "weekday.short.sunday", "weekday.short.monday", "weekday.short.tuesday",
            "weekday.short.wednesday", "weekday.short.thursday", "weekday.short.friday", "weekday.short.saturday" };

        String calendarName = PeriodType.getCalendar().name() + ".";

        for ( int i = 0; i < 12; ++i )
        {
            if ( resourceBundle.containsKey( calendarName + months[i] ) )
            {
                months[i] = resourceBundle.getString( calendarName + months[i] );
            }
            else
            {
                months[i] = resourceBundle.getString( months[i] );
            }

            if ( resourceBundle.containsKey( calendarName + shortMonths[i] ) )
            {
                shortMonths[i] = resourceBundle.getString( calendarName + shortMonths[i] );
            }
            else
            {
                shortMonths[i] = resourceBundle.getString( shortMonths[i] );
            }
        }

        for ( int i = 0; i < 7; ++i )
        {
            if ( resourceBundle.containsKey( calendarName + weekdays[i] ) )
            {
                weekdays[i] = resourceBundle.getString( calendarName + weekdays[i] );
            }
            else
            {
                weekdays[i] = resourceBundle.getString( weekdays[i] );
            }

            if ( resourceBundle.containsKey( calendarName + shortWeekdays[i] ) )
            {
                shortWeekdays[i] = resourceBundle.getString( calendarName + shortWeekdays[i] );
            }
            else
            {
                shortWeekdays[i] = resourceBundle.getString( shortWeekdays[i] );
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormatSymbols = dateFormat.getDateFormatSymbols();

        dateFormatSymbols.setMonths( months );
        dateFormatSymbols.setShortMonths( shortMonths );
        dateFormatSymbols.setWeekdays( weekdays );
        dateFormatSymbols.setShortWeekdays( shortWeekdays );
    }

    // -------------------------------------------------------------------------
    // Format methods
    // -------------------------------------------------------------------------

    public Date parseDate( String date )
    {
        if ( date == null )
        {
            return null;
        }

        return commonParsing( date, FORMAT_DATE );
    }

    public Date parseTime( String time )
    {
        if ( time == null )
        {
            return null;
        }

        return commonParsing( time, FORMAT_TIME );
    }

    public Date parseDateTime( String dateTime )
    {
        if ( dateTime == null )
        {
            return null;
        }

        return commonParsing( dateTime, FORMAT_DATETIME );
    }

    public String formatDate( Date date )
    {
        if ( date == null )
        {
            return null;
        }

        return commonFormatting( date, FORMAT_DATE );
    }

    public String formatTime( Date date )
    {
        if ( date == null )
        {
            return null;
        }

        return commonFormatting( date, FORMAT_TIME );
    }

    public String formatDateTime( Date date )
    {
        if ( date == null )
        {
            return null;
        }

        return commonFormatting( date, FORMAT_DATETIME );
    }

    /**
     * Formats a period. Returns null if value is null. Returns INVALID_DATE if formatting string is invalid.
     *
     * @param period the value to format.
     */
    public String formatPeriod( Period period )
    {
        if ( period == null )
        {
            return null;
        }

        String typeName = period.getPeriodType().getName();

        if ( typeName.equals( WeeklyPeriodType.NAME ) ) // Use ISO dates due to potential week confusion
        {
            return period.getIsoDate();
        }

        String keyStartDate = "format." + typeName + ".startDate";
        String keyEndDate = "format." + typeName + ".endDate";

        String startPattern = resourceBundle.getString( keyStartDate );
        String endPattern = resourceBundle.getString( keyEndDate );

        boolean dayPattern = startPattern.contains( "dd" ) || endPattern.contains( "dd" );

        Date periodStartDate = period.getStartDate();
        Date periodEndDate = period.getEndDate();

        DateTimeUnit start = PeriodType.getCalendar().fromIso( periodStartDate );
        DateTimeUnit end = PeriodType.getCalendar().fromIso( periodEndDate );

        String startDate;
        String endDate;

        if ( !dayPattern )
        {
            // Set day to first of month to not overflow when converting to JDK date
            start.setDay( 1 );
            end.setDay( 1 );

            startDate = commonFormatting( new DateTimeUnit( start, true ).toJdkDate(), startPattern );
            endDate = commonFormatting( new DateTimeUnit( end, true ).toJdkDate(), endPattern );
        }
        else
        {
            startDate = PeriodType.getCalendar().formattedDate( startPattern, start );
            endDate = PeriodType.getCalendar().formattedDate( endPattern, end );
        }

        try
        {
            return Character.toUpperCase( startDate.charAt( 0 ) ) + startDate.substring( 1 ) + endDate;
        }
        catch ( IllegalArgumentException ex )
        {
            return INVALID_DATE;
        }
    }
    /**
     * Formats value. Returns empty string if value is null. Returns NaN if value
     * is not a number. Return a formatted string if value is an instance of Number,
     * if not returns the value as a string.
     *
     * @param value the value to format.
     */
    public String formatValue( Object value )
    {
        if ( value == null )
        {
            return EMPTY;
        }
        
        if ( value instanceof Number )
        {
            try
            {
                return FORMAT_VALUE.format( value );
            }
            catch ( IllegalArgumentException ex )
            {
                return NAN;
            }
        }
        else
        {
            return String.valueOf( value );
        }
    }
    // -------------------------------------------------------------------------
    // Support methods
    // -------------------------------------------------------------------------

    private Date commonParsing( String input, String pattern )
    {
        DateFormat dateFormat = new SimpleDateFormat( pattern, dateFormatSymbols );

        Date parsedDate;

        try
        {
            parsedDate = dateFormat.parse( input );
        }
        catch ( ParseException e )
        {
            return null;
        }

        if ( !commonFormatting( parsedDate, pattern ).equals( input ) )
        {
            return null;
        }

        return parsedDate;
    }

    private String commonFormatting( Date date, String pattern )
    {
        DateFormat dateFormat = new SimpleDateFormat( pattern, dateFormatSymbols );

        return dateFormat.format( date );
    }
}
