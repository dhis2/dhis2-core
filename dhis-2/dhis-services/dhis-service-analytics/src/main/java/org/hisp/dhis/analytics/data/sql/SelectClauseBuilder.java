/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.data.sql;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.db.sql.SqlBuilder;

public class SelectClauseBuilder implements SqlClauseBuilder {

  private final DataQueryParams params;
  private final DimensionsUtils dimensionsUtils;

  public SelectClauseBuilder(DataQueryParams params, SqlBuilder sqlBuilder) {
    this.params = params;
    this.dimensionsUtils = new DimensionsUtils(sqlBuilder);
  }

  @Override
  public String buildForPostgres() {
    var dimensions =
        dimensionsUtils.getCommaDelimitedQuotedDimensionColumns(params.getDimensions());
    var valueClause =
        params.isAggregation() ? buildAggregateValueClause(params) : params.getValueColumn();
    return String.format("select %s, %s", dimensions, valueClause);
  }

  private String buildAggregateValueClause(DataQueryParams params) {
    String sql = "";

    if (params.isAggregation()) {
      sql += getAggregateValueColumn(params);
    } else {
      sql += params.getValueColumn();
    }

    return sql + " as value ";
  }

  /**
   * Returns an aggregate clause for the numeric value column.
   *
   * @param params the {@link DataQueryParams}.
   * @return a SQL numeric value column.
   */
  private String getAggregateValueColumn(DataQueryParams params) {
    return new AggregateValueColumnBuilder(params).build();
  }
}
