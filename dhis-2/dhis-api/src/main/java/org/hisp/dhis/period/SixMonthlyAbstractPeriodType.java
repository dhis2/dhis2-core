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
 */

import com.google.common.collect.Lists;

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;

import java.util.Date;
import java.util.List;

/**
 * Abstract class for SixMonthly period types, including those starting
 * at the beginning of the calendar year and those starting at the beginning
 * of other months.
 *
 * @author Jim Grace
 */

public abstract class SixMonthlyAbstractPeriodType
    extends CalendarPeriodType
{
    private static final long serialVersionUID = -7135018015977806913L;

    public static final int FREQUENCY_ORDER = 182;

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    protected abstract int getBaseMonth();

    // -------------------------------------------------------------------------
    // PeriodType functionality
    // -------------------------------------------------------------------------

    @Override
    public Period createPeriod( DateTimeUnit dateTimeUnit, org.hisp.dhis.calendar.Calendar calendar )
    {
        DateTimeUnit start = new DateTimeUnit( dateTimeUnit );

        int baseMonth = getBaseMonth();

        int year = start.getMonth() < baseMonth ? (start.getYear() - 1) : start.getYear();
        int month = start.getMonth() >= baseMonth && start.getMonth() < (baseMonth + 6) ? baseMonth : (baseMonth + 6);

        start.setYear( year );
        start.setMonth( month );
        start.setDay( 1 );

        DateTimeUnit end = new DateTimeUnit( start );
        end = calendar.plusMonths( end, 5 );
        end.setDay( calendar.daysInMonth( end.getYear(), end.getMonth() ) );

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
        DateTimeUnit dateTimeUnit = calendar.fromIso( DateTimeUnit.fromJdkDate( period.getStartDate() ) );
        dateTimeUnit = calendar.plusMonths( dateTimeUnit, 6 );

        return createPeriod( calendar.toIso( dateTimeUnit ), calendar );
    }

    @Override
    public Period getPreviousPeriod( Period period, Calendar calendar )
    {
        DateTimeUnit dateTimeUnit = calendar.fromIso( DateTimeUnit.fromJdkDate( period.getStartDate() ) );
        dateTimeUnit = calendar.minusMonths( dateTimeUnit, 6 );

        return createPeriod( dateTimeUnit, calendar );
    }

    /**
     * Generates six-monthly Periods for the whole year in which the given
     * Period's startDate exists.
     */
    @Override
    public List<Period> generatePeriods( DateTimeUnit dateTimeUnit )
    {
        Calendar cal = getCalendar();

        Period period = createPeriod( dateTimeUnit, cal );
        dateTimeUnit = createLocalDateUnitInstance( period.getStartDate(), cal );

        List<Period> periods = Lists.newArrayList();

        if ( dateTimeUnit.getMonth() == getBaseMonth() )
        {
            periods.add( period );
            periods.add( getNextPeriod( period ) );
        }
        else
        {
            periods.add( getPreviousPeriod( period ) );
            periods.add( period );
        }

        return periods;
    }

    /**
     * Generates the last 2 six-months where the last one is the six-month
     * which the given date is inside.
     */
    @Override
    public List<Period> generateRollingPeriods( Date date )
    {
        Period period = createPeriod( date );

        List<Period> periods = Lists.newArrayList();

        periods.add( getPreviousPeriod( period ) );
        periods.add( period );

        return periods;
    }

    @Override
    public List<Period> generateRollingPeriods( DateTimeUnit dateTimeUnit )
    {
        return generateRollingPeriods( getCalendar().toIso( dateTimeUnit ).toJdkDate() );
    }

    @Override
    public Date getRewindedDate( Date date, Integer rewindedPeriods )
    {
        Calendar cal = getCalendar();

        date = date != null ? date : new Date();
        rewindedPeriods = rewindedPeriods != null ? rewindedPeriods : 1;

        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( date );
        cal.minusMonths( dateTimeUnit, rewindedPeriods * 6 );

        return cal.toIso( dateTimeUnit ).toJdkDate();
    }
}
