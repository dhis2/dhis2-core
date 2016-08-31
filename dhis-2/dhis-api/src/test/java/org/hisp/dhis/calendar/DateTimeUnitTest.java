package org.hisp.dhis.calendar;

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

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DateTimeUnitTest
{
    @Test
    public void fromDateTimeTest()
    {
        DateTime dateTime = new DateTime( 2014, 3, 20, 0, 0 );
        DateTimeUnit dateTimeUnit = DateTimeUnit.fromJodaDateTime( dateTime );

        assertEquals( 2014, dateTimeUnit.getYear() );
        assertEquals( 3, dateTimeUnit.getMonth() );
        assertEquals( 20, dateTimeUnit.getDay() );
    }

    @Test
    public void fromJdkDateTest()
    {
        java.util.Calendar cal = new GregorianCalendar( 2014, 2, 20 );
        Date date = cal.getTime();
        DateTimeUnit dateTimeUnit = DateTimeUnit.fromJdkDate( date );

        assertEquals( 2014, dateTimeUnit.getYear() );
        assertEquals( 3, dateTimeUnit.getMonth() );
        assertEquals( 20, dateTimeUnit.getDay() );
    }

    @Test
    public void fromJdkCalendarTest()
    {
        java.util.Calendar cal = new GregorianCalendar( 2014, 2, 20 );
        DateTimeUnit dateTimeUnit = DateTimeUnit.fromJdkCalendar( cal );

        assertEquals( 2014, dateTimeUnit.getYear() );
        assertEquals( 3, dateTimeUnit.getMonth() );
        assertEquals( 20, dateTimeUnit.getDay() );
    }

    @Test
    public void toDateTimeTest()
    {
        DateTimeUnit dateTimeUnit = new DateTimeUnit( 2014, 3, 20, true );
        DateTime dateTime = dateTimeUnit.toJodaDateTime();

        assertEquals( 2014, dateTime.getYear() );
        assertEquals( 3, dateTime.getMonthOfYear() );
        assertEquals( 20, dateTime.getDayOfMonth() );
    }

    @Test
    public void toJdkCalendarTest()
    {
        DateTimeUnit dateTimeUnit = new DateTimeUnit( 2014, 3, 20, true );
        Calendar calendar = dateTimeUnit.toJdkCalendar();

        assertEquals( 2014, calendar.get( Calendar.YEAR ) );
        assertEquals( 2, calendar.get( Calendar.MONTH ) );
        assertEquals( 20, calendar.get( Calendar.DAY_OF_MONTH ) );
    }

    @Test
    public void toJdkDateTest()
    {
        DateTimeUnit dateTimeUnit = new DateTimeUnit( 2014, 3, 20, true );

        assertEquals( 2014, dateTimeUnit.getYear() );
        assertEquals( 3, dateTimeUnit.getMonth() );
        assertEquals( 20, dateTimeUnit.getDay() );
    }

    @Test
    public void defaultTimeZoneTest()
    {
        assertEquals( TimeZone.getDefault(), new DateTimeUnit().getTimeZone() );
    }

    @Test
    public void adjustedTimeZoneTest()
    {
        TimeZone timeZone = TimeZone.getTimeZone( "Asia/Ho_Chi_Minh" ); // UTC/GMT +7.00 hours

        DateTimeUnit dateTimeUnit = new DateTimeUnit( true );
        dateTimeUnit.setDate( 2014, 3, 20 );
        dateTimeUnit.setTime( 14, 0, 0, 0 );
        dateTimeUnit.setTimeZone( timeZone );

        assertEquals( 7, dateTimeUnit.toUtc().getHour() );
    }

    // Test for JT conversion exception:
    // Illegal instant due to time zone offset transition (daylight savings time 'gap'): 1986-01-01T00:00:00.000 (Asia/Kathmandu)
    @Test
    public void illegalInstantGapTest()
    {
        TimeZone timeZone = TimeZone.getTimeZone( "Asia/Kathmandu" );

        DateTimeUnit dateTimeUnit = new DateTimeUnit( true );
        dateTimeUnit.setDate( 1986, 1, 1 );
        dateTimeUnit.setTime( 0, 0, 0, 0 );
        dateTimeUnit.setTimeZone( timeZone );

        dateTimeUnit.toJodaDateTime();
    }
}
