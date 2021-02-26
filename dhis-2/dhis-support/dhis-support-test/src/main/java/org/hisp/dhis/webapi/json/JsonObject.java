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
     *
     * @param names a set of names that should exist
     * @return true if this object has (at least) all the given names
     */
    boolean has( String... names );

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
        return asList( getArray( name ), as );
    }

}
