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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.webapi.json.JsonDocument.JsonNodeType;

/**
 * API of a JSON tree as it actually exist in a HTTP response with a JSON
 * payload.
 * <p>
 * Operations are lazily evaluated to make working with the JSON tree efficient.
 *
 * Trying use operations that are not supported for the {@link JsonNodeType},
 * like access the {@link #members()} or {@link #elements()} of a node that is
 * not an object or array respectively, will throw an
 * {@link UnsupportedOperationException} error immediately.
 *
 * @author Jan Bernitt
 */
public interface JsonNode extends Serializable
{
    /**
     * @return the type of the node as derived from the node beginning
     */
    JsonNodeType getType();

    /**
     * Size of an array of number of object members.
     *
     * This is preferable to calling {@link #value()} or {@link #members()} or
     * {@link #elements()} when size is only property of interest as it might
     * have better performance.
     *
     * @return number of elements in an array or number of fields in an object,
     *         otherwise undefined
     * @throws UnsupportedOperationException when this node in neither an array
     *         nor an object
     */
    default int size()
    {
        throw new UnsupportedOperationException( getType() + " node has no size property." );
    }

    /**
     * Whether or not an array or object has no elements or members.
     *
     * This is preferable to calling {@link #value()} or {@link #members()} or
     * {@link #elements()} when emptiness is only property of interest as it
     * might have better performance.
     *
     * @return true if an array or object has no elements/fields, otherwise
     *         undefined
     * @throws UnsupportedOperationException when this node in neither an array
     *         nor an object
     */
    default boolean isEmpty()
    {
        throw new UnsupportedOperationException( getType() + " node has no empty property." );
    }

    /**
     * The value depends on the {@link #getType()}:
     * <ul>
     * <li>{@link JsonNodeType#NULL} returns {@code null}</li>
     * <li>{@link JsonNodeType#BOOLEAN} returns {@link Boolean}</li>
     * <li>{@link JsonNodeType#STRING} returns {@link String}</li>
     * <li>{@link JsonNodeType#NUMBER} returns either {@link Integer},
     * {@link Long} or {@link Double}</li>
     * <li>{@link JsonNodeType#ARRAY} returns an {@link java.util.List} of
     * {@link JsonNode}</li>
     * <li>{@link JsonNodeType#ARRAY} returns a {@link Map} or {@link String}
     * keys and {@link JsonNode} values</li>
     * </ul>
     *
     * @return the nodes values as described in the above table
     */
    Serializable value();

    /**
     * OBS! Only defined when this node is of type {@link JsonNodeType#OBJECT})
     *
     * @return this {@link #value()} as as {@link Map}
     */
    default Map<String, JsonNode> members()
    {
        throw new UnsupportedOperationException( getType() + " node has no members property." );
    }

    /**
     * OBS! Only defined when this node is of type {@link JsonNodeType#ARRAY}).
     *
     * @return this {@link #value()} as as {@link List}
     */
    default List<JsonNode> elements()
    {
        throw new UnsupportedOperationException( getType() + " node has no elements property." );
    }

    /*
     * API about this node in the context of the underlying JSON document
     */

    /**
     * @return path within the overall content this node represents
     */
    String getPath();

    /**
     * @return the plain JSON of this node as defined in the overall content
     */
    String getDeclaration();

    /**
     * @return offset or index in the overall content where this node starts
     *         (inclusive, points to first index that belongs to the node)
     */
    int startIndex();

    /**
     * @return offset or index in the overall content where this node ends
     *         (exclusive, points to first index after the node)
     */
    int endIndex();

    /*
     * API about using this node to create new documents
     */

    /**
     * @return This node as a new independent JSON document where this node is
     *         the new root of that document.
     */
    default JsonNode extract()
    {
        return new JsonDocument( getDeclaration() ).get( "$" );
    }

    /**
     * Replace this node and return the root of the document where this node got
     * replaced.
     *
     * @param json The JSON used instead of the on this node represents
     * @return A new document root where this node got replaced with the
     *         provided JSON
     */
    JsonNode replaceWith( String json );
}
