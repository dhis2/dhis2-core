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
package org.hisp.dhis.analytics.data.sql;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.data.sql.where.DataApprovalSqlClause;
import org.hisp.dhis.analytics.data.sql.where.DimensionSqlClause;
import org.hisp.dhis.analytics.data.sql.where.FilterSqlClause;
import org.hisp.dhis.analytics.data.sql.where.RestrictionsSqlClause;
import org.hisp.dhis.analytics.data.sql.where.SqlClauseAppender;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.SqlBuilder;

@RequiredArgsConstructor
public class WhereClauseBuilder implements SqlClauseBuilder {
  private final List<SqlClauseAppender> components;

  public WhereClauseBuilder(
      DataQueryParams params, SqlBuilder sqlBuilder, AnalyticsTableType tableType) {
    this.components = initializeComponents(params, sqlBuilder, tableType);
  }

  private List<SqlClauseAppender> initializeComponents(
      DataQueryParams params, SqlBuilder sqlBuilder, AnalyticsTableType tableType) {
    return List.of(
        // Order matters! - dimensions and filters first, then approval rules, then restrictions
        new DimensionSqlClause(params, sqlBuilder),
        new FilterSqlClause(params, sqlBuilder),
        new DataApprovalSqlClause(params, sqlBuilder),
        new RestrictionsSqlClause(params, sqlBuilder, tableType));
  }

  @Override
  public String buildForPostgres() {
    StringBuilder sql = new StringBuilder();
    SqlHelper sqlHelper = new SqlHelper();
    components.forEach(component -> component.appendTo(sql, sqlHelper));
    return sql.toString();
  }
}
