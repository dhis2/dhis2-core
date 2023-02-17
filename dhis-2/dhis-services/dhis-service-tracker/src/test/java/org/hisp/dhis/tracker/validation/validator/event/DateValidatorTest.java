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

import static org.hisp.dhis.tracker.validation.ValidationCode.E1031;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1042;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1043;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1046;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1047;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1050;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class DateValidatorTest extends DhisConvenienceTest
{

    private static final String PROGRAM_WITH_REGISTRATION_ID = "ProgramWithRegistration";

    private static final String PROGRAM_WITHOUT_REGISTRATION_ID = "ProgramWithoutRegistration";

    private DateValidator validator;

    @Mock
    private TrackerPreheat preheat;

    private TrackerBundle bundle;

    private Reporter reporter;

    @BeforeEach
    public void setUp()
    {
        validator = new DateValidator();

        User user = makeUser( "A" );

        bundle = TrackerBundle.builder()
            .user( user )
            .preheat( preheat )
            .build();

        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_WITH_REGISTRATION_ID ) ) )
            .thenReturn( getProgramWithRegistration() );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_WITHOUT_REGISTRATION_ID ) ) )
            .thenReturn( getProgramWithoutRegistration() );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new Reporter( idSchemes );
    }

    @Test
    void testEventIsValid()
    {
        // given
        Event event = new Event();
        event.setProgram( MetadataIdentifier.ofUid( PROGRAM_WITHOUT_REGISTRATION_ID ) );
        event.setOccurredAt( now() );
        event.setStatus( EventStatus.ACTIVE );

        TrackerBundle bundle = TrackerBundle.builder()
            .user( getEditExpiredUser() )
            .preheat( preheat )
            .build();

        // when
        validator.validate( reporter, bundle, event );

        // then
        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void testEventIsNotValidWhenOccurredDateIsNotPresentAndProgramIsWithoutRegistration()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgram( MetadataIdentifier.ofUid( PROGRAM_WITHOUT_REGISTRATION_ID ) );

        // when
        validator.validate( reporter, bundle, event );

        // then
        assertHasError( reporter, event, E1031 );
    }

    @Test
    void testEventIsNotValidWhenOccurredDateIsNotPresentAndEventIsActive()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgram( MetadataIdentifier.ofUid( PROGRAM_WITH_REGISTRATION_ID ) );
        event.setStatus( EventStatus.ACTIVE );

        // when
        validator.validate( reporter, bundle, event );

        // then
        assertHasError( reporter, event, E1031 );
    }

    @Test
    void testEventIsNotValidWhenOccurredDateIsNotPresentAndEventIsCompleted()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgram( MetadataIdentifier.ofUid( PROGRAM_WITH_REGISTRATION_ID ) );
        event.setStatus( EventStatus.COMPLETED );

        // when
        validator.validate( reporter, bundle, event );

        // then
        assertHasError( reporter, event, E1031 );
    }

    @Test
    void testEventIsNotValidWhenScheduledDateIsNotPresentAndEventIsSchedule()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgram( MetadataIdentifier.ofUid( PROGRAM_WITH_REGISTRATION_ID ) );
        event.setOccurredAt( Instant.now() );
        event.setStatus( EventStatus.SCHEDULE );

        // when
        validator.validate( reporter, bundle, event );

        // then
        assertHasError( reporter, event, E1050 );
    }

    @Test
    void testEventIsNotValidWhenCompletedAtIsNotPresentAndEventIsCompleted()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgram( MetadataIdentifier.ofUid( PROGRAM_WITH_REGISTRATION_ID ) );
        event.setOccurredAt( now() );
        event.setStatus( EventStatus.COMPLETED );

        // when
        validator.validate( reporter, bundle, event );

        // then
        assertHasError( reporter, event, E1042 );
    }

    @Test
    void testEventIsNotValidWhenCompletedAtIsTooSoonAndEventIsCompleted()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgram( MetadataIdentifier.ofUid( PROGRAM_WITH_REGISTRATION_ID ) );
        event.setOccurredAt( now() );
        event.setCompletedAt( sevenDaysAgo() );
        event.setStatus( EventStatus.COMPLETED );

        // when
        validator.validate( reporter, bundle, event );

        // then
        assertHasError( reporter, event, E1043 );
    }

    @Test
    void testEventIsNotValidWhenOccurredAtAndScheduledAtAreNotPresent()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgram( MetadataIdentifier.ofUid( PROGRAM_WITH_REGISTRATION_ID ) );
        event.setOccurredAt( null );
        event.setScheduledAt( null );
        event.setStatus( EventStatus.SKIPPED );

        // when
        validator.validate( reporter, bundle, event );

        // then
        assertHasError( reporter, event, E1046 );
    }

    @Test
    void testEventIsNotValidWhenDateBelongsToExpiredPeriod()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgram( MetadataIdentifier.ofUid( PROGRAM_WITH_REGISTRATION_ID ) );
        event.setOccurredAt( sevenDaysAgo() );
        event.setStatus( EventStatus.ACTIVE );

        // when
        validator.validate( reporter, bundle, event );

        // then
        assertHasError( reporter, event, E1047 );
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
        User user = makeUser( "A" );
        UserRole userRole = createUserRole( 'A' );
        userRole.setAuthorities( Sets.newHashSet( Authorities.F_EDIT_EXPIRED.getAuthority() ) );

        user.setUserRoles( Sets.newHashSet( userRole ) );

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
