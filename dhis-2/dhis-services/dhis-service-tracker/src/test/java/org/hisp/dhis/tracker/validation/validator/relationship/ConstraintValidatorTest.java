/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.validation.validator.relationship;

import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4010;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4012;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4014;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class ConstraintValidatorTest
{
    private ConstraintValidator validator;

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    @Mock
    private TrackerIdSchemeParams params;

    private Reporter reporter;

    @BeforeEach
    public void setUp()
    {
        validator = new ConstraintValidator();

        when( bundle.getPreheat() ).thenReturn( preheat );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new Reporter( idSchemes );
    }

    @Test
    void shouldBeValidWhenRelationshipTypeIsCorrectlySetAndEntitiesExist()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );
        TrackedEntityType trackedEntityType = new TrackedEntityType();
        relType.getFromConstraint().setTrackedEntityType( trackedEntityType );
        relType.getToConstraint().setTrackedEntityType( trackedEntityType );
        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setTrackedEntityType( trackedEntityType );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( trackedEntityRelationshipItem() )
            .to( trackedEntityRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( bundle.getPreheat().getRelationshipType( relationship.getRelationshipType() ) ).thenReturn( relType );
        when( bundle.getPreheat().getTrackedEntity( anyString() ) ).thenReturn( trackedEntityInstance );
        when( params.toMetadataIdentifier( trackedEntityType ) )
            .thenReturn( MetadataIdentifier.ofUid( trackedEntityType.getUid() ) );
        when( bundle.getPreheat().getIdSchemes() ).thenReturn( params );

        validator.validate( reporter, bundle, relationship );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void shouldFailWhenRelationshipEntityIsTrackedEntityInstanceAndToConstraintIsSetToEnrollment()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( trackedEntityRelationshipItem() )
            .to( enrollmentRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( bundle.getPreheat().getRelationshipType( relationship.getRelationshipType() ) ).thenReturn( relType );

        validator.validate( reporter, bundle, relationship );

        assertHasError( reporter, relationship, E4010,
            "Relationship type `to` constraint requires a trackedEntity but a enrollment was found." );
    }

    @Test
    void shouldFailWhenRelationshipEntityIsTrackedEntityInstanceAndEntityDoesNotExist()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( trackedEntityRelationshipItem() )
            .to( enrollmentRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( bundle.getPreheat().getRelationshipType( relationship.getRelationshipType() ) ).thenReturn( relType );

        validator.validate( reporter, bundle, relationship );

        assertHasError( reporter, relationship, E4012,
            "Could not find `trackedEntity`: `" + relationship.getFrom().getTrackedEntity()
                + "`, linked to relationship." );
    }

    @Test
    void shouldFailWhenRelationshipEntityIsProgramStageInstanceAndToConstraintIsSetToEnrollment()
    {
        RelationshipType relType = createRelTypeConstraint( PROGRAM_INSTANCE, PROGRAM_STAGE_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( enrollmentRelationshipItem() )
            .to( enrollmentRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( bundle.getPreheat().getRelationshipType( relationship.getRelationshipType() ) ).thenReturn( relType );

        validator.validate( reporter, bundle, relationship );

        assertHasError( reporter, relationship, E4010,
            "Relationship type `to` constraint requires a event but a enrollment was found." );
    }

    @Test
    void shouldFailWhenRelationshipEntityIsTrackedEntityInstanceAndEntityTypeDoesNotMatch()
    {
        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );
        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid( "madeUpUid" );
        relType.getFromConstraint().setTrackedEntityType( trackedEntityType );
        relType.getToConstraint().setTrackedEntityType( trackedEntityType );
        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setTrackedEntityType( trackedEntityType );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( trackedEntityRelationshipItem() )
            .to( trackedEntityRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( bundle.getPreheat().getRelationshipType( relationship.getRelationshipType() ) ).thenReturn( relType );
        when( bundle.getPreheat().getTrackedEntity( anyString() ) ).thenReturn( trackedEntityInstance );
        String uid = CodeGenerator.generateUid();
        when( params.toMetadataIdentifier( trackedEntityType ) ).thenReturn( MetadataIdentifier.ofUid( uid ) );
        when( bundle.getPreheat().getIdSchemes() ).thenReturn( params );

        validator.validate( reporter, bundle, relationship );

        assertHasError( reporter, relationship, E4014,
            "Relationship type `from` constraint requires a tracked entity having type `madeUpUid` but `" + uid
                + "` was found." );
    }

    @Test
    void shouldFailWhenRelationshipEntityIsProgramInstanceAndEnrollmentDoesNotExist()
    {
        RelationshipType relType = createRelTypeConstraint( PROGRAM_INSTANCE, PROGRAM_STAGE_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( enrollmentRelationshipItem() )
            .to( enrollmentRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( bundle.getPreheat().getRelationshipType( relationship.getRelationshipType() ) ).thenReturn( relType );

        validator.validate( reporter, bundle, relationship );

        assertHasError( reporter, relationship, E4012,
            "Could not find `enrollment`: `" + relationship.getFrom().getEnrollment() + "`, linked to relationship." );
    }

    @Test
    void shouldFailWhenRelationshipEntityIsProgramInstanceAndFromConstraintIsSetToEvent()
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

        when( bundle.getPreheat().getRelationshipType( relationship.getRelationshipType() ) ).thenReturn( relType );

        validator.validate( reporter, bundle, relationship );

        assertHasError( reporter, relationship, E4010,
            "Relationship type `from` constraint requires a enrollment but a event was found." );
    }

    @Test
    void shouldFailWhenRelationshipEntityIsProgramStageInstanceAndEventDoesNotExist()
    {
        RelationshipType relType = createRelTypeConstraint( PROGRAM_STAGE_INSTANCE, TRACKED_ENTITY_INSTANCE );

        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .event( event() )
                .build() )
            .to( trackedEntityRelationshipItem() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        when( bundle.getPreheat().getRelationshipType( relationship.getRelationshipType() ) ).thenReturn( relType );

        validator.validate( reporter, bundle, relationship );

        assertHasError( reporter, relationship, E4012,
            "Could not find `event`: `" + relationship.getFrom().getEvent() + "`, linked to relationship." );
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
