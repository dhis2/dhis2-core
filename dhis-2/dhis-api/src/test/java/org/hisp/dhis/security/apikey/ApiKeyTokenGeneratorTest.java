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

import org.hisp.dhis.common.Base62;
import org.hisp.dhis.common.CRC32Utils;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
class ApiKeyTokenGeneratorTest
{
    private final static String FIXTURES_PATV1 = """
        d2pat_e0b5Y9bXWtUcfEY8BMHHj9aJZkXCQ7O50174669582
        d2pat_rxIbXljF8Pj5oFRTos4bDmPTnnRu8f4M2723141895
        d2pat_Bp5PJb63ppNCNMQVVbtyS9E6WwoeJOEo2885366845
        d2pat_sKFdO7tjvZlZIFDFYsCk8KmH8sbgSwKy0810651361
        d2pat_kUZDgXG5BbA3xXxgVTsx1SxiIVTRHA653675120217
        d2pat_Wp7YxRQQtejHnpk50vNUmbMWpM16VwOe0751768194
        d2pat_d5jEAFMWLEmgqes6wRJzPI2ZoMSp8rTJ3701533066
        d2pat_aFAQKHM3X7Uc5qpVQVcClogYiCKn6Zme0306540142
        d2pat_V5Z6Sc4K0497APjqs3qtCvmnQtMTb6bS2506225006
        d2pat_k2iCLtSwjXxFFE9OYarDaEAn1BXSdXAw3300780998
        d2pat_IbJmd231Rb0zlr5mQ0ROhxzFG1DirqTH1635466712
        d2pat_tOduz1QO2gzUwcRpkIehx0uCoAyKb59j3466637050
        d2pat_Q4evOUK3G7PhHVafr68KWiGiRcAM5tzd3404585560
        d2pat_tFCPnU6bj8RG42LDDXlOAJC8l3zVGcn82130457278
        d2pat_v6lCfbVkbwG6bku4Pwkx1dYampDZJLyM0098822136
        d2pat_ipIeXZVMnPmMy0GZgNJIZY4BZQOJCUb02442885064
        """;

    private final static String FIXTURES_PATV2 = """
        d2p_iKrAY7sR6rPea1rub181baRzyyui2AW07ymXPRExAgwa051Mei
        d2p_z9T7lpwaWUcvi8yBWt5zvB0LMUfUayHChm7SWGeNrYOJ38vtdY
        d2p_KT73ShoY3UGXmKTN4mLIxBtbDgOz6oB1PLRibZlodP0p32ITYN
        d2p_loOpu9ax0gIfAAc9wO5PavpbNZB0HOHEZzsuGVgcVKZK27MnlW
        d2p_IWMXnDAUjYkoH6zjQlkkmRURZp4qtNQQnRAeWerDv3Yw0wbRlP
        d2p_XM8O2nlzS7XqHrg7LJYl6D2JSLcK9dXaKaELKXLtqs6u10J4sw
        d2p_ISpkoZW4gkZywSSVEqD78AxDppdWoL4OtzutAT4VCAXA1nRKnf
        d2p_OBHMJzUDY7Axa2g552vNs0zddMDU4DddVkKaMX6KYD9E2DnJPY
        d2p_ZBbYnjV8oRi9Tn8FUyMjLee5vceOMJOmFagAuohRyNag2Np2oU
        d2p_nBQWWYIa3kTtBxKgzbnxbx4bO9JaAshdT6XVIu9sIQ0A3SXDad
        d2p_lkhw3UXgceOJN8tm3RLOHHciPRNeTal4D6xfSpYEoaDC0OgCIm
        d2p_lyt5j9WqXaVzJsUhvGJ1w0Ea5GL4jkszazPSWo5HWLFC1QnPCi
        d2p_kgZwKkQOOUhVAUHfaWtpBGMcMgdAGXmpOgn2nttND2xx4GNcWl
        d2p_VEEOPDgPSnQe33g1iJpXnZ4ECX31mzvuDdlYraZDaNPg1j0YLH
        d2p_ncHEvom5vkYYb6Rr2Jg4LbpS8L7khNzk8YDPw1KqZKD54Z8410
        d2p_JWXuAmuC97jQJEaRz1XEz2z1FSDrBvLKVmNj7oQxNLAS4M0lXY
        """;

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

            log.error( "token: " + new String( token ) + " code: " + new String( code ) + " checksum: " + new String(
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

            long crc32 = CRC32Utils.generateCRC32Checksum( code );
            char[] checksum = pair.getChecksum();
            long decode = Base62.decodeBase62( new String( checksum ) );
            assertEquals( decode, crc32 );

            log.info( "token: " + new String( token ) + " code: " + new String( code ) + " checksum: " + new String(
                checksum ) + " decoded: " + decode + " encoded: " + crc32 );

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

        long longChecksum = CRC32Utils.generateCRC32Checksum( code );
        String base62EncodedLongChecksum = Base62.encodeUnsigned32bitToBase62( longChecksum );

        assertEquals( base62EncodedLongChecksum, new String( checksum ) );
    }
}
