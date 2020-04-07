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

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    public List<ProgramStageInstance> save(WorkContext context, List<Event> events )
    {
        List<ProgramStageInstance> programStageInstances = convertToProgramStageInstances(
            new ProgramStageInstanceMapper( context ), events );

        if ( !events.isEmpty() )
        {
            jdbcEventStore.saveEvents( programStageInstances );

            // TODO save notes too

            long now = System.nanoTime();
            // TODO this for loop slows down the process 5X
            for ( ProgramStageInstance programStageInstance : programStageInstances )
            {
                ImportSummary importSummary = new ImportSummary( programStageInstance.getUid() );
                eventDataValueService.processDataValues(
                    programStageInstanceStore.getByUid( programStageInstance.getUid() ),
                    getEvent( programStageInstance.getUid(), events ), false, context.getImportOptions(), importSummary,
                    context.getDataElementMap() );
            }
            System.out.println( "Processing Data Value for " + programStageInstances.size() + " PSI took : "
                    + TimeUnit.SECONDS.convert( System.nanoTime() - now, TimeUnit.NANOSECONDS ) );

        }
        return null; // TODO

    }

    @Override
    public List<ProgramStageInstance> update(final WorkContext context, final List<Event> events )
    {
        if ( isNotEmpty( events ) )
        {
            // FIXME: Implement and add the correct mapper.
            final List<ProgramStageInstance> programStageInstances = convertToProgramStageInstances(
                new ProgramStageInstanceMapper( context ), events );

            // TODO: Implement the jdbcEventStore.updateEvents method
            jdbcEventStore.updateEvents( programStageInstances );

            long now = System.nanoTime();

            // TODO this for loop slows down the process...
            for ( final ProgramStageInstance programStageInstance : programStageInstances )
            {
                final ImportSummary importSummary = new ImportSummary( programStageInstance.getUid() );
                final Event event = getEvent( programStageInstance.getUid(), events );
                final ImportOptions importOptions = context.getImportOptions();

                saveTrackedEntityComment( programStageInstance, event, importOptions );

                eventDataValueService.processDataValues(
                    programStageInstanceStore.getByUid( programStageInstance.getUid() ),
                    event, false, importOptions, importSummary,
                    context.getDataElementMap() );

                // TODO: Find how to bring this into the update transaction.
                context.getTrackedEntityInstanceMap().values().forEach( tei -> identifiableObjectManager.update( tei, importOptions.getUser() ) );
            }

            System.out.println( "UPDATE: Processing Data Value for " + programStageInstances.size() + " PSI took : "
                + TimeUnit.SECONDS.convert( System.nanoTime() - now, TimeUnit.NANOSECONDS ) );
        }

        return null;

    }

    private Event getEvent( String uid, List<Event> events )
    {
        return events.stream().filter( event -> event.getUid().equals( uid ) ).findFirst().get();
    }

    private List<ProgramStageInstance> convertToProgramStageInstances( ProgramStageInstanceMapper mapper,
        List<Event> events )
    {
        return events.stream().map( mapper::convert ).collect( Collectors.toList() );
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
