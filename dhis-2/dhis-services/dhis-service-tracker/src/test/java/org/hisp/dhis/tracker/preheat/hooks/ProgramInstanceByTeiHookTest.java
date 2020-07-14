package org.hisp.dhis.tracker.preheat.hooks;

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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createTrackedEntityInstance;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class ProgramInstanceByTeiHookTest
{
    private ProgramInstanceByTeiHook hook;

    @Mock
    private ProgramInstanceStore programInstanceStore;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp()
    {
        this.hook = new ProgramInstanceByTeiHook( programInstanceStore );
    }

    @Test
    public void verifyProgramInstanceAreSetIntoPreheat()
    {
        // Given
        Program program1 = createProgram( 'A' );
        program1.setUid( CodeGenerator.generateUid() );

        OrganisationUnit ou1 = createOrganisationUnit( 'A' );
        TrackedEntityInstance t1 = createTrackedEntityInstance( 'A', ou1 );
        TrackedEntityInstance t2 = createTrackedEntityInstance( 'B', ou1 );
        TrackedEntityInstance t3 = createTrackedEntityInstance( 'C', ou1 );
        TrackedEntityInstance t4 = createTrackedEntityInstance( 'D', ou1 );

        ProgramInstance p1 = createProgramInstance( program1 );
        ProgramInstance p2 = createProgramInstance( program1 );
        ProgramInstance p3 = createProgramInstance( program1 );

        TrackerPreheatParams params = TrackerPreheatParams.builder().build();

        Event e1 = createEvent( p1, t1 );
        Event e2 = createEvent( p2, t2 );
        Event e3 = createEvent( p3, t3 );
        Event e4 = createEvent( null, program1, t4 );

        params.setEvents( Lists.newArrayList( e1, e2, e3, e4 ) );

        TrackerPreheat trackerPreheat = new TrackerPreheat();

        Map<TrackerIdScheme, Map<String, ProgramInstance>> enrollmentMap = new HashMap<>();
        Map<String, ProgramInstance> programInstanceMap = new HashMap<>();

        programInstanceMap.put( p1.getUid(), p1 );
        programInstanceMap.put( p2.getUid(), p2 );
        programInstanceMap.put( p3.getUid(), p3 );

        enrollmentMap.put( TrackerIdScheme.UID, programInstanceMap );
        trackerPreheat.setEnrollments( enrollmentMap );

        ProgramInstance p4 = new ProgramInstance();
        p4.setUid( CodeGenerator.generateUid() );
        p4.setProgram( program1 );
        p4.setEntityInstance( t4 );

        when( programInstanceStore.getByProgramAndTrackedEntityInstance( anyList(), eq(ProgramStatus.ACTIVE) ) )
                .thenReturn( Collections.singletonList( p4 ) );

        // When
        this.hook.preheat( params, trackerPreheat );

        // Then
        final Map<String, List<ProgramInstance>> programInstancesByProgramAndTei = trackerPreheat
                .getProgramInstances();
        assertThat( programInstancesByProgramAndTei, is( notNullValue() ) );
        assertThat( programInstancesByProgramAndTei.get( e4.getUid() ), hasSize( 1 ) );
        assertThat( programInstancesByProgramAndTei.get( e4.getUid() ).get( 0 ), is( p4 ) );
    }

    @Test
    public void verifyEventWithoutProgramOrTeiAreSkipped()
    {
        // Given
        Program program1 = createProgram( 'A' );
        program1.setUid( CodeGenerator.generateUid() );

        OrganisationUnit ou1 = createOrganisationUnit( 'A' );
        TrackedEntityInstance t1 = createTrackedEntityInstance( 'A', ou1 );
        TrackedEntityInstance t2 = createTrackedEntityInstance( 'B', ou1 );
        TrackedEntityInstance t3 = createTrackedEntityInstance( 'C', ou1 );
        TrackedEntityInstance t4 = createTrackedEntityInstance( 'D', ou1 );

        ProgramInstance p1 = createProgramInstance( program1 );
        ProgramInstance p2 = createProgramInstance( program1 );
        ProgramInstance p3 = createProgramInstance( program1 );

        TrackerPreheatParams params = TrackerPreheatParams.builder().build();

        Event e1 = createEvent( p1, t1 );
        Event e2 = createEvent( p2, t2 );
        Event e3 = createEvent( p3, t3 );
        // setting Program to null, will force this event to be skipped!
        Event e4 = createEvent( null, null, t4 );
        Event e5 = createEvent( p3, null, null );

        params.setEvents( Lists.newArrayList( e1, e2, e3, e4, e5) );

        TrackerPreheat trackerPreheat = new TrackerPreheat();

        Map<TrackerIdScheme, Map<String, ProgramInstance>> enrollmentMap = new HashMap<>();
        Map<String, ProgramInstance> programInstanceMap = new HashMap<>();

        programInstanceMap.put( p1.getUid(), p1 );
        programInstanceMap.put( p2.getUid(), p2 );
        programInstanceMap.put( p3.getUid(), p3 );

        enrollmentMap.put( TrackerIdScheme.UID, programInstanceMap );
        trackerPreheat.setEnrollments( enrollmentMap );

        ProgramInstance p4 = new ProgramInstance();
        p4.setUid( CodeGenerator.generateUid() );
        p4.setProgram( program1 );
        p4.setEntityInstance( t4 );

        when( programInstanceStore.getByProgramAndTrackedEntityInstance( anyList(), eq( ProgramStatus.ACTIVE ) ) )
                .thenReturn( Collections.singletonList( p4 ) );

        // When
        this.hook.preheat( params, trackerPreheat );

        // Then
        final Map<String, List<ProgramInstance>> programInstancesByProgramAndTei = trackerPreheat
                .getProgramInstances();
        assertThat( programInstancesByProgramAndTei, is( notNullValue() ) );
        assertThat( programInstancesByProgramAndTei.keySet(), hasSize( 0 ) );
    }

    private Event createEvent( ProgramInstance programInstance, Program program,
                               TrackedEntityInstance trackedEntityInstance )
    {
        Event event = new Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setEnrollment( programInstance == null ? null : programInstance.getUid() );
        event.setTrackedEntity( trackedEntityInstance == null ? null : trackedEntityInstance.getUid() );
        event.setProgram( program == null ? null : program.getUid() );
        return event;
    }

    private Event createEvent( ProgramInstance programInstance, TrackedEntityInstance trackedEntityInstance )
    {
        Event event = new Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setEnrollment( programInstance == null ? null : programInstance.getUid() );
        event.setTrackedEntity( trackedEntityInstance == null ? null : trackedEntityInstance.getUid() );
        event.setProgram( programInstance == null ? null : programInstance.getProgram().getUid() );
        return event;
    }

    private ProgramInstance createProgramInstance( Program program )
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setProgram( program );
        programInstance.setUid( CodeGenerator.generateUid() );
        return programInstance;
    }

}