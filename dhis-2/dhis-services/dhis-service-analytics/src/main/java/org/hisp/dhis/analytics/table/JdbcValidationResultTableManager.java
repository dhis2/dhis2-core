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

import static org.hisp.dhis.analytics.ColumnDataType.CHARACTER_11;
import static org.hisp.dhis.analytics.ColumnDataType.DATE;
import static org.hisp.dhis.analytics.ColumnDataType.INTEGER;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.ColumnDataType.TIMESTAMP;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.util.DateUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Henning Håkonsen
 */
@Service( "org.hisp.dhis.analytics.ValidationResultAnalyticsTableManager" )
public class JdbcValidationResultTableManager
    extends AbstractJdbcTableManager
{
    private static final List<AnalyticsTableColumn> FIXED_COLS = ImmutableList.of(
        new AnalyticsTableColumn( quote( "dx" ), CHARACTER_11, NOT_NULL, "vr.uid" ),
        new AnalyticsTableColumn( quote( "pestartdate" ), TIMESTAMP, "pe.startdate" ),
        new AnalyticsTableColumn( quote( "peenddate" ), TIMESTAMP, "pe.enddate" ),
        new AnalyticsTableColumn( quote( "year" ), INTEGER, NOT_NULL, "ps.year" ) );

    public JdbcValidationResultTableManager( IdentifiableObjectManager idObjectManager,
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

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.VALIDATION_RESULT;
    }

    @Override
    public List<AnalyticsTable> getAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        AnalyticsTable table = params.isLatestUpdate() ? new AnalyticsTable()
            : getRegularAnalyticsTable( params, getDataYears( params ), getDimensionColumns(), getValueColumns() );

        return table.hasPartitionTables() ? Lists.newArrayList( table ) : Lists.newArrayList();
    }

    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return Sets.newHashSet( getTableName() );
    }

    @Override
    public String validState()
    {
        boolean hasData = jdbcTemplate.queryForRowSet( "select validationresultid from validationresult limit 1" )
            .next();

        if ( !hasData )
        {
            return "No validation results exist, not updating validation result analytics tables";
        }

        return null;
    }

    @Override
    protected boolean hasUpdatedLatestData( Date startDate, Date endDate )
    {
        return false;
    }

    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return Lists.newArrayList(
            "year = " + partition.getYear() + "" );
    }

    @Override
    protected String getPartitionColumn()
    {
        return "year";
    }

    @Override
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        final String tableName = partition.getTempTableName();

        String insert = "insert into " + partition.getTempTableName() + " (";

        List<AnalyticsTableColumn> columns = partition.getMasterTable().getDimensionColumns();
        List<AnalyticsTableColumn> values = partition.getMasterTable().getValueColumns();

        validateDimensionColumns( columns );

        for ( AnalyticsTableColumn col : ListUtils.union( columns, values ) )
        {
            insert += col.getName() + ",";
        }

        insert = TextUtils.removeLastComma( insert ) + ") ";

        String select = "select ";

        for ( AnalyticsTableColumn col : columns )
        {
            select += col.getAlias() + ",";
        }

        // Database legacy fix

        select = select.replace( "organisationunitid", "sourceid" );

        select += "vrs.created as value " +
            "from validationresult vrs " +
            "inner join period pe on vrs.periodid=pe.periodid " +
            "inner join _periodstructure ps on vrs.periodid=ps.periodid " +
            "inner join validationrule vr on vr.validationruleid=vrs.validationruleid " +
            "inner join _organisationunitgroupsetstructure ougs on vrs.organisationunitid=ougs.organisationunitid " +
            "and (cast(date_trunc('month', pe.startdate) as date)=ougs.startdate or ougs.startdate is null) " +
            "left join _orgunitstructure ous on vrs.organisationunitid=ous.organisationunitid " +
            "inner join _categorystructure acs on vrs.attributeoptioncomboid=acs.categoryoptioncomboid " +
            "where ps.year = " + partition.getYear() + " " +
            "and vrs.created < '" + getLongDateString( params.getStartTime() ) + "' " +
            "and vrs.created is not null";

        final String sql = insert + select;

        invokeTimeAndLog( sql, String.format( "Populate %s", tableName ) );
    }

    private List<Integer> getDataYears( AnalyticsTableUpdateParams params )
    {
        String sql = "select distinct(extract(year from pe.startdate)) " +
            "from validationresult vrs " +
            "inner join period pe on vrs.periodid=pe.periodid " +
            "where pe.startdate is not null " +
            "and vrs.created < '" + getLongDateString( params.getStartTime() ) + "' ";

        if ( params.getFromDate() != null )
        {
            sql += "and pe.startdate >= '" + DateUtils.getMediumDateString( params.getFromDate() ) + "'";
        }

        return jdbcTemplate.queryForList( sql, Integer.class );
    }

    private List<AnalyticsTableColumn> getDimensionColumns()
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        List<OrganisationUnitGroupSet> orgUnitGroupSets = idObjectManager
            .getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        List<OrganisationUnitLevel> levels = organisationUnitService.getFilledOrganisationUnitLevels();

        List<Category> attributeCategories = categoryService.getAttributeDataDimensionCategoriesNoAcl();

        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), CHARACTER_11,
                "ougs." + quote( groupSet.getUid() ) ).withCreated( groupSet.getCreated() ) );
        }

        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            columns.add(
                new AnalyticsTableColumn( column, CHARACTER_11, "ous." + column ).withCreated( level.getCreated() ) );
        }

        for ( Category category : attributeCategories )
        {
            columns.add( new AnalyticsTableColumn( quote( category.getUid() ), CHARACTER_11,
                "acs." + quote( category.getUid() ) ).withCreated( category.getCreated() ) );
        }

        for ( PeriodType periodType : PeriodType.getAvailablePeriodTypes() )
        {
            String column = quote( periodType.getName().toLowerCase() );
            columns.add( new AnalyticsTableColumn( column, TEXT, "ps." + column ) );
        }

        columns.addAll( getFixedColumns() );
        return filterDimensionColumns( columns );
    }

    private List<AnalyticsTableColumn> getValueColumns()
    {
        return Lists.newArrayList( new AnalyticsTableColumn( quote( "value" ), DATE, "value" ) );
    }

    @Override
    public List<AnalyticsTableColumn> getFixedColumns()
    {
        return FIXED_COLS;
    }
}
