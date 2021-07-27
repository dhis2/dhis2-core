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
 * The {@link JsonValue} is a virtual read-only view for JSON responses.
 *
 * As usual there are specific node types for the JSON building blocks:
 * <ul>
 * <li>{@link JsonObject}</li>
 * <li>{@link JsonArray}</li>
 * <li>{@link JsonString}</li>
 * <li>{@link JsonNumber}</li>
 * <li>{@link JsonBoolean}</li>
 * </ul>
 * In addition there is {@link JsonCollection} as a common base type of
 * {@link JsonObject} and {@link JsonArray} and {@link JsonPrimitive} as common
 * base type of {@link JsonString}, {@link JsonNumber} and {@link JsonBoolean}.
 *
 * In addition {@link JsonList} is a typed JSON array of uniform elements (which
 * can be understood as a typed wrapper around a {@link JsonArray}.
 *
 * Similarly {@link JsonMap} is a typed JSON object map of uniform values (which
 * can be understood as a typed wrapper around a {@link JsonObject}.
 *
 * The API is designed to:
 * <ul>
 * <li>be extended by further types extending {@link JsonValue}, such as
 * {@link JsonDate} but also further specific object types</li>
 * <li>fail at point of assertion. This means traversing the virtual tree does
 * not cause errors unless explicitly provoked.</li>
 * <li>be implemented by a single class which only builds a lookup path and
 * checks or provides the leaf values on demand. Interfaces not directly
 * implemented by this class are dynamically created using a
 * {@link java.lang.reflect.Proxy}.</li>
 * </ul>
 *
 * @author Jan Bernitt
 */
public interface JsonValue
{
    /**
     * A property exists when it is part of the JSON response. This means it can
     * be declared JSON {@code null}. Only a path that does not exist returns
     * false.
     *
     * @return true, if the value exists, else false
     */
    boolean exists();

    /**
     * @return true if the value exists and is defined JSON {@code null}
     * @throws java.util.NoSuchElementException in case this value does not
     *         exist in the JSON document
     */
    boolean isNull();

    /**
     * @return true if this JSON node either does not exist at all or is defined
     *         as JSON {@code null}, otherwise false
     */
    default boolean isUndefined()
    {
        return !exists() || isNull();
    }

    /**
     * @return true if the value exists and is a JSON array node (empty or not)
     *         but not JSON {@code null}
     * @throws java.util.NoSuchElementException in case this value does not
     *         exist in the JSON document
     */
    boolean isArray();

    /**
     * @return true if the value exists and is an JSON object node (empty or
     *         not) but not JSON {@code null}
     * @throws java.util.NoSuchElementException in case this value does not
     *         exist in the JSON document
     */
    boolean isObject();

    /**
     * "Cast" this JSON value to a more specific type. Note that any type can be
     * switched to any other type. Types here are just what we believe to be
     * true. They are only here to guide us, not assert existence.
     *
     * Whether or not assumptions are actually true is determined when leaf
     * values are accessed.
     *
     * @param as assumed value type
     * @param <T> value type returned
     * @return this object as the provided type, this might mean this object is
     *         wrapped as the provided type or literally cast.
     */
    <T extends JsonValue> T as( Class<T> as );

    /**
     * This value as a list of uniform elements (view on JSON array).
     *
     * @param elementType assumed value element type
     * @param <E> type of list elements
     * @return list view of this value (assumes array)
     */
    default <E extends JsonValue> JsonList<E> asList( Class<E> elementType )
    {
        return JsonCollection.asList( as( JsonArray.class ), elementType );
    }

    /**
     * This value as map of uniform values (view on JSON object).
     *
     * @param valueType assumed map value type
     * @param <V> type of map values
     * @return map view of this value (assumes object)
     */
    default <V extends JsonValue> JsonMap<V> asMap( Class<V> valueType )
    {
        return JsonCollection.asMap( as( JsonObject.class ), valueType );
    }

    /**
     * This value as map of list value with of uniform elements (view on JSON
     * object).
     *
     * @param valueType assumed map value type
     * @param <V> type of map values
     * @return map view of this value (assumes object)
     */
    default <V extends JsonValue> JsonMultiMap<V> asMultiMap( Class<V> valueType )
    {
        return JsonCollection.asMultiMap( as( JsonObject.class ), valueType );
    }

    /**
     * Access the node in the JSON document. This can be the low level API that
     * is concerned with extraction by path.
     *
     * This might be useful in test to access the
     * {@link JsonNode#getDeclaration()} to modify and reuse it.
     *
     * @return the underlying {@link JsonNode} in the overall JSON document if
     *         it exists
     * @throws java.util.NoSuchElementException in case this value does not
     *         exist in the JSON document
     */
    JsonNode node();
}
