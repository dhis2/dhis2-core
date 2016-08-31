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
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.scheduling.annotation.Async;

/**
 * @author Lars Helge Overland
 */
public class JdbcCompletenessTableManager
    extends AbstractJdbcTableManager
{
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
    public String getTableName()
    {
        return COMPLETENESS_TABLE_NAME;
    }
    
    @Override
    public void createTable( AnalyticsTable table )
    {
        final String tableName = table.getTempTableName();
        
        final String sqlDrop = "drop table " + tableName;
        
        executeSilently( sqlDrop );

        String sqlCreate = "create table " + tableName + " (";

        List<String[]> columns = getDimensionColumns( table );
        
        validateDimensionColumns( columns );

        for ( String[] col : columns )
        {
            sqlCreate += col[0] + " " + col[1] + ",";
        }
        
        sqlCreate += "value date) ";
        
        sqlCreate += statementBuilder.getTableOptions( false );

        log.info( "Creating table: " + tableName + ", columns: " + columns.size() );
        
        log.debug( "Create SQL: " + sqlCreate );
        
        jdbcTemplate.execute( sqlCreate );
    }
    
    @Override
    @Async
    public Future<?> populateTableAsync( ConcurrentLinkedQueue<AnalyticsTable> tables )
    {
        taskLoop : while ( true )
        {
            AnalyticsTable table = tables.poll();
                
            if ( table == null )
            {
                break taskLoop;
            }
            
            final String start = DateUtils.getMediumDateString( table.getPeriod().getStartDate() );
            final String end = DateUtils.getMediumDateString( table.getPeriod().getEndDate() );
            final String tableName = table.getTempTableName();

            String insert = "insert into " + table.getTempTableName() + " (";

            List<String[]> columns = getDimensionColumns( table );
            
            validateDimensionColumns( columns );

            for ( String[] col : columns )
            {
                insert += col[0] + ",";
            }
            
            insert += "value) ";
            
            String select = "select ";
            
            for ( String[] col : columns )
            {
                select += col[2] + ",";
            }
            
            select = select.replace( "organisationunitid", "sourceid" ); // Legacy fix TODO remove
            
            select += 
                "cdr.date as value " +
                "from completedatasetregistration cdr " +
                "left join _organisationunitgroupsetstructure ougs on cdr.sourceid=ougs.organisationunitid " +
                "left join _orgunitstructure ous on cdr.sourceid=ous.organisationunitid " +
                "inner join period pe on cdr.periodid=pe.periodid " +
                "left join _periodstructure ps on cdr.periodid=ps.periodid " +
                "inner join dataset ds on cdr.datasetid=ds.datasetid " +
                "where pe.startdate >= '" + start + "' " +
                "and pe.startdate <= '" + end + "'" +
                "and cdr.date is not null";
    
            final String sql = insert + select;
            
            populateAndLog( sql, tableName );
        }
        
        return null;
    }
    
    @Override
    public List<String[]> getDimensionColumns( AnalyticsTable table )
    {
        List<String[]> columns = new ArrayList<>();

        Collection<OrganisationUnitGroupSet> orgUnitGroupSets = 
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );
        
        Collection<OrganisationUnitLevel> levels =
            organisationUnitService.getFilledOrganisationUnitLevels();
        
        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            String[] col = { quote( groupSet.getUid() ), "character(11)", "ougs." + quote( groupSet.getUid() ) };
            columns.add( col );
        }
        
        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            String[] col = { column, "character(11)", "ous." + column };
            columns.add( col );
        }
        
        for ( PeriodType periodType : PeriodType.getAvailablePeriodTypes() )
        {
            String column = quote( periodType.getName().toLowerCase() );
            String[] col = { column, "character varying(15)", "ps." + column };
            columns.add( col );
        }
        
        String[] ds = { quote( "dx" ), "character(11) not null", "ds.uid" };
        
        columns.add( ds );
        
        return columns;
    }

    @Override
    public List<Integer> getDataYears( Date earliest )
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
