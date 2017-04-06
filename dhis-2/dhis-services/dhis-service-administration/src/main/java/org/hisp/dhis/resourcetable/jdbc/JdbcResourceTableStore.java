package org.hisp.dhis.resourcetable.jdbc;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableStore;
import org.hisp.dhis.system.util.Clock;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

/**
 * @author Lars Helge Overland
 */
public class JdbcResourceTableStore
    implements ResourceTableStore
{
    private static final Log log = LogFactory.getLog( JdbcResourceTableStore.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    private StatementBuilder statementBuilder;
    
    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }

    private DbmsManager dbmsManager;

    public void setDbmsManager( DbmsManager dbmsManager )
    {
        this.dbmsManager = dbmsManager;
    }

    // -------------------------------------------------------------------------
    // ResourceTableStore implementation
    // -------------------------------------------------------------------------

    public void generateResourceTable( ResourceTable<?> resourceTable )
    {
        final Clock clock = new Clock().startClock();
        final String createTableSql = resourceTable.getCreateTempTableStatement();
        final Optional<String> populateTableSql = resourceTable.getPopulateTempTableStatement();
        final Optional<List<Object[]>> populateTableContent = resourceTable.getPopulateTempTableContent();
        final List<String> createIndexSql = resourceTable.getCreateIndexStatements();
        final String analyzeTableSql = statementBuilder.getAnalyze( resourceTable.getTableName() );

        // ---------------------------------------------------------------------
        // Drop temporary table if it exists
        // ---------------------------------------------------------------------

        if ( dbmsManager.tableExists( resourceTable.getTempTableName() ) )
        {
            jdbcTemplate.execute( resourceTable.getDropTempTableStatement() );
        }
                
        // ---------------------------------------------------------------------
        // Create temporary table
        // ---------------------------------------------------------------------

        log.info( "Create table SQL: " + createTableSql );
        
        jdbcTemplate.execute( createTableSql );

        // ---------------------------------------------------------------------
        // Populate temporary table through SQL or object batch update
        // ---------------------------------------------------------------------

        if ( populateTableSql.isPresent() )
        {
            log.info( "Populate table SQL: " + populateTableSql.get() );
            
            jdbcTemplate.execute( populateTableSql.get() );
        }
        else if ( populateTableContent.isPresent() )
        {
            List<Object[]> content = populateTableContent.get();
            
            log.info( "Populate table content rows: " + content.size() );
            
            if ( content.size() > 0 )
            {
                int columns = content.get( 0 ).length;
                
                batchUpdate( columns, resourceTable.getTempTableName(), content );
            }
        }

        // ---------------------------------------------------------------------
        // Create indexes
        // ---------------------------------------------------------------------

        for ( final String sql : createIndexSql )
        {
            log.info( "Create index SQL: " + sql );
            
            jdbcTemplate.execute( sql );
        }
        
        // ---------------------------------------------------------------------
        // Swap tables
        // ---------------------------------------------------------------------

        if ( dbmsManager.tableExists( resourceTable.getTableName() ) )
        {
            jdbcTemplate.execute( resourceTable.getDropTableStatement() );
        }
        
        jdbcTemplate.execute( resourceTable.getRenameTempTableStatement() );
        
        log.info( "Swapped resource table: " + resourceTable.getTableName() );

        // ---------------------------------------------------------------------
        // Analyze
        // ---------------------------------------------------------------------

        if ( analyzeTableSql != null )
        {
            log.info( "Analyze table SQL: " + analyzeTableSql );
            
            jdbcTemplate.execute( analyzeTableSql );
        }
        
        log.info( "Analyzed resource table: " + resourceTable.getTableName() + ", done in: " + clock.time() );
    }
    
    @Override
    public void batchUpdate( int columns, String tableName, List<Object[]> batchArgs )
    {
        if ( columns == 0 || tableName == null )
        {
            return;
        }
        
        StringBuilder builder = new StringBuilder( "insert into " + tableName + " values (" );
        
        for ( int i = 0; i < columns; i++ )
        {
            builder.append( "?," );
        }
        
        builder.deleteCharAt( builder.length() - 1 ).append( ")" );
        
        jdbcTemplate.batchUpdate( builder.toString(), batchArgs );
    }
}
