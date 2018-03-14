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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsIndex;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTablePhase;
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
public abstract class AbstractJdbcTableManager
    implements AnalyticsTableManager
{
    protected static final Log log = LogFactory.getLog( JdbcAnalyticsTableManager.class );
    
    protected static final String DATE_REGEXP = "^\\d{4}-\\d{2}-\\d{2}(\\s|T)?(\\d{2}:\\d{2}:\\d{2})?$";

    public static final String PREFIX_ORGUNITGROUPSET = "ougs_";
    public static final String PREFIX_ORGUNITLEVEL = "uidlevel";
    public static final String PREFIX_INDEX = "in_";

    @Autowired
    protected IdentifiableObjectManager idObjectManager;
   
    @Autowired
    protected OrganisationUnitService organisationUnitService;
    
    @Autowired
    protected DataElementCategoryService categoryService;
    
    @Autowired
    protected SystemSettingManager systemSettingManager;
    
    @Autowired
    protected DataApprovalLevelService dataApprovalLevelService;
    
    @Autowired
    protected ResourceTableService resourceTableService;
    
    @Autowired
    private AnalyticsTableHookService tableHookService;
    
    @Autowired
    protected StatementBuilder statementBuilder;
    
    @Autowired
    protected PartitionManager partitionManager;
    
    @Autowired
    protected DatabaseInfo databaseInfo;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    /**
     * Override in order to perform work before tables are being generated.
     */
    @Override
    public void preCreateTables()
    {
    }

    @Override
    public void createTable( AnalyticsTable table, boolean skipMasterTable )
    {
        if ( !skipMasterTable )
        {
            createTempTable( table );
        }
        
        createTempTablePartitions( table );
    }
    
    @Override
    @Async
    public Future<?> createIndexesAsync( ConcurrentLinkedQueue<AnalyticsIndex> indexes )
    {
        taskLoop : while ( true )
        {
            AnalyticsIndex inx = indexes.poll();
            
            if ( inx == null )
            {
                break taskLoop;
            }
            
            final String indexName = getIndexName( inx );            
            final String indexType = inx.hasType() ? " using " + inx.getType() : "";
            
            final String sql = "create index " + indexName + " on " + inx.getTable() + indexType + " (" + inx.getColumn() + ")";
            
            log.debug( "Create index: " + indexName + " SQL: " + sql );
            
            jdbcTemplate.execute( sql );
            
            log.debug( "Created index: " + indexName );
        }
        
        return null;
    }
    
    @Override
    public void swapTable( AnalyticsTable table, boolean skipMasterTable )
    {
        for ( AnalyticsTablePartition partition : table.getPartitionTables() )
        {
            swapTable( partition.getTempTableName(), partition.getTableName() );
        }
        
        if ( !skipMasterTable )
        {
            swapTable( table.getTempTableName(), table.getTableName() );
        }
    }
    
    @Override
    public void dropTempTable( AnalyticsTable table )
    {
        table.getPartitionTables().stream().forEach( p -> dropTable( p.getTempTableName() ) );
        
        dropTableCascade( table.getTempTableName() );
    }
    
    @Override
    public void dropTable( String tableName )
    {        
        executeSilently( "drop table " + tableName );
    }

    @Override
    public void dropTableCascade( String tableName )
    {        
        executeSilently( "drop table " + tableName + " cascade" );
    }

    @Override
    public void analyzeTable( String tableName )
    {
        String sql = StringUtils.trimToEmpty( statementBuilder.getAnalyze( tableName ) );
        
        executeSilently( sql );
    }

    @Override
    @Async
    public Future<?> populateTablesAsync( ConcurrentLinkedQueue<AnalyticsTablePartition> partitions )
    {
        taskLoop: while ( true )
        {
            AnalyticsTablePartition partition = partitions.poll();

            if ( partition == null )
            {
                break taskLoop;
            }

            populateTable( partition );
        }

        return null;
    }
    
    @Override
    public void invokeAnalyticsTableSqlHooks()
    {
        AnalyticsTableType type = getAnalyticsTableType();        
        List<AnalyticsTableHook> hooks = tableHookService.getByPhaseAndAnalyticsTableType( AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED, type );
        tableHookService.executeAnalyticsTableSqlHooks( hooks );
    }
    
    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------
  
    /**
     * Returns a list of table checks (constraints) for the given analytics table 
     * partition.
     * 
     * @param partition the {@link AnalyticsTablePartition}.
     */
    protected abstract List<String> getPartitionChecks( AnalyticsTablePartition partition );
    
    /**
     * Populates the given analytics table.
     * 
     * @param table the analytics table to populate.
     */
    protected abstract void populateTable( AnalyticsTablePartition partition );

    // -------------------------------------------------------------------------
    // Protected supportive methods
    // -------------------------------------------------------------------------
  
    /**
     * Returns the analytics table name.
     */
    protected String getTableName()
    {
        return getAnalyticsTableType().getTableName();
    }
    
    /**
     * Quotes the given column name.
     * 
     * @param column the column name.
     */
    protected String quote( String column )
    {
        return statementBuilder.columnQuote( column );
    }
    
    /**
     * Remove quotes from the given column name.
     * 
     * @param column the column name.
     */
    private String removeQuote( String column )
    {
        return column != null ? column.replaceAll( statementBuilder.getColumnQuote(), StringUtils.EMPTY ) : null;
    }
    
    /**
     * Shortens the given table name.
     * 
     * @param table the table name.
     */
    private String shortenTableName( String table )
    {
        table = table.replaceAll( getAnalyticsTableType().getTableName(), "ax" );
        table = table.replaceAll( TABLE_TEMP_SUFFIX, StringUtils.EMPTY );
        
        return table;
    }
    
    /**
     * Returns index name for column. Purpose of code suffix is to avoid uniqueness
     * collision between indexes for temporary and real tables.
     * 
     * @param inx the {@link AnalyticsIndex}.
     */
    protected String getIndexName( AnalyticsIndex inx )
    {
        return quote( PREFIX_INDEX + removeQuote( inx.getColumn() ) + "_" + shortenTableName( inx.getTable() ) + "_" + CodeGenerator.generateCode( 5 ) );        
    }
    
    /**
     * Indicates whether the given table exists and has at least one row.
     * 
     * @param tableName the table name.
     */
    protected boolean hasRows( String tableName )
    {
        final String sql = "select * from " + tableName + " limit 1";
        
        try
        {
            return jdbcTemplate.queryForRowSet( sql ).next();
        }
        catch ( BadSqlGrammarException ex )
        {
            return false;
        }
    }
    
    /**
     * Executes a SQL statement. Ignores existing tables/indexes when attempting
     * to create new.
     * 
     * @param sql the SQL statement.
     */
    protected void executeSilently( String sql )
    {
        try
        {            
            jdbcTemplate.execute( sql );
        }
        catch ( BadSqlGrammarException ex )
        {
            log.debug( ex.getMessage() );
        }
    }

    /**
     * Drops and creates the given analytics table.
     * 
     * @param table the {@link AnalyticsTable}.
     */
    protected void createTempTable( AnalyticsTable table )
    {
        validateDimensionColumns( table.getDimensionColumns() );
        
        final String tableName = table.getTempTableName();        

        String sqlCreate = "create table " + tableName + " (";

        for ( AnalyticsTableColumn col : ListUtils.union( table.getDimensionColumns(), table.getValueColumns() ) )
        {
            sqlCreate += col.getName() + " " + col.getDataType() + ",";
        }

        sqlCreate = TextUtils.removeLastComma( sqlCreate ) + ")";
        
        log.info( String.format( "Creating table: %s, columns: %d", tableName, table.getDimensionColumns().size() ) );

        log.debug( "Create SQL: " + sqlCreate );

        jdbcTemplate.execute( sqlCreate );
    }
    
    /**
     * Drops and creates the table partitions for the given analytics table.
     * 
     * @param table the {@link AnalyticsTable}.
     */
    protected void createTempTablePartitions( AnalyticsTable table )
    {
        for ( AnalyticsTablePartition partition : table.getPartitionTables() )
        {         
            final String tableName = partition.getTempTableName();
            final List<String> checks = getPartitionChecks( partition );
            
            String sqlCreate = "create table " + tableName + " ";
            
            if ( !checks.isEmpty() )
            {
                StringBuilder sqlCheck = new StringBuilder( "(" );
                checks.stream().forEach( check -> sqlCheck.append( "check (" + check + "), " ) );
                sqlCreate += TextUtils.removeLastComma( sqlCheck.toString() ) + ") ";
            }
            
            sqlCreate += "inherits (" + table.getTempTableName() + ")";
            
            log.info( String.format( "Creating partition table: %s", tableName ) );

            log.debug( "Create SQL: " + sqlCreate );

            jdbcTemplate.execute( sqlCreate );
        }
    }
    
    /**
     * Generates a list of {@link AnalyticsTable} based on a list of years with data.
     * 
     * @param dataYears the list of years with data.
     * @param dimensionColumns the list of dimension {@link AnalyticsTableColumn}.
     * @param valueColumns the list of value {@link AnalyticsTableColumn}.
     */
    protected AnalyticsTable getAnalyticsTable( List<Integer> dataYears, List<AnalyticsTableColumn> dimensionColumns, List<AnalyticsTableColumn> valueColumns )
    {        
        Calendar calendar = PeriodType.getCalendar();

        Collections.sort( dataYears );
        
        String baseName = getAnalyticsTableType().getTableName();
        
        AnalyticsTable table = new AnalyticsTable( baseName, dimensionColumns, valueColumns );
        
        for ( Integer year : dataYears )
        {            
            table.addPartitionTable( year, PartitionUtils.getStartDate( calendar, year ), PartitionUtils.getEndDate( calendar, year ) );
        }

        return table;
    }
    
    /**
     * Checks whether the given list of columns are valid.
     * 
     * @param columns the list of {@link AnalyticsTableColumn}.
     * @throws IllegalStateException if not valid.
     */
    protected void validateDimensionColumns( List<AnalyticsTableColumn> columns )
    {        
        if ( columns == null || columns.isEmpty() )
        {
            throw new IllegalStateException( "Analytics table dimensions are empty" );
        }
                
        List<String> columnNames = columns.stream().map( d -> d.getName() ).collect( Collectors.toList() );
                
        Set<String> duplicates = ListUtils.getDuplicates( columnNames );
        
        if ( !duplicates.isEmpty() )
        {
            throw new IllegalStateException( "Analytics table dimensions contain duplicates: " + duplicates );
        }
    }
    
    /**
     * Filters out analytics table columns which were created
     * after the time of the last successful resource table update.
     * 
     * @param columns the analytics table columns.
     * @return a list of {@link AnalyticsTableColumn}.
     */
    protected List<AnalyticsTableColumn> filterDimensionColumns( List<AnalyticsTableColumn> columns )
    {
        Date lastResourceTableUpdate = (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE );
        
        if ( lastResourceTableUpdate == null )
        {
            return columns;
        }
        
        return columns.stream()
            .filter( c -> c.getCreated() == null || c.getCreated().before( lastResourceTableUpdate ) )
            .collect( Collectors.toList() );
    }

    /**
     * Executes the given table population SQL statement, log and times the operation.
     * 
     * @param sql the SQL statement.
     * @param tableName the table name.
     */
    protected void populateAndLog( String sql, String tableName )
    {
        log.debug( String.format( "Populate table: %s with SQL: ", tableName, sql ) );

        Timer timer = new SystemTimer().start();

        jdbcTemplate.execute( sql );
        
        log.info( String.format( "Populated table in %s: %s", timer.stop().toString(), tableName ) );
    }

    // -------------------------------------------------------------------------
    // Private supportive methods
    // -------------------------------------------------------------------------

    /**
     * Swaps a database table, meaning drops the real table and renames the
     * temporary table to become the real table.
     * 
     * @param tempTableName the temporary table name.
     * @param realTableName the real table name.
     */
    private void swapTable( String tempTableName, String realTableName )
    {        
        final String sqlDrop = "drop table " + realTableName;
        
        executeSilently( sqlDrop );
        
        final String sqlAlter = "alter table " + tempTableName + " rename to " + realTableName;
        
        executeSilently( sqlAlter );
    }
}
