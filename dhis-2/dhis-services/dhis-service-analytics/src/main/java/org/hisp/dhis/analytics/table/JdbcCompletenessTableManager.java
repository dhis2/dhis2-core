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
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

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
    public List<AnalyticsTable> getTables( Date earliest )
    {
        log.info( "Get tables using earliest: " + earliest );

        return getTables( getDataYears( earliest ) );
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
    public void createTable( AnalyticsTable table )
    {
        final String tableName = table.getTempTableName();
        
        final String sqlDrop = "drop table " + tableName;
        
        executeSilently( sqlDrop );

        String sqlCreate = "create table " + tableName + " (";

        List<AnalyticsTableColumn> columns = getDimensionColumns( table );
        
        validateDimensionColumns( columns );

        for ( AnalyticsTableColumn col : columns )
        {
            sqlCreate += col.getName() + " " + col.getDataType() + ",";
        }
        
        sqlCreate += "value date)";
        
        log.info( "Creating table: " + tableName + ", columns: " + columns.size() );
        
        log.debug( "Create SQL: " + sqlCreate );
        
        jdbcTemplate.execute( sqlCreate );
    }
    
    @Override
    protected void populateTable( AnalyticsTable table )
    {
        final String start = DateUtils.getMediumDateString( table.getPeriod().getStartDate() );
        final String end = DateUtils.getMediumDateString( table.getPeriod().getEndDate() );
        final String tableName = table.getTempTableName();

        String insert = "insert into " + table.getTempTableName() + " (";

        List<AnalyticsTableColumn> columns = getDimensionColumns( table );
        
        validateDimensionColumns( columns );
        
        for ( AnalyticsTableColumn col : columns )
        {
            insert += col.getName() + ",";
        }
        
        insert += "value) ";

        String select = "select ";
        
        for ( AnalyticsTableColumn col : columns )
        {
            select += col.getAlias() + ",";
        }
        
        select = select.replace( "organisationunitid", "sourceid" ); // Legacy fix
        
        select +=
            "cdr.date as value " +
            "from completedatasetregistration cdr " +
            "inner join dataset ds on cdr.datasetid=ds.datasetid " +
            "inner join _organisationunitgroupsetstructure ougs on cdr.sourceid=ougs.organisationunitid " +
            "left join _orgunitstructure ous on cdr.sourceid=ous.organisationunitid " +
            "inner join _categorystructure acs on cdr.attributeoptioncomboid=acs.categoryoptioncomboid " +
            "inner join period pe on cdr.periodid=pe.periodid " +
            "inner join _periodstructure ps on cdr.periodid=ps.periodid " +
            "where pe.startdate >= '" + start + "' " +
            "and pe.startdate <= '" + end + "' " +
            "and cdr.date is not null";

        final String sql = insert + select;
        
        populateAndLog( sql, tableName );
    }
    
    @Override
    public List<AnalyticsTableColumn> getDimensionColumns( AnalyticsTable table )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        List<OrganisationUnitGroupSet> orgUnitGroupSets = 
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );
        
        List<OrganisationUnitLevel> levels =
            organisationUnitService.getFilledOrganisationUnitLevels();

        List<CategoryOptionGroupSet> attributeCategoryOptionGroupSets =
            categoryService.getAttributeCategoryOptionGroupSetsNoAcl();

        List<DataElementCategory> attributeCategories =
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

        for ( DataElementCategory category : attributeCategories )
        {
            columns.add( new AnalyticsTableColumn( quote( category.getUid() ), "character(11)", "acs." + quote( category.getUid() ), category.getCreated() ) );
        }
        
        for ( PeriodType periodType : PeriodType.getAvailablePeriodTypes() )
        {
            String column = quote( periodType.getName().toLowerCase() );
            columns.add( new AnalyticsTableColumn( column, "character varying(15)", "ps." + column ) );
        }
        
        String timelyDateDiff = statementBuilder.getDaysBetweenDates( "pe.enddate", statementBuilder.getCastToDate( "cdr.date" ) );        
        String timelyAlias = "(select (" + timelyDateDiff + ") <= ds.timelydays) as timely";
        
        AnalyticsTableColumn tm = new AnalyticsTableColumn( quote( "timely" ), "boolean", timelyAlias );

        AnalyticsTableColumn ds = new AnalyticsTableColumn( quote( "dx" ), "character(11) not null", "ds.uid" );
        
        columns.addAll( Lists.newArrayList( ds, tm ) );
        
        return filterDimensionColumns( columns );
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
    public Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTable> tables, Collection<String> dataElements, int aggregationLevel )
    {
        return null; // Not relevant
    }

    @Override
    @Async
    public Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTable> tables )
    {
        return null; // Not needed
    }
}
