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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ConcurrentUtils;
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
 * table designed for analysis which contains raw data values.
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
    
    @Autowired
    private PartitionManager partitionManager;

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
    public List<AnalyticsTable> getAnalyticsTables( Date earliest )
    {
        AnalyticsTable table = getAnalyticsTable( getDataYears( earliest ), getDimensionColumns( null ), getValueColumns() );
        
        return table.hasPartitionTables() ? Lists.newArrayList( table ) : Lists.newArrayList();
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
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return Lists.newArrayList(
            "yearly = '" + partition.getYear() + "'",
            "pestartdate < '" + DateUtils.getMediumDateString( partition.getEndDate() ) + "'" );
    }
    
    @Override
    protected void populateTable( AnalyticsTablePartition partition )
    {
        final String dbl = statementBuilder.getDoubleColumnType();
        final boolean skipDataTypeValidation = (Boolean) systemSettingManager.getSystemSetting( SettingKey.SKIP_DATA_TYPE_VALIDATION_IN_ANALYTICS_TABLE_EXPORT );

        final String approvalClause = getApprovalJoinClause( partition.getYear() );
        final String numericClause = skipDataTypeValidation ? "" : ( "and dv.value " + statementBuilder.getRegexpMatch() + " '" + MathUtils.NUMERIC_LENIENT_REGEXP + "' " );

        String intClause =
            "( dv.value != '0' or de.aggregationtype in ('" + AggregationType.AVERAGE + ',' + AggregationType.AVERAGE_SUM_ORG_UNIT + "') or de.zeroissignificant = true ) " +
            numericClause;

        populateTable( partition, "cast(dv.value as " + dbl + ")", "null", ValueType.NUMERIC_TYPES, intClause, approvalClause );

        populateTable( partition, "1", "null", Sets.newHashSet( ValueType.BOOLEAN, ValueType.TRUE_ONLY ), "dv.value = 'true'", approvalClause );

        populateTable( partition, "0", "null", Sets.newHashSet( ValueType.BOOLEAN ), "dv.value = 'false'", approvalClause );

        populateTable( partition, "null", "dv.value", Sets.union( ValueType.TEXT_TYPES, ValueType.DATE_TYPES ), null, approvalClause );
    }

    /**
     * Populates the given analytics table.
     *
     * @param table analytics table to populate.
     * @param valueExpression numeric value expression.
     * @param textValueExpression textual value expression.
     * @param valueTypes data element value types to include data for.
     * @param whereClause where clause to constrain data query.
     */
    private void populateTable( AnalyticsTablePartition partition, String valueExpression,
        String textValueExpression, Set<ValueType> valueTypes, String whereClause, String approvalClause )
    {
        final String tableName = partition.getTempTableName();
        final String valTypes = TextUtils.getQuotedCommaDelimitedString( ObjectUtils.asStringList( valueTypes ) );
        final boolean respectStartEndDates = (Boolean) systemSettingManager.getSystemSetting( SettingKey.RESPECT_META_DATA_START_END_DATES_IN_ANALYTICS_TABLE_EXPORT );

        String sql = "insert into " + partition.getTempTableName() + " (";

        List<AnalyticsTableColumn> columns = getDimensionColumns( partition.getYear() );
        List<AnalyticsTableColumn> values = partition.getMasterTable().getValueColumns();

        validateDimensionColumns( columns );
        
        for ( AnalyticsTableColumn col : ListUtils.union( columns, values ) )
        {
            sql += col.getName() + ",";
        }
        
        sql = TextUtils.removeLastComma( sql ) + ") select ";

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
            "inner join period pe on dv.periodid=pe.periodid " +
            "inner join _periodstructure ps on dv.periodid=ps.periodid " +
            "inner join dataelement de on dv.dataelementid=de.dataelementid " +
            "inner join _dataelementstructure des on dv.dataelementid = des.dataelementid " +
            "inner join _dataelementgroupsetstructure degs on dv.dataelementid=degs.dataelementid " +
            "inner join organisationunit ou on dv.sourceid=ou.organisationunitid " +
            "left join _orgunitstructure ous on dv.sourceid=ous.organisationunitid " +
            "inner join _organisationunitgroupsetstructure ougs on dv.sourceid=ougs.organisationunitid " +
                "and (cast(date_trunc('month', pe.startdate) as date)=ougs.startdate or ougs.startdate is null) " +
            "inner join categoryoptioncombo co on dv.categoryoptioncomboid=co.categoryoptioncomboid " +
            "inner join categoryoptioncombo ao on dv.attributeoptioncomboid=ao.categoryoptioncomboid " +
            "inner join _categorystructure dcs on dv.categoryoptioncomboid=dcs.categoryoptioncomboid " +
            "inner join _categorystructure acs on dv.attributeoptioncomboid=acs.categoryoptioncomboid " +
            "inner join _categoryoptioncomboname aon on dv.attributeoptioncomboid=aon.categoryoptioncomboid " +
            "inner join _categoryoptioncomboname con on dv.categoryoptioncomboid=con.categoryoptioncomboid " +

            approvalClause +
            "where de.valuetype in (" + valTypes + ") " +
            "and de.domaintype = 'AGGREGATE' " +
            "and ps.yearly = '" + partition.getYear() + "' " +
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
     * 
     * @param year the data year.
     */
    private String getApprovalJoinClause( Integer year )
    {
        if ( isApprovalEnabled( year ) )
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
    
    private List<AnalyticsTableColumn> getDimensionColumns( Integer year )
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
            columns.add( new AnalyticsTableColumn( column, "text", "ps." + column ) );
        }

        String approvalCol = isApprovalEnabled( year ) ?
            "coalesce(des.datasetapprovallevel, aon.approvallevel, da.minlevel, " + APPROVAL_LEVEL_UNAPPROVED + ") as approvallevel " :
            DataApprovalLevelService.APPROVAL_LEVEL_HIGHEST + " as approvallevel";
        
        columns.add( new AnalyticsTableColumn( quote( "dx" ), "character(11) not null", "de.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "co" ), "character(11) not null", "co.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "ao" ), "character(11) not null", "ao.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "pestartdate" ), "timestamp", "pe.startdate" ) );
        columns.add( new AnalyticsTableColumn( quote( "peenddate" ),"timestamp", "pe.enddate" ) );
        columns.add( new AnalyticsTableColumn( quote( "pe" ), "text not null", "ps.iso" ) );
        columns.add( new AnalyticsTableColumn( quote( "ou" ), "character(11) not null", "ou.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "level" ), "integer", "ous.level" ) );
        columns.add( new AnalyticsTableColumn( quote( "approvallevel" ), "integer", approvalCol ) );

        return filterDimensionColumns( columns );
    }

    private List<AnalyticsTableColumn> getValueColumns()
    {
        final String dbl = statementBuilder.getDoubleColumnType();

        return Lists.newArrayList(
            new AnalyticsTableColumn( quote( "daysxvalue" ), dbl, "daysxvalue" ),
            new AnalyticsTableColumn( quote( "daysno" ), "integer not null", "daysno" ),
            new AnalyticsTableColumn( quote( "value" ), dbl, "value" ),
            new AnalyticsTableColumn( quote( "textvalue" ), "text", "textvalue" ) );
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
    public Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTablePartition> partitions, Collection<String> dataElements, int aggregationLevel )
    {
        taskLoop:
        while ( true )
        {
            AnalyticsTablePartition partition = partitions.poll();

            if ( partition == null )
            {
                break taskLoop;
            }

            StringBuilder sql = new StringBuilder( "update " + partition.getTempTableName() + " set " );

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

        return ConcurrentUtils.getImmediateFuture();
    }

    @Override
    @Async
    public Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTablePartition> partitions )
    {
        taskLoop:
        while ( true )
        {
            AnalyticsTablePartition partition = partitions.poll();

            if ( partition == null )
            {
                break taskLoop;
            }

            final String sql = statementBuilder.getVacuum( partition.getTempTableName() );

            log.debug( "Vacuum SQL: " + sql );

            jdbcTemplate.execute( sql );
        }

        return ConcurrentUtils.getImmediateFuture();
    }

    /**
     * Indicates whether the system should ignore data which has not been approved
     * in analytics tables.
     */
    private boolean isApprovalEnabled( Integer year )
    {
        boolean setting = systemSettingManager.hideUnapprovedDataInAnalytics();
        boolean levels = !dataApprovalLevelService.getAllDataApprovalLevels().isEmpty();
        Integer maxYears = (Integer) systemSettingManager.getSystemSetting( SettingKey.IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD );
        
        log.debug( String.format( "Hide approval setting: %b, approval levels exists: %b, max years threshold: %d", setting, levels, maxYears ) );
        
        if ( year != null )
        {
            boolean periodOverMaxYears = AnalyticsUtils.periodIsOutsideApprovalMaxYears( year, maxYears );
            
            return setting && levels && !periodOverMaxYears;
        }
        else
        {
            return setting && levels;
        }
    }
}
