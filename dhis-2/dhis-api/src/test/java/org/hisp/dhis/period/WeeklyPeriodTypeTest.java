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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.GregorianCalendar;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Bob Jolliffe
 * @version $Id$
 */
public class WeeklyPeriodTypeTest
{
    WeeklyPeriodType periodType;

    @Before
    public void init()
    {
        periodType = new WeeklyPeriodType();
    }

    @Test
    @Ignore
    public void testGeneratePeriods()
    {
        DateTime testDate = new DateTime();
        
        WeeklyPeriodType wpt = new WeeklyPeriodType();

        for ( int year = 1990; year < 2020; year++ )
        {
            for ( int day = -7; day < 7; day++ )
            { 
                testDate = new DateTime( year, 1, 1, 0, 0); // 1st day of year
                testDate = testDate.minusDays( day );
                
                Period p1 = wpt.createPeriod( testDate.toDate() );
                List<Period> generatedPeriods = wpt.generatePeriods( p1 );
                assertTrue( "Period " + p1 + " in generated set", generatedPeriods.contains( p1 ) );
            }
        }
    }

    @Test
    public void testCreatePeriod()
    {   
        DateTime testDate = new DateTime( 2009, 4, 27, 0, 0 );
        WeeklyPeriodType wpt = new WeeklyPeriodType();
        Period p = wpt.createPeriod( testDate.toDate() );
        
        DateTime startDate = new DateTime( 2009, 4, 27, 0, 0 );
        DateTime endDate = new DateTime( 2009, 5, 3, 0, 0 );

        assertFalse( "start date after given date", startDate.isAfter(  p.getStartDate().getTime() ) );
        assertFalse( "end date before given date", endDate.isAfter( p.getEndDate().getTime() ) );
        

      assertTrue( startDate.getDayOfWeek() == DateTimeConstants.MONDAY );
      assertTrue( endDate.getDayOfWeek() == DateTimeConstants.SUNDAY  );
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
    public void testGeneratePeriodsWithPeriod()
    {
        Period period = new Period( periodType, new GregorianCalendar( 2008, 11, 28 ).getTime(),
            new GregorianCalendar( 2009, 0, 3 ).getTime() );

        List<Period> periods = periodType.generatePeriods( period );
        assertEquals( 53, periods.size() );

        period = new Period( periodType, new GregorianCalendar( 2012, 11, 31 ).getTime(),
            new GregorianCalendar( 2013, 0, 6 ).getTime() );
        periods = periodType.generatePeriods( period );
        assertEquals( 52, periods.size() );

        assertEquals( new Period( periodType, new GregorianCalendar( 2012, 11, 31 ).getTime(),
            new GregorianCalendar( 2013, 0, 6 ).getTime() ), periods.get( 0 ) );

        assertEquals( new Period( periodType, new GregorianCalendar( 2013, 11, 23 ).getTime(),
            new GregorianCalendar( 2013, 11, 29 ).getTime() ), periods.get( periods.size() - 1 ) );
    }

    @Test
    public void testGetIsoDate()
    {        
        DateTime testDate = new DateTime(2012, 12, 31, 0, 0);

        assertEquals( "2013W1", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime(2012, 12, 30, 0, 0);
        assertEquals( "2012W52", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime(2009, 12, 29, 0, 0);
        assertEquals( "2009W53", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );

        testDate = new DateTime(2010, 1, 4, 0, 0);
        assertEquals( "2010W1", periodType.getIsoDate( new Period( periodType, testDate.toDate(), testDate.toDate() ) ) );
    }
}
