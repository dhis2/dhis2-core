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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * A redis backed implementation of {@link Cache}. This implementation uses a
 * shared redis cache server for any number of instances.
 * 
 * @author Ameen Mohamed
 *
 */
public class RedisCache
    implements
    Cache
{

    private RedisTemplate<String, Serializable> redisTemplate;

    private boolean refreshExpriryOnAccess;

    private long expiryInSeconds;

    private String cacheRegion;

    private Serializable defaultValue;

    /**
     * Constructor for instantiating RedisCache.
     * 
     * @param region The cache region name which serves as a logical separation
     *        and will be added as a prefix to the keys specified.
     * @param refreshExpiryOnAccess Indicates whether the expiry (timeToLive)
     *        has to reset on every access
     * @param expiryInSeconds The time to live value in seconds
     * @param defaultValue Default value to be returned if no associated value
     *        for a key is found in the cache. The defaultValue will not be
     *        stored in the cache, but should be used as an indicator that the
     *        key did not have an associated value. By default the defaultValue
     *        is null
     * 
     */
    public RedisCache( RedisTemplate<String, Serializable> redisTemplate, String region, boolean refreshExpiryOnAccess,
        long expiryInSeconds, Serializable defaultValue )
    {
        this.redisTemplate = redisTemplate;
        this.refreshExpriryOnAccess = refreshExpiryOnAccess;
        this.expiryInSeconds = expiryInSeconds;
        this.cacheRegion = region;
        this.defaultValue = defaultValue;
    }

    @Override
    public Optional<Serializable> getIfPresent( String key )
    {
        String redisKey = generateActualKey( key );
        if ( refreshExpriryOnAccess )
        {
            redisTemplate.expire( redisKey, expiryInSeconds, TimeUnit.SECONDS );
        }
        return Optional.ofNullable( redisTemplate.boundValueOps( redisKey ).get() );
    }

    @Override
    public Optional<Serializable> get( String key )
    {
        String redisKey = generateActualKey( key );
        if ( refreshExpriryOnAccess )
        {
            redisTemplate.expire( redisKey, expiryInSeconds, TimeUnit.SECONDS );
        }
        return Optional
            .ofNullable( Optional.ofNullable( redisTemplate.boundValueOps( redisKey ).get() ).orElse( defaultValue ) );
    }

    @Override
    public Optional<Serializable> get( String key, Function<String, Serializable> mappingFunction )
    {

        String redisKey = generateActualKey( key );
        if ( refreshExpriryOnAccess )
        {
            redisTemplate.expire( redisKey, expiryInSeconds, TimeUnit.SECONDS );
        }
        Serializable value = redisTemplate.boundValueOps( redisKey ).get();

        if ( null == value )
        {
            value = mappingFunction.apply( key );

            if ( null != value )
            {
                redisTemplate.boundValueOps( redisKey ).set( value );
            }
        }

        return Optional.ofNullable( Optional.ofNullable( value ).orElse( defaultValue ) );
    }

    @Override
    public void put( String key, Serializable value )
    {
        if ( null == value )
        {
            throw new NullPointerException();
        }
        redisTemplate.boundValueOps( generateActualKey( key ) ).set( value );
    }

    @Override
    public void invalidate( String key )
    {
        redisTemplate.delete( generateActualKey( key ) );

    }

    private String generateActualKey( String key )
    {
        return cacheRegion.concat( ":" ).concat( key );
    }

    @Override
    public void invalidateAll()
    {
        // No operation

    }

}
