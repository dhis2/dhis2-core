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
package org.hisp.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.utils.RelationshipUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RelationshipUtilsTest {
  private static final UID TE_A_UID = UID.generate();

  private static final UID TE_B_UID = UID.generate();

  private static final UID ENROLLMENT_UID = UID.generate();

  private static final UID EVENT_UID = UID.generate();

  private static final String RELATIONSHIP_TYPE_UID = "RELATIONSHIP_TYPE_UID";

  private TrackedEntity teA, teB;

  private Enrollment enrollmentA;

  private TrackerEvent eventA;

  private RelationshipType relationshipType;

  @BeforeEach
  void setup() {
    teA = new TrackedEntity();
    teA.setUid(TE_A_UID.getValue());
    teB = new TrackedEntity();
    teB.setUid(TE_B_UID.getValue());
    enrollmentA = new Enrollment();
    enrollmentA.setUid(ENROLLMENT_UID.getValue());
    eventA = new TrackerEvent();
    eventA.setUid(EVENT_UID.getValue());
    relationshipType = new RelationshipType();
    relationshipType.setUid(RELATIONSHIP_TYPE_UID);
  }

  @Test
  void testExtractRelationshipItemUid() {
    RelationshipItem itemA = new RelationshipItem();
    RelationshipItem itemB = new RelationshipItem();
    RelationshipItem itemC = new RelationshipItem();
    itemA.setTrackedEntity(teA);
    itemB.setEnrollment(enrollmentA);
    itemC.setTrackerEvent(eventA);
    assertEquals(teA.getUid(), RelationshipUtils.extractRelationshipItemUid(itemA));
    assertEquals(enrollmentA.getUid(), RelationshipUtils.extractRelationshipItemUid(itemB));
    assertEquals(eventA.getUid(), RelationshipUtils.extractRelationshipItemUid(itemC));
  }

  @Test
  void testGenerateRelationshipKeyForTrackedEntityToTrackedEntity() {
    Relationship relationship = teAToTrackedEntityBRelationship();
    String key = relationshipType.getUid() + "_" + teA.getUid() + "_" + teB.getUid();
    assertEquals(key, RelationshipUtils.generateRelationshipKey(relationship));
  }

  @Test
  void testGenerateRelationshipInvertedKeyForTrackedEntityToTrackedEntity() {
    Relationship relationship = teAToTrackedEntityBRelationship();
    String invertedKey = relationshipType.getUid() + "_" + teB.getUid() + "_" + teA.getUid();
    assertEquals(invertedKey, RelationshipUtils.generateRelationshipInvertedKey(relationship));
  }

  @Test
  void testGenerateRelationshipKeyForTrackedEntityToEnrollemnt() {
    Relationship relationship = teToEnrollmentRelationship();
    String key = relationshipType.getUid() + "_" + teA.getUid() + "_" + enrollmentA.getUid();
    assertEquals(key, RelationshipUtils.generateRelationshipKey(relationship));
  }

  @Test
  void testGenerateRelationshipInvertedKeyForTrackedEntityToEnrollment() {
    Relationship relationship = teToEnrollmentRelationship();
    String invertedKey =
        relationshipType.getUid() + "_" + enrollmentA.getUid() + "_" + teA.getUid();
    assertEquals(invertedKey, RelationshipUtils.generateRelationshipInvertedKey(relationship));
  }

  @Test
  void testGenerateRelationshipKeyForTrackedEntityToEvent() {
    Relationship relationship = teToEventRelationship();
    String key = relationshipType.getUid() + "_" + teA.getUid() + "_" + eventA.getUid();
    assertEquals(key, RelationshipUtils.generateRelationshipKey(relationship));
  }

  @Test
  void testGenerateRelationshipInvertedKeyForTrackedEntityToEvent() {
    Relationship relationship = teToEventRelationship();
    String invertedKey = relationshipType.getUid() + "_" + eventA.getUid() + "_" + teA.getUid();
    assertEquals(invertedKey, RelationshipUtils.generateRelationshipInvertedKey(relationship));
  }

  @Test
  void testGenerateRelationshipKeyForEnrollmentToEvent() {
    Relationship relationship = enrollmentToEventRelationship();
    String key = relationshipType.getUid() + "_" + enrollmentA.getUid() + "_" + eventA.getUid();
    assertEquals(key, RelationshipUtils.generateRelationshipKey(relationship));
  }

  @Test
  void testGenerateRelationshipInvertedKeyForEnrollmentToEvent() {
    Relationship relationship = enrollmentToEventRelationship();
    String invertedKey =
        relationshipType.getUid() + "_" + eventA.getUid() + "_" + enrollmentA.getUid();
    assertEquals(invertedKey, RelationshipUtils.generateRelationshipInvertedKey(relationship));
  }

  private Relationship teAToTrackedEntityBRelationship() {
    RelationshipItem from = new RelationshipItem();
    RelationshipItem to = new RelationshipItem();
    from.setTrackedEntity(teA);
    to.setTrackedEntity(teB);

    return relationship(from, to);
  }

  private Relationship teToEnrollmentRelationship() {
    RelationshipItem from = new RelationshipItem();
    RelationshipItem to = new RelationshipItem();
    from.setTrackedEntity(teA);
    to.setEnrollment(enrollmentA);

    return relationship(from, to);
  }

  private Relationship teToEventRelationship() {
    RelationshipItem from = new RelationshipItem();
    RelationshipItem to = new RelationshipItem();
    from.setTrackedEntity(teA);
    to.setTrackerEvent(eventA);

    return relationship(from, to);
  }

  private Relationship enrollmentToEventRelationship() {
    RelationshipItem from = new RelationshipItem();
    RelationshipItem to = new RelationshipItem();
    from.setEnrollment(enrollmentA);
    to.setTrackerEvent(eventA);

    return relationship(from, to);
  }

  private Relationship relationship(RelationshipItem from, RelationshipItem to) {
    Relationship relationship = new Relationship();
    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(from);
    relationship.setTo(to);

    return relationship;
  }
}
