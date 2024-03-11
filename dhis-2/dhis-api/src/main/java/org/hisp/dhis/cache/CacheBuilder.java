package org.hisp.dhis.cache;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import java.util.concurrent.TimeUnit;

/**
 * A Builder class that helps in building Cache instances. Sensible defaults are
 * in place which can be modified with a fluent builder api.
 * 
 * @author Ameen Mohamed
 *
 * @param <V> The Value type to be stored in cache
 */
public interface CacheBuilder<V>
{
    /**
     * Set the maximum size for the cache instance to be built. If set to 0, no
     * caching will take place. Cannot be a negative value.
     * 
     * @param maximumSize The maximum size
     * @return The builder instance
     * @throws IllegalArgumentException if specified maximumSize is a negative
     *         value.
     */
    public CacheBuilder<V> withMaximumSize( long maximumSize );

    /**
     * Sets the minimum total size for the internal data structures.
     *
     * @param initialCapacity minimum total size for the internal data structures
     * @return this {@code CacheBuilder} instance (for chaining)
     * @throws IllegalArgumentException if {@code initialCapacity} is negative
     */
    public CacheBuilder<V> withInitialCapacity( int initialCapacity );

    /**
     * Set the cacheRegion for the cache instance to be built. If not specified
     * default is "default" region.
     * 
     * @param region The cache region name to be used.
     * @return The builder instance.
     * @throws IllegalArgumentException if specified region is null.
     */
    public CacheBuilder<V> forRegion( String region );

    /**
     * Configure the cache instance to expire the keys, if the expiry duration
     * elapses after last access.
     * 
     * @param duration The duration
     * @param timeUnit The time unit of the duration
     * @return The builder instance.
     * @throws IllegalArgumentException if specified timeUnit is null.
     */
    public CacheBuilder<V> expireAfterAccess( long duration, TimeUnit timeUnit );
    /**
     * Configure the cache instance to expire the keys, if the expiry duration
     * elapses after writing. The key expires irrespective of the last access.
     * 
     * @param duration The duration
     * @param timeUnit The time unit of the duration
     * @return The builder instance.
     * @throws IllegalArgumentException if specified timeUnit is null.
     */
    public CacheBuilder<V> expireAfterWrite( long duration, TimeUnit timeUnit );

    /**
     * Configure the cache instance to have a default value if the key does not have an associated value in cache. The default value will not be stored in the cache.
     * 
     * @param defaultValue The default value
     * @return The builder instance.
     */
    public CacheBuilder<V> withDefaultValue( V defaultValue );

    /**
     * Configure the cache instance to use local inmemory storage even in clustered or standalone environment.
     * Ideally used in scenarios where stale data is not critical and faster lookup is preferred.
     * 
     * @return The builder instance.
     */
    public CacheBuilder<V> forceInMemory();

    /**
     * Configure the cache instance to disable caching.
     * 
     * @return The builder instance.
     */
    public CacheBuilder<V> disabled();

    /**
     * Construct the cache instance based on the input parameters and return it.
     * @return The cache instance created.
     */
    public Cache<V> build();

    /**
     * Getter for maximumSize
     * @return the maximumSize value set in the builder
     */
    public long getMaximumSize();

    /**
     * Getter for initialCapacity
     * @return the initialCapacity value set in the builder
     */
    public int getInitialCapacity();

    /**
     * Getter for region
     * @return the region set in the builder
     */
    public String getRegion();

    /**
     * Getter for refreshExpiryOnAccess
     * @return the refreshExpiryOnAccess flag set in the builder
     */
    public boolean isRefreshExpiryOnAccess();

    /**
     * Getter for expiryEnabled
     * @return the expiryEnabled flag set in the builder
     */
    public boolean isExpiryEnabled();

    /**
     * Getter for expiryInSeconds
     * @return the expiryInSeconds value set in the builder
     */
    public long getExpiryInSeconds();

    /**
     * Getter for defaultvalue
     * @return the defaultvalue value set in the builder
     */
    public V getDefaultValue();

} 