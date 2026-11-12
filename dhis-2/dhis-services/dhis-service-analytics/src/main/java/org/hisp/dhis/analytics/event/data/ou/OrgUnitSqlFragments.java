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

import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.db.sql.SqlBuilder;

/** Pure SQL fragments used by the tracker org unit dimensions. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrgUnitSqlFragments {

  /**
   * Builds the join predicate between the analytics table and the org unit structure table.
   *
   * @param dimension the tracker org unit dimension
   * @param structAlias alias used for the org unit structure table in the current query
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return SQL join condition using quoted identifiers
   */
  public static String joinCondition(
      TrackerOrgUnitDimension dimension, String structAlias, SqlBuilder sqlBuilder) {
    return sqlBuilder.quote(structAlias, OrgUnitSqlConstants.STRUCT_UID_COLUMN)
        + " = "
        + sqlBuilder.quote(ANALYTICS_TBL_ALIAS, dimension.getAnalyticsColumn());
  }

  /**
   * Builds the string-based inner join clause to the org unit structure table. The join is inner
   * because the org unit columns of the event analytics table are never null.
   *
   * @param dimension the tracker org unit dimension
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return full {@code inner join ... on ...} clause with trailing space
   */
  public static String innerJoinClause(TrackerOrgUnitDimension dimension, SqlBuilder sqlBuilder) {
    return "inner join "
        + OrgUnitSqlConstants.STRUCT_TABLE
        + " as "
        + dimension.getStructAlias()
        + " on "
        + joinCondition(dimension, dimension.getStructAlias(), sqlBuilder)
        + " ";
  }

  /**
   * Builds a predicate matching org units at or below the given org units, by comparing the
   * ancestor UID held at their hierarchy level.
   *
   * @param dimension the tracker org unit dimension
   * @param level the org unit hierarchy level of the requested org units
   * @param quotedUidList comma-delimited and quoted UID values
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return SQL predicate fragment
   */
  public static String predicateByUidLevel(
      TrackerOrgUnitDimension dimension, int level, String quotedUidList, SqlBuilder sqlBuilder) {
    return uidLevelColumn(dimension, level, sqlBuilder) + " in (" + quotedUidList + ")";
  }

  /**
   * Builds the aggregate disaggregation column, which is the ancestor UID at the level of the
   * requested org units. This is what makes each requested org unit one output row aggregating its
   * whole subtree.
   *
   * @param dimension the tracker org unit dimension
   * @param level the org unit hierarchy level of the requested org units
   * @param groupBy when true returns a raw column reference for group-by, otherwise an aliased
   *     projection
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return SQL fragment
   */
  public static String selectUidLevel(
      TrackerOrgUnitDimension dimension, int level, boolean groupBy, SqlBuilder sqlBuilder) {
    String column = uidLevelColumn(dimension, level, sqlBuilder);

    return groupBy ? column : column + " as " + dimension.getUidHeader().getItem();
  }

  /**
   * Builds the org unit UID projection for query output. This is the org unit held by the event
   * row, not the requested ancestor.
   *
   * @param dimension the tracker org unit dimension
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return SQL fragment
   */
  public static String selectUid(TrackerOrgUnitDimension dimension, SqlBuilder sqlBuilder) {
    return sqlBuilder.quote(dimension.getStructAlias(), OrgUnitSqlConstants.STRUCT_UID_COLUMN)
        + " as "
        + dimension.getUidHeader().getItem();
  }

  /**
   * Builds the org unit display name projection for query output.
   *
   * @param dimension the tracker org unit dimension
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return SQL fragment
   */
  public static String selectName(TrackerOrgUnitDimension dimension, SqlBuilder sqlBuilder) {
    return sqlBuilder.quote(dimension.getStructAlias(), OrgUnitSqlConstants.STRUCT_NAME_COLUMN)
        + " as "
        + dimension.getNameHeader().getItem();
  }

  private static String uidLevelColumn(
      TrackerOrgUnitDimension dimension, int level, SqlBuilder sqlBuilder) {
    return sqlBuilder.quote(
        dimension.getStructAlias(), OrgUnitSqlConstants.UID_LEVEL_PREFIX + level);
  }

  /**
   * Builds the sort column for an output column of the dimension, reading it from the joined org
   * unit structure table.
   *
   * @param dimension the tracker org unit dimension
   * @param item the output column name to sort on
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return the qualified structure table column, or empty when the item is not an output column of
   *     the dimension
   */
  public static Optional<String> sortColumn(
      TrackerOrgUnitDimension dimension, String item, SqlBuilder sqlBuilder) {
    String column = null;

    if (dimension.getUidHeader().getItem().equals(item)) {
      column = OrgUnitSqlConstants.STRUCT_UID_COLUMN;
    } else if (dimension.getNameHeader().getItem().equals(item)) {
      column = OrgUnitSqlConstants.STRUCT_NAME_COLUMN;
    }

    return Optional.ofNullable(column)
        .map(col -> sqlBuilder.quote(dimension.getStructAlias(), col));
  }
}
