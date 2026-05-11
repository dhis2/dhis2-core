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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.event.EventStatus;
import org.locationtech.jts.util.Assert;
import org.springframework.stereotype.Component;

/**
 * Renders the inline subqueries that {@code JdbcEnrollmentAnalyticsManager} embeds in its outer
 * select to project a value or coordinate from an event linked to the enrollment in scope. All
 * three variants — repeatable-stage value, single-stage value, and coordinate (with optional {@code
 * ST_Centroid} for organisation-unit values) — share the same skeleton: parenthesised select
 * against {@code analytics_event_<programUid>}, joined by enrollment, ordered and limited by {@link
 * ProgramStageOffsetSqlBuilder}.
 */
@Component
@RequiredArgsConstructor
public class EnrollmentEventSubqueryBuilder {

  private static final String ANALYTICS_EVENT_TABLE_PREFIX = "analytics_event_";

  private static final String LIMIT_1 = "limit 1";

  private static final String IS_NOT_NULL = " is not null ";

  private final AnalyticsSqlBuilder sqlBuilder;

  private final ProgramStageOffsetSqlBuilder stageOffsetBuilder;

  /**
   * Renders the value subquery for a program-stage query item. Caller must ensure {@code
   * item.hasProgramStage()} before invoking — only the program-stage branches of {@code
   * JdbcEnrollmentAnalyticsManager.getColumn} delegate here.
   */
  public String renderValueSubquery(QueryItem item, String suffix) {
    assertProgram(item);
    String quotedColName = sqlBuilder.quote(item.getItemName() + suffix);
    String eventTableName = ANALYTICS_EVENT_TABLE_PREFIX + item.getProgram().getUid();

    if (item.getProgramStage().getRepeatable() && item.hasRepeatableStageParams()) {
      return repeatableStageSubquery(item, quotedColName, eventTableName);
    }
    return singleStageSubquery(item, quotedColName, eventTableName);
  }

  /**
   * Renders the coordinate subquery. Returns {@link ColumnAndAlias#EMPTY} when the item has no
   * associated program — matching the legacy {@code getCoordinateColumn} contract.
   */
  public ColumnAndAlias renderCoordinateSubquery(QueryItem item) {
    if (item.getProgram() == null) {
      return ColumnAndAlias.EMPTY;
    }

    String eventTableName = ANALYTICS_EVENT_TABLE_PREFIX + item.getProgram().getUid();
    String quotedColName = sqlBuilder.quote(item.getItemId());

    String psCondition = "";
    if (item.hasProgramStage()) {
      assertProgram(item);
      psCondition = "and ps = '" + item.getProgramStage().getUid() + "' ";
    }

    String stCentroidFunction =
        ValueType.ORGANISATION_UNIT == item.getValueType() ? "ST_Centroid" : "";
    String alias = getAlias(item).orElse(null);

    String selectExpr = coordinateExpression(quotedColName, stCentroidFunction);
    String suffixPredicates = "and " + quotedColName + IS_NOT_NULL + psCondition + " ";

    return ColumnAndAlias.ofColumnAndAlias(
        wrap(selectExpr, eventTableName, "", suffixPredicates, item.getProgramStageOffset()),
        alias);
  }

  private String repeatableStageSubquery(
      QueryItem item, String quotedColName, String eventTableName) {
    String prefixPredicates =
        eventTableName + ".eventstatus != '" + EventStatus.SCHEDULE + "' and ";
    String suffixPredicates =
        "and ps = '"
            + item.getProgramStage().getUid()
            + "' "
            + stageOffsetBuilder.executionDateFilter(
                item.getRepeatableStageParams().getStartDate(),
                item.getRepeatableStageParams().getEndDate());
    return wrap(
        quotedColName,
        eventTableName,
        prefixPredicates,
        suffixPredicates,
        item.getProgramStageOffset());
  }

  private String singleStageSubquery(QueryItem item, String quotedColName, String eventTableName) {
    String prefixPredicates =
        eventTableName + ".eventstatus != '" + EventStatus.SCHEDULE + "' and ";

    String alias = "";
    if (item.getItem().getDimensionItemType() == DATA_ELEMENT && item.getProgramStage() != null) {
      alias =
          " as "
              + sqlBuilder.quote(item.getProgramStage().getUid() + "." + item.getItem().getUid());
    }
    String selectExpr = quotedColName + alias;

    String suffixPredicates =
        "and "
            + quotedColName
            + IS_NOT_NULL
            + "and ps = '"
            + item.getProgramStage().getUid()
            + "' ";
    return wrap(
        selectExpr,
        eventTableName,
        prefixPredicates,
        suffixPredicates,
        item.getProgramStageOffset());
  }

  private String coordinateExpression(String quotedColName, String stCentroidFunction) {
    return "'[' || round(ST_X("
        + stCentroidFunction
        + "("
        + quotedColName
        + "))::numeric, 6) || ',' || round(ST_Y("
        + stCentroidFunction
        + "("
        + quotedColName
        + "))::numeric, 6) || ']' as "
        + quotedColName;
  }

  /**
   * Common subquery skeleton:
   *
   * <pre>
   * (select &lt;selectExpr&gt;
   *    from &lt;eventTable&gt;
   *   where &lt;prefixPredicates&gt;&lt;eventTable&gt;.enrollment = ax.enrollment &lt;suffixPredicates&gt;
   *         &lt;orderType&gt; &lt;offset&gt; limit 1 )
   * </pre>
   *
   * Spacing inside {@code prefixPredicates} and {@code suffixPredicates} is the caller's
   * responsibility — preserved as-is to keep the rendered SQL byte-identical to the legacy inline
   * concatenation.
   */
  private String wrap(
      String selectExpr,
      String eventTableName,
      String prefixPredicates,
      String suffixPredicates,
      int programStageOffset) {
    return "(select "
        + selectExpr
        + " from "
        + eventTableName
        + " where "
        + prefixPredicates
        + eventTableName
        + ".enrollment = "
        + ANALYTICS_TBL_ALIAS
        + ".enrollment "
        + suffixPredicates
        + stageOffsetBuilder.orderType(programStageOffset)
        + " "
        + stageOffsetBuilder.offsetClause(programStageOffset)
        + " "
        + LIMIT_1
        + " )";
  }

  private static void assertProgram(QueryItem item) {
    Assert.isTrue(
        item.hasProgram(),
        "Can not query item with program stage but no program:" + item.getItemName());
  }

  private static Optional<String> getAlias(QueryItem item) {
    return Optional.of(item)
        .filter(QueryItem::hasProgramStage)
        .filter(QueryItem::hasRepeatableStageParams)
        .map(QueryItem::getRepeatableStageParams)
        .map(RepeatableStageParams::getDimension);
  }
}
