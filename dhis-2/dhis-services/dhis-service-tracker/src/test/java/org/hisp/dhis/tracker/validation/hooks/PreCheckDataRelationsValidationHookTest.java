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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
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
class PreCheckDataRelationsValidationHookTest extends DhisConvenienceTest
{

    private static final String PROGRAM_UID = "PROGRAM_UID";

    private static final String PROGRAM_STAGE_ID = "PROGRAM_STAGE_ID";

    private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

    private static final String TEI_TYPE_ID = "TEI_TYPE_ID";

    private static final String ANOTHER_TEI_TYPE_ID = "ANOTHER_TEI_TYPE_ID";

    private static final String TEI_ID = "TEI_ID";

    private static final String ENROLLMENT_ID = "ENROLLMENT_ID";

    private PreCheckDataRelationsValidationHook hook;

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    @BeforeEach
    void setUp()
    {
        hook = new PreCheckDataRelationsValidationHook();

        bundle = TrackerBundle.builder().preheat( preheat ).build();
        reporter = new ValidationErrorReporter( bundle );
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
        when( bundle.getTrackedEntityInstance( TEI_ID ) )
            .thenReturn( trackedEntityInstance( TEI_TYPE_ID, teiType, orgUnit ) );

        Enrollment enrollment = Enrollment.builder()
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .enrollment( CodeGenerator.generateUid() )
            .trackedEntity( TEI_ID )
            .build();

        hook.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
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

        hook.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1014 ) );
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

        hook.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1041 ) );
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
        when( bundle.getTrackedEntityInstance( TEI_ID ) )
            .thenReturn( trackedEntityInstance( TEI_ID, anotherTrackedEntityType, orgUnit ) );

        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .build();

        hook.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1022 ) );
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
        when( bundle.getTrackedEntityInstance( TEI_ID ) ).thenReturn( null );

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .trackedEntityType( ANOTHER_TEI_TYPE_ID )
            .build();
        bundle.setTrackedEntities( Collections.singletonList( trackedEntity ) );

        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .trackedEntity( TEI_ID )
            .build();

        hook.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1022 ) );
    }

    @Test
    void eventValidationSucceedsWhenAOCAndCOsAreNotSetAndProgramHasDefaultCC()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( orgUnit );
        Program program = programWithRegistration( PROGRAM_UID, orgUnit );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( program );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) ) )
            .thenReturn( programStage( PROGRAM_STAGE_ID, program ) );
        when( bundle.getProgramInstance( ENROLLMENT_ID ) )
            .thenReturn( programInstance( ENROLLMENT_ID, program ) );

        CategoryCombo defaultCC = defaultCategoryCombo();
        program.setCategoryCombo( defaultCC );
        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCC );
        when( preheat.getDefault( CategoryOptionCombo.class ) ).thenReturn( defaultAOC );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( program ) )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .enrollment( ENROLLMENT_ID )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void eventValidationFailsWhenEventAndProgramStageProgramDontMatch()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( orgUnit );
        Program program = programWithRegistration( PROGRAM_UID, orgUnit );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( program );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) ) )
            .thenReturn(
                programStage( PROGRAM_STAGE_ID, programWithRegistration( CodeGenerator.generateUid(), orgUnit ) ) );

        CategoryCombo defaultCC = defaultCategoryCombo();
        program.setCategoryCombo( defaultCC );
        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCC );
        when( preheat.getDefault( CategoryOptionCombo.class ) ).thenReturn( defaultAOC );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( program ) )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .build();

        hook.validateEvent( reporter, event );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1089 ) );
    }

    @Test
    void eventValidationFailsWhenProgramIsRegistrationAndEnrollmentIsMissing()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( orgUnit );
        Program program = programWithRegistration( PROGRAM_UID, orgUnit );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( program );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) ) )
            .thenReturn( programStage( PROGRAM_STAGE_ID, program ) );

        CategoryCombo defaultCC = defaultCategoryCombo();
        program.setCategoryCombo( defaultCC );
        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCC );
        when( preheat.getDefault( CategoryOptionCombo.class ) ).thenReturn( defaultAOC );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( program ) )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1033 ) );
    }

    @Test
    void eventValidationFailsWhenEventAndEnrollmentProgramDontMatch()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( orgUnit );
        Program program = programWithRegistration( PROGRAM_UID, orgUnit );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( program );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) ) )
            .thenReturn( programStage( PROGRAM_STAGE_ID, program ) );
        when( bundle.getProgramInstance( ENROLLMENT_ID ) )
            .thenReturn(
                programInstance( ENROLLMENT_ID, programWithRegistration( CodeGenerator.generateUid(), orgUnit ) ) );

        CategoryCombo defaultCC = defaultCategoryCombo();
        program.setCategoryCombo( defaultCC );
        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCC );
        when( preheat.getDefault( CategoryOptionCombo.class ) ).thenReturn( defaultAOC );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( program ) )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .enrollment( ENROLLMENT_ID )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1079 ) );
    }

    @Test
    void eventValidationFailsWhenEventAndProgramOrganisationUnitDontMatch()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) ).thenReturn( orgUnit );
        OrganisationUnit anotherOrgUnit = organisationUnit( CodeGenerator.generateUid() );
        Program program = programWithRegistration( PROGRAM_UID, anotherOrgUnit );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( program );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn(
                Collections.singletonMap( PROGRAM_UID, Collections.singletonList( anotherOrgUnit.getUid() ) ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) ) )
            .thenReturn( programStage( PROGRAM_STAGE_ID, program ) );
        when( bundle.getProgramInstance( ENROLLMENT_ID ) )
            .thenReturn( programInstance( ENROLLMENT_ID, program ) );

        CategoryCombo defaultCC = defaultCategoryCombo();
        program.setCategoryCombo( defaultCC );
        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCC );
        when( preheat.getDefault( CategoryOptionCombo.class ) ).thenReturn( defaultAOC );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( program ) )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .enrollment( ENROLLMENT_ID )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1029 ) );
    }

    @Test
    void eventValidationFailsWhenNoAOCAndNoCOsAreSetAndProgramHasNonDefaultCC()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );
        program.setCategoryCombo( categoryCombo() );

        Event event = eventBuilder()
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1055 ) );
    }

    @Test
    void eventValidationFailsWhenOnlyCOsAreSetAndExist()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo cc = categoryCombo();
        program.setCategoryCombo( cc );
        CategoryOption co = cc.getCategoryOptions().get( 0 );
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( co ) ) ).thenReturn( co );

        Event event = eventBuilder()
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( co ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1117 &&
            r.getErrorMessage().contains( program.getCategoryCombo().getUid() ) &&
            r.getErrorMessage().contains( co.getUid() ) ) );
    }

    @Test
    void eventValidationSucceedsWhenOnlyCOsAreSetAndEventProgramHasDefaultCC()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo defaultCC = defaultCategoryCombo();
        program.setCategoryCombo( defaultCC );

        CategoryOption defaultCO = defaultCC.getCategoryOptions().get( 0 );
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( defaultCO ) ) ).thenReturn( defaultCO );
        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCC );
        when( preheat.getDefault( CategoryOptionCombo.class ) ).thenReturn( defaultAOC );

        Event event = eventBuilder()
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( defaultCO ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void eventValidationFailsWhenOnlyCOsAreSetToCONotInCCAndEventProgramHasDefaultCC()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );
        CategoryCombo defaultCC = defaultCategoryCombo();
        program.setCategoryCombo( defaultCC );
        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCC );
        when( preheat.getDefault( CategoryOptionCombo.class ) ).thenReturn( defaultAOC );

        CategoryCombo cc = categoryCombo();
        CategoryOption co = cc.getCategoryOptions().get( 0 );
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( co ) ) ).thenReturn( co );

        Event event = eventBuilder()
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( co ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1117 &&
            r.getErrorMessage().contains( program.getCategoryCombo().getUid() ) &&
            r.getErrorMessage().contains( co.getUid() ) ) );
    }

    @Test
    void eventValidationFailsWhenOnlyCOsAreSetToCONotInProgramCC()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );
        CategoryCombo cc = categoryCombo();
        program.setCategoryCombo( cc );

        CategoryOption co = createCategoryOption( 'B' );
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( co ) ) ).thenReturn( co );

        Event event = eventBuilder()
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( co ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1117 &&
            r.getErrorMessage().contains( program.getCategoryCombo().getUid() ) &&
            r.getErrorMessage().contains( co.getUid() ) ) );
    }

    @Test
    void eventValidationSucceedsWhenOnlyAOCIsSet()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo cc = categoryCombo();
        program.setCategoryCombo( cc );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( cc );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( aoc ) ) ).thenReturn( aoc );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( aoc ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void eventValidationSucceedsWhenOnlyAOCIsSetAndEventProgramHasDefaultCC()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo defaultCC = defaultCategoryCombo();
        program.setCategoryCombo( defaultCC );
        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCC );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( defaultAOC ) ) )
            .thenReturn( defaultAOC );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( defaultAOC ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void eventValidationFailsWhenOnlyAOCIsSetEventProgramHasDefaultCCAndAOCIsNotFound()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );
        program.setCategoryCombo( defaultCategoryCombo() );

        String UNKNOWN_AOC_ID = CodeGenerator.generateUid();
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( UNKNOWN_AOC_ID ) ) ).thenReturn( null );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( UNKNOWN_AOC_ID ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1115 ) );
    }

    @Test
    void eventValidationFailsWhenOnlyAOCIsSetAndAOCIsNotFound()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );
        program.setCategoryCombo( categoryCombo() );

        String UNKNOWN_AOC_ID = CodeGenerator.generateUid();
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( UNKNOWN_AOC_ID ) ) ).thenReturn( null );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( UNKNOWN_AOC_ID ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1115 ) );
    }

    @Test
    void eventValidationFailsWhenOnlyAOCIsSetToAOCNotInProgramCC()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );
        program.setCategoryCombo( categoryCombo( 'A' ) );

        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo( 'B' ) );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( aoc ) ) ).thenReturn( aoc );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( aoc ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1054 &&
            r.getErrorMessage().contains( aoc.getUid() ) &&
            r.getErrorMessage().contains( program.getCategoryCombo().getUid() ) ) );
    }

    @Test
    void eventValidationFailsWhenOnlyAOCIsSetToDefaultAOCNotInProgramCC()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );
        program.setCategoryCombo( categoryCombo( 'A' ) );

        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCategoryCombo() );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( defaultAOC ) ) )
            .thenReturn( defaultAOC );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( defaultAOC ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1055 ) );
    }

    @Test
    void eventValidationFailsWhenOnlyAOCIsSetToAOCNotInProgramCCAndEventProgramHasDefaultCC()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );
        program.setCategoryCombo( defaultCategoryCombo() );

        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo( 'B' ) );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( aoc ) ) ).thenReturn( aoc );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( aoc ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1054 &&
            r.getErrorMessage().contains( aoc.getUid() ) &&
            r.getErrorMessage().contains( program.getCategoryCombo().getUid() ) ) );
    }

    @Test
    void eventValidationSucceedsWhenEventAOCAndEventCOsAreSetAndProgramHasDefaultCC()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo defaultCC = defaultCategoryCombo();
        program.setCategoryCombo( defaultCC );
        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCC );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( defaultAOC ) ) )
            .thenReturn( defaultAOC );

        CategoryOption defaultCO = defaultCC.getCategoryOptions().get( 0 );
        program.setCategoryCombo( defaultCC );
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( defaultCO ) ) ).thenReturn( defaultCO );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( defaultAOC ) )
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( defaultCO ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void eventValidationSucceedsWhenEventAOCAndEventCOsAreSetAndBothFound()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo cc = categoryCombo();
        program.setCategoryCombo( cc );
        CategoryOption co = cc.getCategoryOptions().get( 0 );
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( co ) ) ).thenReturn( co );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( cc );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( aoc ) ) ).thenReturn( aoc );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( aoc ) )
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( co ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void eventValidationFailsWhenEventAOCAndEventCOsAreSetAndAOCIsNotFound()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo cc = categoryCombo();
        program.setCategoryCombo( cc );
        CategoryOption co = cc.getCategoryOptions().get( 0 );
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( co ) ) ).thenReturn( co );

        String UNKNOWN_AOC_ID = CodeGenerator.generateUid();
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( UNKNOWN_AOC_ID ) ) ).thenReturn( null );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( UNKNOWN_AOC_ID ) )
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( co ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1115 ) );
    }

    @Test
    void eventValidationFailsWhenEventAOCAndEventCOsAreSetAndAOCIsSetToDefault()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo cc = categoryCombo();
        program.setCategoryCombo( cc );
        CategoryOption co = cc.getCategoryOptions().get( 0 );
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( co ) ) ).thenReturn( co );

        CategoryOptionCombo defaultAOC = firstCategoryOptionCombo( defaultCategoryCombo() );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( defaultAOC ) ) )
            .thenReturn( defaultAOC );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( defaultAOC ) )
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( co ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1055 ) );
    }

    @Test
    void eventValidationFailsWhenEventAOCAndEventCOsAreSetAndCOIsNotFound()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo cc = categoryCombo();
        program.setCategoryCombo( cc );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( cc );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( aoc ) ) ).thenReturn( aoc );

        String UNKNOWN_CO_ID = CodeGenerator.generateUid();
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( UNKNOWN_CO_ID ) ) ).thenReturn( null );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( aoc ) )
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( UNKNOWN_CO_ID ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1116 ) );
    }

    @Test
    void eventValidationFailsAccumulatingAOCAndCOsNotFoundErrors()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo cc = categoryCombo();
        program.setCategoryCombo( cc );
        CategoryOption co = cc.getCategoryOptions().get( 0 );
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( co ) ) ).thenReturn( co );

        String UNKNOWN_CO_ID1 = CodeGenerator.generateUid();
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( UNKNOWN_CO_ID1 ) ) ).thenReturn( null );
        String UNKNOWN_CO_ID2 = CodeGenerator.generateUid();
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( UNKNOWN_CO_ID2 ) ) ).thenReturn( null );

        String UNKNOWN_AOC_ID = CodeGenerator.generateUid();
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( UNKNOWN_AOC_ID ) ) ).thenReturn( null );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( UNKNOWN_AOC_ID ) )
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( UNKNOWN_CO_ID1 ),
                MetadataIdentifier.ofUid( co ), MetadataIdentifier.ofUid( UNKNOWN_CO_ID2 ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 3, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1115 ) );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1116
            && r.getErrorMessage().contains( UNKNOWN_CO_ID1 ) ) );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1116
            && r.getErrorMessage().contains( UNKNOWN_CO_ID2 ) ) );
    }

    @Test
    void eventValidationFailsWhenEventAOCAndEventCOsAreSetAndCOIsNotInProgramCC()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );

        CategoryCombo cc = categoryCombo();
        program.setCategoryCombo( cc );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( cc );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( aoc ) ) ).thenReturn( aoc );

        CategoryOption eventCO = createCategoryOption( 'C' );
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( eventCO ) ) ).thenReturn( eventCO );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( aoc ) )
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( eventCO ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1117 &&
            r.getErrorMessage().contains( eventCO.getUid() ) &&
            r.getErrorMessage().contains( aoc.getUid() ) ) );
    }

    @Test
    void eventValidationFailsWhenEventAOCAndEventCOsAreSetAndInProgramCCButDoNotMatch()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );
        CategoryCombo cc = categoryCombo();
        program.setCategoryCombo( cc );

        CategoryOptionCombo aoc1 = cc.getSortedOptionCombos().get( 0 );
        CategoryOption co1 = (CategoryOption) aoc1.getCategoryOptions().toArray()[0];
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( co1 ) ) ).thenReturn( co1 );

        CategoryOptionCombo aoc2 = cc.getSortedOptionCombos().get( 1 );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( aoc2 ) ) ).thenReturn( aoc2 );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( aoc2 ) )
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( co1 ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1117 &&
            r.getErrorMessage().contains( co1.getUid() ) &&
            r.getErrorMessage().contains( aoc2.getUid() ) ) );
    }

    @Test
    void eventValidationFailsWhenEventAOCAndEventCOsAreSetAndInProgramCCButNotAllCOsInAOCAreGiven()
    {
        OrganisationUnit orgUnit = setupOrgUnit();
        Program program = setupProgram( orgUnit );
        CategoryCombo cc = categoryComboWithTwoCategories();
        program.setCategoryCombo( cc );

        CategoryOptionCombo aoc = firstCategoryOptionCombo( cc );
        when( preheat.getCategoryOptionCombo( MetadataIdentifier.ofUid( aoc ) ) ).thenReturn( aoc );
        CategoryOption co1 = (CategoryOption) aoc.getCategoryOptions().toArray()[0];
        when( preheat.getCategoryOption( MetadataIdentifier.ofUid( co1 ) ) ).thenReturn( co1 );

        Event event = eventBuilder()
            .attributeOptionCombo( MetadataIdentifier.ofUid( aoc ) )
            .attributeCategoryOptions( Set.of( MetadataIdentifier.ofUid( co1 ) ) )
            .build();

        hook.validateEvent( reporter, event );

        assertEquals( 1, reporter.getReportList().size() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1117 &&
            r.getErrorMessage().contains( co1.getUid() ) &&
            r.getErrorMessage().contains( aoc.getUid() ) ) );
    }

    @Test
    void verifyValidationFailsWhenLinkedTrackedEntityIsNotFound()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( trackedEntityRelationshipItem( "validTrackedEntity" ) )
            .to( trackedEntityRelationshipItem( "anotherValidTrackedEntity" ) )
            .relationshipType( relType.getUid() )
            .build();

        hook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E4012 ) );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorMessage ).collect( Collectors.toList() ),
            hasItem( "Could not find `trackedEntity`: `validTrackedEntity`, linked to Relationship." ) );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorMessage ).collect( Collectors.toList() ),
            hasItem( "Could not find `trackedEntity`: `anotherValidTrackedEntity`, linked to Relationship." ) );
    }

    @Test
    void verifyValidationSuccessWhenLinkedTrackedEntityIsFound()
    {

        TrackedEntityInstance validTrackedEntity = new TrackedEntityInstance();
        validTrackedEntity.setUid( "validTrackedEntity" );
        when( bundle.getTrackedEntityInstance( "validTrackedEntity" ) ).thenReturn( validTrackedEntity );

        ReferenceTrackerEntity anotherValidTrackedEntity = new ReferenceTrackerEntity( "anotherValidTrackedEntity",
            null );
        when( preheat.getReference( "anotherValidTrackedEntity" ) )
            .thenReturn( Optional.of( anotherValidTrackedEntity ) );

        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( trackedEntityRelationshipItem( "validTrackedEntity" ) )
            .to( trackedEntityRelationshipItem( "anotherValidTrackedEntity" ) )
            .relationshipType( relType.getUid() )
            .build();

        hook.validateRelationship( reporter, relationship );

        assertFalse( reporter.hasErrors() );
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
    private Program programWithRegistration( @SuppressWarnings( "SameParameterValue" ) String uid,
        OrganisationUnit orgUnit, TrackedEntityType teiType )
    {
        return program( uid, ProgramType.WITH_REGISTRATION, 'A', orgUnit, teiType );
    }

    private Program programWithoutRegistration( @SuppressWarnings( "SameParameterValue" ) String uid,
        OrganisationUnit orgUnit )
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

    private TrackedEntityType trackedEntityType( @SuppressWarnings( "SameParameterValue" ) String uid )
    {
        return trackedEntityType( uid, 'A' );
    }

    private TrackedEntityType trackedEntityType( String uid, char uniqueChar )
    {
        TrackedEntityType trackedEntityType = createTrackedEntityType( uniqueChar );
        trackedEntityType.setUid( uid );
        return trackedEntityType;
    }

    private TrackedEntityInstance trackedEntityInstance( @SuppressWarnings( "SameParameterValue" ) String uid,
        TrackedEntityType type, OrganisationUnit orgUnit )
    {
        TrackedEntityInstance tei = createTrackedEntityInstance( orgUnit );
        tei.setUid( uid );
        tei.setTrackedEntityType( type );
        return tei;
    }

    private ProgramStage programStage( @SuppressWarnings( "SameParameterValue" ) String uid, Program program )
    {
        ProgramStage programStage = createProgramStage( 'A', program );
        programStage.setUid( uid );
        return programStage;
    }

    private ProgramInstance programInstance( @SuppressWarnings( "SameParameterValue" ) String uid, Program program )
    {
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( uid );
        programInstance.setProgram( program );
        return programInstance;
    }

    private Program setupProgram( OrganisationUnit orgUnit )
    {
        Program program = programWithRegistration( PROGRAM_UID, orgUnit );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM_UID ) ) )
            .thenReturn( program );
        when( preheat.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) ) )
            .thenReturn( programStage( PROGRAM_STAGE_ID, program ) );
        when( bundle.getProgramInstance( ENROLLMENT_ID ) )
            .thenReturn( programInstance( ENROLLMENT_ID, program ) );
        return program;
    }

    private OrganisationUnit setupOrgUnit()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( preheat.getOrganisationUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) ) )
            .thenReturn( orgUnit );
        return orgUnit;
    }

    private CategoryCombo defaultCategoryCombo()
    {
        CategoryOption co = new CategoryOption( CategoryOption.DEFAULT_NAME );
        co.setAutoFields();
        assertTrue( co.isDefault(), "tests rely on this CO being the default one" );
        Category ca = createCategory( 'A', co );
        CategoryCombo cc = createCategoryCombo( 'A', ca );
        cc.setName( CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME );
        assertTrue( cc.isDefault(), "tests rely on this CC being the default one" );
        cc.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        CategoryOptionCombo aoc = createCategoryOptionCombo( cc, co );
        assertTrue( aoc.isDefault(), "tests rely on this AOC being the default one" );
        cc.setOptionCombos( Sets.newHashSet( aoc ) );
        return cc;
    }

    private CategoryCombo categoryCombo()
    {
        return categoryCombo( 'A' );
    }

    private CategoryCombo categoryCombo( char uniqueIdentifier )
    {
        CategoryOption co1 = createCategoryOption( uniqueIdentifier );
        CategoryOption co2 = createCategoryOption( uniqueIdentifier );
        Category ca = createCategory( uniqueIdentifier, co1, co2 );
        CategoryCombo cc = createCategoryCombo( uniqueIdentifier, ca );
        cc.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        CategoryOptionCombo aoc1 = createCategoryOptionCombo( cc, co1 );
        CategoryOptionCombo aoc2 = createCategoryOptionCombo( cc, co2 );
        cc.setOptionCombos( Sets.newHashSet( aoc1, aoc2 ) );
        return cc;
    }

    private CategoryCombo categoryComboWithTwoCategories()
    {
        char uniqueIdentifier = 'A';
        CategoryOption co1 = createCategoryOption( uniqueIdentifier );
        Category ca1 = createCategory( uniqueIdentifier, co1 );
        CategoryOption co2 = createCategoryOption( uniqueIdentifier );
        Category ca2 = createCategory( uniqueIdentifier, co2 );
        CategoryCombo cc = createCategoryCombo( uniqueIdentifier, ca1, ca2 );
        cc.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        CategoryOptionCombo aoc1 = createCategoryOptionCombo( cc, co1, co2 );
        cc.setOptionCombos( Sets.newHashSet( aoc1 ) );
        return cc;
    }

    private CategoryOptionCombo firstCategoryOptionCombo( CategoryCombo categoryCombo )
    {
        assertNotNull( categoryCombo.getOptionCombos() );
        assertFalse( categoryCombo.getOptionCombos().isEmpty() );

        return categoryCombo.getSortedOptionCombos().get( 0 );
    }

    private Event.EventBuilder eventBuilder()
    {
        return Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( PROGRAM_UID ) )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_ID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .enrollment( ENROLLMENT_ID );
    }

    private RelationshipType createRelTypeConstraint( @SuppressWarnings( "SameParameterValue" ) RelationshipEntity from,
        @SuppressWarnings( "SameParameterValue" ) RelationshipEntity to )
    {
        RelationshipType relType = new RelationshipType();
        relType.setUid( CodeGenerator.generateUid() );
        RelationshipConstraint relationshipConstraintFrom = new RelationshipConstraint();
        relationshipConstraintFrom.setRelationshipEntity( from );
        RelationshipConstraint relationshipConstraintTo = new RelationshipConstraint();
        relationshipConstraintTo.setRelationshipEntity( to );

        relType.setFromConstraint( relationshipConstraintFrom );
        relType.setToConstraint( relationshipConstraintTo );

        return relType;
    }

    private RelationshipItem trackedEntityRelationshipItem( String trackedEntityUid )
    {
        return RelationshipItem.builder()
            .trackedEntity( trackedEntityUid )
            .build();
    }

}