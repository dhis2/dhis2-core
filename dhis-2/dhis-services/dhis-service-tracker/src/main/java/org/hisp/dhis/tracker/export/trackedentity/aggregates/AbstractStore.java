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
package org.hisp.dhis.tracker.export.trackedentity.aggregates;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.List;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.mapper.AbstractMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
abstract class AbstractStore {
  protected static final int PARITITION_SIZE = 20000;

  protected final NamedParameterJdbcTemplate jdbcTemplate;

  protected AbstractStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  MapSqlParameterSource createIdsParam(List<Long> ids) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("ids", ids);
    return parameters;
  }

  protected String applySortOrder(String sql, String sortOrderIds) {
    String trackedentityid = "trackedentityid";
    return "select * from ("
        + sql
        + ") as t JOIN unnest('{"
        + sortOrderIds
        + "}'::bigint[]) WITH ORDINALITY s("
        + trackedentityid
        + ", sortorder) USING ("
        + trackedentityid
        + ")ORDER  BY s.sortorder";
  }

  /**
   * Execute a SELECT statement and maps the results to the specified Mapper
   *
   * @param sql The SELECT statement to execute
   * @param handler the {@see RowCallbackHandler} to use for mapping a Resultset to an object
   * @param ids the list of primary keys mapped to the :ids parameter
   * @return a Multimap where the keys are of the same type as the specified {@see
   *     RowCallbackHandler}
   */
  protected <T> Multimap<String, T> fetch(String sql, AbstractMapper<T> handler, List<Long> ids) {
    List<List<Long>> idPartitions = Lists.partition(ids, PARITITION_SIZE);

    Multimap<String, T> multimap = ArrayListMultimap.create();

    idPartitions.forEach(partition -> multimap.putAll(fetchPartitioned(sql, handler, partition)));
    return multimap;
  }

  private <T> Multimap<String, T> fetchPartitioned(
      String sql, AbstractMapper<T> handler, List<Long> ids) {
    jdbcTemplate.query(sql, createIdsParam(ids), handler);
    return handler.getItems();
  }
}
