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
import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTablePhase;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.Resource;

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

    @Autowired
    protected IdentifiableObjectManager idObjectManager;

    @Autowired
    protected OrganisationUnitService organisationUnitService;

    @Autowired
    protected CategoryService categoryService;

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

    @Resource( name = "slowQueryJdbcTemplate" )
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
    public void createTable( AnalyticsTable table )
    {
        createTempTable( table );
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

            final String indexName = inx.getIndexName( getAnalyticsTableType() );
            final String indexType = inx.hasType() ? " using " + inx.getType() : "";
            final String indexColumns = StringUtils.join( inx.getColumns(), "," );

            final String sql = "create index " + indexName + " on " + inx.getTable() + indexType + " (" + indexColumns + ")";

            log.debug( "Create index: " + indexName + " SQL: " + sql );

            jdbcTemplate.execute( sql );

            log.debug( "Created index: " + indexName );
        }

        return null;
    }

    @Override
    public void swapTable( AnalyticsTableUpdateParams params, AnalyticsTable table )
    {
        boolean tableExists = partitionManager.tableExists( table.getTableName() );
        boolean skipMasterTable = params.isPartialUpdate() && tableExists;

        log.info( String.format( "Swapping table, master table exists: %b, skip master table: %b", tableExists, skipMasterTable ) );

        table.getPartitionTables().stream().forEach( p -> swapTable( p.getTempTableName(), p.getTableName() ) );

        if ( !skipMasterTable )
        {
            swapTable( table.getTempTableName(), table.getTableName() );
        }
        else
        {
            table.getPartitionTables().stream().forEach( p -> swapInheritance( p.getTableName(),table.getTempTableName(), table.getTableName() ) );
            dropTempTable( table );
        }
    }

    @Override
    public void dropTempTable( AnalyticsTable table )
    {
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
    public Future<?> populateTablesAsync( AnalyticsTableUpdateParams params, ConcurrentLinkedQueue<AnalyticsTablePartition> partitions )
    {
        taskLoop: while ( true )
        {
            AnalyticsTablePartition partition = partitions.poll();

            if ( partition == null )
            {
                break taskLoop;
            }

            populateTable( params, partition );
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
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param table the analytics table to populate.
     */
    protected abstract void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition );

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
        final String sql =
            "drop table if exists " + realTableName + ";" +
            "alter table " + tempTableName + " rename to " + realTableName + ";";

        executeSilently( sql );
    }

    /**
     * Updates table inheritance of a table partition from the temp master table
     * to the real master table.
     *
     * @param partitionTableName the partition table name.
     * @param tempMasterTableName the temporary master table name.
     * @param realMasterTableName the real master table name.
     */
    private void swapInheritance( String partitionTableName, String tempMasterTableName, String realMasterTableName )
    {
        final String sql =
            "alter table " + partitionTableName + " inherit " + realMasterTableName + ";" +
            "alter table " + partitionTableName + " no inherit " + tempMasterTableName + ";";

        executeSilently( sql );
    }
}
