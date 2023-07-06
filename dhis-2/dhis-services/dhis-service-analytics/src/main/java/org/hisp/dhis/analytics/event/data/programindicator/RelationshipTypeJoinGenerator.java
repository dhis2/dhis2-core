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

import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;

/**
 * Generates a SQL JOIN to join an enrollment or event with one ore more related entities, based on
 * the specified relationship type
 *
 * @author Luciano Fiandesio
 */
public class RelationshipTypeJoinGenerator {
  static final String RELATIONSHIP_JOIN = " WHERE rty.relationshiptypeid = ${relationshipid}";

  /**
   * Generate a sub query that joins an incoming Event/Enrollment/TEI UID to one or more related
   * entities, based on the selected relationship type
   *
   * @param alias the table alias to use for the main analytics table
   * @param relationshipType the type of relationship to fetch data for
   * @param programIndicatorType the type or Program Indicator that is used for this join
   *     (Enrollment or Event)
   * @return a SQL string containing the JOIN between analytics table and relationship
   */
  public static String generate(
      String alias, RelationshipType relationshipType, AnalyticsType programIndicatorType) {
    String sql =
        getFromRelationshipEntity(
            alias,
            relationshipType.getFromConstraint().getRelationshipEntity(),
            programIndicatorType);

    sql +=
        " LEFT JOIN relationship r on r.from_relationshipitemid = ri.relationshipitemid "
            + "LEFT JOIN relationshipitem ri2 on r.to_relationshipitemid = ri2.relationshipitemid "
            + "LEFT JOIN relationshiptype rty on rty.relationshiptypeid = r.relationshiptypeid ";

    sql += getToJoin(relationshipType.getToConstraint().getRelationshipEntity());

    sql +=
        addRelationshipWhereClause(
            relationshipType.getId(), relationshipType.getToConstraint().getRelationshipEntity());

    sql += ")";
    return sql;
  }

  private static String getToJoin(RelationshipEntity relationshipEntity) {
    String sql = "LEFT JOIN ";
    switch (relationshipEntity) {
      case TRACKED_ENTITY_INSTANCE:
        return sql
            + "trackedentityinstance tei on tei.trackedentityinstanceid = ri2.trackedentityinstanceid";
      case PROGRAM_STAGE_INSTANCE:
        return sql
            + "programstageinstance psi on psi.programstageinstanceid = ri2.programstageinstanceid";
      case PROGRAM_INSTANCE:
        return sql + "programinstance pi on pi.programinstanceid = ri2.programinstanceid";
      default:
        throw new IllegalQueryException(
            new ErrorMessage(ErrorCode.E7227, relationshipEntity.name()));
    }
  }

  private static String getFromRelationshipEntity(
      String alias, RelationshipEntity relationshipEntity, AnalyticsType programIndicatorType) {
    switch (relationshipEntity) {
      case TRACKED_ENTITY_INSTANCE:
        return getTei(alias);
      case PROGRAM_STAGE_INSTANCE:
      case PROGRAM_INSTANCE:
        return (programIndicatorType.equals(AnalyticsType.EVENT)
            ? getEvent(alias)
            : getEnrollment(alias));
    }
    throw new IllegalQueryException(new ErrorMessage(ErrorCode.E7227, relationshipEntity.name()));
  }

  private static String getTei(String alias) {
    return " "
        + alias
        + ".tei in (select tei.uid from trackedentityinstance tei"
        + " LEFT JOIN relationshipitem ri on tei.trackedentityinstanceid = ri.trackedentityinstanceid ";
  }

  private static String getEnrollment(String alias) {
    return " "
        + alias
        + ".pi in (select pi.uid from programinstance pi"
        + " LEFT JOIN relationshipitem ri on pi.programinstanceid = ri.programinstanceid ";
  }

  private static String getEvent(String alias) {
    return " "
        + alias
        + ".psi in (select psi.uid from programstageinstance psi"
        + " LEFT JOIN relationshipitem ri on psi.programstageinstanceid = ri.programstageinstanceid ";
  }

  private static String addRelationshipWhereClause(
      Long relationshipTypeId, RelationshipEntity relationshipEntity) {
    String sql =
        new StringSubstitutor(Map.of("relationshipid", relationshipTypeId))
            .replace(RELATIONSHIP_JOIN);

    sql += " AND ";

    switch (relationshipEntity) {
      case TRACKED_ENTITY_INSTANCE:
        return sql + "tei.uid = ax.tei ";
      case PROGRAM_STAGE_INSTANCE:
        return sql + "psi.uid = ax.psi ";
      case PROGRAM_INSTANCE:
        return sql + "pi.uid = ax.pi ";
      default:
        throw new IllegalQueryException(
            new ErrorMessage(ErrorCode.E7227, relationshipEntity.name()));
    }
  }
}
