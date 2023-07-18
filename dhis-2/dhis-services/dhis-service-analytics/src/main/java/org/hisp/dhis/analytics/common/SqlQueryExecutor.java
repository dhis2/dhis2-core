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
package org.hisp.dhis.analytics.common;

import static org.springframework.util.Assert.notNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.jsonextractor.AggregatedJsonExtractingSqlRowSet;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryCreator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

/**
 * @see org.hisp.dhis.analytics.common.QueryExecutor
 * @author maikel arabori
 */
@Component
public class SqlQueryExecutor implements QueryExecutor<SqlQuery, SqlQueryResult> {
  @Nonnull private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  public SqlQueryExecutor(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  /**
   * @throws IllegalArgumentException if the query argument is null.
   */
  @Override
  public SqlQueryResult find(SqlQueryCreator queryCreator) {
    notNull(queryCreator, "The 'query' must not be null");

    SqlQuery forSelect = queryCreator.createForSelect();

    SqlRowSet rowSet =
        namedParameterJdbcTemplate.queryForRowSet(
            forSelect.getStatement(), new MapSqlParameterSource().addValues(forSelect.getParams()));

    List<DimensionIdentifier<DimensionParam>> allDimensionIdentifiers =
        Stream.concat(
                queryCreator
                    .getQueryContext()
                    .getTeiQueryParams()
                    .getCommonParams()
                    .getDimensionIdentifiers()
                    .stream(),
                queryCreator
                    .getQueryContext()
                    .getTeiQueryParams()
                    .getCommonParams()
                    .getOrderParams()
                    .stream()
                    .map(AnalyticsSortingParams::getOrderBy))
            .toList();

    return new SqlQueryResult(
        new AggregatedJsonExtractingSqlRowSet(rowSet, allDimensionIdentifiers));
  }

  /**
   * @throws IllegalArgumentException if the query argument is null.
   */
  @Override
  public long count(SqlQueryCreator queryCreator) {
    notNull(queryCreator, "The 'query' must not be null");

    SqlQuery forCount = queryCreator.createForCount();

    return Optional.ofNullable(
            namedParameterJdbcTemplate.queryForObject(
                forCount.getStatement(),
                new MapSqlParameterSource().addValues(forCount.getParams()),
                Long.class))
        .orElse(0L);
  }
}
