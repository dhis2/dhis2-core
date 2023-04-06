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

import static java.util.Collections.emptyList;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.DOUBLE;
import static org.hisp.dhis.analytics.ColumnDataType.GEOMETRY;
import static org.hisp.dhis.analytics.ColumnDataType.INTEGER;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_255;
import static org.hisp.dhis.analytics.ColumnDataType.VARCHAR_50;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.getClosingParentheses;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.resourcetable.ResourceTable.NEWEST_YEAR_PERIOD_SUPPORTED;
import static org.hisp.dhis.resourcetable.ResourceTable.OLDEST_YEAR_PERIOD_SUPPORTED;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.AnalyticsExportSettings;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.AnalyticsTableView;
import org.hisp.dhis.analytics.ColumnDataType;
import org.hisp.dhis.analytics.IndexType;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.util.DateUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service( "org.hisp.dhis.analytics.EventAnalyticsTableManager" )
public class JdbcEventAnalyticsTableManager
    extends AbstractEventJdbcTableManager
{
    public static final String OU_NAME_COL_SUFFIX = "_name";

    public static final String OU_GEOMETRY_COL_SUFFIX = "_geom";

    public JdbcEventAnalyticsTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        SystemSettingManager systemSettingManager, DataApprovalLevelService dataApprovalLevelService,
        ResourceTableService resourceTableService, AnalyticsTableHookService tableHookService,
        StatementBuilder statementBuilder, PartitionManager partitionManager, DatabaseInfo databaseInfo,
        JdbcTemplate jdbcTemplate, AnalyticsExportSettings analyticsExportSettings,
        PeriodDataProvider periodDataProvider )
    {
        super( idObjectManager, organisationUnitService, categoryService, systemSettingManager,
            dataApprovalLevelService, resourceTableService, tableHookService, statementBuilder, partitionManager,
            databaseInfo, jdbcTemplate, analyticsExportSettings, periodDataProvider );
    }

    private static final List<AnalyticsTableColumn> FIXED_COLS = ImmutableList.of(
        new AnalyticsTableColumn( quote( "psi" ), CHARACTER_11, NOT_NULL, "psi.uid" ),
        new AnalyticsTableColumn( quote( "pi" ), CHARACTER_11, NOT_NULL, "pi.uid" ),
        new AnalyticsTableColumn( quote( "ps" ), CHARACTER_11, NOT_NULL, "ps.uid" ),
        new AnalyticsTableColumn( quote( "ao" ), CHARACTER_11, NOT_NULL, "ao.uid" ),
        new AnalyticsTableColumn( quote( "enrollmentdate" ), TIMESTAMP, "pi.enrollmentdate" ),
        new AnalyticsTableColumn( quote( "incidentdate" ), TIMESTAMP, "pi.incidentdate" ),
        new AnalyticsTableColumn( quote( "executiondate" ), TIMESTAMP, "psi.executiondate" ),
        new AnalyticsTableColumn( quote( "duedate" ), TIMESTAMP, "psi.duedate" ),
        new AnalyticsTableColumn( quote( "completeddate" ), TIMESTAMP, "psi.completeddate" ),
        new AnalyticsTableColumn( quote( "created" ), TIMESTAMP, "psi.created" ),
        new AnalyticsTableColumn( quote( "lastupdated" ), TIMESTAMP, "psi.lastupdated" ),
        new AnalyticsTableColumn( quote( STORED_BY_COL_NAME ), VARCHAR_255, "psi.storedby" ),
        new AnalyticsTableColumn( quote( CREATED_BY_COL_USER_NAME ), VARCHAR_255,
            "psi.createdbyuserinfo ->> 'username' as " + CREATED_BY_COL_USER_NAME ),
        new AnalyticsTableColumn( quote( CREATED_BY_COL_NAME ), VARCHAR_255,
            "psi.createdbyuserinfo ->> 'firstName' as " + CREATED_BY_COL_NAME ),
        new AnalyticsTableColumn( quote( CREATED_BY_COL_LAST_NAME ), VARCHAR_255,
            "psi.createdbyuserinfo ->> 'surname' as " + CREATED_BY_COL_LAST_NAME ),
        new AnalyticsTableColumn( quote( CREATED_BY_COL_DISPLAY_LAST_NAME ), VARCHAR_255,
            getDisplayName( "createdbyuserinfo", "psi", CREATED_BY_COL_DISPLAY_LAST_NAME ) ),
        new AnalyticsTableColumn( quote( LAST_UPDATED_BY_COL_USER_NAME ), VARCHAR_255,
            "psi.lastupdatedbyuserinfo ->> 'username' as " + LAST_UPDATED_BY_COL_USER_NAME ),
        new AnalyticsTableColumn( quote( LAST_UPDATED_BY_COL_NAME ), VARCHAR_255,
            "psi.lastupdatedbyuserinfo ->> 'firstName' as " + LAST_UPDATED_BY_COL_NAME ),
        new AnalyticsTableColumn( quote( LAST_UPDATED_BY_COL_LAST_NAME ), VARCHAR_255,
            "psi.lastupdatedbyuserinfo ->> 'surname' as " + LAST_UPDATED_BY_COL_LAST_NAME ),
        new AnalyticsTableColumn( quote( LAST_UPDATED_BY_COL_DISPLAY_LAST_NAME ), VARCHAR_255,
            getDisplayName( "lastupdatedbyuserinfo", "psi", LAST_UPDATED_BY_COL_DISPLAY_LAST_NAME ) ),
        new AnalyticsTableColumn( quote( "pistatus" ), VARCHAR_50, "pi.status" ),
        new AnalyticsTableColumn( quote( "psistatus" ), VARCHAR_50, "psi.status" ),
        new AnalyticsTableColumn( quote( "psigeometry" ), GEOMETRY, "psi.geometry" )
            .withIndexType( IndexType.GIST ),
        // TODO latitude and longitude deprecated in 2.30, remove in 2.33
        new AnalyticsTableColumn( quote( "longitude" ), DOUBLE,
            "CASE WHEN 'POINT' = GeometryType(psi.geometry) THEN ST_X(psi.geometry) ELSE null END" ),
        new AnalyticsTableColumn( quote( "latitude" ), DOUBLE,
            "CASE WHEN 'POINT' = GeometryType(psi.geometry) THEN ST_Y(psi.geometry) ELSE null END" ),
        new AnalyticsTableColumn( quote( "ou" ), CHARACTER_11, NOT_NULL, "ou.uid" ),
        new AnalyticsTableColumn( quote( "ouname" ), TEXT, NOT_NULL, "ou.name" ),
        new AnalyticsTableColumn( quote( "oucode" ), TEXT, "ou.code" ),
        new AnalyticsTableColumn( quote( "oulevel" ), INTEGER, "ous.level" ),
        new AnalyticsTableColumn( quote( "ougeometry" ), GEOMETRY, "ou.geometry" )
            .withIndexType( IndexType.GIST ),
        new AnalyticsTableColumn( quote( "pigeometry" ), GEOMETRY, "pi.geometry" )
            .withIndexType( IndexType.GIST ) );

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.EVENT;
    }

    @Override
    @Transactional
    public List<AnalyticsTable> getAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        log.info( String.format( "Get tables using earliest: %s, spatial support: %b", params.getFromDate(),
            databaseInfo.isSpatialSupport() ) );

        List<Integer> availableDataYears = periodDataProvider.getAvailableYears();

        return params.isLatestUpdate() ? getLatestAnalyticsTables( params )
            : getRegularAnalyticsTables( params, availableDataYears );
    }

    /**
     * Creates a list of {@link AnalyticsTable} for each program. The tables
     * contain a partition for each year for which events exist.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @return a list of {@link AnalyticsTableUpdateParams}.
     */
    private List<AnalyticsTable> getRegularAnalyticsTables( AnalyticsTableUpdateParams params,
        List<Integer> availableDataYears )
    {
        List<AnalyticsTable> tables = new ArrayList<>();

        Calendar calendar = PeriodType.getCalendar();

        List<Program> programs = params.isSkipPrograms() ? idObjectManager.getAllNoAcl( Program.class )
            : idObjectManager.getAllNoAcl( Program.class )
                .stream()
                .filter( p -> !params.getSkipPrograms().contains( p.getUid() ) )
                .collect( Collectors.toList() );

        Integer firstDataYear = availableDataYears.get( 0 );
        Integer latestDataYear = availableDataYears.get( availableDataYears.size() - 1 );

        for ( Program program : programs )
        {
            List<Integer> dataYears = getDataYears( params, program, firstDataYear, latestDataYear );

            Collections.sort( dataYears );

            AnalyticsTable table = new AnalyticsTable( getAnalyticsTableType(), getDimensionColumns( program ),
                Lists.newArrayList(), program );

            for ( Integer year : dataYears )
            {
                if ( !params.isViewsEnabled() )
                {
                    table.addPartitionTable( year, PartitionUtils.getStartDate( calendar, year ),
                        PartitionUtils.getEndDate( calendar, year ) );
                }
                else
                {
                    table.addView( year );
                }
            }

            if ( table.hasPartitionTables() || table.hasViews() )
            {
                tables.add( table );
            }
        }

        return tables;
    }

    /**
     * Creates a list of {@link AnalyticsTable} with a partition each or the
     * "latest" data. The start date of the partition is the time of the last
     * successful full analytics table update. The end date of the partition is
     * the start time of this analytics table update process.
     *
     * @param params the {@link AnalyticsTableUpdateParams}.
     * @return a list of {@link AnalyticsTableUpdateParams}.
     */
    private List<AnalyticsTable> getLatestAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        Date lastFullTableUpdate = systemSettingManager
            .getDateSetting( SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE );
        Date lastLatestPartitionUpdate = systemSettingManager
            .getDateSetting( SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE );
        Date lastAnyTableUpdate = DateUtils.getLatest( lastLatestPartitionUpdate, lastFullTableUpdate );

        Assert.notNull( lastFullTableUpdate,
            "A full analytics table update process must be run prior to a latest partition update process" );

        Date startDate = lastFullTableUpdate;
        Date endDate = params.getStartTime();

        List<AnalyticsTable> tables = new ArrayList<>();

        List<Program> programs = params.isSkipPrograms() ? idObjectManager.getAllNoAcl( Program.class )
            .stream()
            .filter( p -> !params.getSkipPrograms().contains( p.getUid() ) )
            .collect( Collectors.toList() ) : idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            boolean hasUpdatedData = hasUpdatedLatestData( lastAnyTableUpdate, endDate, program );

            if ( hasUpdatedData )
            {
                AnalyticsTable table = new AnalyticsTable( getAnalyticsTableType(), getDimensionColumns( program ),
                    Lists.newArrayList(), program );
                table.addPartitionTable( AnalyticsTablePartition.LATEST_PARTITION, startDate, endDate );
                tables.add( table );

                log.info( String.format(
                    "Added latest event analytics partition for program: '%s' with start: '%s' and end: '%s'",
                    program.getUid(), getLongDateString( startDate ), getLongDateString( endDate ) ) );
            }
            else
            {
                log.info( String.format(
                    "No updated latest event data found for program: '%s' with start: '%s' and end: '%s",
                    program.getUid(), getLongDateString( lastAnyTableUpdate ), getLongDateString( endDate ) ) );
            }
        }

        return tables;
    }

    /**
     * Indicates whether event data stored between the given start and end date
     * and for the given program exists.
     *
     * @param startDate the start date.
     * @param endDate the end date.
     * @param program the program.
     * @return whether event data exists.
     */
    private boolean hasUpdatedLatestData( Date startDate, Date endDate, Program program )
    {
        String sql = "select psi.programstageinstanceid " +
            "from programstageinstance psi " +
            "inner join programinstance pi on psi.programinstanceid=pi.programinstanceid " +
            "where pi.programid = " + program.getId() + " " +
            "and psi.lastupdated >= '" + getLongDateString( startDate ) + "' " +
            "and psi.lastupdated < '" + getLongDateString( endDate ) + "' " +
            "limit 1";

        return !jdbcTemplate.queryForList( sql ).isEmpty();
    }

    @Override
    public void removeUpdatedData( List<AnalyticsTable> tables )
    {
        for ( AnalyticsTable table : tables )
        {
            AnalyticsTablePartition partition = table.getLatestPartition();

            String sql = "delete from " + quote( table.getTableName() ) + " ax " +
                "where ax.psi in (" +
                "select psi.uid " +
                "from programstageinstance psi " +
                "inner join programinstance pi on psi.programinstanceid=pi.programinstanceid " +
                "where pi.programid = " + table.getProgram().getId() + " " +
                "and psi.lastupdated >= '" + getLongDateString( partition.getStartDate() ) + "' " +
                "and psi.lastupdated < '" + getLongDateString( partition.getEndDate() ) + "')";

            invokeTimeAndLogSafely( sql,
                String.format( "Remove updated events for table: '%s'", table.getTableName() ) );
        }
    }

    @Override
    public List<AnalyticsTableColumn> getFixedColumns()
    {
        return FIXED_COLS;
    }

    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return partition.isLatestPartition() ? emptyList() : List.of( "yearly = '" + partition.getYear() + "'" );
    }

    @Override
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        Program program = partition.getMasterTable().getProgram();
        String start = DateUtils.getLongDateString( partition.getStartDate() );
        String end = DateUtils.getLongDateString( partition.getEndDate() );
        String partitionClause = partition.isLatestPartition() ? "and psi.lastupdated >= '" + start + "' "
            : "and psi.executiondate >= '" + start + "' and psi.executiondate < '" + end + "' ";

        String fromClause = "from programstageinstance psi " +
            "inner join programinstance pi on psi.programinstanceid=pi.programinstanceid " +
            "inner join programstage ps on psi.programstageid=ps.programstageid " +
            "inner join program pr on pi.programid=pr.programid and pi.deleted is false " +
            "inner join categoryoptioncombo ao on psi.attributeoptioncomboid=ao.categoryoptioncomboid " +
            "left join trackedentityinstance tei on pi.trackedentityinstanceid=tei.trackedentityinstanceid " +
            "and tei.deleted is false " +
            "inner join organisationunit ou on psi.organisationunitid=ou.organisationunitid " +
            "left join _orgunitstructure ous on psi.organisationunitid=ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on psi.organisationunitid=ougs.organisationunitid " +
            "and (cast(date_trunc('month', psi.executiondate) as date)=ougs.startdate or ougs.startdate is null) " +
            "inner join _categorystructure acs on psi.attributeoptioncomboid=acs.categoryoptioncomboid " +
            "left join _dateperiodstructure dps on cast(psi.executiondate as date)=dps.dateperiod " +
            "where psi.lastupdated < '" + getLongDateString( params.getStartTime() ) + "' " +
            partitionClause +
            "and pr.programid=" + program.getId() + " " +
            "and psi.organisationunitid is not null " +
            "and psi.executiondate is not null " +
            "and dps.yearly is not null " +
            "and dps.year >= " + OLDEST_YEAR_PERIOD_SUPPORTED + " " +
            "and dps.year <= " + NEWEST_YEAR_PERIOD_SUPPORTED + " " +
            "and psi.deleted is false ";

        populateTableInternal( partition, getDimensionColumns( program ), fromClause );
    }

    @Override
    protected void populateViews( AnalyticsTableUpdateParams params, AnalyticsTableView view )
    {
        Program program = view.getMasterTable().getProgram();
        Calendar calendar = PeriodType.getCalendar();
        String start = DateUtils.getLongDateString( PartitionUtils.getStartDate( calendar, view.getYear() ) );
        String end = DateUtils.getLongDateString( PartitionUtils.getEndDate( calendar, view.getYear() ) );
        String viewClause = "and psi.executiondate >= '" + start + "' and psi.executiondate < '" + end + "' ";

        String fromClause = "from programstageinstance psi " +
            "inner join programinstance pi on psi.programinstanceid=pi.programinstanceid " +
            "inner join programstage ps on psi.programstageid=ps.programstageid " +
            "inner join program pr on pi.programid=pr.programid and pi.deleted is false " +
            "inner join categoryoptioncombo ao on psi.attributeoptioncomboid=ao.categoryoptioncomboid " +
            "left join trackedentityinstance tei on pi.trackedentityinstanceid=tei.trackedentityinstanceid " +
            "and tei.deleted is false " +
            "inner join organisationunit ou on psi.organisationunitid=ou.organisationunitid " +
            "left join _orgunitstructure ous on psi.organisationunitid=ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on psi.organisationunitid=ougs.organisationunitid " +
            "and (cast(date_trunc('month', psi.executiondate) as date)=ougs.startdate or ougs.startdate is null) " +
            "inner join _categorystructure acs on psi.attributeoptioncomboid=acs.categoryoptioncomboid " +
            "left join _dateperiodstructure dps on cast(psi.executiondate as date)=dps.dateperiod " +
            "where psi.lastupdated < '" + getLongDateString( params.getStartTime() ) + "' " +
            viewClause +
            "and pr.programid=" + program.getId() + " " +
            "and psi.organisationunitid is not null " +
            "and psi.executiondate is not null " +
            "and dps.yearly is not null " +
            "and dps.year >= " + OLDEST_YEAR_PERIOD_SUPPORTED + " " +
            "and dps.year <= " + NEWEST_YEAR_PERIOD_SUPPORTED + " " +
            "and psi.deleted is false ";

        populateViewInternal( view, getDimensionColumns( program ), fromClause );
    }

    /**
     * Returns dimensional analytics table columns.
     *
     * @param program the program.
     * @return a list of {@link AnalyticsTableColumn}.
     */
    private List<AnalyticsTableColumn> getDimensionColumns( Program program )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        if ( program.hasNonDefaultCategoryCombo() )
        {
            List<Category> categories = program.getCategoryCombo().getCategories();

            for ( Category category : categories )
            {
                if ( category.isDataDimension() )
                {
                    columns.add( new AnalyticsTableColumn( quote( category.getUid() ), CHARACTER_11,
                        "acs." + quote( category.getUid() ) ).withCreated( category.getCreated() ) );
                }
            }
        }

        columns.addAll( addOrganisationUnitLevels() );
        columns.addAll( addOrganisationUnitGroupSets() );

        columns.addAll( categoryService.getAttributeCategoryOptionGroupSetsNoAcl().stream()
            .map( l -> toCharColumn( quote( l.getUid() ), "acs", l.getCreated() ) )
            .collect( Collectors.toList() ) );
        columns.addAll( addPeriodTypeColumns( "dps" ) );

        columns.addAll( program.getAnalyticsDataElements().stream()
            .map( de -> getColumnFromDataElement( de, false ) ).flatMap( Collection::stream )
            .collect( Collectors.toList() ) );

        columns.addAll( program.getAnalyticsDataElementsWithLegendSet().stream()
            .map( de -> getColumnFromDataElement( de, true ) ).flatMap( Collection::stream )
            .collect( Collectors.toList() ) );

        columns.addAll( program.getNonConfidentialTrackedEntityAttributes().stream()
            .map( tea -> getColumnFromTrackedEntityAttribute( tea, getNumericClause(), getDateClause(), false ) )
            .flatMap( Collection::stream ).collect( Collectors.toList() ) );

        columns.addAll( program.getNonConfidentialTrackedEntityAttributesWithLegendSet().stream()
            .map( tea -> getColumnFromTrackedEntityAttribute( tea, getNumericClause(), getDateClause(), true ) )
            .flatMap( Collection::stream ).collect( Collectors.toList() ) );

        columns.addAll( getFixedColumns() );

        if ( program.isRegistration() )
        {
            columns.add( new AnalyticsTableColumn( quote( "tei" ), CHARACTER_11, "tei.uid" ) );
        }

        return filterDimensionColumns( columns );
    }

    private List<AnalyticsTableColumn> getColumnFromTrackedEntityAttribute( TrackedEntityAttribute attribute,
        String numericClause, String dateClause, boolean withLegendSet )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        ColumnDataType dataType = getColumnType( attribute.getValueType(), databaseInfo.isSpatialSupport() );
        String dataClause = attribute.isNumericType() ? numericClause : attribute.isDateType() ? dateClause : "";
        String select = getSelectClause( attribute.getValueType(), "value" );
        String sql = selectForInsert( attribute, select, dataClause );
        boolean skipIndex = skipIndex( attribute.getValueType(), attribute.hasOptionSet() );

        if ( attribute.getValueType().isOrganisationUnit() )
        {
            columns.addAll( getColumnsFromOrgUnitTrackedEntityAttribute( attribute, dataClause ) );
        }

        columns.add( new AnalyticsTableColumn( quote( attribute.getUid() ), dataType, sql )
            .withSkipIndex( skipIndex ) );

        return withLegendSet ? getColumnFromTrackedEntityAttributeWithLegendSet( attribute, numericClause ) : columns;
    }

    private List<AnalyticsTableColumn> getColumnFromTrackedEntityAttributeWithLegendSet(
        TrackedEntityAttribute attribute, String numericClause )
    {
        String select = getSelectClause( attribute.getValueType(), "value" );

        return attribute.getLegendSets().stream().map( ls -> {
            String column = quote( attribute.getUid() + PartitionUtils.SEP + ls.getUid() );

            String sql = "(select l.uid from maplegend l " +
                "inner join trackedentityattributevalue av on l.startvalue <= " + select + " " +
                "and l.endvalue > " + select + " " +
                "and l.maplegendsetid=" + ls.getId() + " " +
                "and av.trackedentityinstanceid=pi.trackedentityinstanceid " +
                "and av.trackedentityattributeid=" + attribute.getId() + numericClause + ") as " + column;

            return new AnalyticsTableColumn( column, CHARACTER_11, sql );
        } ).collect( Collectors.toList() );
    }

    private List<AnalyticsTableColumn> getColumnFromDataElement( DataElement dataElement, boolean withLegendSet )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        ColumnDataType dataType = getColumnType( dataElement.getValueType(), databaseInfo.isSpatialSupport() );
        String dataClause = getDataClause( dataElement.getUid(), dataElement.getValueType() );
        String columnName = "eventdatavalues #>> '{" + dataElement.getUid() + ", value}'";
        String select = getSelectClause( dataElement.getValueType(), columnName );
        String sql = selectForInsert( dataElement, select, dataClause );
        boolean skipIndex = skipIndex( dataElement.getValueType(), dataElement.hasOptionSet() );

        if ( dataElement.getValueType().isOrganisationUnit() )
        {
            columns.addAll( getColumnFromOrgUnitDataElement( dataElement, dataClause ) );
        }

        columns.add( new AnalyticsTableColumn( quote( dataElement.getUid() ), dataType, sql )
            .withSkipIndex( skipIndex ) );

        return withLegendSet ? getColumnFromDataElementWithLegendSet( dataElement, select, dataClause ) : columns;
    }

    private List<AnalyticsTableColumn> getColumnsFromOrgUnitTrackedEntityAttribute( TrackedEntityAttribute attribute,
        String dataClause )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        if ( databaseInfo.isSpatialSupport() )
        {
            String geoSql = selectForInsert( attribute,
                "ou.geometry from organisationunit ou where ou.uid = (select value", dataClause );
            columns.add( new AnalyticsTableColumn( quote( attribute.getUid() + OU_GEOMETRY_COL_SUFFIX ),
                ColumnDataType.GEOMETRY, geoSql )
                    .withSkipIndex( false ).withIndexType( IndexType.GIST ) );
        }

        // Add org unit name column
        String fromTypeSql = "ou.name from organisationunit ou where ou.uid = (select value";
        String ouNameSql = selectForInsert( attribute, fromTypeSql, dataClause );

        columns.add( new AnalyticsTableColumn( quote( attribute.getUid() + OU_NAME_COL_SUFFIX ), TEXT, ouNameSql )
            .withSkipIndex( true ) );

        return columns;
    }

    private List<AnalyticsTableColumn> getColumnFromOrgUnitDataElement( DataElement dataElement, String dataClause )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        String columnName = "eventdatavalues #>> '{" + dataElement.getUid() + ", value}'";

        if ( databaseInfo.isSpatialSupport() )
        {
            String geoSql = selectForInsert( dataElement,
                "ou.geometry from organisationunit ou where ou.uid = (select " + columnName, dataClause );

            columns.add( new AnalyticsTableColumn( quote( dataElement.getUid() + OU_GEOMETRY_COL_SUFFIX ),
                ColumnDataType.GEOMETRY, geoSql )
                    .withSkipIndex( false ).withIndexType( IndexType.GIST ) );
        }

        // Add org unit name column
        String fromTypeSql = "ou.name from organisationunit ou where ou.uid = (select " + columnName;
        String ouNameSql = selectForInsert( dataElement, fromTypeSql, dataClause );

        columns.add( new AnalyticsTableColumn( quote( dataElement.getUid() + OU_NAME_COL_SUFFIX ), TEXT, ouNameSql )
            .withSkipIndex( true ) );

        return columns;
    }

    private String selectForInsert( DataElement dataElement, String fromType, String dataClause )
    {
        return String.format(
            "(select %s from programstageinstance where programstageinstanceid=psi.programstageinstanceid " +
                dataClause + ")" + getClosingParentheses( fromType ) + " as " + quote( dataElement.getUid() ),
            fromType );
    }

    private String selectForInsert( TrackedEntityAttribute attribute, String fromType, String dataClause )
    {
        return String.format( "(select %s" +
            " from trackedentityattributevalue where trackedentityinstanceid=pi.trackedentityinstanceid " +
            "and trackedentityattributeid=" + attribute.getId() + dataClause + ")" + getClosingParentheses( fromType ) +
            " as " + quote( attribute.getUid() ), fromType );
    }

    private List<AnalyticsTableColumn> getColumnFromDataElementWithLegendSet( DataElement dataElement, String select,
        String dataClause )
    {
        return dataElement.getLegendSets().stream().map( ls -> {
            String column = quote( dataElement.getUid() + PartitionUtils.SEP + ls.getUid() );

            String sql = "(select l.uid from maplegend l " +
                "inner join programstageinstance on l.startvalue <= " + select + " " +
                "and l.endvalue > " + select + " " +
                "and l.maplegendsetid=" + ls.getId() + " " +
                "and programstageinstanceid=psi.programstageinstanceid " +
                dataClause + ") as " + column;
            return new AnalyticsTableColumn( column, CHARACTER_11, sql );
        } ).collect( Collectors.toList() );
    }

    private String getDataClause( String uid, ValueType valueType )
    {
        if ( valueType.isNumeric() || valueType.isDate() )
        {
            String regex = valueType.isNumeric() ? NUMERIC_LENIENT_REGEXP : DATE_REGEXP;

            return " and eventdatavalues #>> '{" + uid + ",value}' " + statementBuilder.getRegexpMatch() + " '" + regex
                + "'";
        }

        return "";
    }

    private List<Integer> getDataYears( AnalyticsTableUpdateParams params, Program program, Integer firstDataYear,
        Integer latestDataYear )
    {
        String sql = "select temp.supportedyear from " +
            "(select distinct extract(year from psi.executiondate) as supportedyear " +
            "from programstageinstance psi " +
            "inner join programinstance pi on psi.programinstanceid = pi.programinstanceid " +
            "where psi.lastupdated <= '" + getLongDateString( params.getStartTime() ) + "' " +
            "and pi.programid = " + program.getId() + " " +
            "and psi.executiondate is not null " +
            "and psi.executiondate > '1000-01-01' " +
            "and psi.deleted is false ";
        if ( params.getFromDate() != null )
        {
            sql += "and psi.executiondate >= '" + DateUtils.getMediumDateString( params.getFromDate() ) + "'";
        }

        sql += ") as temp where temp.supportedyear >= " + firstDataYear +
            " and temp.supportedyear <= " + latestDataYear;

        return jdbcTemplate.queryForList( sql, Integer.class );
    }

    private AnalyticsTableColumn toCharColumn( String name, String prefix, Date created )
    {
        return new AnalyticsTableColumn( name, CHARACTER_11, prefix + "." + name ).withCreated( created );
    }
}
