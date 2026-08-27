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

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;

/**
 * Resolves event item SELECT columns while keeping dialect-specific item-column behavior isolated
 * from {@link JdbcEventAnalyticsManager}.
 */
@RequiredArgsConstructor
final class EventItemSelectColumnResolver {
  private final AnalyticsSqlBuilder sqlBuilder;

  private final OrganisationUnitResolver organisationUnitResolver;

  private final StageOuTableAliasResolver stageOuTableAliasResolver;

  private final ColumnResolver columnResolver;

  private final RowContextAppender rowContextAppender;

  List<String> resolve(EventQueryParams params, CteContext cteContext) {
    List<String> columns = new ArrayList<>();

    for (QueryItem queryItem : params.getItems()) {
      columns.addAll(resolveItem(params, cteContext, queryItem));
    }

    return columns;
  }

  private List<String> resolveItem(
      EventQueryParams params, CteContext cteContext, QueryItem queryItem) {
    if (isStageOuDimension(queryItem)) {
      return resolveStageOuColumns(params, queryItem);
    }

    if (!sqlBuilder.supportsCorrelatedSubquery()) {
      return resolveClickHouseItemColumns(params, cteContext, queryItem);
    }

    return resolveCorrelatedSubqueryItemColumns(params, cteContext, queryItem);
  }

  private boolean isStageOuDimension(QueryItem queryItem) {
    return ValueType.ORGANISATION_UNIT == queryItem.getValueType()
        && OrganisationUnitResolver.isStageOuDimension(queryItem);
  }

  private List<String> resolveStageOuColumns(EventQueryParams params, QueryItem queryItem) {
    List<String> columns = new ArrayList<>();
    String stageUid = queryItem.getProgramStage().getUid();
    String stageOuTableAlias = stageOuTableAliasResolver.resolve(params);
    OrganisationUnitResolver.StageOuCteContext stageOuContext =
        organisationUnitResolver.buildStageOuCteContext(queryItem, params, stageOuTableAlias);

    columns.add(stageOuContext.valueColumn() + " as " + sqlBuilder.quote(stageUid + ".ou"));
    addRequestedStageOuHeaderColumns(columns, params, stageUid, stageOuTableAlias);

    return columns;
  }

  private void addRequestedStageOuHeaderColumns(
      List<String> columns, EventQueryParams params, String stageUid, String stageOuTableAlias) {
    if (!params.hasHeaders()) {
      return;
    }

    if (params.getHeaders().contains(stageUid + ".ouname")) {
      columns.add(
          sqlBuilder.quote(stageOuTableAlias, EventAnalyticsColumnName.OU_NAME_COLUMN_NAME)
              + " as "
              + sqlBuilder.quote(stageUid + ".ouname"));
    }

    if (params.getHeaders().contains(stageUid + ".oucode")) {
      columns.add(
          sqlBuilder.quote(stageOuTableAlias, EventAnalyticsColumnName.OU_CODE_COLUMN_NAME)
              + " as "
              + sqlBuilder.quote(stageUid + ".oucode"));
    }
  }

  private List<String> resolveClickHouseItemColumns(
      EventQueryParams params, CteContext cteContext, QueryItem queryItem) {
    ColumnAndAlias columnAndAlias = columnResolver.resolve(queryItem, params);

    if (columnAndAlias == null) {
      return List.of();
    }

    if (queryItem.isProgramIndicator()) {
      CteDefinition cteDef = cteContext.getDefinitionByItemUid(columnAndAlias.alias);
      if (cteDef != null) {
        return List.of(getProgramIndicatorCteColumn(cteDef, columnAndAlias.alias));
      }
    }

    List<String> columns = new ArrayList<>();
    columns.add(columnAndAlias.asSql());
    rowContextAppender.append(columns, params, queryItem, columnAndAlias);
    return columns;
  }

  private String getProgramIndicatorCteColumn(CteDefinition cteDef, String alias) {
    return cteDef.isRequiresCoalesce()
        ? "coalesce(%s.value, 0) as %s".formatted(cteDef.getAlias(), alias)
        : "%s.value as %s".formatted(cteDef.getAlias(), alias);
  }

  private List<String> resolveCorrelatedSubqueryItemColumns(
      EventQueryParams params, CteContext cteContext, QueryItem queryItem) {
    List<String> columns = new ArrayList<>();
    ColumnAndAlias columnAndAlias = columnResolver.resolve(queryItem, params);

    if (columnAndAlias != null && !cteContext.containsCte(columnAndAlias.alias)) {
      columns.add(columnAndAlias.asSql());
    }

    rowContextAppender.append(columns, params, queryItem, columnAndAlias);
    return columns;
  }

  @FunctionalInterface
  interface ColumnResolver {
    ColumnAndAlias resolve(QueryItem queryItem, EventQueryParams params);
  }

  @FunctionalInterface
  interface RowContextAppender {
    void append(
        List<String> columns,
        EventQueryParams params,
        QueryItem queryItem,
        ColumnAndAlias columnAndAlias);
  }

  @FunctionalInterface
  interface StageOuTableAliasResolver {
    String resolve(EventQueryParams params);
  }
}
