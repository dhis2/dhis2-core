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
package org.hisp.dhis.common;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class Base62Utils
{
    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Encodes a CRC32 checksum in decimal into base-62.
     * <p>
     * The CRC32 checksum is 32 bits. To calculate the padding length, we do:
     * log_62(2^32 - 1) = 5.3743 ≈ 6
     * <p>
     * The encoded string is padded with leading zeros to a length of 6
     * characters to make the result string uniform.
     *
     * @param crc32 the CRC32 checksum to encode.
     * @return the encoded string.
     */
    public static String encodeCRC32IntoBase62( long crc32 )
    {
        return encodeIntoBase62( crc32, 6 );
    }

    private static String encodeIntoBase62( long numberToEncode, int padding )
    {
        int base = BASE62_ALPHABET.length();
        List<Character> encodedCharacters = new ArrayList<>();

        long remainder;
        while ( numberToEncode > 0 )
        {
            remainder = numberToEncode % base;
            encodedCharacters.add( 0, BASE62_ALPHABET.charAt( (int) remainder ) );
            numberToEncode -= remainder;
            numberToEncode = numberToEncode / base;
        }

        StringBuilder encodedStringBuilder = new StringBuilder();
        for ( char character : encodedCharacters )
        {
            encodedStringBuilder.append( character );
        }

        while ( encodedStringBuilder.length() < padding )
        {
            encodedStringBuilder.insert( 0, BASE62_ALPHABET.charAt( 0 ) );
        }

        return encodedStringBuilder.toString();
    }

    public static boolean isMatchingCrc32B62Checksum( char[] inputCode, String base62EncodedCRC32Checksum )
    {
        String checksum = encodeCRC32IntoBase62( CRC32Utils.generateCrc32Checksum( inputCode ) );
        return checksum.equals( base62EncodedCRC32Checksum );
    }
}
