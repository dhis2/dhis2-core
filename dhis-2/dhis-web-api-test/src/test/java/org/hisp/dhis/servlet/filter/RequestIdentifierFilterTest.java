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
package org.hisp.dhis.servlet.filter;

import static org.hisp.dhis.external.conf.ConfigurationKey.LOGGING_REQUEST_ID_ENABLED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.webapi.filter.RequestIdentifierFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class RequestIdentifierFilterTest
{

    @Mock
    private DhisConfigurationProvider dhisConfigurationProvider;

    private RequestIdentifierFilter subject;

    @BeforeEach
    public void setUp()
    {
        MDC.clear();
    }

    @Test
    void testIsDisabled()
        throws ServletException,
        IOException
    {
        init( false );
        doFilter();

        assertNull( MDC.get( "sessionId" ) );
    }

    @Test
    void testIsEnabled()
        throws ServletException,
        IOException
    {
        init( true );
        doFilter();

        assertNotNull( MDC.get( "sessionId" ) );
    }

    private void doFilter()
        throws ServletException,
        IOException
    {
        HttpServletRequest req = mock( HttpServletRequest.class );
        HttpServletResponse res = mock( HttpServletResponse.class );
        FilterChain filterChain = mock( FilterChain.class );

        HttpSession session = mock( HttpSession.class );

        when( req.getSession() ).thenReturn( session );
        when( session.getId() ).thenReturn( "ABCDEFGHILMNO" );

        subject.doFilter( req, res, filterChain );
    }

    private void init( boolean enabled )
    {
        when( dhisConfigurationProvider.isEnabled( LOGGING_REQUEST_ID_ENABLED ) ).thenReturn( enabled );
        subject = new RequestIdentifierFilter( dhisConfigurationProvider );
    }
}
