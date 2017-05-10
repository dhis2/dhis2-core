/*
 * Copyright (c) 2017, UiO
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.calendar.impl;


import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author hans
 */
public class PersianCalendarTest
{
    
    private Calendar calendar;

    @Before
    public void init()
    {
        calendar = PersianCalendar.getInstance();
    }
   
    @Test
    public void testIsoStartOfYear()
    {
        DateTimeUnit startOfYear = calendar.isoStartOfYear( 1383 );
        Assert.assertEquals( 2004, startOfYear.getYear() );
        Assert.assertEquals( 3, startOfYear.getMonth() );
        Assert.assertEquals( 20, startOfYear.getDay() );
        
        startOfYear = calendar.isoStartOfYear( 1409 );
        Assert.assertEquals( 2030, startOfYear.getYear() );
        Assert.assertEquals( 3, startOfYear.getMonth() );
        Assert.assertEquals( 21, startOfYear.getDay() );        
    }
    
    @Test( expected = RuntimeException.class )
    public void testDaysInMonth13()
    {
        calendar.daysInMonth( 1389, 13 );
    }
    
    
    @Test
    public void testDaysInMonth()
    {
        Assert.assertEquals( 29, calendar.daysInMonth( 1389, 12 ));
        Assert.assertEquals( 30, calendar.daysInMonth( 1395, 12 ));
    }
    
    @Test
    public void testDaysInYears()
    {
        Assert.assertEquals( 365, calendar.daysInYear( 1389 ));
        Assert.assertEquals( 366, calendar.daysInYear( 1395 ));
    }
      

    @Test
    public void testToIso()
    {
        Assert.assertEquals( new DateTimeUnit( 1993, 3, 21, true ), calendar.toIso( new DateTimeUnit( 1372, 1, 1 ) ) );
        Assert.assertEquals( new DateTimeUnit( 2020, 3, 20, true ), calendar.toIso( new DateTimeUnit( 1399, 1, 1 ) ) );
    }
       

    @Test
    public void testFromIso()
    {
        Assert.assertEquals( new DateTimeUnit( 1372, 1, 1, false ), calendar.fromIso( new DateTimeUnit( 1993, 3, 21, true ) ) );
        Assert.assertEquals( new DateTimeUnit( 1399, 1, 1, false ), calendar.fromIso( new DateTimeUnit( 2020, 3, 20, true ) ) );
    }
    
    @Test
    public void testPlusDays()
    {
        DateTimeUnit dateTimeUnit = new DateTimeUnit( 1382, 1, 1 );

        DateTimeUnit testDateTimeUnit = calendar.plusDays( dateTimeUnit, 31 );
        Assert.assertEquals( 1382, testDateTimeUnit.getYear() );
        Assert.assertEquals( 2, testDateTimeUnit.getMonth() );
        Assert.assertEquals( 1, testDateTimeUnit.getDay() );

        testDateTimeUnit = calendar.plusDays( dateTimeUnit, 366 );
        Assert.assertEquals( 1383, testDateTimeUnit.getYear() );
        Assert.assertEquals( 1, testDateTimeUnit.getMonth() );
        Assert.assertEquals( 2, testDateTimeUnit.getDay() );

        dateTimeUnit = new DateTimeUnit( 1403, 12, 29 );

        testDateTimeUnit = calendar.plusDays( dateTimeUnit, 1 );
        Assert.assertEquals( 1403, testDateTimeUnit.getYear() );
        Assert.assertEquals( 12, testDateTimeUnit.getMonth() );
        Assert.assertEquals( 30, testDateTimeUnit.getDay() );

    }
    
    
    //anchor date. One good such date is Sunday, 1 Farvardin 1372, which equals 21 March 1993. 
    
    @Test
    public void testPlusWeeks()
    {
        DateTimeUnit dateTimeUnit = new DateTimeUnit( 1382, 1, 20 );

        DateTimeUnit testDateTimeUnit = calendar.plusWeeks( dateTimeUnit, 4 );
        Assert.assertEquals( 1382, testDateTimeUnit.getYear());
        Assert.assertEquals( 2, testDateTimeUnit.getMonth());
        Assert.assertEquals( 17, testDateTimeUnit.getDay());
              
    }
    
    @Test
    public void testPlusMonths()
    {
        DateTimeUnit dateTimeUnit = new DateTimeUnit( 1382, 1, 20 );

        DateTimeUnit testDateTimeUnit = calendar.plusMonths( dateTimeUnit, 4 );
        Assert.assertEquals( 1382, testDateTimeUnit.getYear());
        Assert.assertEquals( 5, testDateTimeUnit.getMonth());
        Assert.assertEquals( 20, testDateTimeUnit.getDay());
           
    }
    
    @Test
    public void testMinusDays()
    {
        DateTimeUnit dateTimeUnit = new DateTimeUnit( 1371, 1, 1 );

        DateTimeUnit testDateTimeUnit = calendar.minusDays( dateTimeUnit, 1 );
        Assert.assertEquals( 1370, testDateTimeUnit.getYear() );
        Assert.assertEquals( 12, testDateTimeUnit.getMonth() );
        Assert.assertEquals( 30, testDateTimeUnit.getDay() );
        
        testDateTimeUnit = calendar.minusDays( dateTimeUnit, 366 );
        Assert.assertEquals( 1370, testDateTimeUnit.getYear() );
        Assert.assertEquals( 1, testDateTimeUnit.getMonth() );
        Assert.assertEquals( 1, testDateTimeUnit.getDay() );  
        
        dateTimeUnit = new DateTimeUnit( 1371, 7, 1 );
        testDateTimeUnit = calendar.minusDays( dateTimeUnit, 1 );
        Assert.assertEquals( 1371, testDateTimeUnit.getYear() );
        Assert.assertEquals( 6, testDateTimeUnit.getMonth() );
        Assert.assertEquals( 31, testDateTimeUnit.getDay() );
        
        dateTimeUnit = new DateTimeUnit( 1371, 8, 1 );
        testDateTimeUnit = calendar.minusDays( dateTimeUnit, 1 );
        Assert.assertEquals( 1371, testDateTimeUnit.getYear() );
        Assert.assertEquals( 7, testDateTimeUnit.getMonth() );
        Assert.assertEquals( 30, testDateTimeUnit.getDay() );            
    }
    
    
    @Test
    public void testMinusWeeks()
    {
        DateTimeUnit dateTimeUnit = new DateTimeUnit( 1382, 1, 10 );

        DateTimeUnit testDateTimeUnit = calendar.minusWeeks( dateTimeUnit, 2 );
        Assert.assertEquals( 1381, testDateTimeUnit.getYear());
        Assert.assertEquals( 12, testDateTimeUnit.getMonth());
        Assert.assertEquals( 25, testDateTimeUnit.getDay());
              
    }
    
    @Test
    public void testMinusMonths()
    {
        DateTimeUnit dateTimeUnit = new DateTimeUnit( 1382, 1, 20 );

        DateTimeUnit testDateTimeUnit = calendar.minusMonths( dateTimeUnit, 1 );
        Assert.assertEquals( 1381, testDateTimeUnit.getYear());
        Assert.assertEquals( 12, testDateTimeUnit.getMonth());
        Assert.assertEquals( 20, testDateTimeUnit.getDay());
           
    }
    
    @Test
    public void testWeekday() 
    {
        Assert.assertEquals( 2, calendar.weekday( new DateTimeUnit( 1372, 1, 2 ) ));
    }
    

}
