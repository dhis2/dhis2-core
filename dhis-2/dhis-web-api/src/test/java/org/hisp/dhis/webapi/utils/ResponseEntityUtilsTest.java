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
package org.hisp.dhis.webapi.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.net.HttpHeaders;

@ExtendWith( MockitoExtension.class )
class ResponseEntityUtilsTest
{
    @Mock
    private HttpServletRequest request;

    @Test
    void testcheckNotModifiedEtag()
    {
        when( request.getHeader( matches( HttpHeaders.IF_NONE_MATCH ) ) )
            .thenReturn( "aa9108b4a9b5553cdd160526cdc" );

        assertTrue( ResponseEntityUtils.checkNotModified( "aa9108b4a9b5553cdd160526cdc", request ) );
        assertTrue( ResponseEntityUtils.checkNotModified( "\"aa9108b4a9b5553cdd160526cdc\"", request ) );
        assertTrue( ResponseEntityUtils.checkNotModified( "W/\"aa9108b4a9b5553cdd160526cdc\"", request ) );
        assertTrue( ResponseEntityUtils.checkNotModified( "W/aa9108b4a9b5553cdd160526cdc", request ) );
        assertTrue( ResponseEntityUtils.checkNotModified( "  W/\"aa9108b4a9b5553cdd160526cdc\"", request ) );
        assertTrue( ResponseEntityUtils.checkNotModified( "  aa9108b4a9b5553cdd160526cdc", request ) );
        assertTrue( ResponseEntityUtils.checkNotModified( "  W/aa9108b4a9b5553cdd160526cdc", request ) );
        assertTrue( ResponseEntityUtils.checkNotModified( "aa9108b4a9b5553cdd160526cdc  ", request ) );
        assertTrue( ResponseEntityUtils.checkNotModified( "aa9108b4a9b5553cdd160526cdc  ", request ) );
        assertTrue( ResponseEntityUtils.checkNotModified( " aa9108b4a9b5553cdd160526cdc  ", request ) );
        assertTrue( ResponseEntityUtils.checkNotModified( " \"aa9108b4a9b5553cdd160526cdc\"  ", request ) );

        assertFalse( ResponseEntityUtils.checkNotModified( "b56ygt9ikj68764419gkh73k9g2", request ) );
        assertFalse( ResponseEntityUtils.checkNotModified( "Wb56ygt9ikj68764419gkh73k9g2", request ) );
        assertFalse( ResponseEntityUtils.checkNotModified( "W/   \"aa9108b4a9b5553cdd160526cdc\"", request ) );
    }

    @Test
    void testcheckNotModifiedIfNoneMatch()
    {
        when( request.getHeader( matches( HttpHeaders.IF_NONE_MATCH ) ) )
            .thenReturn( "b56ygt9ikj68764419gkh73k9g2" );
        assertTrue( ResponseEntityUtils.checkNotModified( "b56ygt9ikj68764419gkh73k9g2", request ) );

        when( request.getHeader( matches( HttpHeaders.IF_NONE_MATCH ) ) )
            .thenReturn( "W/b56ygt9ikj68764419gkh73k9g2" );
        assertTrue( ResponseEntityUtils.checkNotModified( "b56ygt9ikj68764419gkh73k9g2", request ) );

        when( request.getHeader( matches( HttpHeaders.IF_NONE_MATCH ) ) )
            .thenReturn( "cht61hgy3m9sjg6whg135jt5jqw" );
        assertFalse( ResponseEntityUtils.checkNotModified( "b56ygt9ikj68764419gkh73k9g2", request ) );

        when( request.getHeader( matches( HttpHeaders.IF_NONE_MATCH ) ) )
            .thenReturn( "W/  b56ygt9ikj68764419gkh73k9g2" );
        assertFalse( ResponseEntityUtils.checkNotModified( "b56ygt9ikj68764419gkh73k9g2", request ) );
    }
}
