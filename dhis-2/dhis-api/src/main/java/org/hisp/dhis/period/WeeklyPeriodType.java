package org.hisp.dhis.period;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateInterval;
import org.hisp.dhis.calendar.DateIntervalType;
import org.hisp.dhis.calendar.DateTimeUnit;

import java.util.Date;
import java.util.List;

/**
 * PeriodType for weekly Periods. A valid weekly Period has startDate set to
 * monday and endDate set to sunday the same week, assuming monday is the first
 * day and sunday is the last day of the week.
 *
 * @author Torgeir Lorange Ostby
 * @version $Id: WeeklyPeriodType.java 2976 2007-03-03 22:50:19Z torgeilo $
 */
public class WeeklyPeriodType
    extends CalendarPeriodType
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 6466760375688564528L;

    private static final String ISO_FORMAT = "yyyyWn";

    private static final String ISO8601_DURATION = "P7D";

    /**
     * The name of the WeeklyPeriodType, which is "Weekly".
     */
    public static final String NAME = "Weekly";

    public static final int FREQUENCY_ORDER = 7;

    // -------------------------------------------------------------------------
    // PeriodType functionality
    // -------------------------------------------------------------------------

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public Period createPeriod( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        DateTimeUnit start = new DateTimeUnit( dateTimeUnit );
        start = calendar.minusDays( dateTimeUnit, calendar.weekday( start ) - 1 );

        DateTimeUnit end = new DateTimeUnit( start );
        end = calendar.plusDays( end, calendar.daysInWeek() - 1 );

        return toIsoPeriod( start, end, calendar );
    }

    @Override
    public int getFrequencyOrder()
    {
        return FREQUENCY_ORDER;
    }

    // -------------------------------------------------------------------------
    // CalendarPeriodType functionality
    // -------------------------------------------------------------------------

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
    public List<Period> generatePeriods( DateTimeUnit dateTimeUnit )
    {
        Calendar calendar = getCalendar();

        List<Period> periods = Lists.newArrayList();

        // rewind to start of week
        dateTimeUnit = calendar.minusDays( dateTimeUnit, calendar.weekday( dateTimeUnit ) - 1 );

        for ( int i = 0; i < calendar.weeksInYear( dateTimeUnit.getYear() ); i++ )
        {
            DateInterval interval = calendar.toInterval( dateTimeUnit, DateIntervalType.ISO8601_WEEK );
            periods.add( new Period( this, interval.getFrom().toJdkDate(), interval.getTo().toJdkDate() ) );

            dateTimeUnit = calendar.plusWeeks( dateTimeUnit, 1 );
        }

        return periods;
    }

    /**
     * Generates the last 52 weeks where the last one is the week which the
     * given date is inside.
     */
    @Override
    public List<Period> generateRollingPeriods( DateTimeUnit dateTimeUnit )
    {
        Calendar calendar = getCalendar();

        List<Period> periods = Lists.newArrayList();
        dateTimeUnit = calendar.minusDays( dateTimeUnit, calendar.weekday( dateTimeUnit ) - 1 );
        dateTimeUnit = calendar.minusDays( dateTimeUnit, 357 );

        for ( int i = 0; i < 52; i++ )
        {
            periods.add( createPeriod( dateTimeUnit, calendar ) );
            dateTimeUnit = calendar.plusWeeks( dateTimeUnit, 1 );
        }

        return periods;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    @Override
    public String getIsoDate( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        int week = calendar.week( dateTimeUnit );

        if ( week == 1 && dateTimeUnit.getMonth() == calendar.monthsInYear() )
        {
            dateTimeUnit.setYear( dateTimeUnit.getYear() + 1 );
        }

        return String.format( "%dW%d", dateTimeUnit.getYear(), week );
    }

    /**
     * n refers to week number, can be [1-53].
     */
    @Override
    public String getIsoFormat()
    {
        return ISO_FORMAT;
    }

    @Override
    public String getIso8601Duration()
    {
        return ISO8601_DURATION;
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
}
