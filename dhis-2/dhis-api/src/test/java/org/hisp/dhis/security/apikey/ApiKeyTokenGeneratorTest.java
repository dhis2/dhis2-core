/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.security.apikey;

import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.generatePlainTextToken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.Base62Utils;
import org.hisp.dhis.common.CRC32Utils;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
class ApiKeyTokenGeneratorTest
{
    // @formatter:off
    private final static String FIXTURES_PATV1 = "";
    private final static String FIXTURES_PATV2 = "";

    // @formatter:on

    @Test
    void validatePATV1Fixtures()
    {
        for ( String token : FIXTURES_PATV1.split( "\n" ) )
        {
            ApiTokenType type = ApiTokenType.PERSONAL_ACCESS_TOKEN_V1;
            ApiKeyTokenGenerator.CodeAndChecksum pair = ApiKeyTokenGenerator
                .extractCodeAndChecksum( token.toCharArray() );
            char[] code = pair.getCode();
            assertEquals( type.getLength(), code.length );
            char[] checksum = pair.getChecksum();

            log.info( "token: " + new String( token ) + " code: " + new String( code ) + " checksum: " + new String(
                checksum ) );

            assertEquals( type.getPrefix().length() + "_".length() + code.length + checksum.length, token.length() );

            assertTrue( ApiKeyTokenGenerator.isValidTokenChecksum( token.toCharArray() ) );
        }
    }

    @Test
    void validatePATV2Fixtures()
    {
        for ( String token : FIXTURES_PATV2.split( "\n" ) )
        {
            ApiTokenType type = ApiTokenType.PERSONAL_ACCESS_TOKEN_V2;
            ApiKeyTokenGenerator.CodeAndChecksum pair = ApiKeyTokenGenerator
                .extractCodeAndChecksum( token.toCharArray() );
            char[] code = pair.getCode();
            assertEquals( type.getLength(), code.length );
            char[] checksum = pair.getChecksum();

            log.info( "token: " + new String( token ) + " code: " + new String( code ) + " checksum: " + new String(
                checksum ) );

            assertEquals( type.getPrefix().length() + "_".length() + code.length + checksum.length, token.length() );
            assertTrue( ApiKeyTokenGenerator.isValidTokenChecksum( token.toCharArray() ) );
        }
    }

    @Test
    void testGenerateALotOfPATV1Tokens()
    {
        for ( int i = 0; i < 1000; i++ )
        {
            ApiTokenType type = ApiTokenType.PERSONAL_ACCESS_TOKEN_V1;
            char[] token = generatePlainTextToken( type );

            ApiKeyTokenGenerator.CodeAndChecksum pair = ApiKeyTokenGenerator.extractCodeAndChecksum( token );
            char[] code = pair.getCode();
            assertEquals( type.getLength(), code.length );
            char[] checksum = pair.getChecksum();

            log.info( "token: " + new String( token ) + " code: " + new String( code ) + " checksum: " + new String(
                checksum ) );

            assertEquals( type.getPrefix().length() + "_".length() + code.length + checksum.length, token.length );
            assertTrue( ApiKeyTokenGenerator.isValidTokenChecksum( token ) );
        }
    }

    @Test
    void testGenerateALotOfPATV2Tokens()
    {
        for ( int i = 0; i < 1000; i++ )
        {
            ApiTokenType type = ApiTokenType.PERSONAL_ACCESS_TOKEN_V2;
            char[] token = generatePlainTextToken( type );

            ApiKeyTokenGenerator.CodeAndChecksum pair = ApiKeyTokenGenerator.extractCodeAndChecksum( token );
            char[] code = pair.getCode();
            assertEquals( type.getLength(), code.length );

            char[] checksum = pair.getChecksum();

            log.info( "token: " + new String( token ) + " code: " + new String( code ) + " checksum: " + new String(
                checksum ) );

            assertEquals( type.getPrefix().length() + "_".length() + code.length + checksum.length, token.length );
            assertTrue( ApiKeyTokenGenerator.isValidTokenChecksum( token ) );
        }
    }

    @Test
    void testGeneratePersonalAccessTokensV2CheckChecksumMatch()
    {
        char[] token = generatePlainTextToken( ApiTokenType.PERSONAL_ACCESS_TOKEN_V2 );

        ApiKeyTokenGenerator.CodeAndChecksum pair = ApiKeyTokenGenerator.extractCodeAndChecksum( token );
        char[] code = pair.getCode();
        char[] checksum = pair.getChecksum();

        long longChecksum = CRC32Utils.generateCrc32Checksum( code );
        String base62EncodedLongChecksum = Base62Utils.encodeCRC32IntoBase62( longChecksum );

        assertEquals( base62EncodedLongChecksum, new String( checksum ) );
    }
}
