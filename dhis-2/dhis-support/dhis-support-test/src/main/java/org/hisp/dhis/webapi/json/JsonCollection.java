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
        class List implements JsonList<E>
        {

            @Override
            public E get( int index )
            {
                return array.get( index, as );
            }

            @Override
            public boolean exists()
            {
                return array.exists();
            }

            @Override
            public boolean isNull()
            {
                return array.isNull();
            }

            @Override
            public int size()
            {
                return array.size();
            }

            @Override
            public boolean isEmpty()
            {
                return array.isEmpty();
            }

            @Override
            public <T extends JsonValue> T as( Class<T> as )
            {
                return array.as( as );
            }
        }
        return new List();
    }
}
