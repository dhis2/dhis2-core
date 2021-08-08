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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A {@link JsonMap} with {@link JsonList} of elements.
 *
 * This needs a dedicated type as we cannot pass a {@link JsonMap} {@link Class}
 * with the generics of a {@link JsonList} of the element type otherwise.
 *
 * @author Jan Bernitt
 *
 * @param <E> type of the map list elements
 */
public interface JsonMultiMap<E extends JsonValue> extends JsonMap<JsonList<E>>
{

    /**
     * Convert this {@link JsonMultiMap} to a {@link Map} of {@link List} values
     * where the list elements are mapped from {@link JsonValue} by the provided
     * mapping {@link Function}.
     *
     * The order of the elements in the list are kept.
     *
     * @param mapper maps map list elements
     * @param <T> type of map value list elements
     * @return this {@link JsonMultiMap} as {@link Map}
     * @throws java.util.NoSuchElementException in case this value does not
     *         exist in the JSON document
     * @throws UnsupportedOperationException in case this node does exist but is
     *         not an object node
     */
    default <T> Map<String, List<T>> toMap( Function<E, T> mapper )
    {
        return toMap( mapper, null );
    }

    /**
     * Same as {@link #toMap(Function)} but the order of the elements in a
     * {@link List} is sorted by the provided order.
     *
     * @param mapper maps map list elements
     * @param order comparison used to sort the lists representing the map
     *        values
     * @param <T> type of map value list elements
     * @return this {@link JsonMultiMap} as {@link Map}
     * @throws java.util.NoSuchElementException in case this value does not
     *         exist in the JSON document
     * @throws UnsupportedOperationException in case this node does exist but is
     *         not an object node
     */
    default <T> Map<String, List<T>> toMap( Function<E, T> mapper, Comparator<T> order )
    {
        Map<String, List<T>> res = new LinkedHashMap<>();
        for ( String key : keys() )
        {
            List<T> list = get( key ).toList( mapper );
            if ( order != null )
            {
                list.sort( order );
            }
            res.put( key, list );
        }
        return res;
    }
}
