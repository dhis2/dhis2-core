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
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class RelationshipTypeJoinGeneratorTest {
  private static final String ALIAS = "subax";

  private static final String RELATIONSHIP_JOIN =
      " LEFT JOIN relationship r on r.from_relationshipitemid = ri.relationshipitemid "
          + "LEFT JOIN relationshipitem ri2 on r.to_relationshipitemid = ri2.relationshipitemid "
          + "LEFT JOIN relationshiptype rty on rty.relationshiptypeid = r.relationshiptypeid ";

  private static final String TRACKED_ENTITY_JOIN_START =
      ALIAS
          + ".trackedentity in (select te.uid from trackedentity te LEFT JOIN relationshipitem ri on te.trackedentityid = ri.trackedentityid ";

  private static final String ENROLLMENT_JOIN_START =
      ALIAS
          + ".enrollment in (select en.uid from enrollment en LEFT JOIN relationshipitem ri on en.enrollmentid = ri.enrollmentid ";

  private static final String EVENT_JOIN_START =
      ALIAS
          + ".event in (select ev.uid from event ev LEFT JOIN relationshipitem ri on ev.eventid = ri.eventid ";

  private static final String TRACKED_ENTITY_RELTO_JOIN =
      "LEFT JOIN trackedentity te on te.trackedentityid = ri2.trackedentityid";

  private static final String ENROLLMENT_RELTO_JOIN =
      "LEFT JOIN enrollment en on en.enrollmentid = ri2.enrollmentid";

  private static final String EVENT_RELTO_JOIN = "LEFT JOIN event ev on ev.eventid = ri2.eventid";

  private final BeanRandomizer rnd = BeanRandomizer.create();

  @Test
  void verifyTrackedEntityToTrackedEntity() {
    RelationshipType relationshipType =
        createRelationshipType(
            TRACKED_ENTITY_INSTANCE.getName(), TRACKED_ENTITY_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
    asserter(relationshipType, AnalyticsType.EVENT);
  }

  @Test
  void verifyEnrollmentToEnrollment() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_INSTANCE.getName(), PROGRAM_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  void verifyEventToEvent() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_STAGE_INSTANCE.getName(), PROGRAM_STAGE_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  void verifyTrackedEntityToEnrollment() {
    RelationshipType relationshipType =
        createRelationshipType(TRACKED_ENTITY_INSTANCE.getName(), PROGRAM_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  void verifyTrackedEntityToEvent() {
    RelationshipType relationshipType =
        createRelationshipType(TRACKED_ENTITY_INSTANCE.getName(), PROGRAM_STAGE_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  void verifyEnrollmentToTrackedEntity() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_INSTANCE.getName(), TRACKED_ENTITY_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  void verifyEnrollmentToEvent() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_INSTANCE.getName(), PROGRAM_STAGE_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
  void verifyEventToTrackedEntity() {
    RelationshipType relationshipType =
        createRelationshipType(PROGRAM_STAGE_INSTANCE.getName(), TRACKED_ENTITY_INSTANCE.getName());
    asserter(relationshipType, AnalyticsType.EVENT);
    asserter(relationshipType, AnalyticsType.ENROLLMENT);
  }

  @Test
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
            ? " AND te.uid = ax.trackedentity )"
            : (to.equals(PROGRAM_INSTANCE)
                ? " AND en.uid = ax.enrollment )"
                : " AND ev.uid = ax.event )"));
    assertEquals(expected, RelationshipTypeJoinGenerator.generate(ALIAS, relationshipType, type));
  }

  private static String getFromRelationshipEntity(
      RelationshipEntity relationshipEntity, AnalyticsType programIndicatorType) {
    switch (relationshipEntity) {
      case TRACKED_ENTITY_INSTANCE:
        return TRACKED_ENTITY_JOIN_START;
      case PROGRAM_STAGE_INSTANCE:
      case PROGRAM_INSTANCE:
        return (programIndicatorType.equals(AnalyticsType.EVENT)
            ? EVENT_JOIN_START
            : ENROLLMENT_JOIN_START);
    }
    return "";
  }

  private static String getToRelationshipEntity(RelationshipEntity relationshipEntity) {
    switch (relationshipEntity) {
      case TRACKED_ENTITY_INSTANCE:
        return TRACKED_ENTITY_RELTO_JOIN;
      case PROGRAM_STAGE_INSTANCE:
        return EVENT_RELTO_JOIN;
      case PROGRAM_INSTANCE:
        return ENROLLMENT_RELTO_JOIN;
    }
    return "";
  }
}
