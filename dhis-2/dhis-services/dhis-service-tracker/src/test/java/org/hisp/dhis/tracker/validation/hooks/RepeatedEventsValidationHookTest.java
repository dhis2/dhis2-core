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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;

/**
 * @author Enrico Colasante
 */
public class RepeatedEventsValidationHookTest
    extends DhisConvenienceTest
{
    private final static String NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION = "NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION";

    private final static String REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION = "REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION";

    private final static String NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION = "NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION";

    private final static String REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION = "REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION";

    private final static String PROGRAM_WITH_REGISTRATION = "PROGRAM_WITH_REGISTRATION";

    private final static String PROGRAM_WITHOUT_REGISTRATION = "PROGRAM_WITHOUT_REGISTRATION";

    private final static String ENROLLMENT_A = "ENROLLMENT_A";

    private final static String ENROLLMENT_B = "ENROLLMENT_B";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private RepeatedEventsValidationHook validatorToTest;

    private TrackerImportValidationContext ctx;

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    @Before
    public void setUp()
    {
        validatorToTest = new RepeatedEventsValidationHook();

        bundle = TrackerBundle.builder().build();
        bundle.setPreheat( preheat );
        ctx = new TrackerImportValidationContext( bundle );

        when( preheat.get( ProgramStage.class, NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION ) )
            .thenReturn( notRepeatebleProgramStageWithRegistration() );
        when( preheat.get( ProgramStage.class, REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION ) )
            .thenReturn( repeatebleProgramStageWithRegistration() );
        when( preheat.get( ProgramStage.class, NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION ) )
            .thenReturn( notRepeatebleProgramStageWithoutRegistration() );
        when( preheat.get( ProgramStage.class, REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION ) )
            .thenReturn( repeatebleProgramStageWithoutRegistration() );
    }

    @Test
    public void testSingleEventIsPassingValidation()
    {
        List<Event> events = Lists.newArrayList( notRepeatableEvent( "A" ) );
        bundle.setEvents( events );

        ValidationErrorReporter errorReporter = validatorToTest.validate( ctx );

        assertTrue( errorReporter.getReportList().isEmpty() );
    }

    @Test
    public void testTwoEventInNotRepeatableProgramStageAreNotPassingValidation()
    {
        List<Event> events = Lists.newArrayList( notRepeatableEvent( "A" ), notRepeatableEvent( "B" ) );
        bundle.setEvents( events );

        ValidationErrorReporter errorReporter = validatorToTest.validate( ctx );

        assertEquals( 2, errorReporter.getReportList().size() );
        assertThat( errorReporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1039 ) );
        assertThat( errorReporter.getReportList().get( 0 ).getErrorMessage(),
            is( "ProgramStage: `" + NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION +
                "`, is not repeatable and an event already exists." ) );
        assertThat( errorReporter.getReportList().get( 1 ).getErrorCode(), is( TrackerErrorCode.E1039 ) );
        assertThat( errorReporter.getReportList().get( 1 ).getErrorMessage(),
            is( "ProgramStage: `" + NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION +
                "`, is not repeatable and an event already exists." ) );
    }

    @Test
    public void testTwoEventInRepeatableProgramStageArePassingValidation()
    {
        List<Event> events = Lists.newArrayList( repeatableEvent( "A" ), repeatableEvent( "B" ) );
        bundle.setEvents( events );

        ValidationErrorReporter errorReporter = validatorToTest.validate( ctx );

        assertTrue( errorReporter.getReportList().isEmpty() );
    }

    @Test
    public void testTwoEventsInNotRepeatableProgramStageWhenOneIsInvalidArePassingValidation()
    {
        Event invalidEvent = notRepeatableEvent( "A" );
        List<Event> events = Lists.newArrayList( invalidEvent, notRepeatableEvent( "B" ) );
        ctx.getRootReporter().getInvalidDTOs().put( TrackerType.EVENT, Lists.newArrayList( invalidEvent.getUid() ) );
        bundle.setEvents( events );

        ValidationErrorReporter errorReporter = validatorToTest.validate( ctx );

        assertTrue( errorReporter.getReportList().isEmpty() );
    }

    @Test
    public void testTwoEventsInNotRepeatableProgramStageButInDifferentEnrollmentsArePassingValidation()
    {
        Event eventEnrollmentA = notRepeatableEvent( "A" );
        Event eventEnrollmentB = notRepeatableEvent( "B" );
        eventEnrollmentB.setEnrollment( ENROLLMENT_B );
        List<Event> events = Lists.newArrayList( eventEnrollmentA, eventEnrollmentB );
        bundle.setEvents( events );

        ValidationErrorReporter errorReporter = validatorToTest.validate( ctx );

        assertTrue( errorReporter.getReportList().isEmpty() );
    }

    @Test
    public void testTwoProgramEventsInSameProgramStageArePassingValidation()
    {
        Event eventProgramA = programEvent( "A" );
        Event eventProgramB = programEvent( "B" );
        List<Event> events = Lists.newArrayList( eventProgramA, eventProgramB );
        bundle.setEvents( events );

        ValidationErrorReporter errorReporter = validatorToTest.validate( ctx );

        assertTrue( errorReporter.getReportList().isEmpty() );
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

    private ProgramStage repeatebleProgramStageWithoutRegistration()
    {
        ProgramStage programStage = createProgramStage( 'A', 1, true );
        programStage.setUid( REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION );
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
        event.setProgramStage( NOT_REPEATABLE_PROGRAM_STAGE_WITHOUT_REGISTRATION );
        return event;
    }

    private Event notRepeatableEvent( String uid )
    {
        Event event = new Event();
        event.setEvent( uid );
        event.setEnrollment( ENROLLMENT_A );
        event.setProgramStage( NOT_REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION );
        return event;
    }

    private Event repeatableEvent( String uid )
    {
        Event event = new Event();
        event.setEvent( uid );
        event.setEnrollment( ENROLLMENT_A );
        event.setProgramStage( REPEATABLE_PROGRAM_STAGE_WITH_REGISTRATION );
        return event;
    }
}