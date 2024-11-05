/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.data.sql.where;

import static org.hisp.dhis.analytics.data.sql.AnalyticsColumns.PEENDDATE;
import static org.hisp.dhis.analytics.data.sql.AnalyticsColumns.PESTARTDATE;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.db.sql.SqlBuilder;

@RequiredArgsConstructor
public class RestrictionsSqlClause implements SqlClauseAppender {
  private final DataQueryParams params;
  private final SqlBuilder sqlBuilder;
  private final AnalyticsTableType tableType;

  private static final String DATE_RESTRICTION_TEMPLATE =
      " (%s <= '%s' or %s is null) and (%s >= '%s' or %s is null)";

  private static final String PERIOD_RESTRICTION_TEMPLATE = " %s %s '%s' ";

  private static final String PARTITION_RESTRICTION_TEMPLATE = " %s in (%s) ";

  @Override
  public void appendTo(StringBuilder sql, SqlHelper sqlHelper) {
    appendOrgUnitRestrictions(sql, sqlHelper);
    appendCategoryOptionRestrictions(sql, sqlHelper);
    appendPeriodRestrictions(sql, sqlHelper);
    appendTimelyRestriction(sql, sqlHelper);
    appendPartitionsRestriction(sql, sqlHelper);
    appendPeriodRankRestriction(sql, sqlHelper);
  }

  private void appendOrgUnitRestrictions(StringBuilder sql, SqlHelper sqlHelper) {
    if (params.isRestrictByOrgUnitOpeningClosedDate() && params.hasStartEndDateRestriction()) {
      String openingDateCol = sqlBuilder.quoteAx("ouopeningdate");
      String closedDateCol = sqlBuilder.quoteAx("oucloseddate");
      String startDate = toMediumDate(params.getStartDateRestriction());
      String endDate = toMediumDate(params.getEndDateRestriction());

      String restriction =
          String.format(
              DATE_RESTRICTION_TEMPLATE,
              openingDateCol,
              startDate,
              openingDateCol,
              closedDateCol,
              endDate,
              closedDateCol);

      sql.append(sqlHelper.whereAnd()).append(" (").append(restriction).append(") ");
    }
  }

  private void appendCategoryOptionRestrictions(StringBuilder sql, SqlHelper sqlHelper) {
    if (params.isRestrictByCategoryOptionStartEndDate() && params.hasStartEndDateRestriction()) {
      String startDateCol = sqlBuilder.quoteAx("costartdate");
      String endDateCol = sqlBuilder.quoteAx("coenddate");
      String startDate = toMediumDate(params.getStartDateRestriction());
      String endDate = toMediumDate(params.getEndDateRestriction());

      String restriction =
          String.format(
              DATE_RESTRICTION_TEMPLATE,
              startDateCol,
              startDate,
              startDateCol,
              endDateCol,
              endDate,
              endDateCol);

      sql.append(sqlHelper.whereAnd()).append(" (").append(restriction).append(") ");
    }
  }

  private void appendPeriodRestrictions(StringBuilder sql, SqlHelper sqlHelper) {
    if (tableType.isPeriodDimension() && params.hasStartDate()) {
      String restriction =
          String.format(
              PERIOD_RESTRICTION_TEMPLATE,
              sqlBuilder.quoteAx(PESTARTDATE),
              ">=",
              toMediumDate(params.getStartDate()));

      sql.append(sqlHelper.whereAnd()).append(restriction);
    }

    if (tableType.isPeriodDimension() && params.hasEndDate()) {
      String restriction =
          String.format(
              PERIOD_RESTRICTION_TEMPLATE,
              sqlBuilder.quoteAx(PEENDDATE),
              "<=",
              toMediumDate(params.getEndDate()));

      sql.append(sqlHelper.whereAnd()).append(restriction);
    }
  }

  private void appendTimelyRestriction(StringBuilder sql, SqlHelper sqlHelper) {
    if (params.isTimely()) {
      sql.append(sqlHelper.whereAnd())
          .append(String.format(" %s = true ", sqlBuilder.quoteAx("timely")));
    }
  }

  private void appendPartitionsRestriction(StringBuilder sql, SqlHelper sqlHelper) {
    if (!params.isSkipPartitioning() && params.hasPartitions()) {
      String restriction =
          String.format(
              PARTITION_RESTRICTION_TEMPLATE,
              sqlBuilder.quoteAx("year"),
              TextUtils.getCommaDelimitedString(params.getPartitions().getPartitions()));

      sql.append(sqlHelper.whereAnd()).append(restriction);
    }
  }

  private void appendPeriodRankRestriction(StringBuilder sql, SqlHelper sqlHelper) {
    if (params.getAggregationType().isFirstOrLastOrLastInPeriodAggregationType()) {
      sql.append(sqlHelper.whereAnd())
          .append(String.format(" %s = 1 ", sqlBuilder.quoteAx("pe_rank")));
    }
  }
}
