package org.hisp.dhis.mapgeneration;

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

import static org.hisp.dhis.mapgeneration.MapUtils.getWidthHeight;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Lars Helge Overland
 */
public class MapUtilsTest
{    
    @Test
    public void testGetWidthHeight()
    {
        assertEquals( 150, getWidthHeight( 200, 300, 0, 0, 0.5 )[0] );
        assertEquals( 300, getWidthHeight( 200, 300, 0, 0,  0.5 )[1] );
        assertEquals( 200, getWidthHeight( 200, 300, 0, 0,  2 )[0] );
        assertEquals( 100, getWidthHeight( 200, 300, 0, 0,  2 )[1] );
        assertEquals( 300, getWidthHeight( 600, 300, 0, 0,  1d )[0] );
        assertEquals( 300, getWidthHeight( 600, 300, 0, 0,  1d )[1] );

        assertEquals( 200, getWidthHeight( 200, null, 0, 0,  0.5 )[0] );
        assertEquals( 400, getWidthHeight( 200, null, 0, 0,  0.5 )[1] );
        assertEquals( 200, getWidthHeight( 200, null, 0, 0,  2 )[0] );
        assertEquals( 100, getWidthHeight( 200, null, 0, 0,  2 )[1] );

        assertEquals( 150, getWidthHeight( null, 300, 0, 0,  0.5 )[0] );
        assertEquals( 300, getWidthHeight( null, 300, 0, 0,  0.5 )[1] );
        assertEquals( 600, getWidthHeight( null, 300, 0, 0,  2 )[0] );
        assertEquals( 300, getWidthHeight( null, 300, 0, 0,  2 )[1] );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testGetWidthHeightIllegalArgument()
    {
        getWidthHeight( null, null, 0, 0, 0.5 );
    }
    
}
