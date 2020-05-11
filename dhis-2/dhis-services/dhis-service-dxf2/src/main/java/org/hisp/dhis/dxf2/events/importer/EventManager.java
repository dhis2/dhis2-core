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
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.dxf2.importsummary.ImportStatus.ERROR;
import static org.hisp.dhis.dxf2.importsummary.ImportStatus.SUCCESS;
import static org.hisp.dhis.dxf2.importsummary.ImportSummary.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.persistence.EventPersistenceService;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.insert.validation.ValidationFactory;
import org.hisp.dhis.dxf2.events.importer.update.validation.UpdateValidationFactory;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.ProgramStageInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class EventManager
{
    private final ValidationFactory validationFactory;

    private final UpdateValidationFactory updateValidationFactory;

    private final EventProcessing preProcessorFactory;

    private final EventProcessing preUpdateProcessorFactory;

    private final EventPersistenceService eventPersistenceService;

    public EventManager( final ValidationFactory validationFactory,
        final UpdateValidationFactory updateValidationFactory, @Qualifier( "eventsPreProcessorFactory" )
        final EventProcessing preProcessorFactory, @Qualifier( "eventsPreUpdateProcessorFactory" )
        final EventProcessing preUpdateProcessorFactory, final EventPersistenceService eventPersistenceService )
    {
        checkNotNull( validationFactory );
        checkNotNull( updateValidationFactory );
        checkNotNull( preProcessorFactory );
        checkNotNull( preUpdateProcessorFactory );
        checkNotNull( eventPersistenceService );

        this.validationFactory = validationFactory;
        this.updateValidationFactory = updateValidationFactory;
        this.preProcessorFactory = preProcessorFactory;
        this.preUpdateProcessorFactory = preUpdateProcessorFactory;
        this.eventPersistenceService = eventPersistenceService;
    }

    public ImportSummary addEvent( final Event event, final WorkContext workContext )
    {
        final List<Event> singleEvent = asList( event );

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
        final List<Event> validEvents = resolveImportableEvents( events, importSummaries, workContext );

        // pre-process events
        preProcessorFactory.process( workContext, events );

        // @formatter:off
        importSummaries.addImportSummaries(
            // Run validation against the remaining "insertable" events //
            validationFactory.validateEvents( workContext, validEvents )
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

        for ( final ProgramStageInstance programStageInstance : persisted )
        {
            importSummaries.getByReference( programStageInstance.getUid() ).ifPresent( is -> {
                is.setStatus( SUCCESS );
                is.incrementImported();
            } );
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
            // Run validation against the remaining "insertable" events //
            updateValidationFactory.validateEvents( workContext, validEvents )
        );
        // @formatter:on

        // collect the UIDs of events that did not pass validation
        final List<String> failedUids = importSummaries.getImportSummaries().stream().filter( i -> i.isStatus( ERROR ) )
            .map( ImportSummary::getReference ).collect( toList() );


        if ( failedUids.size() == validEvents.size() )
        {
            return importSummaries;
        }

        if ( failedUids.isEmpty() )
        {
            try
            {
                eventPersistenceService.update( workContext, validEvents );
            }
            catch ( Exception e )
            {
                handleFailedUpdate( workContext, importSummaries, validEvents );
            }
        }
        else
        {
            try
            {
                // collect the events that passed validation and can be persisted
                eventPersistenceService.update( workContext,
                    validEvents.stream().filter( e -> !failedUids.contains( e.getEvent() ) ).collect( toList() ) );
            }
            catch ( Exception e )
            {
                handleFailedUpdate( workContext, importSummaries, validEvents );
            }
        }

        return importSummaries;
    }

    private void handleFailedUpdate( final WorkContext workContext, final ImportSummaries importSummaries,
        final List<Event> validEvents )
    {
        final List<Event> failedEvents = retryEach( workContext, validEvents );

        failedEvents.forEach( failedEvent -> importSummaries.getImportSummaries()
            .add( error( "Invalid or conflicting data", failedEvent.getEvent() ) ) );
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
        if (CollectionUtils.isNotEmpty(events)) {
            final Map<String, ProgramStageInstance> programStageInstanceMap = workContext.getProgramStageInstanceMap();

            for (final String foundEventUid : programStageInstanceMap.keySet()) {
                final ImportSummary is = new ImportSummary(ERROR,
                    "Event " + foundEventUid + " already exists or was deleted earlier").setReference(foundEventUid)
                    .incrementIgnored();

                importSummaries.addImportSummary(is);
            }

            return events.stream().filter(e -> !programStageInstanceMap.containsKey(e.getEvent())).collect(toList());
        }

        return emptyList();
    }

    private List<Event> retryEach( final WorkContext workContext, final List<Event> retryEvents )
    {
        final List<Event> failedEvents = new ArrayList<>( 0 );

        for ( final Event event : retryEvents )
        {
            try
            {
                eventPersistenceService.update( workContext, event );
            }
            catch ( Exception e )
            {
                failedEvents.add( event );
            }
        }

        return failedEvents;
    }
}
