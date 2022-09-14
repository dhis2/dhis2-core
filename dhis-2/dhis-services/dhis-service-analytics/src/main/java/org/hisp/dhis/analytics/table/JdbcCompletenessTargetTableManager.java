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
import static org.hisp.dhis.analytics.ColumnDataType.DATE;
import static org.hisp.dhis.analytics.ColumnDataType.DOUBLE;
import static org.hisp.dhis.analytics.ColumnNotNullConstraint.NOT_NULL;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;

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
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.CompletenessTargetTableManager" )
public class JdbcCompletenessTargetTableManager
    extends AbstractJdbcTableManager
{
    public JdbcCompletenessTargetTableManager( IdentifiableObjectManager idObjectManager,
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

    private static final List<AnalyticsTableColumn> FIXED_COLS = ImmutableList.of(
        new AnalyticsTableColumn( quote( "ouopeningdate" ), DATE, "ou.openingdate" ),
        new AnalyticsTableColumn( quote( "oucloseddate" ), DATE, "ou.closeddate" ),
        new AnalyticsTableColumn( quote( "costartdate" ), DATE, "doc.costartdate" ),
        new AnalyticsTableColumn( quote( "coenddate" ), DATE, "doc.coenddate" ),
        new AnalyticsTableColumn( quote( "dx" ), CHARACTER_11, NOT_NULL, "ds.uid" ),
        new AnalyticsTableColumn( quote( "ao" ), CHARACTER_11, NOT_NULL, "ao.uid" ) );

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.COMPLETENESS_TARGET;
    }

    @Override
    @Transactional
    public List<AnalyticsTable> getAnalyticsTables( AnalyticsTableUpdateParams params )
    {
        return params.isLatestUpdate() ? Lists.newArrayList()
            : Lists.newArrayList(
                new AnalyticsTable( getAnalyticsTableType(), getDimensionColumns(), getValueColumns() ) );
    }

    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return Sets.newHashSet( getTableName() );
    }

    @Override
    public String validState()
    {
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
        return emptyList();
    }

    @Override
    protected void populateTable( AnalyticsTableUpdateParams params, AnalyticsTablePartition partition )
    {
        final String tableName = partition.getTempTableName();

        String sql = "insert into " + tableName + " (";

        List<AnalyticsTableColumn> columns = partition.getMasterTable().getDimensionColumns();
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

        sql += "1 as value " +
            "from _datasetorganisationunitcategory doc " +
            "inner join dataset ds on doc.datasetid=ds.datasetid " +
            "inner join organisationunit ou on doc.organisationunitid=ou.organisationunitid " +
            "left join _orgunitstructure ous on doc.organisationunitid=ous.organisationunitid " +
            "left join _organisationunitgroupsetstructure ougs on doc.organisationunitid=ougs.organisationunitid " +
            "left join categoryoptioncombo ao on doc.attributeoptioncomboid=ao.categoryoptioncomboid " +
            "left join _categorystructure acs on doc.attributeoptioncomboid=acs.categoryoptioncomboid ";

        invokeTimeAndLog( sql, String.format( "Populate %s", tableName ) );
    }

    private List<AnalyticsTableColumn> getDimensionColumns()
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        List<OrganisationUnitGroupSet> orgUnitGroupSets = idObjectManager
            .getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        List<OrganisationUnitLevel> levels = organisationUnitService.getFilledOrganisationUnitLevels();

        List<CategoryOptionGroupSet> attributeCategoryOptionGroupSets = categoryService
            .getAttributeCategoryOptionGroupSetsNoAcl();

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

        for ( CategoryOptionGroupSet groupSet : attributeCategoryOptionGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), CHARACTER_11,
                "acs." + quote( groupSet.getUid() ) ).withCreated( groupSet.getCreated() ) );
        }

        for ( Category category : attributeCategories )
        {
            columns.add( new AnalyticsTableColumn( quote( category.getUid() ), CHARACTER_11,
                "acs." + quote( category.getUid() ) ).withCreated( category.getCreated() ) );
        }

        columns.addAll( getFixedColumns() );

        return filterDimensionColumns( columns );
    }

    private List<AnalyticsTableColumn> getValueColumns()
    {
        return Lists.newArrayList( new AnalyticsTableColumn( quote( "value" ), DOUBLE, "value" ) );
    }

    @Override
    public List<AnalyticsTableColumn> getFixedColumns()
    {
        return FIXED_COLS;
    }
}
