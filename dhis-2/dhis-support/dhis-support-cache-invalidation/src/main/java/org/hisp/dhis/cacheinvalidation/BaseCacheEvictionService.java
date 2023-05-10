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
package org.hisp.dhis.cacheinvalidation;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.PaginationCacheManager;
import org.hisp.dhis.cache.QueryCacheManager;
import org.hisp.dhis.cacheinvalidation.debezium.KnownTransactionsService;
import org.hisp.dhis.cacheinvalidation.debezium.TableNameToEntityMapping;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class BaseCacheEvictionService
{
    @Autowired
    protected SessionFactory sessionFactory;

    @Autowired
    protected KnownTransactionsService knownTransactionsService;

    @Autowired
    protected PaginationCacheManager paginationCacheManager;

    @Autowired
    protected QueryCacheManager queryCacheManager;

    @Autowired
    protected TableNameToEntityMapping tableNameToEntityMapping;

    @Autowired
    protected IdentifiableObjectManager idObjectManager;

    @Autowired
    protected TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    protected TrackedEntityService trackedEntityService;

    @Autowired
    protected PeriodService periodService;

    protected void tryFetchNewEntity( Serializable entityId, Class<?> entityClass )
    {
        try ( Session session = sessionFactory.openSession() )
        {
            session.get( entityClass, entityId );
        }
        catch ( Exception e )
        {
            log.warn(
                String.format( "Fetching new entity failed, failed to execute get query! entityId=%s, entityClass=%s",
                    entityId, entityClass ),
                e );
            if ( e instanceof HibernateException )
            {
                log.debug( "tryFetchNewEntity caused a Hibernate exception: " + e.getMessage() );
                // Ignore HibernateExceptions, as they are expected.
                return;
            }

            throw e;
        }
    }

    /**
     * It evicts the entity and all its collections from the cache
     *
     * @param entityAndRoles A list of Object arrays, each containing the entity
     *        class and the role name.
     * @param id The id of the entity to evict
     */
    protected void evictCollections( List<Object[]> entityAndRoles, Serializable id )
    {
        Object[] firstEntityAndRole = entityAndRoles.get( 0 );
        Objects.requireNonNull( firstEntityAndRole, "firstEntityAndRole can't be null!" );

        // It's only a collection if we also have a role mapped
        if ( firstEntityAndRole.length == 2 )
        {
            for ( Object[] entityAndRole : entityAndRoles )
            {
                Class<?> eKlass = (Class<?>) entityAndRole[0];
                sessionFactory.getCache().evict( eKlass, id );
                queryCacheManager.evictQueryCache( sessionFactory.getCache(), eKlass );
                paginationCacheManager.evictCache( eKlass.getName() );

                String role = (String) entityAndRole[1];
                sessionFactory.getCache().evictCollectionData( role, id );
            }
        }
    }
}
