package org.hisp.dhis.dxf2.adx;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bobj
 */
public class ADXPeriodTest
{
    private Period period;

    public ADXPeriodTest()
    {
    }

    @Test
    public void testParser()
    {
        try 
        {
            period = ADXPeriod.parse( "2015-01-01/P1Y" );
            assertEquals( "2015", period.getIsoDate() );
            period = ADXPeriod.parse( "2015-01-01/P1M" );
            assertEquals( "201501", period.getIsoDate() );
            period = ADXPeriod.parse( "2015-01-01/P1D" );
            assertEquals( "20150101", period.getIsoDate() );
            period = ADXPeriod.parse( "2015-01-01/P1Q" );
            assertEquals( "2015Q1", period.getIsoDate() );
            period = ADXPeriod.parse( "2015-04-01/P1Q" );
            assertEquals( "2015Q2", period.getIsoDate() );
            period = ADXPeriod.parse( "2015-01-01/P7D" );
            assertEquals( "2015W1", period.getIsoDate() );
            period = ADXPeriod.parse( "2015-01-05/P7D" );
            assertEquals( "2015W2", period.getIsoDate() );
        }
        catch (ADXException ex)
        {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testBadDuration()
    {
        try
        {
            period = ADXPeriod.parse( "2014-01-01/P1" );
            fail( "Should have thrown exception parsing 2015-01-01/P1" );
        } 
        catch ( Exception ex )
        {
            assertEquals( ADXException.class, ex.getClass() );
        }
    }

    @Test
    public void testFinancialTypes()
    {
        try
        {
            period = ADXPeriod.parse( "2015-01-01/P1Y" );
            assertEquals( "2015", period.getIsoDate() );
            period = ADXPeriod.parse( "2015-04-01/P1Y" );
            assertEquals( "2015April", period.getIsoDate() );
            period = ADXPeriod.parse( "2015-07-01/P1Y" );
            assertEquals( "2015July", period.getIsoDate() );
            period = ADXPeriod.parse( "2015-10-01/P1Y" );
            assertEquals( "2015Oct", period.getIsoDate() );
        }
        catch (ADXException ex)
        {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testFailFinancialType()
    {
        try
        {
            period = ADXPeriod.parse( "2014-02-01/P1Y" );
            fail( "Should have thrown exception parsing 2014-02-01/P1Y" );
        } 
        catch ( Exception ex )
        {
            assertEquals( ADXException.class, ex.getClass() );
        }
    }

    @Test
    public void testSerialize()
    {
        period = PeriodType.getPeriodFromIsoString( "2015" );
        assertEquals( "2015-01-01/P1Y", ADXPeriod.serialize( period ) );
        period = PeriodType.getPeriodFromIsoString( "201503" );
        assertEquals( "2015-03-01/P1M", ADXPeriod.serialize( period ) );
        period = PeriodType.getPeriodFromIsoString( "2015W1" );
        assertEquals( "2014-12-29/P7D", ADXPeriod.serialize( period ) );
        period = PeriodType.getPeriodFromIsoString( "2015Q2" );
        assertEquals( "2015-04-01/P1Q", ADXPeriod.serialize( period ) );
        period = PeriodType.getPeriodFromIsoString( "2015April" );
        assertEquals( "2015-04-01/P1Y", ADXPeriod.serialize( period ) );
        period = PeriodType.getPeriodFromIsoString( "2015S2" );
        assertEquals( "2015-07-01/P6M", ADXPeriod.serialize( period ) );
        period = PeriodType.getPeriodFromIsoString( "2015AprilS2" );
        assertEquals( "2015-10-01/P6M", ADXPeriod.serialize( period ) );
    }
}
