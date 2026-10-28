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
package org.hisp.dhis.tablereplication.jdbc;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.tablereplication.TableReplicationStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Store for replication of operational database tables in the analytics database, if configured.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcTableReplicationStore implements TableReplicationStore {

  @Qualifier("analyticsJdbcTemplate")
  private final JdbcTemplate jdbcTemplate;

  @Qualifier("analyticsPostgresJdbcTemplate")
  private final JdbcTemplate postgresJdbcTemplate;

  private final SqlBuilder sqlBuilder;

  @Override
  public void replicateAnalyticsDatabaseTable(Table table) {
    final Clock clock = new Clock().startClock();
    final String tableName = table.getName();

    dropTable(table);
    createTable(table);
    replicateTable(table);

    log.info("Analytics database table replicated: '{}' '{}'", tableName, clock.time());
  }

  @Override
  public boolean tableExists(Table table) {
    return !jdbcTemplate.queryForList(sqlBuilder.tableExists(table)).isEmpty();
  }

  @Override
  public List<Long> getTrackedEntityIdsUpdatedSince(Date since, int limit) {
    String sql = "select trackedentityid from trackedentity where lastupdated >= ? limit ?";
    return postgresJdbcTemplate.queryForList(sql, Long.class, since, limit);
  }

  @Override
  public void deleteRows(Table table, String column, List<Long> ids) {
    if (ids.isEmpty()) {
      return;
    }

    String sql =
        String.format(
            "delete from %s where %s;", sqlBuilder.quote(table.getName()), inClause(column, ids));
    log.debug("Delete rows SQL: '{}'", sql);
    jdbcTemplate.execute(sql);
  }

  @Override
  public void replicateRows(Table table, String column, List<Long> ids) {
    if (ids.isEmpty()) {
      return;
    }

    String fromTable = sqlBuilder.qualifyTable(table.getName());
    String sql = sqlBuilder.insertIntoSelectFrom(table, fromTable, inClause(column, ids));
    log.debug("Replicate rows SQL: '{}'", sql);
    jdbcTemplate.execute(sql);
  }

  /**
   * Returns an in clause matching the given column against the given identifiers. The identifiers
   * are numbers, so writing them as literals is safe.
   *
   * @param column the column name.
   * @param ids the identifiers.
   * @return an in clause.
   */
  private String inClause(String column, List<Long> ids) {
    String values = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    return sqlBuilder.quote(column) + " in (" + values + ")";
  }

  /**
   * Drops the given analytics database table.
   *
   * @param table the {@link Table}.
   */
  private void dropTable(Table table) {
    String sql = sqlBuilder.dropTableIfExists(table);
    log.info("Drop table SQL: '{}'", sql);
    jdbcTemplate.execute(sql);
  }

  /**
   * Creates the given analytics database table.
   *
   * @param table the {@link Table}.
   */
  private void createTable(Table table) {
    String sql = sqlBuilder.createTable(table);
    log.info("Create table SQL: '{}'", sql);
    jdbcTemplate.execute(sql);
  }

  /**
   * Replicates the given table in the analytics database.
   *
   * @param table the {@link Table}.
   */
  private void replicateTable(Table table) {
    String fromTable = sqlBuilder.qualifyTable(table.getName());
    String sql = sqlBuilder.insertIntoSelectFrom(table, fromTable);
    log.info("Replicate table SQL: '{}'", sql);
    jdbcTemplate.execute(sql);
  }
}
