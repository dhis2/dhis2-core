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

import java.util.Arrays;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class Base62
{
    private Base62()
    {
        throw new IllegalStateException( "Utility class" );
    }

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Encodes a number to a Base62 encoded string.
     *
     * @param num the number to encode, must be positive.
     * @param padding the length of the encoded string, will be padded with
     *        zeros prefixed to the string if the encoded string is shorter.
     * @return the Base62 encoded string
     */
    public static String encodeToBase62( long num, int padding )
    {
        if ( num < 0 )
        {
            throw new IllegalArgumentException( "Number must be positive" );
        }
        if ( padding < 0 )
        {
            throw new IllegalArgumentException( "Padding must greater than one" );
        }

        int base = BASE62_ALPHABET.length();
        char[] chars = new char[padding];
        Arrays.fill( chars, '0' );

        long r;
        for ( int i = padding - 1; i >= 0; i-- )
        {
            r = num % base;
            chars[i] = BASE62_ALPHABET.charAt( (int) r );
            num -= r;
            num = num / base;
        }

        return new String( chars );
    }

    /**
     * Decodes a Base62 encoded string to a number.
     *
     * @param str the Base62 encoded string
     * @return the decoded number
     */
    public static long decodeBase62( final String str )
    {
        int base = BASE62_ALPHABET.length();
        long num = 0;
        for ( char c : str.toCharArray() )
        {
            num = num * base;
            int index = BASE62_ALPHABET.indexOf( c );
            if ( index == -1 )
            {
                throw new IllegalArgumentException( "Invalid character for Base62: " + c );
            }
            num = num + index;
        }
        return num;
    }

}
