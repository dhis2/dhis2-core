package org.hisp.dhis.analytics.table;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsIndex;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableManager;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.collection.UniqueArrayList;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
public abstract class AbstractJdbcTableManager
    implements AnalyticsTableManager
{
    protected static final Log log = LogFactory.getLog( JdbcAnalyticsTableManager.class );
    
    protected static final String DATE_REGEXP = "^\\d{4}-\\d{2}-\\d{2}(\\s|T)?(\\d{2}:\\d{2}:\\d{2})?$";

    public static final String PREFIX_ORGUNITGROUPSET = "ougs_";
    public static final String PREFIX_ORGUNITLEVEL = "uidlevel";
    public static final String PREFIX_INDEX = "in_";

    @Autowired
    protected IdentifiableObjectManager idObjectManager;
   
    @Autowired
    protected OrganisationUnitService organisationUnitService;
    
    @Autowired
    protected DataElementCategoryService categoryService;
    
    @Autowired
    protected SystemSettingManager systemSettingManager;
    
    @Autowired
    protected DataApprovalLevelService dataApprovalLevelService;
    
    @Autowired
    protected ResourceTableService resourceTableService;
   
    @Autowired
    protected StatementBuilder statementBuilder;
    
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    /**
     * Returns a list of string arrays in where the first index holds the database
     * column name, the second index holds the database column data type and the 
     * third column holds a table alias and name. Column names are quoted.
     * 
     * <ul>
     * <li>0 = database column name (quoted)</li>
     * <li>1 = database column data type</li>
     * <li>2 = column alias and name</li>
     * </ul>
     */
    protected abstract List<String[]> getDimensionColumns( AnalyticsTable table );
    
    /**
     * Override to perform work before tables are being generated.
     */
    @Override
    public void preCreateTables()
    {
    }
    
    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public List<AnalyticsTable> getTables( Date earliest )
    {
        log.info( "Get tables using earliest: " + earliest );

        return getTables( getDataYears( earliest ) );
    }

    @Override
    @Transactional
    public List<AnalyticsTable> getAllTables()
    {
        return getTables( ListUtils.getClosedOpenList( 1500, 2100 ) );
    }
    
    private List<AnalyticsTable> getTables( List<Integer> dataYears )
    {
        List<AnalyticsTable> tables = new UniqueArrayList<>();
        
        Calendar calendar = PeriodType.getCalendar();

        Collections.sort( dataYears );
        
        String baseName = getTableName();
        
        for ( Integer year : dataYears )
        {
            Period period = PartitionUtils.getPeriod( calendar, year );
            
            tables.add( new AnalyticsTable( baseName, getDimensionColumns( null ), period ) );
        }

        return tables;
    }
    
    @Override
    public String getTempTableName()
    {
        return getTableName() + TABLE_TEMP_SUFFIX;
    }
    
    @Override
    @Async
    public Future<?> createIndexesAsync( ConcurrentLinkedQueue<AnalyticsIndex> indexes )
    {
        taskLoop : while ( true )
        {
            AnalyticsIndex inx = indexes.poll();
            
            if ( inx == null )
            {
                break taskLoop;
            }
            
            final String indexName = getIndexName( inx );
            
            final String sql = "create index " + indexName + " on " + inx.getTable() + " (" + inx.getColumn() + ")";
            
            log.debug( "Create index: " + indexName + " SQL: " + sql );
            
            jdbcTemplate.execute( sql );
            
            log.debug( "Created index: " + indexName );
        }
        
        return null;
    }

    @Override
    public void swapTable( AnalyticsTable table )
    {
        final String tempTable = table.getTempTableName();
        final String realTable = table.getTableName();
        
        final String sqlDrop = "drop table " + realTable;
        
        executeSilently( sqlDrop );
        
        final String sqlAlter = "alter table " + tempTable + " rename to " + realTable;
        
        executeSilently( sqlAlter );
    }

    @Override
    public void dropTable( String tableName )
    {
        final String realTable = tableName.replaceFirst( TABLE_TEMP_SUFFIX, "" );
        
        executeSilently( "drop table " + tableName );
        executeSilently( "drop table " + realTable );
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------
  
    /**
     * Quotes the given column name.
     */
    protected String quote( String column )
    {
        return statementBuilder.columnQuote( column );
    }
    
    /**
     * Remove quotes from the given column name.
     */
    private String removeQuote( String column )
    {
        return column != null ? column.replaceAll( statementBuilder.getColumnQuote(), StringUtils.EMPTY ) : null;
    }
    
    /**
     * Shortens the given table name.
     */
    private String shortenTableName( String table )
    {
        table = table.replaceAll( ANALYTICS_TABLE_NAME, "ax" );
        table = table.replaceAll( TABLE_TEMP_SUFFIX, StringUtils.EMPTY );
        
        return table;
    }
    
    /**
     * Returns index name for column. Purpose of code suffix is to avoid uniqueness
     * collision between indexes for temporary and real tables.
     */
    protected String getIndexName( AnalyticsIndex inx )
    {
        return quote( PREFIX_INDEX + removeQuote( inx.getColumn() ) + "_" + shortenTableName( inx.getTable() ) + "_" + CodeGenerator.generateCode( 5 ) );        
    }
    
    /**
     * Indicates whether the given table exists and has at least one row.
     */
    protected boolean hasRows( String tableName )
    {
        final String sql = "select * from " + tableName + " limit 1";
        
        try
        {
            return jdbcTemplate.queryForRowSet( sql ).next();
        }
        catch ( BadSqlGrammarException ex )
        {
            return false;
        }
    }
    
    /**
     * Executes a SQL statement. Ignores existing tables/indexes when attempting
     * to create new.
     */
    protected void executeSilently( String sql )
    {
        try
        {            
            jdbcTemplate.execute( sql );
        }
        catch ( BadSqlGrammarException ex )
        {
            log.debug( ex.getMessage() );
        }
    }
    
    /**
     * Checks whether the given list of dimensions are valid.
     * @throws IllegalStateException if not valid.
     */
    protected void validateDimensionColumns( List<String[]> dimensions )
    {        
        if ( dimensions == null || dimensions.isEmpty() )
        {
            throw new IllegalStateException( "Analytics table dimensions are empty" );
        }
        
        dimensions = new ArrayList<>( dimensions );
        
        List<String> columns = new ArrayList<>();
        
        for ( String[] dimension : dimensions )
        {
            columns.add( dimension[0] );
        }
        
        Set<String> duplicates = ListUtils.getDuplicates( columns );
        
        if ( !duplicates.isEmpty() )
        {
            throw new IllegalStateException( "Analytics table dimensions contain duplicates: " + duplicates );
        }
    }

    /**
     * Executes the given table population SQL statement, log and times the operation.
     */
    protected void populateAndLog( String sql, String tableName )
    {
        log.debug( "Populate table: " + tableName + " SQL: " + sql );

        Timer timer = new SystemTimer().start();
        
        jdbcTemplate.execute( sql );
        
        log.info( "Populated " + tableName + ": " + timer.stop().toString() );
    }
}
