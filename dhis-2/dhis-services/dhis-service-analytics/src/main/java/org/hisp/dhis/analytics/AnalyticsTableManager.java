/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.analytics;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * Manager for the analytics database tables.
 *
 * @author Lars Helge Overland
 */
public interface AnalyticsTableManager
{
    String TABLE_TEMP_SUFFIX = "_temp";

    /**
     * Returns the {@link AnalyticsTableType} of analytics table which this
     * manager handles.
     *
     * @return type of analytics table.
     */
    AnalyticsTableType getAnalyticsTableType();

    /**
     * Returns a {@link AnalyticsTable} with a list of yearly
     * {@link AnalyticsTablePartition}.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @return the analytics table with partitions.
     */
    List<AnalyticsTable> getAnalyticsTables( AnalyticsTableUpdateParams params );

    /**
     * Returns a list of existing analytics database table names.
     *
     * @return a list of existing analytics database table names.
     */
    Set<String> getExistingDatabaseTables();

    /**
     * Checks if the database content is in valid state for analytics table
     * generation.
     *
     * @return null if valid, a descriptive string if invalid.
     */
    String validState();

    /**
     * Performs work before tables are being created.
     *
     * @param the {@link AnalyticsTableUpdateParams}.
     */
    void preCreateTables( AnalyticsTableUpdateParams params );

    /**
     * Removes updated and deleted data from tables for "latest" partition
     * update.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param tables the list of {@link AnalyticsTable}.
     */
    void removeUpdatedData( AnalyticsTableUpdateParams params, List<AnalyticsTable> tables );

    /**
     * Attempts to drop and then create analytics table.
     *
     * @param table the analytics table.
     */
    void createTable( AnalyticsTable table );

    /**
     * Creates single indexes on the given columns of the analytics table with
     * the given name.
     *
     * @param indexes the analytics indexes.
     * @return a future representing the asynchronous task.
     */
    Future<?> createIndexesAsync( ConcurrentLinkedQueue<AnalyticsIndex> indexes );

    /**
     * Attempts to drop the analytics table with partitions and rename the
     * temporary table with partitions as replacement.
     * <p>
     * If this is a partial update and the master table currently exists, the
     * master table is not swapped and instead the inheritance of the partitions
     * are set to the existing master table.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param table the analytics table.
     */
    void swapTable( AnalyticsTableUpdateParams params, AnalyticsTable table );

    /**
     * Copies and denormalizes rows from data value table into analytics table.
     * The data range is based on the start date of the data value row.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param tablePartitions the analytics table partitions.
     * @return a future representing the asynchronous task.
     */
    Future<?> populateTablesAsync( AnalyticsTableUpdateParams params,
        ConcurrentLinkedQueue<AnalyticsTablePartition> tablePartitions );

    /**
     * Invokes analytics table SQL hooks for the table type.
     *
     * @return the number of analytics table hooks being executed.
     */
    int invokeAnalyticsTableSqlHooks();

    /**
     * Drops the given {@link AnalyticsTable}.
     *
     * @param table the analytics table.
     */
    void dropTempTable( AnalyticsTable table );

    /**
     * Drops the given {@link AnalyticsTable}.
     *
     * @param tablePartition the analytics partition table.
     */
    public void dropTempTablePartition( AnalyticsTablePartition tablePartition );

    /**
     * Drops the given table.
     *
     * @param tableName the table name.
     */
    void dropTable( String tableName );

    /**
     * Drops the given table and all potential partitions.
     *
     * @param tableName the table name.
     */
    void dropTableCascade( String tableName );

    /**
     * Performs an analyze operation on the given table name.
     *
     * @param tableName the table name.
     */
    void analyzeTable( String tableName );

    /**
     * Applies aggregation level logic to the analytics table by setting the
     * organisation unit level column values to null for the levels above the
     * given aggregation level.
     *
     * @param partitions the analytics table partitions.
     * @param dataElements the data element identifiers to apply aggregation
     *        levels for.
     * @param aggregationLevel the aggregation level.
     * @return a future representing the asynchronous task.
     */
    Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTablePartition> partitions,
        Collection<String> dataElements, int aggregationLevel );

    /**
     * Performs vacuum or optimization of the given table. The type of operation
     * performed is dependent on the underlying DBMS.
     *
     * @param partitions the analytics table partitions.
     * @return a future representing the asynchronous task.
     */
    Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTablePartition> partitions );

    /**
     * Returns a list of non-dynamic {@link AnalyticsTableColumn}.
     *
     * @return a List of {@link AnalyticsTableColumn}.
     */
    List<AnalyticsTableColumn> getFixedColumns();
}
