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

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Builder class that helps in building Cache instances. Sensible defaults are
 * in place which can be modified with a fluent builder api.
 * 
 * @author Ameen Mohamed
 *
 * @param <V> The Value type to be stored in cache
 */
public class SimpleCacheBuilder<V> implements CacheBuilder<V>
{
    private static final Log log = LogFactory.getLog( SimpleCacheBuilder.class );

    private long maximumSize;
    
    private int initialCapacity;

    private String region;

    private boolean refreshExpiryOnAccess;

    private long expiryInSeconds;

    private V defaultValue;
    
    private boolean expiryEnabled;
    
    private boolean disabled;

    public SimpleCacheBuilder()
    {
        // Applying sensible defaults explicitly
        this.maximumSize = -1;
        this.maximumSize = -1;
        this.region = "default";
        this.region = "default";
        this.refreshExpiryOnAccess = false;
        this.refreshExpiryOnAccess = false;
        this.expiryInSeconds = 0;
        this.expiryInSeconds = 0;
        this.defaultValue = null;
        this.defaultValue = null;
        this.expiryEnabled = false;
        this.expiryEnabled = false;
        this.disabled = false;
        this.initialCapacity = 16;
    }

    public CacheBuilder<V> withMaximumSize( long maximumSize )
    {
        if ( maximumSize < 0 )
        {
            throw new IllegalArgumentException( "MaximumSize cannot be negative" );
        }
        this.maximumSize = maximumSize;
        return this;
    }
    
    public CacheBuilder<V> withInitialCapacity( int initialCapacity )
    {
        if ( initialCapacity < 0 )
        {
            throw new IllegalArgumentException( "InitialCapacity cannot be negative" );
        }
        this.initialCapacity = initialCapacity;
        return this;
    }

    public CacheBuilder<V> forRegion( String region )
    {
        if ( region == null )
        {
            throw new IllegalArgumentException( "Region cannot be null" );
        }
        this.region = region;
        return this;
    }

    public CacheBuilder<V> expireAfterAccess( long duration, TimeUnit timeUnit )
    {
        if ( timeUnit == null )
        {
            throw new IllegalArgumentException( "TimeUnit cannot be null" );
        }
        this.expiryInSeconds = timeUnit.toSeconds( duration );
        this.refreshExpiryOnAccess = true;
        this.expiryEnabled = true;
        return this;
    }

    public CacheBuilder<V> expireAfterWrite( long duration, TimeUnit timeUnit )
    {
        if ( timeUnit == null )
        {
            throw new IllegalArgumentException( "TimeUnit cannot be null" );
        }
        this.expiryInSeconds = timeUnit.toSeconds( duration );
        this.refreshExpiryOnAccess = false;
        this.expiryEnabled = true;
        return this;
    }

    public CacheBuilder<V> withDefaultValue( V defaultValue )
    {
        this.defaultValue = defaultValue;
        return this;
    }
    
    public CacheBuilder<V> disabled()
    {
        this.disabled = true;
        return this;
    }

    /**
     * Creates and returns a {@link LocalCache}. If {@code maximumSize} is 0 or {@code disabled} is true then a
     * {@link NoOpCache} instance will be returned which does not cache anything.
     *  @return A cache instance based on the input
     *         parameters. Returns one of {@link LocalCache}
     *         or {@link NoOpCache}
     */
    public Cache<V> build()
    {
        if ( maximumSize == 0 || disabled )
        {
            log.info( String.format( "NoOp Cache instance created for region:'%s'", region ) );
            return new NoOpCache<V>( this );
        }
        else
        {
            log.info( String.format( "Simple Local Cache instance created for region:'%s'", region ) );
            return new LocalCache<V>( this );
        }
    }

    public long getMaximumSize()
    {
        return maximumSize;
    }
    
    public int getInitialCapacity()
    {
        return initialCapacity;
    }

    public String getRegion()
    {
        return region;
    }

    public boolean isRefreshExpiryOnAccess()
    {
        return refreshExpiryOnAccess;
    }
    
    public boolean isExpiryEnabled()
    {
        return expiryEnabled;
    }

    public boolean isDisabled()
    {
        return disabled;
    }
    
    public long getExpiryInSeconds()
    {
        return expiryInSeconds;
    }

    public V getDefaultValue()
    {
        return defaultValue;
    }

    public CacheBuilder<V> forceInMemory()
    {
        return this;
    }   
}