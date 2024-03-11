package org.hisp.dhis.security.acl;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.security.acl.AccessStringHelper;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AccessStringHelperTest
{
    @Test
    public void testCanRead()
    {
        String access = "r-------";
        String access_will_fail = "--------";

        assertTrue( AccessStringHelper.canRead( access ) );
        assertFalse( AccessStringHelper.canRead( access_will_fail ) );
    }

    @Test
    public void testCanWrite()
    {
        String access1 = "rw------";
        String access2 = "-w------";
        String access_will_fail = "--------";

        assertTrue( AccessStringHelper.canWrite( access1 ) );
        assertTrue( AccessStringHelper.canWrite( access2 ) );
        assertFalse( AccessStringHelper.canWrite( access_will_fail ) );
    }

    @Test
    public void staticRead()
    {
        assertTrue( AccessStringHelper.canRead( AccessStringHelper.READ ) );
        assertFalse( AccessStringHelper.canWrite( AccessStringHelper.READ ) );
    }

    @Test
    public void staticWrite()
    {
        assertFalse( AccessStringHelper.canRead( AccessStringHelper.WRITE ) );
        assertTrue( AccessStringHelper.canWrite( AccessStringHelper.WRITE ) );
    }

    @Test
    public void staticReadWrite()
    {
        assertTrue( AccessStringHelper.canRead( AccessStringHelper.READ_WRITE ) );
        assertTrue( AccessStringHelper.canWrite( AccessStringHelper.READ_WRITE ) );
    }
}
