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

import org.hisp.dhis.calendar.DateTimeUnit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.Before;
import org.junit.Test;

import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Bob Jolliffe
 */
public class WeeklyPeriodTypeTest
{
    WeeklyAbstractPeriodType periodType;

    @Before
    public void init()
    {
        periodType = new WeeklyPeriodType();
    }

    @Test
    public void testCreatePeriod()
    {
        DateTime testDate = new DateTime( 2009, 4, 27, 0, 0 );
        WeeklyPeriodType wpt = new WeeklyPeriodType();
        Period p = wpt.createPeriod( testDate.toDate() );

        DateTime startDate = new DateTime( 2009, 4, 27, 0, 0 );
        DateTime endDate = new DateTime( 2009, 5, 3, 0, 0 );

        assertFalse( "start date after given date", startDate.isAfter( p.getStartDate().getTime() ) );
        assertFalse( "end date before given date", endDate.isAfter( p.getEndDate().getTime() ) );


        assertTrue( startDate.getDayOfWeek() == DateTimeConstants.MONDAY );
        assertTrue( endDate.getDayOfWeek() == DateTimeConstants.SUNDAY );
    }

    @Test
    public void isoDates()
    {
        DateTime testDate = new DateTime( 2008, 12, 29, 0, 0 );

        Period period = periodType.createPeriod( "2009W1" );
        assertEquals( testDate.toDate(), period.getStartDate() );

        testDate = new DateTime( 2011, 1, 3, 0, 0 );
        period = periodType.createPeriod( "2011W1" );
        assertEquals( testDate.toDate(), period.getStartDate() );

        testDate = new DateTime( 2011, 3, 14, 0, 0 );
        period = periodType.createPeriod( "2011W11" );
        assertEquals( testDate.toDate(), period.getStartDate() );
    }

    @Test
    public void getIsoDate()
    {
        DateTime testDate = new DateTime( 2011, 1, 3, 0, 0 );

        Period p = periodType.createPeriod( testDate.toDate() );
        assertEquals( "2011W1", p.getIsoDate() );

        testDate = new DateTime( 2012, 12, 31, 0, 0 ); // Monday
        p = periodType.createPeriod( testDate.toDate() );
        assertEquals( "2013W1", p.getIsoDate() );
    }

    @Test
    public void testGetPeriodsBetween()
    {
        assertEquals( 1, periodType.createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 1, new WeeklyWednesdayPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 1, new WeeklyThursdayPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 1, new WeeklySaturdayPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 1, new WeeklySundayPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 4, new MonthlyPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 8, new BiMonthlyPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 13, new QuarterlyPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 26, new SixMonthlyPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 52, new YearlyPeriodType().createPeriod().getPeriodSpan( periodType ) );
    }

    @Test
    public void testGeneratePeriodsWithCalendar()
    {
        List<Period> periods = periodType.generatePeriods( new GregorianCalendar( 2009, 0, 1 ).getTime() );
        assertEquals( 53, periods.size() );

        periods = periodType.generatePeriods( new GregorianCalendar( 2011, 0, 3 ).getTime() );
        assertEquals( 52, periods.size() );
    }

    @Test
    public void testGetIsoDate()
    {
        DateTime testDate = new DateTime( 2012, 12, 31, 0, 0 );

        assertEquals( "2013W1", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2012, 12, 30, 0, 0 );
        assertEquals( "2012W52", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2009, 12, 29, 0, 0 );
        assertEquals( "2009W53", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2010, 1, 4, 0, 0 );
        assertEquals( "2010W1", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );
    }

    @Test
    public void testWeeklyWednesday()
    {
        periodType = new WeeklyWednesdayPeriodType();

        DateTime testDate = new DateTime( 2017, 5, 4, 0, 0 );
        assertEquals( "2017WedW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 3, 0, 0 );
        assertEquals( "2017WedW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 4, 0, 0 );
        assertEquals( "2017WedW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 2, 0, 0 );
        assertEquals( "2017WedW17", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 1, 0, 0 );
        assertEquals( "2017WedW17", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        Period period = periodType.createPeriod( "2017WedW17" );
        assertNotNull( period );
        assertEquals( "2017WedW17", periodType.getIsoDate( period ) );

        period = periodType.createPeriod( "2017WedW18" );
        assertNotNull( period );
        assertEquals( "2017WedW18", periodType.getIsoDate( period ) );
    }

    @Test
    public void testWeeklyThursday()
    {
        periodType = new WeeklyThursdayPeriodType();

        DateTime testDate = new DateTime( 2017, 5, 7, 0, 0 );
        assertEquals( "2017ThuW19", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 6, 0, 0 );
        assertEquals( "2017ThuW19", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 5, 0, 0 );
        assertEquals( "2017ThuW19", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 4, 0, 0 );
        assertEquals( "2017ThuW19", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 3, 0, 0 );
        assertEquals( "2017ThuW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 2, 0, 0 );
        assertEquals( "2017ThuW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 1, 0, 0 );
        assertEquals( "2017ThuW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        Period period = periodType.createPeriod( "2017ThuW17" );
        assertNotNull( period );
        assertEquals( "2017ThuW17", periodType.getIsoDate( period ) );

        period = periodType.createPeriod( "2017ThuW18" );
        assertNotNull( period );
        assertEquals( "2017ThuW18", periodType.getIsoDate( period ) );
    }

    @Test
    public void testWeeklySaturday()
    {
        periodType = new WeeklySaturdayPeriodType();

        DateTime testDate = new DateTime( 2017, 5, 7, 0, 0 );
        assertEquals( "2017SatW19", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 6, 0, 0 );
        assertEquals( "2017SatW19", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 5, 0, 0 );
        assertEquals( "2017SatW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 4, 0, 0 );
        assertEquals( "2017SatW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 3, 0, 0 );
        assertEquals( "2017SatW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 2, 0, 0 );
        assertEquals( "2017SatW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 1, 0, 0 );
        assertEquals( "2017SatW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        Period period = periodType.createPeriod( "2017SatW17" );
        assertNotNull( period );
        assertEquals( "2017SatW17", periodType.getIsoDate( period ) );

        period = periodType.createPeriod( "2017SatW18" );
        assertNotNull( period );
        assertEquals( "2017SatW18", periodType.getIsoDate( period ) );
    }

    @Test
    public void testWeeklySunday()
    {
        periodType = new WeeklySundayPeriodType();

        DateTime testDate = new DateTime( 2017, 5, 7, 0, 0 );
        assertEquals( "2017SunW19", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 6, 0, 0 );
        assertEquals( "2017SunW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 5, 0, 0 );
        assertEquals( "2017SunW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 4, 0, 0 );
        assertEquals( "2017SunW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 3, 0, 0 );
        assertEquals( "2017SunW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 2, 0, 0 );
        assertEquals( "2017SunW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 5, 1, 0, 0 );
        assertEquals( "2017SunW18", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 1, 8, 0, 0 );
        assertEquals( "2017SunW2", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime( 2017, 1, 1, 0, 0 );
        assertEquals( "2017SunW1", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        Period period = periodType.createPeriod( "2017SunW17" );
        assertNotNull( period );
        assertEquals( "2017SunW17", periodType.getIsoDate( period ) );

        period = periodType.createPeriod( "2017SunW18" );
        assertNotNull( period );
        assertEquals( "2017SunW18", periodType.getIsoDate( period ) );
    }

    @Test
    public void testGenerateWeeklyPeriodWithinAYear()
    {
        periodType = new WeeklyPeriodType();

        List<Period> periods = periodType.generatePeriods( new DateTimeUnit( 2019, 4, 1 ) );
        assertEquals( 52, periods.size() );
        assertFalse( periodsInYear( periods, 2018 ) );
        assertFalse( periodsInYear( periods, 2019 ) );
        assertFalse( periodsInYear( periods, 2020 ) );
        assertEquals( 2018, DateTimeUnit.fromJdkDate( periods.get( 0 ).getStartDate() ).getYear() );
        assertEquals( 2019, DateTimeUnit.fromJdkDate( periods.get( 1 ).getStartDate() ).getYear() );

        periods = periodType.generatePeriods( new DateTimeUnit( 2018, 1, 1 ) );
        assertEquals( 52, periods.size() );
        assertFalse( periodsInYear( periods, 2017 ) );
        assertTrue( periodsInYear( periods, 2018 ) );
        assertFalse( periodsInYear( periods, 2019 ) );

        periods = periodType.generatePeriods( new DateTimeUnit( 2015, 4, 1 ) );
        assertEquals( 53, periods.size() );
        assertFalse( periodsInYear( periods, 2014 ) );
        assertFalse( periodsInYear( periods, 2015 ) );
        assertFalse( periodsInYear( periods, 2016 ) );
        assertEquals( 2014, DateTimeUnit.fromJdkDate( periods.get( 0 ).getStartDate() ).getYear() );
        assertEquals( 2015, DateTimeUnit.fromJdkDate( periods.get( 1 ).getStartDate() ).getYear() );

        periods = periodType.generatePeriods( new DateTimeUnit( 1990, 1, 1 ) );
        assertEquals( 52, periods.size() );
        assertFalse( periodsInYear( periods, 1989 ) );
        assertTrue( periodsInYear( periods, 1990 ) );
        assertFalse( periodsInYear( periods, 1991 ) );
        assertEquals( 1990, DateTimeUnit.fromJdkDate( periods.get( 0 ).getStartDate() ).getYear() );

        periods = periodType.generatePeriods( new DateTimeUnit( 1981, 1, 1 ) );
        assertEquals( 53, periods.size() );
        assertFalse( periodsInYear( periods, 1980 ) );
        assertFalse( periodsInYear( periods, 1981 ) );
        assertFalse( periodsInYear( periods, 1982 ) );
        assertEquals( 1980, DateTimeUnit.fromJdkDate( periods.get( 0 ).getStartDate() ).getYear() );
        assertEquals( 1981, DateTimeUnit.fromJdkDate( periods.get( 1 ).getStartDate() ).getYear() );

        periods = periodType.generatePeriods( new DateTimeUnit( 1980, 12, 29 ) );
        assertEquals( 52, periods.size() );
        assertFalse( periodsInYear( periods, 1980 ) );
        assertFalse( periodsInYear( periods, 1980 ) );
        assertFalse( periodsInYear( periods, 1981 ) );
        assertEquals( 1979, DateTimeUnit.fromJdkDate( periods.get( 0 ).getStartDate() ).getYear() );
        assertEquals( 1980, DateTimeUnit.fromJdkDate( periods.get( 1 ).getStartDate() ).getYear() );
    }

    private boolean periodsInYear( List<Period> periods, int year )
    {
        for ( Period period : periods )
        {
            DateTimeUnit start = DateTimeUnit.fromJdkDate( period.getStartDate() );

            if ( start.getYear() != year )
            {
                return false;
            }
        }

        return true;
    }
}
