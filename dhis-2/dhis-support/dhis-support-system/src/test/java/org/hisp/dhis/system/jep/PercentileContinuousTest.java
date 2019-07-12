package org.hisp.dhis.system.jep;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jim Grace
 */

public class PercentileContinuousTest
    extends PercentileTest
{
    @Override
    protected PercentileBase getPercentileToTest()
    {
        return new PercentileContinuous();
    }

    @Test
    public void testGetNumberOfParameters()
    {
        assertEquals( 2, getPercentileToTest().getNumberOfParameters() );
    }

    @Test
    public void testRun()
        throws org.nfunk.jep.ParseException
    {
        assertEquals( 1.0, eval( .5, 1d ), DELTA );
        assertEquals( 1.5, eval( .5, 1d, 2d ), DELTA );
        assertEquals( 2.0, eval( .5, 1d, 2d, 3d ), DELTA );
        assertEquals( 2.5, eval( .5, 1d, 2d, 3d, 4d ), DELTA );

        assertEquals( 1.0, eval( 0d, 1d ), DELTA );
        assertEquals( 1.0, eval( 0d, 1d, 2d ), DELTA );
        assertEquals( 1.0, eval( 0d, 1d, 2d, 3d ), DELTA );
        assertEquals( 1.0, eval( 0d, 1d, 2d, 3d, 4d ), DELTA );

        assertEquals( 1.0, eval( 1d, 1d ), DELTA );
        assertEquals( 2.0, eval( 1d, 1d, 2d ), DELTA );
        assertEquals( 3.0, eval( 1d, 1d, 2d, 3d ), DELTA );
        assertEquals( 4.0, eval( 1d, 1d, 2d, 3d, 4d ), DELTA );

        assertEquals( 1.0, eval( 1d/3d, 1d ), DELTA );
        assertEquals( 1d+1d/3d, eval( 1d/3d, 1d, 2d ), DELTA );
        assertEquals( 1d+2d/3d, eval( 1d/3d, 1d, 2d, 3d ), DELTA );
        assertEquals( 2.0, eval( 1d/3d, 1d, 2d, 3d, 4d ), DELTA );
        assertEquals( 2d+1d/3d, eval( 1d/3d, 1d, 2d, 3d, 4d, 5d ), DELTA );
        assertEquals( 2d+2d/3d, eval( 1d/3d, 1d, 2d, 3d, 4d, 5d, 6d ), DELTA );
        assertEquals( 3.0, eval( 1d/3d, 1d, 2d, 3d, 4d, 5d, 6d, 7d ), DELTA );
    }

    @Test( expected = NoValueException.class )
    public void testRunNoData()
        throws org.nfunk.jep.ParseException
    {
        eval( .5 );
    }

    @Test( expected = NoValueException.class )
    public void testRunFractionTooLow()
        throws org.nfunk.jep.ParseException
    {
        eval( -.1, 1d, 2d );
    }

    @Test( expected = NoValueException.class )
    public void testRunFractionTooHigh()
        throws org.nfunk.jep.ParseException
    {
        eval( 1.1, 1d, 2d );
    }
}
