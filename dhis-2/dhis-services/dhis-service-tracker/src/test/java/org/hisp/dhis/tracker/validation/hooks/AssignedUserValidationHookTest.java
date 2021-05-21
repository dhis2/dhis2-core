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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Enrico Colasante
 */
public class AssignedUserValidationHookTest
{
    private static final String USER_ID = "ABCDEF12345";

    private static final String PROGRAM_STAGE = "ProgramStage";

    private AssignedUserValidationHook hookToTest;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerImportValidationContext validationContext;

    @Before
    public void setUp()
    {
        hookToTest = new AssignedUserValidationHook();

        TrackerBundle bundle = new TrackerBundle().toBuilder().build();
        TrackerPreheat preheat = new TrackerPreheat();

        User user = new User();
        user.setUid( USER_ID );
        preheat.put( TrackerIdentifier.UID, user );

        bundle.setPreheat( preheat );

        when( validationContext.getBundle() ).thenReturn( bundle );

        ProgramStage programStage = new ProgramStage();
        programStage.setEnableUserAssignment( true );
        when( validationContext.getProgramStage( PROGRAM_STAGE ) ).thenReturn( programStage );
    }

    @Test
    public void testAssignedUserIsValid()
    {
        // given
        Event event = new Event();
        event.setAssignedUser( USER_ID );
        event.setProgramStage( PROGRAM_STAGE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void testEventWithNotValidUserUid()
    {
        // given
        Event event = new Event();
        event.setAssignedUser( "not_valid_uid" );
        event.setProgramStage( PROGRAM_STAGE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1118 ) );
    }

    @Test
    public void testEventWithUserNotPresentInPreheat()
    {
        // given
        Event event = new Event();
        event.setAssignedUser( USER_ID );
        event.setProgramStage( PROGRAM_STAGE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        TrackerBundle bundle = new TrackerBundle().toBuilder().build();
        bundle.setPreheat( new TrackerPreheat() );
        when( validationContext.getBundle() ).thenReturn( bundle );

        this.hookToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1118 ) );
    }

    @Test
    public void testEventWithNotEnabledUserAssignment()
    {
        // given
        Event event = new Event();
        event.setAssignedUser( USER_ID );
        event.setProgramStage( PROGRAM_STAGE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        ProgramStage programStage = new ProgramStage();
        programStage.setEnableUserAssignment( false );
        when( validationContext.getProgramStage( PROGRAM_STAGE ) ).thenReturn( programStage );

        this.hookToTest.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
        assertTrue( reporter.hasWarnings() );
        assertThat( reporter.getWarningsReportList().get( 0 ).getWarningCode(), is( TrackerErrorCode.E1120 ) );
    }

    @Test
    public void testEventWithNullEnabledUserAssignment()
    {
        // given
        Event event = new Event();
        event.setAssignedUser( USER_ID );
        event.setProgramStage( PROGRAM_STAGE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        ProgramStage programStage = new ProgramStage();
        programStage.setEnableUserAssignment( null );
        when( validationContext.getProgramStage( PROGRAM_STAGE ) ).thenReturn( programStage );

        this.hookToTest.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
        assertTrue( reporter.hasWarnings() );
        assertThat( reporter.getWarningsReportList().get( 0 ).getWarningCode(), is( TrackerErrorCode.E1120 ) );
    }
}
