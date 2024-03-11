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
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.analytics.AnalyticsTablePartition;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.ConcurrentUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.system.util.DateUtils.getLongDateString;

/**
 * @author Lars Helge Overland
 */
public class JdbcCompletenessTableManager
    extends AbstractJdbcTableManager
{
    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.COMPLETENESS;
    }

    @Override
    @Transactional
    public List<AnalyticsTable> getAnalyticsTables( Date earliest )
    {
        AnalyticsTable table = getAnalyticsTable( getDataYears( earliest ), getDimensionColumns(), getValueColumns() );

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
        boolean hasData = jdbcTemplate.queryForRowSet( "select datasetid from completedatasetregistration limit 1" ).next();

        if ( !hasData )
        {
            return "No complete registrations exist, not updating completeness analytics tables";
        }

        return null;
    }

    @Override
    protected List<String> getPartitionChecks( AnalyticsTablePartition partition )
    {
        return Lists.newArrayList(
            "year = " + partition.getYear() + "" );
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

        select = select.replace( "organisationunitid", "sourceid" ); // Database legacy fix

        select +=
            "cdr.date as value " +
            "from completedatasetregistration cdr " +
            "inner join dataset ds on cdr.datasetid=ds.datasetid " +
            "inner join period pe on cdr.periodid=pe.periodid " +
            "inner join _periodstructure ps on cdr.periodid=ps.periodid " +
            "inner join _organisationunitgroupsetstructure ougs on cdr.sourceid=ougs.organisationunitid " +
                "and (cast(date_trunc('month', pe.startdate) as date)=ougs.startdate or ougs.startdate is null) " +
            "left join _orgunitstructure ous on cdr.sourceid=ous.organisationunitid " +
            "inner join _categorystructure acs on cdr.attributeoptioncomboid=acs.categoryoptioncomboid " +
            "where ps.year = " + partition.getYear() + " " +
            "and cdr.date <= '" + getLongDateString( params.getStartTime() ) + "' " +
            "and cdr.date is not null";

        final String sql = insert + select;

        populateAndLog( sql, tableName );
    }

    private List<AnalyticsTableColumn> getDimensionColumns()
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        List<OrganisationUnitGroupSet> orgUnitGroupSets =
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        List<OrganisationUnitLevel> levels =
            organisationUnitService.getFilledOrganisationUnitLevels();

        List<CategoryOptionGroupSet> attributeCategoryOptionGroupSets =
            categoryService.getAttributeCategoryOptionGroupSetsNoAcl();

        List<Category> attributeCategories =
            categoryService.getAttributeDataDimensionCategoriesNoAcl();

        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "ougs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            columns.add( new AnalyticsTableColumn( column, "character(11)", "ous." + column, level.getCreated() ) );
        }

        for ( CategoryOptionGroupSet groupSet : attributeCategoryOptionGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "acs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

        for ( Category category : attributeCategories )
        {
            columns.add( new AnalyticsTableColumn( quote( category.getUid() ), "character(11)", "acs." + quote( category.getUid() ), category.getCreated() ) );
        }

        for ( PeriodType periodType : PeriodType.getAvailablePeriodTypes() )
        {
            String column = quote( periodType.getName().toLowerCase() );
            columns.add( new AnalyticsTableColumn( column, "text", "ps." + column ) );
        }

        String timelyDateDiff = statementBuilder.getDaysBetweenDates( "pe.enddate", statementBuilder.getCastToDate( "cdr.date" ) );
        String timelyAlias = "(select (" + timelyDateDiff + ") <= ds.timelydays) as timely";

        columns.add( new AnalyticsTableColumn( quote( "timely" ), "boolean", timelyAlias ) );
        columns.add( new AnalyticsTableColumn( quote( "dx" ), "character(11) not null", "ds.uid" ) );
        columns.add( new AnalyticsTableColumn( quote( "year" ), "integer not null", "ps.year" ) );

        return filterDimensionColumns( columns );
    }

    private List<AnalyticsTableColumn> getValueColumns()
    {
        return Lists.newArrayList( new AnalyticsTableColumn( quote( "value" ), "date", "value" ) );
    }

    private List<Integer> getDataYears( Date earliest )
    {
        String sql =
            "select distinct(extract(year from pe.startdate)) " +
            "from completedatasetregistration cdr " +
            "inner join period pe on cdr.periodid=pe.periodid " +
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
        return ConcurrentUtils.getImmediateFuture();
    }

    @Override
    @Async
    public Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTablePartition> partitions )
    {
        return ConcurrentUtils.getImmediateFuture();
    }
}
