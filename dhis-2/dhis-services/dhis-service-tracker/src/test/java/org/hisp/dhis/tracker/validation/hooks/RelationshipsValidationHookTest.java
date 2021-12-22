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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.tracker.report.TrackerErrorReport.newReport;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class RelationshipsValidationHookTest
{

    private RelationshipsValidationHook validationHook;

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerImportValidationContext ctx;

    @Mock
    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        validationHook = new RelationshipsValidationHook();

        when( ctx.getBundle() ).thenReturn( bundle );
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( bundle.getPreheat() ).thenReturn( preheat );
    }

    @Test
    void verifyValidationFailsOnInvalidRelationshipType()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( "do-not-exist" )
            .from( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .build();

        reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4009 ) );
    }

    @Test
    void verifyValidationFailsOnFromWithMultipleDataset()
    {
        String relationshipUid = "nBx6auGDUHG";
        Relationship relationship = Relationship.builder()
            .relationship( relationshipUid )
            .relationshipType( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .enrollment( CodeGenerator.generateUid() )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .build();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( relationship.getRelationshipType() );
        RelationshipConstraint constraint = new RelationshipConstraint();
        constraint.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipType.setFromConstraint( constraint );
        relationshipType.setToConstraint( constraint );
        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relationshipType ) );

        reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4001 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), is(
            "Relationship Item `from` for Relationship `nBx6auGDUHG` is invalid: an Item can link only one Tracker entity." ) );
    }

    @Test
    void verifyValidationFailsOnFromWithNoDataset()
    {
        String relationshipUid = "nBx6auGDUHG";
        Relationship relationship = Relationship.builder()
            .relationship( relationshipUid )
            .relationshipType( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .build();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( relationship.getRelationshipType() );
        RelationshipConstraint constraint = new RelationshipConstraint();
        constraint.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipType.setFromConstraint( constraint );
        relationshipType.setToConstraint( constraint );
        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relationshipType ) );

        reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4013 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), is(
            "Relationship Type `from` constraint is missing trackedEntity." ) );
    }

    @Test
    void verifyValidationFailsOnToWithMultipleDataset()
    {
        String relationshipUid = "nBx6auGDUHG";
        Relationship relationship = Relationship.builder()
            .relationship( relationshipUid )
            .relationshipType( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .enrollment( CodeGenerator.generateUid() )
                .build() )
            .build();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( relationship.getRelationshipType() );
        RelationshipConstraint constraint = new RelationshipConstraint();
        constraint.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipType.setFromConstraint( constraint );
        relationshipType.setToConstraint( constraint );
        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relationshipType ) );

        reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4001 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), is(
            "Relationship Item `to` for Relationship `nBx6auGDUHG` is invalid: an Item can link only one Tracker entity." ) );
    }

    @Test
    void verifyValidationFailsOnInvalidToConstraint()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .to( RelationshipItem.builder()
                .enrollment( CodeGenerator.generateUid() )
                .build() )
            .relationshipType( relType.getUid() )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4010 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `to` constraint requires a trackedEntity but a enrollment was found." ) );
    }

    @Test
    void verifyValidationFailsOnInvalidToConstraintOfTypeProgramStage()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, PROGRAM_STAGE_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .to( RelationshipItem.builder()
                .enrollment( CodeGenerator.generateUid() )
                .build() )
            .relationshipType( relType.getUid() )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4010 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `to` constraint requires a event but a enrollment was found." ) );
    }

    @Test
    void verifyValidationFailsOnInvalidFromConstraint()
    {
        RelationshipType relType = createRelTypeConstraint( PROGRAM_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .event( CodeGenerator.generateUid() )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .relationshipType( relType.getUid() )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4010 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `from` constraint requires a enrollment but a event was found." ) );
    }

    @Test
    void verifyValidationFailsOnInvalidToTrackedEntityType()
    {
        RelationshipType relType = createRelTypeConstraint( PROGRAM_INSTANCE, TRACKED_ENTITY_INSTANCE );
        String trackedEntityUid = CodeGenerator.generateUid();

        TrackedEntityType constraintTrackedEntityType = new TrackedEntityType();
        constraintTrackedEntityType.setUid( CodeGenerator.generateUid() );
        relType.getToConstraint().setTrackedEntityType( constraintTrackedEntityType );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .enrollment( CodeGenerator.generateUid() )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( trackedEntityUid )
                .build() )
            .relationshipType( relType.getUid() )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        TrackedEntityType teiTrackedEntityType = new TrackedEntityType();
        teiTrackedEntityType.setUid( CodeGenerator.generateUid() );

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( trackedEntityUid );
        trackedEntityInstance.setTrackedEntityType( teiTrackedEntityType );

        when( ctx.getTrackedEntityInstance( trackedEntityUid ) ).thenReturn( trackedEntityInstance );

        reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4014 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `to` constraint requires a Tracked Entity having type `"
                + constraintTrackedEntityType.getUid() + "` but `" + teiTrackedEntityType.getUid() + "` was found." ) );
    }

    @Test
    void verifyValidationFailsOnInvalidFromTrackedEntityType()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, PROGRAM_INSTANCE );
        String trackedEntityUid = CodeGenerator.generateUid();

        TrackedEntityType constraintTrackedEntityType = new TrackedEntityType();
        constraintTrackedEntityType.setUid( CodeGenerator.generateUid() );
        relType.getFromConstraint().setTrackedEntityType( constraintTrackedEntityType );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( trackedEntityUid )
                .build() )
            .to( RelationshipItem.builder()
                .enrollment( CodeGenerator.generateUid() )
                .build() )
            .relationshipType( relType.getUid() )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        List<TrackedEntity> trackedEntities = new ArrayList<>();

        TrackedEntity trackedEntity = new TrackedEntity();
        trackedEntity.setTrackedEntity( trackedEntityUid );
        trackedEntity.setTrackedEntityType( CodeGenerator.generateUid() );
        trackedEntities.add( trackedEntity );

        when( bundle.getTrackedEntities() ).thenReturn( trackedEntities );

        reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4014 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `from` constraint requires a Tracked Entity having type `"
                + constraintTrackedEntityType.getUid() + "` but `" + trackedEntity.getTrackedEntityType()
                + "` was found." ) );
    }

    @Test
    void verifyValidationFailsWhenParentObjectFailed()
    {
        reporter = new ValidationErrorReporter( ctx );
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( "validTrackedEntity" )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( "notValidTrackedEntity" )
                .build() )
            .relationshipType( relType.getUid() )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        TrackedEntity tei = new TrackedEntity();
        tei.setTrackedEntity( "notValidTrackedEntity" );
        ValidationErrorReporter teiErrorReport = new ValidationErrorReporter( ctx, tei );
        teiErrorReport.addError( newReport( TrackerErrorCode.E9999 ).uid( "notValidTrackedEntity" ) );

        reporter.merge( teiErrorReport );

        ValidationErrorReporter relReporter = new ValidationErrorReporter( ctx, relationship );
        relReporter.getInvalidDTOs().putAll( reporter.getInvalidDTOs() );

        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorCode ).collect( Collectors.toList() ),
            hasItem( TrackerErrorCode.E4011 ) );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorMessage ).collect( Collectors.toList() ),
            hasItem( "Relationship: `" + relationship.getRelationship() +
                "` cannot be persisted because trackedEntity notValidTrackedEntity referenced by this relationship is not valid." ) );
    }

    @Test
    void verifyValidationSuccessWhenSomeObjectsFailButNoParentObject()
    {
        reporter = new ValidationErrorReporter( ctx );
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

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        TrackedEntity tei = new TrackedEntity();
        tei.setTrackedEntity( "notValidTrackedEntity" );
        ValidationErrorReporter teiErrorReport = new ValidationErrorReporter( ctx, tei );

        teiErrorReport.addError( newReport( TrackerErrorCode.E9999 ).uid( "notValidTrackedEntity" ) );

        reporter.merge( teiErrorReport );

        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorCode ).collect( Collectors.toList() ),
            not( hasItem( TrackerErrorCode.E4011 ) ) );
    }

    @Test
    void verifyFailAuto()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );
        String uid = CodeGenerator.generateUid();
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( uid )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( uid )
                .build() )
            .relationshipType( relType.getUid() )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4000 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship: `" + relationship.getRelationship() + "` cannot link to itself" ) );
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