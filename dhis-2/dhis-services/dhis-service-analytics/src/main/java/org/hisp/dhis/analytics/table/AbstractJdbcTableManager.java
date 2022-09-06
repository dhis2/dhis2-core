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
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
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
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.Assert;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public abstract class AbstractJdbcTableManager
    implements AnalyticsTableManager
{
    /**
     * Matches the following patterns:
     *
     * <ul>
     * <li>1999-12-12</li>
     * <li>1999-12-12T</li>
     * <li>1999-12-12T10:10:10</li>
     * <li>1999-10-10 10:10:10</li>
     * <li>1999-10-10 10:10</li>
     * <li>2021-12-14T11:45:00.000Z</li>
     * <li>2021-12-14T11:45:00.000</li>
     * </ul>
     */
    protected static final String DATE_REGEXP = "^\\d{4}-\\d{2}-\\d{2}(\\s|T)?((\\d{2}:)(\\d{2}:)?(\\d{2}))?(|.(\\d{3})|.(\\d{3})Z)?$";

    protected static final Set<ValueType> NO_INDEX_VAL_TYPES = ImmutableSet.of( ValueType.TEXT, ValueType.LONG_TEXT );

    public static final String PREFIX_ORGUNITLEVEL = "uidlevel";

    public static final String WITH_AUTOVACUUM_ENABLED_FALSE = "with(autovacuum_enabled = false)";

    protected final IdentifiableObjectManager idObjectManager;

    protected final OrganisationUnitService organisationUnitService;

    protected final CategoryService categoryService;

    protected final SystemSettingManager systemSettingManager;

    protected final DataApprovalLevelService dataApprovalLevelService;

    protected final ResourceTableService resourceTableService;

    private final AnalyticsTableHookService tableHookService;

    protected final StatementBuilder statementBuilder;

    protected final PartitionManager partitionManager;

    protected final DatabaseInfo databaseInfo;

    protected final JdbcTemplate jdbcTemplate;

    @Autowired
    public AbstractJdbcTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        SystemSettingManager systemSettingManager, DataApprovalLevelService dataApprovalLevelService,
        ResourceTableService resourceTableService, AnalyticsTableHookService tableHookService,
        StatementBuilder statementBuilder, PartitionManager partitionManager, DatabaseInfo databaseInfo,
        JdbcTemplate jdbcTemplate )
    {
        checkNotNull( idObjectManager );
        checkNotNull( organisationUnitService );
        checkNotNull( categoryService );
        checkNotNull( systemSettingManager );
        checkNotNull( dataApprovalLevelService );
        checkNotNull( resourceTableService );
        checkNotNull( tableHookService );
        checkNotNull( statementBuilder );
        checkNotNull( partitionManager );
        checkNotNull( databaseInfo );

        this.idObjectManager = idObjectManager;
        this.organisationUnitService = organisationUnitService;
        this.categoryService = categoryService;
        this.systemSettingManager = systemSettingManager;
        this.dataApprovalLevelService = dataApprovalLevelService;
        this.resourceTableService = resourceTableService;
        this.tableHookService = tableHookService;
        this.statementBuilder = statementBuilder;
        this.partitionManager = partitionManager;
        this.databaseInfo = databaseInfo;
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return partitionManager.getAnalyticsPartitions( getAnalyticsTableType() );
    }

    /**
     * Override in order to perform work before tables are being generated.
     */
    @Override
    public void preCreateTables( AnalyticsTableUpdateParams params )
    {
    }

    /**
     * Removes data which was updated or deleted between the last successful
     * analytics table update and the start of this analytics table update
     * process, excluding data which was created during that time span.
     *
     * Override in order to remove updated and deleted data for "latest"
     * partition update.
     */
    @Override
    public void removeUpdatedData( AnalyticsTableUpdateParams params, List<AnalyticsTable> tables )
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
        while ( true )
        {
            AnalyticsIndex inx = indexes.poll();

            if ( inx == null )
            {
                break;
            }

            String indexName = inx.getIndexName( getAnalyticsTableType() );
            String indexType = inx.hasType() ? " using " + inx.getType() : EMPTY;
            String indexColumns = StringUtils.join( inx.getColumns(), "," );

            String sql = "create index " + indexName + " on " + inx.getTable() + indexType + " (" + indexColumns + ")";

            log.debug( "Create index: {} with SQL: {}", indexName, sql );

            jdbcTemplate.execute( sql );

            log.debug( "Created index: {}", indexName );
        }

        return null;
    }

    @Override
    public void swapTable( AnalyticsTableUpdateParams params, AnalyticsTable table )
    {
        boolean tableExists = partitionManager.tableExists( table.getTableName() );
        boolean skipMasterTable = params.isPartialUpdate() && tableExists
            && table.getTableType().hasLatestPartition();

        log.info( "Swapping table, master table exists: {}, skip master table: {}", tableExists, skipMasterTable );

        table.getTablePartitions().stream().forEach( p -> swapTable( p.getTempTableName(), p.getTableName() ) );

        if ( !skipMasterTable )
        {
            swapTable( table.getTempTableName(), table.getTableName() );
        }
        else
        {
            table.getTablePartitions().stream()
                .forEach( p -> swapInheritance( p.getTableName(), table.getTempTableName(), table.getTableName() ) );
            dropTempTable( table );
        }
    }

    @Override
    public void dropTempTable( AnalyticsTable table )
    {
        dropTableCascade( table.getTempTableName() );
    }

    @Override
    public void dropTempTablePartition( AnalyticsTablePartition tablePartition )
    {
        dropTableCascade( tablePartition.getTempTableName() );
    }

    @Override
    public void dropTable( String tableName )
    {
        executeSilently( "drop table if exists " + tableName );
    }

    @Override
    public void dropTableCascade( String tableName )
    {
        executeSilently( "drop table if exists " + tableName + " cascade" );
    }

    @Override
    public void analyzeTable( String tableName )
    {
        String sql = StringUtils.trimToEmpty( statementBuilder.getAnalyze( tableName ) );

        executeSilently( sql );
    }

    @Override
    @Async
    public Future<?> populateTablesAsync( AnalyticsTableUpdateParams params,
        ConcurrentLinkedQueue<AnalyticsTablePartition> partitions )
    {
        while ( true )
        {
            AnalyticsTablePartition partition = partitions.poll();

            if ( partition == null )
            {
                break;
            }

            populateTable( params, partition );
        }

        return null;
    }

    @Override
    public int invokeAnalyticsTableSqlHooks()
    {
        AnalyticsTableType type = getAnalyticsTableType();
        List<AnalyticsTableHook> hooks = tableHookService
            .getByPhaseAndAnalyticsTableType( AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED, type );
        tableHookService.executeAnalyticsTableSqlHooks( hooks );
        return hooks.size();
    }

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    /**
     * Returns a list of table checks (constraints) for the given analytics
     * table partition.
     *
     * @param partition the {@link AnalyticsTablePartition}.
     */
    protected abstract List<String> getPartitionChecks( AnalyticsTablePartition partition );

    /**
     * Populates the given analytics table.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param partition the {@link AnalyticsTablePartition} to populate.
     */
    protected abstract void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition );

    /**
     * Indicates whether data was created or updated for the given time range
     * since last successful "latest" table partition update.
     *
     * @param startDate the start date.
     * @param endDate the end date.
     * @return true if updated data exists.
     */
    protected abstract boolean hasUpdatedLatestData( Date startDate, Date endDate );

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
        String sql = "select * from " + tableName + " limit 1";

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
        catch ( DataAccessException ex )
        {
            log.error( ex.getMessage() );
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

        String tableName = table.getTempTableName();

        StringBuilder sqlCreate = new StringBuilder( "create table " + tableName + " (" );

        for ( AnalyticsTableColumn col : ListUtils.union( table.getDimensionColumns(), table.getValueColumns() ) )
        {
            String notNull = col.getNotNull().isNotNull() ? " not null" : EMPTY;

            sqlCreate.append( col.getName() + " " + col.getDataType().getValue() + notNull + "," );
        }

        String sql = TextUtils.removeLastComma( sqlCreate.toString() ) + ") " + WITH_AUTOVACUUM_ENABLED_FALSE;

        log.info( "Creating table: {}, columns: {}", tableName, table.getDimensionColumns().size() );

        log.debug( "Create SQL: {}", sql );

        jdbcTemplate.execute( sql );
    }

    /**
     * Drops and creates the table partitions for the given analytics table.
     *
     * @param table the {@link AnalyticsTable}.
     */
    protected void createTempTablePartitions( AnalyticsTable table )
    {
        for ( AnalyticsTablePartition partition : table.getTablePartitions() )
        {
            String tableName = partition.getTempTableName();
            List<String> checks = getPartitionChecks( partition );

            String sqlCreate = "create table " + tableName + " (";

            if ( !checks.isEmpty() )
            {
                StringBuilder sqlCheck = new StringBuilder();
                checks.stream().forEach( check -> sqlCheck.append( "check (" + check + "), " ) );
                sqlCreate += TextUtils.removeLastComma( sqlCheck.toString() );
            }

            sqlCreate += ") inherits (" + table.getTempTableName() + ") " + WITH_AUTOVACUUM_ENABLED_FALSE;

            log.info( "Creating partition table: {}", tableName );

            log.debug( "Create SQL: {}", sqlCreate );

            jdbcTemplate.execute( sqlCreate );
        }
    }

    /**
     * Creates a {@link AnalyticsTable} with partitions based on a list of years
     * with data.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param dataYears the list of years with data.
     * @param dimensionColumns the list of dimension
     *        {@link AnalyticsTableColumn}.
     * @param valueColumns the list of value {@link AnalyticsTableColumn}.
     */
    protected AnalyticsTable getRegularAnalyticsTable( AnalyticsTableUpdateParams params, List<Integer> dataYears,
        List<AnalyticsTableColumn> dimensionColumns, List<AnalyticsTableColumn> valueColumns )
    {
        Calendar calendar = PeriodType.getCalendar();

        Collections.sort( dataYears );

        AnalyticsTable table = new AnalyticsTable( getAnalyticsTableType(), dimensionColumns, valueColumns );

        for ( Integer year : dataYears )
        {
            table.addPartitionTable( year, PartitionUtils.getStartDate( calendar, year ),
                PartitionUtils.getEndDate( calendar, year ) );
        }

        return table;
    }

    /**
     * Creates a {@link AnalyticsTable} with a partition for the "latest" data.
     * The start date of the partition is the time of the last successful full
     * analytics table update. The end date of the partition is the start time
     * of this analytics table update process.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @param dimensionColumns the list of dimension
     *        {@link AnalyticsTableColumn}.
     * @param valueColumns the list of value {@link AnalyticsTableColumn}.
     */
    protected AnalyticsTable getLatestAnalyticsTable( AnalyticsTableUpdateParams params,
        List<AnalyticsTableColumn> dimensionColumns, List<AnalyticsTableColumn> valueColumns )
    {
        Date lastFullTableUpdate = (Date) systemSettingManager
            .getSystemSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE );
        Date lastLatestPartitionUpdate = (Date) systemSettingManager
            .getSystemSetting( SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE );
        Date lastAnyTableUpdate = DateUtils.getLatest( lastLatestPartitionUpdate, lastFullTableUpdate );

        Assert.notNull( lastFullTableUpdate,
            "A full analytics table update process must be run prior to a latest partition update process" );

        Date startDate = lastFullTableUpdate;
        Date endDate = params.getStartTime();
        boolean hasUpdatedData = hasUpdatedLatestData( lastAnyTableUpdate, endDate );

        AnalyticsTable table = new AnalyticsTable( getAnalyticsTableType(), dimensionColumns, valueColumns );

        if ( hasUpdatedData )
        {
            table.addPartitionTable( AnalyticsTablePartition.LATEST_PARTITION, startDate, endDate );
            log.info( "Added latest analytics partition with start: '{}' and end: '{}'",
                getLongDateString( startDate ), getLongDateString( endDate ) );
        }
        else
        {
            log.info( "No updated latest data found with start: '{}' and end: '{}'",
                getLongDateString( lastAnyTableUpdate ), getLongDateString( endDate ) );
        }

        return table;
    }

    /**
     * Checks whether the given list of columns are valid.
     *
     * @param columns the list of {@link AnalyticsTableColumn}.
     * @throws IllegalArgumentException if not valid.
     */
    protected void validateDimensionColumns( List<AnalyticsTableColumn> columns )
    {
        if ( columns == null || columns.isEmpty() )
        {
            throw new IllegalStateException( "Analytics table dimensions are empty" );
        }

        List<String> columnNames = columns.stream().map( AnalyticsTableColumn::getName ).collect( Collectors.toList() );

        Set<String> duplicates = ListUtils.getDuplicates( columnNames );

        boolean columnsAreUnique = duplicates.isEmpty();

        Preconditions.checkArgument( columnsAreUnique,
            String.format( "Analytics table dimensions contain duplicates: %s", duplicates ) );
    }

    /**
     * Filters out analytics table columns which were created after the time of
     * the last successful resource table update. This so that the the create
     * table query does not refer to columns not present in resource tables.
     *
     * @param columns the analytics table columns.
     * @return a list of {@link AnalyticsTableColumn}.
     */
    protected List<AnalyticsTableColumn> filterDimensionColumns( List<AnalyticsTableColumn> columns )
    {
        Date lastResourceTableUpdate = (Date) systemSettingManager
            .getSystemSetting( SettingKey.LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE );

        if ( lastResourceTableUpdate == null )
        {
            return columns;
        }

        return columns.stream()
            .filter( c -> c.getCreated() == null || c.getCreated().before( lastResourceTableUpdate ) )
            .collect( Collectors.toList() );
    }

    /**
     * Executes the given SQL statement. Logs and times the operation.
     *
     * @param sql the SQL statement.
     * @param logMessage the custom log message to include in the log statement.
     */
    protected void invokeTimeAndLog( String sql, String logMessage )
    {
        log.debug( "{} with SQL: {}", logMessage, sql );

        Timer timer = new SystemTimer().start();

        jdbcTemplate.execute( sql );

        log.info( "{} in: {}", logMessage, timer.stop().toString() );
    }

    /**
     * Collects all the {@link PeriodType} as a list of
     * {@link AnalyticsTableColumn}.
     *
     * @param prefix the prefix to use for the column name
     * @return a List of {@link AnalyticsTableColumn}
     */
    protected List<AnalyticsTableColumn> addPeriodTypeColumns( String prefix )
    {
        return PeriodType.getAvailablePeriodTypes().stream()
            .map( pt -> {
                String column = quote( pt.getName().toLowerCase() );
                return new AnalyticsTableColumn( column, TEXT, prefix + "." + column );
            } )
            .collect( Collectors.toList() );
    }

    /**
     * Collects all the {@link OrganisationUnitLevel} as a list of
     * {@link AnalyticsTableColumn}.
     *
     * @return a List of {@link AnalyticsTableColumn}
     */
    protected List<AnalyticsTableColumn> addOrganisationUnitLevels()
    {
        return organisationUnitService.getFilledOrganisationUnitLevels().stream()
            .map( lv -> {
                String column = quote( PREFIX_ORGUNITLEVEL + lv.getLevel() );
                return new AnalyticsTableColumn( column, CHARACTER_11, "ous." + column ).withCreated( lv.getCreated() );
            } )
            .collect( Collectors.toList() );
    }

    /**
     * Collects all the {@link OrganisationUnitGroupSet} as a list of
     * {@link AnalyticsTableColumn}.
     *
     * @return a List of {@link AnalyticsTableColumn}
     */
    protected List<AnalyticsTableColumn> addOrganisationUnitGroupSets()
    {
        return idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class ).stream()
            .map( ougs -> {
                String column = quote( ougs.getUid() );
                return new AnalyticsTableColumn( column, CHARACTER_11, "ougs." + column )
                    .withCreated( ougs.getCreated() );
            } )
            .collect( Collectors.toList() );
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
        String sql = "drop table if exists " + realTableName + " cascade; " +
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
        String sql = "alter table " + partitionTableName + " inherit " + realMasterTableName + ";" +
            "alter table " + partitionTableName + " no inherit " + tempMasterTableName + ";";

        executeSilently( sql );
    }
}
