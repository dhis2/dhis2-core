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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import javax.annotation.Nonnull;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class CRC32Utils
{
    private CRC32Utils()
    {
        throw new IllegalStateException( "Utility class" );
    }

    /**
     * Calculates a checksum for the given input string.
     *
     * @param input the input char array.
     * @return the checksum.
     */
    public static long generateCRC32Checksum( @Nonnull char[] input )
    {
        Charset charset = StandardCharsets.UTF_8;
        CharBuffer charBuffer = CharBuffer.wrap( input );
        ByteBuffer byteBuffer = charset.encode( charBuffer );
        byte[] bytes = byteBuffer.array();

        CRC32 crc = new CRC32();
        crc.update( bytes, 0, bytes.length );
        return crc.getValue();
    }

    /**
     * Tests whether the given input string generates the same checksum.
     *
     * @param input the input string to checksum.
     * @param checksum the checksum to compare against.
     * @return true if they match.
     */
    public static boolean isMatchingCRC32Checksum( @Nonnull char[] input, @Nonnull char[] checksum )
    {
        long s1 = CRC32Utils.generateCRC32Checksum( input );
        long s2 = Long.parseLong( new String( checksum ) );

        return s1 == s2;
    }

    public static boolean isMatchingCRC32Base62Checksum( char[] input, char[] base62EncodedCRC32Checksum )
    {
        long crc32Checksum = generateCRC32Checksum( input );
        String checksum = Base62.encodeUnsigned32bitToBase62( crc32Checksum );
        return checksum.equals( new String( base62EncodedCRC32Checksum ) );
    }
}
