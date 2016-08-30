package org.hisp.dhis.webapi.documentation.common;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.codehaus.jackson.JsonNode;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

/**
 * @author Viet Nguyen <viet@dhis.org>
 */
public class TestUtils
{
    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType( MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName( "utf8" ) );

    public static byte[] convertObjectToJsonBytes( Object object ) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        objectMapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
        objectMapper.configure( SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false );
        objectMapper.configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false );
        objectMapper.configure( SerializationFeature.WRAP_EXCEPTIONS, true );

        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        objectMapper.configure( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true );
        objectMapper.configure( DeserializationFeature.WRAP_EXCEPTIONS, true );

        objectMapper.disable( MapperFeature.AUTO_DETECT_FIELDS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_CREATORS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_GETTERS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_SETTERS );
        objectMapper.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );

        return objectMapper.writeValueAsBytes( object );
    }

    public static String getFieldDescription( Property p )
    {
        String desc = "";
        desc += p.isRequired() ? "Required, " : "";
        desc += p.isAttribute() ? "Attribute, " : "";
        desc += p.isReadable() ? "Readable" : "";

        return desc;
    }

    public static Set<FieldDescriptor> getFieldDescriptors( Schema schema )
    {
        Set<FieldDescriptor> fieldDescriptors = new HashSet<>();
        Map<String, Property> persistedPropertyMap = schema.getPersistedProperties();

        Iterator<?> persistedItr = persistedPropertyMap.keySet().iterator();

        while ( persistedItr.hasNext() )
        {
            Property p = persistedPropertyMap.get( persistedItr.next() );
            String pName = p.isCollection() ? p.getCollectionName() : p.getName();
            FieldDescriptor f = fieldWithPath( pName ).description( TestUtils.getFieldDescription( p ) );

            if ( !p.isRequired() )
            {
                f.optional().type( p.getPropertyType() );
            }

            fieldDescriptors.add( f );
        }

        Map<String, Property> nonPersistedPropertyMap = schema.getNonPersistedProperties();
        Iterator<?> nonPersistedItr = nonPersistedPropertyMap.keySet().iterator();

        while ( nonPersistedItr.hasNext() )
        {
            Property p = nonPersistedPropertyMap.get( nonPersistedItr.next() );
            String pName = p.isCollection() ? p.getCollectionName() : p.getName();
            FieldDescriptor f = fieldWithPath( pName ).description( TestUtils.getFieldDescription( p ) );

            if ( !p.isRequired() )
            {
                f.optional().type( p.getPropertyType() );
            }
            fieldDescriptors.add( f );
        }

        return fieldDescriptors;
    }

    public static String getCreatedUid( String responseJson ) throws IOException
    {
        org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();
        JsonNode node = mapper.readTree( responseJson );
        return node.get( "response" ).get( "uid" ).asText();
    }

}
