package org.hisp.dhis.webapi.documentation.controller;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.io.ByteStreams;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.webapi.documentation.common.ResponseDocumentation;
import org.hisp.dhis.webapi.documentation.common.TestUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class AttributeControllerDocumentation
    extends AbstractWebApiTest<Attribute>
{

    @Override
    public void testCreate() throws Exception
    {

        InputStream input = new ClassPathResource( "attribute/SQLViewAttribute.json" ).getInputStream();

        MockHttpSession session = getSession( "ALL" );

        Set<FieldDescriptor> fieldDescriptors = TestUtils.getFieldDescriptors( schema );

        mvc.perform( post( schema.getRelativeApiEndpoint() )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( ByteStreams.toByteArray( input ) ) )
            .andExpect( status().is( createdStatus ) )
            .andDo( documentPrettyPrint( schema.getPlural() + "/create",
                requestFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) ) )
            );
    }

    @Override
    public void testUpdate() throws Exception
    {
        InputStream input = new ClassPathResource( "attribute/SQLViewAttribute.json" ).getInputStream();

        MockHttpSession session = getSession( "ALL" );

        MvcResult postResult = mvc.perform( post( schema.getRelativeApiEndpoint() )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( ByteStreams.toByteArray( input ) ) )
            .andExpect( status().is( createdStatus ) ).andReturn();

        String uid = TestUtils.getCreatedUid( postResult.getResponse().getContentAsString() );

        InputStream inputUpdate = new ClassPathResource( "attribute/SQLViewAttribute.json" ).getInputStream();

        mvc.perform( put( schema.getRelativeApiEndpoint() + "/" + uid )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content(  ByteStreams.toByteArray( inputUpdate )  ) )
            .andExpect( status().is( updateStatus ) )
            .andDo( documentPrettyPrint( schema.getPlural() + "/update" ) );

    }

    @Override
    public void testGetAll() throws Exception
    {
        InputStream input = new ClassPathResource( "attribute/SQLViewAttribute.json" ).getInputStream();

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( post( schema.getRelativeApiEndpoint() )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( ByteStreams.toByteArray( input ) ) )
            .andExpect( status().is( createdStatus ) ).andReturn();


        List<FieldDescriptor> fieldDescriptors = new ArrayList<>();
        fieldDescriptors.addAll( ResponseDocumentation.pager() );
        fieldDescriptors.add( fieldWithPath( schema.getPlural() ).description( schema.getPlural() ) );

        mvc.perform( get( schema.getRelativeApiEndpoint() ).session( session ).accept( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentTypeCompatibleWith( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( jsonPath( "$." + schema.getPlural() ).isArray() )
            .andExpect( jsonPath( "$." + schema.getPlural() + ".length()" ).value( 1 ) )
            .andDo( documentPrettyPrint( schema.getPlural() + "/all",
                responseFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) )
            ) );
    }

    @Override
    public void testGetByIdOk() throws Exception
    {
        InputStream input = new ClassPathResource( "attribute/SQLViewAttribute.json" ).getInputStream();

        MockHttpSession session = getSession( "ALL" );

        MvcResult postResult = mvc.perform( post( schema.getRelativeApiEndpoint() )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( ByteStreams.toByteArray( input ) ) )
            .andExpect( status().is( createdStatus ) ).andReturn();

        Set<FieldDescriptor> fieldDescriptors = TestUtils.getFieldDescriptors( schema );

        String uid = TestUtils.getCreatedUid( postResult.getResponse().getContentAsString() );

        mvc.perform( get( schema.getRelativeApiEndpoint() + "/{id}", uid ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.name" ).value( "sqlViewAttribute") )
            .andDo( documentPrettyPrint( schema.getPlural() + "/id",
                responseFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) ) ) );

    }

    @Override
    public void testDeleteByIdOk() throws Exception
    {
        InputStream input = new ClassPathResource( "attribute/SQLViewAttribute.json" ).getInputStream();

        MockHttpSession session = getSession( "ALL" );

        MvcResult postResult = mvc.perform( post( schema.getRelativeApiEndpoint() )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( ByteStreams.toByteArray( input ) ) )
            .andExpect( status().is( createdStatus ) ).andReturn();

        String uid = TestUtils.getCreatedUid( postResult.getResponse().getContentAsString() );

        mvc.perform( delete( schema.getRelativeApiEndpoint() + "/{id}", uid ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().is( deleteStatus ) )
            .andDo( documentPrettyPrint( schema.getPlural() + "/delete" ) );

    }

    @Test
    public void testCreateSectionAttribute() throws Exception
    {
        InputStream input = new ClassPathResource( "attribute/SectionAttribute.json" ).getInputStream();

        MockHttpSession session = getSession( "ALL" );

        mvc.perform( post( schema.getRelativeApiEndpoint() )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( ByteStreams.toByteArray( input ) ) )
            .andExpect( status().is( createdStatus ));
    }
}
