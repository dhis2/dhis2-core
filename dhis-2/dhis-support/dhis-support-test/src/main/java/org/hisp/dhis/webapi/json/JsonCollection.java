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
 * Common base class for JSON nodes that have children.
 *
 * These are their
 * <ul>
 * <li>{@link JsonArray}s, or</li>
 * <li>{@link JsonObject}s</li>
 * </ul>
 *
 * Each has their typed wrapper type:
 * <ul>
 * <li>{@link JsonList}</li>
 * <li>{@link JsonMap}</li>
 * </ul>
 *
 * @author Jan Bernitt
 */
public interface JsonCollection extends JsonValue
{

    /**
     * @return true, in case the collection exists but has no elements and is
     *         not {@code null}
     * @throws java.util.NoSuchElementException in case this value does not
     *         exist in the content
     * @throws IllegalArgumentException in case the value does exist but is not
     *         a collection
     */
    boolean isEmpty();

    /**
     * @return the number of elements in the collection
     * @throws java.util.NoSuchElementException in case this value does not
     *         exist in the content
     * @throws IllegalArgumentException in case the value does exist but is not
     *         a collection
     */
    int size();

    default <E extends JsonValue> JsonList<E> asList( JsonArray array, Class<E> as )
    {
        class List extends Collection<JsonArray> implements JsonList<E>
        {
            List( JsonArray wrapped )
            {
                super( wrapped );
            }

            @Override
            public E get( int index )
            {
                return array.get( index, as );
            }
        }
        return new List( array );
    }

    default <E extends JsonValue> JsonMap<E> asMap( JsonObject object, Class<E> as )
    {
        class Map extends Collection<JsonObject> implements JsonMap<E>
        {
            Map( JsonObject wrapped )
            {
                super( wrapped );
            }

            @Override
            public E get( String key )
            {
                return wrapped.get( key, as );
            }
        }
        return new Map( object );
    }

    abstract class Collection<T extends JsonCollection> implements JsonCollection
    {
        protected final T wrapped;

        protected Collection( T wrapped )
        {
            this.wrapped = wrapped;
        }

        @Override
        public final boolean isEmpty()
        {
            return wrapped.isEmpty();
        }

        @Override
        public final int size()
        {
            return wrapped.size();
        }

        @Override
        public final boolean exists()
        {
            return wrapped.exists();
        }

        @Override
        public final boolean isNull()
        {
            return wrapped.isNull();
        }

        @Override
        public final boolean isArray()
        {
            return wrapped.isArray();
        }

        @Override
        public final boolean isObject()
        {
            return wrapped.isObject();
        }

        @Override
        public final <V extends JsonValue> V as( Class<V> as )
        {
            return wrapped.as( as );
        }
    }
}
