/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.table;

import static org.hisp.dhis.commons.util.TextUtils.replace;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Helper class for applying aggregation level updates to analytics tables in both Apache Doris and
 * PostgreSQL.
 *
 * <p>This class provides utilities to update or nullify organization unit level columns (e.g.,
 * level1, level2, ...) in partitioned or non-partitioned analytics tables, using the appropriate
 * strategy for the underlying database:
 *
 * <ul>
 *   <li><b>Doris:</b> Uses an "Insert and Delete" strategy, which involves a staging table and
 *       partition-aware deletes.
 *   <li><b>PostgreSQL:</b> Uses a direct UPDATE SQL statement to set affected columns to NULL.
 * </ul>
 */
@Slf4j
public class AggregationLevelsHelper {

  private final JdbcTemplate jdbcTemplate;
  private final SqlBuilder sqlBuilder;

  public AggregationLevelsHelper(JdbcTemplate jdbcTemplate, SqlBuilder sqlBuilder) {
    this.jdbcTemplate = jdbcTemplate;
    this.sqlBuilder = sqlBuilder;
  }

  private String quote(String identifier) {
    return sqlBuilder.quote(identifier);
  }

  /** Returns a comma-separated, single-quoted string list for SQL IN clause. */
  private String quotedCommaDelimitedString(Collection<String> values) {
    return values.stream()
        .map(v -> "'" + v.replace("'", "''") + "'")
        .collect(Collectors.joining(", "));
  }

  /** Fetches all partition names from a Doris table, regardless of partition column. */
  private List<String> getAllPartitions(String tableName) {
    return jdbcTemplate.query(
        "show partitions from " + quote(tableName), (rs, rowNum) -> rs.getString("PartitionName"));
  }

  /** Fetches all column names from the target table. */
  private List<String> getColumnNames(Table table) {
    if (table.getColumns().isEmpty()) {
      Table parent = table.getParent();
      if (parent != null) {
        return parent.getColumns().stream().map(Column::getName).toList();
      }
    } else {
      return table.getColumns().stream().map(Column::getName).toList();
    }
    throw new IllegalQueryException("No columns found for table: " + table.getName());
  }

  /**
   * Applies aggregation levels to the given analytics table, setting certain organization unit
   * level columns (e.g., level1, level2, ...) to NULL for rows with a higher oulevel than the
   * specified aggregation level and matching data elements.
   *
   * <p>The actual strategy depends on the underlying database:
   *
   * <ul>
   *   <li>If {@code supportsUpdate} is true (PostgreSQL), an UPDATE statement is used.
   *   <li>If {@code supportsUpdate} is false (Doris), a staging table and an INSERT/DELETE approach
   *       is used.
   * </ul>
   *
   * @param table the analytics table to update
   * @param dataElements a collection of data element identifiers to match in the "dx" column
   * @param aggregationLevel the maximum oulevel to retain; higher levels will be nullified
   * @param supportsUpdate true for PostgreSQL (UPDATE), false for Doris (INSERT/DELETE)
   */
  public void applyAggregationLevels(
      Table table, Collection<String> dataElements, int aggregationLevel, boolean supportsUpdate) {
    if (dataElements.isEmpty()) {
      log.warn("No data elements provided for aggregation level update.");
      return;
    }
    if (supportsUpdate) {
      withUpdate(table, dataElements, aggregationLevel);
    } else {
      withInsertAndDelete(table, dataElements, aggregationLevel);
    }
  }

  /**
   * Performs aggregation level updates using the "Insert and Delete" strategy for Apache Doris.
   *
   * <p>This method is used when the underlying database is Doris, which does not support multi-row
   * UPDATE statements efficiently. It creates a staging table, inserts the updated rows (with
   * selected level columns set to NULL) into the staging table, deletes affected rows from the main
   * table (using partition-aware deletion if needed), and then reinserts the updated rows back into
   * the main table. The staging table is dropped at the end.
   *
   * <p>
   *
   * @param table the Doris table to update
   * @param dataElements a collection of data element identifiers to match in the "dx" column
   * @param aggregationLevel the maximum oulevel to retain; higher levels will be nullified
   */
  void withInsertAndDelete(Table table, Collection<String> dataElements, int aggregationLevel) {
    String mainTable = table.getName();
    String stagingTable = mainTable + "_staging_" + System.currentTimeMillis();

    // Get columns from the table
    List<String> columns = getColumnNames(table);

    // Create staging table
    String createStagingTableSql =
        String.format("create table %s like %s", quote(stagingTable), quote(mainTable));
    log.debug("Creating staging table: {}", createStagingTableSql);
    jdbcTemplate.execute(createStagingTableSql);

    // Build SELECT clause with NULL for appropriate levels
    StringBuilder selectClause = new StringBuilder();
    for (String col : columns) {
      if (col.startsWith(DataQueryParams.LEVEL_PREFIX)) {
        int levelNum = Integer.parseInt(col.substring(DataQueryParams.LEVEL_PREFIX.length()));
        if (levelNum <= aggregationLevel) {
          selectClause.append("null as ").append(quote(col));
        } else {
          selectClause.append(quote(col));
        }
      } else {
        selectClause.append(quote(col));
      }
      selectClause.append(", ");
    }
    selectClause.setLength(selectClause.length() - 2);

    String insertIntoStagingSql =
        String.format(
            "insert into %s (%s) select %s from %s where oulevel > %d and dx in (%s)",
            quote(stagingTable),
            columns.stream().map(this::quote).collect(Collectors.joining(", ")),
            selectClause,
            quote(mainTable),
            aggregationLevel,
            quotedCommaDelimitedString(dataElements));
    log.debug("Inserting into staging table: {}", insertIntoStagingSql);
    jdbcTemplate.execute(insertIntoStagingSql);

    // Delete from main table using all partitions (generic)
    List<String> partitions = getAllPartitions(mainTable);
    if (!partitions.isEmpty()) {
      String partitionList = partitions.stream().map(this::quote).collect(Collectors.joining(", "));
      String deleteSql =
          String.format(
              "delete from %s partition (%s) where oulevel > %d and dx in (%s)",
              quote(mainTable),
              partitionList,
              aggregationLevel,
              quotedCommaDelimitedString(dataElements));
      log.debug("Deleting from main table: {}", deleteSql);
      jdbcTemplate.execute(deleteSql);
    } else {
      // Table is not partitioned; just delete without PARTITION clause
      String deleteSql =
          String.format(
              "delete from %s where oulevel > %d and dx in (%s)",
              quote(mainTable), aggregationLevel, quotedCommaDelimitedString(dataElements));
      log.debug("Deleting from main table (no partitions): {}", deleteSql);
      jdbcTemplate.execute(deleteSql);
    }

    // Insert back from staging table
    String insertBackSql =
        String.format(
            "insert into %s (%s) select %s from %s",
            quote(mainTable),
            columns.stream().map(this::quote).collect(Collectors.joining(", ")),
            columns.stream().map(this::quote).collect(Collectors.joining(", ")),
            quote(stagingTable));
    log.debug("Inserting back into main table: {}", insertBackSql);
    jdbcTemplate.execute(insertBackSql);

    // Drop staging table
    String dropStagingSql = String.format("drop table %s", quote(stagingTable));
    log.debug("Dropping staging table: {}", dropStagingSql);
    jdbcTemplate.execute(dropStagingSql);
  }

  /**
   * Performs aggregation level updates using a direct UPDATE SQL statement for PostgreSQL.
   *
   * <p>This method is used when the underlying database is PostgreSQL, which supports efficient
   * multi-row UPDATE statements. It sets all organization unit level columns (e.g., level1, level2,
   * ...) up to the specified aggregation level to NULL for rows where oulevel is greater than the
   * aggregation level and the "dx" column matches one of the provided data elements.
   *
   * @param table the PostgreSQL table to update
   * @param dataElements a collection of data element identifiers to match in the "dx" column
   * @param aggregationLevel the maximum oulevel to retain; higher levels will be nullified
   */
  void withUpdate(Table table, Collection<String> dataElements, int aggregationLevel) {
    StringBuilder sql = new StringBuilder("update ${tableName} set ");

    for (int i = 0; i < aggregationLevel; i++) {
      int level = i + 1;

      String column = quote(DataQueryParams.LEVEL_PREFIX + level);

      sql.append(column).append(" = null,");
    }

    sql.deleteCharAt(sql.length() - ",".length()).append(" ");
    sql.append(
        """
                        where oulevel > ${aggregationLevel} \
                        and dx in ( ${dataElements} )\s""");

    String updateQuery =
        replace(
            sql.toString(),
            Map.of(
                "tableName", table.getName(),
                "aggregationLevel", String.valueOf(aggregationLevel),
                "dataElements", quotedCommaDelimitedString(dataElements)));

    log.debug("Aggregation level SQL: '{}'", updateQuery);
    jdbcTemplate.execute(updateQuery);
  }
}
