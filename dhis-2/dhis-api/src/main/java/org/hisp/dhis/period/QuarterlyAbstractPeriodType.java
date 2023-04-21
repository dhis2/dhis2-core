/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.period;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;

import com.google.common.collect.Lists;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
public abstract class QuarterlyAbstractPeriodType
    extends CalendarPeriodType
{
    public static final int FREQUENCY_ORDER = 91;

    private static final String ISO8601_DURATION = "P3M";

    public static final String SQL_INTERVAL = "3 months";

    protected static final String ISO_CALENDAR_NAME = org.hisp.dhis.calendar.impl.Iso8601Calendar.getInstance().name();

    // -------------------------------------------------------------------------
    // PeriodType functionality
    // -------------------------------------------------------------------------

    @Override
    public int getFrequencyOrder()
    {
        return FREQUENCY_ORDER;
    }

    @Override
    public String getSqlInterval()
    {
        return SQL_INTERVAL;
    }

    @Override
    public String getIso8601Duration()
    {
        return ISO8601_DURATION;
    }

    // -------------------------------------------------------------------------
    // CalendarPeriodType functionality
    // -------------------------------------------------------------------------

    @Override
    public DateTimeUnit getDateWithOffset( DateTimeUnit dateTimeUnit, int offset, Calendar calendar )
    {
        return calendar.plusMonths( dateTimeUnit, offset * 3 );
    }

    /**
     * Generates quarterly Periods for the whole year in which the given
     * Period's startDate exists.
     */
    @Override
    public List<Period> generatePeriods( DateTimeUnit dateTimeUnit )
    {
        org.hisp.dhis.calendar.Calendar cal = getCalendar();

        dateTimeUnit.setMonth( 1 );
        dateTimeUnit.setDay( 1 );

        int year = dateTimeUnit.getYear();
        List<Period> periods = Lists.newArrayList();

        while ( year == dateTimeUnit.getYear() )
        {
            periods.add( createPeriod( dateTimeUnit, cal ) );
            dateTimeUnit = cal.plusMonths( dateTimeUnit, 3 );
        }

        return periods;
    }

    /**
     * Generates the last 4 quarters where the last one is the quarter which the
     * given date is inside.
     */
    @Override
    public List<Period> generateRollingPeriods( Date date )
    {
        date = createPeriod( date ).getStartDate();

        return generateRollingPeriods( createLocalDateUnitInstance( date ), getCalendar() );
    }

    @Override
    public List<Period> generateRollingPeriods( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        dateTimeUnit.setDay( 1 );

        DateTimeUnit iterationDateTimeUnit = calendar.minusMonths( dateTimeUnit, 9 );

        List<Period> periods = Lists.newArrayList();

        for ( int i = 0; i < 4; i++ )
        {
            periods.add( createPeriod( iterationDateTimeUnit, calendar ) );
            iterationDateTimeUnit = calendar.plusMonths( iterationDateTimeUnit, 3 );
        }

        return periods;
    }

    @Override
    public Date getRewindedDate( Date date, Integer rewindedPeriods )
    {
        Calendar cal = getCalendar();

        date = date != null ? date : new Date();
        rewindedPeriods = rewindedPeriods != null ? rewindedPeriods : 1;

        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( date );
        dateTimeUnit = cal.minusMonths( dateTimeUnit, rewindedPeriods * 3 );

        return cal.toIso( dateTimeUnit ).toJdkDate();
    }
}