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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
public class EventDateValidationHookTest
    extends DhisConvenienceTest
{
    private static final String PROGRAM_STAGE_WITH_REGISTRATION_ID = "ProgramStageWithRegistration";

    private static final String PROGRAM_STAGE_WITHOUT_REGISTRATION_ID = "ProgramStageWithoutRegistration";

    private static final String PROGRAM_WITH_REGISTRATION_ID = "ProgramWithRegistration";

    private static final String PROGRAM_WITHOUT_REGISTRATION_ID = "ProgramWithoutRegistration";

    private EventDateValidationHook hookToTest;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerImportValidationContext validationContext;

    @Before
    public void setUp()
    {
        hookToTest = new EventDateValidationHook();

        User user = createUser( 'A' );

        TrackerBundle bundle = TrackerBundle.builder().user( user ).build();

        when( validationContext.getBundle() ).thenReturn( bundle );

        when( validationContext.getProgramStage( PROGRAM_STAGE_WITH_REGISTRATION_ID ) )
            .thenReturn( getProgramStageWithRegistration() );

        when( validationContext.getProgramStage( PROGRAM_STAGE_WITHOUT_REGISTRATION_ID ) )
            .thenReturn( getProgramStageWithoutRegistration() );
    }

    @Test
    public void testEventIsValid()
    {
        // given
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITHOUT_REGISTRATION_ID );
        event.setOccurredAt( now() );
        event.setStatus( EventStatus.ACTIVE );

        TrackerBundle bundle = TrackerBundle.builder().user( getEditExpiredUser() ).build();

        when( validationContext.getBundle() ).thenReturn( bundle );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void testEventIsNotValidWhenOccurredDateIsNotPresentAndProgramIsWithoutRegistration()
    {
        // given
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITHOUT_REGISTRATION_ID );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1031 ) );

    }

    @Test
    public void testEventIsNotValidWhenOccurredDateIsNotPresentAndEventIsActive()
    {
        // given
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITH_REGISTRATION_ID );
        event.setStatus( EventStatus.ACTIVE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1031 ) );
    }

    @Test
    public void testEventIsNotValidWhenOccurredDateIsNotPresentAndEventIsCompleted()
    {
        // given
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITH_REGISTRATION_ID );
        event.setStatus( EventStatus.COMPLETED );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1031 ) );
    }

    @Test
    public void testEventIsNotValidWhenScheduledDateIsNotPresentAndEventIsSchedule()
    {
        // given
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITH_REGISTRATION_ID );
        event.setOccurredAt( Instant.now() );
        event.setStatus( EventStatus.SCHEDULE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1050 ) );
    }

    @Test
    public void testEventIsNotValidWhenCompletedAtIsNotPresentAndEventIsCompleted()
    {
        // given
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITH_REGISTRATION_ID );
        event.setOccurredAt( now() );
        event.setStatus( EventStatus.COMPLETED );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1042 ) );
    }

    @Test
    public void testEventIsNotValidWhenCompletedAtIsTooSoonAndEventIsCompleted()
    {
        // given
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITH_REGISTRATION_ID );
        event.setOccurredAt( now() );
        event.setCompletedAt( sevenDaysAgo() );
        event.setStatus( EventStatus.COMPLETED );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1043 ) );
    }

    @Test
    public void testEventIsNotValidWhenOccurredAtAndScheduledAtAreNotPresent()
    {
        // given
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITH_REGISTRATION_ID );
        event.setOccurredAt( null );
        event.setScheduledAt( null );
        event.setStatus( EventStatus.SKIPPED );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1046 ) );
    }

    @Test
    public void testEventIsNotValidWhenDateBelongsToExpiredPeriod()
    {
        // given
        Event event = new Event();
        event.setProgramStage( PROGRAM_STAGE_WITH_REGISTRATION_ID );
        event.setOccurredAt( sevenDaysAgo() );
        event.setStatus( EventStatus.ACTIVE );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );

        // when
        this.hookToTest.validateEvent( reporter, event );

        // then
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1047 ) );
    }

    private ProgramStage getProgramStageWithRegistration()
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setUid( PROGRAM_STAGE_WITH_REGISTRATION_ID );
        programStage.setProgram( getProgramWithRegistration() );
        return programStage;
    }

    private ProgramStage getProgramStageWithoutRegistration()
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setUid( PROGRAM_STAGE_WITHOUT_REGISTRATION_ID );
        programStage.setProgram( getProgramWithoutRegistration() );
        return programStage;
    }

    private Program getProgramWithRegistration()
    {
        Program program = createProgram( 'A' );
        program.setUid( PROGRAM_WITH_REGISTRATION_ID );
        program.setProgramType( ProgramType.WITH_REGISTRATION );
        program.setCompleteEventsExpiryDays( 5 );
        program.setExpiryDays( 5 );
        program.setExpiryPeriodType( new DailyPeriodType() );
        return program;
    }

    private Program getProgramWithoutRegistration()
    {
        Program program = createProgram( 'A' );
        program.setUid( PROGRAM_WITHOUT_REGISTRATION_ID );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        return program;
    }

    private User getEditExpiredUser()
    {
        User user = createUser( 'A' );
        UserCredentials userCredentials = createUserCredentials( 'A', user );
        UserAuthorityGroup userAuthorityGroup = createUserAuthorityGroup( 'A' );
        userAuthorityGroup.setAuthorities( Sets.newHashSet( Authorities.F_EDIT_EXPIRED.getAuthority() ) );

        userCredentials.setUserAuthorityGroups( Sets.newHashSet( userAuthorityGroup ) );
        user.setUserCredentials( userCredentials );

        return user;
    }

    private Instant now()
    {
        return Instant.now();
    }

    private Instant sevenDaysAgo()
    {
        return LocalDateTime.now().minus( 7, ChronoUnit.DAYS ).toInstant( ZoneOffset.UTC );
    }
}
