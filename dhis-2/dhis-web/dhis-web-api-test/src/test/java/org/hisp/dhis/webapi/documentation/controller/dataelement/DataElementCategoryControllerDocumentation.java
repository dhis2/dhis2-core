package org.hisp.dhis.webapi.documentation.controller.dataelement;


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


import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.descriptors.CategorySchemaDescriptor;
import org.hisp.dhis.webapi.documentation.common.ResponseDocumentation;
import org.hisp.dhis.webapi.documentation.common.TestUtils;
import org.hisp.dhis.webapi.documentation.controller.AbstractWebApiTest;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Viet Nguyen <viet@dhis.org>
 */
public class DataElementCategoryControllerDocumentation
    extends AbstractWebApiTest<DataElementCategory>
{
    private static final String ENDPOINT = "categories";

    @Test
    @Override
    public void testGetAll() throws Exception
    {
        manager.save( createDataElementCategory( 'A' ) );
        manager.save( createDataElementCategory( 'B' ) );
        manager.save( createDataElementCategory( 'C' ) );
        manager.save( createDataElementCategory( 'D' ) );

        MockHttpSession session = getSession( "ALL" );

        List<FieldDescriptor> fieldDescriptors = new ArrayList<>();
        fieldDescriptors.addAll( ResponseDocumentation.pager() );
        fieldDescriptors.add( fieldWithPath( CategorySchemaDescriptor.PLURAL ).description( "Data element categories" ) );

        mvc.perform( get( "/" + CategorySchemaDescriptor.PLURAL ).session( session ).accept( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentTypeCompatibleWith( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( jsonPath( "$." + CategorySchemaDescriptor.PLURAL ).isArray() )
            .andExpect( jsonPath( "$." + CategorySchemaDescriptor.PLURAL + ".length()" ).value( 5 ) )
            .andDo( documentPrettyPrint( CategorySchemaDescriptor.PLURAL + "/all",
                responseFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) )
            ) );
    }

    @Test
    @Override
    public void testGetByIdOk() throws Exception
    {

        DataElementCategory cat = createDataElementCategory( 'A' );
        manager.save( cat );
        MockHttpSession session = getSession( "ALL" );

        Schema schema = schemaService.getSchema( DataElementCategory.class );
        Set<FieldDescriptor> fieldDescriptors = TestUtils.getFieldDescriptors( schema );

        mvc.perform( get( "/" + CategorySchemaDescriptor.PLURAL + "/{id}", cat.getUid() ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
            .andExpect( jsonPath( "$.name" ).value( "DataElementCategoryA" ) )
            .andExpect( jsonPath( "$.shortName" ).value( "DataElementCategoryA" ) )
            .andDo( documentPrettyPrint( ENDPOINT + "/id",
                responseFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) )
            ) );
    }


    @Test
    public void testAddDeleteCollectionItem() throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        DataElementCategory category = createDataElementCategory( 'A' );
        manager.save( category );

        Schema schema = schemaService.getSchema( DataElementCategory.class );

        List<Property> properties = schema.getProperties();

        for ( Property property : properties )
        {
            if ( property.isCollection() )
            {
                String collectionName = property.getCollectionName();

                IdentifiableObject item = createTestObject( property.getItemKlass(), 'A', category );

                if ( item == null )
                {
                    continue;
                }
                else
                {
                    manager.save( item );
                }

                mvc.perform( post( "/" + ENDPOINT + "/" + category.getUid() + "/" + collectionName + "/" + item.getUid() )
                    .session( session )
                    .contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
                    .andDo( documentPrettyPrint( ENDPOINT + "/add" + collectionName ) )
                    .andExpect( status().isNoContent() );

                mvc.perform( delete( "/" + ENDPOINT + "/" + category.getUid() + "/" + collectionName + "/" + item.getUid() )
                    .session( session )
                    .contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
                    .andDo( documentPrettyPrint( ENDPOINT + "/delete" + collectionName ) )
                    .andExpect( status().isNoContent() );

            }
        }
    }


    @Test
    @Override
    public void testCreate() throws Exception
    {
        MockHttpSession session = getSession( "F_CATEGORY_PUBLIC_ADD" );

        DataElementCategoryOption categoryOptionA = createCategoryOption( 'A' );
        DataElementCategoryOption categoryOptionB = createCategoryOption( 'B' );
        DataElementCategoryOption categoryOptionC = createCategoryOption( 'C' );

        DataElementCategory cat = createDataElementCategory( 'A', categoryOptionA, categoryOptionB, categoryOptionC );

        Schema schema = schemaService.getSchema( DataElementCategory.class );

        Set<FieldDescriptor> fieldDescriptors = TestUtils.getFieldDescriptors( schema );

        mvc.perform( post( "/" + ENDPOINT )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( TestUtils.convertObjectToJsonBytes( cat ) ) )
            .andExpect( status().is( createdStatus ) )
            .andDo( documentPrettyPrint( "categories/create",
                requestFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) ) )
            );

        cat = manager.getByName( DataElementCategory.class, "DataElementCategoryA" );

        assertNotNull( cat );
    }


    @Test
    @Override
    public void testDeleteByIdOk() throws Exception
    {
        DataElementCategory cat = createDataElementCategory( 'A' );
        manager.save( cat );
        MockHttpSession session = getSession( "ALL" );

        mvc.perform( delete( "/" + ENDPOINT + "/{id}", cat.getUid() ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().is( deleteStatus ) )
            .andDo( documentPrettyPrint( "categories/delete" ) );
    }
}
