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

import java.util.Date;
import java.util.List;

/**
 * PeriodType for quarterly Periods. A valid quarterly Period has startDate set
 * to the first day of a calendar quarter, and endDate set to the last day of
 * the same quarter.
 *
 * @author Torgeir Lorange Ostby
 * @version $Id: QuarterlyPeriodType.java 2971 2007-03-03 18:54:56Z torgeilo $
 */
public class QuarterlyPeriodType
    extends CalendarPeriodType
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = -5973809094923012052L;

    private static final String ISO_FORMAT = "yyyyQn";

    private static final String ISO8601_DURATION = "P3M";

    private static final String ISO_CALENDAR_NAME = org.hisp.dhis.calendar.impl.Iso8601Calendar.getInstance().name();

    /**
     * The name of the QuarterlyPeriodType, which is "Quarterly".
     */
    public static final String NAME = "Quarterly";

    public static final int FREQUENCY_ORDER = 91;

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

        start.setMonth( ((dateTimeUnit.getMonth() - 1) - ((dateTimeUnit.getMonth() - 1) % 3)) + 1 );
        start.setDay( 1 );

        if ( start.getMonth() > 12 )
        {
            start.setYear( start.getYear() + 1 );
            start.setMonth( 1 );
        }

        DateTimeUnit end = new DateTimeUnit( start );
        end = calendar.plusMonths( end, 2 );
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
    public String getIsoDate( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        DateTimeUnit newUnit = dateTimeUnit;
        
        if ( !calendar.name().equals( ISO_CALENDAR_NAME ) && newUnit.isIso8601() )
        {
            newUnit = calendar.fromIso( newUnit );
        }

        switch ( newUnit.getMonth() )
        {
            case 1:
                return newUnit.getYear() + "Q1";
            case 4:
                return newUnit.getYear() + "Q2";
            case 7:
                return newUnit.getYear() + "Q3";
            case 10:
                return newUnit.getYear() + "Q4";
            default:
                throw new IllegalArgumentException( "Month not valid [1,4,7,10], was given " + dateTimeUnit.getMonth() );
        }
    }

    /**
     * n refers to the quarter, can be [1-4].
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
        dateTimeUnit = cal.minusMonths( dateTimeUnit, rewindedPeriods * 3 );

        return cal.toIso( dateTimeUnit ).toJdkDate();
    }
}
