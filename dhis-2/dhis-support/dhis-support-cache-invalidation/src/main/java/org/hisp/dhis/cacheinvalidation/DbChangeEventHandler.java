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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.metamodel.EntityType;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metamodel.internal.MetamodelImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hisp.dhis.cache.PaginationCacheManager;
import org.hisp.dhis.cache.QueryCacheManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.debezium.data.Envelope;
import io.debezium.engine.RecordChangeEvent;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
public class DbChangeEventHandler
{
    @PersistenceUnit
    private EntityManagerFactory emf;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private KnownTransactionsService knownTransactionsService;

    @Autowired
    private PaginationCacheManager paginationCacheManager;

    @Autowired
    private QueryCacheManager queryCacheManager;

    private final Map<String, Class<?>> entityToTableNames = new HashMap<>();

    protected void handleDbChange( RecordChangeEvent<SourceRecord> event )
    {
        init();

        try
        {
            tryEvictCache( event );
        }
        catch ( Exception e )
        {
            log.error( "Failed to evict cache!", e );
        }
    }

    private void tryEvictCache( RecordChangeEvent<SourceRecord> event )
    {
        SourceRecord record = event.record();

        log.info( "New RecordChangeEvent incoming! topic=" + record.topic() );

        String[] topic = record.topic().split( "\\." );

        if ( topic.length == 0 )
        {
            return;
        }

        String tableName = topic[topic.length - 1];
        Class<?> entityClass = entityToTableNames.get( tableName );

        if ( entityClass == null )
        {
            log.info( "No entityClass mapped to the table name, ignoring event! tablename=" + tableName );
            return;
        }

        Schema schema = record.keySchema();
        List<Field> allIdFields = schema.fields();
        Field idField = allIdFields.get( 0 );
        String idFieldName = idField.name();

        Schema.Type type = idField.schema().type();

        Struct keyStruct = (Struct) record.key();
        Serializable id = null;
        Object o1 = keyStruct.get( idField );
        Field field = keyStruct.schema().field( idFieldName );

        String cname = entityClass.getName();
        String cname3 = TrackedEntityAttributeValue.class.getName();
        if ( cname.equals( cname3 ) )
        {
            Field idField1 = allIdFields.get( 0 );
            Field idField2 = allIdFields.get( 1 );
            Long o = (Long) keyStruct.get( idField1 );
            Long o2 = (Long) keyStruct.get( idField2 );
            id = new TrackedEntityAttributeValue( new TrackedEntityAttribute( o2 ), new TrackedEntityInstance( o ) );
        }
        else if ( Schema.Type.INT64 == type )
        {
            id = keyStruct.getInt64( idFieldName );
        }
        else if ( Schema.Type.INT32 == type )
        {
            id = keyStruct.getInt32( idFieldName );
        }

        // org.apache.kafka.connect.errors.DataException: Field
        // 'trackedentityprogramownerid' is not of type INT64
        // at org.apache.kafka.connect.data.Struct.getCheckType(Struct.java:263)
        // at org.apache.kafka.connect.data.Struct.getInt64(Struct.java:130)
        // at
        // org.hisp.dhis.cacheinvalidation.DbChangeEventHandler.tryEvictCache(DbChangeEventHandler.java:124)
        // at
        // org.hisp.dhis.cacheinvalidation.DbChangeEventHandler.handleDbChange(DbChangeEventHandler.java:88)
        // at
        // io.debezium.embedded.ConvertingEngineBuilder.lambda$notifying$0(ConvertingEngineBuilder.java:72)
        // at
        // io.debezium.embedded.EmbeddedEngine$1.handleBatch(EmbeddedEngine.java:473)
        // at io.debezium.embedded.EmbeddedEngine.run(EmbeddedEngine.java:821)
        // at
        // io.debezium.embedded.ConvertingEngineBuilder$2.run(ConvertingEngineBuilder.java:188)
        // at
        // java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        // at
        // java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        // at java.base/java.lang.Thread.run(Thread.java:829)

        Struct payload = (Struct) record.value();
        if ( payload == null )
        {
            log.warn( "payload is null!" );
            return;
        }

        Long txId = null;

        try
        {
            txId = ((Struct) payload.get( "source" )).getInt64( "txId" );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw e;
        }
        // java.lang.NullPointerException
        // at
        // org.hisp.dhis.cacheinvalidation.DbChangeEventHandler.tryEvictCache(DbChangeEventHandler.java:127)
        // at
        // org.hisp.dhis.cacheinvalidation.DbChangeEventHandler.handleDbChange(DbChangeEventHandler.java:88)
        // at
        // io.debezium.embedded.ConvertingEngineBuilder.lambda$notifying$0(ConvertingEngineBuilder.java:72)
        // at
        // io.debezium.embedded.EmbeddedEngine$1.handleBatch(EmbeddedEngine.java:473)
        // at io.debezium.embedded.EmbeddedEngine.run(EmbeddedEngine.java:821)
        // at
        // io.debezium.embedded.ConvertingEngineBuilder$2.run(ConvertingEngineBuilder.java:188)
        // at
        // java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        // at
        // java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        // at java.base/java.lang.Thread.run(Thread.java:829)
        Envelope.Operation operation = Envelope.Operation.forCode( payload.getString( "op" ) );

        if ( !knownTransactionsService.isKnown( txId ) )
        {
            if ( operation == Envelope.Operation.UPDATE || operation == Envelope.Operation.DELETE )
            {
                log.info( String.format( "RecordChangeEvent is an external %s event! "
                    + "Trying to evict; entityClass=%s, id=%s", operation.name(), entityClass, id ) );

                sessionFactory.getCache().evict( entityClass, id );
            }
            else if ( operation == Envelope.Operation.CREATE )
            {
                log.info( String.format( "RecordChangeEvent is an external %s event! "
                    + "Trying to find new entity and evict query cache; entityClass=%s", operation.name(),
                    entityClass ) );

                queryCacheManager.evictQueryCache( sessionFactory.getCache(), entityClass );

                paginationCacheManager.evictCache( entityClass.getName() );

                try ( Session session = sessionFactory.openSession() )
                {
                    Object o = session.get( entityClass, id );
                    log.info( "Found new object:" + o );
                }
                catch ( HibernateException e )
                {
                    log.error( "Failed to execute get query!", e );
                }
            }
        }
        else
        {
            log.info( "RecordChangeEvent is a local event, ignoring..." );
        }
    }

    private void init()
    {
        if ( !entityToTableNames.isEmpty() )
        {
            return;
        }

        for ( EntityType<?> entity : sessionFactory.getMetamodel().getEntities() )
        {
            Class<?> javaType = entity.getJavaType();
            String value = extractTableName( javaType );
            entityToTableNames.put( value, javaType );
        }
    }

    public String extractTableName( final Class<?> modelClazz )
    {
        final MetamodelImpl metamodel = (MetamodelImpl) sessionFactory.getMetamodel();
        final EntityPersister entityPersister = metamodel.entityPersister( modelClazz );

        if ( entityPersister instanceof SingleTableEntityPersister )
        {
            return ((SingleTableEntityPersister) entityPersister).getTableName();
        }
        else
        {
            throw new IllegalArgumentException( modelClazz + " does not map to a single table." );
        }
    }
}
