/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.webapi.documentation.controller.dataelement;

import static org.junit.Assert.assertNull;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.webapi.documentation.common.ResponseDocumentation;
import org.hisp.dhis.webapi.documentation.common.TestUtils;
import org.hisp.dhis.webapi.documentation.controller.AbstractWebApiTest;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.FieldDescriptor;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DataElementControllerDocumentation
    extends AbstractWebApiTest<DataElement>
{

    @Test
    public void testGetById404()
        throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        mvc.perform(
            get( "/dataElements/{id}", "deabcdefghA" ).session( session ).accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isNotFound() );
    }

    @Test
    public void testCreateValidation()
        throws Exception
    {
        MockHttpSession session = getSession( "F_DATAELEMENT_PUBLIC_ADD" );

        DataElement de = createDataElement( 'A' );
        de.setName( null );

        mvc.perform( post( "/dataElements" )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 )
            .content( TestUtils.convertObjectToJsonBytes( de ) ) );

        de = manager.getByName( DataElement.class, "DataElementA" );

        assertNull( de );
    }

    @Test
    public void testFilterLike()
        throws Exception
    {
        MockHttpSession session = getSession( "F_DATAELEMENT_PUBLIC_ADD" );
        DataElement de = createDataElement( 'A' );
        manager.save( de );

        List<FieldDescriptor> fieldDescriptors = new ArrayList<>( ResponseDocumentation.pager() );
        fieldDescriptors.add( fieldWithPath( "dataElements" ).description( "Data elements" ) );

        mvc.perform( get( "/dataElements?filter=name:like:DataElementA" )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( jsonPath( "$.pager.total", org.hamcrest.Matchers.greaterThan( 0 ) ) )
            .andDo( documentPrettyPrint( "data-elements/filter",
                responseFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) ) ) );
    }

    @Test
    public void testFilteriLikeOk()
        throws Exception
    {
        MockHttpSession session = getSession( "F_DATAELEMENT_PUBLIC_ADD" );
        DataElement de = createDataElement( 'A' );
        manager.save( de );

        mvc.perform( get( "/dataElements?filter=name:ilike:DataElementA" )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( jsonPath( "$.pager.total", org.hamcrest.Matchers.greaterThan( 0 ) ) );
    }

    @Test
    public void testFilterEqualOk()
        throws Exception
    {
        MockHttpSession session = getSession( "F_DATAELEMENT_PUBLIC_ADD" );
        DataElement de = createDataElement( 'A' );
        manager.save( de );

        mvc.perform( get( "/dataElements?filter=name:eq:DataElementA" )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( jsonPath( "$.pager.total", org.hamcrest.Matchers.greaterThan( 0 ) ) );
    }

    @Test
    public void testFieldsFilterOk()
        throws Exception
    {
        MockHttpSession session = getSession( "F_DATAELEMENT_PUBLIC_ADD" );
        DataElement de = createDataElement( 'A' );
        manager.save( de );

        List<FieldDescriptor> fieldDescriptors = new ArrayList<>( ResponseDocumentation.pager() );
        fieldDescriptors.add( fieldWithPath( "dataElements" ).description( "Data elements" ) );

        mvc.perform( get( "/dataElements?filter=name:eq:DataElementA&fields=id,name,valueType" )
            .session( session )
            .contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
            .andExpect( jsonPath( "$.dataElements[*].id" ).exists() )
            .andExpect( jsonPath( "$.dataElements[*].name" ).exists() )
            .andExpect( jsonPath( "$.dataElements[*].valueType" ).exists() )
            .andExpect( jsonPath( "$.dataElements[*].categoryCombo" ).doesNotExist() )
            .andDo( documentPrettyPrint( "data-elements/fields",
                responseFields( fieldDescriptors.toArray( new FieldDescriptor[fieldDescriptors.size()] ) ) ) );
    }

    @Test
    public void testAddDeleteCollectionItem()
        throws Exception
    {
        MockHttpSession session = getSession( "ALL" );

        DataElement de = createDataElement( 'A' );
        manager.save( de );

        Schema schema = schemaService.getSchema( DataElement.class );

        List<Property> properties = schema.getProperties();

        for ( Property property : properties )
        {
            if ( property.isCollection() )
            {
                String collectionName = property.getCollectionName();

                IdentifiableObject item = createTestObject( property.getItemKlass(), 'A' );

                if ( item == null )
                {
                    continue;
                }
                else
                {
                    manager.save( item );
                }

                mvc.perform( post( "/dataElements/" + de.getUid() + "/" + collectionName + "/" + item.getUid() )
                    .session( session )
                    .contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
                    .andDo( documentPrettyPrint( "data-elements/add" + collectionName ) )
                    .andExpect( status().isNoContent() );

                mvc.perform( delete( "/dataElements/" + de.getUid() + "/" + collectionName + "/" + item.getUid() )
                    .session( session )
                    .contentType( TestUtils.APPLICATION_JSON_UTF8 ) )
                    .andDo( documentPrettyPrint( "data-elements/delete" + collectionName ) )
                    .andExpect( status().isNoContent() );

            }
        }
    }
}
