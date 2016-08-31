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

import static org.hisp.dhis.system.util.GeoUtils.getBoxShape;
import static org.hisp.dhis.system.util.GeoUtils.replaceUnsafeSvgText;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Lars Helge Overland
 */
public class GeoUtilsTest
{
    private static final double DELTA = 0.01;
    
    @Test
    public void testGetBoxShape()
    {
        // Equator
        
        double[] box = getBoxShape( 0, 0, 110574.27 );
        
        assertEquals( 1d, box[0], DELTA );
        assertEquals( 1d, box[1], DELTA );
        assertEquals( -1d, box[2], DELTA );
        assertEquals( -1d, box[3], DELTA );
        
        // Punta Arenas

        box = getBoxShape( -71, -53, 67137.20 );

        assertEquals( -52.4, box[0], DELTA );
        assertEquals( -70d, box[1], DELTA );
        assertEquals( -53.6, box[2], DELTA );
        assertEquals( -72d, box[3], DELTA );        
    }
    
    @Test
    public void testReplaceUnsafeSvgText()
    {
        String text = 
            "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
            "<text id=\"ext-sprite-1866\" zIndex=\"500\" text=\"Measles Coverage <1y\" font=\"bold 18px Arial,Sans-serif,Lucida Grande\" hidden=\"false\">" +
            "<text id=\"ext-sprite-1866\" zIndex=\"500\" text=\"BCG & DPT Coverage\" font=\"bold 18px Arial,Sans-serif,Lucida Grande\" hidden=\"false\">" +
            "</svg>";

        String expected = 
            "<svg xmlns=\"http://www.w3.org/2000/svg\">" +
            "<text id=\"ext-sprite-1866\" zIndex=\"500\" text=\"Measles Coverage 1y\" hidden=\"false\">" +
            "<text id=\"ext-sprite-1866\" zIndex=\"500\" text=\"BCG  DPT Coverage\" hidden=\"false\">" +
            "</svg>";
        
        String actual = replaceUnsafeSvgText( text );
        
        assertEquals( expected, actual );
    }
}
