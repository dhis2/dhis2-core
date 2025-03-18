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
package org.hisp.dhis.tracker.imports.preprocess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class DuplicateRelationshipsPreProcessorTest {

  private DuplicateRelationshipsPreProcessor preProcessor;

  private TrackerPreheat preheat;

  private static final String REL_TYPE_BIDIRECTIONAL_UID = CodeGenerator.generateUid();

  private static final String REL_TYPE_NONBIDIRECTIONAL_UID = CodeGenerator.generateUid();

  @BeforeEach
  void setUp() {
    preheat = new TrackerPreheat();
    RelationshipType relationshipTypeBidirectional = new RelationshipType();
    relationshipTypeBidirectional.setUid(REL_TYPE_BIDIRECTIONAL_UID);
    relationshipTypeBidirectional.setBidirectional(true);
    RelationshipType relationshipTypeNonBidirectional = new RelationshipType();
    relationshipTypeNonBidirectional.setUid(REL_TYPE_NONBIDIRECTIONAL_UID);
    preheat.put(TrackerIdSchemeParam.UID, relationshipTypeBidirectional);
    preheat.put(TrackerIdSchemeParam.UID, relationshipTypeNonBidirectional);
    this.preProcessor = new DuplicateRelationshipsPreProcessor();
  }

  @Test
  void test_relationshipIsIgnored_on_null_relType() {
    String relType = CodeGenerator.generateUid();
    UID fromTeUid = UID.generate();
    UID toTeUid = UID.generate();
    Relationship relationship1 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    Relationship relationship2 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .preheat(this.preheat)
            .relationships(Lists.newArrayList(relationship1, relationship2))
            .build();
    preProcessor.process(bundle);
    assertThat(bundle.getRelationships(), hasSize(2));
  }

  /*
   * Verifies that:
   *
   * - given 2 identical relationships
   *
   * - one is removed
   */
  @Test
  void shouldRemoveRelationshipFromBundleWhenThereAreTwoIdenticalRelationships() {
    String relType = REL_TYPE_NONBIDIRECTIONAL_UID;
    UID fromTeUid = UID.generate();
    UID toTeUid = UID.generate();
    UID relUid = UID.generate();

    Relationship relationship1 =
        Relationship.builder()
            .relationship(relUid)
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    Relationship relationship2 =
        Relationship.builder()
            .relationship(relUid)
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .preheat(this.preheat)
            .relationships(Lists.newArrayList(relationship1, relationship2))
            .build();
    preProcessor.process(bundle);
    assertThat(bundle.getRelationships(), hasSize(1));
  }

  @Test
  void shouldRemoveRelationshipFromBundleWhenThereAreTwoIdenticalRelationshipsWithDifferentUids() {
    String relType = REL_TYPE_NONBIDIRECTIONAL_UID;
    UID fromTeUid = UID.generate();
    UID toTeUid = UID.generate();
    UID relationship1Uid = UID.generate();
    Relationship relationship1 =
        Relationship.builder()
            .relationship(relationship1Uid)
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    Relationship relationship2 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .preheat(this.preheat)
            .relationships(Lists.newArrayList(relationship1, relationship2))
            .build();
    preProcessor.process(bundle);
    assertThat(bundle.getRelationships(), hasSize(1));
    assertEquals(relationship1Uid, bundle.getRelationships().get(0).getRelationship());
  }

  /*
   * Verifies that:
   *
   * - given 2 non-identical relationships
   *
   * - none is removed
   */
  @Test
  void test_on_different_rels_none_is_removed() {
    UID fromTeUid = UID.generate();
    UID toTeUid = UID.generate();
    Relationship relationship1 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(REL_TYPE_NONBIDIRECTIONAL_UID))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    Relationship relationship2 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(REL_TYPE_NONBIDIRECTIONAL_UID))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(enrollmentRelationshipItem(toTeUid))
            .build();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .preheat(this.preheat)
            .relationships(Lists.newArrayList(relationship1, relationship2))
            .build();
    preProcessor.process(bundle);
    assertThat(bundle.getRelationships(), hasSize(2));
  }

  /*
   * Verifies that:
   *
   * - given 2 relationships having identical but "inverted" data
   *
   * - none is removed
   */
  @Test
  void test_on_identical_but_inverted_rels_none_is_removed() {
    String relType = REL_TYPE_NONBIDIRECTIONAL_UID;
    UID fromTeUid = UID.generate();
    UID toTeUid = UID.generate();
    Relationship relationship1 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    Relationship relationship2 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(toTeUid))
            .to(trackedEntityRelationshipItem(fromTeUid))
            .build();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .preheat(this.preheat)
            .relationships(Lists.newArrayList(relationship1, relationship2))
            .build();
    preProcessor.process(bundle);
    assertThat(bundle.getRelationships(), hasSize(2));
  }

  /*
   * Verifies that:
   *
   * - given 2 identical relationships having identical but "inverted" data
   *
   * - and relationship type's bidirectional property = true
   *
   * - none is removed
   */
  @Test
  void test_on_identical_rels_but_inverted_type_bi_1_is_removed() {
    String relType = REL_TYPE_BIDIRECTIONAL_UID;
    UID fromTeUid = UID.generate();
    UID toTeUid = UID.generate();
    Relationship relationship1 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    Relationship relationship2 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(toTeUid))
            .to(trackedEntityRelationshipItem(fromTeUid))
            .build();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .preheat(this.preheat)
            .relationships(Lists.newArrayList(relationship1, relationship2))
            .build();
    preProcessor.process(bundle);
    assertThat(bundle.getRelationships(), hasSize(1));
  }

  /*
   * Verifies that:
   *
   * - given 2 identical relationships
   *
   * - and relationship type's bidirectional property = true
   *
   * - one is removed
   */
  @Test
  void test_on_identical_rels_relType_bi_1_is_removed() {
    String relType = REL_TYPE_BIDIRECTIONAL_UID;
    UID fromTeUid = UID.generate();
    UID toTeUid = UID.generate();
    Relationship relationship1 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    Relationship relationship2 =
        Relationship.builder()
            .relationship(UID.generate())
            .relationshipType(MetadataIdentifier.ofUid(relType))
            .from(trackedEntityRelationshipItem(fromTeUid))
            .to(trackedEntityRelationshipItem(toTeUid))
            .build();
    TrackerBundle bundle =
        TrackerBundle.builder()
            .preheat(this.preheat)
            .relationships(Lists.newArrayList(relationship1, relationship2))
            .build();
    preProcessor.process(bundle);
    assertThat(bundle.getRelationships(), hasSize(1));
  }

  private RelationshipItem trackedEntityRelationshipItem(UID trackedEntityUid) {
    return RelationshipItem.builder().trackedEntity(trackedEntityUid).build();
  }

  private RelationshipItem enrollmentRelationshipItem(UID enrollmentUid) {
    return RelationshipItem.builder().enrollment(enrollmentUid).build();
  }
}
