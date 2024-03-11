package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.Set;
import org.junit.Test;
import com.google.common.collect.Sets;

/**
* @author Lars Helge Overland
*/
public class ProgramIndicatorTest
{
    @Test
    public void testGetIdentifiersEvent()
    {
        String expression = "#{chG8sINMf11.yD5mUKAm3aK} + #{chG8sINMf11.UaGD9u0kaur} - A{y1Bhi6xHtVk}";
        
        Set<String> expected = Sets.newHashSet( "yD5mUKAm3aK", "UaGD9u0kaur", "y1Bhi6xHtVk" );
        
        assertEquals( expected, ProgramIndicator.getDataElementAndAttributeIdentifiers( expression, AnalyticsType.EVENT ) );
    }
    
    @Test
    public void testGetIdentifiersEnrollment()
    {
        String expression = "#{chG8sINMf11.yD5mUKAm3aK} + #{chG8sINMf11.UaGD9u0kaur} - A{y1Bhi6xHtVk}";
        
        Set<String> expected = Sets.newHashSet( "chG8sINMf11_yD5mUKAm3aK", "chG8sINMf11_UaGD9u0kaur", "y1Bhi6xHtVk" );
        
        assertEquals( expected, ProgramIndicator.getDataElementAndAttributeIdentifiers( expression, AnalyticsType.ENROLLMENT ) );
    }
}
