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
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
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

public class EnrollmentInExistingValidationHookTest
{

    private EnrollmentInExistingValidationHook hookToTest;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

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

    private ValidationErrorReporter validationErrorReporter;

    private static final String programUid = "program";

    private static final String trackedEntity = "trackedEntity";

    private static final String enrollmentUid = "enrollment";

    @Before
    public void setUp()
    {
        hookToTest = new EnrollmentInExistingValidationHook();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getIdentifier() ).thenReturn( TrackerIdScheme.UID );
        when( enrollment.getProgram() ).thenReturn( programUid );
        when( enrollment.getTrackedEntity() ).thenReturn( trackedEntity );
        when( enrollment.getStatus() ).thenReturn( EnrollmentStatus.ACTIVE );
        when( enrollment.getEnrollment() ).thenReturn( enrollmentUid );

        when( validationContext.getTrackedEntityInstance( trackedEntity ) ).thenReturn( trackedEntityInstance );
        when( trackedEntityInstance.getUid() ).thenReturn( trackedEntity );

        when( validationContext.getBundle() ).thenReturn( bundle );

        Program program = new Program();
        program.setOnlyEnrollOnce( false );
        program.setUid( programUid );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );
        validationErrorReporter = new ValidationErrorReporter( validationContext );
    }

    @Test
    public void shouldExitCancelledStatus()
    {
        when( enrollment.getStatus() ).thenReturn( EnrollmentStatus.CANCELLED );
        hookToTest.validateEnrollment( validationErrorReporter, enrollment );

        verify( validationContext, times( 0 ) ).getProgram( programUid );
    }

    @Test( expected = NullPointerException.class )
    public void shouldThrowProgramNotFound()
    {
        when( enrollment.getProgram() ).thenReturn( null );
        hookToTest.validateEnrollment( validationErrorReporter, enrollment );
    }

    @Test
    public void shouldExitProgramOnlyEnrollOnce()
    {
        Program program = new Program();
        program.setOnlyEnrollOnce( false );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );
        when( enrollment.getStatus() ).thenReturn( EnrollmentStatus.COMPLETED );
        hookToTest.validateEnrollment( validationErrorReporter, enrollment );

        verify( validationContext.getBundle(), times( 0 ) ).getPreheat();
    }

    @Test( expected = NullPointerException.class )
    public void shouldThrowTrackedEntityNotFound()
    {
        Program program = new Program();
        program.setOnlyEnrollOnce( true );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );

        when( enrollment.getTrackedEntity() ).thenReturn( null );
        hookToTest.validateEnrollment( validationErrorReporter, enrollment );
    }

    @Test
    public void shouldPassValidation()
    {
        Program program = new Program();
        program.setOnlyEnrollOnce( true );

        when( validationContext.getProgram( programUid ) ).thenReturn( program );

        hookToTest.validateEnrollment( validationErrorReporter, enrollment );
    }

    @Test
    public void shouldFailEnrollmentAlreadyInPayload()
    {

        setEnrollmentInPayload( EnrollmentStatus.ACTIVE );

        hookToTest.validateEnrollment( validationErrorReporter, enrollment );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 2, validationErrorReporter.getReportList().size() );

        assertThat( validationErrorReporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1015 ) ) ) );

        assertThat( validationErrorReporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1016 ) ) ) );

    }

    @Test
    public void shouldFailEnrollmentAlreadyInDb()
    {

        setTeiInDb();

        hookToTest.validateEnrollment( validationErrorReporter, enrollment );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 2, validationErrorReporter.getReportList().size() );

        assertThat( validationErrorReporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1015 ) ) ) );

        assertThat( validationErrorReporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1016 ) ) ) );

    }

    @Test
    public void shouldFailWithPriorityInPayload()
    {

        setEnrollmentInPayload( EnrollmentStatus.COMPLETED );
        setTeiInDb();

        hookToTest.validateEnrollment( validationErrorReporter, enrollment );

        assertTrue( validationErrorReporter.hasErrors() );
        assertEquals( 1, validationErrorReporter.getReportList().size() );

        assertThat( validationErrorReporter.getReportList(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1016 ) ) ) );

    }

    private void setTeiInDb()
    {
        when( preheat.getTrackedEntityToProgramInstanceMap() ).thenReturn( new HashMap<String, List<ProgramInstance>>()
        {
            {
                ProgramInstance programInstance = new ProgramInstance();

                Program program = new Program();
                program.setUid( programUid );

                programInstance.setUid( "another_enrollment" );
                programInstance.setStatus( ProgramStatus.ACTIVE );
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
