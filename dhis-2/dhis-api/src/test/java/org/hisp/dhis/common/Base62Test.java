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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class Base62Test
{
    @Test
    void encodeCRC32DecimalToBase62()
    {
        String crc32Max64BitLongInBase62 = "AzL8n0Y58m7";
        long max64BitValue = (long) (Math.pow( 2, 64 ) - 1);
        double logBase62Max64 = Math.log( max64BitValue ) / Math.log( 62 );
        int maxCharLen64Max = (int) Math.ceil( logBase62Max64 );
        String encoded64Max = Base62.encodeToBase62( max64BitValue, maxCharLen64Max );
        assertEquals( maxCharLen64Max, encoded64Max.length() );
        assertEquals( encoded64Max, crc32Max64BitLongInBase62 );

        String crc32Max32BitLongInBase62 = "4gfFC3";
        long max32BitValue = (long) (Math.pow( 2, 32 ) - 1);
        double logBase62Max32 = Math.log( max32BitValue ) / Math.log( 62 );
        int maxCharLen32Max = (int) Math.ceil( logBase62Max32 );
        String encoded32Max = Base62.encodeToBase62( max32BitValue, maxCharLen32Max );
        assertEquals( maxCharLen32Max, encoded32Max.length() );
        assertEquals( encoded32Max, crc32Max32BitLongInBase62 );
    }

    @Test
    void encodeMaxLong()
    {
        String maxInBase62 = "AzL8n0Y58m7";
        String encoded = Base62.encodeToBase62( Long.MAX_VALUE, 11 );
        assertEquals( maxInBase62, encoded );
    }

    @Test
    void encodeZero()
    {
        String zeroInBase62PaddedTo6Zeros = "000000";
        String encoded = CRC32Utils.crc32ToBase62( 0L );
        assertEquals( zeroInBase62PaddedTo6Zeros, encoded );
    }
}
