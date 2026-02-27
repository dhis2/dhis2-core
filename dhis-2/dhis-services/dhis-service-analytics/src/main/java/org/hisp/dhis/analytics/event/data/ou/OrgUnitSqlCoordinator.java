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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;

/** Orchestrates ENROLLMENT_OU SQL clauses for query and aggregate paths. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrgUnitSqlCoordinator {

  /**
   * Adds the ENROLLMENT_OU join to a {@link SelectBuilder} query when enrollment OU is used as a
   * filter or dimension.
   *
   * @param sb builder being assembled
   * @param params query parameters
   */
  public static void addJoinIfNeeded(SelectBuilder sb, EventQueryParams params) {
    if (!params.hasEnrollmentOu()) {
      return;
    }

    String enrollmentTableName = enrollmentTableName(params);

    sb.innerJoin(
        enrollmentTableName,
        OrgUnitSqlConstants.ENROLLMENT_TABLE_ALIAS,
        OrgUnitSqlFragments::joinCondition);
  }

  /**
   * Appends the ENROLLMENT_OU legacy join clause for string-based SQL generation.
   *
   * @param sql SQL buffer being assembled
   * @param params query parameters
   */
  public static void appendLegacyJoin(StringBuilder sql, EventQueryParams params) {
    if (params.hasEnrollmentOu()) {
      sql.append(OrgUnitSqlFragments.innerJoinClause(enrollmentTableName(params)));
    }
  }

  /**
   * Adds ENROLLMENT_OU aggregate select/group-by columns when required.
   *
   * <p>This is only applicable for event analytics aggregate queries where ENROLLMENT_OU is a
   * dimension.
   *
   * @param columns mutable output column list
   * @param params query parameters
   * @param isGroupBy whether the target list is used for group-by
   * @param isAggregated whether the query is in aggregated mode
   * @param analyticsType current analytics type
   */
  public static void addDimensionSelectColumns(
      List<String> columns,
      EventQueryParams params,
      boolean isGroupBy,
      boolean isAggregated,
      AnalyticsType analyticsType) {
    // Enrollment OU is filter-only in aggregate queries: it contributes to the WHERE clause
    // (via appendWherePredicateIfNeeded) but must not appear in SELECT/GROUP BY, because
    // aggregate queries sum across all matching enrollment org units.
  }

  /**
   * Adds ENROLLMENT_OU query output columns (UID and name) for event query endpoint rows.
   *
   * @param columns mutable output column list
   * @param params query parameters
   */
  public static void addQuerySelectColumns(List<String> columns, EventQueryParams params) {
    if (!params.hasEnrollmentOuDimension()) {
      return;
    }

    columns.add(OrgUnitSqlFragments.selectEnrollmentOuUid(false));
    columns.add(OrgUnitSqlFragments.selectEnrollmentOuName());
  }

  /**
   * Appends ENROLLMENT_OU where conditions. UID items are grouped by org unit level and produce
   * {@code enrl."uidlevel{N}" in (...)} conditions joined with AND. Level constraints produce
   * {@code enrl."oulevel" in (...)} conditions. The two groups are combined with OR semantics.
   *
   * @param sql SQL buffer being assembled
   * @param hlp helper used to add {@code where/and} prefixes
   * @param params query parameters
   * @param sqlBuilder SQL dialect helper used for quoting UID lists
   */
  public static void appendWherePredicateIfNeeded(
      StringBuilder sql, SqlHelper hlp, EventQueryParams params, AnalyticsSqlBuilder sqlBuilder) {
    if (!params.hasEnrollmentOu()) {
      return;
    }

    List<String> predicates = new ArrayList<>();
    List<DimensionalItemObject> enrollmentOuItems = params.getAllEnrollmentOuItemsForSql();

    if (!enrollmentOuItems.isEmpty()) {
      String uidLevelClause = buildUidLevelClause(enrollmentOuItems);
      predicates.add(" " + uidLevelClause + " ");
    }

    if (!params.getAllEnrollmentOuLevelsForSql().isEmpty()) {
      String levels =
          params.getAllEnrollmentOuLevelsForSql().stream()
              .map(String::valueOf)
              .collect(joining(","));
      predicates.add(OrgUnitSqlFragments.predicateByLevels(levels));
    }

    if (!predicates.isEmpty()) {
      sql.append(hlp.whereAnd()).append(" (").append(String.join(" or ", predicates)).append(") ");
    }
  }

  /**
   * Groups org unit items by level and produces uidlevel-based IN conditions joined with AND.
   *
   * @param items org unit items (must be OrganisationUnit instances)
   * @return combined uidlevel predicates joined with " and "
   */
  private static String buildUidLevelClause(List<DimensionalItemObject> items) {
    Map<Integer, List<OrganisationUnit>> byLevel =
        items.stream()
            .map(item -> (OrganisationUnit) item)
            .collect(groupingBy(OrganisationUnit::getLevel));

    return byLevel.entrySet().stream()
        .map(
            entry -> {
              String uids =
                  entry.getValue().stream()
                      .map(ou -> "'" + ou.getUid() + "'")
                      .collect(joining(","));
              return OrgUnitSqlFragments.predicateByUidLevel(entry.getKey(), uids);
            })
        .collect(joining(" and "));
  }

  private static String enrollmentTableName(EventQueryParams params) {
    return AnalyticsTable.getTableName(AnalyticsTableType.ENROLLMENT, params.getProgram());
  }
}
