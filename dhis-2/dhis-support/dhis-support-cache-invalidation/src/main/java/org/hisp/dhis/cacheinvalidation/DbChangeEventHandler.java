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
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.debezium.data.Envelope;
import io.debezium.engine.RecordChangeEvent;

/**
 * Debezium event handler responsible for acting on {@link RecordChangeEvent}s
 * emitted from the Debezium engine. This handler is attached to the Debezium
 * engine configuration in {@link DebeziumService#startDebeziumEngine()}
 * <p>
 * This class will try to evict the cache elements based on information in the
 * incoming {@link RecordChangeEvent} objects and then calling
 * {@link org.hibernate.Cache#evict(Class, Object)} and
 * {@link org.hibernate.Cache#evictCollectionData(String, Serializable)}.
 *
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
    private TableNameToEntityMapping tableNameToEntityMapping;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private PeriodService periodService;

    /**
     * Called by the {@link io.debezium.embedded.EmbeddedEngine}'s event
     * handler. Configured in {@link DebeziumService#startDebeziumEngine()}
     *
     * @param event RecordChangeEvent<SourceRecord> containing the database
     *        replication event information
     */
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

        Envelope.Operation operation = Envelope.Operation.forCode( payload.getString( "op" ) );
        if ( operation == Envelope.Operation.READ )
        {
            log.debug( "Operation is READ, skipping event..." );
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

        // Looks up the incoming transaction ID in our local txId cache.
        if ( knownTransactionsService.isKnown( txId ) )
        {
            log.debug( "Incoming event txId is registered on this instance, skipping this event..." );
            return;
        }

        String[] topic = sourceRecord.topic().split( "\\." );
        if ( topic.length == 0 )
        {
            log.warn( "Topic is length is 0, skipping event..." );
            return;
        }

        String tableName = topic[topic.length - 1];

        List<Object[]> entityClasses = tableNameToEntityMapping.getEntities( tableName );
        Objects.requireNonNull( entityClasses, "Failed to look up entity in entity table! Table name=" + tableName );

        Schema keySchema = sourceRecord.keySchema();
        if ( keySchema == null )
        {
            Object key = sourceRecord.key();
            log.warn( String.format( "No key schema for tablename=%s, key=%s", tableName, key ) );
            return;
        }

        Class<?> firstEntityClass = (Class<?>) entityClasses.get( 0 )[0];

        Serializable entityId = null;

        if ( DataValue.class == firstEntityClass )
        {
            entityId = getDataValueId( sourceRecord );
        }
        else if ( TrackedEntityAttributeValue.class == firstEntityClass )
        {
            entityId = getTrackedEntityAttributeValueId( sourceRecord );
        }
        else
        {
            // If there is more than one id field, this is a composite key and
            // needs special handling, if they are not handled above
            // we must ignore this event.
            Schema schema = sourceRecord.keySchema();
            List<Field> allIdFields = schema.fields();
            if ( allIdFields.size() > 1 )
            {
                log.warn( "More than one ID field found in the key schema, using the first one. sourceRecord="
                    + sourceRecord );
            }
            else
            {
                entityId = getEntityIdFromFirstField( sourceRecord );
            }
        }

        if ( entityId == null )
        {
            log.error( String.format( "No entity id for entity class=%s, operation=%s", firstEntityClass, operation ) );
            throw new NullPointerException(
                "Could not extract an entity id from the event, can not continue with cache eviction." );
        }

        evictExternalEntityChanges( txId, operation, entityClasses, entityId );
    }

    private Serializable getTrackedEntityAttributeValueId( SourceRecord sourceRecord )
    {
        Schema schema = sourceRecord.keySchema();
        List<Field> allIdFields = schema.fields();
        Struct keyStruct = (Struct) sourceRecord.key();

        Long trackedEntityAttributeId = (Long) getIdFromField( keyStruct, allIdFields.get( 0 ) );
        Long entityInstanceId = (Long) getIdFromField( keyStruct, allIdFields.get( 1 ) );

        TrackedEntityAttribute trackedEntityAttribute = trackedEntityAttributeService.getTrackedEntityAttribute(
            trackedEntityAttributeId );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance(
            entityInstanceId );

        return new TrackedEntityAttributeValue( trackedEntityAttribute, entityInstance );
    }

    private Serializable getDataValueId( SourceRecord sourceRecord )
    {
        Schema schema = sourceRecord.keySchema();
        List<Field> allIdFields = schema.fields();
        Struct keyStruct = (Struct) sourceRecord.key();

        Long dataElementId = (Long) getIdFromField( keyStruct, allIdFields.get( 0 ) );
        Long periodId = (Long) getIdFromField( keyStruct, allIdFields.get( 1 ) );
        Long organisationUnitId = (Long) getIdFromField( keyStruct, allIdFields.get( 2 ) );
        Long categoryOptionComboId = (Long) getIdFromField( keyStruct, allIdFields.get( 3 ) );
        Long attributeOptionComboId = (Long) getIdFromField( keyStruct, allIdFields.get( 4 ) );

        DataElement dataElement = idObjectManager.get( DataElement.class, dataElementId );
        OrganisationUnit organisationUnit = idObjectManager.get( OrganisationUnit.class, organisationUnitId );
        CategoryOptionCombo categoryOptionCombo = idObjectManager.get( CategoryOptionCombo.class,
            categoryOptionComboId );
        CategoryOptionCombo attributeOptionCombo = idObjectManager.get( CategoryOptionCombo.class,
            attributeOptionComboId );
        Period period = periodService.getPeriod( periodId );

        return new DataValue( dataElement, period, organisationUnit, categoryOptionCombo,
            attributeOptionCombo );
    }

    private Serializable getIdFromField( Struct keyStruct, Field field )
    {
        String idFieldName = field.name();
        Schema.Type idType = field.schema().type();

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

    /**
     * Tries to extract the entity ID from the source record.
     *
     * @param sourceRecord SourceRecord object containing info on the event.
     *
     * @return A Serializable object representing an entity ID.
     */
    private Serializable getEntityIdFromFirstField( SourceRecord sourceRecord )
    {
        Schema schema = sourceRecord.keySchema();
        List<Field> allIdFields = schema.fields();

        Struct keyStruct = (Struct) sourceRecord.key();
        Field firstIdField = allIdFields.get( 0 );

        return getIdFromField( keyStruct, firstIdField );
    }

    private void evictExternalEntityChanges( Long txId, Envelope.Operation operation, List<Object[]> entityClasses,
        Serializable entityId )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( String.format( "Evicting entity due to external event! "
                + "txId=%s, totalTxId=%s, operation=%s, entityClasses=%s, entityId=%s",
                txId, knownTransactionsService.size(), operation, printEntityTableValue( entityClasses ), entityId ) );
        }

        Class<?> firstEntityClass = (Class<?>) entityClasses.get( 0 )[0];
        Objects.requireNonNull( firstEntityClass, "Entity class can't be null!" );

        if ( operation == Envelope.Operation.CREATE )
        {
            // Make sure queries will re-fetch to capture the new object.
            queryCacheManager.evictQueryCache( sessionFactory.getCache(), firstEntityClass );
            paginationCacheManager.evictCache( firstEntityClass.getName() );
            // Try to fetch the new entity, so it might get cached.
            tryFetchNewEntity( entityId, firstEntityClass );
        }
        else if ( operation == Envelope.Operation.UPDATE )
        {
            sessionFactory.getCache().evict( firstEntityClass, entityId );
        }
        else if ( operation == Envelope.Operation.DELETE
            || operation == Envelope.Operation.TRUNCATE )
        {
            queryCacheManager.evictQueryCache( sessionFactory.getCache(), firstEntityClass );
            paginationCacheManager.evictCache( firstEntityClass.getName() );
            sessionFactory.getCache().evict( firstEntityClass, entityId );
        }

        if ( operation != Envelope.Operation.MESSAGE )
        {
            evictCollections( entityClasses, entityId );
        }
    }

    private void tryFetchNewEntity( Serializable entityId, Class<?> entityClass )
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
                // Ignore HibernateExceptions, as they are expected.
                return;
            }

            throw e;
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
