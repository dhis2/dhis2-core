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

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1015;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1016;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EnrollmentInExistingValidationHookTest
{

    private EnrollmentInExistingValidationHook hookToTest;

    @Mock
    private TrackerImportValidationContext validationContext;

    @Mock
    Enrollment enrollment;

    @Mock
    TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    @Mock
    private TrackedEntityInstance trackedEntityInstance;

    private ValidationErrorReporter reporter;

    private static final String programUid = "program";

    private static final String trackedEntity = "trackedEntity";

    private static final String enrollmentUid = "enrollment";

    @BeforeEach
    public void setUp()
    {
        hookToTest = new EnrollmentInExistingValidationHook();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( enrollment.getProgram() ).thenReturn( programUid );
        when( enrollment.getTrackedEntity() ).thenReturn( trackedEntity );
        when( enrollment.getStatus() ).thenReturn( EnrollmentStatus.ACTIVE );
        when( enrollment.getEnrollment() ).thenReturn( enrollmentUid );
        when( enrollment.getUid() ).thenReturn( enrollmentUid );

        when( validationContext.getTrackedEntityInstance( trackedEntity ) ).thenReturn( trackedEntityInstance );
        when( trackedEntityInstance.getUid() ).thenReturn( trackedEntity );

        when( validationContext.getBundle() ).thenReturn( bundle );

        Program program = new Program();
        program.setOnlyEnrollOnce( false );
        program.setUid( programUid );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );
        reporter = new ValidationErrorReporter( validationContext, enrollment );
    }

    @Test
    void shouldExitCancelledStatus()
    {
        when( enrollment.getStatus() ).thenReturn( EnrollmentStatus.CANCELLED );
        hookToTest.validateEnrollment( reporter, enrollment );

        verify( validationContext, times( 0 ) ).getProgram( programUid );
    }

    @Test
    void shouldThrowProgramNotFound()
    {
        when( enrollment.getProgram() ).thenReturn( null );
        assertThrows( NullPointerException.class,
            () -> hookToTest.validateEnrollment( reporter, enrollment ) );
    }

    @Test
    void shouldExitProgramOnlyEnrollOnce()
    {
        Program program = new Program();
        program.setOnlyEnrollOnce( false );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );
        when( enrollment.getStatus() ).thenReturn( EnrollmentStatus.COMPLETED );
        hookToTest.validateEnrollment( reporter, enrollment );

        verify( validationContext.getBundle(), times( 0 ) ).getPreheat();
    }

    @Test
    void shouldThrowTrackedEntityNotFound()
    {
        Program program = new Program();
        program.setOnlyEnrollOnce( true );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );

        when( enrollment.getTrackedEntity() ).thenReturn( null );
        assertThrows( NullPointerException.class,
            () -> hookToTest.validateEnrollment( reporter, enrollment ) );
    }

    @Test
    void shouldPassValidation()
    {
        Program program = new Program();
        program.setOnlyEnrollOnce( true );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );

        hookToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void shouldFailActiveEnrollmentAlreadyInPayload()
    {
        setEnrollmentInPayload( EnrollmentStatus.ACTIVE );

        hookToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );

        hasTrackerError( reporter, E1015, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void shouldFailNotActiveEnrollmentAlreadyInPayloadAndEnrollOnce()
    {
        Program program = new Program();
        program.setUid( programUid );
        program.setOnlyEnrollOnce( true );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );
        setEnrollmentInPayload( EnrollmentStatus.COMPLETED );

        hookToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );

        hasTrackerError( reporter, E1016, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void shouldPassNotActiveEnrollmentAlreadyInPayloadAndNotEnrollOnce()
    {
        setEnrollmentInPayload( EnrollmentStatus.COMPLETED );

        hookToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void shouldFailActiveEnrollmentAlreadyInDb()
    {
        setTeiInDb();

        hookToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );

        hasTrackerError( reporter, E1015, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void shouldFailNotActiveEnrollmentAlreadyInDbAndEnrollOnce()
    {
        Program program = new Program();
        program.setUid( programUid );
        program.setOnlyEnrollOnce( true );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );
        setTeiInDb( ProgramStatus.COMPLETED );

        hookToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );
        hasTrackerError( reporter, E1016, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void shouldPassNotActiveEnrollmentAlreadyInDbAndNotEnrollOnce()
    {
        setTeiInDb( ProgramStatus.COMPLETED );

        hookToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void shouldFailAnotherEnrollmentAndEnrollOnce()
    {
        Program program = new Program();
        program.setUid( programUid );
        program.setOnlyEnrollOnce( true );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );
        setEnrollmentInPayload( EnrollmentStatus.COMPLETED );
        setTeiInDb();

        hookToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertEquals( 1, reporter.getReportList().size() );
        hasTrackerError( reporter, E1016, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void shouldPassWhenAnotherEnrollmentAndNotEnrollOnce()
    {
        Program program = new Program();
        program.setUid( programUid );
        program.setOnlyEnrollOnce( false );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );
        setEnrollmentInPayload( EnrollmentStatus.COMPLETED );
        setTeiInDb();

        hookToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );

    }

    private void setTeiInDb()
    {
        setTeiInDb( ProgramStatus.ACTIVE );
    }

    private void setTeiInDb( ProgramStatus programStatus )
    {
        when( preheat.getTrackedEntityToProgramInstanceMap() ).thenReturn( new HashMap<>()
        {
            {
                ProgramInstance programInstance = new ProgramInstance();

                Program program = new Program();
                program.setUid( programUid );

                programInstance.setUid( "another_enrollment" );
                programInstance.setStatus( programStatus );
                programInstance.setProgram( program );

                put( trackedEntity, Collections.singletonList( programInstance ) );
            }
        } );
    }

    private void setEnrollmentInPayload( EnrollmentStatus enrollmentStatus )
    {
        Enrollment enrollmentInBundle = new Enrollment();
        enrollmentInBundle.setProgram( programUid );
        enrollmentInBundle.setTrackedEntity( trackedEntity );
        enrollmentInBundle.setEnrollment( "another_enrollment" );
        enrollmentInBundle.setStatus( enrollmentStatus );

        when( bundle.getEnrollments() ).thenReturn( Collections.singletonList( enrollmentInBundle ) );
    }
}
