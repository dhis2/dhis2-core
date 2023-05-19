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
    private final static String FIXTURES_PATV1 =
        """
            d2pat_SEKjz1g0XfhzmfXW1O8hdOxDdTOVb7wd3919418512
            d2pat_Z2AXKatPmhPOGgluXSeKIqBCoF2nufXo3567589196
            d2pat_LQq5ZwEcnml6IBJ6U0kmTiKq2xNSi1rA0099848045
            d2pat_nKCKRUdPT2DEh0aM1L20cll2SgdSLB3D4250897399
            d2pat_KN0tR3A5tjnQwYg4DBSWWdZAYpS3EbyN1641216922
            d2pat_vCiDCp0RABwgORidPz8N0bLL215Q57Qj3985694640
            d2pat_Rc1BRt50QIkN95jEqDtNFbFDZszlrdD41584410927
            d2pat_QcXbPTobIH9PqKVxBRrq7GOmIVcaYMDz2209240277
            d2pat_v65eHjPMZNeMLa36CIn9npSYsajDeAQk0663131245
            d2pat_I4NbzOFWYUVmnPRo07qkoVwUD8E1XBG41994959940
            d2pat_XjefzGAHmwtyYcx2hmsLrqRDFGQtWiec2670173241
            d2pat_EWA0mcGe0DUCwld7pcYFKzL3kfhoR48P0011742236
            d2pat_mk313UlzxvAAbl1cqSrbn9zXYZp5NgQa1997077242
            d2pat_Vl7d0LpTr6EeFpAUqvnuSvrEWSaKflBP0968065654
            d2pat_BXlATC3A6b3RjiopvibAGH130gQhpgn71127336113
            d2pat_bMSiwb9qr0S8Z9BupK3aJpQ35O5GrQAj0356242118
            d2pat_vtOA5AUBOr1mu2Uw2N4AzUJ7qCleWqxM4097853127
            d2pat_gddY7LJlsRiZR2tpRO5OFGCOBvKayRE33382655301
            d2pat_gT96ccS7ZG31FjBBR01drCrThh6RmSeD3293066640
            d2pat_K8hRmGSY3mZf8K0hGtjYb7uO1ei2ysXa0608201155
            d2pat_LQ2dASpO6gLk8ct46PH3voiVBtMAqLdT3931323390
            d2pat_TttEpWwfiGgooUVtHlutChPNRkfM74HC1767656965
            d2pat_riTi3L6Lfw4ZsK5rSwpPEW0skLO8abvv2947404266
            d2pat_cDcJ5FAISJqru3g8FQLSHus2lVBhwq7u2394822806
            d2pat_oGj1XJY3BMcpdHWLdHpsFTRP3xfPy3IB0301520836
             """;

    private final static String FIXTURES_PATV2 =
        """
            d2p_s7JKA5ASR2q27J1ahp7YlxfaIY0Mb5uBlcsgdlfjuze81FZfF5
            d2p_JQpZuZMvaGpBCBE8TE5XZfh260q1KQBb3gdT3nOGqdVm2X2qw1
            d2p_bgUeERTUx9hkH9XCWqnF2TfEN7DV4FKeIvZv5uHeaYpd4UcL7V
            d2p_hrFpCRIqJqoFkjwfHFytao0Hl2MaEath4hxsTGJTADcz4ffW73
            d2p_lgBUfm773XzgZg8ckK5zW254KI3qfCFDhLs3JvT0r4OY2aKlxh
            d2p_tdBFx2Z3cUzGpzrzqWNe2eNKqAbLDqiPgMLhDPKoaOv34OU5gI
            d2p_bXbuZxcfaYmICBPYiTA9fKFewuMpkvkG5UW75IU6PcKf3fGZjK
            d2p_P4DCnBV7U3o6lZTDLgUS6cYpotM4GDs2qdydyPvSPOKG0dcnmT
            d2p_xx3XIx3FVVQauWr0wRVuz7usUujS50ZhTV4NRM2AqJAP1GIgQX
            d2p_slHKz1NiPnmXkMXdTeEd5GPkkRFv8bUYffoOhAqJHBIW0Jr4ZK
            d2p_HKUcIGpeZMTdNWjMow7ZpGt0eUdvDgNcdngbkwetK1ut42ME2V
            d2p_ePdO9bv21xWXCJgd8LRzDFOwcqRIby9OC6uKqjIcBwng0bsqmE
            d2p_rJkUS8vjtywQE9W1iSfkQNR5MxjBehKWFeFDkudjWmVZ3Alnon
            d2p_DMkqzpxxYJBPCJ0nTcBIfYaA8ocA9K4WqRqxs0pAMS2806dmtz
            d2p_TsrGMWnDg6iNlDkZ7JX1QrDfJDMH3lL95BioqlOymW3N39mroo
            d2p_sIgJAIfqX017AVKLhMie10DfLAlKXhpS2FSzrTxVZ7Dq3t8BIJ
            d2p_PxKVGohRX1wZuEtGnGn9DbgbbO1VD52v4larpfHAe7QT1IJ0kc
            d2p_YZouAiLL0RysXtV3qVoI4VJxXhEYVhfscOxuXjPNXYCJ3mfF4E
            d2p_U4ZOonQjuwYtwN0k1jcDpMWAgUoPANJLTOWJ7QYLFzjY2h0dLF
            d2p_FQFnUfa9wG90D9RGj2t4cmNoo4GAZiWrtOH6WG1BR7MF4V0XEm
            d2p_rmJcBwDSswhiYLZzKYivMQPJp7GdlJqWb7mVkGsEzkgl1BEO8P
            d2p_S6p8w67EpNTWz8HSnmO7mV1kSRQucuXM68qVDRMzdy9K1taiIa
            d2p_Hynyp60zBJi2lJMTl9YHSuhCU04THTJJjs2Wt0wJRQat1LGrpI
            d2p_ZYsn9JB1CDLEpaqUDr1S9U0l3s08HCaLkpf7ngQrLAqi3zmZBi
            d2p_YMQhjAQMDvviHjvUrgIQSRWaGNDc3tVwf63Jfk3DdQsA2wo8HL
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
