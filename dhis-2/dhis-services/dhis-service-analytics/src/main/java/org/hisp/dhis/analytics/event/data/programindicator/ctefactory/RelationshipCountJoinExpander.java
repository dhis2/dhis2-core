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
package org.hisp.dhis.analytics.event.data.programindicator.ctefactory;

import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.function.RelationshipCountPlaceholder;

/**
 * Expands {@code __D2RELCNT__(uid='...')__} placeholders inside processed Program Indicator SQL.
 *
 * <p>On engines that support correlated subqueries the placeholder is expanded back to the original
 * scalar subquery against {@code analytics_rs_relationship}. On engines that do not (e.g.
 * ClickHouse), one shared aggregated CTE is registered per distinct relationship-type UID and each
 * placeholder is replaced with a {@code value} reference against that CTE.
 */
public final class RelationshipCountJoinExpander {

  private RelationshipCountJoinExpander() {}

  /**
   * Expands every {@code __D2RELCNT__(uid='...')__} placeholder found in {@code sql}.
   *
   * <p>The placeholder is the alias-free token emitted by {@link
   * org.hisp.dhis.program.function.D2RelationshipCount} in place of an inline correlated subquery
   * against {@code analytics_rs_relationship}. The expansion strategy is dialect-driven:
   *
   * <ul>
   *   <li>When {@link SqlBuilder#supportsCorrelatedSubquery()} is {@code true} (e.g. PostgreSQL),
   *       the placeholder is restored to its original correlated subquery shape, qualified by
   *       {@code outerAlias}.
   *   <li>When it is {@code false} (e.g. ClickHouse), each distinct relationship-type UID is backed
   *       by a single aggregated CTE registered in {@code cteContext}, and the placeholder is
   *       rewritten into a {@code <alias>.value} reference that joins against that CTE.
   * </ul>
   *
   * <p>Returns {@code sql} unchanged when it contains no placeholder, including {@code null} and
   * blank inputs.
   *
   * @param sql processed Program Indicator SQL that may contain placeholders.
   * @param cteContext the CTE registry the join-form expansion registers shared CTEs into; ignored
   *     in the correlated-subquery branch.
   * @param outerAlias the alias of the enclosing analytics row source (e.g. {@code subax}); used to
   *     qualify {@code trackedentity} in the correlated-subquery branch.
   * @param sqlBuilder the dialect indicator that selects the expansion strategy.
   * @return SQL with every placeholder substituted.
   */
  public static String expand(
      String sql, CteContext cteContext, String outerAlias, SqlBuilder sqlBuilder) {
    if (StringUtils.isBlank(sql) || !sql.contains("__D2RELCNT__")) {
      return sql;
    }
    if (sqlBuilder.supportsCorrelatedSubquery()) {
      return RelationshipCountPlaceholder.expandCorrelated(sql, outerAlias);
    }
    return expandToJoin(sql, cteContext);
  }

  /**
   * Rewrites placeholders into {@code <alias>.value} references against shared per-UID CTEs.
   *
   * <p>One CTE is registered per distinct relationship-type UID encountered in {@code sql}: the
   * empty UID (any relationship type) maps to {@code relcnt_all}, a non-empty UID maps to {@code
   * relcnt_<uid>}. CTEs are deduplicated across multiple placeholders, multiple PIs, and the
   * expression vs. filter halves of one PI, so the aggregation runs at most once per UID per query.
   *
   * @param sql SQL containing one or more placeholders.
   * @param cteContext the CTE registry that receives the aggregated CTEs.
   * @return SQL with every placeholder replaced by the matching CTE alias's {@code value} column.
   */
  private static String expandToJoin(String sql, CteContext cteContext) {
    Matcher matcher = RelationshipCountPlaceholder.PATTERN.matcher(sql);
    StringBuilder out = new StringBuilder();
    while (matcher.find()) {
      String relationshipTypeUid = matcher.group(1);
      String key = cteKey(relationshipTypeUid);
      CteDefinition definition = cteContext.getDefinitionByKey(key);
      if (definition == null) {
        definition = CteDefinition.forRelationshipCount(key, aggregatedCteSql(relationshipTypeUid));
        cteContext.addRelationshipCountCte(key, definition);
      }
      matcher.appendReplacement(out, Matcher.quoteReplacement(definition.getAlias() + ".value"));
    }
    matcher.appendTail(out);
    return out.toString();
  }

  /**
   * Builds the stable CTE key used to deduplicate shared relationship-count CTEs across a query.
   *
   * <p>A blank UID (any relationship type) is normalised to {@code "all"} so the {@code "all"}
   * bucket and a typed-UID bucket never collide.
   *
   * @param relationshipTypeUid the relationship-type UID; may be {@code null} or blank.
   * @return {@code relcnt_all} when no UID is supplied, otherwise {@code relcnt_<uid>}.
   */
  private static String cteKey(String relationshipTypeUid) {
    return "relcnt_" + (StringUtils.isBlank(relationshipTypeUid) ? "all" : relationshipTypeUid);
  }

  /**
   * Renders the body of the shared aggregation CTE for a given relationship-type UID.
   *
   * <p>The CTE projects one row per {@code trackedentityid} with a {@code value} column holding the
   * summed {@code relationship_count}. When a UID is supplied the aggregation is pre-filtered to
   * that relationship type.
   *
   * @param relationshipTypeUid the relationship-type UID to filter by, or blank for any type.
   * @return the {@code SELECT ... FROM analytics_rs_relationship ... GROUP BY trackedentityid} body
   *     to register under {@link CteDefinition.CteType#D2_RELATIONSHIP_COUNT}.
   */
  private static String aggregatedCteSql(String relationshipTypeUid) {
    String where =
        StringUtils.isBlank(relationshipTypeUid)
            ? ""
            : " where relationshiptypeuid = '" + relationshipTypeUid + "'";
    return "select trackedentityid, sum(relationship_count) as value "
        + "from analytics_rs_relationship"
        + where
        + " group by trackedentityid";
  }
}
