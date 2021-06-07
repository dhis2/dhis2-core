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
package org.hisp.dhis.tracker.preprocess;

import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
@RunWith( MockitoJUnitRunner.class )
public class EventProgramPreProcessorTest
{

    private final static String PROGRAM_STAGE_WITH_REGISTRATION = "PROGRAM_STAGE_WITH_REGISTRATION";

    private final static String PROGRAM_STAGE_WITHOUT_REGISTRATION = "PROGRAM_STAGE_WITHOUT_REGISTRATION";

    private final static String PROGRAM_WITH_REGISTRATION = "PROGRAM_WITH_REGISTRATION";

    private final static String PROGRAM_WITHOUT_REGISTRATION = "PROGRAM_WITHOUT_REGISTRATION";

    @Mock
    private TrackerPreheat preheat;

    private EventProgramPreProcessor preProcessorToTest;

    @Before
    public void setUp()
    {
        when( preheat.get( Program.class, PROGRAM_WITHOUT_REGISTRATION ) )
            .thenReturn( programWithoutRegistrationWithProgramStages() );
        when( preheat.get( Program.class, PROGRAM_WITH_REGISTRATION ) )
            .thenReturn( programWithRegistrationWithProgramStages() );
        when( preheat.get( ProgramStage.class, PROGRAM_STAGE_WITHOUT_REGISTRATION ) )
            .thenReturn( programStageWithoutRegistration() );
        when( preheat.get( ProgramStage.class, PROGRAM_STAGE_WITH_REGISTRATION ) )
            .thenReturn( programStageWithRegistration() );

        this.preProcessorToTest = new EventProgramPreProcessor();
    }

    @Test
    public void testTrackerEventIsEnhancedWithProgram()
    {
        // Given
        TrackerBundle bundle = TrackerBundle.builder()
            .events( Collections.singletonList( trackerEventWithProgramStage() ) )
            .preheat( preheat )
            .build();

        // When
        preProcessorToTest.process( bundle );

        // Then
        verify( preheat ).put( TrackerIdentifier.UID, programWithRegistration() );
        assertEquals( PROGRAM_WITH_REGISTRATION, bundle.getEvents().get( 0 ).getProgram() );
    }

    @Test
    public void testProgramEventIsEnhancedWithProgram()
    {
        // Given
        TrackerBundle bundle = TrackerBundle.builder()
            .events( Collections.singletonList( programEventWithProgramStage() ) )
            .preheat( preheat )
            .build();

        // When
        preProcessorToTest.process( bundle );

        // Then
        verify( preheat ).put( TrackerIdentifier.UID, programWithoutRegistration() );
        assertEquals( PROGRAM_WITHOUT_REGISTRATION, bundle.getEvents().get( 0 ).getProgram() );
    }

    @Test
    public void testTrackerEventWithProgramAndProgramStageIsNotProcessed()
    {
        // Given
        Event event = completeTrackerEvent();
        TrackerBundle bundle = TrackerBundle.builder()
            .events( Collections.singletonList( event ) )
            .preheat( preheat )
            .build();

        // When
        preProcessorToTest.process( bundle );

        // Then
        verify( preheat, never() ).get( Program.class, PROGRAM_WITH_REGISTRATION );
        verify( preheat, never() ).get( ProgramStage.class, PROGRAM_STAGE_WITH_REGISTRATION );
        assertEquals( PROGRAM_WITH_REGISTRATION, bundle.getEvents().get( 0 ).getProgram() );
        assertEquals( PROGRAM_STAGE_WITH_REGISTRATION, bundle.getEvents().get( 0 ).getProgramStage() );
    }

    @Test
    public void testProgramEventIsEnhancedWithProgramStage()
    {
        // Given
        Event event = programEventWithProgram();
        TrackerBundle bundle = TrackerBundle.builder()
            .events( Collections.singletonList( event ) )
            .preheat( preheat )
            .build();

        // When
        preProcessorToTest.process( bundle );

        // Then
        verify( preheat ).put( TrackerIdentifier.UID, programStageWithoutRegistration() );
        assertEquals( PROGRAM_STAGE_WITHOUT_REGISTRATION, bundle.getEvents().get( 0 ).getProgramStage() );
    }

    @Test
    public void testTrackerEventIsNotEnhancedWithProgramStage()
    {
        // Given
        Event event = trackerEventWithProgram();
        TrackerBundle bundle = TrackerBundle.builder()
            .events( Collections.singletonList( event ) )
            .preheat( preheat )
            .build();

        // When
        preProcessorToTest.process( bundle );

        // Then
        assertEquals( PROGRAM_WITH_REGISTRATION, bundle.getEvents().get( 0 ).getProgram() );
        assertNull( bundle.getEvents().get( 0 ).getProgramStage() );
    }

    @Test
    public void testProgramEventWithProgramAndProgramStageIsNotProcessed()
    {
        // Given
        Event event = completeProgramEvent();
        TrackerBundle bundle = TrackerBundle.builder()
            .events( Collections.singletonList( event ) )
            .preheat( preheat )
            .build();

        // When
        preProcessorToTest.process( bundle );

        // Then
        // Then
        verify( preheat, never() ).get( Program.class, PROGRAM_WITHOUT_REGISTRATION );
        verify( preheat, never() ).get( ProgramStage.class, PROGRAM_STAGE_WITHOUT_REGISTRATION );
        assertEquals( PROGRAM_WITHOUT_REGISTRATION, bundle.getEvents().get( 0 ).getProgram() );
        assertEquals( PROGRAM_STAGE_WITHOUT_REGISTRATION, bundle.getEvents().get( 0 ).getProgramStage() );
    }

    private ProgramStage programStageWithRegistration()
    {
        ProgramStage programStage = createProgramStage( 'A', 1, false );
        programStage.setUid( PROGRAM_STAGE_WITH_REGISTRATION );
        programStage.setProgram( programWithRegistration() );
        return programStage;
    }

    private ProgramStage programStageWithoutRegistration()
    {
        ProgramStage programStage = createProgramStage( 'A', 1, false );
        programStage.setUid( PROGRAM_STAGE_WITHOUT_REGISTRATION );
        programStage.setProgram( programWithoutRegistration() );
        return programStage;
    }

    private Program programWithRegistrationWithProgramStages()
    {
        Program program = createProgram( 'A' );
        program.setUid( PROGRAM_WITH_REGISTRATION );
        program.setProgramType( ProgramType.WITH_REGISTRATION );
        program.setProgramStages( Sets.newHashSet( programStageWithRegistration() ) );
        return program;
    }

    private Program programWithoutRegistrationWithProgramStages()
    {
        Program program = createProgram( 'B' );
        program.setUid( PROGRAM_WITHOUT_REGISTRATION );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        program.setProgramStages( Sets.newHashSet( programStageWithoutRegistration() ) );
        return program;
    }

    private Program programWithRegistration()
    {
        Program program = createProgram( 'A' );
        program.setUid( PROGRAM_WITH_REGISTRATION );
        program.setProgramType( ProgramType.WITH_REGISTRATION );
        return program;
    }

    private Program programWithoutRegistration()
    {
        Program program = createProgram( 'B' );
        program.setUid( PROGRAM_WITHOUT_REGISTRATION );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        return program;
    }

    private Event programEventWithProgram()
    {
        Event event = new Event();
        event.setProgram( PROGRAM_WITHOUT_REGISTRATION );
        return event;
    }

    private Event programEventWithProgramStage()
    {
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITHOUT_REGISTRATION );
        return event;
    }

    private Event completeProgramEvent()
    {
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITHOUT_REGISTRATION );
        event.setProgram( PROGRAM_WITHOUT_REGISTRATION );
        return event;
    }

    private Event trackerEventWithProgramStage()
    {
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITH_REGISTRATION );
        return event;
    }

    private Event trackerEventWithProgram()
    {
        Event event = new Event();
        event.setProgram( PROGRAM_WITH_REGISTRATION );
        return event;
    }

    private Event completeTrackerEvent()
    {
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITH_REGISTRATION );
        event.setProgram( PROGRAM_WITH_REGISTRATION );
        return event;
    }
}