/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicateRelationshipSupplierTest extends TestBase {

  private static final UID REL_A_UID = UID.generate();

  private static final UID REL_B_UID = UID.generate();

  private static final UID REL_C_UID = UID.generate();

  private static final UID TE_A_UID = UID.generate();

  private static final UID TE_B_UID = UID.generate();

  private static final UID TE_C_UID = UID.generate();

  private static final RelationshipKey KEY_REL_A =
      keyOfTEToTERelationship("UNIRELTYPE", TE_A_UID, TE_B_UID);

  private static final RelationshipKey KEY_REL_B =
      keyOfTEToTERelationship("BIRELTYPE", TE_B_UID, TE_C_UID);

  private static final RelationshipKey KEY_REL_C =
      keyOfTEToTERelationship("UNIRELTYPE", TE_C_UID, TE_A_UID);

  private static final String UNIDIRECTIONAL_RELATIONSHIP_TYPE_UID = "UNIRELTYPE";

  private static final String BIDIRECTIONAL_RELATIONSHIP_TYPE_UID = "BIRELTYPE";

  private org.hisp.dhis.tracker.imports.domain.Relationship relationshipA;

  private org.hisp.dhis.tracker.imports.domain.Relationship relationshipB;

  private org.hisp.dhis.tracker.imports.domain.Relationship relationshipC;

  private RelationshipType unidirectionalRelationshipType;

  private RelationshipType bidirectionalRelationshipType;

  private TrackedEntity teA, teB, teC;

  private TrackerPreheat preheat;

  @Mock private RelationshipService relationshipService;

  @InjectMocks private DuplicateRelationshipSupplier supplier;

  @BeforeEach
  public void setUp() {
    unidirectionalRelationshipType = createRelationshipType('A');
    unidirectionalRelationshipType.setUid(UNIDIRECTIONAL_RELATIONSHIP_TYPE_UID);
    unidirectionalRelationshipType.setBidirectional(false);

    bidirectionalRelationshipType = createRelationshipType('B');
    bidirectionalRelationshipType.setUid(BIDIRECTIONAL_RELATIONSHIP_TYPE_UID);
    bidirectionalRelationshipType.setBidirectional(true);

    OrganisationUnit organisationUnit = createOrganisationUnit('A');

    teA = createTrackedEntity(organisationUnit, createTrackedEntityType('D'));
    teA.setUid(TE_A_UID.getValue());
    teB = createTrackedEntity(organisationUnit, createTrackedEntityType('E'));
    teB.setUid(TE_B_UID.getValue());
    teC = createTrackedEntity(organisationUnit, createTrackedEntityType('F'));
    teC.setUid(TE_C_UID.getValue());

    relationshipA =
        org.hisp.dhis.tracker.imports.domain.Relationship.builder()
            .relationship(REL_A_UID)
            .relationshipType(MetadataIdentifier.ofUid(UNIDIRECTIONAL_RELATIONSHIP_TYPE_UID))
            .from(RelationshipItem.builder().trackedEntity(TE_A_UID).build())
            .to(RelationshipItem.builder().trackedEntity(TE_B_UID).build())
            .build();

    relationshipB =
        org.hisp.dhis.tracker.imports.domain.Relationship.builder()
            .relationship(REL_B_UID)
            .relationshipType(MetadataIdentifier.ofUid(BIDIRECTIONAL_RELATIONSHIP_TYPE_UID))
            .from(RelationshipItem.builder().trackedEntity(TE_B_UID).build())
            .to(RelationshipItem.builder().trackedEntity(TE_C_UID).build())
            .build();

    relationshipC =
        org.hisp.dhis.tracker.imports.domain.Relationship.builder()
            .relationship(REL_C_UID)
            .relationshipType(MetadataIdentifier.ofUid(UNIDIRECTIONAL_RELATIONSHIP_TYPE_UID))
            .from(RelationshipItem.builder().trackedEntity(TE_C_UID).build())
            .to(RelationshipItem.builder().trackedEntity(TE_A_UID).build())
            .build();

    preheat = new TrackerPreheat();
    preheat.put(TrackerIdSchemeParam.UID, unidirectionalRelationshipType);
    preheat.put(TrackerIdSchemeParam.UID, bidirectionalRelationshipType);
  }

  @Test
  void verifySupplier() {
    when(relationshipService.getRelationshipsByRelationshipKeys(
            List.of(KEY_REL_A, KEY_REL_B, KEY_REL_C)))
        .thenReturn(List.of(relationshipA(), relationshipB()));

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .relationships(List.of(relationshipA, relationshipB, relationshipC))
            .build();

    supplier.preheatAdd(trackerObjects, preheat);

    assertTrue(preheat.isDuplicate(relationshipA));
    assertFalse(preheat.isDuplicate(invertTeToTeRelationship(relationshipA)));
    assertTrue(preheat.isDuplicate(relationshipB));
    assertTrue(preheat.isDuplicate(invertTeToTeRelationship(relationshipB)));
    assertFalse(preheat.isDuplicate(relationshipC));
  }

  private Relationship relationshipA() {
    return createTeToTeRelationship(teA, teB, unidirectionalRelationshipType);
  }

  private Relationship relationshipB() {
    return createTeToTeRelationship(teB, teC, bidirectionalRelationshipType);
  }

  private org.hisp.dhis.tracker.imports.domain.Relationship invertTeToTeRelationship(
      org.hisp.dhis.tracker.imports.domain.Relationship relationship) {
    return org.hisp.dhis.tracker.imports.domain.Relationship.builder()
        .relationship(relationship.getRelationship())
        .relationshipType(relationship.getRelationshipType())
        .from(
            RelationshipItem.builder()
                .trackedEntity(relationship.getTo().getTrackedEntity())
                .build())
        .to(
            RelationshipItem.builder()
                .trackedEntity(relationship.getFrom().getTrackedEntity())
                .build())
        .build();
  }

  private static RelationshipKey keyOfTEToTERelationship(
      String relationshipType, UID from, UID to) {
    return RelationshipKey.of(
        relationshipType,
        RelationshipKey.RelationshipItemKey.builder().trackedEntity(from).build(),
        RelationshipKey.RelationshipItemKey.builder().trackedEntity(to).build());
  }
}
