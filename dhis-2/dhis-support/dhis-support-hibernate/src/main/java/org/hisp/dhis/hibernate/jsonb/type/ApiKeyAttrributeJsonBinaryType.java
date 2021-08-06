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
package org.hisp.dhis.hibernate.jsonb.type;

import java.util.List;

import org.hisp.dhis.security.apikey.ApiTokenAttribute;

import com.fasterxml.jackson.core.type.TypeReference;

public class ApiKeyAttrributeJsonBinaryType extends JsonBinaryType
{
    // @Override
    // protected ObjectMapper getResultingMapper()
    // {
    // ObjectMapper objectMapper = MAPPER.copy();
    // objectMapper.configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false
    // );
    // objectMapper.configure(
    // DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
    // objectMapper.configure( DeserializationFeature.FAIL_ON_INVALID_SUBTYPE,
    // false );
    // return objectMapper;
    // }

    public ApiKeyAttrributeJsonBinaryType()
    {
        super();
        writer = MAPPER.writerFor( new TypeReference<List<ApiTokenAttribute>>()
        {
        } );
        reader = MAPPER.readerFor( new TypeReference<List<ApiTokenAttribute>>()
        {
        } );
        returnedClass = ApiTokenAttribute.class;
    }

    @Override
    protected void init( Class<?> klass )
    {
        returnedClass = klass;
        writer = MAPPER.writerFor( new TypeReference<List<ApiTokenAttribute>>()
        {
        } );
        reader = MAPPER.readerFor( new TypeReference<List<ApiTokenAttribute>>()
        {
        } );
    }

    // @Override
    // public Object deepCopy( Object value )
    // throws HibernateException
    // {
    // String json = convertObjectToJson( value );
    // return convertJsonToObject( json );
    // }
    //
    // /**
    // * Serializes an object to JSON.
    // *
    // * @param object the object to convert.
    // *
    // * @return JSON content.
    // */
    // @SuppressWarnings( "unchecked" )
    // @Override
    // protected String convertObjectToJson( Object object )
    // {
    // try
    // {
    // List<ApiTokenAttribute> eventDataValues = object == null ?
    // Collections.emptyList()
    // : (List<ApiTokenAttribute>) object;
    //
    // List<ApiTokenAttribute> tempMap = new ArrayList<>( eventDataValues );
    //
    // return writer.writeValueAsString( tempMap );
    // }
    // catch ( IOException e )
    // {
    // throw new IllegalArgumentException( e );
    // }
    // }
    //
    // /**
    // * Deserializes JSON content to an object.
    // *
    // * @param content the JSON content.
    // *
    // * @return an object.
    // */
    // @Override
    // public Object convertJsonToObject( String content )
    // {
    // try
    // {
    // List<ApiTokenAttribute> data = reader.readValue( content );
    //
    // return reader;
    // }
    // catch ( IOException e )
    // {
    // throw new IllegalArgumentException( e );
    // }
    // }

    // public static Set<EventDataValue> convertEventDataValuesMapIntoSet(
    // Map<String, EventDataValue> data )
    // {
    //
    // Set<EventDataValue> eventDataValues = new HashSet<>();
    //
    // for ( Map.Entry<String, EventDataValue> entry : data.entrySet() )
    // {
    //
    // EventDataValue eventDataValue = entry.getValue();
    // eventDataValue.setDataElement( entry.getKey() );
    // eventDataValues.add( eventDataValue );
    // }
    //
    // return eventDataValues;
    // }
}
