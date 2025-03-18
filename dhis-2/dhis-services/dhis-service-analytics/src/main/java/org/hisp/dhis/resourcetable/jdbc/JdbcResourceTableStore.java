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
package org.hisp.dhis.resourcetable.jdbc;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePhase;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableStore;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.system.util.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.resourcetable.ResourceTableStore")
public class JdbcResourceTableStore implements ResourceTableStore {

  private final AnalyticsTableHookService analyticsTableHookService;

  private final JdbcTemplate jdbcTemplate;

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  @Override
  public void generateResourceTable(ResourceTable resourceTable) {
    final Clock clock = new Clock().startClock();
    final Table stagingTable = resourceTable.getTable();
    final List<Index> indexes = resourceTable.getIndexes();
    final String tableName = Table.fromStaging(stagingTable.getName());
    final ResourceTableType tableType = resourceTable.getTableType();

    log.info("Generating resource table: '{}'", tableName);

    dropTable(stagingTable);

    createTable(stagingTable);

    populateTable(resourceTable, stagingTable);

    invokeTableHooks(tableType);

    createIndexes(indexes);

    analyzeTable(stagingTable);

    jdbcTemplate.execute(sqlBuilder.dropTableIfExists(tableName));

    jdbcTemplate.execute(sqlBuilder.renameTable(stagingTable, tableName));

    log.info("Resource table update done: '{}' '{}'", tableName, clock.time());
  }

  /**
   * Drops the given table.
   *
   * @param table the {@link Table}.
   */
  private void dropTable(Table table) {
    String sql = sqlBuilder.dropTableIfExists(table);
    log.debug("Drop table SQL: '{}'", sql);
    jdbcTemplate.execute(sql);
  }

  /**
   * Creates the given table.
   *
   * @param table the {@link Table}.
   */
  private void createTable(Table table) {
    String sql = sqlBuilder.createTable(table);
    log.debug("Create table SQL: '{}'", sql);
    jdbcTemplate.execute(sql);
  }

  /**
   * Populates the resource table.
   *
   * @param resourceTable the {@link ResourceTable}.
   * @param table the {@link Table}.
   */
  private void populateTable(ResourceTable resourceTable, Table table) {
    Optional<String> populateTableSql = resourceTable.getPopulateTempTableStatement();
    Optional<List<Object[]>> populateTableContent = resourceTable.getPopulateTempTableContent();

    if (populateTableSql.isPresent()) {
      log.debug("Populate table SQL: '{}'", populateTableSql.get());

      jdbcTemplate.execute(populateTableSql.get());
    } else if (populateTableContent.isPresent()) {
      List<Object[]> content = populateTableContent.get();
      log.debug("Populate table content rows: {}", content.size());

      if (isNotEmpty(content)) {
        int columns = content.get(0).length;
        batchUpdate(columns, table.getName(), content);
      }
    }
  }

  /**
   * Invokes table hooks.
   *
   * @param tableType the {@link ResourceTableType}.
   */
  private void invokeTableHooks(ResourceTableType tableType) {
    List<AnalyticsTableHook> hooks =
        analyticsTableHookService.getByPhaseAndResourceTableType(
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED, tableType);

    if (isNotEmpty(hooks)) {
      analyticsTableHookService.executeAnalyticsTableSqlHooks(hooks);

      log.info("Invoked resource table hooks: '{}'", hooks.size());
    }
  }

  /**
   * Creates indexes for the given table.
   *
   * @param indexes the list of {@link Index} to create.
   */
  private void createIndexes(List<Index> indexes) {
    if (isNotEmpty(indexes) && sqlBuilder.requiresIndexesForAnalytics()) {
      for (Index index : indexes) {
        jdbcTemplate.execute(sqlBuilder.createIndex(index));
      }
    }
  }

  /**
   * Analyzes the given table.
   *
   * @param table the {@link Table}.
   */
  private void analyzeTable(Table table) {
    if (sqlBuilder.supportsAnalyze()) {
      jdbcTemplate.execute(sqlBuilder.analyzeTable(table));
    }
  }

  /**
   * Performs a batch update.
   *
   * @param columns the number of columns in the table to update.
   * @param tableName the name of the table to update.
   * @param batchArgs the arguments to use for the update statement.
   */
  private void batchUpdate(int columns, String tableName, List<Object[]> batchArgs) {
    if (columns == 0 || StringUtils.isBlank(tableName)) {
      return;
    }

    StringBuilder sql = new StringBuilder("insert into " + tableName + " values (");

    for (int i = 0; i < columns; i++) {
      sql.append("?,");
    }

    removeLastComma(sql).append(")");

    jdbcTemplate.batchUpdate(sql.toString(), batchArgs);
  }
}
