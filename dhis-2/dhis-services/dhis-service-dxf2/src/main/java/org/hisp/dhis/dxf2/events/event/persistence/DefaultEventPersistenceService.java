package org.hisp.dhis.dxf2.events.event.persistence;

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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.dxf2.events.event.EventUtils.getValidUsername;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.JdbcEventStore;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.event.mapper.ProgramStageInstanceMapper;
import org.hisp.dhis.dxf2.events.event.validation.WorkContext;

import org.hisp.dhis.dxf2.events.eventdatavalue.EventDataValueService;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;

import org.springframework.stereotype.Service;

/**
 * @author Luciano Fiandesio
 */
@Service
@Slf4j
public class DefaultEventPersistenceService
    implements
    EventPersistenceService
{
    private final JdbcEventStore jdbcEventStore;

    private final EventDataValueService eventDataValueService;

    private final ProgramStageInstanceStore programStageInstanceStore;
    
    private final IdentifiableObjectManager identifiableObjectManager;
    
    private final TrackedEntityCommentService trackedEntityCommentService;

    public DefaultEventPersistenceService( JdbcEventStore jdbcEventStore, EventDataValueService eventDataValueService,
        ProgramStageInstanceStore programStageInstanceStore, IdentifiableObjectManager identifiableObjectManager,
        TrackedEntityCommentService trackedEntityCommentService )
    {
        checkNotNull( jdbcEventStore );
        checkNotNull( eventDataValueService );
        checkNotNull( programStageInstanceStore );
        checkNotNull( identifiableObjectManager );
        checkNotNull( trackedEntityCommentService );

        this.jdbcEventStore = jdbcEventStore;
        this.eventDataValueService = eventDataValueService;
        this.programStageInstanceStore = programStageInstanceStore;
        this.identifiableObjectManager = identifiableObjectManager;
        this.trackedEntityCommentService = trackedEntityCommentService;
    }

    @Override
    @Transactional
    public List<ProgramStageInstance> save(WorkContext context, List<Event> events )
    {
        final Map<Event, ProgramStageInstance> eventProgramStageInstanceMap = convertToProgramStageInstances(
            new ProgramStageInstanceMapper( context ), events );

        if ( isNotEmpty(events) )
        {
            // ...
            // save events and notes
            // ...
            jdbcEventStore.saveEvents( new ArrayList<>( eventProgramStageInstanceMap.values() ) );

            ImportSummary importSummary = new ImportSummary();

            // ...
            // process data values
            // ...
            Stopwatch stopwatch = Stopwatch.createStarted();

            eventDataValueService.processDataValues( eventProgramStageInstanceMap, false, context.getImportOptions(),
                importSummary, context.getDataElementMap() );
            log.debug( "Event save ::: Processing Data Value for {} PSIs took {}", eventProgramStageInstanceMap.size(),
                stopwatch.stop().elapsed( TimeUnit.MILLISECONDS ) );


        }
        return null; // TODO

    }

    @Override
    @Transactional
    public List<ProgramStageInstance> update(final WorkContext context, final List<Event> events )
    {
        if ( isNotEmpty( events ) )
        {
            // FIXME: Implement and add the correct mapper.
            final Map<Event, ProgramStageInstance> eventProgramStageInstanceMap = convertToProgramStageInstances(
                new ProgramStageInstanceMapper( context ), events );

            // TODO: Implement the jdbcEventStore.updateEvents method
            jdbcEventStore.updateEvents( new ArrayList<>(  eventProgramStageInstanceMap.values() ) );

            long now = System.nanoTime();

            ImportSummary importSummary = new ImportSummary();

            // ...
            // process data values
            // ...
            eventDataValueService.processDataValues( eventProgramStageInstanceMap, false, context.getImportOptions(), importSummary,
                    context.getDataElementMap() );

            // TODO this for loop slows down the process...
            for ( final ProgramStageInstance programStageInstance : eventProgramStageInstanceMap.values() )
            {
//                final ImportSummary importSummary1 = new ImportSummary( programStageInstance.getUid() );
                final Event event = getEvent( programStageInstance.getUid(), events );
                final ImportOptions importOptions = context.getImportOptions();

                saveTrackedEntityComment( programStageInstance, event, importOptions );

                // TODO: Find how to bring this into the update transaction.
                context.getTrackedEntityInstanceMap().values().forEach( tei -> identifiableObjectManager.update( tei, importOptions.getUser() ) );
            }

            System.out.println( "UPDATE: Processing Data Value for " + eventProgramStageInstanceMap.size() + " PSI took : "
                + TimeUnit.SECONDS.convert( System.nanoTime() - now, TimeUnit.NANOSECONDS ) );
        }

        return null;

    }

    private Event getEvent( String uid, List<Event> events )
    {
        return events.stream().filter( event -> event.getUid().equals( uid ) ).findFirst().get();
    }

    private Map<Event, ProgramStageInstance> convertToProgramStageInstances( ProgramStageInstanceMapper mapper,
        List<Event> events )
    {
        return events.stream().collect( Collectors.toMap( Function.identity(), mapper::map ) );
    }

    private void saveTrackedEntityComment( final ProgramStageInstance programStageInstance, final Event event,
        final ImportOptions importOptions )
    {
        for ( final Note note : event.getNotes() )
        {
            final String noteUid = isValidUid( note.getNote() ) ? note.getNote() : generateUid();

            if ( !trackedEntityCommentService.trackedEntityCommentExists( noteUid ) && !isEmpty( note.getValue() ) )
            {
                final TrackedEntityComment comment = new TrackedEntityComment();
                comment.setUid( noteUid );
                comment.setCommentText( note.getValue() );
                comment.setCreator( getValidUsername( note.getStoredBy(), importOptions ) );

                final Date created = parseDate( note.getStoredDate() );
                comment.setCreated( created );

                trackedEntityCommentService.addTrackedEntityComment( comment );

                programStageInstance.getComments().add( comment );
            }
        }
    }
}
