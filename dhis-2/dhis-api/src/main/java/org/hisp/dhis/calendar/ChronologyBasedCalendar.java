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

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.chrono.ISOChronology;

import java.util.Date;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class ChronologyBasedCalendar extends AbstractCalendar
{
    protected final Chronology chronology;

    protected ChronologyBasedCalendar( Chronology chronology )
    {
        this.chronology = chronology;
    }

    @Override
    public DateTimeUnit toIso( DateTimeUnit dateTimeUnit )
    {
        if ( dateTimeUnit.isIso8601() )
        {
            return dateTimeUnit;
        }

        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
        dateTime = dateTime.withChronology( ISOChronology.getInstance( DateTimeZone.forTimeZone( dateTimeUnit.getTimeZone() ) ) );

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
        if ( !dateTimeUnit.isIso8601() )
        {
            return dateTimeUnit;
        }

        DateTime dateTime = dateTimeUnit.toJodaDateTime( ISOChronology.getInstance( DateTimeZone.forTimeZone( dateTimeUnit.getTimeZone() ) ) );
        dateTime = dateTime.withChronology( chronology );

        return DateTimeUnit.fromJodaDateTime( dateTime );
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
        DateTime from = dateTimeUnit.toJodaDateTime( chronology );

        if ( offset > 0 )
        {
            from = from.plusYears( offset );
        }
        else if ( offset < 0 )
        {
            from = from.minusYears( -offset );
        }

        DateTime to = new DateTime( from ).plusYears( length ).minusDays( 1 );

        DateTimeUnit fromDateTimeUnit = DateTimeUnit.fromJodaDateTime( from );
        DateTimeUnit toDateTimeUnit = DateTimeUnit.fromJodaDateTime( to );

        fromDateTimeUnit.setDayOfWeek( isoWeekday( fromDateTimeUnit ) );
        toDateTimeUnit.setDayOfWeek( isoWeekday( toDateTimeUnit ) );

        return new DateInterval( toIso( fromDateTimeUnit ), toIso( toDateTimeUnit ), DateIntervalType.ISO8601_YEAR );
    }

    private DateInterval toMonthIsoInterval( DateTimeUnit dateTimeUnit, int offset, int length )
    {
        DateTime from = dateTimeUnit.toJodaDateTime( chronology );

        if ( offset > 0 )
        {
            from = from.plusMonths( offset );
        }
        else if ( offset < 0 )
        {
            from = from.minusMonths( -offset );
        }

        DateTime to = new DateTime( from ).plusMonths( length ).minusDays( 1 );

        DateTimeUnit fromDateTimeUnit = DateTimeUnit.fromJodaDateTime( from );
        DateTimeUnit toDateTimeUnit = DateTimeUnit.fromJodaDateTime( to );

        fromDateTimeUnit.setDayOfWeek( isoWeekday( fromDateTimeUnit ) );
        toDateTimeUnit.setDayOfWeek( isoWeekday( toDateTimeUnit ) );

        return new DateInterval( toIso( fromDateTimeUnit ), toIso( toDateTimeUnit ), DateIntervalType.ISO8601_MONTH );
    }

    private DateInterval toWeekIsoInterval( DateTimeUnit dateTimeUnit, int offset, int length )
    {
        DateTime from = dateTimeUnit.toJodaDateTime( chronology );

        if ( offset > 0 )
        {
            from = from.plusWeeks( offset );
        }
        else if ( offset < 0 )
        {
            from = from.minusWeeks( -offset );
        }

        DateTime to = new DateTime( from ).plusWeeks( length ).minusDays( 1 );

        DateTimeUnit fromDateTimeUnit = DateTimeUnit.fromJodaDateTime( from );
        DateTimeUnit toDateTimeUnit = DateTimeUnit.fromJodaDateTime( to );

        fromDateTimeUnit.setDayOfWeek( isoWeekday( fromDateTimeUnit ) );
        toDateTimeUnit.setDayOfWeek( isoWeekday( toDateTimeUnit ) );

        return new DateInterval( toIso( fromDateTimeUnit ), toIso( toDateTimeUnit ), DateIntervalType.ISO8601_WEEK );
    }

    private DateInterval toDayIsoInterval( DateTimeUnit dateTimeUnit, int offset, int length )
    {
        DateTime from = dateTimeUnit.toJodaDateTime( chronology );

        if ( offset > 0 )
        {
            from = from.plusDays( offset );
        }
        else if ( offset < 0 )
        {
            from = from.minusDays( -offset );
        }

        DateTime to = new DateTime( from ).plusDays( length );

        DateTimeUnit fromDateTimeUnit = DateTimeUnit.fromJodaDateTime( from );
        DateTimeUnit toDateTimeUnit = DateTimeUnit.fromJodaDateTime( to );

        fromDateTimeUnit.setDayOfWeek( isoWeekday( fromDateTimeUnit ) );
        toDateTimeUnit.setDayOfWeek( isoWeekday( toDateTimeUnit ) );

        return new DateInterval( toIso( fromDateTimeUnit ), toIso( toDateTimeUnit ), DateIntervalType.ISO8601_DAY );
    }

    @Override
    public int daysInWeek()
    {
        LocalDate localDate = new LocalDate( 1, 1, 1, chronology );
        return localDate.toDateTimeAtStartOfDay().dayOfWeek().getMaximumValue();
    }

    @Override
    public int daysInYear( int year )
    {
        LocalDate localDate = new LocalDate( year, 1, 1, chronology );
        return (int) localDate.toDateTimeAtStartOfDay().year().toInterval().toDuration().getStandardDays();
    }

    @Override
    public int daysInMonth( int year, int month )
    {
        LocalDate localDate = new LocalDate( year, month, 1, chronology );
        return localDate.toDateTimeAtStartOfDay().dayOfMonth().getMaximumValue();
    }

    @Override
    public int weeksInYear( int year )
    {
        LocalDate localDate = new LocalDate( year, 1, 1, chronology );
        return localDate.toDateTimeAtStartOfDay().weekOfWeekyear().getMaximumValue();
    }

    @Override
    public int isoWeek( DateTimeUnit dateTimeUnit )
    {
        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
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
        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
        dateTime = dateTime.withChronology( ISOChronology.getInstance( DateTimeZone.getDefault() ) );
        return dateTime.getDayOfWeek();
    }

    @Override
    public int weekday( DateTimeUnit dateTimeUnit )
    {
        return dateTimeUnit.toJodaDateTime( chronology ).getDayOfWeek();
    }

    @Override
    public DateTimeUnit plusDays( DateTimeUnit dateTimeUnit, int days )
    {
        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
        return DateTimeUnit.fromJodaDateTime( dateTime.plusDays( days ), dateTimeUnit.isIso8601() );
    }

    @Override
    public DateTimeUnit minusDays( DateTimeUnit dateTimeUnit, int days )
    {
        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
        return DateTimeUnit.fromJodaDateTime( dateTime.minusDays( days ), dateTimeUnit.isIso8601() );
    }

    @Override
    public DateTimeUnit plusWeeks( DateTimeUnit dateTimeUnit, int weeks )
    {
        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
        return DateTimeUnit.fromJodaDateTime( dateTime.plusWeeks( weeks ), dateTimeUnit.isIso8601() );
    }

    @Override
    public DateTimeUnit minusWeeks( DateTimeUnit dateTimeUnit, int weeks )
    {
        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
        return DateTimeUnit.fromJodaDateTime( dateTime.minusWeeks( weeks ), dateTimeUnit.isIso8601() );
    }

    @Override
    public DateTimeUnit plusMonths( DateTimeUnit dateTimeUnit, int months )
    {
        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
        return DateTimeUnit.fromJodaDateTime( dateTime.plusMonths( months ), dateTimeUnit.isIso8601() );
    }

    @Override
    public DateTimeUnit minusMonths( DateTimeUnit dateTimeUnit, int months )
    {
        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
        return DateTimeUnit.fromJodaDateTime( dateTime.minusMonths( months ), dateTimeUnit.isIso8601() );
    }

    @Override
    public DateTimeUnit plusYears( DateTimeUnit dateTimeUnit, int years )
    {
        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
        return DateTimeUnit.fromJodaDateTime( dateTime.plusYears( years ), dateTimeUnit.isIso8601() );
    }

    @Override
    public DateTimeUnit minusYears( DateTimeUnit dateTimeUnit, int years )
    {
        DateTime dateTime = dateTimeUnit.toJodaDateTime( chronology );
        return DateTimeUnit.fromJodaDateTime( dateTime.minusYears( years ), dateTimeUnit.isIso8601() );
    }

    @Override
    public DateTimeUnit isoStartOfYear( int year )
    {
        DateTime dateTime = new DateTime( year, 1, 1, 11, 0, chronology ).withChronology( ISOChronology.getInstance() );
        return DateTimeUnit.fromJodaDateTime( dateTime );
    }
}
