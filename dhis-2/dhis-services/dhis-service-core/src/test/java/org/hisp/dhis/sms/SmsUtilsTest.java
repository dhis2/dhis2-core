package org.hisp.dhis.sms;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.system.util.SmsUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Zubair Asghar
 */
public class SmsUtilsTest
{
    @Test
    public void testSMSTextEncoding()
    {
        assertEquals( "Hi+User", SmsUtils.encode( "Hi User" ) );
        assertEquals( "Jeg+er+p%C3%A5+universitetet", SmsUtils.encode( "Jeg er på universitetet" ) );
        assertEquals( "endelig+oppn%C3%A5+m%C3%A5let", SmsUtils.encode( "endelig oppnå målet" ) );
        assertEquals( "%D8%B4%D9%83%D8%B1%D8%A7+%D9%84%D9%83%D9%85", SmsUtils.encode( "شكرا لكم" ) );
        assertEquals( " ", SmsUtils.encode( " " ) );
        assertNull( SmsUtils.encode( null ) );
    }

    @Test
    public void testRemovePhoneNumberPrefix()
    {
        assertEquals( "4740123456", SmsUtils.removePhoneNumberPrefix( "004740123456" ) );
        assertEquals( "4740123456", SmsUtils.removePhoneNumberPrefix( "+4740123456" ) );
    }

    @Test
    public void testBase64Compression()
    {
        assertTrue( SmsUtils.isBase64( "c2FtcGxlIHNtcyB0ZXh0" ) );
        assertFalse( SmsUtils.isBase64( "sample sms text" ) );
    }
}
