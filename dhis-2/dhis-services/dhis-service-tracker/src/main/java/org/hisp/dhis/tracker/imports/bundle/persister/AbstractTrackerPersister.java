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
package org.hisp.dhis.tracker.imports.bundle.persister;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAudit;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.FlushMode;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
@RequiredArgsConstructor( access = AccessLevel.PROTECTED )
public abstract class AbstractTrackerPersister<T extends TrackerDto, V extends BaseIdentifiableObject>
    implements TrackerPersister<T, V>
{
    protected final ReservedValueService reservedValueService;

    protected final TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService;

    /**
     * Template method that can be used by classes extending this class to
     * execute the persistence flow of Tracker entities
     *
     * @param session a valid Hibernate Session
     * @param bundle the Bundle to persist
     * @return a {@link TrackerTypeReport}
     */
    @Override
    public TrackerTypeReport persist( Session session, TrackerBundle bundle )
    {
        //
        // Init the report that will hold the results of the persist operation
        //
        TrackerTypeReport typeReport = new TrackerTypeReport( getType() );

        List<TrackerSideEffectDataBundle> sideEffectDataBundles = new ArrayList<>();

        //
        // Extract the entities to persist from the Bundle
        //
        List<T> dtos = getByType( getType(), bundle );

        Set<String> updatedTeiList = bundle.getUpdatedTeis();

        for ( int idx = 0; idx < dtos.size(); idx++ )
        {
            //
            // Create the Report for the entity being persisted
            //
            final T trackerDto = dtos.get( idx );

            Entity objectReport = new Entity( getType(), trackerDto.getUid(), idx );

            try
            {
                //
                // Convert the TrackerDto into an Hibernate-managed entity
                //
                V convertedDto = convert( bundle, trackerDto );

                //
                // Handle comments persistence, if required
                //
                persistComments( bundle.getPreheat(), convertedDto );

                //
                // Handle ownership records, if required
                //
                persistOwnership( bundle.getPreheat(), convertedDto );

                updateDataValues( session, bundle.getPreheat(), trackerDto, convertedDto );

                //
                // Save or update the entity
                //
                if ( isNew( bundle.getPreheat(), trackerDto ) )
                {
                    session.persist( convertedDto );
                    typeReport.getStats().incCreated();
                    typeReport.addEntity( objectReport );
                    updateAttributes( session, bundle.getPreheat(), trackerDto, convertedDto );
                }
                else
                {
                    if ( isUpdatable() )
                    {
                        updateAttributes( session, bundle.getPreheat(), trackerDto, convertedDto );
                        session.merge( convertedDto );
                        typeReport.getStats().incUpdated();
                        typeReport.addEntity( objectReport );
                        Optional.ofNullable( getUpdatedTrackedEntity( convertedDto ) ).ifPresent( updatedTeiList::add );
                    }
                    else
                    {
                        typeReport.getStats().incIgnored();
                    }
                }

                //
                // Add the entity to the Preheat
                //
                updatePreheat( bundle.getPreheat(), convertedDto );

                if ( FlushMode.OBJECT == bundle.getFlushMode() )
                {
                    session.flush();
                }

                if ( !bundle.isSkipSideEffects() )
                {
                    sideEffectDataBundles.add( handleSideEffects( bundle, convertedDto ) );
                }

                bundle.setUpdatedTeis( updatedTeiList );
            }
            catch ( Exception e )
            {
                final String msg = "A Tracker Entity of type '" + getType().getName() + "' (" + trackerDto.getUid()
                    + ") failed to persist.";

                if ( bundle.getAtomicMode().equals( AtomicMode.ALL ) )
                {
                    throw new PersistenceException( msg, e );
                }
                else
                {
                    // TODO currently we do not keep track of the failed entity
                    // in the TrackerObjectReport

                    log.warn( msg + "\nThe Import process will process remaining entities.", e );

                    typeReport.getStats().incIgnored();
                }
            }
        }

        typeReport.getSideEffectDataBundles().addAll( sideEffectDataBundles );

        return typeReport;
    }

    // // // // // // // //
    // // // // // // // //
    // TEMPLATE METHODS //
    // // // // // // // //
    // // // // // // // //

    /**
     * Get Tracked Entity for enrollments or events that have been updated
     */
    protected abstract String getUpdatedTrackedEntity( V entity );

    /**
     * Converts an object implementing the {@link TrackerDto} interface into the
     * corresponding Hibernate-managed object
     */
    protected abstract V convert( TrackerBundle bundle, T trackerDto );

    /**
     * Persists the comments for the given entity, if the entity has comments
     */
    protected abstract void persistComments( TrackerPreheat preheat, V entity );

    /**
     * Persists ownership records for the given entity
     */
    protected abstract void persistOwnership( TrackerPreheat preheat, V entity );

    /**
     * Execute the persistence of Data values linked to the entity being
     * processed
     */
    protected abstract void updateDataValues( Session session, TrackerPreheat preheat,
        T trackerDto, V hibernateEntity );

    /**
     * Execute the persistence of Attribute values linked to the entity being
     * processed
     */
    protected abstract void updateAttributes( Session session, TrackerPreheat preheat,
        T trackerDto, V hibernateEntity );

    /**
     * Updates the {@link TrackerPreheat} object with the entity that has been
     * persisted
     */
    protected abstract void updatePreheat( TrackerPreheat preheat, V convertedDto );

    /**
     * informs this persister wether specific entity type should be updated
     * defaults to true, is known to be false for Relationships
     */
    protected boolean isUpdatable()
    {
        return true;
    }

    /**
     * Determines if the given trackerDto belongs to an existing entity
     */
    protected boolean isNew( TrackerPreheat preheat, T trackerDto )
    {
        return isNew( preheat, trackerDto.getUid() );
    }

    /**
     * Determines if the given uid belongs to an existing entity
     */
    protected abstract boolean isNew( TrackerPreheat preheat, String uid );

    /**
     * TODO add comment
     */
    protected abstract TrackerSideEffectDataBundle handleSideEffects( TrackerBundle bundle, V entity );

    /**
     * Get the Tracker Type for which the current Persister is responsible for.
     */
    protected abstract TrackerType getType();

    @SuppressWarnings( "unchecked" )
    private List<T> getByType( TrackerType type, TrackerBundle bundle )
    {

        if ( type.equals( TrackerType.TRACKED_ENTITY ) )
        {
            return (List<T>) bundle.getTrackedEntities();
        }
        else if ( type.equals( TrackerType.ENROLLMENT ) )
        {
            return (List<T>) bundle.getEnrollments();
        }
        else if ( type.equals( TrackerType.EVENT ) )
        {
            return (List<T>) bundle.getEvents();
        }
        else if ( type.equals( TrackerType.RELATIONSHIP ) )
        {
            return (List<T>) bundle.getRelationships();
        }
        else
        {
            return new ArrayList<>();
        }
    }

    // // // // // // // //
    // // // // // // // //
    // SHARED METHODS //
    // // // // // // // //
    // // // // // // // //

    protected void assignFileResource( Session session, TrackerPreheat preheat,
        String fileResourceOwner, String fr )
    {
        assignFileResource( session, preheat, fileResourceOwner, fr, true );
    }

    protected void unassignFileResource( Session session, TrackerPreheat preheat,
        String fileResourceOwner, String fr )
    {
        assignFileResource( session, preheat, fileResourceOwner, fr, false );
    }

    private void assignFileResource( Session session, TrackerPreheat preheat,
        String fileResourceOwner, String fr, boolean isAssign )
    {
        FileResource fileResource = preheat.get( FileResource.class, fr );

        if ( fileResource == null )
        {
            return;
        }

        fileResource.setAssigned( isAssign );
        fileResource.setFileResourceOwner( fileResourceOwner );
        session.merge( fileResource );
    }

    protected void handleTrackedEntityAttributeValues( Session session, TrackerPreheat preheat,
        List<Attribute> payloadAttributes, TrackedEntityInstance trackedEntityInstance )
    {
        if ( payloadAttributes.isEmpty() )
        {
            return;
        }

        TrackerIdSchemeParams idSchemes = preheat.getIdSchemes();
        Map<MetadataIdentifier, TrackedEntityAttributeValue> attributeValueById = trackedEntityInstance
            .getTrackedEntityAttributeValues()
            .stream()
            .collect( Collectors.toMap( teav -> idSchemes.toMetadataIdentifier( teav.getAttribute() ),
                Function.identity() ) );

        payloadAttributes
            .forEach( attribute -> {

                // We cannot get the value from attributeToStore because it uses
                // encryption logic, so we need to use the one from payload
                boolean isDelete = StringUtils.isEmpty( attribute.getValue() );

                TrackedEntityAttributeValue trackedEntityAttributeValue = attributeValueById
                    .get( attribute.getAttribute() );

                boolean isUpdated = false;

                boolean isNew = Objects.isNull( trackedEntityAttributeValue );

                if ( isDelete && isNew )
                {
                    return;
                }

                if ( isDelete )
                {
                    delete( session, preheat, trackedEntityAttributeValue, trackedEntityInstance );
                }
                else
                {
                    if ( !isNew )
                    {
                        isUpdated = !trackedEntityAttributeValue.getPlainValue().equals( attribute.getValue() );
                    }

                    trackedEntityAttributeValue = Optional.ofNullable( trackedEntityAttributeValue )
                        .orElseGet( () -> new TrackedEntityAttributeValue()
                            .setAttribute( getTrackedEntityAttributeFromPreheat( preheat, attribute.getAttribute() ) )
                            .setEntityInstance( trackedEntityInstance ) )
                        .setStoredBy( attribute.getStoredBy() )
                        .setValue( attribute.getValue() );

                    saveOrUpdate( session, preheat, isNew, trackedEntityInstance, trackedEntityAttributeValue,
                        isUpdated );
                }

                handleReservedValue( trackedEntityAttributeValue );
            } );
    }

    private void delete( Session session, TrackerPreheat preheat,
        TrackedEntityAttributeValue trackedEntityAttributeValue, TrackedEntityInstance trackedEntityInstance )
    {
        if ( isFileResource( trackedEntityAttributeValue ) )
        {
            unassignFileResource( session, preheat, trackedEntityInstance.getUid(),
                trackedEntityAttributeValue.getValue() );
        }

        session.remove( trackedEntityAttributeValue );

        logTrackedEntityAttributeValueHistory(
            preheat.getUsername(),
            trackedEntityAttributeValue,
            trackedEntityInstance,
            AuditType.DELETE );
    }

    private void saveOrUpdate( Session session, TrackerPreheat preheat, boolean isNew,
        TrackedEntityInstance trackedEntityInstance, TrackedEntityAttributeValue trackedEntityAttributeValue,
        boolean isUpdated )
    {
        if ( isFileResource( trackedEntityAttributeValue ) )
        {
            assignFileResource( session, preheat, trackedEntityInstance.getUid(),
                trackedEntityAttributeValue.getValue() );
        }

        AuditType auditType = null;

        if ( isNew )
        {
            session.persist( trackedEntityAttributeValue );
            // In case it's a newly created attribute we'll add it back to TEI,
            // so it can end up in preheat
            trackedEntityInstance.getTrackedEntityAttributeValues().add( trackedEntityAttributeValue );
            auditType = AuditType.CREATE;
        }
        else
        {
            session.merge( trackedEntityAttributeValue );

            if ( isUpdated )
            {
                auditType = AuditType.UPDATE;
            }
        }

        logTrackedEntityAttributeValueHistory(
            preheat.getUsername(),
            trackedEntityAttributeValue,
            trackedEntityInstance,
            auditType );
    }

    private static boolean isFileResource( TrackedEntityAttributeValue trackedEntityAttributeValue )
    {
        return trackedEntityAttributeValue.getAttribute().getValueType() == ValueType.FILE_RESOURCE;
    }

    private static TrackedEntityAttribute getTrackedEntityAttributeFromPreheat( TrackerPreheat preheat,
        MetadataIdentifier attribute )
    {
        TrackedEntityAttribute trackedEntityAttribute = preheat.getTrackedEntityAttribute( attribute );

        checkNotNull( trackedEntityAttribute,
            "Attribute " + attribute.getIdentifierOrAttributeValue()
                + " should never be NULL here if validation is enforced before commit." );

        return trackedEntityAttribute;
    }

    private void handleReservedValue( TrackedEntityAttributeValue attributeValue )
    {
        if ( attributeValue.getAttribute().isGenerated() && attributeValue.getAttribute().getTextPattern() != null )
        {
            reservedValueService.useReservedValue( attributeValue.getAttribute().getTextPattern(),
                attributeValue.getValue() );
        }
    }

    private void logTrackedEntityAttributeValueHistory( String userName,
        TrackedEntityAttributeValue attributeValue, TrackedEntityInstance trackedEntityInstance, AuditType auditType )
    {
        boolean allowAuditLog = trackedEntityInstance.getTrackedEntityType().isAllowAuditLog();

        // create log entry only for updated, created and deleted attributes
        if ( allowAuditLog && auditType != null )
        {
            TrackedEntityAttributeValueAudit valueAudit = new TrackedEntityAttributeValueAudit(
                attributeValue, attributeValue.getValue(), userName, auditType );
            valueAudit.setEntityInstance( trackedEntityInstance );
            trackedEntityAttributeValueAuditService.addTrackedEntityAttributeValueAudit( valueAudit );
        }
    }
}
