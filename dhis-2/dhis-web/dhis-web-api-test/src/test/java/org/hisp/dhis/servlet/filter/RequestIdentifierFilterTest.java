/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.external.conf.ConfigurationKey.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.webapi.filter.RequestIdentifierFilter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;
import org.slf4j.MDC;

/**
 * @author Luciano Fiandesio
 */
public class RequestIdentifierFilterTest
{
    @Mock
    private DhisConfigurationProvider dhisConfigurationProvider;

    private RequestIdentifierFilter subject;

    @Rule
    public MockitoRule mockitoRule = rule();

    private static final String DEFAULT_HASH_ALGO = MONITORING_LOG_REQUESTID_HASHALGO.getDefaultValue();

    @Before
    public void setUp()
    {
        MDC.clear();
    }

    @Test
    public void testHashSessionIdNoTruncate()
        throws ServletException,
        IOException
    {
        init( -1, DEFAULT_HASH_ALGO, true );

        doFilter();

        assertThat( MDC.get( "sessionId" ), is( "IDJShqHVoTDIhlsHjr7eIvUrMsMM7Gs2LYGjog1W6nQFo=" ) );

    }

    @Test
    public void testHashSessionIdWithTruncate()
        throws ServletException,
        IOException
    {
        init( 5, DEFAULT_HASH_ALGO, true );

        doFilter();

        assertThat( MDC.get( "sessionId" ), is( "IDJShqH" ) );

    }

    @Test
    public void testHashSessionIdWithMd5()
        throws ServletException,
        IOException
    {
        init( -1, "MD5", true );
        doFilter();

        assertThat( MDC.get( "sessionId" ), is( "IDrBKUxIZl6blN7EtczRa7fQ==" ) );

    }

    @Test
    public void testHashSessionIdWithInvalidAlgorithm()
        throws ServletException,
        IOException
    {
        init( -1, "RIJKA", true );
        doFilter();

        assertNull( MDC.get( "sessionId" ) );

    }

    @Test
    public void testisDisabled()
        throws ServletException,
        IOException
    {
        init( -1, "SHA-256", false );
        doFilter();

        assertNull( MDC.get( "sessionId" ) );

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

    private void init( int maxSize, String hashAlgo, boolean enabled )
    {

        when( dhisConfigurationProvider.getProperty( MONITORING_LOG_REQUESTID_MAXSIZE ) )
            .thenReturn( Integer.toString( maxSize ) );
        when( dhisConfigurationProvider.isEnabled( MONITORING_LOG_REQUESTID_ENABLED ) ).thenReturn( enabled );
        when( dhisConfigurationProvider.getProperty( MONITORING_LOG_REQUESTID_HASHALGO ) ).thenReturn( hashAlgo );

        subject = new RequestIdentifierFilter( dhisConfigurationProvider );
    }
}
