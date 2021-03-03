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

import java.util.List;

/**
 * Represents a JSON array node.
 *
 * As all nodes are mere views or virtual index access will never throw an
 * {@link ArrayIndexOutOfBoundsException}. Whether or not an element at an index
 * exists is determined first when {@link JsonValue#exists()} or other value
 * accessing operations are performed on a node.
 *
 * @author Jan Bernitt
 */
public interface JsonArray extends JsonCollection
{

    /**
     * Index access to the array.
     *
     * Note that this will neither check index nor element type.
     *
     * @param index index to access (>= 0)
     * @param as assumed type of the element
     * @param <T> type of the returned element
     * @return element at the given index
     */
    <T extends JsonValue> T get( int index, Class<T> as );

    /**
     * @return the array elements as a uniform list of {@link String}
     * @throws IllegalArgumentException in case the node is not an array or the
     *         array has mixed elements
     */
    List<String> stringValues();

    /**
     * @return the array elements as a uniform list of {@link Number}
     * @throws IllegalArgumentException in case the node is not an array or the
     *         array has mixed elements
     */
    List<Number> numberValues();

    /**
     * @return the array elements as a uniform list of {@link Boolean}
     * @throws IllegalArgumentException in case the node is not an array or the
     *         array has mixed elements
     */
    List<Boolean> boolValues();

    default JsonValue get( int index )
    {
        return get( index, JsonValue.class );
    }

    default JsonNumber getNumber( int index )
    {
        return get( index, JsonNumber.class );
    }

    default JsonArray getArray( int index )
    {
        return get( index, JsonArray.class );
    }

    default JsonString getString( int index )
    {
        return get( index, JsonString.class );
    }

    default JsonBoolean getBoolean( int index )
    {
        return get( index, JsonBoolean.class );
    }

    default JsonObject getObject( int index )
    {
        return get( index, JsonObject.class );
    }

    default <E extends JsonValue> JsonList<E> getList( int index, Class<E> as )
    {
        return asList( getArray( index ), as );
    }

    default <E extends JsonValue> JsonMap<E> getMap( int index, Class<E> as )
    {
        return asMap( getObject( index ), as );
    }
}
