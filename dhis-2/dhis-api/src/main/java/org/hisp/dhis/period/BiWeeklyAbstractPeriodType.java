package org.hisp.dhis.period;


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

import com.google.common.collect.Lists;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Kristian Wærstad
 */
public class BiWeeklyAbstractPeriodType
    extends CalendarPeriodType
{
    protected final String name;

    protected final int startOfWeek;

    protected final String isoFormat;

    protected final String isoDuration;

    protected final int frequencyOrder;

    protected final String weekPrefix;

    protected BiWeeklyAbstractPeriodType( String name, int startOfWeek, String isoFormat, String isoDuration,
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
        DateTimeUnit start = adjustToStartOfBiWeek( new DateTimeUnit( dateTimeUnit ), calendar );
        DateTimeUnit end = new DateTimeUnit( start );
        end = calendar.plusDays( end, calendar.daysInWeek() * 2 - 1 );

        return toIsoPeriod( start, end, calendar );
    }

    @Override
    protected DateTimeUnit getDateWithOffset( DateTimeUnit dateTimeUnit, int offset, Calendar calendar )
    {
        return calendar.plusWeeks( dateTimeUnit, 2 * offset );
    }

    /**
     * Generates bi-weekly Periods for the whole year in which the given Period's
     * startDate exists.
     */
    @Override
    public List<Period> generatePeriods( DateTimeUnit start )
    {
        Calendar calendar = getCalendar();
        List<Period> periods = new ArrayList<>();
        DateTimeUnit date = adjustToStartOfBiWeek( start, calendar );
        date = adjustToStartOfBiWeek( calendar.fromIso( date.getYear(), 1, 1 ), calendar );

        for ( int i = 0; i < calendar.weeksInYear( start.getYear() ) / 2; i++ )
        {
            periods.add( createPeriod( date, calendar ) );
            date = calendar.plusWeeks( date, 2 );
        }

        return periods;
    }

    /**
     * Generates the last 26 bi-weeks where the last one is the week which the
     * given date is inside.
     */
    @Override
    public List<Period> generateRollingPeriods( DateTimeUnit end, Calendar calendar )
    {
        List<Period> periods = Lists.newArrayList();
        DateTimeUnit date = adjustToStartOfBiWeek( end, calendar );
        date = calendar.minusDays( date, 350 );

        for ( int i = 0; i < 26; i++ )
        {
            periods.add( createPeriod( date, calendar ) );
            date = calendar.plusWeeks( date, 2 );
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
            WeekFields weekFields = WeekFields.of( DayOfWeek.MONDAY, 4 );

            year = date.get( weekFields.weekBasedYear() );
            week = date.get( weekFields.weekOfWeekBasedYear() ) / 2;
        }
        else
        {
            DateTimeUnit date = adjustToStartOfBiWeek( dateTimeUnit, calendar );
            week = calendar.week( date );

            if ( week == 1 && date.getMonth() == calendar.monthsInYear() )
            {
                date.setYear( date.getYear() + 1 );
            }

            year = date.getYear();
        }

        return String.format( "%d%s%d", year, weekPrefix, week );
    }

    @Override
    public Date getRewindedDate( Date date, Integer rewindedPeriods )
    {
        Calendar cal = getCalendar();

        Date rewindedDate = date != null ? date : new Date();
        rewindedPeriods = rewindedPeriods != null ? rewindedPeriods : 1;

        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( rewindedDate );
        dateTimeUnit = cal.minusWeeks( dateTimeUnit, rewindedPeriods * 2);

        return cal.toIso( dateTimeUnit ).toJdkDate();
    }

    public DateTimeUnit adjustToStartOfBiWeek( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        int biWeekday = calendar.weekday( dateTimeUnit ) +
            ( 1 - ( calendar.week( dateTimeUnit ) % 2 ) ) * 7;

        if ( biWeekday > startOfWeek )
        {
            dateTimeUnit = calendar.minusDays( dateTimeUnit, biWeekday - startOfWeek );
        }
        else if ( biWeekday < startOfWeek )
        {
            dateTimeUnit = calendar.minusDays( dateTimeUnit, biWeekday + (frequencyOrder - startOfWeek) );
        }

        return dateTimeUnit;
    }
}

