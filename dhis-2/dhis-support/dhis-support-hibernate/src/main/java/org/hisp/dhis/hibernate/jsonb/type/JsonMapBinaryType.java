package org.hisp.dhis.hibernate.jsonb.type;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.HibernateException;
import org.hisp.dhis.attribute.AttributeValue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class JsonMapBinaryType<T>
    extends JsonBinaryType
{
    @Override
    protected JavaType getResultingJavaType( Class<?> returnedClass )
    {
        return MAPPER.getTypeFactory().constructMapLikeType( Map.class, String.class, returnedClass );
    }

    @Override
    public Object deepCopy( Object value ) throws HibernateException
    {
        String json = convertObjectToJson( value );
        return convertJsonToObject( json );
    }

    @Override
    public Object convertJsonToObject( String content )
    {
        try
        {
            Map<String, T> data = reader.readValue( content );

            return convertAttributeValueMapIntoSet( data );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * Convert Collection<Object> into Map<uid,Object>
     *     then write converted Map to Json String
     * @param object the object to convert. Should be a Collection
     * @return Json String
     */
    @Override
    public String convertObjectToJson( Object object )
    {
        try
        {
            Set<T> elements = object == null ? Collections.emptySet() : (Set<T>) object;

            Map<String, T> attrValueMap = new HashMap<>();

            for ( T element : elements )
            {
                String key = getKey( element );
                if ( key != null )
                {
                    attrValueMap.put( key, element );
                }
            }

            return writer.writeValueAsString( attrValueMap );

        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * Convert Map<Uid,Object> to Collection<Object>
     * @param data
     * @return
     */
    private Set<T> convertAttributeValueMapIntoSet( Map<String, T> data )
    {
        Set<T> set = new HashSet<>();

        for ( Map.Entry<String, T> entry : data.entrySet() )
        {
            set.add( entry.getValue() );
        }

        return set;
    }

    protected abstract String getKey( T object );
}
