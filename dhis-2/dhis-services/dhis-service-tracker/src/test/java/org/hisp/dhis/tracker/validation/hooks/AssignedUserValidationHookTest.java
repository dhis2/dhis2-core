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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1118;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1120;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.User;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
@ExtendWith( MockitoExtension.class )
class AssignedUserValidationHookTest extends DhisConvenienceTest
{

    private static final String USER_NAME = "Username";

    private static final String NOT_VALID_USERNAME = "not_valid_username";

    private static final String PROGRAM_STAGE = "ProgramStage";

    private AssignedUserValidationHook hookToTest;

    private TrackerBundle bundle;

    private ValidationErrorReporter reporter;

    private ProgramStage programStage;

    private static final User VALID_USER = User.builder().username( USER_NAME ).build();

    private static final User INVALID_USER = User.builder().username( NOT_VALID_USERNAME ).build();

    @BeforeEach
    public void setUp()
    {
        hookToTest = new AssignedUserValidationHook();

        bundle = TrackerBundle.builder().build();
        TrackerPreheat preheat = new TrackerPreheat();
        org.hisp.dhis.user.User user = makeUser( "A" );
        user.setUsername( USER_NAME );
        preheat.addUsers( Sets.newHashSet( user ) );
        bundle.setPreheat( preheat );

        programStage = new ProgramStage();
        programStage.setUid( PROGRAM_STAGE );
        programStage.setEnableUserAssignment( true );
        preheat.put( programStage );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
    }

    @Test
    void testAssignedUserIsValid()
    {
        // given
        Event event = new Event();
        event.setAssignedUser( VALID_USER );
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );

        // when
        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void testAssignedUserIsNull()
    {
        // given
        Event event = new Event();
        event.setAssignedUser( null );
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );

        // when
        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void testAssignedUserIsEmpty()
    {
        // given
        Event event = new Event();
        event.setAssignedUser( User.builder().build() );
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );

        // when
        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void testEventWithNotValidUsername()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setAssignedUser( INVALID_USER );
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );

        // when
        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        hasTrackerError( reporter, E1118, TrackerType.EVENT, event.getUid() );
    }

    @Test
    void testEventWithUserNotPresentInPreheat()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setAssignedUser( VALID_USER );
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );

        // when
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.put( programStage );
        bundle = TrackerBundle.builder().preheat( preheat ).build();

        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        hasTrackerError( reporter, E1118, TrackerType.EVENT, event.getUid() );
    }

    @Test
    void testEventWithNotEnabledUserAssignment()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setAssignedUser( VALID_USER );
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );

        // when
        programStage.setEnableUserAssignment( false );

        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        assertFalse( reporter.hasErrors() );
        assertTrue( reporter.hasWarnings() );
        assertTrue( reporter.hasWarningReport( r -> E1120.equals( r.getWarningCode() ) &&
            TrackerType.EVENT.equals( r.getTrackerType() ) &&
            event.getUid().equals( r.getUid() ) ) );
    }

    @Test
    void testEventWithNullEnabledUserAssignment()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setAssignedUser( VALID_USER );
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );

        // when
        programStage.setEnableUserAssignment( null );

        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        assertFalse( reporter.hasErrors() );
        assertTrue( reporter.hasWarnings() );
        assertTrue( reporter.hasWarningReport( r -> E1120.equals( r.getWarningCode() ) &&
            TrackerType.EVENT.equals( r.getTrackerType() ) &&
            event.getUid().equals( r.getUid() ) ) );
    }
}
