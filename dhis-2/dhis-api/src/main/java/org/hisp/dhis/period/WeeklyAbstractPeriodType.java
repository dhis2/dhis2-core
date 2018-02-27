package org.hisp.dhis.period;

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
 *
 */

import com.google.common.collect.Lists;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateInterval;
import org.hisp.dhis.calendar.DateIntervalType;
import org.hisp.dhis.calendar.DateTimeUnit;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class WeeklyAbstractPeriodType extends CalendarPeriodType
{
    protected final String name;

    protected final int startOfWeek;

    protected final String isoFormat;

    protected final String isoDuration;

    protected final int frequencyOrder;

    protected final String weekPrefix;

    protected WeeklyAbstractPeriodType( String name, int startOfWeek, String isoFormat, String isoDuration,
        int frequencyOrder, String weekPrefix )
    {
        this.name = name;
        this.startOfWeek = startOfWeek;
        this.isoFormat = isoFormat;
        this.isoDuration = isoDuration;
        this.frequencyOrder = frequencyOrder;
        this.weekPrefix = weekPrefix;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public int getStartOfWeek()
    {
        return startOfWeek;
    }

    @Override
    public String getIso8601Duration()
    {
        return isoDuration;
    }

    @Override
    public int getFrequencyOrder()
    {
        return frequencyOrder;
    }

    @Override
    public String getIsoFormat()
    {
        return isoFormat;
    }

    @Override
    public Period createPeriod( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        DateTimeUnit start = adjustToStartOfWeek( new DateTimeUnit( dateTimeUnit ), calendar );
        DateTimeUnit end = new DateTimeUnit( start );
        end = calendar.plusDays( end, calendar.daysInWeek() - 1 );

        return toIsoPeriod( start, end, calendar );
    }

    @Override
    public Period getNextPeriod( Period period, Calendar calendar )
    {
        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( period.getStartDate(), calendar );
        dateTimeUnit = calendar.plusWeeks( dateTimeUnit, 1 );

        return createPeriod( dateTimeUnit, calendar );
    }

    @Override
    public Period getPreviousPeriod( Period period, Calendar calendar )
    {
        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( period.getStartDate(), calendar );
        dateTimeUnit = calendar.minusWeeks( dateTimeUnit, 1 );

        return createPeriod( dateTimeUnit, calendar );
    }

    /**
     * Generates weekly Periods for the whole year in which the given Period's
     * startDate exists.
     */
    @Override
    public List<Period> generatePeriods( DateTimeUnit start )
    {
        Calendar calendar = getCalendar();
        List<Period> periods = new ArrayList<>();
        start = new DateTimeUnit( start ); // create clone so we don't modify the original start DT

        start.setMonth( 1 );
        start.setDay( 4 );
        start = adjustToStartOfWeek( start, calendar );

        for ( int i = 0; i < calendar.weeksInYear( start.getYear() ); i++ )
        {
            DateInterval interval = calendar.toInterval( start, DateIntervalType.ISO8601_WEEK );
            periods.add( new Period( this, interval.getFrom().toJdkDate(), interval.getTo().toJdkDate() ) );

            start = calendar.plusWeeks( start, 1 );
        }

        return periods;
    }

    /**
     * Generates the last 52 weeks where the last one is the week which the
     * given date is inside.
     */
    @Override
    public List<Period> generateRollingPeriods( DateTimeUnit end )
    {
        Calendar calendar = getCalendar();

        List<Period> periods = Lists.newArrayList();
        end = adjustToStartOfWeek( end, calendar );
        end = calendar.minusDays( end, 357 );

        for ( int i = 0; i < 52; i++ )
        {
            periods.add( createPeriod( end, calendar ) );
            end = calendar.plusWeeks( end, 1 );
        }

        return periods;
    }

    @Override
    public String getIsoDate( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        int year;
        int week;

        if ( calendar.isIso8601() )
        {
            LocalDate date = LocalDate.of( dateTimeUnit.getYear(), dateTimeUnit.getMonth(), dateTimeUnit.getDay() );
            WeekFields weekFields = WeekFields.of(PeriodType.MAP_WEEK_TYPE.get( getName() ), 4 );

            year = date.get( weekFields.weekBasedYear() );
            week = date.get( weekFields.weekOfWeekBasedYear() );
        }
        else
        {
            dateTimeUnit = adjustToStartOfWeek( dateTimeUnit, calendar );
            week = calendar.week( dateTimeUnit );

            if ( week == 1 && dateTimeUnit.getMonth() == calendar.monthsInYear() )
            {
                dateTimeUnit.setYear( dateTimeUnit.getYear() + 1 );
            }

            year = dateTimeUnit.getYear();
        }

        return String.format( "%d%s%d", year, weekPrefix, week );
    }

    @Override
    public Date getRewindedDate( Date date, Integer rewindedPeriods )
    {
        Calendar cal = getCalendar();

        date = date != null ? date : new Date();
        rewindedPeriods = rewindedPeriods != null ? rewindedPeriods : 1;

        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( date );
        dateTimeUnit = cal.minusWeeks( dateTimeUnit, rewindedPeriods );

        return cal.toIso( dateTimeUnit ).toJdkDate();
    }

    public DateTimeUnit adjustToStartOfWeek( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        int weekday = calendar.weekday( dateTimeUnit );

        if ( weekday > startOfWeek )
        {
            dateTimeUnit = calendar.minusDays( dateTimeUnit, weekday - startOfWeek );
        }
        else if ( weekday < startOfWeek )
        {
            dateTimeUnit = calendar.minusDays( dateTimeUnit, weekday + (frequencyOrder - startOfWeek) );
        }

        return dateTimeUnit;
    }
}
