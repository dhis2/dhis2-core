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
package org.hisp.dhis.analytics.event.data.programindicator;

import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.random.BeanRandomizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class RelationshipTypeJoinGeneratorTest {
  private static final String ALIAS = "subax";

  private static final String RELATIONSHIP_JOIN =
      " left join relationship r on r.from_relationshipitemid = ri.relationshipitemid "
          + "left join relationshipitem ri2 on r.to_relationshipitemid = ri2.relationshipitemid "
          + "left join relationshiptype rty on rty.relationshiptypeid = r.relationshiptypeid ";

  private static final String TRACKED_ENTITY_JOIN_START =
      ALIAS
          + ".trackedentity in (select te.uid from trackedentity te left join relationshipitem ri on te.trackedentityid = ri.trackedentityid ";

  private static final String ENROLLMENT_JOIN_START =
      ALIAS
          + ".enrollment in (select en.uid from enrollment en left join relationshipitem ri on en.enrollmentid = ri.enrollmentid ";

  private static final String EVENT_JOIN_START =
      ALIAS
          + ".event in (select ev.uid from event ev left join relationshipitem ri on ev.eventid = ri.eventid ";

  private static final String TRACKED_ENTITY_RELTO_JOIN =
      "left join trackedentity te2 on te2.trackedentityid = ri2.trackedentityid";

  private static final String ENROLLMENT_RELTO_JOIN =
      "left join enrollment en2 on en2.enrollmentid = ri2.enrollmentid";

  private static final String EVENT_RELTO_JOIN = "left join event ev2 on ev2.eventid = ri2.eventid";

  private final BeanRandomizer rnd = BeanRandomizer.create();

  @Test
  @DisplayName("Should generate correct join for tracked entity to tracked entity")
  void verifyTrackedEntityToTrackedEntity() {
    RelationshipType relationshipType =
        createRelationshipType(
            TRACKED_ENTITY_INSTANCE.getName(), TRACKED_ENTITY_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
    asserter(relationshipType, AnalyticsType.EVENT);
  }

  @Test
  @DisplayName("Should generate correct join for program to program")
  void verifyEnrollmentToEnrollment() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_INSTANCE.getName(), PROGRAM_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  @DisplayName("Should generate correct join for program stage to program stage")
  void verifyEventToEvent() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_STAGE_INSTANCE.getName(), PROGRAM_STAGE_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  @DisplayName("Should generate correct join for tracked entity to program")
  void verifyTrackedEntityToEnrollment() {
    RelationshipType relationshipType =
        createRelationshipType(TRACKED_ENTITY_INSTANCE.getName(), PROGRAM_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  @DisplayName("Should generate correct join for tracked entity to program stage instance")
  void verifyTrackedEntityToEvent() {
    RelationshipType relationshipType =
        createRelationshipType(TRACKED_ENTITY_INSTANCE.getName(), PROGRAM_STAGE_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  @DisplayName("Should generate correct join for program to tracked entity")
  void verifyEnrollmentToTrackedEntity() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_INSTANCE.getName(), TRACKED_ENTITY_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  @DisplayName("Should generate correct join for program to program stage instance")
  void verifyEnrollmentToEvent() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_INSTANCE.getName(), PROGRAM_STAGE_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  @DisplayName("Should generate correct join for program stage instance to tracked entity")
  void verifyEventToTrackedEntity() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_STAGE_INSTANCE.getName(), TRACKED_ENTITY_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  @DisplayName("Should generate correct join for program stage instance to program")
  void verifyEventToEnrollment() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_STAGE_INSTANCE.getName(), PROGRAM_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  private RelationshipType createRelationshipType(String fromConstraint, String toConstraint) {
    RelationshipType relationshipType = rnd.nextObject(RelationshipType.class);
    relationshipType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.get(fromConstraint));
    relationshipType.getToConstraint().setRelationshipEntity(RelationshipEntity.get(toConstraint));
    return relationshipType;
  }

  private String addWhere(RelationshipType relationshipType) {
    return new StringSubstitutor(Map.of("relationshipid", relationshipType.getId()))
        .replace(RelationshipTypeJoinGenerator.RELATIONSHIP_JOIN);
  }

  private void asserter(RelationshipType relationshipType, AnalyticsType type) {
    RelationshipEntity from = relationshipType.getFromConstraint().getRelationshipEntity();
    RelationshipEntity to = relationshipType.getToConstraint().getRelationshipEntity();
    String expected = " ";
    expected += getFromRelationshipEntity(from, type);
    expected += RELATIONSHIP_JOIN;
    expected += getToRelationshipEntity(to);
    expected += addWhere(relationshipType);
    expected +=
        (to.equals(TRACKED_ENTITY_INSTANCE)
            ? " and te2.uid = ax.trackedentity )"
            : (to.equals(PROGRAM_INSTANCE)
                ? " and en2.uid = ax.enrollment )"
                : " and ev2.uid = ax.event )"));

    assertEquals(expected, RelationshipTypeJoinGenerator.generate(ALIAS, relationshipType, type));
  }

  private static String getFromRelationshipEntity(
      RelationshipEntity relationshipEntity, AnalyticsType programIndicatorType) {
    return switch (relationshipEntity) {
      case TRACKED_ENTITY_INSTANCE -> TRACKED_ENTITY_JOIN_START;
      case PROGRAM_STAGE_INSTANCE, PROGRAM_INSTANCE ->
          (programIndicatorType.equals(AnalyticsType.EVENT)
              ? EVENT_JOIN_START
              : ENROLLMENT_JOIN_START);
    };
  }

  private static String getToRelationshipEntity(RelationshipEntity relationshipEntity) {
    return switch (relationshipEntity) {
      case TRACKED_ENTITY_INSTANCE -> TRACKED_ENTITY_RELTO_JOIN;
      case PROGRAM_STAGE_INSTANCE -> EVENT_RELTO_JOIN;
      case PROGRAM_INSTANCE -> ENROLLMENT_RELTO_JOIN;
    };
  }
}
