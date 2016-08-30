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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class MonthlyPeriodTypeTest
{
    private DateTime startDate;
    private DateTime endDate;
    private DateTime testDate;
    private MonthlyPeriodType periodType = new MonthlyPeriodType();
        
    @Test
    public void testCreatePeriod()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );
        
        startDate = new DateTime( 2009, 8, 1, 0, 0 );
        endDate = new DateTime( 2009, 8, 31, 0, 0 );

        Period period = periodType.createPeriod( testDate.toDate() );
        
        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );
        
        testDate = new DateTime( 2009, 6, 15, 0, 0 );
        
        startDate = new DateTime( 2009, 6, 1, 0, 0 );
        endDate = new DateTime( 2009, 6, 30, 0, 0 );

        period = periodType.createPeriod( testDate.toDate() );
        
        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );
    }

    @Test
    public void testCreatePeriodFromISOString()
    {
        String isoPeriod = "201001";
        String alternativeIsoPeriod = "201001";
        
        Period period1 = periodType.createPeriod( isoPeriod );
        Period period2 = periodType.createPeriod( alternativeIsoPeriod );
        
        testDate = new DateTime( period1.getStartDate());
        assertEquals( 2010, testDate.getYear() );
        assertEquals( 1, testDate.getMonthOfYear() );
        
        testDate = new DateTime( period2.getStartDate());
        assertEquals( 2010, testDate.getYear() );
        assertEquals( 1, testDate.getMonthOfYear() );        
    }
    
    @Test
    public void testGetDaysInPeriod()
    {
        Period pA = periodType.createPeriod( "20040315" );
        Period pB = periodType.createPeriod( "201403" );
        Period pC = periodType.createPeriod( "2014Q2" );

        assertEquals( 1, pA.getDaysInPeriod() );
        assertEquals( 31, pB.getDaysInPeriod() );
        assertEquals( 91, pC.getDaysInPeriod() );
    }
        
    @Test
    public void testGetNextPeriod()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );

        Period period = periodType.createPeriod( testDate.toDate() );
        
        period = periodType.getNextPeriod( period );

        startDate = new DateTime( 2009, 9, 1, 0, 0 );
        endDate = new DateTime( 2009, 9, 30, 0, 0 );

        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );
    }
    
    @Test
    public void testGetNextPeriods()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );

        Period period = periodType.createPeriod( testDate.toDate() );
        
        period = periodType.getNextPeriod( period, 3 );

        startDate = new DateTime( 2009, 11, 1, 0, 0 );
        endDate = new DateTime( 2009, 11, 30, 0, 0 );

        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );
    }
    
    @Test
    public void testGetPreviousPeriod()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );

        Period period = periodType.createPeriod( testDate.toDate() );
        
        period = periodType.getPreviousPeriod( period );

        startDate = new DateTime( 2009, 7, 1, 0, 0 );
        endDate = new DateTime( 2009, 7, 31, 0, 0 );

        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );
    }

    @Test
    public void testIsAfter()
    {
        Period april = periodType.createPeriod( new DateTime( 2009, 4, 1, 0, 0 ).toDate() );
        Period may = periodType.createPeriod( new DateTime( 2009, 5, 1, 0, 0 ).toDate() );
        
        assertTrue( may.isAfter( april ) );
        assertFalse( april.isAfter( may ) );
        assertFalse( may.isAfter( null ) );
    }
    
    @Test
    public void testGeneratePeriods()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );
        
        List<Period> periods = periodType.generatePeriods( testDate.toDate() );
        
        assertEquals( 12, periods.size() );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 1, 1, 0, 0 ).toDate() ), periods.get( 0 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 2, 1, 0, 0 ).toDate() ), periods.get( 1 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 3, 1, 0, 0 ).toDate() ), periods.get( 2 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 4, 1, 0, 0 ).toDate() ), periods.get( 3 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 5, 1, 0, 0 ).toDate() ), periods.get( 4 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 6, 1, 0, 0 ).toDate() ), periods.get( 5 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 7, 1, 0, 0 ).toDate() ), periods.get( 6 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 8, 1, 0, 0 ).toDate() ), periods.get( 7 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 9, 1, 0, 0 ).toDate() ), periods.get( 8 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 10, 1, 0, 0 ).toDate() ), periods.get( 9 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 11, 1, 0, 0 ).toDate() ), periods.get( 10 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 12, 1, 0, 0 ).toDate() ), periods.get( 11 ) );
    }

    @Test
    public void testGenerateRollingPeriods()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );
        
        List<Period> periods = periodType.generateRollingPeriods( testDate.toDate() );
        
        assertEquals( 12, periods.size() );
        assertEquals( periodType.createPeriod( new DateTime(  2008, 9, 1, 0, 0 ).toDate() ), periods.get( 0 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2008, 10, 1, 0, 0 ).toDate() ), periods.get( 1 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2008, 11, 1, 0, 0 ).toDate() ), periods.get( 2 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2008, 12, 1, 0, 0 ).toDate() ), periods.get( 3 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 1, 1, 0, 0 ).toDate() ), periods.get( 4 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 2, 1, 0, 0 ).toDate() ), periods.get( 5 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 3, 1, 0, 0 ).toDate() ), periods.get( 6 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 4, 1, 0, 0 ).toDate() ), periods.get( 7 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 5, 1, 0, 0 ).toDate() ), periods.get( 8 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 6, 1, 0, 0 ).toDate() ), periods.get( 9 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 7, 1, 0, 0 ).toDate() ), periods.get( 10 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 8, 1, 0, 0 ).toDate() ), periods.get( 11 ) );
    }
    
    @Test
    public void testGenerateLast5Years()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );
        
        List<Period> periods = periodType.generateLast5Years( testDate.toDate() );
        
        assertEquals( 60, periods.size() );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 1, 1, 0, 0 ).toDate() ), periods.get( 0 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 2, 1, 0, 0 ).toDate() ), periods.get( 1 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 3, 1, 0, 0 ).toDate() ), periods.get( 2 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 4, 1, 0, 0 ).toDate() ), periods.get( 3 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 5, 1, 0, 0 ).toDate() ), periods.get( 4 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 6, 1, 0, 0 ).toDate() ), periods.get( 5 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 7, 1, 0, 0 ).toDate() ), periods.get( 6 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 8, 1, 0, 0 ).toDate() ), periods.get( 7 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 9, 1, 0, 0 ).toDate() ), periods.get( 8 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 10, 1, 0, 0 ).toDate() ), periods.get( 9 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 11, 1, 0, 0 ).toDate() ), periods.get( 10 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2005, 12, 1, 0, 0 ).toDate() ), periods.get( 11 ) );
    }
    
    @Test
    public void testGeneratePeriodsBetweenDates()
    {
        startDate = new DateTime( 2009, 8, 15, 0, 0 );
        endDate = new DateTime( 2010, 2, 20, 0, 0 );
        
        List<Period> periods = periodType.generatePeriods( startDate.toDate(), endDate.toDate() );

        assertEquals( 7, periods.size() );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 8, 1, 0, 0 ).toDate() ), periods.get( 0 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 9, 1, 0, 0 ).toDate() ), periods.get( 1 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 10, 1, 0, 0 ).toDate() ), periods.get( 2 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 11, 1, 0, 0 ).toDate() ), periods.get( 3 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2009, 12, 1, 0, 0 ).toDate() ), periods.get( 4 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2010, 1, 1, 0, 0 ).toDate() ), periods.get( 5 ) );
        assertEquals( periodType.createPeriod( new DateTime(  2010, 2, 1, 0, 0 ).toDate() ), periods.get( 6 ) );
    }
    
    @Test
    public void testGetPeriodsBetween()
    {
        assertEquals( 1, periodType.createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 2, new BiMonthlyPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 3, new QuarterlyPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 6, new SixMonthlyPeriodType().createPeriod().getPeriodSpan( periodType ) );
        assertEquals( 12, new YearlyPeriodType().createPeriod().getPeriodSpan( periodType ) );
    }
}
