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
package org.hisp.dhis.dxf2.events.importer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.dxf2.importsummary.ImportStatus.ERROR;
import static org.hisp.dhis.dxf2.importsummary.ImportStatus.SUCCESS;
import static org.hisp.dhis.dxf2.importsummary.ImportSummary.error;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE;
import static org.hisp.dhis.importexport.ImportStrategy.DELETE;
import static org.hisp.dhis.importexport.ImportStrategy.UPDATE;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.persistence.EventPersistenceService;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventManager
{
    @NonNull
    @Qualifier( "eventsInsertValidationFactory" )
    private final EventChecking insertValidationFactory;

    @NonNull
    @Qualifier( "eventsUpdateValidationFactory" )
    private final EventChecking updateValidationFactory;

    @NonNull
    @Qualifier( "eventsDeleteValidationFactory" )
    private final EventChecking deleteValidationFactory;

    @NonNull
    private final ProcessingManager processingManager;

    @NonNull
    private final EventPersistenceService eventPersistenceService;

    @NonNull
    private final TrackedEntityDataValueAuditService entityDataValueAuditService;

    @NonNull
    private final CurrentUserService currentUserService;

    private static final String IMPORT_ERROR_STRING = "Invalid or conflicting data";

    public ImportSummary addEvent( final Event event, final WorkContext workContext )
    {
        final ImportSummaries importSummaries = addEvents( ImmutableList.of( event ), workContext );

        if ( isNotEmpty( importSummaries.getImportSummaries() ) )
        {
            return importSummaries.getImportSummaries().get( 0 );
        }
        else
        {
            return error( "Not inserted", event.getEvent() );
        }
    }

    public ImportSummaries addEvents( final List<Event> events, final WorkContext workContext )
    {
        final ImportSummaries importSummaries = new ImportSummaries();

        if ( CollectionUtils.isEmpty( events ) )
        {
            return importSummaries;
        }

        // filter out events which are already in the database as well as
        // duplicates in the payload (if stage is not repeatable)
        List<Event> validEvents = resolveImportableEvents( events, importSummaries, workContext );

        if ( validEvents.isEmpty() )
        {
            return importSummaries;
        }

        // pre-process events
        processingManager.getPreInsertProcessorFactory().process( workContext, validEvents );

        importSummaries.addImportSummaries(
            // Run validation against the remaining "insertable" events //
            insertValidationFactory.check( workContext, validEvents ) );

        // collect the UIDs of events that did not pass validation
        final List<String> invalidEvents = importSummaries.getImportSummaries().stream()
            .filter( i -> i.isStatus( ERROR ) ).map( ImportSummary::getReference ).collect( toList() );

        if ( invalidEvents.size() == events.size() )
        {
            return importSummaries;
        }

        if ( !workContext.getImportOptions().isDryRun() )
        {
            // fetch persistable events //
            List<Event> eventsToInsert = invalidEvents.isEmpty() ? validEvents
                : validEvents.stream().filter( e -> !invalidEvents.contains( e.getEvent() ) ).collect( toList() );

            if ( isNotEmpty( eventsToInsert ) )
            {
                try
                {
                    // save the entire batch in one transaction
                    eventPersistenceService.save( workContext, eventsToInsert );
                }
                catch ( Exception e )
                {
                    handleFailure( workContext, importSummaries, events, IMPORT_ERROR_STRING, CREATE );

                }
            }

            final List<String> eventPersistenceFailedUids = importSummaries.getImportSummaries().stream()
                .filter( i -> i.isStatus( ERROR ) ).map( ImportSummary::getReference ).collect( toList() );

            // Post processing only the events that passed validation and were
            // persisted
            // correctly.
            List<Event> savedEvents = events.stream()
                .filter( e -> !eventPersistenceFailedUids.contains( e.getEvent() ) ).collect( toList() );

            processingManager.getPostInsertProcessorFactory().process( workContext, savedEvents );

            Date today = new Date();
            savedEvents.forEach( e -> auditTrackedEntityDataValueHistory( e, workContext, today ) );
            incrementSummaryTotals( events, importSummaries, CREATE );

        }
        return importSummaries;
    }

    public ImportSummary updateEvent( final Event event, final WorkContext workContext )
    {
        final List<Event> singleEvent = singletonList( event );

        final ImportSummaries importSummaries = updateEvents( singleEvent, workContext );

        if ( isNotEmpty( importSummaries.getImportSummaries() ) )
        {
            return importSummaries.getImportSummaries().get( 0 );
        }
        else
        {
            return error( "Not updated", event.getEvent() );
        }
    }

    public ImportSummaries updateEvents( final List<Event> events, final WorkContext workContext )
    {
        final ImportSummaries importSummaries = new ImportSummaries();

        // pre-process events
        processingManager.getPreUpdateProcessorFactory().process( workContext, events );

        importSummaries.addImportSummaries(
            // Run validation against the remaining "updatable" events //
            updateValidationFactory.check( workContext, events ) );

        // collect the UIDs of events that did not pass validation
        final List<String> eventValidationFailedUids = importSummaries.getImportSummaries().stream()
            .filter( i -> i.isStatus( ERROR ) )
            .map( ImportSummary::getReference ).collect( toList() );

        if ( eventValidationFailedUids.size() == events.size() )
        {
            return importSummaries;
        }

        if ( !workContext.getImportOptions().isDryRun() )
        {
            try
            {
                eventPersistenceService.update( workContext,
                    eventValidationFailedUids.isEmpty() ? events
                        : events.stream().filter( e -> !eventValidationFailedUids.contains( e.getEvent() ) )
                            .collect( toList() ) );
            }
            catch ( Exception e )
            {
                handleFailure( workContext, importSummaries, events, IMPORT_ERROR_STRING, UPDATE );
            }

            final List<String> eventPersistenceFailedUids = importSummaries.getImportSummaries().stream()
                .filter( i -> i.isStatus( ERROR ) ).map( ImportSummary::getReference ).collect( toList() );

            // Post processing only the events that passed validation and were
            // persisted
            // correctly.

            List<Event> savedEvents = events.stream()
                .filter( e -> !eventPersistenceFailedUids.contains( e.getEvent() ) ).collect( toList() );

            processingManager.getPostUpdateProcessorFactory().process( workContext, savedEvents );

            Date today = new Date();
            savedEvents.forEach( e -> auditTrackedEntityDataValueHistory( e, workContext, today ) );

            incrementSummaryTotals( events, importSummaries, UPDATE );

        }

        return importSummaries;
    }

    public ImportSummaries deleteEvents( final List<Event> events, final WorkContext workContext )
    {
        final ImportSummaries importSummaries = new ImportSummaries();

        // pre-process events
        processingManager.getPreDeleteProcessorFactory().process( workContext, events );

        importSummaries.addImportSummaries(
            // Run validation against the remaining "insertable" events //
            deleteValidationFactory.check( workContext, events ) );

        // collect the UIDs of events that did not pass validation
        final List<String> eventValidationFailedUids = importSummaries.getImportSummaries().stream()
            .filter( i -> i.isStatus( ERROR ) )
            .map( ImportSummary::getReference ).collect( toList() );

        if ( eventValidationFailedUids.size() == events.size() )
        {
            return importSummaries;
        }

        if ( !workContext.getImportOptions().isDryRun() )
        {

            try
            {
                eventPersistenceService.delete( workContext,
                    eventValidationFailedUids.isEmpty() ? events
                        : events.stream().filter( e -> !eventValidationFailedUids.contains( e.getEvent() ) )
                            .collect( toList() ) );
            }
            catch ( Exception e )
            {
                handleFailure( workContext, importSummaries, events, IMPORT_ERROR_STRING, DELETE );
            }

            final List<String> eventPersistenceFailedUids = importSummaries.getImportSummaries().stream()
                .filter( i -> i.isStatus( ERROR ) ).map( ImportSummary::getReference ).collect( toList() );

            // Post processing only the events that passed validation and were
            // persisted
            // correctly.
            List<Event> deletedEvents = events.stream()
                .filter( e -> !eventPersistenceFailedUids.contains( e.getEvent() ) ).collect( toList() );
            processingManager.getPostDeleteProcessorFactory().process( workContext, deletedEvents );

            Date today = new Date();
            deletedEvents.forEach( e -> auditTrackedEntityDataValueHistory( e, workContext, today ) );

            incrementSummaryTotals( events, importSummaries, DELETE );

        }

        return importSummaries;
    }

    private void auditTrackedEntityDataValueHistory( Event event, WorkContext workContext, Date today )
    {
        String persistedDataValue = null;

        ProgramStageInstance psi = workContext.getPersistedProgramStageInstanceMap().get( event.getUid() );

        Map<String, EventDataValue> dataValueDBMap = Optional.ofNullable( psi ).map( p -> p.getEventDataValues()
            .stream()
            .collect( Collectors.toMap( EventDataValue::getDataElement, Function.identity() ) ) )
            .orElse( new HashMap<>() );

        for ( DataValue dv : event.getDataValues() )
        {
            AuditType auditType = AuditType.READ;

            DataElement dateElement = workContext.getDataElementMap().get( dv.getDataElement() );

            checkNotNull( dateElement,
                "Data element should never be NULL here if validation is enforced before commit." );

            EventDataValue eventDataValue = dataValueDBMap.get( dv.getDataElement() );

            if ( eventDataValue == null )
            {
                auditType = AuditType.CREATE;
                persistedDataValue = dv.getValue();
            }
            else if ( StringUtils.isEmpty( dv.getValue() ) )
            {
                auditType = AuditType.DELETE;
                persistedDataValue = eventDataValue.getValue();
            }
            else if ( !dv.getValue().equals( eventDataValue.getValue() ) )
            {
                auditType = AuditType.UPDATE;
                persistedDataValue = dv.getValue();
            }

            TrackedEntityDataValueAudit audit = new TrackedEntityDataValueAudit();
            audit.setCreated( today );
            audit.setAuditType( auditType );
            audit.setProvidedElsewhere( dv.getProvidedElsewhere() );
            audit.setProgramStageInstance( psi );
            audit.setValue( persistedDataValue != null ? persistedDataValue : dv.getValue() );
            audit.setDataElement( workContext.getDataElementMap().get( dv.getDataElement() ) );
            audit.setModifiedBy( currentUserService.getCurrentUsername() );

            entityDataValueAuditService.addTrackedEntityDataValueAudit( audit );
        }
    }

    private void incrementSummaryTotals( final List<Event> events, final ImportSummaries importSummaries,
        final ImportStrategy importStrategy )
    {
        for ( final Event event : events )
        {
            if ( !importSummaries.getByReference( event.getUid() ).isPresent() )
            {
                final ImportSummary is = new ImportSummary();
                is.setReference( event.getUid() );
                is.setStatus( SUCCESS );

                switch ( importStrategy )
                {
                case CREATE:
                    is.incrementImported();
                    break;

                case UPDATE:
                    is.incrementUpdated();
                    break;

                case DELETE:
                    is.incrementDeleted();
                    is.setDescription( "Deletion of event " + event.getUid() + " was successful" );
                    break;

                default:
                    log.warn( "Invalid Import Strategy during summary increment, ignoring" );
                }

                importSummaries.addImportSummary( is );
            }
        }
    }

    private void handleFailure( final WorkContext workContext, final ImportSummaries importSummaries,
        final List<Event> validEvents, final String errorMessage, final ImportStrategy importStrategy )
    {
        final List<Event> failedEvents = retryEach( workContext, validEvents, importStrategy );

        failedEvents.forEach( failedEvent -> importSummaries.getImportSummaries()
            .add( error( errorMessage, failedEvent.getEvent() ) ) );
    }

    /**
     * Filters out Events which are already present in the database (regardless
     * of the 'deleted' state) as well as duplicates within the payload (if
     * stage is not repeatable)
     *
     * @param events Events to import
     * @param importSummaries ImportSummaries used for import
     * @return Events that is possible to import (pass validation)
     */
    private List<Event> resolveImportableEvents( final List<Event> events, final ImportSummaries importSummaries,
        final WorkContext workContext )
    {
        List<Event> importableEvents = new ArrayList<>();
        Set<String> importableStageEvents = new HashSet<>();
        final Set<String> existingProgramStageInstances = workContext.getProgramStageInstanceMap().keySet();

        for ( Event eventToImport : events )
        {
            if ( existingProgramStageInstances.contains( eventToImport.getUid() ) )
            {
                final ImportSummary is = new ImportSummary( ERROR,
                    "Event " + eventToImport.getUid() + " already exists or was deleted earlier" )
                        .setReference( eventToImport.getUid() ).incrementIgnored();

                importSummaries.addImportSummary( is );

                continue;
            }

            Program program = workContext.getProgramsMap().get( eventToImport.getProgram() );

            ProgramStage programStage = workContext.getProgramStage( IdScheme.UID, eventToImport.getProgramStage() );

            if ( program != null && programStage != null && program.isRegistration() && !programStage.getRepeatable() )
            {
                String eventContextId = programStage.getUid() + "-" + eventToImport.getEnrollment();

                if ( importableStageEvents.contains( eventContextId ) )
                {
                    final ImportSummary is = new ImportSummary( ERROR,
                        "ProgramStage " + eventToImport.getProgramStage()
                            + " is not repeatable. Current payload contains duplicate event" )
                                .setReference( eventToImport.getUid() ).incrementIgnored();

                    importSummaries.addImportSummary( is );
                }
                else
                {
                    importableEvents.add( eventToImport );

                    importableStageEvents.add( eventContextId );
                }
            }
            else
            {
                importableEvents.add( eventToImport );
            }
        }

        return importableEvents;
    }

    private List<Event> retryEach( final WorkContext workContext, final List<Event> retryEvents,
        final ImportStrategy importStrategy )
    {
        final List<Event> failedEvents = new ArrayList<>( 0 );

        for ( final Event event : retryEvents )
        {
            try
            {
                if ( UPDATE == importStrategy )
                {
                    eventPersistenceService.update( workContext, singletonList( event ) );
                }
                else if ( CREATE == importStrategy )
                {
                    eventPersistenceService.save( workContext, singletonList( event ) );
                }
                else
                {
                    eventPersistenceService.delete( workContext, singletonList( event ) );
                }
            }
            catch ( Exception e )
            {
                failedEvents.add( event );
            }
        }

        return failedEvents;
    }
}
