package org.hisp.dhis.analytics.table;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class JdbcCompletenessTargetTableManager
    extends AbstractJdbcTableManager
{
    @Override
    @Transactional
    public List<AnalyticsTable> getTables( Date earliest )
    {
        List<AnalyticsTable> tables = new ArrayList<>();
        tables.add( new AnalyticsTable( getTableName(), getDimensionColumns( null ) ) );
        return tables;
    }
    
    @Override
    public List<AnalyticsTable> getAllTables()
    {
        return getTables( null );
    }
    
    @Override
    public String validState()
    {
        return null;
    }    
    
    @Override
    public String getTableName()
    {
        return COMPLETENESS_TARGET_TABLE_NAME;
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
        
        sqlCreate += "value double precision) ";
        
        sqlCreate += statementBuilder.getTableOptions( false );

        log.info( "Creating table: " + tableName + ", columns: " + columns.size() );
        
        log.debug( "Create SQL: " + sqlCreate );
        
        jdbcTemplate.execute( sqlCreate );
    }

    @Override
    @Async
    public Future<?> populateTableAsync( ConcurrentLinkedQueue<AnalyticsTable> tables )
    {
        taskLoop: while ( true )
        {
            AnalyticsTable table = tables.poll();
                
            if ( table == null )
            {
                break taskLoop;
            }

            final String tableName = table.getTempTableName();

            String sql = "insert into " + table.getTempTableName() + " (";

            List<AnalyticsTableColumn> columns = getDimensionColumns( table );
            
            validateDimensionColumns( columns );

            for ( AnalyticsTableColumn col : columns )
            {
                sql += col.getName() + ",";
            }

            sql += "value) select ";

            for ( AnalyticsTableColumn col : columns )
            {
                sql += col.getAlias() + ",";
            }
            
            sql += 
                "1 as value " +
                "from _datasetorganisationunitcategory doc " +
                "inner join dataset ds on doc.datasetid=ds.datasetid " +
                "inner join organisationunit ou on doc.organisationunitid=ou.organisationunitid " +
                "left join _orgunitstructure ous on doc.organisationunitid=ous.organisationunitid " +
                "left join _organisationunitgroupsetstructure ougs on doc.organisationunitid=ougs.organisationunitid " +
                "left join _categorystructure acs on doc.attributeoptioncomboid=acs.categoryoptioncomboid ";

            populateAndLog( sql, tableName );
        }
        
        return null;
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
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "ougs." + quote( groupSet.getUid() ) ) );
        }
        
        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            columns.add( new AnalyticsTableColumn( column, "character(11)", "ous." + column ) );
        }

        for ( CategoryOptionGroupSet groupSet : attributeCategoryOptionGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "acs." + quote( groupSet.getUid() ) ) );
        }

        for ( DataElementCategory category : attributeCategories )
        {
            columns.add( new AnalyticsTableColumn( quote( category.getUid() ), "character(11)", "acs." + quote( category.getUid() ) ) );
        }

        AnalyticsTableColumn ouOpening = new AnalyticsTableColumn( quote( "ouopeningdate"), "date", "ou.openingdate" );
        AnalyticsTableColumn ouClosed = new AnalyticsTableColumn( quote( "oucloseddate"), "date", "ou.closeddate" );
        AnalyticsTableColumn coStart = new AnalyticsTableColumn( quote( "costartdate" ), "date", "doc.costartdate" );
        AnalyticsTableColumn coEnd = new AnalyticsTableColumn( quote( "coenddate" ), "date", "doc.coenddate" );
        AnalyticsTableColumn ds = new AnalyticsTableColumn( quote( "dx" ), "character(11) not null", "ds.uid" );
        
        columns.addAll( Lists.newArrayList( ouOpening, ouClosed, coStart, coEnd, ds ) );
        
        return columns;
    }

    @Override
    public List<Integer> getDataYears( Date earliest )
    {
        return null; // Not relevant
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
