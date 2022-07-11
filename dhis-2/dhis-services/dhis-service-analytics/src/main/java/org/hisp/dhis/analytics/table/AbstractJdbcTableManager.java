/*
 * Copyright (c) 2004-2022, University of Oslo
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
import static org.hisp.dhis.analytics.AnalyticsTablePartition.LATEST_PARTITION;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.createIndexStatement;
import static org.hisp.dhis.analytics.util.AnalyticsIndexHelper.getIndexName;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
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

    protected static final String PREFIX_ORGUNITLEVEL = "uidlevel";

    protected final IdentifiableObjectManager idObjectManager;

    protected final OrganisationUnitService organisationUnitService;

    protected final CategoryService categoryService;

    protected final SystemSettingManager systemSettingManager;

    protected final DataApprovalLevelService dataApprovalLevelService;

    protected final ResourceTableService resourceTableService;

    protected final AnalyticsTableHookService tableHookService;

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

    @Override
    public void createTable( AnalyticsTable table )
    {
        if ( tableTypeIsPartitioned() )
        {
            createPartitionTableWithPartitions( table );
        }
        else
        {
            createNonPartitionedAnalyticsTable( table );
        }
    }

    private String getCreateTableStatement( final String tableName, final List<AnalyticsTableColumn> columns )
    {
        String sqlCreate = "create table if not exists " + tableName + " (";

        for ( final AnalyticsTableColumn col : columns )
        {
            final String notNull = col.getNotNull().isNotNull() ? " not null" : "";

            sqlCreate = sqlCreate + (col.getName() + " " + col.getDataType().getValue() + notNull + ",");
        }

        return TextUtils.removeLastComma( sqlCreate ) + ")";
    }

    @Override
    public void createIndex( final AnalyticsIndex index )
    {
        final String indexName = getIndexName( index, getAnalyticsTableType() );
        final String sql = createIndexStatement( index, getAnalyticsTableType() );

        log.debug( "Create index: '{}' with SQL: '{}'", indexName, sql );

        jdbcTemplate.execute( sql );

        log.debug( "Created index: '{}'", indexName );
    }

    @Override
    public void swapTable( AnalyticsTableUpdateParams params, AnalyticsTable table )
    {
        log.info( "Swapping master table including partitions: '{}'", table.getTableName() );

        swapTable( table, params.getLastYears() );

        if ( getPartitionColumn() != null )
        {
            table.getTablePartitions().forEach( p -> swapTable( table, p ) );
        }

        // Drop temp master table (if any), now that all partitions were
        // attached to the master table.
        executeSilently( "drop table if exists " + table.getTempTableName() + " cascade" );
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
    public void populateTablePartition( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        if ( !partition.skipPopulation() )
        {
            populateTable( params, partition );
        }
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
     * Returns the partition column name for the analytics table type, or null
     * if the table type is not partitioned.
     */
    protected abstract String getPartitionColumn();

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
    private void createPartitionTableWithPartitions( AnalyticsTable table )
    {
        createAnalyticsTable( table );
        table.getTablePartitions().forEach( p -> createAnalyticsTable( table, p ) );
    }

    private void createNonPartitionedAnalyticsTable( AnalyticsTable table )
    {
        final String tableName = table.getTableName();
        final String tempTableName = table.getTempTableName();
        final String createTableSql = "create table if not exists ";

        String sqlCreate = createTableSql + tableName + " (";
        String sqlCreateTemp = createTableSql + tempTableName + " (";

        String columns = ListUtils.union( table.getDimensionColumns(), table.getValueColumns() )
            .stream()
            .map( col -> {
                String notNull = col.getNotNull().isNotNull() ? " not null" : "";
                return col.getName() + " " + col.getDataType().getValue() + notNull;
            } )
            .collect( Collectors.joining( "," ) ) + ")";

        sqlCreate = sqlCreate + columns;
        sqlCreateTemp = sqlCreateTemp + columns;

        log.debug( "Creating non partitioned analytic table: '{}'", tableName );

        log.debug( "Create SQL: '{}'", sqlCreate );

        jdbcTemplate.execute( sqlCreate );

        log.debug( "CreateTemp SQL: '{}'", sqlCreateTemp );

        jdbcTemplate.execute( sqlCreateTemp );
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

        // Always add the latest partition as well. It might be required by some
        // analytics queries.
        table.addPartitionTable( LATEST_PARTITION, true );

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
        Date lastFullTableUpdate = systemSettingManager
            .getDateSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE );
        Date lastLatestPartitionUpdate = systemSettingManager
            .getDateSetting( SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE );
        Date lastAnyTableUpdate = DateUtils.getLatest( lastLatestPartitionUpdate, lastFullTableUpdate );

        Assert.notNull( lastFullTableUpdate,
            "A full analytics table update process must be run prior to a latest partition update process" );

        Date endDate = params.getStartTime();
        boolean hasUpdatedData = hasUpdatedLatestData( lastAnyTableUpdate, endDate );

        AnalyticsTable table = new AnalyticsTable( getAnalyticsTableType(), dimensionColumns, valueColumns );

        if ( hasUpdatedData )
        {
            table.addPartitionTable( LATEST_PARTITION, lastFullTableUpdate, endDate );
            log.info( "Added latest analytics partition with start: '{}' and end: '{}'",
                getLongDateString( lastFullTableUpdate ), getLongDateString( endDate ) );
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
        Date lastResourceTableUpdate = systemSettingManager
            .getDateSetting( SettingKey.LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE );

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
        log.debug( "{} with SQL: '{}'", logMessage, sql );

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
     * @param mainTable the partition table.
     * @param tablePartition the partition.
     */
    private void swapTable( AnalyticsTable mainTable, AnalyticsTablePartition tablePartition )
    {
        String mainTableName = mainTable.getTableName();
        String realTableName = tablePartition.getTableName();
        String tempTableName = tablePartition.getTempTableName();

        final List<String> sqlSteps = new ArrayList<>();

        sqlSteps.add( " alter table if exists " + mainTableName + " detach partition " + realTableName );
        sqlSteps.add( " drop table if exists " + realTableName + " cascade" );
        sqlSteps.add( " alter table if exists " + tempTableName + " rename to " + realTableName );

        // Do not attach the latest partition ("0"). Otherwise we will face
        // issues with the "year" column.
        if ( !tablePartition.isLatestPartition() )
        {
            sqlSteps.add( " alter table if exists " + mainTableName + " attach partition " + realTableName
                + " for values in (" + tablePartition.getYear() + ")" );
        }

        for ( final String step : sqlSteps )
        {
            log.debug( step );
            executeSilently( step );
        }
    }

    private void swapTable( final AnalyticsTable mainTable, final Integer lastYears )
    {
        final String mainTableName = mainTable.getTableName();
        final String tempTableName = mainTable.getTempTableName();
        final boolean isFullExport = lastYears == null;
        final List<String> sqlSteps = new ArrayList<>();

        if ( isFullExport || !mainTable.hasPartitionTables() )
        {
            sqlSteps.add( " drop table if exists " + mainTableName + " cascade" );
        }

        sqlSteps.add( " alter table if exists " + tempTableName + " rename to " + mainTableName );

        final String sql = String.join( ";", sqlSteps ) + ";";

        log.debug( sql );

        executeSilently( sql );
    }

    /**
     * Create a analytics table (non partition)
     *
     * @param table the partition table.
     */
    private void createAnalyticsTable( AnalyticsTable table )
    {
        createAnalyticsTable( table, null );
    }

    /**
     * Create a analytics partition table, when partition is not null and there
     * is a valid column for partition index otherwise create a analytics table
     * (non partition)
     *
     * @param table the partition table.
     * @param partition the table partition.
     */
    private void createAnalyticsTable( AnalyticsTable table, AnalyticsTablePartition partition )
    {
        final boolean isLatestPartition = partition != null && partition.isLatestPartition();

        if ( !isLatestPartition )
        {
            createTableAsPartitionOf( table, partition );
        }

        final String tableName = partition == null ? table.getTempTableName() : partition.getTempTableName();

        String createTableStatement = getCreateTableStatement( tableName,
            ListUtils.union( table.getDimensionColumns(), table.getValueColumns() ) );

        if ( partition == null && getPartitionColumn() != null && !isLatestPartition )
        {
            createTableStatement += " partition by list(\"" + getPartitionColumn() + "\")";
        }

        log.debug( "Creating table: '{}', columns: {}", tableName, table.getDimensionColumns().size() );
        log.debug( "Created SQL: '{}'", createTableStatement );

        jdbcTemplate.execute( createTableStatement );
    }

    /**
     * Create a table partition when partition is not null and there is a valid
     * column for partition index.
     *
     * @param table the partition table.
     * @param partition the table partition.
     */
    private void createTableAsPartitionOf( AnalyticsTable table, AnalyticsTablePartition partition )
    {
        if ( partition != null && getPartitionColumn() != null )
        {
            String createTableAsPartitionOfSql = "create table if not exists " + partition.getTableName()
                + " partition of " + table.getTempTableName() + " for values in " + "(" + partition.getYear() + ")";

            log.debug( "Creating table: '{}', columns: {}", partition.getTableName(),
                table.getDimensionColumns().size() );

            jdbcTemplate.execute( createTableAsPartitionOfSql );
        }
    }

    /**
     * Indicates whether this analytics table type is partitioned.
     */
    private boolean tableTypeIsPartitioned()
    {
        return getPartitionColumn() != null;
    }
}
