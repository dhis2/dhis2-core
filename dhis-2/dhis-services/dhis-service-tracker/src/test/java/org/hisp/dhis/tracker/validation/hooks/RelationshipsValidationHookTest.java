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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4000;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4001;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4009;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4010;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4013;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4014;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4018;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
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
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
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
class RelationshipsValidationHookTest
{

    private RelationshipsValidationHook validationHook;

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        validationHook = new RelationshipsValidationHook();

        when( bundle.getPreheat() ).thenReturn( preheat );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
    }

    @Test
    void verifyValidationFailsOnInvalidRelationshipType()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( "do-not-exist" ) )
            .from( trackedEntityRelationshipItem() )
            .to( trackedEntityRelationshipItem() )
            .build();

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, E4009, RELATIONSHIP, relationship.getUid() );
    }

    @Test
    void verifyValidationFailsOnFromWithMultipleDataset()
    {
        String relationshipUid = "nBx6auGDUHG";
        String reltypeUid = CodeGenerator.generateUid();
        Relationship relationship = Relationship.builder()
            .relationship( relationshipUid )
            .relationshipType( MetadataIdentifier.ofUid( reltypeUid ) )
            .from( RelationshipItem.builder()
                .trackedEntity( trackedEntity() )
                .enrollment( enrollment() )
                .build() )
            .to( trackedEntityRelationshipItem() )
            .build();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( reltypeUid );
        RelationshipConstraint constraint = new RelationshipConstraint();
        constraint.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipType.setFromConstraint( constraint );
        relationshipType.setToConstraint( constraint );
        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relationshipType ) );

        validationHook.validateRelationship( reporter, bundle, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4001 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), is(
            "Relationship Item `from` for Relationship `nBx6auGDUHG` is invalid: an Item can link only one Tracker entity." ) );
    }

    @Test
    void verifyValidationFailsOnFromWithNoDataset()
    {
        String relationshipUid = "nBx6auGDUHG";
        String reltypeUid = CodeGenerator.generateUid();
        Relationship relationship = Relationship.builder()
            .relationship( relationshipUid )
            .relationshipType( MetadataIdentifier.ofUid( reltypeUid ) )
            .from( RelationshipItem.builder().build() )
            .to( trackedEntityRelationshipItem() )
            .build();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( reltypeUid );
        RelationshipConstraint constraint = new RelationshipConstraint();
        constraint.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipType.setFromConstraint( constraint );
        relationshipType.setToConstraint( constraint );
        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relationshipType ) );

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, E4013, RELATIONSHIP, relationship.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), is(
            "Relationship Type `from` constraint is missing trackedEntity." ) );
    }

    @Test
    void verifyValidationFailsOnToWithMultipleDataset()
    {
        String relationshipUid = "nBx6auGDUHG";
        String reltypeUid = CodeGenerator.generateUid();
        Relationship relationship = Relationship.builder()
            .relationship( relationshipUid )
            .relationshipType( MetadataIdentifier.ofUid( reltypeUid ) )
            .from( trackedEntityRelationshipItem() )
            .to( RelationshipItem.builder()
                .trackedEntity( trackedEntity() )
                .enrollment( enrollment() )
                .build() )
            .build();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( reltypeUid );
        RelationshipConstraint constraint = new RelationshipConstraint();
        constraint.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipType.setFromConstraint( constraint );
        relationshipType.setToConstraint( constraint );
        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relationshipType ) );

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, E4001, RELATIONSHIP, relationship.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), is(
            "Relationship Item `to` for Relationship `nBx6auGDUHG` is invalid: an Item can link only one Tracker entity." ) );
    }

    @Test
    void verifyValidationFailsOnInvalidToConstraint()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( trackedEntityRelationshipItem() )
            .to( enrollmentRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, E4010, RELATIONSHIP, relationship.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `to` constraint requires a trackedEntity but a enrollment was found." ) );
    }

    @Test
    void verifyValidationFailsOnInvalidToConstraintOfTypeProgramStage()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, PROGRAM_STAGE_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( trackedEntityRelationshipItem() )
            .to( enrollmentRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, E4010, RELATIONSHIP, relationship.getUid() );
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
                .event( event() )
                .build() )
            .to( trackedEntityRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, E4010, RELATIONSHIP, relationship.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `from` constraint requires a enrollment but a event was found." ) );
    }

    @Test
    void verifyValidationFailsOnInvalidToTrackedEntityType()
    {
        TrackerIdSchemeParams idSchemeParams = TrackerIdSchemeParams.builder()
            .idScheme( TrackerIdSchemeParam.CODE ) // to test trackedEntityType
                                                   // idScheme behavior
            .orgUnitIdScheme( TrackerIdSchemeParam.UID )
            .programIdScheme( TrackerIdSchemeParam.UID )
            .programStageIdScheme( TrackerIdSchemeParam.UID )
            .dataElementIdScheme( TrackerIdSchemeParam.UID )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();
        when( preheat.getIdSchemes() ).thenReturn( idSchemeParams );

        RelationshipType relType = createRelTypeConstraint( PROGRAM_INSTANCE, TRACKED_ENTITY_INSTANCE );
        String trackedEntityUid = CodeGenerator.generateUid();

        TrackedEntityType constraintTrackedEntityType = new TrackedEntityType();
        constraintTrackedEntityType.setUid( CodeGenerator.generateUid() );
        constraintTrackedEntityType.setCode( "GREEN" );
        relType.getToConstraint().setTrackedEntityType( constraintTrackedEntityType );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( enrollmentRelationshipItem() )
            .to( trackedEntityRelationshipItem( trackedEntityUid ) )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        TrackedEntityType teiTrackedEntityType = new TrackedEntityType();
        teiTrackedEntityType.setUid( CodeGenerator.generateUid() );
        teiTrackedEntityType.setCode( "RED" );

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setUid( trackedEntityUid );
        trackedEntityInstance.setTrackedEntityType( teiTrackedEntityType );

        when( bundle.getTrackedEntityInstance( trackedEntityUid ) ).thenReturn( trackedEntityInstance );

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, E4014, RELATIONSHIP, relationship.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `to` constraint requires a Tracked Entity having type `GREEN` but `RED` was found." ) );
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
            .from( trackedEntityRelationshipItem( trackedEntityUid ) )
            .to( enrollmentRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        List<TrackedEntity> trackedEntities = new ArrayList<>();

        TrackedEntity trackedEntity = new TrackedEntity();
        trackedEntity.setTrackedEntity( trackedEntityUid );
        String trackedEntityType = CodeGenerator.generateUid();
        trackedEntity.setTrackedEntityType( MetadataIdentifier.ofUid( trackedEntityType ) );
        trackedEntities.add( trackedEntity );

        when( bundle.getTrackedEntities() ).thenReturn( trackedEntities );

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, E4014, RELATIONSHIP, relationship.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `from` constraint requires a Tracked Entity having type `"
                + constraintTrackedEntityType.getUid() + "` but `" + trackedEntityType + "` was found." ) );
    }

    @Test
    void verifyValidationFailsWhenParentObjectFailed()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );
        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( trackedEntityRelationshipItem( "validTrackedEntity" ) )
            .to( trackedEntityRelationshipItem( "notValidTrackedEntity" ) )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();
        reporter.addError( new TrackerErrorReport( "some error", TrackerErrorCode.E9999, TRACKED_ENTITY,
            relationship.getTo().getTrackedEntity() ) );

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, TrackerErrorCode.E4011, RELATIONSHIP, relationship.getUid() );
        assertThat(
            reporter.getReportList().stream().map( TrackerErrorReport::getErrorMessage ).collect( Collectors.toList() ),
            hasItem( "Relationship: `" + relationship.getRelationship() +
                "` cannot be persisted because trackedEntity notValidTrackedEntity referenced by this relationship is not valid." ) );
    }

    @Test
    void verifyValidationSuccessWhenSomeObjectsFailButNoParentObject()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );
        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( trackedEntityRelationshipItem( "validTrackedEntity" ) )
            .to( trackedEntityRelationshipItem( "anotherValidTrackedEntity" ) )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();
        reporter.addError(
            new TrackerErrorReport( "some error", TrackerErrorCode.E9999, TRACKED_ENTITY, "notValidTrackedEntity" ) );

        validationHook.validateRelationship( reporter, bundle, relationship );

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
            .from( trackedEntityRelationshipItem( uid ) )
            .to( trackedEntityRelationshipItem( uid ) )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( preheat.getAll( RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, E4000, RELATIONSHIP, relationship.getUid() );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship: `" + relationship.getRelationship() + "` cannot link to itself" ) );
    }

    @Test
    void verifyValidationFailsWhenRelationshipIsDuplicated()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .from( trackedEntityRelationshipItem() )
            .to( trackedEntityRelationshipItem() )
            .build();
        org.hisp.dhis.relationship.Relationship rel = new org.hisp.dhis.relationship.Relationship();

        when( preheat.getAll( RelationshipType.class ) ).thenReturn( Collections.singletonList( relType ) );
        when( preheat.isDuplicate( relationship ) ).thenReturn( true );

        validationHook.validateRelationship( reporter, bundle, relationship );

        hasTrackerError( reporter, E4018, RELATIONSHIP, relationship.getUid() );
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

    private RelationshipItem trackedEntityRelationshipItem( String trackedEntityUid )
    {
        return RelationshipItem.builder()
            .trackedEntity( trackedEntityUid )
            .build();
    }

    private RelationshipItem trackedEntityRelationshipItem()
    {
        return RelationshipItem.builder()
            .trackedEntity( trackedEntity() )
            .build();
    }

    private RelationshipItem enrollmentRelationshipItem()
    {
        return RelationshipItem.builder()
            .enrollment( enrollment() )
            .build();
    }

    private String trackedEntity()
    {
        return CodeGenerator.generateUid();
    }

    private String enrollment()
    {
        return CodeGenerator.generateUid();
    }

    private String event()
    {
        return CodeGenerator.generateUid();
    }
}