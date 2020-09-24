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

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventStore;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.mapper.ProgramStageInstanceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Luciano Fiandesio
 */
@Service
@Slf4j
@AllArgsConstructor
public class DefaultEventPersistenceService implements EventPersistenceService
{
    @NonNull
    private final EventStore jdbcEventStore;

    @Override
    @Transactional
    public void save( WorkContext context, List<Event> events )
    {
        if ( isNotEmpty( events ) )
        {
            ProgramStageInstanceMapper mapper = new ProgramStageInstanceMapper( context );

            jdbcEventStore.saveEvents( events.stream().map( mapper::map ).collect( Collectors.toList() ) );

            if ( !context.getImportOptions().isSkipLastUpdated() )
            {
                updateTeis( context, events );
            }
        }
    }

    @Override
    @Transactional
    public void update( final WorkContext context, final List<Event> events )
    {
        if ( isNotEmpty( events ) )
        {
            ProgramStageInstanceMapper mapper = new ProgramStageInstanceMapper( context );
            
            jdbcEventStore.updateEvents( events.stream().map( mapper::map ).collect( Collectors.toList() ) );

            if ( !context.getImportOptions().isSkipLastUpdated() )
            {
                updateTeis( context, events );
            }
        }
    }

    /**
     * Deletes the list of events using a single transaction.
     *
     * @param context a {@see WorkContext}
     * @param events a List of {@see Event}
     */
    @Override
    @Transactional
    public void delete( final WorkContext context, final List<Event> events )
    {
        if ( isNotEmpty( events ) )
        {
            jdbcEventStore.delete( events );
        }
    }

    /**
     * Updates the "lastupdated" and "lastupdatedBy" of the
     * Tracked Entity Instances linked to the provided list of Events.
     *
     * @param context a {@see WorkContext}
     * @param events a List of {@see Event}
     */
    private void updateTeis( final WorkContext context, final List<Event> events )
    {
        // Make sure that the TEI uids are not duplicated
        final List<String> distinctTeiList = events.stream()
            .map( e -> context.getTrackedEntityInstance( e.getUid() ) )
            .filter( Optional::isPresent )
            .map( o -> o.get().getUid() )
            .distinct().collect( Collectors.toList() );

        jdbcEventStore.updateTrackedEntityInstances( distinctTeiList, context.getImportOptions().getUser() );
    }
}
