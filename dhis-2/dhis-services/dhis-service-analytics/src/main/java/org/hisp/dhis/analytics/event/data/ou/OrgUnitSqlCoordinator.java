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
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Orchestrates the SQL clauses of the tracker org unit dimensions for query and aggregate paths.
 * Applies to both event and enrollment analytics, since the org unit columns exist in both table
 * types; the enrollment endpoints only support REGISTRATION_OU.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrgUnitSqlCoordinator {

  /**
   * Adds the org unit structure join to a {@link SelectBuilder} query when the dimension is used as
   * a dimension or a filter.
   *
   * @param dimension the tracker org unit dimension
   * @param sb builder being assembled
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   */
  public static void addJoinIfNeeded(
      TrackerOrgUnitDimension dimension,
      SelectBuilder sb,
      EventQueryParams params,
      SqlBuilder sqlBuilder) {
    if (!dimension.isRequested(params)) {
      return;
    }

    sb.innerJoin(
        OrgUnitSqlConstants.STRUCT_TABLE,
        dimension.getStructAlias(),
        alias -> OrgUnitSqlFragments.joinCondition(dimension, alias, sqlBuilder));
  }

  /**
   * Returns the org unit structure join clause, or an empty string when the dimension is not used.
   *
   * @param dimension the tracker org unit dimension
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return {@code inner join ... on ...} clause, or an empty string
   */
  public static String joinClause(
      TrackerOrgUnitDimension dimension, EventQueryParams params, SqlBuilder sqlBuilder) {
    return dimension.isRequested(params)
        ? OrgUnitSqlFragments.innerJoinClause(dimension, sqlBuilder)
        : "";
  }

  /**
   * Returns the where conditions of the dimension. Items are grouped by hierarchy level and matched
   * against the corresponding {@code uidlevel} column, which yields "at or below" semantics. Within
   * a dimension or filter the per-level predicates are OR-ed, because the requested subtrees form a
   * union. The dimension and the filter are AND-ed, because each restricts the result
   * independently.
   *
   * @param dimension the tracker org unit dimension
   * @param params query parameters
   * @param hlp helper used to add {@code where/and} prefixes
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return where conditions, or an empty string when the dimension restricts nothing
   */
  public static String wherePredicate(
      TrackerOrgUnitDimension dimension,
      EventQueryParams params,
      SqlHelper hlp,
      SqlBuilder sqlBuilder) {
    List<String> restrictions = new ArrayList<>();

    addRestriction(restrictions, dimension, dimension.dimensionItems(params), sqlBuilder);
    addRestriction(restrictions, dimension, dimension.filterItems(params), sqlBuilder);

    if (restrictions.isEmpty()) {
      return "";
    }

    return hlp.whereAnd() + " " + String.join(" and ", restrictions) + " ";
  }

  /**
   * Returns the select or group-by column of the dimension for aggregate queries, producing one
   * output row per requested org unit, each aggregating its whole subtree.
   *
   * @param dimension the tracker org unit dimension
   * @param params query parameters
   * @param isGroupBy whether the column is destined for the group-by clause
   * @param isAggregated whether the query is in aggregated mode
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return the column, or empty when the query has no disaggregation by the dimension
   */
  public static Optional<String> dimensionSelectColumn(
      TrackerOrgUnitDimension dimension,
      EventQueryParams params,
      boolean isGroupBy,
      boolean isAggregated,
      SqlBuilder sqlBuilder) {
    if (!isAggregated || !dimension.hasAggregateColumn(params)) {
      return Optional.empty();
    }

    return Optional.of(
        OrgUnitSqlFragments.selectUidLevel(
            dimension,
            singleLevelOf(dimension, dimension.dimensionItems(params)),
            isGroupBy,
            sqlBuilder));
  }

  /**
   * Returns the query output columns of the dimension, being the UID and the name of the org unit
   * held by the event row.
   *
   * @param dimension the tracker org unit dimension
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return the UID and name projections, or an empty list when the dimension is absent
   */
  public static List<String> querySelectColumns(
      TrackerOrgUnitDimension dimension, EventQueryParams params, SqlBuilder sqlBuilder) {
    if (!dimension.isDimensionRequested(params)) {
      return List.of();
    }

    return List.of(
        OrgUnitSqlFragments.selectUid(dimension, sqlBuilder),
        OrgUnitSqlFragments.selectName(dimension, sqlBuilder));
  }

  /**
   * True if the given projection is the registration OU column contributed by {@link
   * #dimensionSelectColumn}. The enrollment aggregate base CTE strips table aliases from its
   * projections, which would turn this column into a bare {@code uidlevelN} that is ambiguous
   * between the analytics table and the joined org unit structure table, so it has to be recognised
   * and handled separately.
   */
  public static boolean isRegistrationOuColumn(String column) {
    return column != null
        && column.contains(TrackerOrgUnitDimension.REGISTRATION_OU.getStructAlias() + ".");
  }

  /**
   * Returns the registration OU projection for the enrollment aggregate base CTE, qualified and
   * aliased as {@code registrationou} so the outer query can select and group by it off the CTE.
   *
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return the projection, or empty when the dimension carries no org units
   */
  public static Optional<String> baseCteSelectColumn(
      EventQueryParams params, SqlBuilder sqlBuilder) {
    return dimensionSelectColumn(
        TrackerOrgUnitDimension.REGISTRATION_OU, params, false, true, sqlBuilder);
  }

  /**
   * Keeps the table qualifier on a {@code uidlevelN} projection instead of the alias-stripped form.
   * The org unit structure table joined for registration org unit also carries {@code uidlevelN}
   * columns, so a bare reference is ambiguous whenever the org unit dimension is itself
   * level-based. The CTE's output column name is unaffected, because a column reference is named
   * after the column rather than its qualifier.
   *
   * @param qualified the projection as produced by the dimension resolver, table alias included
   * @param stripped the same projection with its table alias removed
   * @return the qualified form for org unit level columns, otherwise the stripped form
   */
  public static String preserveQualifierIfAmbiguous(String qualified, String stripped) {
    String bare = stripped == null ? "" : stripped.replace("\"", "").trim();

    return bare.startsWith(OrgUnitSqlConstants.UID_LEVEL_PREFIX) ? qualified : stripped;
  }

  /**
   * Adds the org unit structure joins of every requested tracker org unit dimension.
   *
   * @param sb builder being assembled
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   */
  public static void addJoinIfNeeded(
      SelectBuilder sb, EventQueryParams params, SqlBuilder sqlBuilder) {
    for (TrackerOrgUnitDimension dimension : TrackerOrgUnitDimension.values()) {
      addJoinIfNeeded(dimension, sb, params, sqlBuilder);
    }
  }

  /**
   * Returns the org unit structure join clauses of every requested tracker org unit dimension.
   *
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return the {@code inner join ... on ...} clauses, or an empty string
   */
  public static String joinClause(EventQueryParams params, SqlBuilder sqlBuilder) {
    return Arrays.stream(TrackerOrgUnitDimension.values())
        .map(dimension -> joinClause(dimension, params, sqlBuilder))
        .collect(joining());
  }

  /**
   * Returns the where conditions of every tracker org unit dimension. The dimensions restrict
   * independently, so their conditions are AND-ed.
   *
   * @param params query parameters
   * @param hlp helper used to add {@code where/and} prefixes
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return where conditions, or an empty string when no dimension restricts anything
   */
  public static String wherePredicate(
      EventQueryParams params, SqlHelper hlp, SqlBuilder sqlBuilder) {
    return Arrays.stream(TrackerOrgUnitDimension.values())
        .map(dimension -> wherePredicate(dimension, params, hlp, sqlBuilder))
        .collect(joining());
  }

  /**
   * Returns the query output columns of every tracker org unit dimension.
   *
   * @param params query parameters
   * @param sqlBuilder database-specific SQL builder for column quoting
   * @return the UID and name projections, in dimension order
   */
  public static List<String> querySelectColumns(EventQueryParams params, SqlBuilder sqlBuilder) {
    return Arrays.stream(TrackerOrgUnitDimension.values())
        .flatMap(dimension -> querySelectColumns(dimension, params, sqlBuilder).stream())
        .toList();
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Adds one parenthesised restriction covering all levels present in the given items. */
  private static void addRestriction(
      List<String> restrictions,
      TrackerOrgUnitDimension dimension,
      List<OrganisationUnit> items,
      SqlBuilder sqlBuilder) {
    if (items.isEmpty()) {
      return;
    }

    String predicate =
        byLevel(items).entrySet().stream()
            .map(
                entry ->
                    OrgUnitSqlFragments.predicateByUidLevel(
                        dimension, entry.getKey(), quotedUids(entry.getValue()), sqlBuilder))
            .collect(joining(" or "));

    restrictions.add("(" + predicate + ")");
  }

  /**
   * Returns the single hierarchy level shared by the given org units. One group-by column cannot
   * represent several levels at once, and an org unit below two requested ancestors at different
   * levels belongs to both, so a mixed-level set has no unambiguous disaggregation.
   */
  private static int singleLevelOf(
      TrackerOrgUnitDimension dimension, List<OrganisationUnit> items) {
    Map<Integer, List<OrganisationUnit>> byLevel = byLevel(items);

    if (byLevel.size() > 1) {
      throwIllegalQueryEx(ErrorCode.E7261, dimension.getDimensionName());
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
