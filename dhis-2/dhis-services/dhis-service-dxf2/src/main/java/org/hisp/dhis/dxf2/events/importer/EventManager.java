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

package org.hisp.dhis.dxf2.events.importer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.dxf2.importsummary.ImportStatus.ERROR;
import static org.hisp.dhis.dxf2.importsummary.ImportStatus.SUCCESS;
import static org.hisp.dhis.dxf2.importsummary.ImportSummary.error;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE;
import static org.hisp.dhis.importexport.ImportStrategy.DELETE;
import static org.hisp.dhis.importexport.ImportStrategy.UPDATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.persistence.EventPersistenceService;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.delete.validation.DeleteValidationFactory;
import org.hisp.dhis.dxf2.events.importer.insert.validation.InsertValidationFactory;
import org.hisp.dhis.dxf2.events.importer.update.validation.UpdateValidationFactory;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Component
public class EventManager
{
    private final EventChecking insertValidationFactory;

    private final EventChecking updateValidationFactory;

    private final EventChecking deleteValidationFactory;

    private final EventProcessing preInsertProcessorFactory;

    private final EventProcessing preUpdateProcessorFactory;

    private final EventProcessing postUpdateProcessorFactory;

    private final EventPersistenceService eventPersistenceService;

    public EventManager(
        @Qualifier( "eventsInsertValidationFactory" )
        final InsertValidationFactory insertValidationFactory,
        @Qualifier( "eventsUpdateValidationFactory" )
        final UpdateValidationFactory updateValidationFactory,
        @Qualifier( "eventsDeleteValidationFactory" )
        final DeleteValidationFactory deleteValidationFactory,
        @Qualifier( "eventsPreInsertProcessorFactory" )
        final EventProcessing preInsertProcessorFactory,
        @Qualifier( "eventsPreUpdateProcessorFactory" )
        final EventProcessing preUpdateProcessorFactory,
        @Qualifier( "eventsPostUpdateProcessorFactory" )
        final EventProcessing postUpdateProcessorFactory,
        final EventPersistenceService eventPersistenceService )
    {
        checkNotNull( insertValidationFactory );
        checkNotNull( updateValidationFactory );
        checkNotNull( deleteValidationFactory );
        checkNotNull( preInsertProcessorFactory );
        checkNotNull( preUpdateProcessorFactory );
        checkNotNull( postUpdateProcessorFactory );
        checkNotNull( eventPersistenceService );

        this.insertValidationFactory = insertValidationFactory;
        this.updateValidationFactory = updateValidationFactory;
        this.deleteValidationFactory = deleteValidationFactory;
        this.preInsertProcessorFactory = preInsertProcessorFactory;
        this.preUpdateProcessorFactory = preUpdateProcessorFactory;
        this.postUpdateProcessorFactory = postUpdateProcessorFactory;
        this.eventPersistenceService = eventPersistenceService;
    }

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

        // filter out events which are already in the database
        List<Event> validEvents = resolveImportableEvents( events, importSummaries, workContext );

        if ( validEvents.isEmpty() )
        {
            return importSummaries;
        }

        // pre-process events
        preInsertProcessorFactory.process( workContext, validEvents );

        // @formatter:off
        importSummaries.addImportSummaries(
            // Run validation against the remaining "insertable" events //
            insertValidationFactory.check( workContext, validEvents )
        );
        // @formatter:on

        // collect the UIDs of events that did not pass validation
        final List<String> invalidEvents = importSummaries.getImportSummaries().stream()
            .filter( i -> i.isStatus( ERROR ) ).map( ImportSummary::getReference ).collect( toList() );

        if ( invalidEvents.size() == validEvents.size() )
        {
            return importSummaries;
        }

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
                handleFailure( workContext, importSummaries, events, "Invalid or conflicting data", CREATE );

                return importSummaries;
            }
        }

        incrementSummaryTotals( events, importSummaries, CREATE );

        return importSummaries;
    }

    public ImportSummary updateEvent( final Event event, final WorkContext workContext )
    {
        final List<Event> singleEvent = Collections.singletonList( event );

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
        preUpdateProcessorFactory.process( workContext, events );

        // @formatter:off
        importSummaries.addImportSummaries(
            // Run validation against the remaining "updatable" events //
            updateValidationFactory.check( workContext, events)
        );
        // @formatter:on

        // collect the UIDs of events that did not pass validation
        final List<String> eventValidationFailedUids = importSummaries.getImportSummaries().stream()
            .filter( i -> i.isStatus( ERROR ) )
            .map( ImportSummary::getReference ).collect( toList() );

        if ( eventValidationFailedUids.size() == events.size() )
        {
            return importSummaries;
        }

        try
        {
            eventPersistenceService.update( workContext,
                eventValidationFailedUids.isEmpty() ? events
                    : events.stream().filter( e -> !eventValidationFailedUids.contains( e.getEvent() ) )
                        .collect( toList() ) );
        }
        catch ( Exception e )
        {
            handleFailure( workContext, importSummaries, events, "Invalid or conflicting data", UPDATE );
        }

        final List<String> eventPersistenceFailedUids = importSummaries.getImportSummaries().stream()
            .filter( i -> i.isStatus( ERROR ) ).map( ImportSummary::getReference ).collect( toList() );

        // Post processing only the events that passed validation and ware persisted
        // correctly.
        postUpdateProcessorFactory.process( workContext, events.stream()
            .filter( e -> !eventPersistenceFailedUids.contains( e.getEvent() ) ).collect( toList() ) );

        incrementSummaryTotals( events, importSummaries, UPDATE );

        return importSummaries;
    }

    public ImportSummaries deleteEvents( final List<Event> events, final WorkContext workContext )
    {
        final ImportSummaries importSummaries = new ImportSummaries();

        // @formatter:off
        importSummaries.addImportSummaries(
            // Run validation against the remaining "insertable" events //
            deleteValidationFactory.check( workContext, events )
        );
        // @formatter:on

        // collect the UIDs of events that did not pass validation
        final List<String> eventValidationFailedUids = importSummaries.getImportSummaries().stream()
            .filter( i -> i.isStatus( ERROR ) )
            .map( ImportSummary::getReference ).collect( toList() );

        if ( eventValidationFailedUids.size() == events.size() )
        {
            return importSummaries;
        }

        try
        {
            eventPersistenceService.delete( workContext,
                eventValidationFailedUids.isEmpty() ? events
                    : events.stream().filter( e -> !eventValidationFailedUids.contains( e.getEvent() ) )
                        .collect( toList() ) );
        }
        catch ( Exception e )
        {
            handleFailure( workContext, importSummaries, events, "Invalid or conflicting data", UPDATE );
        }

        incrementSummaryTotals( events, importSummaries, DELETE );

        return importSummaries;
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
                }

                importSummaries.addImportSummary( is );
            }
            else
            {
                final Optional<ImportSummary> isOptional = importSummaries.getByReference( event.getUid() );

                if ( isOptional.isPresent() )
                {
                    final ImportSummary is = isOptional.get();
                    is.setStatus( ERROR );
                    is.incrementIgnored();
                }
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
     * Filters out Events which are already present in the database (regardless of
     * the 'deleted' state)
     *
     * @param events Events to import
     * @param importSummaries ImportSummaries used for import
     * @return Events that is possible to import (pass validation)
     */
    private List<Event> resolveImportableEvents( final List<Event> events, final ImportSummaries importSummaries,
        final WorkContext workContext )
    {
        List<Event> importableEvents = new ArrayList<>();
        if ( CollectionUtils.isNotEmpty( events ) )
        {
            final Set<String> existingProgramStageInstances = workContext.getProgramStageInstanceMap().keySet();

            for ( Event eventToImport : events )
            {
                if ( existingProgramStageInstances.contains( eventToImport.getUid() ) )
                {
                    final ImportSummary is = new ImportSummary( ERROR,
                        "Event " + eventToImport.getUid() + " already exists or was deleted earlier" )
                            .setReference( eventToImport.getUid() ).incrementIgnored();

                    importSummaries.addImportSummary( is );
                }
                else
                {
                    importableEvents.add( eventToImport );
                }
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
                    eventPersistenceService.update( workContext, Collections.singletonList( event ) );
                }
                else if ( CREATE == importStrategy )
                {
                    eventPersistenceService.save( workContext, Collections.singletonList( event ) );
                }
                else
                {
                    eventPersistenceService.delete( workContext, event );
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
