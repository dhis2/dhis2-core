package org.hisp.dhis.tracker.validation.hooks;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
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

/**
 * @author Luciano Fiandesio
 */
public class RelationshipsValidationHookTest
{
    private RelationshipsValidationHook validationHook;

    @Mock
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerImportValidationContext ctx;

    @Mock
    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    @Before
    public void setUp()
    {
        validationHook = new RelationshipsValidationHook( trackedEntityAttributeService );

        when( ctx.getBundle() ).thenReturn( bundle );
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( bundle.getPreheat() ).thenReturn( preheat );

        reporter = new ValidationErrorReporter( ctx, Relationship.class );
    }

    @Test
    public void verifyValidationFailsOnInvalidRelationshipType()
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

        reporter.setMainId( relationship.getRelationship() );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4009 ) );
    }

    @Test
    public void verifyValidationFailsOnMissingFrom()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( CodeGenerator.generateUid() )
            .to( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .build();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( relationship.getRelationshipType() );
        when( preheat.getAll( TrackerIdScheme.UID, RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relationshipType ) );

        reporter.setMainId( relationship.getRelationship() );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4007 ) );
    }

    @Test
    public void verifyValidationFailsOnMissingTo()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .build();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( relationship.getRelationshipType() );
        when( preheat.getAll( TrackerIdScheme.UID, RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relationshipType ) );

        reporter.setMainId( relationship.getRelationship() );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4008 ) );
    }

    @Test
    public void verifyValidationFailsOnFromWithMultipleDataset()
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
        when( preheat.getAll( TrackerIdScheme.UID, RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relationshipType ) );

        reporter.setMainId( relationshipUid );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4001 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), is(
            "Relationship Item `from` for Relationship `nBx6auGDUHG` is invalid: an Item can link only one Tracker entity." ) );
    }

    @Test
    public void verifyValidationFailsOnToWithMultipleDataset()
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
        when( preheat.getAll( TrackerIdScheme.UID, RelationshipType.class ) )
                .thenReturn( Collections.singletonList( relationshipType ) );

        reporter.setMainId( relationshipUid );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4001 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(), is(
           "Relationship Item `to` for Relationship `nBx6auGDUHG` is invalid: an Item can link only one Tracker entity." ) );
    }

    @Test
    public void verifyValidationFailsOnMissingRelationshipType()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .build();

        reporter.setMainId( relationship.getRelationship() );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4004 ) );
    }

    @Test
    public void verifyValidationFailsOnInvalidToConstraint()
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

        when( preheat.getAll( TrackerIdScheme.UID, RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        reporter.setMainId( relationship.getRelationship() );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4010 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `to` constraint requires a trackedEntity but a enrollment was found." ) );

    }

    @Test
    public void verifyValidationFailsOnInvalidFromConstraint()
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

        when( preheat.getAll( TrackerIdScheme.UID, RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        reporter.setMainId( relationship.getRelationship() );
        validationHook.validateRelationship( reporter, relationship );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E4010 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Relationship Type `from` constraint requires a enrollment but a event was found." ) );
    }

    @Test
    public void verifyFailAuto()
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

        when( preheat.getAll( TrackerIdScheme.UID, RelationshipType.class ) )
            .thenReturn( Collections.singletonList( relType ) );

        reporter.setMainId( relationship.getRelationship() );
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