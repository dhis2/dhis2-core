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
package org.hisp.dhis.tracker.bundle.persister;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.converter.TrackerSideEffectConverterService;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EventPersister extends AbstractTrackerPersister<Event, ProgramStageInstance>
{
    private final TrackerConverterService<Event, ProgramStageInstance> eventConverter;

    private final TrackedEntityCommentService trackedEntityCommentService;

    private final TrackerSideEffectConverterService sideEffectConverterService;

    private final TrackedEntityDataValueAuditService trackedEntityDataValueAuditService;

    public EventPersister( ReservedValueService reservedValueService,
        TrackerConverterService<Event, ProgramStageInstance> eventConverter,
        TrackedEntityCommentService trackedEntityCommentService,
        TrackerSideEffectConverterService sideEffectConverterService,
        TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService,
        TrackedEntityDataValueAuditService trackedEntityDataValueAuditService )
    {
        super( reservedValueService, trackedEntityAttributeValueAuditService );
        this.eventConverter = eventConverter;
        this.trackedEntityCommentService = trackedEntityCommentService;
        this.sideEffectConverterService = sideEffectConverterService;
        this.trackedEntityDataValueAuditService = trackedEntityDataValueAuditService;
    }

    @Override
    protected void persistComments( TrackerPreheat preheat, ProgramStageInstance programStageInstance )
    {
        if ( !programStageInstance.getComments().isEmpty() )
        {
            for ( TrackedEntityComment comment : programStageInstance.getComments() )
            {
                if ( Objects.isNull( preheat.getNote( comment.getUid() ) ) )
                {
                    this.trackedEntityCommentService.addTrackedEntityComment( comment );
                }
            }
        }
    }

    @Override
    protected void updatePreheat( TrackerPreheat preheat, ProgramStageInstance programStageInstance )
    {
        preheat.putEvents( Collections.singletonList( programStageInstance ) );
    }

    @Override
    protected boolean isNew( TrackerPreheat preheat, String uid )
    {
        return preheat.getEvent( uid ) == null;
    }

    @Override
    protected TrackerSideEffectDataBundle handleSideEffects( TrackerBundle bundle,
        ProgramStageInstance programStageInstance )
    {
        return TrackerSideEffectDataBundle.builder()
            .klass( ProgramStageInstance.class )
            .enrollmentRuleEffects( new HashMap<>() )
            .eventRuleEffects( sideEffectConverterService.toTrackerSideEffects( bundle.getEventRuleEffects() ) )
            .object( programStageInstance.getUid() )
            .importStrategy( bundle.getImportStrategy() )
            .accessedBy( bundle.getUsername() )
            .programStageInstance( programStageInstance )
            .program( programStageInstance.getProgramStage().getProgram() )
            .build();
    }

    @Override
    protected ProgramStageInstance convert( TrackerBundle bundle, Event event )
    {
        return eventConverter.from( bundle.getPreheat(), event );
    }

    @Override
    protected TrackerType getType()
    {
        return TrackerType.EVENT;
    }

    @Override
    protected void updateAttributes( Session session, TrackerPreheat preheat,
        Event event, ProgramStageInstance programStageInstance )
    {
        // DO NOTHING - EVENT HAVE NO ATTRIBUTES
    }

    @Override
    protected void updateDataValues( Session session, TrackerPreheat preheat,
        Event event, ProgramStageInstance programStageInstance )
    {
        handleDataValues( session, preheat, event.getDataValues(), programStageInstance );
    }

    private void handleDataValues( Session session, TrackerPreheat preheat, Set<DataValue> payloadDataValues,
        ProgramStageInstance psi )
    {
        Map<String, EventDataValue> dataValueDBMap = Optional.ofNullable( preheat.getEvent( psi.getUid() ) )
            .map( a -> a.getEventDataValues()
                .stream()
                .collect( Collectors.toMap( EventDataValue::getDataElement, Function.identity() ) ) )
            .orElse( new HashMap<>() );

        Date today = new Date();

        for ( DataValue dv : payloadDataValues )
        {
            AuditType auditType;

            DataElement dateElement = preheat.get( DataElement.class, dv.getDataElement() );

            checkNotNull( dateElement,
                "Data element should never be NULL here if validation is enforced before commit." );

            EventDataValue eventDataValue = dataValueDBMap.get( dv.getDataElement() );

            if ( eventDataValue == null )
            {
                eventDataValue = new EventDataValue();
                auditType = AuditType.CREATE;
            }
            else
            {
                final String persistedValue = eventDataValue.getValue();

                Optional<AuditType> optionalAuditType = Optional.ofNullable( dv.getValue() )
                    .filter( v -> !dv.getValue().equals( persistedValue ) )
                    .map( v1 -> AuditType.UPDATE )
                    .or( () -> Optional.ofNullable( dv.getValue() ).map( a -> AuditType.READ )
                        .or( () -> Optional.of( AuditType.DELETE ) ) );

                auditType = optionalAuditType.orElse( null );
            }

            eventDataValue.setDataElement( dateElement.getUid() );
            eventDataValue.setStoredBy( dv.getStoredBy() );

            handleDataValueCreatedUpdatedDates( dv, eventDataValue );

            if ( StringUtils.isEmpty( dv.getValue() ) )
            {
                if ( dateElement.isFileType() )
                {
                    unassignFileResource( session, preheat, dataValueDBMap.get( dv.getDataElement() ).getValue() );
                }

                psi.getEventDataValues().remove( eventDataValue );
            }
            else
            {
                eventDataValue.setValue( dv.getValue() );

                if ( dateElement.isFileType() )
                {
                    assignFileResource( session, preheat, eventDataValue.getValue() );
                }

                psi.getEventDataValues().add( eventDataValue );
            }

            logTrackedEntityDataValueHistory( preheat.getUsername(), eventDataValue, dateElement, psi, auditType,
                today );
        }
    }

    private void handleDataValueCreatedUpdatedDates( DataValue dv, EventDataValue eventDataValue )
    {
        eventDataValue.setCreated( getFromOrNewDate( dv, DataValue::getCreatedAt ) );
        eventDataValue.setLastUpdated( getFromOrNewDate( dv, DataValue::getUpdatedAt ) );
    }

    private Date getFromOrNewDate( DataValue dv, Function<DataValue, Instant> dateGetter )
    {
        return Optional.of( dv )
            .map( dateGetter )
            .map( DateUtils::fromInstant )
            .orElseGet( Date::new );
    }

    private void logTrackedEntityDataValueHistory( String userName,
        EventDataValue eventDataValue, DataElement de, ProgramStageInstance psi, AuditType auditType, Date created )
    {
        if ( auditType != null )
        {
            TrackedEntityDataValueAudit valueAudit = new TrackedEntityDataValueAudit();
            valueAudit.setProgramStageInstance( psi );
            valueAudit.setValue( eventDataValue.getValue() );
            valueAudit.setAuditType( auditType );
            valueAudit.setDataElement( de );
            valueAudit.setModifiedBy( userName );
            valueAudit.setProvidedElsewhere( eventDataValue.getProvidedElsewhere() );
            valueAudit.setCreated( created );

            trackedEntityDataValueAuditService.addTrackedEntityDataValueAudit( valueAudit );
        }
    }

    @Override
    protected void persistOwnership( TrackerPreheat preheat, ProgramStageInstance entity )
    {
        // DO NOTHING. Event creation does not create ownership records.
    }

    @Override
    protected String getUpdatedTrackedEntity( ProgramStageInstance entity )
    {
        return Optional.ofNullable( entity.getProgramInstance() ).filter( pi -> pi.getEntityInstance() != null )
            .map( pi -> pi.getEntityInstance().getUid() ).orElse( null );
    }
}
