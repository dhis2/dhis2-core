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
package org.hisp.dhis.statistics.jdbc;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.statistics.StatisticsProvider")
public class JdbcStatisticsProvider implements StatisticsProvider {
  private final JdbcTemplate jdbcTemplate;

  // -------------------------------------------------------------------------
  // StatisticsProvider implementation
  // -------------------------------------------------------------------------

  @Override
  public Map<Objects, Long> getObjectCounts() {
    final Map<Objects, Long> map = new HashMap<>();

    // Metadata, use exact counts

    map.put(Objects.DATAELEMENT, query("select count(*) from dataelement;"));
    map.put(Objects.DATAELEMENTGROUP, query("select count(*) from dataelementgroup;"));
    map.put(Objects.INDICATORTYPE, query("select count(*) from indicatortype;"));
    map.put(Objects.INDICATOR, query("select count(*) from indicator;"));
    map.put(Objects.INDICATORGROUP, query("select count(*) from indicatorgroup;"));
    map.put(Objects.DATASET, query("select count(*) from dataset;"));
    map.put(Objects.ORGANISATIONUNIT, query("select count(*) from organisationunit;"));
    map.put(Objects.ORGANISATIONUNITGROUP, query("select count(*) from orgunitgroup;"));
    map.put(Objects.VALIDATIONRULE, query("select count(*) from validationrule;"));
    map.put(Objects.PROGRAM, query("select count(*) from program;"));
    map.put(Objects.PERIOD, query("select count(*) from period;"));
    map.put(Objects.USER, query("select count(*) from userinfo;"));
    map.put(Objects.USERGROUP, query("select count(*) from usergroup;"));
    map.put(Objects.VISUALIZATION, query("select count(*) from visualization;"));
    map.put(Objects.EVENTVISUALIZATION, query("select count(*) from eventvisualization;"));
    map.put(Objects.MAP, query("select count(*) from map;"));
    map.put(Objects.DASHBOARD, query("select count(*) from dashboard;"));

    // Data, use approximate counts

    map.put(Objects.DATAVALUE, approximateCount("datavalue"));

    map.put(Objects.TRACKEDENTITY, approximateCount("trackedentity"));
    map.put(Objects.ENROLLMENT, approximateCount("enrollment"));
    // TODO(DHIS2-19702): Calculate event count from separated tables
    map.put(Objects.EVENT, approximateCount("event"));

    return map;
  }

  /**
   * Returns the response of the given SQL query as a long value.
   *
   * @param sql the SQL query.
   * @return the response of the given SQL query as a long value.
   */
  private Long query(final String sql) {
    return jdbcTemplate.queryForObject(sql, Long.class);
  }

  /**
   * Returns the approximate count of rows in the given table.
   *
   * @param table the table name.
   * @return the approximate count of rows in the given table. If the table has no rows, 0 is
   *     returned instead of a negative value.
   */
  private Long approximateCount(final String table) {
    final String sql = "SELECT reltuples::bigint FROM pg_class WHERE relname = ?";
    Long result = jdbcTemplate.queryForObject(sql, Long.class, table);
    return Math.max(0L, result);
  }
}
