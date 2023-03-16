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
import static org.hisp.dhis.tracker.validation.ValidationCode.E4013;
import static org.hisp.dhis.tracker.validation.validator.AssertValidations.assertHasError;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
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
class ItemValidatorTest
{

    private ItemValidator validator;

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private Reporter reporter;

    @BeforeEach
    public void setUp()
    {
        validator = new ItemValidator();

        when( bundle.getPreheat() ).thenReturn( preheat );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new Reporter( idSchemes );
    }

    @Test
    void shouldFailWhenRelationshipEntityIsTrackedEntityInstanceAndFromConstraintIsMissingTrackedEntity()
    {
        String relationshipUid = "nBx6auGDUHG";
        String relTypeUid = CodeGenerator.generateUid();
        MetadataIdentifier metadataIdentifier = MetadataIdentifier.ofUid( relTypeUid );
        Relationship relationship = Relationship.builder()
            .relationship( relationshipUid )
            .relationshipType( metadataIdentifier )
            .from( RelationshipItem.builder().build() )
            .to( trackedEntityRelationshipItem() )
            .build();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( relTypeUid );
        RelationshipConstraint constraint = new RelationshipConstraint();
        constraint.setRelationshipEntity( RelationshipEntity.TRACKED_ENTITY_INSTANCE );
        relationshipType.setFromConstraint( constraint );
        relationshipType.setToConstraint( constraint );

        when( bundle.getPreheat().getRelationshipType( metadataIdentifier ) ).thenReturn( relationshipType );

        validator.validate( reporter, bundle, relationship );

        assertHasError( reporter, relationship, E4013,
            "Relationship Type `from` constraint is missing tracked_entity." );
    }

    @Test
    void shouldFailWhenRelationshipEntityIsProgramStageAndConstraintIsMissingEvent()
    {
        String relationshipUid = "nBx6auGDUHG";
        String relTypeUid = CodeGenerator.generateUid();
        MetadataIdentifier metadataIdentifier = MetadataIdentifier.ofUid( relTypeUid );
        Relationship relationship = Relationship.builder()
            .relationship( relationshipUid )
            .relationshipType( MetadataIdentifier.ofUid( relTypeUid ) )
            .from( RelationshipItem.builder().build() )
            .to( RelationshipItem.builder().build() )
            .build();

        RelationshipType relationshipType = createRelTypeConstraint( PROGRAM_STAGE_INSTANCE );
        relationshipType.setUid( relTypeUid );

        when( bundle.getPreheat().getRelationshipType( metadataIdentifier ) ).thenReturn( relationshipType );

        validator.validate( reporter, bundle, relationship );

        assertAll( () -> {
            assertHasError( reporter, relationship, E4013,
                "Relationship Type `from` constraint is missing event." );
            assertHasError( reporter, relationship, E4013,
                "Relationship Type `to` constraint is missing event." );
        } );
    }

    @Test
    void shouldFailWhenRelationshipEntityIsProgramAndConstraintIsMissingEnrollment()
    {
        String relationshipUid = "nBx6auGDUHG";
        String relTypeUid = CodeGenerator.generateUid();
        MetadataIdentifier metadataIdentifier = MetadataIdentifier.ofUid( relTypeUid );
        Relationship relationship = Relationship.builder()
            .relationship( relationshipUid )
            .relationshipType( MetadataIdentifier.ofUid( relTypeUid ) )
            .from( RelationshipItem.builder().build() )
            .to( RelationshipItem.builder().build() )
            .build();

        RelationshipType relationshipType = createRelTypeConstraint( PROGRAM_INSTANCE );
        relationshipType.setUid( relTypeUid );

        when( bundle.getPreheat().getRelationshipType( metadataIdentifier ) ).thenReturn( relationshipType );

        validator.validate( reporter, bundle, relationship );

        assertAll( () -> {
            assertHasError( reporter, relationship, E4013,
                "Relationship Type `from` constraint is missing enrollment." );
            assertHasError( reporter, relationship, E4013,
                "Relationship Type `to` constraint is missing enrollment." );
        } );
    }

    private RelationshipItem trackedEntityRelationshipItem()
    {
        return RelationshipItem.builder()
            .trackedEntity( CodeGenerator.generateUid() )
            .build();
    }

    private RelationshipType createRelTypeConstraint( RelationshipEntity entity )
    {
        RelationshipConstraint constraint = new RelationshipConstraint();
        constraint.setRelationshipEntity( entity );

        String relTypeUid = CodeGenerator.generateUid();

        RelationshipType relationshipType = new RelationshipType();
        relationshipType.setUid( relTypeUid );
        relationshipType.setFromConstraint( constraint );
        relationshipType.setToConstraint( constraint );

        return relationshipType;
    }

}
