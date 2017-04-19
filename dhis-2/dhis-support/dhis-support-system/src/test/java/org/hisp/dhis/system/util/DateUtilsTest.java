package org.hisp.dhis.system.util;

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

import com.google.common.collect.Sets;
import org.hisp.dhis.calendar.impl.NepaliCalendar;
import org.joda.time.DateTime;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.hisp.dhis.system.util.DateUtils.dateIsValid;
import static org.hisp.dhis.system.util.DateUtils.dateTimeIsValid;
import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class DateUtilsTest
{
    @Test
    public void testDateIsValid()
    {
        assertTrue( dateIsValid( "2000-01-01" ) );
        assertTrue( dateIsValid( "1067-04-28" ) );
        assertFalse( dateIsValid( "07-07-2000" ) );
        assertFalse( dateIsValid( "2000-03-40" ) );
        assertFalse( dateIsValid( "20d20-03-01" ) );
        assertTrue( dateIsValid( "2014-01-01" ) );
        assertFalse( dateIsValid( "2014-12-33" ) );
        assertFalse( dateIsValid( "2014-13-32" ) );
        assertFalse( dateIsValid( "2014-ab-cd" ) );
        assertFalse( dateIsValid( "201-01-01" ) );
        assertFalse( dateIsValid( "01-01-01" ) );
        assertFalse( dateIsValid( "abcd-01-01" ) );
        assertFalse( dateIsValid( "2017-04-31" ) );
        assertFalse( dateIsValid( "2017-04-32" ) );
        assertFalse( dateIsValid( "2016-09-31" ) );

        assertTrue( dateIsValid( NepaliCalendar.getInstance(), "2074-04-32" ) );
        assertFalse( dateIsValid( NepaliCalendar.getInstance(), "2074-03-32" ) );
        assertFalse( dateIsValid( NepaliCalendar.getInstance(), "2074-04-33" ) );
    }

    @Test
    public void testDateTimeIsValid()
    {
        assertTrue( dateTimeIsValid( "2000-01-01T10:00:00.000Z" ) );
        assertTrue( dateTimeIsValid( "2000-01-01T10:00:00.000+05:30" ) );
        assertTrue( dateTimeIsValid( "2000-01-01T10:00:00Z" ) );
        assertTrue( dateTimeIsValid( "2000-01-01T10:00:00+05:30" ) );
        assertTrue( dateTimeIsValid( "2000-01-01T10:00:00" ) );
        assertTrue( dateTimeIsValid( "2000-01-01T10:00Z" ) );
        assertTrue( dateTimeIsValid( "2000-01-01T10:00+05:30" ) );
        assertTrue( dateTimeIsValid( "2000-01-01T10:00" ) );
        assertFalse( dateTimeIsValid( "2000-01-01" ) );
        assertFalse( dateTimeIsValid( "01-01-2000" ) );
        assertFalse( dateTimeIsValid( "abcd" ) );
    }

    @Test
    public void testDaysBetween()
    {
        assertEquals( 6, DateUtils.daysBetween( new DateTime( 2014, 3, 1, 0, 0 ).toDate(), new DateTime( 2014, 3, 7, 0, 0 ).toDate() ) );
    }

    @Test
    public void testMax()
    {
        Date date1 = new DateTime( 2014, 5, 15, 3, 3 ).toDate();
        Date date2 = new DateTime( 2014, 5, 18, 1, 1 ).toDate();
        Date date3 = null;
        Date date4 = null;

        assertEquals( date2, DateUtils.max( date1, date2 ) );
        assertEquals( date2, DateUtils.max( date2, date1 ) );
        assertEquals( date1, DateUtils.max( date1, date3 ) );
        assertEquals( date1, DateUtils.max( date3, date1 ) );

        assertNull( DateUtils.max( date3, date4 ) );
    }

    @Test
    public void testMaxCollection()
    {
        Date date1 = new DateTime( 2014, 5, 15, 3, 3 ).toDate();
        Date date2 = new DateTime( 2014, 5, 18, 1, 1 ).toDate();
        Date date3 = new DateTime( 2014, 6, 10, 1, 1 ).toDate();
        Date date4 = null;
        Date date5 = null;
        Date date6 = null;

        assertEquals( date2, DateUtils.max( Sets.newHashSet( date1, date2, date4 ) ) );
        assertEquals( date2, DateUtils.max( Sets.newHashSet( date2, date1, date4 ) ) );
        assertEquals( date3, DateUtils.max( Sets.newHashSet( date1, date2, date3 ) ) );
        assertEquals( date3, DateUtils.max( Sets.newHashSet( date1, date2, date3 ) ) );
        assertEquals( date3, DateUtils.max( Sets.newHashSet( date3, date4, date5 ) ) );
        assertEquals( null, DateUtils.max( Sets.newHashSet( date4, date5, date6 ) ) );
        assertEquals( date1, DateUtils.max( Sets.newHashSet( date1, date5, date4 ) ) );

        assertNull( DateUtils.max( Sets.newHashSet( date4, date5, date6 ) ) );
    }

    @Test
    public void testMin()
    {
        Date date1 = new DateTime( 2014, 5, 15, 3, 3 ).toDate();
        Date date2 = new DateTime( 2014, 5, 18, 1, 1 ).toDate();
        Date date3 = null;
        Date date4 = null;

        assertEquals( date1, DateUtils.min( date1, date2 ) );
        assertEquals( date1, DateUtils.min( date2, date1 ) );
        assertEquals( date1, DateUtils.min( date1, date3 ) );
        assertEquals( date1, DateUtils.min( date3, date1 ) );

        assertNull( DateUtils.max( date3, date4 ) );
    }

    @Test
    public void testMinCollection()
    {
        Date date1 = new DateTime( 2014, 5, 15, 3, 3 ).toDate();
        Date date2 = new DateTime( 2014, 5, 18, 1, 1 ).toDate();
        Date date3 = new DateTime( 2014, 6, 10, 1, 1 ).toDate();
        Date date4 = null;
        Date date5 = null;
        Date date6 = null;

        assertEquals( date1, DateUtils.min( Sets.newHashSet( date1, date2, date4 ) ) );
        assertEquals( date1, DateUtils.min( Sets.newHashSet( date2, date1, date4 ) ) );
        assertEquals( date1, DateUtils.min( Sets.newHashSet( date1, date2, date3 ) ) );
        assertEquals( date1, DateUtils.min( Sets.newHashSet( date1, date2, date3 ) ) );
        assertEquals( date3, DateUtils.min( Sets.newHashSet( date3, date4, date5 ) ) );
        assertEquals( null, DateUtils.min( Sets.newHashSet( date4, date5, date6 ) ) );
        assertEquals( date1, DateUtils.min( Sets.newHashSet( date1, date5, date4 ) ) );

        assertNull( DateUtils.max( Sets.newHashSet( date4, date5, date6 ) ) );
    }

    @Test
    public void testGetPrettyInterval()
    {
        Date start = new DateTime( 2014, 5, 18, 15, 10, 5, 12 ).toDate();
        Date end = new DateTime( 2014, 5, 19, 11, 45, 42, 56 ).toDate();

        String interval = DateUtils.getPrettyInterval( start, end );

        assertNotNull( interval );
    }

    @Test
    public void testGetMediumDate()
    {
        assertEquals( new DateTime( 2014, 5, 18, 0, 0, 0, 0 ).toDate(), DateUtils.getMediumDate( "2014-05-18" ) );
        assertEquals( new DateTime( 2015, 11, 3, 0, 0, 0, 0 ).toDate(), DateUtils.getMediumDate( "2015-11-03" ) );

        assertNull( DateUtils.getMediumDate( null ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetInvalidMediumDate()
    {
        DateUtils.getMediumDate( "StringWhichIsNotADate" );
    }

    @Test
    public void testGetMediumDateString()
    {
        Date date = new DateTime( 2014, 5, 18, 15, 10, 5, 12 ).toDate();

        assertEquals( "2014-05-18", DateUtils.getMediumDateString( date ) );
        assertNull( DateUtils.getMediumDateString( null ) );
    }

    @Test
    public void testGetLongDateString()
    {
        Date date = new DateTime( 2014, 5, 18, 15, 10, 5, 12 ).toDate();

        assertEquals( "2014-05-18T15:10:05", DateUtils.getLongDateString( date ) );
        assertNull( DateUtils.getLongDateString( null ) );
    }

    @Test
    public void testGetHttpDateString()
    {
        Date date = new DateTime( 2014, 5, 18, 15, 10, 5, 12 ).toDate();

        assertEquals( "Sun, 18 May 2014 15:10:05 GMT", DateUtils.getHttpDateString( date ) );
        assertNull( DateUtils.getLongDateString( null ) );
    }

    @Test
    public void testGetDuration()
    {
        Duration s50 = DateUtils.getDuration( "50s" );
        Duration m20 = DateUtils.getDuration( "20m" );
        Duration h2 = DateUtils.getDuration( "2h" );
        Duration d14 = DateUtils.getDuration( "14d" );

        assertEquals( 50, s50.getSeconds() );
        assertEquals( 1200, m20.getSeconds() );
        assertEquals( 7200, h2.getSeconds() );
        assertEquals( 1209600, d14.getSeconds() );

        assertNull( DateUtils.getDuration( "123x" ) );
        assertNull( DateUtils.getDuration( "1y" ) );
        assertNull( DateUtils.getDuration( "10ddd" ) );
    }

    @Test
    public void getDate()
    {
        LocalDateTime time = LocalDateTime.of( 2012, 1, 10, 10, 5 );

        Date date = DateUtils.getDate( time );

        assertEquals( time.toInstant( ZoneOffset.UTC ).toEpochMilli(), date.getTime() );
    }
}
