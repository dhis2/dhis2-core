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
package org.hisp.dhis.analytics.table;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.AnalyticsIndex;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.commons.util.ConcurrentUtils;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public class DefaultAnalyticsTableService
    implements AnalyticsTableService
{
    private AnalyticsTableManager tableManager;

    private OrganisationUnitService organisationUnitService;

    private DataElementService dataElementService;

    private ResourceTableService resourceTableService;

    private Notifier notifier;

    private SystemSettingManager systemSettingManager;

    public DefaultAnalyticsTableService( AnalyticsTableManager tableManager,
        OrganisationUnitService organisationUnitService, DataElementService dataElementService,
        ResourceTableService resourceTableService, Notifier notifier, SystemSettingManager systemSettingManager )
    {
        checkNotNull( tableManager );
        checkNotNull( organisationUnitService );
        checkNotNull( dataElementService );
        checkNotNull( resourceTableService );
        checkNotNull( notifier );
        checkNotNull( systemSettingManager );

        this.tableManager = tableManager;
        this.organisationUnitService = organisationUnitService;
        this.dataElementService = dataElementService;
        this.resourceTableService = resourceTableService;
        this.notifier = notifier;
        this.systemSettingManager = systemSettingManager;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return tableManager.getAnalyticsTableType();
    }

    @Override
    public void update( AnalyticsTableUpdateParams params )
    {
        JobConfiguration jobId = params.getJobId();

        final int processNo = getProcessNo();

        int tableUpdates = 0;

        log.info( String.format( "Analytics table update parameters: %s", params ) );

        AnalyticsTableType tableType = tableManager.getAnalyticsTableType();

        Clock clock = new Clock( log )
            .startClock()
            .logTime( String.format( "Starting update of type: %s, table name: '%s', processes: %d",
                getAnalyticsTableType(), tableType.getTableName(), processNo ) );

        String validState = tableManager.validState();

        if ( validState != null )
        {
            notifier.notify( jobId, validState );
            return;
        }

        final List<AnalyticsTable> tables = tableManager.getAnalyticsTables( params );

        if ( tables.isEmpty() )
        {
            clock.logTime( String.format( "Table update aborted, no table or partitions to be updated: '%s'",
                tableType.getTableName() ) );
            notifier.notify( jobId, "Table updated aborted, no table or partitions to be updated" );
            return;
        }

        clock.logTime( String.format( "Table update start: %s, earliest: %s, parameters: %s",
            tableType.getTableName(), getLongDateString( params.getFromDate() ), params.toString() ) );
        notifier.notify( jobId, "Performing pre-create table work" );

        tableManager.preCreateTables( params );

        clock.logTime( "Performed pre-create table work" );
        notifier.notify( jobId, "Dropping temp tables" );

        dropAllTempTables( tables );

        clock.logTime( "Dropped temp tables" );
        notifier.notify( jobId, "Creating analytics tables" );

        createTables( tables );

        clock.logTime( "Created analytics tables" );
        notifier.notify( jobId, "Populating analytics tables" );

        populateTables( params, tables );

        clock.logTime( "Populated analytics tables" );
        notifier.notify( jobId, "Invoking analytics table hooks" );

        tableUpdates += tableManager.invokeAnalyticsTableSqlHooks();

        clock.logTime( "Invoked analytics table hooks" );
        notifier.notify( jobId, "Applying aggregation levels" );

        tableUpdates += applyAggregationLevels( tables );

        clock.logTime( "Applied aggregation levels" );

        if ( tableUpdates > 0 )
        {
            notifier.notify( jobId, "Vacuuming tables" );
            vacuumTables( tables );
            clock.logTime( "Tables vacuumed" );
        }

        notifier.notify( jobId, "Creating indexes" );

        createIndexes( tables );

        clock.logTime( "Created indexes" );
        notifier.notify( jobId, "Analyzing analytics tables" );

        analyzeTables( tables );

        clock.logTime( "Analyzed tables" );
        notifier.notify( jobId, "Removing updated and deleted data" );

        tableManager.removeUpdatedData( params, tables );

        clock.logTime( "Removed updated and deleted data" );
        notifier.notify( jobId, "Swapping analytics tables" );

        swapTables( params, tables );

        clock.logTime( "Table update done: " + tableType.getTableName() );
        notifier.notify( jobId, "Table update done" );
    }

    @Override
    public void dropTables()
    {
        Set<String> tables = tableManager.getExistingDatabaseTables();

        tables.forEach( table -> tableManager.dropTableCascade( table ) );

        log.info( "Analytics tables dropped" );
    }

    @Override
    public void analyzeAnalyticsTables()
    {
        Set<String> tables = tableManager.getExistingDatabaseTables();

        tables.forEach( table -> tableManager.analyzeTable( table ) );

        log.info( "Analytics tables analyzed" );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Drops the given temporary analytics tables including partition tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void dropAllTempTables( final List<AnalyticsTable> tables )
    {
        dropTempTablesPartitions( tables );
        dropTempTables( tables );
    }

    /**
     * Drops the given temporary analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void dropTempTables( List<AnalyticsTable> tables )
    {
        tables.forEach( table -> tableManager.dropTempTable( table ) );
    }

    private void dropTempTablesPartitions( List<AnalyticsTable> tables )
    {
        tables.forEach( table -> table.getTablePartitions()
            .forEach( partitionTable -> tableManager.dropTempTablePartition( partitionTable ) ) );
    }

    /**
     * Creates the given analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void createTables( List<AnalyticsTable> tables )
    {
        tables.forEach( table -> tableManager.createTable( table ) );
    }

    /**
     * Populates the given analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void populateTables( AnalyticsTableUpdateParams params, List<AnalyticsTable> tables )
    {
        List<AnalyticsTablePartition> partitions = PartitionUtils.getTablePartitions( tables );

        int taskNo = Math.min( getProcessNo(), partitions.size() );

        log.info( "Populate table task number: " + taskNo );

        ConcurrentLinkedQueue<AnalyticsTablePartition> partitionQ = new ConcurrentLinkedQueue<>( partitions );

        List<Future<?>> futures = new ArrayList<>();

        for ( int i = 0; i < taskNo; i++ )
        {
            futures.add( tableManager.populateTablesAsync( params, partitionQ ) );
        }

        ConcurrentUtils.waitForCompletion( futures );
    }

    /**
     * Applies aggregation levels to the given analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     * @return the number of aggregation levels applied for data elements.
     */
    private int applyAggregationLevels( List<AnalyticsTable> tables )
    {
        List<AnalyticsTablePartition> partitions = PartitionUtils.getTablePartitions( tables );

        int maxLevels = organisationUnitService.getNumberOfOrganisationalLevels();

        int aggLevels = 0;

        levelLoop: for ( int i = 0; i < maxLevels; i++ )
        {
            int level = maxLevels - i;

            Collection<String> dataElements = IdentifiableObjectUtils.getUids(
                dataElementService.getDataElementsByAggregationLevel( level ) );

            if ( dataElements.isEmpty() )
            {
                continue levelLoop;
            }

            ConcurrentLinkedQueue<AnalyticsTablePartition> partitionQ = new ConcurrentLinkedQueue<>( partitions );

            List<Future<?>> futures = new ArrayList<>();

            for ( int j = 0; j < getProcessNo(); j++ )
            {
                futures.add( tableManager.applyAggregationLevels( partitionQ, dataElements, level ) );
            }

            aggLevels += dataElements.size();

            ConcurrentUtils.waitForCompletion( futures );
        }

        return aggLevels;
    }

    /**
     * Vacuums the given analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void vacuumTables( List<AnalyticsTable> tables )
    {
        List<AnalyticsTablePartition> partitions = PartitionUtils.getTablePartitions( tables );

        ConcurrentLinkedQueue<AnalyticsTablePartition> partitionQ = new ConcurrentLinkedQueue<>( partitions );

        List<Future<?>> futures = new ArrayList<>();

        for ( int i = 0; i < getProcessNo(); i++ )
        {
            tableManager.vacuumTablesAsync( partitionQ );
        }

        ConcurrentUtils.waitForCompletion( futures );
    }

    /**
     * Creates indexes on the given analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void createIndexes( List<AnalyticsTable> tables )
    {
        List<AnalyticsTablePartition> partitions = PartitionUtils.getTablePartitions( tables );

        ConcurrentLinkedQueue<AnalyticsIndex> indexes = new ConcurrentLinkedQueue<>();

        for ( AnalyticsTablePartition partition : partitions )
        {
            List<AnalyticsTableColumn> columns = partition.getMasterTable().getDimensionColumns();

            for ( AnalyticsTableColumn col : columns )
            {
                if ( !col.isSkipIndex() )
                {
                    List<String> indexColumns = col.hasIndexColumns() ? col.getIndexColumns()
                        : Lists.newArrayList( col.getName() );

                    indexes.add( new AnalyticsIndex( partition.getTempTableName(), indexColumns, col.getIndexType() ) );
                }
            }
        }

        log.info( "No of analytics table indexes: " + indexes.size() );

        List<Future<?>> futures = new ArrayList<>();

        for ( int i = 0; i < getProcessNo(); i++ )
        {
            futures.add( tableManager.createIndexesAsync( indexes ) );
        }

        ConcurrentUtils.waitForCompletion( futures );
    }

    /**
     * Analyzes the given analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void analyzeTables( List<AnalyticsTable> tables )
    {
        List<AnalyticsTablePartition> partitions = PartitionUtils.getTablePartitions( tables );

        partitions.forEach( table -> tableManager.analyzeTable( table.getTempTableName() ) );
    }

    /**
     * Swaps the given analytics tables.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void swapTables( AnalyticsTableUpdateParams params, List<AnalyticsTable> tables )
    {
        resourceTableService.dropAllSqlViews();

        tables.forEach( table -> tableManager.swapTable( params, table ) );

        resourceTableService.createAllSqlViews();
    }

    /**
     * Gets the number of available cores. Uses explicit number from system
     * setting if available. Detects number of cores from current server runtime
     * if not. Subtracts one to the number of cores if greater than two to allow
     * one core for general system operations.
     */
    private int getProcessNo()
    {
        Integer cores = (Integer) systemSettingManager.getSystemSetting( SettingKey.DATABASE_SERVER_CPUS );

        cores = (cores == null || cores == 0) ? SystemUtils.getCpuCores() : cores;

        return cores > 2 ? (cores - 1) : cores;
    }
}
