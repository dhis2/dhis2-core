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
package org.hisp.dhis.webapi.json;

import static java.util.Arrays.stream;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

import org.hisp.dhis.webapi.json.JsonDocument.JsonNodeType;

/**
 * Represents a JSON object node.
 *
 * As all nodes are mere views or virtual field access will never throw a
 * {@link java.util.NoSuchElementException}. Whether or not a field with a given
 * name exists is determined first when {@link JsonValue#exists()} or other
 * value accessing operations are performed on a node.
 *
 * @author Jan Bernitt
 */
public interface JsonObject extends JsonCollection
{

    /**
     * Name access to object fields.
     *
     * Note that this neither checks if a field exist nor if it has the assumed
     * type.
     *
     * @param name field name
     * @param as assumed type of the field
     * @param <T> returned field type
     * @return field value for the given name
     */
    <T extends JsonValue> T get( String name, Class<T> as );

    /**
     * Test an object for member names.
     *
     * @param names a set of member names that should exist
     * @return true if this object has (at least) all the given names
     * @throws UnsupportedOperationException in case this value is not an JSON
     *         object
     */
    boolean has( String... names );

    /**
     * Lists JSON object member names in order of declaration.
     *
     * @return The list of member names in the order they were defined.
     * @throws UnsupportedOperationException in case this value is not an JSON
     *         object
     * @throws java.util.NoSuchElementException in case this value does not
     *         exist in the JSON document
     */
    default List<String> names()
    {
        return new ArrayList<>( node().members().keySet() );
    }

    default JsonValue get( String name )
    {
        return get( name, JsonValue.class );
    }

    default JsonObject getObject( String name )
    {
        return get( name, JsonObject.class );
    }

    default JsonNumber getNumber( String name )
    {
        return get( name, JsonNumber.class );
    }

    default JsonArray getArray( String name )
    {
        return get( name, JsonArray.class );
    }

    default JsonString getString( String name )
    {
        return get( name, JsonString.class );
    }

    default JsonBoolean getBoolean( String name )
    {
        return get( name, JsonBoolean.class );
    }

    default <E extends JsonValue> JsonList<E> getList( String name, Class<E> as )
    {
        return JsonCollection.asList( getArray( name ), as );
    }

    default <E extends JsonValue> JsonMap<E> getMap( String name, Class<E> as )
    {
        return JsonCollection.asMap( getObject( name ), as );
    }

    default <E extends JsonValue> JsonMultiMap<E> getMultiMap( String name, Class<E> as )
    {
        return JsonCollection.asMultiMap( getObject( name ), as );
    }

    /**
     * Uses the {@link Expected} annotations present to check whether this
     * object conforms to the provided type
     *
     * @param type object type to check
     * @return true if this is an object and has all {@link Expected} members of
     *         the provided type
     */
    default boolean isA( Class<? extends JsonObject> type )
    {
        try
        {
            asObject( type );
            return true;
        }
        catch ( NoSuchElementException ex )
        {
            return false;
        }
    }

    /**
     * @see #asObject(Class, boolean, String)
     */
    default <T extends JsonObject> T asObject( Class<T> type )
    {
        return asObject( type, true, "" );
    }

    /**
     * In contrast to {@link #as(Class)} this method does check that this object
     * {@link #exists()}, that it is indeed an object node and that it has all
     * {@link Expected} values expected for the provided object type.
     *
     * @param type expected object type
     * @param recursive true to apply the check to nested {@link JsonObject}s
     * @param <T> type check and of the result
     * @return this node as the provided object type
     * @throws NoSuchElementException when this does not exist, is not an object
     *         node or does not have all of the {@link Expected} members present
     */
    default <T extends JsonObject> T asObject( Class<T> type, boolean recursive, String path )
        throws NoSuchElementException
    {
        if ( !exists() )
        {
            throw new NoSuchElementException(
                String.format( "Expected %s %s node does not exist", path, type.getSimpleName() ) );
        }
        if ( !isObject() )
        {
            throw new NoSuchElementException(
                String.format( "Expected %s %s node is not an object but a %s", path, type.getSimpleName(),
                    node().getType() ) );
        }
        T obj = as( type );
        String parent = path.isEmpty() ? "" : path + ".";
        stream( type.getMethods() )
            .filter( m -> m.getParameterCount() == 0 && m.isAnnotationPresent( Expected.class ) )
            .sorted( Comparator.comparing( Method::getName ) )
            .forEach( m -> {
                Object member = null;
                try
                {
                    member = m.invoke( obj );
                }
                catch ( Exception e )
                {
                    throw new NoSuchElementException( String.format( "Expected %s node member %s had invalid value: %s",
                        type.getSimpleName(), parent + m.getName(), e.getMessage() ) );
                }
                if ( member == null || member instanceof JsonValue && (!((JsonValue) member).exists()
                    || !m.getAnnotation( Expected.class ).nullable() && ((JsonValue) member).isNull()) )
                {
                    throw new NoSuchElementException( String.format( "Expected %s node member %s was not defined",
                        type.getSimpleName(), parent + m.getName() ) );
                }
                if ( recursive && member instanceof JsonObject && !((JsonObject) member).isNull() )
                {
                    @SuppressWarnings( "unchecked" )
                    Class<? extends JsonObject> memberType = (Class<? extends JsonObject>) m.getReturnType();
                    ((JsonObject) member).asObject( memberType, true, parent + m.getName() );
                }
            } );
        return obj;
    }

    /**
     * Finds the first {@link JsonObject} of the given type where the provided
     * test returns true.
     *
     * OBS! When no match is found the resulting {@link JsonValue#exists()} will
     * return true. Use {@link JsonValue#isUndefined()} instead.
     *
     * @param type {@link JsonObject} type to find (must satisfy the
     *        {@link #asObject(Class)} conditions)
     * @param test test to perform on all objects that satisfy the type filter
     * @param <T> type of the object to find
     * @return the first found match or JSON {@code null} object
     */
    default <T extends JsonObject> T find( Class<T> type, Predicate<T> test )
    {
        Optional<JsonNode> match = node().find( JsonNodeType.OBJECT, node -> {
            try
            {
                return test.test( new JsonResponse( node.getDeclaration() ).asObject( type ) );
            }
            catch ( RuntimeException ex )
            {
                return false;
            }
        } );
        return !match.isPresent()
            ? JsonResponse.NULL.as( type )
            : new JsonResponse( match.get().getDeclaration() ).asObject( type );
    }
}
