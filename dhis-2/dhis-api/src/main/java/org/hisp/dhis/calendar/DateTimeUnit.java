package org.hisp.dhis.calendar;

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

import com.google.common.base.MoreObjects;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.IllegalInstantException;
import org.joda.time.LocalDateTime;
import org.joda.time.chrono.ISOChronology;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.TimeZone;

/**
 * Class representing a specific calendar date.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @see DateInterval
 * @see Calendar
 */
public class DateTimeUnit
{
    /**
     * Year of date. Required.
     */
    @NotNull
    private int year;

    /**
     * Month of date. Required.
     */
    @NotNull
    private int month;

    /**
     * Day of date. Required.
     */
    @NotNull
    private int day;

    /**
     * Day of week, numbering is unspecified and left up to user.
     */
    private int dayOfWeek;

    /**
     * Does dateUnit represent ISO 8601.
     */
    final boolean iso8601;

    /**
     * Hour of day, range is 1 - 24.
     */
    private int hour;

    /**
     * Minute of hour, range is 0 - 59.
     */
    private int minute;

    /**
     * Second  of minute, range is 0 - 59.
     */
    private int second;

    /**
     * Millisecond of second, range is 0 - 999.
     */
    private int millis;

    /**
     * TimeZone for this dateTime instance, defaults to the local tz, used when converting to/from joda/jdk calenders.
     */
    private TimeZone timeZone = TimeZone.getDefault();

    public DateTimeUnit( boolean iso8601 )
    {
        this.iso8601 = iso8601;
        this.hour = 0;
        this.minute = 0;
        this.second = 0;
        this.millis = 0;
    }

    public DateTimeUnit()
    {
        this( false );
    }

    public DateTimeUnit( DateTimeUnit dateTimeUnit )
    {
        this( dateTimeUnit.isIso8601() );
        this.year = dateTimeUnit.getYear();
        this.month = dateTimeUnit.getMonth();
        this.day = dateTimeUnit.getDay();
        this.dayOfWeek = dateTimeUnit.getDayOfWeek();
    }

    public DateTimeUnit( DateTimeUnit dateTimeUnit, boolean iso8601 )
    {
        this( iso8601 );
        this.year = dateTimeUnit.getYear();
        this.month = dateTimeUnit.getMonth();
        this.day = dateTimeUnit.getDay();
        this.dayOfWeek = dateTimeUnit.getDayOfWeek();
    }

    public DateTimeUnit( int year, int month, int day, boolean iso8601 )
    {
        this( iso8601 );
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public DateTimeUnit( int year, int month, int day )
    {
        this( year, month, day, false );
    }

    public DateTimeUnit( int year, int month, int day, int dayOfWeek, boolean iso8601 )
    {
        this( year, month, day, iso8601 );
        this.dayOfWeek = dayOfWeek;
    }

    public DateTimeUnit( int year, int month, int day, int dayOfWeek )
    {
        this( year, month, day, dayOfWeek, false );
    }

    public void setDate( int year, int month, int day )
    {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public void setTime( int hour, int minute, int second, int millis )
    {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.millis = millis;
    }

    public int getYear()
    {
        return year;
    }

    public void setYear( int year )
    {
        this.year = year;
    }

    public int getMonth()
    {
        return month;
    }

    public void setMonth( int month )
    {
        this.month = month;
    }

    public int getDay()
    {
        return day;
    }

    public void setDay( int day )
    {
        this.day = day;
    }

    public int getDayOfWeek()
    {
        return dayOfWeek;
    }

    public void setDayOfWeek( int dayOfWeek )
    {
        this.dayOfWeek = dayOfWeek;
    }

    public boolean isIso8601()
    {
        return iso8601;
    }

    public int getHour()
    {
        return hour;
    }

    public void setHour( int hour )
    {
        this.hour = hour;
    }

    public int getMinute()
    {
        return minute;
    }

    public void setMinute( int minute )
    {
        this.minute = minute;
    }

    public int getSecond()
    {
        return second;
    }

    public void setSecond( int second )
    {
        this.second = second;
    }

    public int getMillis()
    {
        return millis;
    }

    public void setMillis( int millis )
    {
        this.millis = millis;
    }

    public TimeZone getTimeZone()
    {
        return timeZone;
    }

    public void setTimeZone( TimeZone timeZone )
    {
        this.timeZone = timeZone;
    }

    /**
     * Return current instance with timeZone set to UTC
     *
     * @return DateTimeUnit instance with timeZone set to UTC
     */
    public DateTimeUnit toUtc()
    {
        return DateTimeUnit.fromJodaDateTime( toJodaDateTime().toDateTime( DateTimeZone.UTC ), true );
    }

    /**
     * Converts dateUnit to Joda-Time DateTime using ISO chronology
     *
     * @return Populated DateTime object
     */
    public DateTime toJodaDateTime()
    {
        return toJodaDateTime( ISOChronology.getInstance() );
    }

    /**
     * Converts dateUnit to Joda-Time DateTime with a specific chronology.
     *
     * @param chronology Chronology to use
     * @return Populated DateTime object
     */
    public DateTime toJodaDateTime( Chronology chronology )
    {
        try
        {
            return new DateTime( year, month, day, hour, minute, second, millis, chronology.withZone( DateTimeZone.forTimeZone( timeZone ) ) );
        }
        catch ( IllegalInstantException ex )
        {
            LocalDateTime localDateTime = new LocalDateTime( year, month, day, hour, minute, second, millis,
                chronology.withZone( DateTimeZone.forTimeZone( timeZone ) ) );

            return localDateTime.toLocalDate().toDateTimeAtStartOfDay();
        }
    }

    /**
     * Converts dateUnit to JDK Calendar
     *
     * @return Populated JDK Calendar object
     */
    public java.util.Calendar toJdkCalendar()
    {
        return toJodaDateTime().toGregorianCalendar();
    }

    /**
     * Converts dateUnit to JDK Date
     *
     * @return Populated JDK Date object
     */
    public Date toJdkDate()
    {
        return toJodaDateTime().toDate();
    }

    /**
     * Converts from Joda-Time DateTime to DateUnit
     *
     * @param dateTime DateTime object
     * @return Populated DateUnit object
     */
    public static DateTimeUnit fromJodaDateTime( DateTime dateTime )
    {
        return new DateTimeUnit( dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), dateTime.getDayOfWeek() );
    }

    /**
     * Converts from Joda-Time DateTime to DateUnit
     *
     * @param dateTime DateTime object
     * @param iso8601  whether date time is iso8601
     * @return Populated DateUnit object
     */
    public static DateTimeUnit fromJodaDateTime( DateTime dateTime, boolean iso8601 )
    {
        DateTimeUnit dateTimeUnit = new DateTimeUnit( iso8601 );
        dateTimeUnit.setDate( dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth() );
        dateTimeUnit.setDayOfWeek( dateTime.getDayOfWeek() );
        dateTimeUnit.setTime( dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), dateTime.getSecondOfMinute(), dateTime.getMillisOfSecond() );
        dateTimeUnit.setTimeZone( dateTime.getZone().toTimeZone() );

        return dateTimeUnit;
    }

    /**
     * Converts from JDK Calendar to DateUnit
     *
     * @param calendar JDK Calendar object
     * @return Populated DateUnit object
     */
    public static DateTimeUnit fromJdkCalendar( java.util.Calendar calendar )
    {
        return fromJodaDateTime( new DateTime( calendar ) );
    }

    /**
     * Converts from JDK Date to DateUnit
     *
     * @param date JDK Date object
     * @return Populated DateUnit object
     */
    public static DateTimeUnit fromJdkDate( Date date )
    {
        return fromJodaDateTime( new DateTime( date.getTime() ), true );
    }

    // Note that we do not include dayOfWeek in equals/hashCode, this might not always be set
    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        DateTimeUnit that = (DateTimeUnit) o;

        if ( day != that.day ) return false;
        if ( hour != that.hour ) return false;
        if ( iso8601 != that.iso8601 ) return false;
        if ( millis != that.millis ) return false;
        if ( minute != that.minute ) return false;
        if ( month != that.month ) return false;
        if ( second != that.second ) return false;
        if ( year != that.year ) return false;
        // if ( !timeZone.equals( that.timeZone ) ) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = year;
        result = 31 * result + month;
        result = 31 * result + day;
        result = 31 * result + (iso8601 ? 1 : 0);
        result = 31 * result + hour;
        result = 31 * result + minute;
        result = 31 * result + second;
        result = 31 * result + millis;
        // result = 31 * result + timeZone.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "year", year )
            .add( "month", month )
            .add( "day", day )
            .add( "dayOfWeek", dayOfWeek )
            .add( "iso8601", iso8601 )
            .add( "hour", hour )
            .add( "minute", minute )
            .add( "second", second )
            .add( "millis", millis )
            .add( "timeZone", timeZone )
            .toString();
    }
}
