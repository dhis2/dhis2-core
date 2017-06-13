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

import org.hisp.dhis.calendar.DateTimeUnit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * PeriodType for two-yearly Periods. A valid two-yearly Period has startDate
 * set to January 1st on an even year (2000, 2002, 2004, etc), and endDate set
 * to the last day of the next year.
 *
 * @author Torgeir Lorange Ostby
 * @version $Id: TwoYearlyPeriodType.java 2975 2007-03-03 22:24:36Z torgeilo $
 */
public class TwoYearlyPeriodType
    extends CalendarPeriodType
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 5813755542990991810L;

    /**
     * The name of the TwoYearlyPeriodType, which is "TwoYearly".
     */
    public static final String NAME = "TwoYearly";
    
    private static final String ISO8601_DURATION = "P2Y";

    public static final int FREQUENCY_ORDER = 730;

    // -------------------------------------------------------------------------
    // PeriodType functionality
    // -------------------------------------------------------------------------

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public Period createPeriod( Calendar cal )
    {
        cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) - cal.get( Calendar.YEAR ) % 2 );
        cal.set( Calendar.DAY_OF_YEAR, 1 );

        Date startDate = cal.getTime();

        cal.add( Calendar.YEAR, 1 );
        cal.set( Calendar.DAY_OF_YEAR, cal.getActualMaximum( Calendar.DAY_OF_YEAR ) );

        return new Period( this, startDate, cal.getTime() );
    }

    @Override
    public Period createPeriod( DateTimeUnit dateTimeUnit, org.hisp.dhis.calendar.Calendar calendar )
    {
        return null;
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
    public Period getNextPeriod( Period period, org.hisp.dhis.calendar.Calendar calendar )
    {
        Calendar cal = createCalendarInstance( period.getStartDate() );
        cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) - cal.get( Calendar.YEAR ) % 2 + 2 );
        cal.set( Calendar.DAY_OF_YEAR, 1 );

        Date startDate = cal.getTime();

        cal.add( Calendar.YEAR, 1 );
        cal.set( Calendar.DAY_OF_YEAR, cal.getActualMaximum( Calendar.DAY_OF_YEAR ) );

        return new Period( this, startDate, cal.getTime() );
    }

    @Override
    public Period getPreviousPeriod( Period period, org.hisp.dhis.calendar.Calendar calendar )
    {
        Calendar cal = createCalendarInstance( period.getStartDate() );
        cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) - cal.get( Calendar.YEAR ) % 2 - 2 );
        cal.set( Calendar.DAY_OF_YEAR, 1 );

        Date startDate = cal.getTime();

        cal.add( Calendar.YEAR, 1 );
        cal.set( Calendar.DAY_OF_YEAR, cal.getActualMaximum( Calendar.DAY_OF_YEAR ) );

        return new Period( this, startDate, cal.getTime() );
    }

    /**
     * Generates two-yearly Periods for the last 10, current and next 10 years.
     */
    @Override
    public List<Period> generatePeriods( Date date )
    {
        Calendar cal = createCalendarInstance( date );
        cal.add( Calendar.YEAR, cal.get( Calendar.YEAR ) % 2 == 0 ? -10 : -9 );
        cal.set( Calendar.DAY_OF_YEAR, 1 );

        ArrayList<Period> twoYears = new ArrayList<>();

        for ( int i = 0; i < 11; ++i )
        {
            Date startDate = cal.getTime();
            cal.add( Calendar.YEAR, 1 );
            cal.set( Calendar.DAY_OF_YEAR, cal.getActualMaximum( Calendar.DAY_OF_YEAR ) );
            twoYears.add( new Period( this, startDate, cal.getTime() ) );
            cal.add( Calendar.DAY_OF_YEAR, 1 );
        }

        return twoYears;
    }

    @Override
    public List<Period> generatePeriods( DateTimeUnit dateTimeUnit )
    {
        return null; // TODO
    }

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
        Calendar cal = createCalendarInstance( date );
        cal.add( Calendar.YEAR, cal.get( Calendar.YEAR ) % 2 == 0 ? -10 : -9 );
        cal.set( Calendar.DAY_OF_YEAR, 1 );

        ArrayList<Period> twoYears = new ArrayList<>();

        for ( int i = 0; i < 5; ++i )
        {
            Date startDate = cal.getTime();
            cal.add( Calendar.YEAR, 1 );
            cal.set( Calendar.DAY_OF_YEAR, cal.getActualMaximum( Calendar.DAY_OF_YEAR ) );
            twoYears.add( new Period( this, startDate, cal.getTime() ) );
            cal.add( Calendar.DAY_OF_YEAR, 1 );
        }

        return twoYears;
    }

    @Override
    public String getIsoDate( Period period )
    {
        return null; // TODO
    }

    @Override
    public String getIsoDate( DateTimeUnit dateTimeUnit, org.hisp.dhis.calendar.Calendar calendar )
    {
        return null; // TODO
    }

    @Override
    public Period createPeriod( String isoDate )
    {
        return null; // TODO
    }

    @Override
    public String getIsoFormat()
    {
        return null; // TODO
    }
    
    @Override
    public String getIso8601Duration() 
    {
        return ISO8601_DURATION; 
    }


    @Override
    public Date getRewindedDate( Date date, Integer rewindedPeriods )
    {
        date = date != null ? date : new Date();
        rewindedPeriods = rewindedPeriods != null ? rewindedPeriods : 1;

        Calendar cal = createCalendarInstance( date );
        cal.add( Calendar.YEAR, (rewindedPeriods * -2) );

        return cal.getTime();
    }
}
