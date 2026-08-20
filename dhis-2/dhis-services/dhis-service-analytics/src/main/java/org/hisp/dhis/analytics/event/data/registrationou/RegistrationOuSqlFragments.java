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
package org.hisp.dhis.analytics.event.data.registrationou;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.db.sql.SqlBuilder;

/** Pure SQL fragments used by REGISTRATION_OU support. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegistrationOuSqlFragments {

  /**
   * Builds the join predicate between the analytics table and the org unit structure table.
   *
   * @param structAlias alias used for the org unit structure table in the current query
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return SQL join condition using quoted identifiers
   */
  public static String joinCondition(String structAlias, SqlBuilder sqlBuilder) {
    return sqlBuilder.quote(structAlias, RegistrationOuSqlConstants.STRUCT_UID_COLUMN)
        + " = "
        + sqlBuilder.quote(
            RegistrationOuSqlConstants.ANALYTICS_TABLE_ALIAS,
            RegistrationOuSqlConstants.REGISTRATION_OU_COLUMN);
  }

  /**
   * Builds the string-based inner join clause for registration org unit resolution. The join is
   * inner because the registration org unit column is never null.
   *
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return full {@code inner join ... on ...} clause with trailing space
   */
  public static String innerJoinClause(SqlBuilder sqlBuilder) {
    return "inner join "
        + RegistrationOuSqlConstants.STRUCT_TABLE
        + " as "
        + RegistrationOuSqlConstants.STRUCT_ALIAS
        + " on "
        + joinCondition(RegistrationOuSqlConstants.STRUCT_ALIAS, sqlBuilder)
        + " ";
  }

  /**
   * Builds a predicate matching org units at or below the given org units, by comparing the
   * ancestor UID held at their hierarchy level.
   *
   * @param level the org unit hierarchy level of the requested org units
   * @param quotedUidList comma-delimited and quoted UID values
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return SQL predicate fragment
   */
  public static String predicateByUidLevel(int level, String quotedUidList, SqlBuilder sqlBuilder) {
    return sqlBuilder.quote(
            RegistrationOuSqlConstants.STRUCT_ALIAS,
            RegistrationOuSqlConstants.UID_LEVEL_PREFIX + level)
        + " in ("
        + quotedUidList
        + ")";
  }

  /**
   * Builds the aggregate disaggregation column, which is the ancestor UID at the level of the
   * requested org units. This is what makes each requested org unit one output row aggregating its
   * whole subtree.
   *
   * @param level the org unit hierarchy level of the requested org units
   * @param groupBy when true returns a raw column reference for group-by, otherwise an aliased
   *     projection
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return SQL fragment
   */
  public static String selectUidLevel(int level, boolean groupBy, SqlBuilder sqlBuilder) {
    String column =
        sqlBuilder.quote(
            RegistrationOuSqlConstants.STRUCT_ALIAS,
            RegistrationOuSqlConstants.UID_LEVEL_PREFIX + level);

    return groupBy
        ? column
        : column + " as " + RegistrationOuSqlConstants.REGISTRATION_OU_RESULT_ALIAS;
  }

  /**
   * Builds the registration org unit UID projection for query output. This is the org unit the
   * tracked entity was registered in, not the requested ancestor.
   *
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return SQL fragment
   */
  public static String selectRegistrationOuUid(SqlBuilder sqlBuilder) {
    return sqlBuilder.quote(
            RegistrationOuSqlConstants.STRUCT_ALIAS, RegistrationOuSqlConstants.STRUCT_UID_COLUMN)
        + " as "
        + RegistrationOuSqlConstants.REGISTRATION_OU_RESULT_ALIAS;
  }

  /**
   * Builds the registration org unit display name projection for query output.
   *
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return SQL fragment
   */
  public static String selectRegistrationOuName(SqlBuilder sqlBuilder) {
    return sqlBuilder.quote(
            RegistrationOuSqlConstants.STRUCT_ALIAS, RegistrationOuSqlConstants.STRUCT_NAME_COLUMN)
        + " as "
        + RegistrationOuSqlConstants.REGISTRATION_OU_NAME_RESULT_ALIAS;
  }
}
