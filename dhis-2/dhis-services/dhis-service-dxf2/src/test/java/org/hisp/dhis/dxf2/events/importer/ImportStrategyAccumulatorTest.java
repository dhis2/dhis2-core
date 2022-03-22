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
package org.hisp.dhis.dxf2.events.importer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.ProgramStageInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImportStrategyAccumulatorTest
{

    private ImportStrategyAccumulator accumulator;

    @BeforeEach
    void setUp()
    {
        accumulator = new ImportStrategyAccumulator();
    }

    @Test
    void verifyCreationOnly()
    {
        accumulator.partitionEvents( createEvents( 5 ), ImportStrategy.CREATE, new HashMap<>() );
        assertAccumulator( 5, 0, 0 );
        reset();
        accumulator.partitionEvents( createEvents( 5 ), ImportStrategy.CREATE,
            createProgramStageInstances( "a", "b" ) );
        assertAccumulator( 5, 0, 0 );
        reset();
        accumulator.partitionEvents( createEvents( 5 ), ImportStrategy.NEW, new HashMap<>() );
        assertAccumulator( 5, 0, 0 );
    }

    @Test
    void verifyCreateAndUpdate()
    {
        accumulator.partitionEvents( createEvents( 5 ), ImportStrategy.CREATE_AND_UPDATE, new HashMap<>() );
        assertAccumulator( 5, 0, 0 );
        reset();
        final List<Event> events = createEvents( 5 );
        accumulator.partitionEvents( events, ImportStrategy.CREATE_AND_UPDATE, createProgramStageInstances(
            events.get( 0 ).getEvent(), events.get( 1 ).getEvent(), events.get( 2 ).getEvent() ) );
        assertAccumulator( 2, 3, 0 );
    }

    @Test
    void verifyUpdateOnly()
    {
        accumulator.partitionEvents( createEvents( 5 ), ImportStrategy.UPDATE, new HashMap<>() );
        assertAccumulator( 0, 5, 0 );
        reset();
        accumulator.partitionEvents( createEvents( 5 ), ImportStrategy.UPDATES, new HashMap<>() );
        assertAccumulator( 0, 5, 0 );
    }

    @Test
    void verifyDeleteOnly()
    {
        List<Event> events = createEvents( 5 );
        accumulator.partitionEvents( events, ImportStrategy.DELETE, existingEventsFromEvents( events ) );
        assertAccumulator( 0, 0, 5 );
        reset();
        events = createEvents( 5 );
        accumulator.partitionEvents( events, ImportStrategy.DELETES, existingEventsFromEvents( events ) );
        assertAccumulator( 0, 0, 5 );
    }

    @Test
    void verifyDeleteSome()
    {
        List<Event> events = createEvents( 5 );
        reset();
        events = createEvents( 5 );
        accumulator.partitionEvents( events, ImportStrategy.DELETES, existingEventsFromEvents( events, 3 ) );
        assertAccumulator( 0, 0, 3 );
    }

    private Map<String, ProgramStageInstance> existingEventsFromEvents( List<Event> events )
    {
        return existingEventsFromEvents( events, events.size() );
    }

    private Map<String, ProgramStageInstance> existingEventsFromEvents( List<Event> events, Integer limit )
    {
        return events.stream().limit( limit )
            .collect( Collectors.toMap( Event::getEvent, event -> createProgramStageInstance( event.getEvent() ) ) );
    }

    @Test
    void verifySync()
    {
        accumulator.partitionEvents( createEvents( 5 ), ImportStrategy.SYNC, new HashMap<>() );
        assertAccumulator( 5, 0, 0 );
        reset();
        final List<Event> events1 = createEvents( 3 );
        events1.get( 0 ).setDeleted( true );
        events1.get( 1 ).setDeleted( true );
        accumulator.partitionEvents( events1, ImportStrategy.SYNC, new HashMap<>() );
        assertAccumulator( 1, 0, 2 );
        reset();
        final List<Event> events2 = createEvents( 10 );
        events2.get( 0 ).setDeleted( true );
        events2.get( 1 ).setDeleted( true );
        accumulator.partitionEvents( events2, ImportStrategy.SYNC, createProgramStageInstances(
            events2.get( 5 ).getEvent(), events2.get( 6 ).getEvent(), events2.get( 7 ).getEvent() ) );
        assertAccumulator( 5, 3, 2 );
    }

    private void reset()
    {
        this.accumulator = new ImportStrategyAccumulator();
    }

    private void assertAccumulator( int createSize, int updateSize, int deleteSize )
    {
        assertThat( "Wrong number of events for creation", accumulator.getCreate(), hasSize( createSize ) );
        assertThat( "Wrong number of events for update", accumulator.getUpdate(), hasSize( updateSize ) );
        assertThat( "Wrong number of events for deletion", accumulator.getDelete(), hasSize( deleteSize ) );
    }

    private Event createEvent()
    {
        Event e = new Event();
        String eventUid = CodeGenerator.generateUid();
        e.setEvent( eventUid );
        e.setUid( eventUid );
        return e;
    }

    private ProgramStageInstance createProgramStageInstance( String uid )
    {
        ProgramStageInstance psi = new ProgramStageInstance();
        psi.setUid( uid );
        return psi;
    }

    private List<Event> createEvents( int amount )
    {
        List<Event> events = new ArrayList<>();
        for ( int i = 0; i < amount; i++ )
        {
            events.add( createEvent() );
        }
        return events;
    }

    private Map<String, ProgramStageInstance> createProgramStageInstances( String... uids )
    {
        Map<String, ProgramStageInstance> psi = new HashMap<>();
        for ( final String uid : uids )
        {
            ProgramStageInstance p = createProgramStageInstance( uid );
            psi.put( p.getUid(), p );
        }
        return psi;
    }
}
