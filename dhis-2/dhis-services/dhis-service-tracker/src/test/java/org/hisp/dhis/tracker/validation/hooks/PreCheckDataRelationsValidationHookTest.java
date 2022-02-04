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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
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
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
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

    @Mock
    private CategoryService categoryService;

    @Mock
    private TrackerImportValidationContext ctx;

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    @BeforeEach
    void setUp()
    {
        hook = new PreCheckDataRelationsValidationHook( categoryService );

        bundle = TrackerBundle.builder().preheat( preheat ).build();
        when( ctx.getBundle() ).thenReturn( bundle );

        reporter = new ValidationErrorReporter( ctx );
    }

    @Test
    void verifyValidationSuccessForEnrollment()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) )
            .thenReturn( orgUnit );
        TrackedEntityType teiType = trackedEntityType( TEI_TYPE_ID );
        when( ctx.getProgram( PROGRAM_UID ) )
            .thenReturn( programWithRegistration( PROGRAM_UID, orgUnit, teiType ) );
        when( ctx.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( ctx.getTrackedEntityInstance( TEI_ID ) )
            .thenReturn( trackedEntityInstance( TEI_TYPE_ID, teiType, orgUnit ) );

        Enrollment enrollment = Enrollment.builder()
            .orgUnit( ORG_UNIT_ID )
            .program( PROGRAM_UID )
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
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) )
            .thenReturn( orgUnit );
        when( ctx.getProgram( PROGRAM_UID ) )
            .thenReturn( programWithoutRegistration( PROGRAM_UID, orgUnit ) );
        when( ctx.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );

        Enrollment enrollment = Enrollment.builder()
            .orgUnit( ORG_UNIT_ID )
            .enrollment( CodeGenerator.generateUid() )
            .program( PROGRAM_UID )
            .build();

        hook.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1014 ) );
    }

    @Test
    void verifyValidationFailsWhenEnrollmentAndProgramOrganisationUnitDontMatch()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) )
            .thenReturn( orgUnit );
        OrganisationUnit anotherOrgUnit = organisationUnit( CodeGenerator.generateUid() );
        when( ctx.getProgram( PROGRAM_UID ) )
            .thenReturn( programWithRegistration( PROGRAM_UID, anotherOrgUnit ) );
        when( ctx.getProgramWithOrgUnitsMap() )
            .thenReturn(
                Collections.singletonMap( PROGRAM_UID, Collections.singletonList( anotherOrgUnit.getUid() ) ) );

        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( PROGRAM_UID )
            .orgUnit( ORG_UNIT_ID )
            .build();

        hook.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1041 ) );
    }

    @Test
    void verifyValidationFailsWhenEnrollmentAndProgramTeiTypeDontMatch()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) )
            .thenReturn( orgUnit );
        when( ctx.getProgram( PROGRAM_UID ) )
            .thenReturn( programWithRegistration( PROGRAM_UID, orgUnit, trackedEntityType( TEI_TYPE_ID ) ) );
        when( ctx.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        TrackedEntityType anotherTrackedEntityType = trackedEntityType( TEI_ID, 'B' );
        when( ctx.getTrackedEntityInstance( TEI_ID ) )
            .thenReturn( trackedEntityInstance( TEI_ID, anotherTrackedEntityType, orgUnit ) );

        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( PROGRAM_UID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( TEI_ID )
            .build();

        hook.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1022 ) );
    }

    @Test
    void verifyValidationFailsWhenEnrollmentAndProgramTeiTypeDontMatchAndTEIIsInPayload()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) )
            .thenReturn( orgUnit );
        when( ctx.getProgram( PROGRAM_UID ) )
            .thenReturn( programWithRegistration( PROGRAM_UID, orgUnit, trackedEntityType( TEI_TYPE_ID ) ) );
        when( ctx.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( ctx.getTrackedEntityInstance( TEI_ID ) ).thenReturn( null );

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( TEI_ID )
            .trackedEntityType( ANOTHER_TEI_TYPE_ID )
            .build();
        bundle.setTrackedEntities( Collections.singletonList( trackedEntity ) );

        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( PROGRAM_UID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( TEI_ID )
            .build();

        hook.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1022 ) );
    }

    @Test
    void verifyValidationSuccessForEvent()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) )
            .thenReturn( orgUnit );
        Program program = programWithRegistration( PROGRAM_UID, orgUnit );
        when( ctx.getProgram( PROGRAM_UID ) )
            .thenReturn( program );
        when( ctx.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( ctx.getProgramStage( PROGRAM_STAGE_ID ) )
            .thenReturn( programStage( PROGRAM_STAGE_ID, program ) );
        when( ctx.getProgramInstance( ENROLLMENT_ID ) )
            .thenReturn( programInstance( ENROLLMENT_ID, program ) );

        when( preheat.getDefault( CategoryOptionCombo.class ) ).thenReturn( createCategoryOptionCombo( 'A' ) );
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( PROGRAM_UID )
            .programStage( PROGRAM_STAGE_ID )
            .orgUnit( ORG_UNIT_ID )
            .enrollment( ENROLLMENT_ID )
            .build();

        hook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    void verifyValidationFailsWhenEventAndProgramStageProgramDontMatch()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) )
            .thenReturn( orgUnit );
        Program program = programWithRegistration( PROGRAM_UID, orgUnit );
        when( ctx.getProgram( PROGRAM_UID ) )
            .thenReturn( program );
        when( ctx.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( ctx.getProgramStage( PROGRAM_STAGE_ID ) )
            .thenReturn(
                programStage( PROGRAM_STAGE_ID, programWithRegistration( CodeGenerator.generateUid(), orgUnit ) ) );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( PROGRAM_UID )
            .programStage( PROGRAM_STAGE_ID )
            .orgUnit( ORG_UNIT_ID )
            .build();

        hook.validateEvent( reporter, event );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1089 ) );
    }

    @Test
    void verifyValidationFailsWhenProgramIsRegistrationAndEnrollmentIsMissing()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) )
            .thenReturn( orgUnit );
        Program program = programWithRegistration( PROGRAM_UID, orgUnit );
        when( ctx.getProgram( PROGRAM_UID ) )
            .thenReturn( program );
        when( ctx.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( ctx.getProgramStage( PROGRAM_STAGE_ID ) )
            .thenReturn( programStage( PROGRAM_STAGE_ID, program ) );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( PROGRAM_UID )
            .programStage( PROGRAM_STAGE_ID )
            .orgUnit( ORG_UNIT_ID )
            .build();

        hook.validateEvent( reporter, event );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1033 ) );
    }

    @Test
    void verifyValidationFailsWhenEventAndEnrollmentProgramDontMatch()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) )
            .thenReturn( orgUnit );
        Program program = programWithRegistration( PROGRAM_UID, orgUnit );
        when( ctx.getProgram( PROGRAM_UID ) )
            .thenReturn( program );
        when( ctx.getProgramWithOrgUnitsMap() )
            .thenReturn( Collections.singletonMap( PROGRAM_UID, Collections.singletonList( ORG_UNIT_ID ) ) );
        when( ctx.getProgramStage( PROGRAM_STAGE_ID ) )
            .thenReturn( programStage( PROGRAM_STAGE_ID, program ) );
        when( ctx.getProgramInstance( ENROLLMENT_ID ) )
            .thenReturn(
                programInstance( ENROLLMENT_ID, programWithRegistration( CodeGenerator.generateUid(), orgUnit ) ) );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( PROGRAM_UID )
            .programStage( PROGRAM_STAGE_ID )
            .orgUnit( ORG_UNIT_ID )
            .enrollment( ENROLLMENT_ID )
            .build();

        hook.validateEvent( reporter, event );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1079 ) );
    }

    @Test
    void verifyValidationFailsWhenEventAndProgramOrganisationUnitDontMatch()
    {
        OrganisationUnit orgUnit = organisationUnit( ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) )
            .thenReturn( orgUnit );
        OrganisationUnit anotherOrgUnit = organisationUnit( CodeGenerator.generateUid() );
        Program program = programWithRegistration( PROGRAM_UID, anotherOrgUnit );
        when( ctx.getProgram( PROGRAM_UID ) )
            .thenReturn( program );
        when( ctx.getProgramWithOrgUnitsMap() )
            .thenReturn(
                Collections.singletonMap( PROGRAM_UID, Collections.singletonList( anotherOrgUnit.getUid() ) ) );
        when( ctx.getProgramStage( PROGRAM_STAGE_ID ) )
            .thenReturn( programStage( PROGRAM_STAGE_ID, program ) );
        when( ctx.getProgramInstance( ENROLLMENT_ID ) )
            .thenReturn( programInstance( ENROLLMENT_ID, program ) );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( PROGRAM_UID )
            .programStage( PROGRAM_STAGE_ID )
            .orgUnit( ORG_UNIT_ID )
            .enrollment( ENROLLMENT_ID )
            .build();

        hook.validateEvent( reporter, event );

        assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1029 ) );
    }

    @Test
    void verifyValidationFailsWhenLinkedTrackedEntityIsNotFound()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( "validTrackedEntity" )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( "anotherValidTrackedEntity" )
                .build() )
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
        when( ctx.getTrackedEntityInstance( "validTrackedEntity" ) ).thenReturn( validTrackedEntity );

        ReferenceTrackerEntity anotherValidTrackedEntity = new ReferenceTrackerEntity( "anotherValidTrackedEntity",
            null );
        when( ctx.getReference( "anotherValidTrackedEntity" ) ).thenReturn( Optional.of( anotherValidTrackedEntity ) );

        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( "validTrackedEntity" )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( "anotherValidTrackedEntity" )
                .build() )
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

}