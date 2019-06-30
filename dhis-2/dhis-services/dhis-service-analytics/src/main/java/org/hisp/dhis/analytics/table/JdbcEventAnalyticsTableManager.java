package org.hisp.dhis.analytics.table;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_50;
import static org.hisp.dhis.analytics.ColumnDataType.DOUBLE;
import static org.hisp.dhis.analytics.ColumnDataType.GEOMETRY;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.addClosingParentheses;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.system.util.MathUtils.NUMERIC_LENIENT_REGEXP;
import static org.hisp.dhis.util.DateUtils.getLongDateString;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.ColumnDataType;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.util.DateUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.EventAnalyticsTableManager" )
public class JdbcEventAnalyticsTableManager
    extends AbstractEventJdbcTableManager
{
    private static final ImmutableSet<ValueType> NO_INDEX_VAL_TYPES = ImmutableSet.of( ValueType.TEXT, ValueType.LONG_TEXT );

    public JdbcEventAnalyticsTableManager( IdentifiableObjectManager idObjectManager,
        OrganisationUnitService organisationUnitService, CategoryService categoryService,
        SystemSettingManager systemSettingManager, DataApprovalLevelService dataApprovalLevelService,
        ResourceTableService resourceTableService, AnalyticsTableHookService tableHookService,
        StatementBuilder statementBuilder, PartitionManager partitionManager, DatabaseInfo databaseInfo,
        JdbcTemplate jdbcTemplate )
    {
        super( idObjectManager, organisationUnitService, categoryService, systemSettingManager,
            dataApprovalLevelService, resourceTableService, tableHookService, statementBuilder, partitionManager,
            databaseInfo, jdbcTemplate );
    }

    private List<AnalyticsTableColumn> FIXED_COLS = Lists.newArrayList(
        new AnalyticsTableColumn( quote( "psi" ), CHARACTER_11, NOT_NULL, "psi.uid" ),
        new AnalyticsTableColumn( quote( "pi" ), CHARACTER_11, NOT_NULL, "pi.uid" ),
        new AnalyticsTableColumn( quote( "ps" ), CHARACTER_11, NOT_NULL, "ps.uid" ),
        new AnalyticsTableColumn( quote( "ao" ), CHARACTER_11, NOT_NULL, "ao.uid" ),
        new AnalyticsTableColumn( quote( "enrollmentdate" ), TIMESTAMP, "pi.enrollmentdate" ),
        new AnalyticsTableColumn( quote( "incidentdate" ), TIMESTAMP, "pi.incidentdate" ),
        new AnalyticsTableColumn( quote( "executiondate" ), TIMESTAMP, "psi.executiondate" ),
        new AnalyticsTableColumn( quote( "duedate" ),TIMESTAMP, "psi.duedate" ),
        new AnalyticsTableColumn( quote( "completeddate" ), TIMESTAMP, "psi.completeddate" ),
        new AnalyticsTableColumn( quote( "created" ), TIMESTAMP, "psi.created" ),
        new AnalyticsTableColumn( quote( "lastupdated" ), TIMESTAMP, "psi.lastupdated" ),
        new AnalyticsTableColumn( quote( "pistatus" ), CHARACTER_50, "pi.status" ),
        new AnalyticsTableColumn( quote( "psistatus" ), CHARACTER_50, "psi.status" ),
        new AnalyticsTableColumn( quote( "psigeometry" ), GEOMETRY, "psi.geometry" ).withIndexType( "gist" ),
        // TODO lat and lng deprecated in 2.30, should be removed after 2.33
        new AnalyticsTableColumn( quote( "longitude" ), DOUBLE, "CASE WHEN 'POINT' = GeometryType(psi.geometry) THEN ST_X(psi.geometry) ELSE null END" ),
        new AnalyticsTableColumn( quote( "latitude" ), DOUBLE, "CASE WHEN 'POINT' = GeometryType(psi.geometry) THEN ST_Y(psi.geometry) ELSE null END" ),
        new AnalyticsTableColumn( quote( "ou" ), CHARACTER_11, NOT_NULL, "ou.uid" ),
        new AnalyticsTableColumn( quote( "ouname" ), TEXT, NOT_NULL, "ou.name" ),
        new AnalyticsTableColumn( quote( "oucode" ), TEXT, "ou.code" )
    );

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.EVENT;
    }

    @Override
    @Transactional
    public List<AnalyticsTable> getAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        Date earliest = params.getFromDate();

        log.info( String.format( "Get tables using earliest: %s, spatial support: %b", earliest, databaseInfo.isSpatialSupport() ) );

        List<AnalyticsTable> tables = new ArrayList<>();

        Calendar calendar = PeriodType.getCalendar();

        List<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            List<Integer> dataYears = getDataYears( program, earliest );

            Collections.sort( dataYears );

            AnalyticsTable table = new AnalyticsTable( getAnalyticsTableType(), getDimensionColumns( program ), Lists.newArrayList(), program );

            for ( Integer year : dataYears )
            {
                table.addPartitionTable( year, PartitionUtils.getStartDate( calendar, year ), PartitionUtils.getEndDate( calendar, year ) );
            }

            if ( table.hasPartitionTables() )
            {
                tables.add( table );
            }
        }

        return tables;
    }

    @Override
    public List<AnalyticsTableColumn> getFixedColumns()
    {
        return this.FIXED_COLS;
    }

    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return Lists.newArrayList(
            "yearly = '" + partition.getYear() + "'",
            "executiondate >= '" + DateUtils.getMediumDateString( partition.getStartDate() ) + "'",
            "executiondate < '" + DateUtils.getMediumDateString( partition.getEndDate() ) + "'" );
    }

    @Override
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        final Program program = partition.getMasterTable().getProgram();
        final String start = DateUtils.getMediumDateString( partition.getStartDate() );
        final String end = DateUtils.getMediumDateString( partition.getEndDate() );

        String sqlJoin = "from programstageinstance psi " +
            "inner join programinstance pi on psi.programinstanceid=pi.programinstanceid " +
            "inner join programstage ps on psi.programstageid=ps.programstageid " +
            "inner join program pr on pi.programid=pr.programid and pi.deleted is false " +
            "inner join categoryoptioncombo ao on psi.attributeoptioncomboid=ao.categoryoptioncomboid " +
            "left join trackedentityinstance tei on pi.trackedentityinstanceid=tei.trackedentityinstanceid and tei.deleted is false " +
            "inner join organisationunit ou on psi.organisationunitid=ou.organisationunitid " +
            "left join _orgunitstructure ous on psi.organisationunitid=ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on psi.organisationunitid=ougs.organisationunitid " +
            "and (cast(date_trunc('month', psi.executiondate) as date)=ougs.startdate or ougs.startdate is null) " +
            "inner join _categorystructure acs on psi.attributeoptioncomboid=acs.categoryoptioncomboid " +
            "left join _dateperiodstructure dps on cast(psi.executiondate as date)=dps.dateperiod " +
            "where psi.executiondate >= '" + start + "' " +
            "and psi.executiondate < '" + end + "' " +
            "and psi.lastupdated <= '" + getLongDateString( params.getStartTime() ) + "' " +
            "and pr.programid=" + program.getId() + " " +
            "and psi.organisationunitid is not null " +
            "and psi.executiondate is not null " +
            "and psi.deleted is false ";

        populateTableInternal( partition, getDimensionColumns( program ), sqlJoin );
    }

    private List<AnalyticsTableColumn> getDimensionColumns( Program program )
    {
        final String numericClause = " and value " + statementBuilder.getRegexpMatch() + " '" + NUMERIC_LENIENT_REGEXP + "'";
        final String dateClause = " and value " + statementBuilder.getRegexpMatch() + " '" + DATE_REGEXP + "'";

        List<AnalyticsTableColumn> columns = new ArrayList<>();

        if ( program.hasCategoryCombo() )
        {
            List<Category> categories = program.getCategoryCombo().getCategories();

            for ( Category category : categories )
            {
                if ( category.isDataDimension() )
                {
                    columns.add( new AnalyticsTableColumn( quote( category.getUid() ), CHARACTER_11, "acs." + quote( category.getUid() ) ).withCreated( category.getCreated() ) );
                }
            }
        }

        List<OrganisationUnitLevel> levels =
            organisationUnitService.getFilledOrganisationUnitLevels();

        List<OrganisationUnitGroupSet> orgUnitGroupSets =
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        List<CategoryOptionGroupSet> attributeCategoryOptionGroupSets =
            categoryService.getAttributeCategoryOptionGroupSetsNoAcl();

        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            columns.add( new AnalyticsTableColumn( column, CHARACTER_11, "ous." + column ).withCreated( level.getCreated() ) );
        }

        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), CHARACTER_11, "ougs." + quote( groupSet.getUid() ) ).withCreated( groupSet.getCreated() ) );
        }

        for ( CategoryOptionGroupSet groupSet : attributeCategoryOptionGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), CHARACTER_11, "acs." + quote( groupSet.getUid() ) ).withCreated( groupSet.getCreated() ) );
        }

        columns.addAll( addPeriodColumns( "dps" ) );

        for ( DataElement dataElement : program.getDataElements() )
        {
            ColumnDataType dataType = getColumnType( dataElement.getValueType(), databaseInfo.isSpatialSupport() );
            // Assemble a regex dataClause with using jsonb #>> operator
            String dataClause = getDataClause( dataElement.getUid(), dataElement.getValueType() );

            // Assemble a String with using jsonb #>> operator that will fetch the required value
            String columnName = "eventdatavalues #>> '{" + dataElement.getUid() + ", value}'";
            String select = getSelectClause( dataElement.getValueType(), columnName );
            boolean skipIndex = NO_INDEX_VAL_TYPES.contains( dataElement.getValueType() ) && !dataElement.hasOptionSet();

            String sql = "(select " + select + " from programstageinstance where programstageinstanceid=psi.programstageinstanceid " +
                dataClause + ")" + addClosingParentheses(select)  + " as " + quote( dataElement.getUid() );

            columns.add( new AnalyticsTableColumn( quote( dataElement.getUid() ), dataType, sql ).withSkipIndex( skipIndex ) );
        }

        for ( DataElement dataElement : program.getDataElementsWithLegendSet() )
        {
            // Assemble a regex dataClause with using jsonb #>> operator
            String dataClause = getDataClause( dataElement.getUid(), dataElement.getValueType() );

            // Assemble a String with using jsonb #>> operator that will fetch the required value
            String columnName = "eventdatavalues #>> '{" + dataElement.getUid() + ", value}'";
            String select = getSelectClause( dataElement.getValueType(), columnName );

            for ( LegendSet legendSet : dataElement.getLegendSets() )
            {
                String column = quote( dataElement.getUid() + PartitionUtils.SEP + legendSet.getUid() );

                String sql =
                    "(select l.uid from maplegend l " +
                    "inner join programstageinstance on l.startvalue <= " + select + " " +
                    "and l.endvalue > " + select + " " +
                    "and l.maplegendsetid=" + legendSet.getId() + " " +
                    "and programstageinstanceid=psi.programstageinstanceid " +
                    dataClause + ") as " + column;

                columns.add( new AnalyticsTableColumn( column, CHARACTER_11, sql ) );
            }
        }

        for ( TrackedEntityAttribute attribute : program.getNonConfidentialTrackedEntityAttributes() )
        {
            ColumnDataType dataType = getColumnType( attribute.getValueType(), databaseInfo.isSpatialSupport() );
            String dataClause = attribute.isNumericType() ? numericClause : attribute.isDateType() ? dateClause : "";
            String select = getSelectClause( attribute.getValueType(), "value" );
            boolean skipIndex = NO_INDEX_VAL_TYPES.contains( attribute.getValueType() ) && !attribute.hasOptionSet();

            String sql = "(select " + select + " from trackedentityattributevalue where trackedentityinstanceid=pi.trackedentityinstanceid " +
                "and trackedentityattributeid=" + attribute.getId() + dataClause + ") as " + quote( attribute.getUid() );

            columns.add( new AnalyticsTableColumn( quote( attribute.getUid() ), dataType, sql ).withSkipIndex( skipIndex ) );
        }

        for ( TrackedEntityAttribute attribute : program.getNonConfidentialTrackedEntityAttributesWithLegendSet() )
        {
            String select = getSelectClause( attribute.getValueType(), "value" );

            for ( LegendSet legendSet : attribute.getLegendSets() )
            {
                String column = quote( attribute.getUid() + PartitionUtils.SEP + legendSet.getUid() );

                String sql =
                    "(select l.uid from maplegend l " +
                    "inner join trackedentityattributevalue av on l.startvalue <= " + select + " " +
                    "and l.endvalue > " + select + " " +
                    "and l.maplegendsetid=" + legendSet.getId() + " " +
                    "and av.trackedentityinstanceid=pi.trackedentityinstanceid " +
                    "and av.trackedentityattributeid=" + attribute.getId() + numericClause + ") as " + column;

                columns.add( new AnalyticsTableColumn( column, CHARACTER_11, sql ) );
            }
        }

        columns.addAll( getFixedColumns() );

        if ( program.isRegistration() )
        {
            columns.add( new AnalyticsTableColumn( quote( "tei" ), CHARACTER_11, "tei.uid" ) );
            columns.add( new AnalyticsTableColumn( quote( "pigeometry" ), GEOMETRY, "pi.geometry" ) );
        }

        return filterDimensionColumns( columns );
    }

    private String getDataClause( String uid, ValueType valueType )
    {
        if ( valueType.isNumeric() || valueType.isDate() )
        {
            String regex = valueType.isNumeric() ? NUMERIC_LENIENT_REGEXP : valueType.isDate() ? DATE_REGEXP : "";
            return " and eventdatavalues #>> '{" + uid + ",value}' " + statementBuilder.getRegexpMatch() + " '" + regex + "'";
        }

        return "";
    }

    private List<Integer> getDataYears( Program program, Date earliest )
    {
        String sql =
            "select distinct(extract(year from psi.executiondate)) " +
            "from programstageinstance psi " +
            "inner join programinstance pi on psi.programinstanceid = pi.programinstanceid " +
            "where pi.programid = " + program.getId() + " " +
            "and psi.executiondate is not null " +
            "and psi.deleted is false ";

        if ( earliest != null )
        {
            sql += "and psi.executiondate >= '" + DateUtils.getMediumDateString( earliest ) + "'";
        }

        return jdbcTemplate.queryForList( sql, Integer.class );
    }
}
