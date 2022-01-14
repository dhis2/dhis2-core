/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Tests valid XRequestID header validation of
 * {@link RequestInfo#isValidXRequestID(String)}
 *
 * @author Jan Bernitt
 */
class RequestInfoTest
{
    @Test
    void testUidIsValidXRequestID()
    {
        assertValid( CodeGenerator.generateUid() );
    }

    @Test
    void testUUIDIsValidXRequestID()
    {
        assertValid( UUID.randomUUID().toString() );
    }

    @Test
    void testLongStringIsInvalidXRequestID()
    {
        assertInvalid( "1234567890123456789012345678901234567890" );
    }

    @Test
    void testQuoteStringIsInvalidXRequestID()
    {
        assertInvalid( "'now-I-escaped" );
        assertInvalid( "\"now-I-escaped" );
    }

    @Test
    void testSpaceStringIsInvalidXRequestID()
    {
        assertInvalid( "no - not having it" );
    }

    private static void assertValid( String xRequestID )
    {
        assertTrue( RequestInfo.isValidXRequestID( xRequestID ),
            "Should be a valid ID but is not: " + xRequestID );
    }

    private static void assertInvalid( String xRequestID )
    {
        assertFalse( RequestInfo.isValidXRequestID( xRequestID ),
            "Should be an invalid ID but is valid: " + xRequestID );
    }
}
