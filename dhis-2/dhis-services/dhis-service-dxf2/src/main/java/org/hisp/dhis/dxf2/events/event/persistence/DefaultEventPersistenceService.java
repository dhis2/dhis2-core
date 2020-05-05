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
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.dxf2.events.event.EventUtils.getValidUsername;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventStore;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.event.context.WorkContext;
import org.hisp.dhis.dxf2.events.event.mapper.ProgramStageInstanceMapper;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Luciano Fiandesio
 */
@Service
@Slf4j
public class DefaultEventPersistenceService
    implements
    EventPersistenceService
{
    private final EventStore jdbcEventStore;

    private IdentifiableObjectManager manager;

    private final TrackedEntityCommentService trackedEntityCommentService;

    public DefaultEventPersistenceService( EventStore jdbcEventStore, IdentifiableObjectManager manager,
        TrackedEntityCommentService trackedEntityCommentService )
    {
        checkNotNull( jdbcEventStore );
        checkNotNull( manager );
        checkNotNull( trackedEntityCommentService );

        this.jdbcEventStore = jdbcEventStore;
        this.manager = manager;
        this.trackedEntityCommentService = trackedEntityCommentService;
    }

    @Override
    @Transactional
    public List<ProgramStageInstance> save( WorkContext context, List<Event> events )
    {
        /*
         * Save Events, Notes and Data Values
         */
        ProgramStageInstanceMapper mapper = new ProgramStageInstanceMapper( context );
        return isNotEmpty( events )
            ? jdbcEventStore.saveEvents( events.stream().map( mapper::map ).collect( Collectors.toList() ) )
            : new ArrayList<>();
    }

    // TODO do we need this?
    private void rollbackOnException( ImportSummaries importSummaries )
    {
        this.jdbcEventStore
            .delete( importSummaries.getImportSummaries().stream().filter( i -> !i.getConflicts().isEmpty() )
                .map( ImportSummary::getReference ).collect( Collectors.toList() ) );
    }

    /**
     * Updates the list of given events using a single transaction.
     *
     * @param context a {@see WorkContext}
     * @param events a List of {@see Event}
     */
    @Override
    @Transactional
    public void update( final WorkContext context, final List<Event> events ) throws JsonProcessingException {

        if ( isNotEmpty( events ) )
        {
            // TODO: Check if this mapper really suits the Update flow as well
            final Map<Event, ProgramStageInstance> eventProgramStageInstanceMap = convertToProgramStageInstances(
                new ProgramStageInstanceMapper( context ), events );

            long now = nanoTime();

            for ( final ProgramStageInstance programStageInstance : eventProgramStageInstanceMap.values() )
            {
                final Event event = getEvent( programStageInstance.getUid(), events );
                final ImportOptions importOptions = context.getImportOptions();
                final Collection<TrackedEntityInstance> trackedEntityInstances = context.getTrackedEntityInstanceMap()
                    .values();

                if ( event != null )
                {
                    persistUpdateData( programStageInstance, event.getNotes(), trackedEntityInstances, importOptions );
                }
            }

            System.out.println( "UPDATE: Processing Data Value for " + eventProgramStageInstanceMap.size()
                + " PSI took : " + SECONDS.convert( nanoTime() - now, NANOSECONDS ) );
        }
    }

    /**
     * Updates the given event using a single transaction.
     *
     * @param context
     * @param event
     * @return
     */
    @Override
    @Transactional
    public void update( final WorkContext context, final Event event ) throws JsonProcessingException {

        if ( context != null && event != null )
        {
            final ProgramStageInstance programStageInstance = new ProgramStageInstanceMapper( context ).map( event );
            final ImportOptions importOptions = context.getImportOptions();
            final Collection<TrackedEntityInstance> trackedEntityInstances = context.getTrackedEntityInstanceMap()
                .values();

            final long now = nanoTime();

            persistUpdateData( programStageInstance, event.getNotes(), trackedEntityInstances, importOptions );

            System.out.println( "UPDATE: Processing Data Value for " + programStageInstance + " PSI took : "
                + SECONDS.convert( nanoTime() - now, NANOSECONDS ) );
        }
    }

    private void persistUpdateData( final ProgramStageInstance programStageInstance, final List<Note> notes,
        final Collection<TrackedEntityInstance> trackedEntityInstances, final ImportOptions importOptions )
        throws JsonProcessingException
    {
        jdbcEventStore.updateEvent( programStageInstance );

        // TODO: Find how to bring this into the update transaction.
        // TODO: Maikel, what exactly needs to be updated here? Commented out for now
        for ( final TrackedEntityInstance tei : trackedEntityInstances )
        {
            updateTrackedEntityInstance( tei, importOptions.getUser() );
        }

        saveTrackedEntityComment( programStageInstance, notes, importOptions );
    }

    private void updateTrackedEntityInstance( final TrackedEntityInstance tei, final User user )
    {
        final TrackedEntityInstance loadedTei = manager.get( TrackedEntityInstance.class, tei.getUid() );

        // TODO: Discuss with Luciano: Should we add an ACL checking for
        // TrackedEntityInstance as part of validation? and do a strait JDBC update
        // here?
        loadedTei.setCreatedAtClient( tei.getCreatedAtClient() );
        loadedTei.setLastUpdatedAtClient( tei.getLastUpdatedAtClient() );
        loadedTei.setInactive( tei.isInactive() );
        loadedTei.setLastSynchronized( tei.getLastSynchronized() );
        loadedTei.setCreated( tei.getCreated() );
        loadedTei.setLastUpdated( tei.getLastUpdated() );
        loadedTei.setUser( tei.getUser() );
        loadedTei.setLastUpdatedBy( tei.getLastUpdatedBy() );

        manager.update( loadedTei, user );
    }

    private Event getEvent( String uid, List<Event> events )
    {
        return events.stream().filter( event -> trimToEmpty( uid ).equals( event.getEvent() ) ).findFirst()
            .get();
    }

    private Map<Event, ProgramStageInstance> convertToProgramStageInstances( ProgramStageInstanceMapper mapper,
        List<Event> events )
    {
        Map<Event, ProgramStageInstance> map = new HashMap<>();
        for ( Event event : events )
        {
            ProgramStageInstance psi = mapper.map( event );
            map.put( event, psi );
        }

        return map;
    }

    private void saveTrackedEntityComment( final ProgramStageInstance programStageInstance, final List<Note> notes,
        final ImportOptions importOptions )
    {
        for ( final Note note : notes )
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
