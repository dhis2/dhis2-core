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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.util.sql.SelectBuilder;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Orchestrates REGISTRATION_OU SQL clauses for query and aggregate paths. Applies to both event and
 * enrollment analytics, since the registration org unit column exists in both table types.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegistrationOuSqlCoordinator {

  private static final String DIMENSION_NAME = "REGISTRATION_OU";

  /**
   * Adds the org unit structure join to a {@link SelectBuilder} query when registration org unit is
   * used as a dimension or a filter.
   *
   * @param sb builder being assembled
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   */
  public static void addJoinIfNeeded(
      SelectBuilder sb, EventQueryParams params, SqlBuilder sqlBuilder) {
    if (!isNeeded(params)) {
      return;
    }

    sb.innerJoin(
        RegistrationOuSqlConstants.STRUCT_TABLE,
        RegistrationOuSqlConstants.STRUCT_ALIAS,
        alias -> RegistrationOuSqlFragments.joinCondition(alias, sqlBuilder));
  }

  /**
   * Appends the org unit structure join clause for string-based SQL generation.
   *
   * @param sql SQL buffer being assembled
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   */
  public static void appendLegacyJoin(
      StringBuilder sql, EventQueryParams params, SqlBuilder sqlBuilder) {
    if (isNeeded(params)) {
      sql.append(RegistrationOuSqlFragments.innerJoinClause(sqlBuilder));
    }
  }

  /**
   * Appends the registration org unit where conditions. Items are grouped by hierarchy level and
   * matched against the corresponding {@code uidlevel} column, which yields "at or below"
   * semantics. Within a dimension or filter the per-level predicates are OR-ed, because the
   * requested subtrees form a union. The dimension and the filter are AND-ed, because each
   * restricts the result independently.
   *
   * @param sql SQL buffer being assembled
   * @param hlp helper used to add {@code where/and} prefixes
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   */
  public static void appendWherePredicateIfNeeded(
      StringBuilder sql, SqlHelper hlp, EventQueryParams params, SqlBuilder sqlBuilder) {
    List<String> restrictions = new ArrayList<>();

    addRestriction(restrictions, params.getRegistrationOuDimensionItems(), sqlBuilder);
    addRestriction(restrictions, params.getRegistrationOuFilterItems(), sqlBuilder);

    if (restrictions.isEmpty()) {
      return;
    }

    sql.append(hlp.whereAnd()).append(" ").append(String.join(" and ", restrictions)).append(" ");
  }

  /**
   * Adds the REGISTRATION_OU select and group-by column for aggregate queries, producing one output
   * row per requested org unit, each aggregating its whole subtree.
   *
   * @param columns mutable output column list
   * @param params query parameters
   * @param isGroupBy whether the target list is used for group-by
   * @param isAggregated whether the query is in aggregated mode
   * @param sqlBuilder database-specific SQL builder for column quoting
   */
  public static void addDimensionSelectColumns(
      List<String> columns,
      EventQueryParams params,
      boolean isGroupBy,
      boolean isAggregated,
      SqlBuilder sqlBuilder) {
    if (!isAggregated || !params.hasRegistrationOuDimension()) {
      return;
    }

    List<OrganisationUnit> items = params.getRegistrationOuDimensionItems();

    if (items.isEmpty()) {
      return;
    }

    columns.add(
        RegistrationOuSqlFragments.selectUidLevel(singleLevelOf(items), isGroupBy, sqlBuilder));
  }

  /**
   * Adds the REGISTRATION_OU query output columns, being the UID and the name of the org unit the
   * tracked entity was registered in.
   *
   * @param columns mutable output column list
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   */
  public static void addQuerySelectColumns(
      List<String> columns, EventQueryParams params, SqlBuilder sqlBuilder) {
    if (!params.hasRegistrationOuDimension()) {
      return;
    }

    columns.add(RegistrationOuSqlFragments.selectRegistrationOuUid(sqlBuilder));
    columns.add(RegistrationOuSqlFragments.selectRegistrationOuName(sqlBuilder));
  }

  /**
   * True if the given projection is the registration OU column contributed by {@link
   * #addDimensionSelectColumns}. The enrollment aggregate base CTE strips table aliases from its
   * projections, which would turn this column into a bare {@code uidlevelN} that is ambiguous
   * between the analytics table and the joined org unit structure table, so it has to be recognised
   * and handled separately.
   */
  public static boolean isRegistrationOuColumn(String column) {
    return column != null && column.contains(RegistrationOuSqlConstants.STRUCT_ALIAS + ".");
  }

  /**
   * Adds the registration OU projection to the enrollment aggregate base CTE, qualified and aliased
   * as {@code registrationou} so the outer query can select and group by it off the CTE.
   *
   * @param sb builder for the base CTE
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   */
  public static void addBaseCteSelectColumn(
      SelectBuilder sb, EventQueryParams params, SqlBuilder sqlBuilder) {
    List<OrganisationUnit> items = params.getRegistrationOuDimensionItems();

    if (items.isEmpty()) {
      return;
    }

    sb.addColumn(
        RegistrationOuSqlFragments.selectUidLevel(singleLevelOf(items), false, sqlBuilder));
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private static boolean isNeeded(EventQueryParams params) {
    return params.hasRegistrationOuDimension() || params.hasRegistrationOuFilter();
  }

  /** Adds one parenthesised restriction covering all levels present in the given items. */
  private static void addRestriction(
      List<String> restrictions, List<OrganisationUnit> items, SqlBuilder sqlBuilder) {
    if (items.isEmpty()) {
      return;
    }

    String predicate =
        byLevel(items).entrySet().stream()
            .map(
                entry ->
                    RegistrationOuSqlFragments.predicateByUidLevel(
                        entry.getKey(), quotedUids(entry.getValue()), sqlBuilder))
            .collect(joining(" or "));

    restrictions.add("(" + predicate + ")");
  }

  /**
   * Returns the single hierarchy level shared by the given org units. One group-by column cannot
   * represent several levels at once, and an org unit below two requested ancestors at different
   * levels belongs to both, so a mixed-level set has no unambiguous disaggregation.
   */
  private static int singleLevelOf(List<OrganisationUnit> items) {
    Map<Integer, List<OrganisationUnit>> byLevel = byLevel(items);

    if (byLevel.size() > 1) {
      throwIllegalQueryEx(ErrorCode.E7261, DIMENSION_NAME);
    }

    return byLevel.keySet().iterator().next();
  }

  /** Groups by level in ascending order, so generated predicates are deterministic. */
  private static Map<Integer, List<OrganisationUnit>> byLevel(List<OrganisationUnit> items) {
    return items.stream().collect(groupingBy(OrganisationUnit::getLevel, TreeMap::new, toList()));
  }

  private static String quotedUids(List<OrganisationUnit> items) {
    return items.stream().map(item -> "'" + item.getUid() + "'").collect(joining(","));
  }
}
