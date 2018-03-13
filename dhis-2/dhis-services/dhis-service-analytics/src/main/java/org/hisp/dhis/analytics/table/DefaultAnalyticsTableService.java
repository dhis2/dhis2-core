package org.hisp.dhis.analytics.table;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.*;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * @author Lars Helge Overland
 */
public class DefaultAnalyticsTableService
    implements AnalyticsTableService
{
    private static final Log log = LogFactory.getLog( DefaultAnalyticsTableService.class );
    
    private AnalyticsTableManager tableManager;
    
    public void setTableManager( AnalyticsTableManager tableManager )
    {
        this.tableManager = tableManager;
    }

    @Autowired
    private OrganisationUnitService organisationUnitService;
    
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private ResourceTableService resourceTableService;
    
    @Autowired
    private Notifier notifier;
    
    @Autowired
    private SystemSettingManager systemSettingManager;

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

        int processNo = getProcessNo();
        int orgUnitLevelNo = organisationUnitService.getNumberOfOrganisationalLevels();
        
        AnalyticsTableType tableType = tableManager.getAnalyticsTableType();

        Date earliest = PartitionUtils.getStartDate( params.getLastYears() );
        
        Clock clock = new Clock( log )
            .startClock()
            .logTime( String.format( "Starting update: %s, processes: %d, org unit levels: %d", tableType.getTableName(), processNo, orgUnitLevelNo ) );
        
        String validState = tableManager.validState();

        if ( validState != null )
        {
            notifier.notify( jobId, validState );
            return;
        }

        final List<AnalyticsTable> tables = tableManager.getAnalyticsTables( earliest );
        
        if ( tables.isEmpty() )
        {
            clock.logTime( "Table updated aborted, no table or partitions found" );
            notifier.notify( jobId, "Table updated aborted, no table or partitions found" );
            return;
        }

        clock.logTime( "Table update start: " + tableType.getTableName() + ", earliest: " + earliest + ", parameters: " + params.toString() );
        notifier.notify( jobId, "Performing pre-create table work, org unit levels: " + orgUnitLevelNo );

        tableManager.preCreateTables();
        
        clock.logTime( "Performed pre-create table work" );
        notifier.notify( jobId, "Dropping temp tables" );

        dropTempTables( tables );

        clock.logTime( "Dropped temp tables" );
        notifier.notify( jobId, "Creating analytics tables" );

        createTables( tables, params.isSkipMasterTable() );
        
        clock.logTime( "Created analytics tables" );
        notifier.notify( jobId, "Populating analytics tables" );
        
        populateTables( tables );
        
        clock.logTime( "Populated analytics tables" );
        notifier.notify( jobId, "Invoking analytics table hooks" );
                
        tableManager.invokeAnalyticsTableSqlHooks();
        
        clock.logTime( "Invoked analytics table hooks" );
        notifier.notify( jobId, "Applying aggregation levels" );
        
        applyAggregationLevels( tables );
        
        clock.logTime( "Applied aggregation levels" );
        notifier.notify( jobId, "Creating indexes" );
        
        createIndexes( tables );
        
        clock.logTime( "Created indexes" );
        notifier.notify( jobId, "Analyzing analytics tables" );
        
        analyzeTables( tables );
        
        clock.logTime( "Analyzed tables" );
        notifier.notify( jobId, "Swapping analytics tables" );
        
        swapTables( tables, params.isSkipMasterTable() );
        
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
     * Drops the given temporary analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void dropTempTables( List<AnalyticsTable> tables )
    {
        tables.forEach( table -> tableManager.dropTempTable( table ) );
    }

    /**
     * Creates the given analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     * @param skipMasterTable whether to skip creating the master analytics table.
     */
    private void createTables( List<AnalyticsTable> tables, boolean skipMasterTable )
    {
        tables.forEach( table -> tableManager.createTable( table, skipMasterTable ) );
    }
    
    /**
     * Populates the given analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void populateTables( List<AnalyticsTable> tables )
    {
        List<AnalyticsTablePartition> partitions = PartitionUtils.getTablePartitions( tables );

        int taskNo = Math.min( getProcessNo(), partitions.size() );
        
        log.info( "Populate table task number: " + taskNo );
        
        ConcurrentLinkedQueue<AnalyticsTablePartition> partitionQ = new ConcurrentLinkedQueue<>( partitions );
        
        List<Future<?>> futures = new ArrayList<>();
        
        for ( int i = 0; i < taskNo; i++ )
        {
            futures.add( tableManager.populateTablesAsync( partitionQ ) );
        }
        
        ConcurrentUtils.waitForCompletion( futures );
    }
    
    /**
     * Applies aggregation levels to the given analytics tables.
     *
     * @param tables the list of {@link AnalyticsTable}.
     */
    private void applyAggregationLevels( List<AnalyticsTable> tables )
    {
        List<AnalyticsTablePartition> partitions = PartitionUtils.getTablePartitions( tables );

        int maxLevels = organisationUnitService.getNumberOfOrganisationalLevels();
        
        boolean hasAggLevels = false;
        
        levelLoop : for ( int i = 0; i < maxLevels; i++ )
        {
            int level = maxLevels - i;
            
            Collection<String> dataElements = IdentifiableObjectUtils.getUids( 
                dataElementService.getDataElementsByAggregationLevel( level ) );
            
            if ( dataElements.isEmpty() )
            {
                continue levelLoop;
            }

            hasAggLevels = true;
            
            ConcurrentLinkedQueue<AnalyticsTablePartition> partitionQ = new ConcurrentLinkedQueue<>( partitions );

            List<Future<?>> futures = new ArrayList<>();
            
            for ( int j = 0; j < getProcessNo(); j++ )
            {
                futures.add( tableManager.applyAggregationLevels( partitionQ, dataElements, level ) );
            }

            ConcurrentUtils.waitForCompletion( futures );
        }
        
        if ( hasAggLevels )
        {
            vacuumTables( tables );

            log.info( "Vacuumed tables" );
        }
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
                    indexes.add( new AnalyticsIndex( partition.getTempTableName(), col.getName(), col.getIndexType() ) );
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
     * @param tables the list of {@link AnalyticsTable}.
     * @param skipMasterTable whether to skip swapping the master analtyics table.
     */
    private void swapTables( List<AnalyticsTable> tables, boolean skipMasterTable )
    {
        resourceTableService.dropAllSqlViews();
        
        tables.forEach( table -> tableManager.swapTable( table, skipMasterTable ) );
        
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
        
        cores = ( cores == null || cores == 0 ) ? SystemUtils.getCpuCores() : cores;
                        
        return cores > 2 ? ( cores - 1 ) : cores;
    }
}
