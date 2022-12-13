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
package org.hisp.dhis.tracker.validation.validators;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1080;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1081;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1113;
import static org.hisp.dhis.tracker.validation.validators.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.ValidationErrorReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith( MockitoExtension.class )
class EnrollmentPreCheckExistenceValidationHookTest
{
    private final static String SOFT_DELETED_ENROLLMENT_UID = "SoftDeletedEnrollmentId";

    private final static String ENROLLMENT_UID = "EnrollmentId";

    private final static String NOT_PRESENT_ENROLLMENT_UID = "NotPresentEnrollmentId";

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private EnrollmentPreCheckExistenceValidationHook validationHook = new EnrollmentPreCheckExistenceValidationHook();

    private ValidationErrorReporter reporter;

    @BeforeEach
    void setUp()
    {
        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenIsCreateAndEnrollmentIsNotPresent()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE );

        validationHook.validate( reporter, bundle, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenEnrollmentIsNotPresent()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        when( bundle.getStrategy( any( Enrollment.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        validationHook.validate( reporter, bundle, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenIsUpdate()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( ENROLLMENT_UID )
            .build();

        when( bundle.getProgramInstance( ENROLLMENT_UID ) ).thenReturn( getEnrollment() );
        when( bundle.getStrategy( any( Enrollment.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        validationHook.validate( reporter, bundle, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsSoftDeleted()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( SOFT_DELETED_ENROLLMENT_UID )
            .build();

        when( bundle.getProgramInstance( SOFT_DELETED_ENROLLMENT_UID ) ).thenReturn( getSoftDeletedEnrollment() );
        when( bundle.getStrategy( any( Enrollment.class ) ) ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        validationHook.validate( reporter, bundle, enrollment );

        hasTrackerError( reporter, E1113, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsCreateAndEnrollmentIsAlreadyPresent()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( ENROLLMENT_UID )
            .build();

        when( bundle.getProgramInstance( ENROLLMENT_UID ) ).thenReturn( getEnrollment() );
        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.CREATE );

        validationHook.validate( reporter, bundle, enrollment );

        hasTrackerError( reporter, E1080, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenIsUpdateAndEnrollmentIsNotPresent()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( NOT_PRESENT_ENROLLMENT_UID )
            .build();

        when( bundle.getStrategy( enrollment ) ).thenReturn( TrackerImportStrategy.UPDATE );

        validationHook.validate( reporter, bundle, enrollment );

        hasTrackerError( reporter, E1081, ENROLLMENT, enrollment.getUid() );
    }

    private ProgramInstance getSoftDeletedEnrollment()
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( SOFT_DELETED_ENROLLMENT_UID );
        programInstance.setDeleted( true );
        return programInstance;
    }

    private ProgramInstance getEnrollment()
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( ENROLLMENT_UID );
        programInstance.setDeleted( false );
        return programInstance;
    }
}