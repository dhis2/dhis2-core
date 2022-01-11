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
package org.hisp.dhis.webapi.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class ApiVersionMethodTest extends DhisWebSpringTest
{

    @Test
    void testMethodV31V32()
        throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        String endpoint = "/method/testV31V32";
        mvc.perform( get( endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/31" + endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/32" + endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/31" + endpoint + "/a" ).session( session ) ).andExpect( status().isOk() );
        mvc.perform( post( "/31" + endpoint + "/a" ).session( session ) ).andExpect( status().isOk() );
        mvc.perform( put( "/31" + endpoint + "/a" ).session( session ) ).andExpect( status().isMethodNotAllowed() );
        mvc.perform( get( "/32" + endpoint + "/b" ).session( session ) ).andExpect( status().isOk() );
        mvc.perform( post( "/32" + endpoint + "/b" ).session( session ) ).andExpect( status().isMethodNotAllowed() );
        mvc.perform( put( "/32" + endpoint + "/b" ).session( session ) ).andExpect( status().isOk() );
    }

    @Test
    void testMethodAll()
        throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        String endpoint = "/method/testAll";
        mvc.perform( get( endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/31" + endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/32" + endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/31" + endpoint + "/a" ).session( session ) ).andExpect( status().isOk() );
        mvc.perform( get( "/32" + endpoint + "/b" ).session( session ) ).andExpect( status().isOk() );
    }

    @Test
    void testMethodAllExcludeV32()
        throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        String endpoint = "/method/testAllExcludeV32";
        mvc.perform( get( endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/32" + endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/32" + endpoint + "/a" ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/32" + endpoint + "/b" ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/31" + endpoint + "/a" ).session( session ) ).andExpect( status().isOk() );
        mvc.perform( get( "/31" + endpoint + "/b" ).session( session ) ).andExpect( status().isOk() );
    }

    @Test
    void testMethodDefault()
        throws Exception
    {
        MockHttpSession session = getSession( "ALL" );
        String endpoint = "/method/testDefault";
        mvc.perform( get( endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/31" + endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/32" + endpoint ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/31" + endpoint + "/a" ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( "/32" + endpoint + "/b" ).session( session ) ).andExpect( status().isNotFound() );
        mvc.perform( get( endpoint + "/a" ).session( session ) ).andExpect( status().isOk() );
        mvc.perform( get( endpoint + "/b" ).session( session ) ).andExpect( status().isOk() );
    }
}
