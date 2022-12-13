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
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1068;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1069;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1070;
import static org.hisp.dhis.tracker.validation.validators.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
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
class EnrollmentPreCheckMetaValidationHookTest
{
    private static final String ORG_UNIT_UID = "OrgUnitUid";

    private static final String TRACKED_ENTITY_UID = "TrackedEntityUid";

    private static final String PROGRAM_UID = "ProgramUid";

    private EnrollmentPreCheckMetaValidationHook validatorToTest;

    @Mock
    private TrackerPreheat preheat;

    private TrackerBundle bundle;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        validatorToTest = new EnrollmentPreCheckMetaValidationHook();

        bundle = TrackerBundle.builder()
            .preheat( preheat )
            .build();

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
    }

    @Test
    void verifyEnrollmentValidationSuccess()
    {
        // given
        Enrollment enrollment = validEnrollment();

        // when
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) ) )
            .thenReturn( new OrganisationUnit() );
        when( bundle.getTrackedEntityInstance( TRACKED_ENTITY_UID ) ).thenReturn( new TrackedEntityInstance() );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) ).thenReturn( new Program() );

        validatorToTest.validate( reporter, bundle, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationSuccessWhenTeiIsInPayload()
    {
        // given
        Enrollment enrollment = validEnrollment();

        // when
        when( preheat.getReference( TRACKED_ENTITY_UID ) )
            .thenReturn( Optional.of( new ReferenceTrackerEntity( "", "" ) ) );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) ) )
            .thenReturn( new OrganisationUnit() );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) ).thenReturn( new Program() );

        validatorToTest.validate( reporter, bundle, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenOrgUnitIsNotPresentInDb()
    {
        // given
        Enrollment enrollment = validEnrollment();

        // when
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) ).thenReturn( new Program() );
        when( bundle.getTrackedEntityInstance( TRACKED_ENTITY_UID ) ).thenReturn( new TrackedEntityInstance() );

        validatorToTest.validate( reporter, bundle, enrollment );

        // then
        hasTrackerError( reporter, E1070, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenTrackedEntityIsNotPresentInDbOrPayload()
    {
        // given
        Enrollment enrollment = validEnrollment();

        // when
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) ) )
            .thenReturn( new OrganisationUnit() );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) ).thenReturn( new Program() );

        validatorToTest.validate( reporter, bundle, enrollment );

        // then
        hasTrackerError( reporter, E1068, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void verifyEnrollmentValidationFailsWhenProgramIsNotPresentInDb()
    {
        // given
        Enrollment enrollment = validEnrollment();

        // when
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) ) )
            .thenReturn( new OrganisationUnit() );
        when( bundle.getTrackedEntityInstance( TRACKED_ENTITY_UID ) ).thenReturn( new TrackedEntityInstance() );

        validatorToTest.validate( reporter, bundle, enrollment );

        // then
        hasTrackerError( reporter, E1069, ENROLLMENT, enrollment.getUid() );
    }

    private Enrollment validEnrollment()
    {
        return Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .trackedEntity( TRACKED_ENTITY_UID )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .build();
    }
}