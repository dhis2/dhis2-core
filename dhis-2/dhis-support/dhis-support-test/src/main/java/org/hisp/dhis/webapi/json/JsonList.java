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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import com.google.common.collect.Iterators;

/**
 * A {@link JsonList} is nothing else then a {@link JsonArray} with "typed"
 * uniform elements.
 *
 * @author Jan Bernitt
 *
 * @param <E> type of the list elements
 */
public interface JsonList<E extends JsonValue> extends JsonCollection, Iterable<E>
{
    /**
     * A typed variant of {@link JsonArray#get(int)}, equivalent to
     * {@link JsonArray#get(int, Class)} where 2nd parameter is the type
     * parameter E.
     *
     * @param index index to access
     * @return element at the provided index
     */
    E get( int index );

    @Override
    @Nonnull
    default Iterator<E> iterator()
    {
        int size = size();
        return new Iterator<E>()
        {
            int index = 0;

            @Override
            public boolean hasNext()
            {
                return index < size;
            }

            @Override
            public E next()
            {
                E e = get( index++ );
                if ( !e.exists() )
                {
                    throw new NoSuchElementException();
                }
                return e;
            }
        };
    }

    default Iterable<E> filtered( Predicate<? super E> filter )
    {
        return () -> Iterators.filter( iterator(), filter::test );
    }

    /**
     * @return this list as a {@link Stream}
     */
    default Stream<E> stream()
    {
        return StreamSupport.stream( spliterator(), false );
    }

    /**
     * Map this {@link JsonList} to a plain {@link List}. To map the element
     * from a {@link JsonValue} to a plain value a mapper {@link Function} is
     * provided.
     *
     * @param mapper maps from {@link JsonValue} to plain value
     * @param <T> type of result list elements
     * @return this list mapped to a {@link List} of elements mapped by the
     *         provided mapper function from the {@link JsonValue}s of this
     *         {@link JsonList}.
     */
    default <T> List<T> toList( Function<E, T> mapper )
    {
        return stream().map( mapper ).collect( Collectors.toList() );
    }
}
