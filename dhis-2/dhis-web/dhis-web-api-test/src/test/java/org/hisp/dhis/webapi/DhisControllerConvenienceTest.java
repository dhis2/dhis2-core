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
package org.hisp.dhis.webapi;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.utils.TestUtils;
import org.hisp.dhis.webapi.json.JsonResponse;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * Base class for convenient testing of the web API on basis of
 * {@link JsonResponse}.
 *
 * @author Jan Bernitt
 */
@RunWith( SpringRunner.class )
@WebAppConfiguration
@ContextConfiguration( classes = { MvcTestConfig.class, WebTestConfiguration.class } )
@ActiveProfiles( "test-h2" )
@Transactional
public abstract class DhisControllerConvenienceTest extends DhisConvenienceTest
{
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService _userService;

    private MockMvc mvc;

    private MockHttpSession session;

    @Before
    public final void setup()
        throws Exception
    {
        userService = _userService;
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding( "UTF-8" );
        characterEncodingFilter.setForceEncoding( true );
        mvc = MockMvcBuilders.webAppContextSetup( webApplicationContext ).build();
        TestUtils.executeStartupRoutines( webApplicationContext );
        session = startSessionWith( "ALL" );
    }

    private MockHttpSession startSessionWith( String... authorities )
    {
        createAndInjectAdminUser( authorities );

        MockHttpSession session = new MockHttpSession();
        session.setAttribute( HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext() );
        return session;
    }

    public static void assertStatus( HttpStatus status, HttpResponse response )
    {
        assertEquals( status, response.status() );
    }

    public static void assertSeries( HttpStatus.Series series, HttpResponse response )
    {
        assertEquals( series, response.series() );
    }

    public static class HttpResponse
    {
        private final MockHttpServletResponse response;

        public HttpResponse( MockHttpServletResponse response )
        {
            this.response = response;
        }

        public HttpStatus status()
        {
            return HttpStatus.resolve( response.getStatus() );
        }

        public HttpStatus.Series series()
        {
            return status().series();
        }

        public boolean success()
        {
            return series() == Series.SUCCESSFUL;
        }

        public JsonResponse content()
        {
            return content( Series.SUCCESSFUL );
        }

        public JsonResponse content( HttpStatus.Series expected )
        {
            assertSeries( expected, this );
            return contentInternal();
        }

        public JsonResponse content( HttpStatus expected )
        {
            assertStatus( expected, this );
            return contentInternal();
        }

        private JsonResponse contentInternal()
        {
            return failUnless( () -> new JsonResponse( response.getContentAsString( StandardCharsets.UTF_8 ) ) );
        }

    }

    static <T> T failUnless( Callable<T> op )
    {
        try
        {
            return op.call();
        }
        catch ( Exception ex )
        {
            throw new AssertionError( ex );
        }
    }

    protected final HttpResponse GET( String url )
    {
        return GET( url, new String[0] );
    }

    protected final HttpResponse GET( String url, String... args )
    {
        return webRequest( get( substitutePlaceholders( url, args ) ), null );
    }

    private String substitutePlaceholders( String url, String[] args )
    {
        return args.length == 0
            ? url
            : String.format( url.replaceAll( "\\{[a-zA-Z]+}", "%s" ), (Object[]) args );
    }

    protected final HttpResponse POST( String url, String body )
    {
        return null;
    }

    protected final HttpResponse POST( String url, Path body )
    {
        return null;
    }

    protected final HttpResponse PATCH( String url, String body )
    {
        return webRequest( patch( url ), body );
    }

    private HttpResponse webRequest( MockHttpServletRequestBuilder builder, String body )
    {
        MockHttpServletRequestBuilder request = builder.session( session );
        if ( body != null && !body.isEmpty() )
        {
            request = request.contentType( APPLICATION_JSON )
                .content( body.replace( '\'', '"' ) );
        }
        MockHttpServletRequestBuilder completeRequest = request;
        return failUnless(
            () -> new HttpResponse( mvc.perform( completeRequest ).andReturn().getResponse() ) );
    }
}
