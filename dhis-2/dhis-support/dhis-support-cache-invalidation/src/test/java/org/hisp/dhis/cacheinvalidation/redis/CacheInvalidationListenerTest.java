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
package org.hisp.dhis.cacheinvalidation.redis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.hibernate.SessionFactory;
import org.hibernate.cache.internal.DisabledCaching;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hisp.dhis.cache.PaginationCacheManager;
import org.hisp.dhis.cache.QueryCacheManager;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CacheInvalidationListenerTest
{

    @Mock
    protected SessionFactory sessionFactory;

    @Mock
    protected PaginationCacheManager paginationCacheManager;

    @Mock
    protected QueryCacheManager queryCacheManager;

    @Mock
    protected IdentifiableObjectManager idObjectManager;

    @Mock
    protected TrackedEntityAttributeService trackedEntityAttributeService;

    @Mock
    protected TrackedEntityService trackedEntityService;

    @Mock
    protected PeriodService periodService;

    @Mock
    protected DisabledCaching disabledCaching;

    private CacheInvalidationListener cacheInvalidationListener;

    private AutoCloseable closeable;

    @Mock
    private ServiceRegistryImplementor ServiceRegistryImplementor;

    @BeforeEach
    void setUp()
    {
        closeable = MockitoAnnotations.openMocks( this );

        cacheInvalidationListener = new CacheInvalidationListener(
            sessionFactory,
            paginationCacheManager,
            queryCacheManager,
            idObjectManager,
            trackedEntityAttributeService,
            trackedEntityService,
            periodService,
            "SERVER_A" );

        lenient().when( sessionFactory.getCache() ).thenReturn( disabledCaching );

    }

    @AfterEach
    void closeService()
        throws Exception
    {
        closeable.close();
    }

    @Test
    @DisplayName( "Should not call evict cache on COLLECTION messages" )
    void testCollectionMessage()
    {
        String message = "SERVER_B" + ":" + "COLLECTION" + ":" + "org.hisp.dhis.user.User" + ":" + "ROLE" + ":" + "1";
        cacheInvalidationListener.message( CacheInvalidationConfiguration.CHANNEL_NAME, message );

        verify( queryCacheManager, times( 0 ) ).evictQueryCache( any(), any() );
        verify( sessionFactory.getCache(), times( 1 ) ).evictCollectionData( any(), any() );
        verify( paginationCacheManager, times( 0 ) ).evictCache( anyString() );
    }

    @Test
    @DisplayName( "Should not call evict cache on INSERT messages" )
    void testInsertMessage()
    {
        String message = "SERVER_B" + ":" + "INSERT" + ":" + "org.hisp.dhis.user.User" + ":" + "1";
        cacheInvalidationListener.message( CacheInvalidationConfiguration.CHANNEL_NAME, message );

        verify( queryCacheManager, times( 1 ) ).evictQueryCache( any(), any() );
        verify( sessionFactory.getCache(), times( 0 ) ).evict( any(), any() );
        verify( paginationCacheManager, times( 1 ) ).evictCache( anyString() );
    }

    @Test
    @DisplayName( "Should call evict cache on UPDATE messages" )
    void testUpdateMessage()
    {
        String message = "SERVER_B" + ":" + "UPDATE" + ":" + "org.hisp.dhis.user.User" + ":" + "1";
        cacheInvalidationListener.message( CacheInvalidationConfiguration.CHANNEL_NAME, message );

        verify( sessionFactory.getCache(), times( 1 ) ).evict( any(), any() );
    }

    @Test
    @DisplayName( "Should call evict cache on DELETE messages" )
    void testDeleteMessage()
    {
        String message = "SERVER_B" + ":" + "DELETE" + ":" + "org.hisp.dhis.user.User" + ":" + "1";
        cacheInvalidationListener.message( CacheInvalidationConfiguration.CHANNEL_NAME, message );

        verify( queryCacheManager, times( 1 ) ).evictQueryCache( any(), any() );
        verify( sessionFactory.getCache(), times( 1 ) ).evict( any(), any() );
        verify( paginationCacheManager, times( 1 ) ).evictCache( anyString() );
    }
}
