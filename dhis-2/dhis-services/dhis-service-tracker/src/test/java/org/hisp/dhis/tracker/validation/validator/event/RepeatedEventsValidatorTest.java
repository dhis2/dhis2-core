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
package org.hisp.dhis.tracker.validation.validator.event;

import static org.hisp.dhis.tracker.validation.ValidationCode.E1039;
import static org.hisp.dhis.tracker.validation.ValidationCode.E9999;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Error;
import org.hisp.dhis.tracker.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;

/**
 * @author Enrico Colasante
 */
@ExtendWith( MockitoExtension.class )
class RepeatedEventsValidatorTest extends DhisConvenienceTest
{

    private final static String NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION = "NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION";

    private final static String REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION = "REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION";

    private final static String NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION = "NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION";

    private final static String ENROLLMENT_A = "ENROLLMENT_A";

    private final static String ENROLLMENT_B = "ENROLLMENT_B";

    private RepeatedEventsValidator validator;

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private Reporter reporter;

    @BeforeEach
    public void setUp()
    {
        validator = new RepeatedEventsValidator();

        bundle = TrackerBundle.builder().build();
        bundle.setPreheat( preheat );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new Reporter( idSchemes );
    }

    @Test
    void testSingleEventIsPassingValidation()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION ) ) )
            .thenReturn( notRepeatebleProgramStageWithRegistration() );
        List<Event> events = Lists.newArrayList( notRepeatableEvent( "A" ) );
        bundle.setEvents( events );
        events.forEach( e -> bundle.setStrategy( e, TrackerImportStrategy.CREATE_AND_UPDATE ) );

        validator.validate( reporter, bundle, bundle.getEvents() );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void testOneEventInNotRepeatableProgramStageAndOneAlreadyOnDBAreNotPassingValidation()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION ) ) )
            .thenReturn( notRepeatebleProgramStageWithRegistration() );
        // given
        Event event = notRepeatableEvent( "A" );
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( event.getEnrollment() );

        // when
        bundle.setStrategy( event, TrackerImportStrategy.CREATE );

        when( preheat.getEnrollment( event.getEnrollment() ) ).thenReturn( programInstance );
        when( preheat.hasProgramStageWithEvents( event.getProgramStage(), event.getEnrollment() ) ).thenReturn( true );
        bundle.setEvents( Lists.newArrayList( event ) );

        validator.validate( reporter, bundle, bundle.getEvents() );

        // then
        assertHasError( reporter, event, E1039, "ProgramStage: `" + NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION +
            "`, is not repeatable and an event already exists." );
    }

    @Test
    void testTwoEventInNotRepeatableProgramStageAreNotPassingValidation()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION ) ) )
            .thenReturn( notRepeatebleProgramStageWithRegistration() );
        List<Event> events = Lists.newArrayList( notRepeatableEvent( "A" ), notRepeatableEvent( "B" ) );
        bundle.setEvents( events );
        events.forEach( e -> bundle.setStrategy( e, TrackerImportStrategy.CREATE_AND_UPDATE ) );

        validator.validate( reporter, bundle, bundle.getEvents() );

        assertAll(
            () -> assertHasError( reporter, events.get( 0 ), E1039,
                "ProgramStage: `" + NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION
                    + "`, is not repeatable and an event already exists." ),
            () -> assertHasError( reporter, events.get( 1 ), E1039,
                "ProgramStage: `" + NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION +
                    "`, is not repeatable and an event already exists." ) );
    }

    @Test
    void testTwoEventInRepeatableProgramStageArePassingValidation()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION ) ) )
            .thenReturn( repeatebleProgramStageWithRegistration() );
        List<Event> events = Lists.newArrayList( repeatableEvent( "A" ), repeatableEvent( "B" ) );
        bundle.setEvents( events );
        events.forEach( e -> bundle.setStrategy( e, TrackerImportStrategy.CREATE_AND_UPDATE ) );

        validator.validate( reporter, bundle, bundle.getEvents() );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void testTwoEventsInNotRepeatableProgramStageWhenOneIsInvalidArePassingValidation()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION ) ) )
            .thenReturn( notRepeatebleProgramStageWithRegistration() );
        Event invalidEvent = notRepeatableEvent( "A" );
        List<Event> events = Lists.newArrayList( invalidEvent, notRepeatableEvent( "B" ) );
        bundle.setEvents( events );
        events.forEach( e -> bundle.setStrategy( e, TrackerImportStrategy.CREATE_AND_UPDATE ) );
        reporter
            .addError( new Error( "", E9999, invalidEvent.getTrackerType(), invalidEvent.getUid() ) );

        validator.validate( reporter, bundle, bundle.getEvents() );

        assertFalse( reporter.hasErrorReport( e -> E1039 == e.getErrorCode() ) );
    }

    @Test
    void testTwoEventsInNotRepeatableProgramStageButInDifferentEnrollmentsArePassingValidation()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION ) ) )
            .thenReturn( notRepeatebleProgramStageWithRegistration() );
        Event eventEnrollmentA = notRepeatableEvent( "A" );
        Event eventEnrollmentB = notRepeatableEvent( "B" );
        eventEnrollmentB.setEnrollment( ENROLLMENT_B );
        List<Event> events = Lists.newArrayList( eventEnrollmentA, eventEnrollmentB );
        bundle.setEvents( events );
        events.forEach( e -> bundle.setStrategy( e, TrackerImportStrategy.CREATE_AND_UPDATE ) );

        validator.validate( reporter, bundle, bundle.getEvents() );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void testTwoProgramEventsInSameProgramStageArePassingValidation()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION ) ) )
            .thenReturn( notRepeatebleProgramStageWithoutRegistration() );
        Event eventProgramA = programEvent( "A" );
        Event eventProgramB = programEvent( "B" );
        List<Event> events = Lists.newArrayList( eventProgramA, eventProgramB );
        bundle.setEvents( events );
        events.forEach( e -> bundle.setStrategy( e, TrackerImportStrategy.CREATE_AND_UPDATE ) );

        validator.validate( reporter, bundle, bundle.getEvents() );

        assertIsEmpty( reporter.getErrors() );
    }

    private ProgramStage notRepeatebleProgramStageWithRegistration()
    {
        ProgramStage programStage = createProgramStage( 'A', 1, false );
        programStage.setUid( NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION );
        programStage.setProgram( programWithRegistration() );
        return programStage;
    }

    private ProgramStage repeatebleProgramStageWithRegistration()
    {
        ProgramStage programStage = createProgramStage( 'A', 1, true );
        programStage.setUid( REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION );
        programStage.setProgram( programWithRegistration() );
        return programStage;
    }

    private ProgramStage notRepeatebleProgramStageWithoutRegistration()
    {
        ProgramStage programStage = createProgramStage( 'A', 1, false );
        programStage.setUid( NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION );
        programStage.setProgram( programWithoutRegistration() );
        return programStage;
    }

    private Program programWithRegistration()
    {
        Program program = createProgram( 'A' );
        program.setProgramType( ProgramType.WITH_REGISTRATION );
        return program;
    }

    private Program programWithoutRegistration()
    {
        Program program = createProgram( 'B' );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        return program;
    }

    private Event programEvent( String uid )
    {
        Event event = new Event();
        event.setEvent( uid );
        event.setProgramStage( MetadataIdentifier.ofUid( NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION ) );
        return event;
    }

    private Event notRepeatableEvent( String uid )
    {
        Event event = new Event();
        event.setEvent( uid );
        event.setEnrollment( ENROLLMENT_A );
        event.setProgramStage( MetadataIdentifier.ofUid( NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION ) );
        return event;
    }

    private Event repeatableEvent( String uid )
    {
        Event event = new Event();
        event.setEvent( uid );
        event.setEnrollment( ENROLLMENT_A );
        event.setProgramStage( MetadataIdentifier.ofUid( REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION ) );
        return event;
    }
}