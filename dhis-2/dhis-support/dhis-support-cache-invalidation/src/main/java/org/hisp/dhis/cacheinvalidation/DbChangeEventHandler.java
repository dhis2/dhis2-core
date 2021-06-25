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
package org.hisp.dhis.cacheinvalidation;

import static org.hisp.dhis.cacheinvalidation.TableNameToEntityMapping.printEntityTableValue;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.PaginationCacheManager;
import org.hisp.dhis.cache.QueryCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.debezium.data.Envelope;
import io.debezium.engine.RecordChangeEvent;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Profile( { "!test", "!test-h2" } )
@Conditional( value = DebeziumCacheInvalidationEnabledCondition.class )
@Component
public class DbChangeEventHandler
{
    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private KnownTransactionsService knownTransactionsService;

    @Autowired
    private PaginationCacheManager paginationCacheManager;

    @Autowired
    private QueryCacheManager queryCacheManager;

    @Autowired
    private TableNameToEntityMapping TableNameToEntityMapping;

    protected void handleDbChange( RecordChangeEvent<SourceRecord> event )
    {
        try
        {
            tryEvictCache( event );
        }
        catch ( Exception e )
        {
            log.error( "Exception thrown during Debezium event handling, this is an unexpected error!", e );
        }
    }

    private void tryEvictCache( RecordChangeEvent<SourceRecord> event )
    {
        log.debug( "New RecordChangeEvent incoming! Event=" + event );

        SourceRecord sourceRecord = event.record();
        Objects.requireNonNull( sourceRecord, "Event record is null! Event=" + event );

        Struct payload = (Struct) sourceRecord.value();
        if ( payload == null )
        {
            log.debug( "Payload is null! Skipping event..." );
            return;
        }

        Long txId;
        try
        {
            txId = ((Struct) payload.get( "source" )).getInt64( "txId" );
            Objects.requireNonNull( txId, "TxId is null!" );
        }
        catch ( Exception e )
        {
            log.error( "Could not extract txId! Skipping event...", e );
            throw e;
        }

        if ( knownTransactionsService.isKnown( txId ) )
        {
            log.debug( "Incoming event txId is registered on this instance, skipping this event..." );
            return;
        }

        Envelope.Operation operation = Envelope.Operation.forCode( payload.getString( "op" ) );
        if ( operation == Envelope.Operation.READ )
        {
            log.debug( "Operation is READ, skipping event..." );
            return;
        }

        String[] topic = sourceRecord.topic().split( "\\." );
        if ( topic.length == 0 )
        {
            log.warn( "Topic is length is 0, skipping event..." );
            return;
        }

        String tableName = topic[topic.length - 1];

        List<Object[]> entityClasses = TableNameToEntityMapping.getEntities( tableName );
        Objects.requireNonNull( entityClasses, "Failed to look up entity in entity table! Table name=" + tableName );

        Serializable entityId = getEntityId( sourceRecord );
        Objects.requireNonNull( entityId, "Failed to extract entity id!" );

        evictExternalEntityChanges( txId, operation, entityClasses, entityId );
    }

    private Serializable getEntityId( SourceRecord sourceRecord )
    {
        Schema schema = sourceRecord.keySchema();
        List<Field> allIdFields = schema.fields();

        Field firstIdField = allIdFields.get( 0 );
        String idFieldName = firstIdField.name();

        Schema.Type idType = firstIdField.schema().type();
        Struct keyStruct = (Struct) sourceRecord.key();

        Serializable entityId = null;

        if ( Schema.Type.INT64 == idType )
        {
            entityId = keyStruct.getInt64( idFieldName );
        }
        else if ( Schema.Type.INT32 == idType )
        {
            entityId = keyStruct.getInt32( idFieldName );
        }

        return entityId;
    }

    private void evictExternalEntityChanges( Long txId, Envelope.Operation operation, List<Object[]> entityClasses,
        Serializable entityId )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( String.format( "Handling external event! "
                + "txId=%s, totalTxId=%s, operation=%s, entityClasses=%s, entityId=%s",
                txId, knownTransactionsService.size(), operation, printEntityTableValue( entityClasses ), entityId ) );
        }

        Class<?> firstEntityClass = (Class<?>) entityClasses.get( 0 )[0];
        Objects.requireNonNull( firstEntityClass, "Entity can't be null!" );

        if ( operation == Envelope.Operation.CREATE )
        {
            // Make sure queries will re-fetch
            queryCacheManager.evictQueryCache( sessionFactory.getCache(), firstEntityClass );
            paginationCacheManager.evictCache( firstEntityClass.getName() );

            evictCollections( entityClasses, entityId );

            // Try to fetch new entity so it might get cached
            try ( Session session = sessionFactory.openSession() )
            {
                session.get( firstEntityClass, entityId );
            }
            catch ( HibernateException e )
            {
                log.warn( "Failed to execute get query!", e );
            }
        }
        else if ( operation == Envelope.Operation.UPDATE
            || operation == Envelope.Operation.DELETE
            || operation == Envelope.Operation.TRUNCATE )
        {
            sessionFactory.getCache().evict( firstEntityClass, entityId );

            evictCollections( entityClasses, entityId );
        }
    }

    private void evictCollections( List<Object[]> entityAndRoles, Serializable id )
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
