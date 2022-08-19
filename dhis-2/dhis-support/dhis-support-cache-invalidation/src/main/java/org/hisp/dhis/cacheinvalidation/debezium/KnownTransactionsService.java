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
package org.hisp.dhis.cacheinvalidation.debezium;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.FlushMode;
import org.hibernate.event.spi.AbstractEvent;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Service responsible for registering local transaction ID in a lookup table.
 * The hash table is a {@link Cache<Long,Boolean>} with a lifetime of:
 * {@link KnownTransactionsService#LOCAL_TXID_CACHE_TIME_MIN}
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
public class KnownTransactionsService
{
    public static final int LOCAL_TXID_CACHE_TIME_MIN = 15;

    @Autowired
    private DhisConfigurationProvider dhisConfig;

    private Cache<Long, Boolean> applicationTransactions;

    public void registerEvent( AbstractEvent event )
    {
        if ( !dhisConfig.isEnabled( ConfigurationKey.DEBEZIUM_ENABLED ) )
        {
            return;
        }

        try
        {
            Number txId = (Number) event.getSession().createNativeQuery( "SELECT txid_current()" )
                .setFlushMode( FlushMode.MANUAL )
                .getSingleResult();

            registerTransactionId( txId.longValue() );
        }
        catch ( Exception e )
        {
            log.error( "Failed to register Hibernate session event!", e );
        }
    }

    public void registerTransactionId( long txId )
    {
        if ( dhisConfig.isEnabled( ConfigurationKey.DEBEZIUM_ENABLED ) )
        {
            log.debug( "Register local txId=" + txId );
            buildIfNull();
            applicationTransactions.put( txId, true );
        }
    }

    private void buildIfNull()
    {
        if ( applicationTransactions == null )
        {
            applicationTransactions = CacheBuilder.newBuilder()
                .expireAfterAccess( LOCAL_TXID_CACHE_TIME_MIN, TimeUnit.MINUTES )
                .build();
        }
    }

    public boolean isKnown( long txId )
    {
        buildIfNull();
        return Boolean.TRUE.equals( applicationTransactions.getIfPresent( txId ) );
    }

    public Long size()
    {
        return applicationTransactions.size();
    }
}