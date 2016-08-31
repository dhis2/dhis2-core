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
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
public class JdbcOrgUnitTargetTableManager
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
        return ORGUNIT_TARGET_TABLE_NAME;
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
                "from orgunitgroupmembers ougm " +
                "inner join orgunitgroup oug on ougm.orgunitgroupid=oug.orgunitgroupid " +
                "left join _orgunitstructure ous on ougm.organisationunitid=ous.organisationunitid " +
                "left join _organisationunitgroupsetstructure ougs on ougm.organisationunitid=ougs.organisationunitid";            

            populateAndLog( sql, tableName );
        }
        
        return null;
    }

    @Override
    public List<AnalyticsTableColumn> getDimensionColumns( AnalyticsTable table )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        List<OrganisationUnitLevel> levels =
            organisationUnitService.getFilledOrganisationUnitLevels();
                
        for ( OrganisationUnitLevel level : levels )
        {
            String column = quote( PREFIX_ORGUNITLEVEL + level.getLevel() );
            columns.add( new AnalyticsTableColumn( column, "character(11)", "ous." + column ) );
        }

        AnalyticsTableColumn ds = new AnalyticsTableColumn( quote( "oug" ), "character(11) not null", "oug.uid" );
        
        columns.add( ds );
        
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
