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
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.dxf2.importsummary.ImportStatus.ERROR;
import static org.hisp.dhis.dxf2.importsummary.ImportStatus.SUCCESS;
import static org.hisp.dhis.dxf2.importsummary.ImportSummary.error;
import static org.hisp.dhis.importexport.ImportStrategy.DELETE;
import static org.hisp.dhis.importexport.ImportStrategy.UPDATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.persistence.EventPersistenceService;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.delete.validation.DeleteValidationFactory;
import org.hisp.dhis.dxf2.events.importer.insert.validation.InsertValidationFactory;
import org.hisp.dhis.dxf2.events.importer.update.validation.UpdateValidationFactory;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.ProgramStageInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

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
        @Qualifier("eventsInsertValidationFactory") final InsertValidationFactory insertValidationFactory,
        @Qualifier("eventsUpdateValidationFactory") final UpdateValidationFactory updateValidationFactory,
        @Qualifier("eventsDeleteValidationFactory") final DeleteValidationFactory deleteValidationFactory,
        @Qualifier( "eventsPreInsertProcessorFactory" ) final EventProcessing preInsertProcessorFactory,
        @Qualifier( "eventsPreUpdateProcessorFactory" ) final EventProcessing preUpdateProcessorFactory,
        @Qualifier("eventsPostUpdateProcessorFactory") final EventProcessing postUpdateProcessorFactory,
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
        final List<Event> singleEvent = ImmutableList.of(event);

        final ImportSummaries importSummaries = addEvents( singleEvent, workContext );

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

        final List<ProgramStageInstance> persisted;

        if ( invalidEvents.isEmpty() )
        {
            persisted = eventPersistenceService.save( workContext, validEvents );

        }
        else
        {
            // collect the events that passed validation and can be persisted
            // @formatter:off
            persisted = eventPersistenceService.save( workContext, validEvents.stream()
                .filter( e -> !invalidEvents.contains( e.getEvent() ) )
                .collect( toList() ) );
            // @formatter:on
        }
        List<String> persistedUid = persisted.stream().map( BaseIdentifiableObject::getUid ).collect( toList() );

        for ( Event event : events )
        {
            if ( !importSummaries.getByReference( event.getUid() ).isPresent() )
            {
                ImportSummary is = new ImportSummary();
                is.setReference( event.getUid() );
                if ( persistedUid.contains( event.getUid() ) )
                {
                    is.setStatus( SUCCESS );
                    is.incrementImported();
                }
                else
                {
                    is.setStatus( ERROR );
                    is.incrementIgnored();
                }
                importSummaries.addImportSummary( is );
            }
        }
        
        return importSummaries;
    }

    public ImportSummary updateEvent( final Event event, final WorkContext workContext )
    {
        final List<Event> singleEvent = asList( event );

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

        // filter out events which are already in the database
        // TODO: Is it needed for Update? Removing for now: resolveImportableEvents(
        // events, importSummaries, ctx );
        final List<Event> validEvents = events;

        // pre-process events
        preUpdateProcessorFactory.process( workContext, events );

        // @formatter:off
        importSummaries.addImportSummaries(
            // Run validation against the remaining "updatable" events //
            updateValidationFactory.check( workContext, validEvents )
        );
        // @formatter:on

        // collect the UIDs of events that did not pass validation
        final List<String> eventValidationFailedUids = importSummaries.getImportSummaries().stream().filter( i -> i.isStatus( ERROR ) )
            .map( ImportSummary::getReference ).collect( toList() );


        if ( eventValidationFailedUids.size() == validEvents.size() )
        {
            return importSummaries;
        }

        if ( eventValidationFailedUids.isEmpty() )
        {
            try
            {
                eventPersistenceService.update( workContext, validEvents );
            }
            catch ( Exception e )
            {
                handleFailure( workContext, importSummaries, validEvents, "Invalid or conflicting data", UPDATE );
            }
        }
        else
        {
            try
            {
                // collect the events that passed validation and can be persisted
                eventPersistenceService.update( workContext, validEvents.stream()
                    .filter( e -> !eventValidationFailedUids.contains( e.getEvent() ) ).collect( toList() ) );
            }
            catch ( Exception e )
            {
                handleFailure( workContext, importSummaries, validEvents, "Invalid or conflicting data", UPDATE );
            }
        }

        final List<String> eventPersistenceFailedUids = importSummaries.getImportSummaries().stream()
            .filter( i -> i.isStatus( ERROR ) ).map( ImportSummary::getReference ).collect( toList() );

        // Post processing only the events that passed validation and ware persisted correctly.
        postUpdateProcessorFactory.process( workContext, validEvents.stream()
            .filter( e -> !eventPersistenceFailedUids.contains( e.getEvent() ) ).collect( toList() ) );

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
        final List<String> eventValidationFailedUids = importSummaries.getImportSummaries().stream().filter( i -> i.isStatus( ERROR ) )
            .map( ImportSummary::getReference ).collect( toList() );

        if ( eventValidationFailedUids.size() == events.size() )
        {
            return importSummaries;
        }

        if ( eventValidationFailedUids.isEmpty() )
        {
            try
            {
                eventPersistenceService.delete( workContext, events );
            }
            catch ( Exception e )
            {
                handleFailure( workContext, importSummaries, events, "Could not delete event", DELETE );
            }
        }
        else
        {
            try
            {
                // collect the events that passed validation and can be deleted
                eventPersistenceService.delete( workContext,
                    events.stream().filter( e -> !eventValidationFailedUids.contains( e.getEvent() ) ).collect( toList() ) );
            }
            catch ( Exception e )
            {
                handleFailure( workContext, importSummaries, events, "Could not delete event", DELETE );
            }
        }

        return importSummaries;
    }

    private void handleFailure(final WorkContext workContext, final ImportSummaries importSummaries,
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
                    eventPersistenceService.update( workContext, event );
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
