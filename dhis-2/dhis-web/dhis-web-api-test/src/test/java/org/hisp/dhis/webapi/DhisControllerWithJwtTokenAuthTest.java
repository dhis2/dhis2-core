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

import static org.hisp.dhis.webapi.utils.WebClientUtils.failOnException;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.utils.TestUtils;
import org.hisp.dhis.webapi.json.JsonResponse;
import org.hisp.dhis.webapi.security.config.WebMvcConfig;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
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
 * @author Morten Svanæs
 */
@RunWith( SpringRunner.class )
@WebAppConfiguration
@ContextConfiguration( classes = { WebMvcConfig.class, WebTestConfigurationWithJwtTokenAuth.class } )
@ActiveProfiles( "test-h2" )
@Transactional
public abstract class DhisControllerWithJwtTokenAuthTest extends DhisConvenienceTest implements WebClient
{
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private UserService _userService;

    protected MockMvc mvc;

    @Before
    public final void setup()
        throws Exception
    {
        userService = _userService;
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding( "UTF-8" );
        characterEncodingFilter.setForceEncoding( true );

        mvc = MockMvcBuilders.webAppContextSetup( webApplicationContext )
            .addFilter( springSecurityFilterChain ).build();

        TestUtils.executeStartupRoutines( webApplicationContext );
    }

    @Override
    public HttpResponse webRequest( MockHttpServletRequestBuilder request )
    {
        return failOnException(
            () -> new HttpResponse( mvc.perform( request ).andReturn().getResponse() ) );
    }
}
