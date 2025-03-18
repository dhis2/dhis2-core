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
package org.hisp.dhis.analytics;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;

/**
 * Manager for the analytics database tables.
 *
 * @author Lars Helge Overland
 */
public interface AnalyticsTableManager {
  /**
   * Returns the {@link AnalyticsTableType} of analytics table which this manager handles.
   *
   * @return type of analytics table.
   */
  AnalyticsTableType getAnalyticsTableType();

  /**
   * Returns a {@link AnalyticsTable} with a list of yearly partitions.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @return the analytics table with partitions.
   */
  List<AnalyticsTable> getAnalyticsTables(AnalyticsTableUpdateParams params);

  /**
   * Returns a list of existing analytics database table names.
   *
   * @return a list of existing analytics database table names.
   */
  Set<String> getExistingDatabaseTables();

  /**
   * Checks if the database content is in valid state for analytics table generation.
   *
   * @return true if valid.
   */
  default boolean validState() {
    return true;
  }

  /**
   * Indicates whether data was created or updated for the given time range since last successful
   * "latest" table partition update.
   *
   * @param startDate the start date.
   * @param endDate the end date.
   * @return true if updated data exists, false if not.
   */
  default boolean hasUpdatedLatestData(Date startDate, Date endDate) {
    return false;
  }

  /**
   * Performs work before tables are being created.
   *
   * @param params {@link AnalyticsTableUpdateParams}.
   */
  void preCreateTables(AnalyticsTableUpdateParams params);

  /**
   * Removes updated and deleted data from the given tables for "latest" partition update.
   *
   * @param tables the list of {@link AnalyticsTable}.
   */
  default void removeUpdatedData(List<AnalyticsTable> tables) {}

  /**
   * Attempts to drop and then create analytics table.
   *
   * @param table the analytics table.
   */
  void createTable(AnalyticsTable table);

  /**
   * Creates single indexes on the given columns of the analytics table with the given name.
   *
   * @param index the index.
   */
  void createIndex(Index index);

  /**
   * Attempts to drop the analytics table with partitions and rename the staging table with
   * partitions as replacement. If this is a partial update and the master table currently exists,
   * the master table is not swapped and instead the inheritance of the partitions are set to the
   * existing master table.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param table the analytics table.
   */
  void swapTable(AnalyticsTableUpdateParams params, AnalyticsTable table);

  /**
   * Populates the analytics table.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param partition the {@link AnalyticsTablePartition}.
   */
  void populateTable(AnalyticsTableUpdateParams params, AnalyticsTablePartition partition);

  /**
   * Invokes analytics table SQL hooks for the table type.
   *
   * @return the number of analytics table hooks being executed.
   */
  int invokeAnalyticsTableSqlHooks();

  /**
   * Drops the given table.
   *
   * @param table the {@link Table}.
   */
  void dropTable(Table table);

  /**
   * Drops the given table and all potential partitions.
   *
   * @param name the table name.
   */
  void dropTable(String name);

  /**
   * Performs an analyze operation on the given table.
   *
   * @param name the table name.
   */
  void analyzeTable(String name);

  /**
   * Performs a vacuum operation on the given table.
   *
   * @param table the {@Table}.
   */
  void vacuumTable(Table table);

  /**
   * Performs an analyze operation on the given table.
   *
   * @param table the {@link Table}.
   */
  void analyzeTable(Table table);

  /**
   * Applies aggregation level logic to the given table.
   *
   * @param table the {@link Table}.
   * @param dataElements the data element identifiers to apply aggregation levels for.
   * @param aggregationLevel the aggregation level.
   */
  default void applyAggregationLevels(
      Table table, Collection<String> dataElements, int aggregationLevel) {}
}
