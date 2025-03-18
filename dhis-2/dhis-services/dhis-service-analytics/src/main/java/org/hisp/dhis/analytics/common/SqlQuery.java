/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.common;

import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.notNull;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;

/**
 * @see org.hisp.dhis.analytics.common.Query
 * @author maikel arabori
 */
@EqualsAndHashCode
@Slf4j
public class SqlQuery implements Query {

  @Getter private final String statement;
  private final SqlQueryContext sqlQueryContext;

  /**
   * @throws IllegalArgumentException if statement or params are null/empty/blank.
   */
  private SqlQuery(String statement, SqlQueryContext sqlQueryContext) {
    hasText(statement, "The 'statement' must not be null/empty/blank");
    notNull(sqlQueryContext, "The 'params' must not be null");

    this.statement = statement;
    this.sqlQueryContext = sqlQueryContext;

    log.debug("STATEMENT: " + statement);
    log.debug("PARAMS: " + sqlQueryContext);
  }

  public static SqlQuery of(String render, QueryContext queryContext) {
    return new SqlQuery(
        render,
        new SqlQueryContext(
            queryContext.getParametersPlaceHolder(),
            queryContext.getContextParams().getCommonParsed().streamDimensions().toList()));
  }

  @Nonnull
  @Override
  public Map<String, Object> getParams() {
    return sqlQueryContext.getParams();
  }

  public List<DimensionIdentifier<DimensionParam>> getDimensionIdentifiers() {
    return sqlQueryContext.getDimensionIdentifiers();
  }

  @RequiredArgsConstructor
  @Getter
  private static class SqlQueryContext {
    private final Map<String, Object> params;
    private final List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers;
  }
}
