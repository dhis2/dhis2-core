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

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class SixMonthlyPeriodTypeTest
{
    private DateTime startDate;
    private DateTime endDate;
    private DateTime testDate;
    private SixMonthlyPeriodType periodType;
    
    @Before
    public void before()
    {
       periodType = new SixMonthlyPeriodType();
    }
    
    @Test
    public void testCreatePeriod()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );
        
        startDate = new DateTime( 2009, 7, 1, 0, 0 );
        endDate = new DateTime( 2009, 12, 31, 0, 0 );

        Period period = periodType.createPeriod( testDate.toDate() );
        
        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );
        
        testDate = new DateTime( 2009, 4, 15, 0, 0 );
        
        startDate = new DateTime( 2009, 1, 1, 0, 0 );
        endDate = new DateTime( 2009, 6, 30, 0, 0 );

        period = periodType.createPeriod( testDate.toDate() );
        
        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );

        testDate = new DateTime( 2014, 6, 1, 0, 0 );

        startDate = new DateTime( 2014, 1, 1, 0, 0 );
        endDate = new DateTime( 2014, 6, 30, 0, 0 );
        
        period = periodType.createPeriod( testDate.toDate() );

        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );
        
        testDate = new DateTime( 2014, 7, 1, 0, 0 );

        startDate = new DateTime( 2014, 7, 1, 0, 0 );
        endDate = new DateTime( 2014, 12, 31, 0, 0 );
        
        period = periodType.createPeriod( testDate.toDate() );

        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );
    }

    @Test
    public void testGetNextPeriod()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );

        Period period = periodType.createPeriod( testDate.toDate() );
        
        period = periodType.getNextPeriod( period );

        startDate = new DateTime( 2010, 1, 1, 0, 0 );
        endDate = new DateTime( 2010, 6, 30, 0, 0 );

        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );

        testDate = new DateTime( 2009, 4, 15, 0, 0 );

        period = periodType.createPeriod( testDate.toDate() );

        period = periodType.getNextPeriod( period );

        startDate = new DateTime( 2009, 7, 1, 0, 0 );
        endDate = new DateTime( 2009, 12, 31, 0, 0 );

        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );
    }
    
    @Test
    public void testGetPreviousPeriod()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );

        Period period = periodType.createPeriod( testDate.toDate() );
        
        period = periodType.getPreviousPeriod( period );

        startDate = new DateTime( 2009, 1, 1, 0, 0 );
        endDate = new DateTime( 2009, 6, 30, 0, 0 );

        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );

        testDate = new DateTime( 2009, 4, 15, 0, 0 );

        period = periodType.createPeriod( testDate.toDate() );

        period = periodType.getPreviousPeriod( period );

        startDate = new DateTime( 2008, 7, 1, 0, 0 );
        endDate = new DateTime( 2008, 12, 31, 0, 0 );

        assertEquals( startDate.toDate(), period.getStartDate() );
        assertEquals( endDate.toDate(), period.getEndDate() );
    }
    
    @Test
    public void testGeneratePeriods()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );
        
        List<Period> periods = periodType.generatePeriods( testDate.toDate() );
        
        assertEquals( 2, periods.size() );
        assertEquals( periodType.createPeriod(new DateTime(  2009, 1, 1, 0, 0 ).toDate() ), periods.get( 0 ) );
        assertEquals( periodType.createPeriod(new DateTime(  2009, 7, 1, 0, 0 ).toDate() ), periods.get( 1 ) );

        testDate = new DateTime(  2009, 4, 15, 0, 0 );

        periods = periodType.generatePeriods( testDate.toDate() );

        assertEquals( 2, periods.size() );
        assertEquals( periodType.createPeriod(new DateTime(  2009, 1, 1, 0, 0 ).toDate() ), periods.get( 0 ) );
        assertEquals( periodType.createPeriod(new DateTime(  2009, 7, 1, 0, 0 ).toDate() ), periods.get( 1 ) );
    }

    @Test
    public void testGenerateRollingPeriods()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );
        
        List<Period> periods = periodType.generateRollingPeriods( testDate.toDate() );
        
        assertEquals( 2, periods.size() );
        assertEquals( periodType.createPeriod(new DateTime(  2009, 1, 1, 0, 0 ).toDate() ), periods.get( 0 ) );
        assertEquals( periodType.createPeriod(new DateTime(  2009, 7, 1, 0, 0 ).toDate() ), periods.get( 1 ) );

        testDate = new DateTime( 2009, 4, 15, 0, 0 );

        periods = periodType.generateRollingPeriods( testDate.toDate() );

        assertEquals( 2, periods.size() );
        assertEquals( periodType.createPeriod(new DateTime(  2008, 7, 1, 0, 0 ).toDate() ), periods.get( 0 ) );
        assertEquals( periodType.createPeriod(new DateTime(  2009, 1, 1, 0, 0 ).toDate() ), periods.get( 1 ) );
    }
    
    @Test
    public void testGenerateLast5Years()
    {
        testDate = new DateTime( 2009, 8, 15, 0, 0 );
        
        List<Period> periods = periodType.generateLast5Years( testDate.toDate() );
        
        assertEquals( 10, periods.size() );
        assertEquals( periodType.createPeriod(new DateTime(  2005, 1, 1, 0, 0 ).toDate() ), periods.get( 0 ) );
        assertEquals( periodType.createPeriod(new DateTime(  2005, 7, 1, 0, 0 ).toDate() ), periods.get( 1 ) );
    }
}
