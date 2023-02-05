/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.cache;

import java.util.concurrent.Callable;

import lombok.RequiredArgsConstructor;

/**
 * Caches a single value if/when needed. This avoids the overhead of getting an
 * object whether needed or not. The object is only fetched once and is cached
 * for possible reuse.
 * <p>
 * The method to get the object is defined when the cache is constructed, which
 * makes it possible to get the object from a context that that is not reachable
 * by the coller--for example if the object is fetched from a service but the
 * caller does not have that service in scope.
 *
 * @author Jim Grace
 */
@RequiredArgsConstructor
public class SingleValueCache<V>
{
    private final Callable<V> callable;

    private V value = null;

    /**
     * Gets the single value, fetching it if necessary and caching it for
     * subsequent use.
     *
     * @return the value
     */
    public V get()
    {
        if ( value == null )
        {
            try
            {
                value = callable.call();
            }
            catch ( Exception ex )
            {
                throw new RuntimeException( ex );
            }
        }

        return value;
    }
}
