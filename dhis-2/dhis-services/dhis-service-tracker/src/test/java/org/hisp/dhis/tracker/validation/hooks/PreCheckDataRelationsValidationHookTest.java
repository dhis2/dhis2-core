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
import static org.hamcrest.Matchers.hasItem;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.*;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.api.client.util.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
public class PreCheckDataRelationsValidationHookTest extends DhisConvenienceTest
{
    private static final String PROGRAM_WITHOUT_REGISTRATION_ID = "PROGRAM_WITHOUT_REGISTRATION_ID";

    private static final String PROGRAM_WITH_REGISTRATION_ID = "PROGRAM_WITH_REGISTRATION_ID";

    private static final String PROGRAM_STAGE_ID = "PROGRAM_STAGE_ID";

    private static final String ORG_UNIT_ID = "ORG_UNIT_ID";

    private static final String ANOTHER_ORG_UNIT_ID = "ANOTHER_ORG_UNIT_ID";

    private static final String TEI_TYPE_ID = "TEI_TYPE_ID";

    private static final String TEI_ID = "TEI_ID";

    private static final String ANOTHER_TEI_TYPE_ID = "ANOTHER_TEI_TYPE_ID";

    private static final String ANOTHER_TEI_ID = "ANOTHER_TEI_ID";

    private static final String ENROLLMENT_ID = "ENROLLMENT_ID";

    private static final String ANOTHER_ENROLLMENT_ID = "ANOTHER_ENROLLMENT_ID";

    private PreCheckDataRelationsValidationHook validatorToTest;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private CategoryService categoryService;

    @Mock
    private TrackerImportValidationContext ctx;

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    private OrganisationUnit organisationUnit;

    private OrganisationUnit anotherOrganisationUnit;

    private Program programWithRegistration;

    private Program programWithoutRegistration;

    private TrackedEntityType trackedEntityType;

    @Before
    public void setUp()
    {
        validatorToTest = new PreCheckDataRelationsValidationHook( categoryService );
        bundle = TrackerBundle.builder().preheat( preheat ).build();

        when( ctx.getBundle() ).thenReturn( bundle );

        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnit.setUid( ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ORG_UNIT_ID ) ).thenReturn( organisationUnit );

        anotherOrganisationUnit = createOrganisationUnit( 'B' );
        anotherOrganisationUnit.setUid( ANOTHER_ORG_UNIT_ID );
        when( ctx.getOrganisationUnit( ANOTHER_ORG_UNIT_ID ) ).thenReturn( anotherOrganisationUnit );

        trackedEntityType = createTrackedEntityType( 'A' );
        trackedEntityType.setUid( TEI_TYPE_ID );
        when( ctx.getTrackedEntityType( TEI_TYPE_ID ) ).thenReturn( trackedEntityType );

        setupPrograms();

        Map<String, List<String>> programWithOrgUnits = Maps.newHashMap();
        programWithOrgUnits.put( PROGRAM_WITH_REGISTRATION_ID, Lists.newArrayList( ORG_UNIT_ID ) );

        when( ctx.getProgramWithOrgUnitsMap() ).thenReturn( programWithOrgUnits );
    }

    private void setupPrograms()
    {
        programWithoutRegistration = createProgram( 'A' );
        programWithoutRegistration.setUid( PROGRAM_WITHOUT_REGISTRATION_ID );
        programWithoutRegistration.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        programWithoutRegistration.setOrganisationUnits( Sets.newHashSet( organisationUnit ) );
        programWithoutRegistration.setTrackedEntityType( trackedEntityType );
        when( ctx.getProgram( PROGRAM_WITHOUT_REGISTRATION_ID ) ).thenReturn( programWithoutRegistration );

        programWithRegistration = createProgram( 'B' );
        programWithRegistration.setUid( PROGRAM_WITH_REGISTRATION_ID );
        programWithRegistration.setProgramType( ProgramType.WITH_REGISTRATION );
        programWithRegistration.setOrganisationUnits( Sets.newHashSet( organisationUnit ) );
        programWithRegistration.setTrackedEntityType( trackedEntityType );
        when( ctx.getProgram( PROGRAM_WITH_REGISTRATION_ID ) ).thenReturn( programWithRegistration );
    }

    private void setupForEvents()
    {
        ProgramStage programStage = createProgramStage( 'A', programWithRegistration );
        programStage.setUid( PROGRAM_STAGE_ID );
        when( ctx.getProgramStage( PROGRAM_STAGE_ID ) ).thenReturn( programStage );

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( ENROLLMENT_ID );
        programInstance.setProgram( programWithRegistration );
        when( ctx.getProgramInstance( ENROLLMENT_ID ) ).thenReturn( programInstance );

        ProgramInstance anotherProgramInstance = new ProgramInstance();
        anotherProgramInstance.setUid( ANOTHER_ENROLLMENT_ID );
        anotherProgramInstance.setProgram( programWithoutRegistration );
        when( ctx.getProgramInstance( ANOTHER_ENROLLMENT_ID ) ).thenReturn( anotherProgramInstance );

        when( preheat.getDefault( CategoryOptionCombo.class ) ).thenReturn( createCategoryOptionCombo( 'A' ) );
    }

    private void setupEnrollments()
    {
        TrackedEntityType anotherTrackedEntityType = createTrackedEntityType( 'B' );
        anotherTrackedEntityType.setUid( ANOTHER_TEI_TYPE_ID );
        when( ctx.getTrackedEntityType( ANOTHER_TEI_TYPE_ID ) ).thenReturn( anotherTrackedEntityType );

        TrackedEntityInstance trackedEntity = createTrackedEntityInstance( organisationUnit );
        trackedEntity.setUid( TEI_ID );
        trackedEntity.setTrackedEntityType( trackedEntityType );

        when( ctx.getTrackedEntityInstance( TEI_ID ) ).thenReturn( trackedEntity );

        TrackedEntityInstance anotherTrackedEntity = createTrackedEntityInstance( organisationUnit );
        anotherTrackedEntity.setUid( ANOTHER_TEI_ID );
        anotherTrackedEntity.setTrackedEntityType( anotherTrackedEntityType );

        when( ctx.getTrackedEntityInstance( ANOTHER_TEI_ID ) ).thenReturn( anotherTrackedEntity );
    }

    @Test
    public void verifyValidationSuccessForEnrollment()
    {
        setupEnrollments();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( PROGRAM_WITH_REGISTRATION_ID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( TEI_ID )
            .build();

        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyValidationFailsWhenEnrollmentIsNotARegistration()
    {
        setupEnrollments();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .trackedEntity( TEI_ID )
            .program( PROGRAM_WITHOUT_REGISTRATION_ID )
            .orgUnit( ORG_UNIT_ID )
            .build();

        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertEquals( TrackerErrorCode.E1014, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void verifyValidationFailsWhenEnrollmentAndProgramOrganisationUnitDontMatch()
    {
        setupEnrollments();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .trackedEntity( TEI_ID )
            .program( PROGRAM_WITH_REGISTRATION_ID )
            .orgUnit( ANOTHER_ORG_UNIT_ID )
            .build();

        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertEquals( TrackerErrorCode.E1041, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void verifyValidationFailsWhenEnrollmentAndProgramTeiTypeDontMatch()
    {
        setupEnrollments();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( PROGRAM_WITH_REGISTRATION_ID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( ANOTHER_TEI_ID )
            .build();

        reporter = new ValidationErrorReporter( ctx, enrollment );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertEquals( TrackerErrorCode.E1022, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void verifyValidationFailsWhenEnrollmentAndProgramTeiTypeDontMatchAndTEIIsInPayload()
    {
        setupEnrollments();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( PROGRAM_WITH_REGISTRATION_ID )
            .orgUnit( ORG_UNIT_ID )
            .trackedEntity( ANOTHER_TEI_ID )
            .build();

        reporter = new ValidationErrorReporter( ctx, enrollment );

        when( ctx.getTrackedEntityInstance( ANOTHER_TEI_ID ) ).thenReturn( null );

        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntity( ANOTHER_TEI_ID )
            .trackedEntityType( ANOTHER_TEI_TYPE_ID )
            .build();

        bundle.setTrackedEntities( Lists.newArrayList( trackedEntity ) );

        validatorToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertEquals( TrackerErrorCode.E1022, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void verifyValidationSuccessForEvent()
    {
        setupForEvents();
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( PROGRAM_WITH_REGISTRATION_ID )
            .programStage( PROGRAM_STAGE_ID )
            .orgUnit( ORG_UNIT_ID )
            .enrollment( ENROLLMENT_ID )
            .build();

        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyValidationFailsWhenEventAndProgramStageProgramDontMatch()
    {
        setupForEvents();
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( PROGRAM_WITHOUT_REGISTRATION_ID )
            .programStage( PROGRAM_STAGE_ID )
            .orgUnit( ANOTHER_ORG_UNIT_ID )
            .build();

        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertTrue( reporter.hasErrors() );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorCode ).collect( Collectors.toList() ),
            hasItem( TrackerErrorCode.E1089 ) );
    }

    @Test
    public void verifyValidationFailsWhenProgramIsRegistrationAndEnrollmentIsMissing()
    {
        setupForEvents();
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( PROGRAM_WITH_REGISTRATION_ID )
            .programStage( PROGRAM_STAGE_ID )
            .orgUnit( ORG_UNIT_ID )
            .build();

        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertTrue( reporter.hasErrors() );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorCode ).collect( Collectors.toList() ),
            hasItem( TrackerErrorCode.E1033 ) );
    }

    @Test
    public void verifyValidationFailsWhenEventAndEnrollmentProgramDontMatch()
    {
        setupForEvents();
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( PROGRAM_WITH_REGISTRATION_ID )
            .programStage( PROGRAM_STAGE_ID )
            .orgUnit( ORG_UNIT_ID )
            .enrollment( ANOTHER_ENROLLMENT_ID )
            .build();

        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertTrue( reporter.hasErrors() );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorCode ).collect( Collectors.toList() ),
            hasItem( TrackerErrorCode.E1079 ) );
    }

    @Test
    public void verifyValidationFailsWhenEventAndProgramOrganisationUnitDontMatch()
    {
        setupForEvents();
        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( PROGRAM_WITH_REGISTRATION_ID )
            .programStage( PROGRAM_STAGE_ID )
            .orgUnit( ANOTHER_ORG_UNIT_ID )
            .enrollment( ENROLLMENT_ID )
            .build();

        reporter = new ValidationErrorReporter( ctx, event );

        validatorToTest.validateEvent( reporter, event );

        assertTrue( reporter.hasErrors() );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorCode ).collect( Collectors.toList() ),
            hasItem( TrackerErrorCode.E1029 ) );
    }

    @Test
    public void verifyValidationFailsWhenLinkedTrackedEntityIsNotFound()
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

        reporter = new ValidationErrorReporter( ctx, relationship );

        validatorToTest.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorCode ).collect( Collectors.toList() ),
            hasItem( TrackerErrorCode.E4012 ) );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorMessage ).collect( Collectors.toList() ),
            hasItem( "Could not find `trackedEntity`: `validTrackedEntity`, linked to Relationship." ) );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorMessage ).collect( Collectors.toList() ),
            hasItem( "Could not find `trackedEntity`: `anotherValidTrackedEntity`, linked to Relationship." ) );
    }

    @Test
    public void verifyValidationSuccessWhenLinkedTrackedEntityIsFound()
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

        reporter = new ValidationErrorReporter( ctx, relationship );

        validatorToTest.validateRelationship( reporter, relationship );

        assertFalse( reporter.hasErrors() );
    }

    private RelationshipType createRelTypeConstraint( RelationshipEntity from, RelationshipEntity to )
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