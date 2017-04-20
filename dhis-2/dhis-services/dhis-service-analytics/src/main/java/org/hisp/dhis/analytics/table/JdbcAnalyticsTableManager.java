package org.hisp.dhis.analytics.table;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.dataapproval.DataApprovalLevelService.APPROVAL_LEVEL_UNAPPROVED;

/**
 * This class manages the analytics tables. The analytics table is a denormalized
 * table designed for analysis which contains raw data values. It has columns for
 * each organisation unit group set and organisation unit level. Also, columns
 * for dataelementid, periodid, organisationunitid, categoryoptioncomboid, value.
 * <p>
 * The analytics table is horizontally partitioned. The partition key is the start
 * date of the  period of the data record. The table is partitioned according to
 * time span with one partition per calendar quarter.
 * <p>
 * The data records in this table are not aggregated. Typically, queries will
 * aggregate in organisation unit hierarchy dimension, in the period/time dimension,
 * and the category dimensions, as well as organisation unit group set dimensions.
 *
 * @author Lars Helge Overland
 */
public class JdbcAnalyticsTableManager
    extends AbstractJdbcTableManager
{
    @Autowired
    private SystemSettingManager systemSettingManager;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.DATA_VALUE;
    }

    @Override
    @Transactional
    public List<AnalyticsTable> getTables( Date earliest )
    {
        log.info( "Get tables using earliest: " + earliest );

        return getTables( getDataYears( earliest ) );
    }
    
    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return partitionManager.getDataValueAnalyticsPartitions();
    }
    
    @Override
    public String validState()
    {
        boolean hasData = jdbcTemplate.queryForRowSet( "select dataelementid from datavalue dv where dv.deleted is false limit 1" ).next();

        if ( !hasData )
        {
            return "No data values exist, not updating aggregate analytics tables";
        }

        int orgUnitLevels = organisationUnitService.getNumberOfOrganisationalLevels();

        if ( orgUnitLevels == 0 )
        {
            return "No organisation unit levels exist, not updating aggregate analytics tables";
        }

        return null;
    }

    @Override
    public void preCreateTables()
    {
        if ( isApprovalEnabled( null ) )
        {
            resourceTableService.generateDataApprovalMinLevelTable();
        }
    }

    @Override
    public void createTable( AnalyticsTable table )
    {
        final String tableName = table.getTempTableName();

        final String dbl = statementBuilder.getDoubleColumnType();

        final String sqlDrop = "drop table " + tableName;

        executeSilently( sqlDrop );

        String sqlCreate = "create table " + tableName + " (";

        List<AnalyticsTableColumn> columns = getDimensionColumns( table );

        validateDimensionColumns( columns );

        for ( AnalyticsTableColumn col : columns )
        {
            sqlCreate += col.getName() + " " + col.getDataType() + ",";
        }

        sqlCreate += "daysxvalue " + dbl + ", daysno integer not null, value " + dbl + ", textvalue text)";

        log.info( String.format( "Creating table: %s, columns: %d", tableName, columns.size() ) );

        log.debug( "Create SQL: " + sqlCreate );

        jdbcTemplate.execute( sqlCreate );
    }

    @Override
    protected void populateTable( AnalyticsTable table )
    {
        final String dbl = statementBuilder.getDoubleColumnType();
        final boolean skipDataTypeValidation = (Boolean) systemSettingManager.getSystemSetting( SettingKey.SKIP_DATA_TYPE_VALIDATION_IN_ANALYTICS_TABLE_EXPORT );

        final String approvalClause = getApprovalJoinClause( table );
        final String numericClause = skipDataTypeValidation ? "" : ( "and dv.value " + statementBuilder.getRegexpMatch() + " '" + MathUtils.NUMERIC_LENIENT_REGEXP + "' " );

        String intClause =
            "( dv.value != '0' or de.aggregationtype in ('" + AggregationType.AVERAGE + ',' + AggregationType.AVERAGE_SUM_ORG_UNIT + "') or de.zeroissignificant = true ) " +
            numericClause;

        populateTable( table, "cast(dv.value as " + dbl + ")", "null", ValueType.NUMERIC_TYPES, intClause, approvalClause );

        populateTable( table, "1", "null", Sets.newHashSet( ValueType.BOOLEAN, ValueType.TRUE_ONLY ), "dv.value = 'true'", approvalClause );

        populateTable( table, "0", "null", Sets.newHashSet( ValueType.BOOLEAN ), "dv.value = 'false'", approvalClause );

        populateTable( table, "null", "dv.value", Sets.union( ValueType.TEXT_TYPES, ValueType.DATE_TYPES ), null, approvalClause );
    }

    /**
     * Populates the given analytics table.
     *
     * @param table               analytics table to populate.
     * @param valueExpression     numeric value expression.
     * @param textValueExpression textual value expression.
     * @param valueTypes          data element value types to include data for.
     * @param whereClause         where clause to constrain data query.
     */
    private void populateTable( AnalyticsTable table, String valueExpression,
        String textValueExpression, Set<ValueType> valueTypes, String whereClause, String approvalClause )
    {
        final String start = DateUtils.getMediumDateString( table.getPeriod().getStartDate() );
        final String end = DateUtils.getMediumDateString( table.getPeriod().getEndDate() );
        final String tableName = table.getTempTableName();
        final String valTypes = TextUtils.getQuotedCommaDelimitedString( ObjectUtils.asStringList( valueTypes ) );
        final boolean respectStartEndDates = (Boolean) systemSettingManager.getSystemSetting( SettingKey.RESPECT_META_DATA_START_END_DATES_IN_ANALYTICS_TABLE_EXPORT );

        String sql = "insert into " + table.getTempTableName() + " (";

        List<AnalyticsTableColumn> columns = getDimensionColumns( table );

        validateDimensionColumns( columns );
        
        for ( AnalyticsTableColumn col : columns )
        {
            sql += col.getName() + ",";
        }

        sql += "daysxvalue, daysno, value, textvalue) select ";

        for ( AnalyticsTableColumn col : columns )
        {
            sql += col.getAlias() + ",";
        }

        sql +=
            valueExpression + " * ps.daysno as daysxvalue, " +
                "ps.daysno as daysno, " +
                valueExpression + " as value, " +
                textValueExpression + " as textvalue " +
                "from datavalue dv " +
                "inner join _dataelementgroupsetstructure degs on dv.dataelementid=degs.dataelementid " +
                "inner join _organisationunitgroupsetstructure ougs on dv.sourceid=ougs.organisationunitid " +
                "inner join _categorystructure dcs on dv.categoryoptioncomboid=dcs.categoryoptioncomboid " +
                "inner join _categorystructure acs on dv.attributeoptioncomboid=acs.categoryoptioncomboid " +
                "left join _orgunitstructure ous on dv.sourceid=ous.organisationunitid " +
                "inner join _dataelementstructure des on dv.dataelementid = des.dataelementid " +
                "inner join dataelement de on dv.dataelementid=de.dataelementid " +
                "inner join categoryoptioncombo co on dv.categoryoptioncomboid=co.categoryoptioncomboid " +
                "inner join categoryoptioncombo ao on dv.attributeoptioncomboid=ao.categoryoptioncomboid " +
                "inner join period pe on dv.periodid=pe.periodid " +
                "inner join _periodstructure ps on dv.periodid=ps.periodid " +
                "inner join organisationunit ou on dv.sourceid=ou.organisationunitid " +
                "inner join _categoryoptioncomboname aon on dv.attributeoptioncomboid=aon.categoryoptioncomboid " +
                "inner join _categoryoptioncomboname con on dv.categoryoptioncomboid=con.categoryoptioncomboid " +

                approvalClause +
                "where de.valuetype in (" + valTypes + ") " +
                "and de.domaintype = 'AGGREGATE' " +
                "and pe.startdate >= '" + start + "' " +
                "and pe.startdate <= '" + end + "' " +
                "and dv.value is not null " +
                "and dv.deleted is false ";

        if ( respectStartEndDates )
        {
            sql +=
                "and (aon.startdate is null or aon.startdate <= pe.startdate) " +
                "and (aon.enddate is null or aon.enddate >= pe.enddate) " +
                "and (con.startdate is null or con.startdate <= pe.startdate) " +
                "and (con.enddate is null or con.enddate >= pe.enddate) ";
        }

        if ( whereClause != null )
        {
            sql += "and " + whereClause;
        }

        populateAndLog( sql, tableName + ", " + valueTypes );
    }

    /**
     * Returns sub-query for approval level. First looks for approval level in
     * data element resource table which will indicate level 0 (highest) if approval
     * is not required. Then looks for highest level in dataapproval table.
     */
    private String getApprovalJoinClause( AnalyticsTable table )
    {
        if ( isApprovalEnabled( table ) )
        {
            String sql =
                "left join _dataapprovalminlevel da " +
                "on des.workflowid=da.workflowid and da.periodid=dv.periodid and da.attributeoptioncomboid=dv.attributeoptioncomboid " +
                "and (";

            Set<OrganisationUnitLevel> levels = dataApprovalLevelService.getOrganisationUnitApprovalLevels();

            for ( OrganisationUnitLevel level : levels )
            {
                sql += "ous.idlevel" + level.getLevel() + " = da.organisationunitid or ";
            }

            return TextUtils.removeLastOr( sql ) + ") ";
        }

        return StringUtils.EMPTY;
    }

    @Override
    public List<AnalyticsTableColumn> getDimensionColumns( AnalyticsTable table )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        List<DataElementGroupSet> dataElementGroupSets =
            idObjectManager.getDataDimensionsNoAcl( DataElementGroupSet.class );

        List<OrganisationUnitGroupSet> orgUnitGroupSets =
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        List<CategoryOptionGroupSet> disaggregationCategoryOptionGroupSets =
            categoryService.getDisaggregationCategoryOptionGroupSetsNoAcl();

        List<CategoryOptionGroupSet> attributeCategoryOptionGroupSets =
            categoryService.getAttributeCategoryOptionGroupSetsNoAcl();

        List<DataElementCategory> disaggregationCategories =
            categoryService.getDisaggregationDataDimensionCategoriesNoAcl();

        List<DataElementCategory> attributeCategories =
            categoryService.getAttributeDataDimensionCategoriesNoAcl();

        List<OrganisationUnitLevel> levels =
            organisationUnitService.getFilledOrganisationUnitLevels();

        for ( DataElementGroupSet groupSet : dataElementGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "degs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "ougs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

        for ( CategoryOptionGroupSet groupSet : disaggregationCategoryOptionGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "dcs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

        for ( CategoryOptionGroupSet groupSet : attributeCategoryOptionGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "acs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

        for ( DataElementCategory category : disaggregationCategories )
        {
            columns.add( new AnalyticsTableColumn( quote( category.getUid() ), "character(11)", "dcs." + quote( category.getUid() ), category.getCreated() ) );
        }

        for ( DataElementCategory category : attributeCategories )
        {
            columns.add( new AnalyticsTableColumn( quote( category.getUid() ), "character(11)", "acs." + quote( category.getUid() ), category.getCreated() ) );
        }

        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            columns.add( new AnalyticsTableColumn( column, "character(11)", "ous." + column, level.getCreated() ) );
        }

        List<PeriodType> periodTypes = PeriodType.getAvailablePeriodTypes();

        for ( PeriodType periodType : periodTypes )
        {
            String column = quote( periodType.getName().toLowerCase() );
            columns.add( new AnalyticsTableColumn( column, "character varying(15)", "ps." + column ) );
        }

        AnalyticsTableColumn de = new AnalyticsTableColumn( quote( "dx" ), "character(11) not null", "de.uid" );
        AnalyticsTableColumn co = new AnalyticsTableColumn( quote( "co" ), "character(11) not null", "co.uid" );
        AnalyticsTableColumn ao = new AnalyticsTableColumn( quote( "ao" ), "character(11) not null", "ao.uid" );
        AnalyticsTableColumn startDate = new AnalyticsTableColumn( quote( "pestartdate" ), "timestamp", "pe.startdate" );
        AnalyticsTableColumn endDate = new AnalyticsTableColumn( quote( "peenddate" ),"timestamp", "pe.enddate" );
        AnalyticsTableColumn pe = new AnalyticsTableColumn( quote( "pe" ), "character varying(15) not null", "ps.iso" );
        AnalyticsTableColumn ou = new AnalyticsTableColumn( quote( "ou" ), "character(11) not null", "ou.uid" );
        AnalyticsTableColumn level = new AnalyticsTableColumn( quote( "level" ), "integer", "ous.level" );

        columns.addAll( Lists.newArrayList( de, co, ao, startDate, endDate, pe, ou, level ) );

        if ( isApprovalEnabled( table ) )
        {
            String col = "coalesce(des.datasetapprovallevel, aon.approvallevel, da.minlevel, " + APPROVAL_LEVEL_UNAPPROVED + ") as approvallevel ";

            columns.add( new AnalyticsTableColumn( quote( "approvallevel" ), "integer", col ) );
        }
        else
        {
            String col = DataApprovalLevelService.APPROVAL_LEVEL_HIGHEST + " as approvallevel";

            columns.add( new AnalyticsTableColumn( quote( "approvallevel" ), "integer", col ) );
        }

        return filterDimensionColumns( columns );
    }

    private List<Integer> getDataYears( Date earliest )
    {
        String sql =
            "select distinct(extract(year from pe.startdate)) " +
            "from datavalue dv " +
            "inner join period pe on dv.periodid=pe.periodid " +
            "where pe.startdate is not null ";

        if ( earliest != null )
        {
            sql += "and pe.startdate >= '" + DateUtils.getMediumDateString( earliest ) + "'";
        }

        return jdbcTemplate.queryForList( sql, Integer.class );
    }

    @Override
    @Async
    public Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTable> tables, Collection<String> dataElements, int aggregationLevel )
    {
        taskLoop:
        while ( true )
        {
            AnalyticsTable table = tables.poll();

            if ( table == null )
            {
                break taskLoop;
            }

            StringBuilder sql = new StringBuilder( "update " + table.getTempTableName() + " set " );

            for ( int i = 0; i < aggregationLevel; i++ )
            {
                int level = i + 1;

                String column = quote( DataQueryParams.LEVEL_PREFIX + level );

                sql.append( column + " = null," );
            }

            sql.deleteCharAt( sql.length() - ",".length() );

            sql.append( " where level > " + aggregationLevel );
            sql.append( " and dx in (" + getQuotedCommaDelimitedString( dataElements ) + ")" );

            log.debug( "Aggregation level SQL: " + sql.toString() );

            jdbcTemplate.execute( sql.toString() );
        }

        return null;
    }

    @Override
    @Async
    public Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTable> tables )
    {
        taskLoop:
        while ( true )
        {
            AnalyticsTable table = tables.poll();

            if ( table == null )
            {
                break taskLoop;
            }

            final String sql = statementBuilder.getVacuum( table.getTempTableName() );

            log.debug( "Vacuum SQL: " + sql );

            jdbcTemplate.execute( sql );
        }

        return null;
    }

    /**
     * Indicates whether the system should ignore data which has not been approved
     * in analytics tables.
     */
    private boolean isApprovalEnabled( AnalyticsTable table )
    {
        boolean setting = systemSettingManager.hideUnapprovedDataInAnalytics();
        boolean levels = !dataApprovalLevelService.getAllDataApprovalLevels().isEmpty();
        Integer maxYears = (Integer) systemSettingManager.getSystemSetting( SettingKey.IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD );
        
        log.debug( String.format( "Hide approval setting: %b, approval levels exists: %b, max years threshold: %d", setting, levels, maxYears ) );
        
        if ( table != null )
        {
            boolean periodOverMaxYears = AnalyticsUtils.periodIsOutsideApprovalMaxYears( table.getPeriod(), maxYears );
            
            return setting && levels && !periodOverMaxYears;
        }
        else
        {
            return setting && levels;
        }
    }
}
