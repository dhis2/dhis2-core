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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.SqlBuilder;

@RequiredArgsConstructor
public class FilterWhereClause implements WhereClauseComponent {
  private final DataQueryParams params;
  private final SqlBuilder sqlBuilder;

  @Override
  public void appendTo(StringBuilder sql, SqlHelper sqlHelper) {
    ListMap<String, DimensionalObject> filterMap = params.getDimensionFilterMap();

    for (String dimension : filterMap.keySet()) {
      List<DimensionalObject> filters = filterMap.get(dimension);

      if (DimensionalObjectUtils.anyDimensionHasItems(filters)) {
        appendFilterGroup(sql, sqlHelper, filters);
      }
    }
  }

  /** Appends a group of filters for the same dimension connected with OR operators */
  private void appendFilterGroup(
      StringBuilder sql, SqlHelper sqlHelper, List<DimensionalObject> filters) {
    sql.append(sqlHelper.whereAnd()).append(" ( ");

    List<String> filterConditions = buildFilterConditions(filters);
    sql.append(String.join(" or ", filterConditions));

    sql.append(" ) ");
  }

  /** Builds individual filter conditions for each filter that has items */
  private List<String> buildFilterConditions(List<DimensionalObject> filters) {
    return filters.stream()
        .filter(DimensionalObject::hasItems)
        .map(this::buildSingleFilterCondition)
        .toList();
  }

  /** Builds a single filter condition using IN clause */
  private String buildSingleFilterCondition(DimensionalObject filter) {
    String columnName = sqlBuilder.quoteAx(filter.getDimensionName());
    String items = sqlBuilder.singleQuotedCommaDelimited(getUids(filter.getItems()));
    return columnName + " in (" + items + ") ";
  }
}
