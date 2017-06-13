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
 * @author Lars Helge Overland
 */
public abstract class FinancialPeriodType
    extends CalendarPeriodType
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 2649990007010207631L;

    public static final int FREQUENCY_ORDER = 365;

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    protected abstract int getBaseMonth();

    // -------------------------------------------------------------------------
    // PeriodType functionality
    // -------------------------------------------------------------------------

    @Override
    public Period createPeriod( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        boolean past = dateTimeUnit.getMonth() >= (getBaseMonth() + 1);

        if ( !past )
        {
            dateTimeUnit = calendar.minusYears( dateTimeUnit, 1 );
        }

        dateTimeUnit.setMonth( getBaseMonth() + 1 );
        dateTimeUnit.setDay( 1 );

        DateTimeUnit start = new DateTimeUnit( dateTimeUnit );
        DateTimeUnit end = new DateTimeUnit( dateTimeUnit );

        end = calendar.plusYears( end, 1 );
        end = calendar.minusDays( end, 1 );

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
        dateTimeUnit = calendar.plusYears( dateTimeUnit, 1 );

        return createPeriod( dateTimeUnit, calendar );
    }

    @Override
    public Period getPreviousPeriod( Period period, Calendar calendar )
    {
        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( period.getStartDate(), calendar );
        dateTimeUnit = calendar.minusYears( dateTimeUnit, 1 );

        return createPeriod( dateTimeUnit, calendar );
    }

    /**
     * Generates financial yearly periods for the last 5, current and next 5
     * financial years.
     */
    @Override
    public List<Period> generatePeriods( DateTimeUnit dateTimeUnit )
    {
        Calendar cal = getCalendar();

        boolean past = dateTimeUnit.getMonth() >= (getBaseMonth() + 1);

        List<Period> periods = Lists.newArrayList();

        dateTimeUnit = cal.minusYears( dateTimeUnit, past ? 5 : 6 );
        dateTimeUnit.setMonth( getBaseMonth() + 1 );
        dateTimeUnit.setDay( 1 );

        Calendar calendar = getCalendar();

        for ( int i = 0; i < 11; i++ )
        {
            periods.add( createPeriod( dateTimeUnit, cal ) );
            dateTimeUnit = calendar.plusYears( dateTimeUnit, 1 );
        }

        return periods;
    }

    /**
     * Generates the last 5 financial years where the last one is the financial
     * year which the given date is inside.
     */
    @Override
    public List<Period> generateRollingPeriods( Date date )
    {
        return generateLast5Years( date );
    }

    @Override
    public List<Period> generateRollingPeriods( DateTimeUnit dateTimeUnit )
    {
        return generateLast5Years( getCalendar().toIso( dateTimeUnit ).toJdkDate() );
    }

    @Override
    public List<Period> generateLast5Years( Date date )
    {
        Calendar cal = getCalendar();

        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( date, cal );
        boolean past = dateTimeUnit.getMonth() >= (getBaseMonth() + 1);

        List<Period> periods = Lists.newArrayList();

        dateTimeUnit = cal.minusYears( dateTimeUnit, past ? 4 : 5 );
        dateTimeUnit.setMonth( getBaseMonth() + 1 );
        dateTimeUnit.setDay( 1 );

        for ( int i = 0; i < 5; i++ )
        {
            periods.add( createPeriod( dateTimeUnit, cal ) );
            dateTimeUnit = cal.plusYears( dateTimeUnit, 1 );
        }

        return periods;
    }

    @Override
    public Date getRewindedDate( Date date, Integer rewindedPeriods )
    {
        Calendar cal = getCalendar();

        date = date != null ? date : new Date();
        rewindedPeriods = rewindedPeriods != null ? rewindedPeriods : 1;

        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( date, cal );
        dateTimeUnit = cal.minusYears( dateTimeUnit, rewindedPeriods );

        return cal.toIso( dateTimeUnit ).toJdkDate();
    }
}
