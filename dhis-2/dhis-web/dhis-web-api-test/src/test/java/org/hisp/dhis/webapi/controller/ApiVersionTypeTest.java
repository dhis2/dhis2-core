package org.hisp.dhis.webapi.controller;

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

import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ApiVersionTypeTest extends DhisWebSpringTest
{
    @Test
    public void testTypeAnnotationDefault() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        String endpoint = "/type/testDefault";

        mvc.perform( get( endpoint ).session( session ) )
            .andExpect( status().isOk() );

        mvc.perform( get( "/26" + endpoint ).session( session ) )
            .andExpect( status().isNotFound() );

        mvc.perform( get( "/27" + endpoint ).session( session ) )
            .andExpect( status().isNotFound() );
    }

    @Test
    public void testTypeAnnotationDefaultV26() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        String endpoint = "/type/testDefaultV26";

        mvc.perform( get( endpoint ).session( session ) )
            .andExpect( status().isOk() );

        mvc.perform( get( "/26" + endpoint ).session( session ) )
            .andExpect( status().isOk() );

        mvc.perform( get( "/27" + endpoint ).session( session ) )
            .andExpect( status().isNotFound() );
    }

    @Test
    public void testTypeAnnotationV26V27() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        String endpoint = "/type/testV26V27";

        mvc.perform( get( endpoint ).session( session ) )
            .andExpect( status().isNotFound() );

        mvc.perform( get( "/26" + endpoint ).session( session ) )
            .andExpect( status().isOk() );

        mvc.perform( get( "/27" + endpoint ).session( session ) )
            .andExpect( status().isOk() );
    }

    @Test
    public void testTypeAnnotationAll() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        String endpoint = "/type/testAll";

        mvc.perform( get( endpoint ).session( session ) )
            .andExpect( status().isNotFound() );

        mvc.perform( get( "/26" + endpoint ).session( session ) )
            .andExpect( status().isOk() );

        mvc.perform( get( "/27" + endpoint ).session( session ) )
            .andExpect( status().isOk() );
    }

    @Test
    public void testTypeAnnotationAllExcludeV27() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        String endpoint = "/type/testAllExcludeV27";

        mvc.perform( get( endpoint ).session( session ) )
            .andExpect( status().isNotFound() );

        mvc.perform( get( "/26" + endpoint ).session( session ) )
            .andExpect( status().isOk() );

        mvc.perform( get( "/27" + endpoint ).session( session ) )
            .andExpect( status().isNotFound() );
    }

    @Test
    public void testTypeAnnotationDefaultAll() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        String endpoint = "/type/testDefaultAll";

        mvc.perform( get( endpoint ).session( session ) )
            .andExpect( status().isOk() );

        mvc.perform( get( "/26" + endpoint ).session( session ) )
            .andExpect( status().isOk() );

        mvc.perform( get( "/27" + endpoint ).session( session ) )
            .andExpect( status().isOk() );
    }
}
