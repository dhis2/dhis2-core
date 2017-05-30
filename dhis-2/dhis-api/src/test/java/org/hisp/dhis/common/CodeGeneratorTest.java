package org.hisp.dhis.common;

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

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author bobj
 */
public class CodeGeneratorTest
{
    @Test
    public void testGetUid()
    {
        int numberOfCodes = 500;

        Set<String> codes = new HashSet<>();
        for ( int n = 0; n < numberOfCodes; ++n )
        {
            String code = CodeGenerator.generateUid();
                        
            // Test syntax
            assertTrue( code.substring( 0, 1 ).matches( "[a-zA-Z]" ) );
            assertTrue( code.matches( "[0-9a-zA-Z]{11}" ) );
            
            // Test uniqueness
            assertTrue( codes.add( code ) );
        }
    }
    
    @Test
    public void testUidIsValid()
    {
        assertTrue( CodeGenerator.isValidUid( "mq4jAnN6fg3" ) );
        assertTrue( CodeGenerator.isValidUid( "QX4LpiTZmUH" ) );
        assertTrue( CodeGenerator.isValidUid( "rT1hdSWjfDC" ) );
        
        assertFalse( CodeGenerator.isValidUid( "1T1hdSWjfDC" ) );
        assertFalse( CodeGenerator.isValidUid( "QX4LpiTZmUHg" ) );
        assertFalse( CodeGenerator.isValidUid( "1T1hdS_WjfD" ) );
    }
    
    @Test
    public void testGetRandomUrlToken()
    {
        assertNotNull( CodeGenerator.getRandomUrlToken() );
        assertNotNull( CodeGenerator.getRandomUrlToken() );
        assertNotNull( CodeGenerator.getRandomUrlToken() );
        assertEquals( 32, CodeGenerator.getRandomUrlToken().length() );
        assertEquals( 32, CodeGenerator.getRandomUrlToken().length() );
        assertEquals( 32, CodeGenerator.getRandomUrlToken().length() );
    }
}
