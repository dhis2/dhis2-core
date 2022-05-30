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

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1020;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1021;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1023;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1025;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class EnrollmentDateValidationHookTest
{

    private EnrollmentDateValidationHook hookToTest;

    @Mock
    private TrackerPreheat preheat;

    private TrackerBundle bundle;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        hookToTest = new EnrollmentDateValidationHook();

        bundle = TrackerBundle.builder()
            .preheat( preheat )
            .build();

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
    }

    @Test
    void testMandatoryDatesMustBePresent()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .occurredAt( Instant.now() )
            .build();

        when( preheat.getProgram( enrollment.getProgram() ) ).thenReturn( new Program() );

        this.hookToTest.validateEnrollment( reporter, bundle, enrollment );

        hasTrackerError( reporter, E1025, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void testDatesMustNotBeInTheFuture()
    {
        final Instant dateInTheFuture = Instant.now().plus( Duration.ofDays( 2 ) );
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .occurredAt( dateInTheFuture )
            .enrolledAt( dateInTheFuture )
            .build();

        when( preheat.getProgram( enrollment.getProgram() ) ).thenReturn( new Program() );

        this.hookToTest.validateEnrollment( reporter, bundle, enrollment );

        hasTrackerError( reporter, E1020, ENROLLMENT, enrollment.getUid() );
        hasTrackerError( reporter, E1021, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void testDatesShouldBeAllowedOnSameDayIfFutureDatesAreNotAllowed()
    {
        final Instant today = Instant.now().plus( Duration.ofMinutes( 1 ) );
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .occurredAt( today )
            .enrolledAt( today )
            .build();

        when( preheat.getProgram( enrollment.getProgram() ) ).thenReturn( new Program() );

        this.hookToTest.validateEnrollment( reporter, bundle, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void testDatesCanBeInTheFuture()
    {
        final Instant dateInTheFuture = Instant.now().plus( Duration.ofDays( 2 ) );
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .occurredAt( dateInTheFuture )
            .enrolledAt( dateInTheFuture )
            .build();

        Program program = new Program();
        program.setSelectEnrollmentDatesInFuture( true );
        program.setSelectIncidentDatesInFuture( true );
        when( preheat.getProgram( enrollment.getProgram() ) ).thenReturn( program );

        this.hookToTest.validateEnrollment( reporter, bundle, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void testFailOnMissingOccurredAtDate()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( CodeGenerator.generateUid() ) )
            .enrolledAt( Instant.now() )
            .build();

        Program program = new Program();
        program.setDisplayIncidentDate( true );
        when( preheat.getProgram( enrollment.getProgram() ) ).thenReturn( program );

        this.hookToTest.validateEnrollment( reporter, bundle, enrollment );

        hasTrackerError( reporter, E1023, ENROLLMENT, enrollment.getUid() );
    }

}
