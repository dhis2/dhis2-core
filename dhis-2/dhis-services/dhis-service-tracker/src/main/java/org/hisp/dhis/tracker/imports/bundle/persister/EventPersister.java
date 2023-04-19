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

import lombok.Builder;
import lombok.Data;

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
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.converter.TrackerConverterService;
import org.hisp.dhis.tracker.imports.converter.TrackerSideEffectConverterService;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
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

        payloadDataValues.forEach( dv -> {

            DataElement dataElement = preheat.getDataElement( dv.getDataElement() );
            checkNotNull( dataElement,
                "Data element should never be NULL here if validation is enforced before commit." );

            // EventDataValue.dataElement contains a UID
            EventDataValue eventDataValue = dataValueDBMap.get( dataElement.getUid() );

            ValuesHolder valuesHolder = getAuditAndDateParameters( eventDataValue, dv );

            eventDataValue = valuesHolder.getEventDataValue();

            eventDataValue.setDataElement( dataElement.getUid() );
            eventDataValue.setStoredBy( dv.getStoredBy() );

            if ( StringUtils.isEmpty( dv.getValue() ) )
            {
                if ( dataElement.isFileType() )
                {
                    unassignFileResource( session, preheat, psi.getUid(), eventDataValue.getValue() );
                }

                psi.getEventDataValues().remove( eventDataValue );
            }
            else
            {
                eventDataValue.setValue( dv.getValue() );

                if ( dataElement.isFileType() )
                {
                    assignFileResource( session, preheat, psi.getUid(), eventDataValue.getValue() );
                }

                psi.getEventDataValues().remove( eventDataValue );
                psi.getEventDataValues().add( eventDataValue );
            }

            logTrackedEntityDataValueHistory( preheat.getUsername(), dataElement, psi,
                new Date(), valuesHolder );
        } );
    }

    private Date getFromOrNewDate( DataValue dv, Function<DataValue, Instant> dateGetter )
    {
        return Optional.of( dv )
            .map( dateGetter )
            .map( DateUtils::fromInstant )
            .orElseGet( Date::new );
    }

    private void logTrackedEntityDataValueHistory( String userName,
        DataElement de, ProgramStageInstance psi, Date created, ValuesHolder valuesHolder )
    {
        AuditType auditType = valuesHolder.getAuditType();

        if ( auditType != null )
        {
            TrackedEntityDataValueAudit valueAudit = new TrackedEntityDataValueAudit();
            valueAudit.setProgramStageInstance( psi );
            valueAudit.setValue( valuesHolder.getValue() );
            valueAudit.setAuditType( auditType );
            valueAudit.setDataElement( de );
            valueAudit.setModifiedBy( userName );
            valueAudit.setProvidedElsewhere( valuesHolder.isProvidedElseWhere() );
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

    private boolean isNewDataValue( EventDataValue eventDataValue, DataValue dv )
    {
        return eventDataValue == null
            || (eventDataValue.getCreated() == null && StringUtils.isNotBlank( dv.getValue() ));
    }

    private boolean isDeletion( EventDataValue eventDataValue, DataValue dv )
    {
        return StringUtils.isNotBlank( eventDataValue.getValue() ) && StringUtils.isBlank( dv.getValue() );
    }

    private boolean isUpdate( EventDataValue eventDataValue, DataValue dv )
    {
        return !StringUtils.equals( dv.getValue(), eventDataValue.getValue() );
    }

    private ValuesHolder getAuditAndDateParameters( EventDataValue eventDataValue, DataValue dv )
    {
        String persistedValue;

        AuditType auditType = null;

        if ( isNewDataValue( eventDataValue, dv ) )
        {
            eventDataValue = new EventDataValue();
            eventDataValue.setCreated( getFromOrNewDate( dv, DataValue::getCreatedAt ) );
            eventDataValue.setLastUpdated( getFromOrNewDate( dv, DataValue::getUpdatedAt ) );
            persistedValue = dv.getValue();
            auditType = AuditType.CREATE;
        }
        else
        {
            persistedValue = eventDataValue.getValue();

            if ( isUpdate( eventDataValue, dv ) )
            {
                auditType = AuditType.UPDATE;
                eventDataValue.setLastUpdated( getFromOrNewDate( dv, DataValue::getUpdatedAt ) );
            }

            if ( isDeletion( eventDataValue, dv ) )
            {
                auditType = AuditType.DELETE;
                eventDataValue.setLastUpdated( getFromOrNewDate( dv, DataValue::getUpdatedAt ) );
            }
        }

        return ValuesHolder.builder().value( persistedValue ).providedElseWhere( dv.isProvidedElsewhere() )
            .auditType( auditType )
            .eventDataValue( eventDataValue ).build();
    }

    @Data
    @Builder
    static class ValuesHolder
    {
        private final String value;

        private final boolean providedElseWhere;

        private final AuditType auditType;

        private final EventDataValue eventDataValue;
    }
}
