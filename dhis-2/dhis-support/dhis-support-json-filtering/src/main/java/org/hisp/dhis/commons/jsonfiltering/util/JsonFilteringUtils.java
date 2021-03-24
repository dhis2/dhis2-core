/*
 * Copyright (c) 2004-2004-2020, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;

/**
 * Provides various convenience methods.
 *
 * @author rbohn
 * @author DheerajKN
 */
public class JsonFilteringUtils
{

    private JsonFilteringUtils()
    {
    }

    /**
     * Convert an object to a collection of maps.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @param targetCollectionType the target collection type
     * @return collection
     */
    public static Collection<Map<String, Object>> collectify( ObjectMapper mapper, Object source,
        Class<? extends Collection> targetCollectionType )
    {
        return collectify( mapper, source, targetCollectionType, String.class, Object.class );
    }

    /**
     * Convert an object to a collection.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @param targetCollectionType the target collection type
     * @param targetElementType the target collection element type
     * @return collection
     */
    public static <E> Collection<E> collectify( ObjectMapper mapper, Object source,
        Class<? extends Collection> targetCollectionType, Class<E> targetElementType )
    {
        CollectionType collectionType = mapper.getTypeFactory().constructCollectionType( targetCollectionType,
            targetElementType );
        return objectify( mapper, convertToCollection( source ), collectionType );
    }

    /**
     * Convert an object to a collection.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @param targetCollectionType the target collection type
     * @param targetElementType the target collection element type
     * @return collection
     */
    public static <E> Collection<E> collectify( ObjectMapper mapper, Object source,
        Class<? extends Collection> targetCollectionType, JavaType targetElementType )
    {
        CollectionType collectionType = mapper.getTypeFactory().constructCollectionType( targetCollectionType,
            targetElementType );
        return objectify( mapper, convertToCollection( source ), collectionType );
    }

    /**
     * Convert an object to a collection of maps.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @param targetCollectionType the target collection type
     * @param targetKeyType the target map key type
     * @param targetValueType the target map value type
     * @return collection
     */
    public static <K, V> Collection<Map<K, V>> collectify( ObjectMapper mapper, Object source,
        Class<? extends Collection> targetCollectionType, Class<K> targetKeyType, Class<V> targetValueType )
    {
        MapType mapType = mapper.getTypeFactory().constructMapType( Map.class, targetKeyType, targetValueType );
        return collectify( mapper, convertToCollection( source ), targetCollectionType, mapType );
    }

    private static Object convertToCollection( Object source )
    {
        if ( source == null )
        {
            return null;
        }

        if ( source instanceof Collection )
        {
            return source;
        }

        return Collections.singleton( source );
    }

    /**
     * Convert an object to a list of maps.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @return list
     */
    public static List<Map<String, Object>> listify( ObjectMapper mapper, Object source )
    {
        return (List<Map<String, Object>>) collectify( mapper, source, List.class );
    }

    /**
     * Convert an object to a list.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @param targetElementType the target list element type
     * @return list
     */
    public static <E> List<E> listify( ObjectMapper mapper, Object source, Class<E> targetElementType )
    {
        return (List<E>) collectify( mapper, source, List.class, targetElementType );
    }

    /**
     * Convert an object to a list.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @param targetElementType the target list element type
     * @return list
     */
    public static <E> List<E> listify( ObjectMapper mapper, Object source, JavaType targetElementType )
    {
        return (List<E>) collectify( mapper, source, List.class, targetElementType );
    }

    /**
     * Convert an object to a collection of maps.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @param targetKeyType the target map key type
     * @param targetValueType the target map value type
     * @return collection
     */
    public static <K, V> List<Map<K, V>> listify( ObjectMapper mapper, Object source, Class<K> targetKeyType,
        Class<V> targetValueType )
    {
        return (List<Map<K, V>>) collectify( mapper, source, List.class, targetKeyType, targetValueType );
    }

    /**
     * Converts an object to an object, with json-filtering filters applied.
     *
     * @param mapper the object mapper
     * @param source the source to convert
     * @return target instance
     * @see JsonFilteringUtils#objectify(ObjectMapper, Object, Class)
     */
    public static Object objectify( ObjectMapper mapper, Object source )
    {
        return objectify( mapper, source, Object.class );
    }

    /**
     * Converts an object to an instance of the target type. Unlike
     * {@link ObjectMapper#convertValue(Object, Class)}, this method will apply
     * Squiggly filters. It does so by first converting the source to bytes and
     * then re-reading it.
     *
     * @param mapper the object mapper
     * @param source the source to convert
     * @param targetType the target class type
     * @return target instance
     */
    public static <T> T objectify( ObjectMapper mapper, Object source, Class<T> targetType )
    {
        return objectify( mapper, source, mapper.getTypeFactory().constructType( targetType ) );
    }

    /**
     * Converts an object to an instance of the target type.
     *
     * @param mapper the object mapper
     * @param source the source to convert
     * @param targetType the target class type
     * @return target instance
     * @see JsonFilteringUtils#objectify(ObjectMapper, Object, Class)
     */
    public static <T> T objectify( ObjectMapper mapper, Object source, JavaType targetType )
    {
        try
        {
            return mapper.readValue( mapper.writeValueAsBytes( source ), targetType );
        }
        catch ( RuntimeException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     * Convert an object to a set of maps.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @return set
     */
    public static Set<Map<String, Object>> setify( ObjectMapper mapper, Object source )
    {
        return (Set<Map<String, Object>>) collectify( mapper, source, Set.class );
    }

    /**
     * Convert an object to a set.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @param targetElementType the target set element type
     * @return set
     */
    public static <E> Set<E> setify( ObjectMapper mapper, Object source, Class<E> targetElementType )
    {
        return (Set<E>) collectify( mapper, source, Set.class, targetElementType );
    }

    /**
     * Convert an object to a set.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @param targetElementType the target set element type
     * @return set
     */
    public static <E> Set<E> setify( ObjectMapper mapper, Object source, JavaType targetElementType )
    {
        return (Set<E>) collectify( mapper, source, Set.class, targetElementType );
    }

    /**
     * Convert an object to a collection of maps.
     *
     * @param mapper the object mapper
     * @param source the source object
     * @param targetKeyType the target map key type
     * @param targetValueType the target map value type
     * @return collection
     */
    public static <K, V> Set<Map<K, V>> setify( ObjectMapper mapper, Object source, Class<K> targetKeyType,
        Class<V> targetValueType )
    {
        return (Set<Map<K, V>>) collectify( mapper, source, Set.class, targetKeyType, targetValueType );
    }

    /**
     * Takes an object and converts it to a string.
     *
     * @param mapper the object mapper
     * @param object the object to convert
     * @return json string
     */
    public static String stringify( ObjectMapper mapper, Object object )
    {
        try
        {
            return mapper.writeValueAsString( object );
        }
        catch ( JsonProcessingException e )
        {
            throw new IllegalArgumentException( e );
        }
    }
}
