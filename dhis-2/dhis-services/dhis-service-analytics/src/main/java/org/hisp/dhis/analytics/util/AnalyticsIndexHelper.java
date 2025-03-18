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
package org.hisp.dhis.analytics.util;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;

import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsConstants;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.IndexFunction;

/**
 * Helper class that encapsulates methods responsible for supporting the creation of analytics
 * indexes based on very specific needs.
 *
 * @author maikel arabori
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnalyticsIndexHelper {
  private static final String PREFIX_INDEX = "in_";

  /**
   * Returns a queue of analytics table indexes.
   *
   * @param partitions the list of {@link AnalyticsTablePartition}.
   * @return a list of {@link Index}
   */
  public static List<Index> getIndexes(List<AnalyticsTablePartition> partitions) {
    List<Index> indexes = new ArrayList<>();

    for (AnalyticsTablePartition partition : partitions) {
      AnalyticsTableType type = partition.getMasterTable().getTableType();
      List<AnalyticsTableColumn> dimensionColumns =
          partition.getMasterTable().getDimensionColumns();

      for (AnalyticsTableColumn col : dimensionColumns) {
        if (!col.isSkipIndex()) {
          String name = getIndexName(partition.getName(), col.getIndexColumns(), type);
          List<String> columns =
              col.hasIndexColumns() ? col.getIndexColumns() : List.of(col.getName());

          indexes.add(
              Index.builder()
                  .name(name)
                  .tableName(partition.getName())
                  .indexType(col.getIndexType())
                  .columns(columns)
                  .build());

          maybeAddTextLowerIndex(indexes, name, partition.getName(), col, columns);
          maybeAddDateSortOrderIndex(indexes, name, partition.getName(), col, columns);
        }
      }
    }

    return indexes;
  }

  /**
   * Returns non-quoted index name for column. Purpose of code suffix is to avoid uniqueness
   * collision between indexes for temporary and real tables.
   *
   * @param tableName the index table name.
   * @param columns the index column names.
   * @param tableType the {@link AnalyticsTableType}
   */
  static String getIndexName(String tableName, List<String> columns, AnalyticsTableType tableType) {
    String columnName = join(columns, "_");

    return PREFIX_INDEX
        + maybeShortenColumnName(columnName)
        + "_"
        + shortenTableName(tableName, tableType)
        + "_"
        + CodeGenerator.generateCode(5);
  }

  /**
   * If the conditions are met, the index name will be shorted. Only part in single parentheses will
   * be used.
   *
   * <p>Column data type is TEXT AND "indexColumns" has ONLY one element AND the column name is a
   * valid UID.
   *
   * @param columnName the column name to be used in the index name
   */
  private static String maybeShortenColumnName(String columnName) {
    // some analytics indexes for jsonb columns are using too long names
    // based on casting
    String shortenName = StringUtils.substringBetween(columnName, "'");

    return StringUtils.isEmpty(shortenName) ? columnName : shortenName;
  }

  /**
   * If the conditions are met, this method adds an index, that uses the "lower" function, into the
   * given list of "indexes". A new index will be added in the following rules are matched:
   *
   * <p>Column data type is TEXT AND "indexColumns" has ONLY one element AND the column name is a
   * valid UID.
   *
   * @param indexes the list of {@link Index}
   * @param indexName the name of the original index
   * @param tableName the table name of the index
   * @param column the {@link AnalyticsTableColumn}
   * @param indexColumns the columns to be used in the function
   */
  private static void maybeAddTextLowerIndex(
      List<Index> indexes,
      String indexName,
      String tableName,
      AnalyticsTableColumn column,
      List<String> indexColumns) {

    String columnName = RegExUtils.removeAll(column.getName(), "\"");
    boolean isSingleColumn = indexColumns.size() == 1;

    if (column.getDataType() == TEXT
        && !column.isStaticDimension()
        && isValidUid(columnName)
        && isSingleColumn) {
      String name = indexName + "_lower";
      indexes.add(
          Index.builder()
              .name(name)
              .tableName(tableName)
              .indexType(column.getIndexType())
              .columns(indexColumns)
              .function(IndexFunction.LOWER)
              .build());
    }
  }

  private static void maybeAddDateSortOrderIndex(
      List<Index> indexes,
      String indexName,
      String tableName,
      AnalyticsTableColumn column,
      List<String> indexColumns) {

    boolean isSingleColumn = indexColumns.size() == 1;

    if (column.getDataType() == TIMESTAMP && column.isStaticDimension() && isSingleColumn) {
      indexes.add(
          Index.builder()
              .name(indexName + "_desc")
              .tableName(tableName)
              .indexType(column.getIndexType())
              .columns(indexColumns)
              .sortOrder("desc nulls last")
              .build());
    }
  }

  /**
   * Shortens the given table name.
   *
   * @param table the table name
   * @param tableType the {@link AnalyticsTableType}
   */
  private static String shortenTableName(String table, AnalyticsTableType tableType) {
    table = table.replace(tableType.getTableName(), "ax");
    table = table.replace(AnalyticsConstants.ANALYTICS_TBL_TEMP_SUFFIX, EMPTY);

    return table;
  }
}
