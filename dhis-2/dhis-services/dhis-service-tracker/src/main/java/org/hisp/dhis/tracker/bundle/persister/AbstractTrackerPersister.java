package org.hisp.dhis.tracker.bundle.persister;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.api.client.util.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleHook;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerObjectReport;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
public abstract class AbstractTrackerPersister<T extends TrackerDto, V extends BaseIdentifiableObject>
    implements TrackerPersister<T, V>
{
    protected List<TrackerBundleHook> bundleHooks;

    protected final ReservedValueService reservedValueService;

    public AbstractTrackerPersister( List<TrackerBundleHook> bundleHooks, ReservedValueService reservedValueService )
    {
        this.bundleHooks = bundleHooks;
        this.reservedValueService = reservedValueService;
    }

    /**
     * Template method that can be used by classes extending this class to execute
     * the persistence flow of Tracker entities
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
        // Execute pre-create hooks - if any
        //
        runPreCreateHooks( bundle );
        session.flush();

        //
        // Extract the entities to persist from the Bundle
        //
        List<T> dtos = getByType( getType(), bundle );

        for ( int idx = 0; idx < dtos.size(); idx++ )
        {
            //
            // Create the Report for the entity being persisted
            //
            final T trackerDto = dtos.get( idx );

            TrackerObjectReport objectReport = new TrackerObjectReport( getType(), trackerDto.getUid(), idx );
            typeReport.addObjectReport( objectReport );

            try
            {
                //
                // Convert the TrackerDto into an Hibernate-managed entity
                //
                V convertedDto = convert( bundle, trackerDto );

                //
                // Handle comments persistence, if required
                //
                persistComments( convertedDto );

                updateDataValues( session, bundle.getPreheat(), trackerDto, convertedDto );

                //
                // Save or update the entity
                //
                if ( isNew( bundle.getPreheat(), trackerDto.getUid() ) )
                {
                    session.persist( convertedDto );
                    typeReport.getStats().incCreated();
                }
                else
                {
                    session.merge( convertedDto );
                    typeReport.getStats().incUpdated();
                }

                updateAttributes( session, bundle.getPreheat(), trackerDto, convertedDto );

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
            }
            catch ( Exception e )
            {
                final String msg = "A Tracker Entity of type '" + getType().getName() + "' (" + trackerDto.getUid()
                    + ") failed to persist.";

                if ( bundle.getAtomicMode().equals( AtomicMode.ALL ) )
                {
                    throw new PersistenceException( msg , e );
                }
                else
                {
                    // TODO currently we do not keep track of the failed entity in the TrackerObjectReport

                    log.warn( msg + "\nThe Import process will process remaining entities.", e );

                    typeReport.getStats().incIgnored();
                }
            }
        }

        session.flush();

        //
        // Execute post-create hooks - if any
        //
        runPostCreateHooks( bundle );

        typeReport.getSideEffectDataBundles().addAll( sideEffectDataBundles );

        return typeReport;
    }

    // // // // // // // //
    // // // // // // // //
    // TEMPLATE METHODS //
    // // // // // // // //
    // // // // // // // //

    /**
     * Executes the configured pre-creation hooks. This method takes place only
     * once, just before the objects persistence
     */
    protected abstract void runPreCreateHooks( TrackerBundle bundle );

    /**
     * Converts an object implementing the {@link TrackerDto} interface into the
     * corresponding Hibernate-managed object
     */
    protected abstract V convert( TrackerBundle bundle, T trackerDto );

    /**
     * Persists the comments for the given entity, if the entity has comments
     */
    protected abstract void persistComments( V entity );

    /**
     * Execute the persistence of Data values linked to the entity
     * being processed
     */
    protected abstract void updateDataValues( Session session, TrackerPreheat preheat,
        T trackerDto, V hibernateEntity );

    /**
     * Execute the persistence of Attribute values linked to the entity
     * being processed
     */
    protected abstract void updateAttributes( Session session, TrackerPreheat preheat,
        T trackerDto, V hibernateEntity );

    /**
     * Updates the {@link TrackerPreheat} object with the entity that has been
     * persisted
     */
    protected abstract void updatePreheat( TrackerPreheat preheat, V convertedDto );

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

    /**
     * Executes the configured post-creation hooks. This method takes place only
     * once, after all objects have been persisted.
     */
    protected abstract void runPostCreateHooks( TrackerBundle bundle );

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
        Map<String, TrackedEntityAttributeValue> attributeValueDBMap = trackedEntityInstance
            .getTrackedEntityAttributeValues()
            .stream()
            .collect( Collectors.toMap( teav -> teav.getAttribute().getUid(), Function.identity() ) );

        for ( Attribute at : payloadAttributes )
        {
            boolean isNew = false;
            TrackedEntityAttribute attribute = preheat.get( TrackedEntityAttribute.class, at.getAttribute() );

            checkNotNull( attribute,
                "Attribute should never be NULL here if validation is enforced before commit." );

            TrackedEntityAttributeValue attributeValue = attributeValueDBMap.get( at.getAttribute() );
            
            if ( attributeValue == null )
            {
                attributeValue = new TrackedEntityAttributeValue();
                isNew = true;
            }

            attributeValue
                .setAttribute( attribute )
                .setEntityInstance( trackedEntityInstance )
                .setValue( at.getValue() )
                .setStoredBy( at.getStoredBy() );

            // We cannot use attributeValue.getValue() because it uses encryption logic
            // So we need to use at.getValue()
            if ( StringUtils.isEmpty( at.getValue() ) )
            {
                if ( attribute.getValueType() == ValueType.FILE_RESOURCE )
                {
                    unassignFileResource( session, preheat, attributeValueDBMap.get( at.getAttribute() ).getValue() );
                }
                session.remove( attributeValue );
            }
            else
            {
                if ( attribute.getValueType() == ValueType.FILE_RESOURCE )
                {
                    assignFileResource( session, preheat, attributeValue.getValue() );
                }

                saveOrUpdate( session, isNew, attributeValue );
            }

            if ( attributeValue.getAttribute().isGenerated() && attributeValue.getAttribute().getTextPattern() != null )
            {
                reservedValueService.useReservedValue( attributeValue.getAttribute().getTextPattern(),
                    attributeValue.getValue() );
            }
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
