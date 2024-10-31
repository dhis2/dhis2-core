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
package org.hisp.dhis.analytics.data.sql.from.strategy;

import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;

import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.data.sql.WhereClauseBuilder;
import org.hisp.dhis.analytics.data.sql.from.SubqueryColumnGenerator;
import org.hisp.dhis.db.sql.SqlBuilder;

/** Strategy for building min/max value subqueries */
public class MinMaxValue extends BaseSubqueryStrategy {
  private final SubqueryColumnGenerator subqueryColumnGenerator;
  private final WhereClauseBuilder whereClauseBuilder;

  public MinMaxValue(
      DataQueryParams params,
      SqlBuilder sqlBuilder,
      SubqueryColumnGenerator subqueryColumnGenerator,
      AnalyticsTableType tableType) {
    super(params, sqlBuilder);
    this.subqueryColumnGenerator = subqueryColumnGenerator;
    this.whereClauseBuilder = new WhereClauseBuilder(params, sqlBuilder, tableType);
  }

  @Override
  public String buildSubquery() {
    String dimensionColumns = subqueryColumnGenerator.getMinMaxDimensionColumns();
    String valueColumns = subqueryColumnGenerator.getMinMaxValueColumns();
    String fromSourceClause = getTablePartitionOrTableName() + " as " + ANALYTICS_TBL_ALIAS;
    String whereClause = whereClauseBuilder.build(sqlBuilder);

    return String.format(
        "(select %s, %s from %s %s group by %s)",
        dimensionColumns, valueColumns, fromSourceClause, whereClause, dimensionColumns);
  }
}
