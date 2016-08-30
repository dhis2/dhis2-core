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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DateTimeUnitPeriodTypeParserTest
{
    private PeriodTypeParser format;

    @Before
    public void init()
    {
        format = new DateUnitPeriodTypeParser();
    }

    @Test
    public void testDateUnitFormatParser()
    {
        // daily
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 2, 4, true ), new DateTimeUnit( 2014, 2, 4, true ) ), format.parse( "20140204" ) );

        // weekly
        assertEquals( new DateInterval( new DateTimeUnit( 2013, 12, 30, true ), new DateTimeUnit( 2014, 1, 5, true ) ), format.parse( "2014W1" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 1, 6, true ), new DateTimeUnit( 2014, 1, 12, true ) ), format.parse( "2014W2" ) );

        // monthly
        assertNull( format.parse( "2014W0" ) );
        assertNull( format.parse( "2014W53" ) );
        assertNotNull( format.parse( "2009W53" ) ); // 2009 has 53 weeks
        assertNull( format.parse( "2009W54" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 2, 1, true ), new DateTimeUnit( 2014, 2, 28, true ) ), format.parse( "201402" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 4, 1, true ), new DateTimeUnit( 2014, 4, 30, true ) ), format.parse( "201404" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 3, 1, true ), new DateTimeUnit( 2014, 3, 31, true ) ), format.parse( "2014-03" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 5, 1, true ), new DateTimeUnit( 2014, 5, 31, true ) ), format.parse( "2014-05" ) );

        // bi-monthly
        assertNull( format.parse( "201400B" ) );
        assertNull( format.parse( "201407B" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 1, 1, true ), new DateTimeUnit( 2014, 2, 28, true ) ), format.parse( "201401B" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 3, 1, true ), new DateTimeUnit( 2014, 4, 30, true ) ), format.parse( "201402B" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 5, 1, true ), new DateTimeUnit( 2014, 6, 30, true ) ), format.parse( "201403B" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 7, 1, true ), new DateTimeUnit( 2014, 8, 31, true ) ), format.parse( "201404B" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 9, 1, true ), new DateTimeUnit( 2014, 10, 31, true ) ), format.parse( "201405B" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 11, 1, true ), new DateTimeUnit( 2014, 12, 31, true ) ), format.parse( "201406B" ) );

        // quarter
        assertNull( format.parse( "2014Q0" ) );
        assertNull( format.parse( "2014Q5" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 1, 1, true ), new DateTimeUnit( 2014, 3, 31, true ) ), format.parse( "2014Q1" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 4, 1, true ), new DateTimeUnit( 2014, 6, 30, true ) ), format.parse( "2014Q2" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 7, 1, true ), new DateTimeUnit( 2014, 9, 30, true ) ), format.parse( "2014Q3" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 10, 1, true ), new DateTimeUnit( 2014, 12, 31, true ) ), format.parse( "2014Q4" ) );

        // six-monthly
        assertNull( format.parse( "2014S0" ) );
        assertNull( format.parse( "2014S3" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 1, 1, true ), new DateTimeUnit( 2014, 6, 30, true ) ), format.parse( "2014S1" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 7, 1, true ), new DateTimeUnit( 2014, 12, 31, true ) ), format.parse( "2014S2" ) );

        // six-monthly april
        assertNull( format.parse( "2014AprilS0" ) );
        assertNull( format.parse( "2014AprilS3" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 4, 1, true ), new DateTimeUnit( 2014, 9, 30, true ) ), format.parse( "2014AprilS1" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 10, 1, true ), new DateTimeUnit( 2015, 3, 31, true ) ), format.parse( "2014AprilS2" ) );

        // yearly
        assertEquals( new DateInterval( new DateTimeUnit( 2013, 1, 1, true ), new DateTimeUnit( 2013, 12, 31, true ) ), format.parse( "2013" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 1, 1, true ), new DateTimeUnit( 2014, 12, 31, true ) ), format.parse( "2014" ) );

        // financial april
        assertEquals( new DateInterval( new DateTimeUnit( 2013, 4, 1, true ), new DateTimeUnit( 2014, 3, 31, true ) ), format.parse( "2013April" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 4, 1, true ), new DateTimeUnit( 2015, 3, 31, true ) ), format.parse( "2014April" ) );

        // financial july
        assertEquals( new DateInterval( new DateTimeUnit( 2013, 7, 1, true ), new DateTimeUnit( 2014, 6, 30, true ) ), format.parse( "2013July" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 7, 1, true ), new DateTimeUnit( 2015, 6, 30, true ) ), format.parse( "2014July" ) );

        // financial october
        assertEquals( new DateInterval( new DateTimeUnit( 2013, 10, 1, true ), new DateTimeUnit( 2014, 9, 30, true ) ), format.parse( "2013Oct" ) );
        assertEquals( new DateInterval( new DateTimeUnit( 2014, 10, 1, true ), new DateTimeUnit( 2015, 9, 30, true ) ), format.parse( "2014Oct" ) );
    }
}
