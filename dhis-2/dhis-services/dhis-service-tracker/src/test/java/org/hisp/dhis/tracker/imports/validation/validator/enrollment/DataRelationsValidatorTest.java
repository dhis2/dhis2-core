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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
@ExtendWith( MockitoExtension.class )
class DataRelationsValidatorTest extends DhisConvenienceTest
{

    private static final String PROGRAM_UID = "PROGRAM_UID";

    private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

    private static final String TEI_TYPE_ID = "TEI_TYPE_ID";

    private static final String ANOTHER_TEI_TYPE_ID = "ANOTHER_TEI_TYPE_ID";

    private static final String TEI_ID = "TEI_ID";

    private DataRelationsValidator validator;

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private Reporter reporter;

    @BeforeEach
    void setUp()
    {
        validator = new DataRelationsValidator();

        when( bundle.getPreheat() ).thenReturn( preheat );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new Reporter( idSchemes );
    }

    @Test
    void verifyValidationSuccessForEnrollment()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) )
            .thenReturn( orgUnit );
        TrackedEntityType teiType = trackedEntityType( TEI_TYPE_ID );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( programWithRegistration( PROGRAM_UID, orgUnit, teiType ) );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( preheat.getTrackedEntity( TEI_ID ) )
            .thenReturn( trackedEntityInstance( TEI_TYPE_ID, teiType, orgUnit ) );

        Enrollment enrollment = Enrollment.builder()
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .enrollment( CodeGenerator.generateUid() )
            .trackedEntity( TEI_ID )
            .build();

        validator.validate( reporter, bundle, enrollment );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void verifyValidationFailsWhenEnrollmentIsNotARegistration()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) )
            .thenReturn( orgUnit );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( programWithoutRegistration( PROGRAM_UID, orgUnit ) );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );

        Enrollment enrollment = Enrollment.builder()
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .build();

        validator.validate( reporter, bundle, enrollment );

        assertHasError( reporter, enrollment, ValidationCode.E1014 );
    }

    @Test
    void verifyValidationFailsWhenEnrollmentAndProgramOrganisationUnitDontMatch()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) )
            .thenReturn( orgUnit );
        OrganisationUnit anotherOrgUnit = organisationUnit( CodeGenerator.generateUid() );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( programWithRegistration( PROGRAM_UID, anotherOrgUnit ) );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn(
                Collections.singletonMap( PROGRAM_UID, Collections.singletonList( anotherOrgUnit.getUid() ) ) );

        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .build();

        validator.validate( reporter, bundle, enrollment );

        assertHasError( reporter, enrollment, ValidationCode.E1041 );
    }

    @Test
    void verifyValidationFailsWhenEnrollmentAndProgramTeiTypeDontMatch()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) )
            .thenReturn( orgUnit );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( programWithRegistration( PROGRAM_UID, orgUnit, trackedEntityType( TEI_TYPE_ID ) ) );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        TrackedEntityType anotherTrackedEntityType = trackedEntityType( TEI_ID, 'B' );
        when( preheat.getTrackedEntity( TEI_ID ) )
            .thenReturn( trackedEntityInstance( TEI_ID, anotherTrackedEntityType, orgUnit ) );

        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .build();

        validator.validate( reporter, bundle, enrollment );

        assertHasError( reporter, enrollment, ValidationCode.E1022 );
    }

    @Test
    void verifyValidationFailsWhenEnrollmentAndProgramTeiTypeDontMatchAndTEIIsInPayload()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) )
            .thenReturn( orgUnit );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( programWithRegistration( PROGRAM_UID, orgUnit, trackedEntityType( TEI_TYPE_ID ) ) );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( preheat.getTrackedEntity( TEI_ID ) ).thenReturn( null );

        org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity = org.hisp.dhis.tracker.imports.domain.TrackedEntity
            .builder()
            .trackedEntity( TEI_ID )
            .trackedEntityType( MetadataIdentifier.ofUid( ANOTHER_TEI_TYPE_ID ) )
            .build();
        bundle.setTrackedEntities( Collections.singletonList( trackedEntity ) );

        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .build();

        validator.validate( reporter, bundle, enrollment );

        assertHasError( reporter, enrollment, ValidationCode.E1022 );
    }

    private OrganisationUnit organisationUnit( String uid )
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnit.setUid( uid );
        return organisationUnit;
    }

    private Program programWithRegistration( String uid, OrganisationUnit orgUnit )
    {
        return program( uid, ProgramType.WITH_REGISTRATION, 'A', orgUnit, trackedEntityType( TEI_TYPE_ID ) );
    }

    // Note : parameters that always have the same value are kept to
    // make connections between different entities clear when looking at the
    // test. Without having to navigate to the
    // helpers.
    private Program programWithRegistration( String uid, OrganisationUnit orgUnit, TrackedEntityType teiType )
    {
        return program( uid, ProgramType.WITH_REGISTRATION, 'A', orgUnit, teiType );
    }

    private Program programWithoutRegistration( String uid, OrganisationUnit orgUnit )
    {
        return program( uid, ProgramType.WITHOUT_REGISTRATION, 'B', orgUnit, trackedEntityType( TEI_TYPE_ID ) );
    }

    private Program program( String uid, ProgramType type, char uniqueCharacter, OrganisationUnit orgUnit,
        TrackedEntityType teiType )
    {
        Program program = createProgram( uniqueCharacter );
        program.setUid( uid );
        program.setProgramType( type );
        program.setOrganisationUnits( Sets.newHashSet( orgUnit ) );
        program.setTrackedEntityType( teiType );
        return program;
    }

    private TrackedEntityType trackedEntityType( String uid )
    {
        return trackedEntityType( uid, 'A' );
    }

    private TrackedEntityType trackedEntityType( String uid, char uniqueChar )
    {
        TrackedEntityType trackedEntityType = createTrackedEntityType( uniqueChar );
        trackedEntityType.setUid( uid );
        return trackedEntityType;
    }

    private TrackedEntity trackedEntityInstance( String uid, TrackedEntityType type, OrganisationUnit orgUnit )
    {
        TrackedEntity tei = createTrackedEntityInstance( orgUnit );
        tei.setUid( uid );
        tei.setTrackedEntityType( type );
        return tei;
    }
}