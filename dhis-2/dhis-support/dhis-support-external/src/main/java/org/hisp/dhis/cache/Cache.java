package org.hisp.dhis.cache;
/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;

public interface Cache
{
    /**
     * Returns the value associated with the {@code key} in this cache instance,
     * or {@code Optional.empty()} if there is no cached value for the
     * {@code key}.
     * <p>
     * Note: This method will NOT return the defaultValue in case of absence of
     * associated cache value.
     * </p>
     *
     * @param key the key whose associated value is to be returned from the
     *        cache instance
     * 
     * @return the value wrapped in Optional, to which the specified key is
     *         mapped in the cache instance or {@code Optional.empty()} if this
     *         cache contains no mapping for the key
     * @throws NullPointerException if the specified key is null
     */
    Optional<Serializable> getIfPresent( String key );

    /**
     * Returns the value associated with the {@code key} in this cache instance,
     * or {@code defaultValue} if there is no cached value for the {@code key}.
     * 
     * <p>
     * Note: This method will return the defaultValue in case of absence of
     * associated cache value. But will not store the default value into the
     * cache.
     * </p>
     *
     * @param key the key whose associated value is to be returned from the
     *        cache instance
     * 
     * @return the value wrapped in Optional, to which the specified key is
     *         mapped in the cache instance or {@code Optional of defaultValue}
     *         if this cache contains no mapping for the key
     * @throws NullPointerException if the specified key is null
     */
    Optional<Serializable> get( String key );

    /**
     * Returns the value associated with the {@code key} in this cache instance,
     * obtaining that value from the {@code mappingFunction} if necessary. This
     * method provides a simple substitute for the conventional "if cached,
     * return; otherwise create, cache and return" pattern.
     * <p>
     * If the specified key is not already associated with a value, attempts to
     * compute its value using the given mapping function and enters it into
     * this cache unless {@code null}. The computation should be short and
     * simple, and must not attempt to update any other mappings of this cache.
     * </p>
     * 
     * <p>
     * Note: This method will return the defaultValue in case of absence of
     * associated cache value. But will not store the default value into the
     * cache.
     * </p>
     * 
     *
     * @param key the key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return an optional containing current (existing or computed) value
     *         associated with the specified key, or Optional.empty() if the
     *         computed value is null
     * @throws NullPointerException if the specified key or mappingFunction is
     *         null
     * @throws IllegalStateException if the computation detectably attempts a
     *         recursive update to this cache that would otherwise never
     *         complete
     * @throws RuntimeException or Error if the mappingFunction does so, in
     *         which case the mapping is left unestablished
     */
    Optional<Serializable> get( String key, Function<String, Serializable> mappingFunction );

    /**
     * Associates the {@code value} with the {@code key} in this cache. If the
     * cache previously contained a value associated with the {@code key}, the
     * old value is replaced by the new {@code value}.
     * <p>
     * Prefer {@link #get(String, Function)} when using the conventional "if
     * cached, return; otherwise create, cache and return" pattern.
     *
     * @param key the key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @throws NullPointerException if the specified key or value is null
     */
    void put( String key, Serializable value );

    /**
     * Discards any cached value for the {@code key}. The behavior of this
     * operation is undefined for an entry that is being loaded and is otherwise
     * not present.
     *
     * @param key the key whose mapping is to be removed from the cache
     * @throws NullPointerException if the specified key is null
     */
    void invalidate( String key );

    /**
     * Discards all entries in this cache instance. If a shared cache is used,
     * this method does not clear anything.
     */
    void invalidateAll();

}
