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
package org.hisp.dhis.tracker.bundle.persister;

import static com.google.api.client.util.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerObjectReport;
import org.hisp.dhis.tracker.report.TrackerTypeReport;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
public abstract class AbstractTrackerPersister<T extends TrackerDto, V extends BaseIdentifiableObject>
    implements TrackerPersister<T, V>
{
    protected final ReservedValueService reservedValueService;

    protected final TrackedEntityAttributeValueService attributeValueService;

    protected final TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService;

    protected AbstractTrackerPersister( ReservedValueService reservedValueService,
        TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService,
        TrackedEntityAttributeValueService attributeValueService )
    {
        this.reservedValueService = reservedValueService;
        this.attributeValueService = attributeValueService;
        this.trackedEntityAttributeValueAuditService = trackedEntityAttributeValueAuditService;
    }

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

            TrackerObjectReport objectReport = new TrackerObjectReport( getType(), trackerDto.getUid(), idx );

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
                    typeReport.addObjectReport( objectReport );
                    updateAttributes( session, bundle.getPreheat(), trackerDto, convertedDto );
                }
                else
                {
                    if ( isUpdatable() )
                    {
                        updateAttributes( session, bundle.getPreheat(), trackerDto, convertedDto );
                        session.merge( convertedDto );
                        typeReport.getStats().incUpdated();
                        typeReport.addObjectReport( objectReport );
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

    protected void assignFileResource( Session session, TrackerPreheat preheat, String fr )
    {
        assignFileResource( session, preheat, fr, true );
    }

    protected void unassignFileResource( Session session, TrackerPreheat preheat, String fr )
    {
        assignFileResource( session, preheat, fr, false );
    }

    private void assignFileResource( Session session, TrackerPreheat preheat, String fr, boolean isAssign )
    {
        FileResource fileResource = preheat.get( FileResource.class, fr );

        if ( fileResource == null )
        {
            return;
        }

        fileResource.setAssigned( isAssign );
        session.persist( fileResource );
    }

    protected void handleTrackedEntityAttributeValues( Session session, TrackerPreheat preheat,
        List<Attribute> payloadAttributes, TrackedEntityInstance trackedEntityInstance )
    {
        // TODO: Do not use attributeValueService.
        // We should have the right version of attribute values present in the
        // TEI
        // at any moment
        Map<String, TrackedEntityAttributeValue> attributeValueDBMap = attributeValueService
            .getTrackedEntityAttributeValues( trackedEntityInstance )
            .stream()
            .collect( Collectors.toMap( teav -> teav.getAttribute().getUid(), Function.identity() ) );

        for ( Attribute at : payloadAttributes )
        {
            boolean isNew = false;

            AuditType auditType = null;

            TrackedEntityAttribute attribute = preheat.get( TrackedEntityAttribute.class, at.getAttribute() );

            checkNotNull( attribute,
                "Attribute " + at.getAttribute()
                    + " should never be NULL here if validation is enforced before commit." );

            TrackedEntityAttributeValue attributeValue = attributeValueDBMap.get( at.getAttribute() );

            if ( attributeValue == null )
            {
                attributeValue = new TrackedEntityAttributeValue();
                attributeValue.setAttribute( attribute );
                attributeValue.setEntityInstance( trackedEntityInstance );

                isNew = true;
                auditType = AuditType.CREATE;
            }
            else if ( !attributeValue.getPlainValue().equals( at.getValue() ) )
            {
                auditType = AuditType.UPDATE;
            }

            attributeValue
                .setValue( at.getValue() )
                .setStoredBy( at.getStoredBy() );

            // We cannot use attributeValue.getValue() because it uses
            // encryption logic
            // So we need to use at.getValue()
            if ( StringUtils.isEmpty( at.getValue() ) )
            {
                if ( attribute.getValueType() == ValueType.FILE_RESOURCE )
                {
                    unassignFileResource( session, preheat, attributeValueDBMap.get( at.getAttribute() ).getValue() );
                }

                session.remove( attributeValue );
                auditType = AuditType.DELETE;
            }
            else
            {
                if ( attribute.getValueType() == ValueType.FILE_RESOURCE )
                {
                    assignFileResource( session, preheat, attributeValue.getValue() );
                }

                saveOrUpdate( session, isNew, attributeValue );
            }

            logTrackedEntityAttributeValueHistory( preheat.getUsername(), attributeValue,
                trackedEntityInstance, auditType );

            handleReservedValue( attributeValue );
        }
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

        if ( allowAuditLog && auditType != null )
        {
            TrackedEntityAttributeValueAudit valueAudit = new TrackedEntityAttributeValueAudit(
                attributeValue, attributeValue.getValue(), userName, auditType );
            valueAudit.setEntityInstance( trackedEntityInstance );
            trackedEntityAttributeValueAuditService.addTrackedEntityAttributeValueAudit( valueAudit );
        }
    }

    private void saveOrUpdate( Session session, boolean isNew, Object persistable )
    {
        if ( isNew )
        {
            session.persist( persistable );
        }
        else
        {
            session.merge( persistable );
        }
    }
}
