/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.event.data.ou;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Pure SQL fragments used by ENROLLMENT_OU support. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrgUnitSqlFragments {

  /**
   * Builds the join predicate between the event analytics table and the enrollment analytics table.
   *
   * @param enrollmentAlias alias used for the enrollment analytics table in the current query
   * @return SQL join condition using quoted identifiers
   */
  public static String joinCondition(String enrollmentAlias) {
    return quotedColumn(
            OrgUnitSqlConstants.EVENT_TABLE_ALIAS, OrgUnitSqlConstants.ENROLLMENT_JOIN_COLUMN)
        + " = "
        + quotedColumn(enrollmentAlias, OrgUnitSqlConstants.ENROLLMENT_JOIN_COLUMN);
  }

  /**
   * Builds the legacy (string-based) inner join clause for enrollment OU resolution.
   *
   * @param enrollmentTableName program-specific enrollment analytics table name
   * @return full {@code inner join ... on ...} clause with trailing space
   */
  public static String innerJoinClause(String enrollmentTableName) {
    return "inner join "
        + enrollmentTableName
        + " as "
        + OrgUnitSqlConstants.ENROLLMENT_TABLE_ALIAS
        + " on "
        + joinCondition(OrgUnitSqlConstants.ENROLLMENT_TABLE_ALIAS)
        + " ";
  }

  /**
   * Builds the enrollment OU UID projection for select/group-by clauses.
   *
   * @param groupBy when true, returns a raw column reference suitable for group-by; when false,
   *     returns a projected alias suitable for select output
   * @return SQL fragment for enrollment OU UID
   */
  public static String selectEnrollmentOuUid(boolean groupBy) {
    String uidCol =
        quotedColumn(
            OrgUnitSqlConstants.ENROLLMENT_TABLE_ALIAS, OrgUnitSqlConstants.ENROLLMENT_OU_COLUMN);
    return groupBy ? uidCol : uidCol + " as " + OrgUnitSqlConstants.ENROLLMENT_OU_RESULT_ALIAS;
  }

  /**
   * Builds the enrollment OU display name projection used by event query output.
   *
   * @return SQL fragment mapping org unit name to the enrollment OU name alias
   */
  public static String selectEnrollmentOuName() {
    return quotedColumn(
            OrgUnitSqlConstants.ENROLLMENT_TABLE_ALIAS,
            OrgUnitSqlConstants.ENROLLMENT_OU_NAME_COLUMN)
        + " as "
        + OrgUnitSqlConstants.ENROLLMENT_OU_NAME_RESULT_ALIAS;
  }

  /**
   * Builds a literal enrollment OU UID projection for hierarchical mode. Produces a constant value
   * aliased as the enrollment OU column, collapsing all rows to one aggregated row.
   *
   * @param uid the org unit UID to use as a literal value
   * @return SQL fragment like {@code 'uid' as enrollmentou}
   */
  public static String selectLiteralEnrollmentOuUid(String uid) {
    return "'" + uid + "' as " + OrgUnitSqlConstants.ENROLLMENT_OU_RESULT_ALIAS;
  }

  /**
   * Builds a UID-membership predicate for enrollment OU.
   *
   * @param quotedUidList comma-delimited and quoted UID values
   * @return SQL predicate fragment for org unit UID filtering
   */
  public static String predicateByUids(String quotedUidList) {
    return " "
        + quotedColumn(
            OrgUnitSqlConstants.ENROLLMENT_TABLE_ALIAS, OrgUnitSqlConstants.ENROLLMENT_OU_COLUMN)
        + " in ("
        + quotedUidList
        + ") ";
  }

  /**
   * Builds a level-membership predicate for enrollment OU.
   *
   * @param commaSeparatedLevels comma-delimited level numbers
   * @return SQL predicate fragment for org unit level filtering
   */
  public static String predicateByLevels(String commaSeparatedLevels) {
    return " "
        + quotedColumn(
            OrgUnitSqlConstants.ENROLLMENT_TABLE_ALIAS,
            OrgUnitSqlConstants.ENROLLMENT_OU_LEVEL_COLUMN)
        + " in ("
        + commaSeparatedLevels
        + ") ";
  }

  /**
   * Builds a uidlevel-based predicate for enrollment OU hierarchical filtering.
   *
   * @param level the org unit hierarchy level
   * @param quotedUidList comma-delimited and quoted UID values
   * @return SQL predicate fragment for uidlevel filtering
   */
  public static String predicateByUidLevel(int level, String quotedUidList) {
    return quotedColumn(
            OrgUnitSqlConstants.ENROLLMENT_TABLE_ALIAS,
            OrgUnitSqlConstants.UID_LEVEL_PREFIX + level)
        + " in ("
        + quotedUidList
        + ")";
  }

  private static String quotedColumn(String alias, String column) {
    return alias + ".\"" + column + "\"";
  }
}
