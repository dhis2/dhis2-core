package org.hisp.dhis.analytics;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * @author Lars Helge Overland
 */
public interface AnalyticsTableManager
{
    public static final String TABLE_TEMP_SUFFIX = "_temp";
    public static final String ANALYTICS_TABLE_NAME = "analytics";
    public static final String COMPLETENESS_TABLE_NAME = "analytics_completeness";
    public static final String COMPLETENESS_TARGET_TABLE_NAME = "analytics_completenesstarget";
    public static final String ORGUNIT_TARGET_TABLE_NAME = "analytics_orgunittarget";
    public static final String EVENT_ANALYTICS_TABLE_NAME = "analytics_event";
    
    /**
     * Returns analytics tables which are yearly partitions.
     * 
     * @param earliest the start date for the first year to generate table partitions.
     * @param latest the end date for the last year to generate table partitions.
     */
    List<AnalyticsTable> getTables( Date earliest );
    
    /**
     * Returns all potential analytics tables which are yearly partitions.
     */
    List<AnalyticsTable> getAllTables();
    
    /**
     * Checks if the database content is in valid state for analytics table generation.
     * Returns null if valid, a descriptive string if invalid.
     */
    String validState();
    
    /**
     * Returns the base table name.
     */
    String getTableName();
    
    /**
     * Returns the temporary table name.
     */
    String getTempTableName();
    
    /**
     * Performs work before tables are being created.
     */
    void preCreateTables();
    
    /**
     * Attempts to drop and then create analytics table.
     * 
     * @param tableName the table name.
     */
    void createTable( AnalyticsTable table );
    
    /**
     * Creates single indexes on the given columns of the analytics table with
     * the given name.
     * 
     * @param indexes the analytics indexes.
     */
    Future<?> createIndexesAsync( ConcurrentLinkedQueue<AnalyticsIndex> indexes );
    
    /**
     * Attempts to drop analytics table, then rename temporary table to analytics
     * table.
     * 
     * @param table the analytics table.
     */
    void swapTable( AnalyticsTable table );
    
    /**
     * Copies and denormalizes rows from data value table into analytics table.
     * The data range is based on the start date of the data value row.
     * 
     * @param tables the analytics tables.
     */
    Future<?> populateTableAsync( ConcurrentLinkedQueue<AnalyticsTable> tables );    

    /**
     * Returns all years for which it exists data values.
     * 
     * @param earliest the earliest date to include as data year, null if no restriction.
     */
    List<Integer> getDataYears( Date earliest );
    
    /**
     * Drops the given table.
     * 
     * @param tableName the name of the table to drop.
     */
    void dropTable( String tableName );
    
    /**
     * Applies aggregation level logic to the analytics table by setting the
     * organisation unit level column values to null for the levels above the
     * given aggregation level.
     * 
     * @param tables the analytics tables.
     * @param dataElements the data element uids to apply aggregation levels for.
     * @param aggregationLevel the aggregation level.
     */
    Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTable> tables, Collection<String> dataElements, int aggregationLevel );
    
    /**
     * Performs vacuum or optimization of the given table. The type of operation
     * performed is dependent on the underlying DBMS.
     * 
     * @param tables the analytics tables.
     */
    Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTable> tables );
}
