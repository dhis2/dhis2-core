package org.hisp.dhis.sqlview;

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dang Duy Hieu
 */
@Transactional
public class DefaultSqlViewService
    implements SqlViewService
{
    private static final Log log = LogFactory.getLog( DefaultSqlViewService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SqlViewStore sqlViewStore;

    public void setSqlViewStore( SqlViewStore sqlViewStore )
    {
        this.sqlViewStore = sqlViewStore;
    }

    private StatementBuilder statementBuilder;

    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }

    // -------------------------------------------------------------------------
    // CRUD methods
    // -------------------------------------------------------------------------

    @Override
    public int saveSqlView( SqlView sqlView )
    {
        sqlViewStore.save( sqlView );

        return sqlView.getId();
    }

    @Override
    public void updateSqlView( SqlView sqlView )
    {
        sqlViewStore.update( sqlView );
    }

    @Override
    public void deleteSqlView( SqlView sqlView )
    {
        if ( !sqlView.isQuery() )
        {
            dropViewTable( sqlView );
        }
        
        sqlViewStore.delete( sqlView );
    }

    @Override
    public List<SqlView> getAllSqlViews()
    {
        return sqlViewStore.getAll();
    }

    @Override
    public List<SqlView> getAllSqlViewsNoAcl()
    {
        return sqlViewStore.getAllNoAcl();
    }

    @Override
    public SqlView getSqlView( int viewId )
    {
        return sqlViewStore.get( viewId );
    }

    @Override
    public SqlView getSqlViewByUid( String uid )
    {
        return sqlViewStore.getByUid( uid );
    }

    @Override
    public SqlView getSqlView( String viewName )
    {
        return sqlViewStore.getByName( viewName );
    }

    @Override
    public int getSqlViewCount()
    {
        return sqlViewStore.getCount();
    }

    @Override
    public List<SqlView> getSqlViewsBetween( int first, int max )
    {
        return sqlViewStore.getAllOrderedName( first, max );
    }

    @Override
    public List<SqlView> getSqlViewsBetweenByName( String name, int first, int max )
    {
        return sqlViewStore.getAllLikeName( name, first, max );
    }

    @Override
    public int getSqlViewCountByName( String name )
    {
        return sqlViewStore.getCountLikeName( name );
    }
    
    // -------------------------------------------------------------------------
    // Service methods
    // -------------------------------------------------------------------------

    @Override
    public boolean viewTableExists( String viewTableName )
    {
        return sqlViewStore.viewTableExists( viewTableName );
    }

    @Override
    public String createViewTable( SqlView sqlView )
    {
        validateSqlView( sqlView, null, null );
        
        return sqlViewStore.createViewTable( sqlView );
    }
    
    @Override
    public Grid getSqlViewGrid( SqlView sqlView, Map<String, String> criteria, Map<String, String> variables, List<String> filters, List<String> fields  )
    {
        validateSqlView( sqlView, criteria, variables );
        
        Grid grid = new ListGrid();
        grid.setTitle( sqlView.getName() );
        grid.setSubtitle( sqlView.getDescription() );

        validateSqlView( sqlView, criteria, variables );

        String sql = sqlView.isQuery() ?
            getSqlForQuery( grid, sqlView, criteria, variables, filters, fields ) :
            getSqlForView( grid, sqlView, criteria, filters, fields );

        sqlViewStore.populateSqlViewGrid( grid, sql );

        return grid;
    }

    private String parseFilters(List<String> filters, SqlHelper sqlHelper ) throws QueryParserException
    {
        String query = StringUtils.EMPTY;

        for ( String filter : filters )
        {
            String[] split = filter.split( ":" );

            if ( split.length == 3 )
            {
                int index = split[0].length() + ":".length() + split[1].length() + ":".length();
                query +=  getFilterQuery(sqlHelper, split[0], split[1], filter.substring( index ) ) ;
            }
            else
            {
                throw new QueryParserException( "Invalid filter => " + filter );
            }
        }

        return query;
    }

    private String getFilterQuery( SqlHelper sqlHelper, String columnName, String operator, String value ) {

        String query = StringUtils.EMPTY;

        query += sqlHelper.whereAnd() + " " + columnName + " " + QueryUtils.parseFilterOperator( operator, value );

        return query;
    }

    private String getSqlForQuery( Grid grid, SqlView sqlView, Map<String, String> criteria, Map<String, String> variables, List<String> filters, List<String> fields )
    {
        boolean hasCriteria = criteria != null && !criteria.isEmpty();

        boolean hasFilter = filters != null && !filters.isEmpty();

        String sql = SqlViewUtils.substituteSqlVariables( sqlView.getSqlQuery(), variables );

        if ( hasCriteria || hasFilter )
        {
            sql = SqlViewUtils.removeQuerySeparator( sql );

            String outerSql = "select " + QueryUtils.parseSelectFields( fields ) + " from " + "(" + sql + ") as qry ";

            SqlHelper sqlHelper = new SqlHelper();

            if ( hasCriteria )
            {
                outerSql += getCriteriaSqlClause( criteria, sqlHelper );
            }

            if ( hasFilter )
            {
                outerSql += parseFilters( filters, sqlHelper );
            }

            sql = outerSql;
        }

        return sql;
    }

    private String getSqlForView( Grid grid, SqlView sqlView, Map<String, String> criteria, List<String> filters, List<String> fields )
    {
        String sql = "select " + QueryUtils.parseSelectFields( fields ) + " from " + statementBuilder.columnQuote( sqlView.getViewName() ) + " ";

        boolean hasCriteria = criteria != null && !criteria.isEmpty();

        boolean hasFilter = filters != null && !filters.isEmpty();

        if ( hasCriteria || hasFilter )
        {
            SqlHelper sqlHelper = new SqlHelper();

            if ( hasCriteria )
            {
                sql += getCriteriaSqlClause( criteria, sqlHelper );
            }

            if ( hasFilter )
            {
                sql += parseFilters( filters, sqlHelper );
            }
        }

        return sql;
    }

    @Override
    public String getCriteriaSqlClause(Map<String, String> criteria,  SqlHelper sqlHelper )
    {
        String sql = StringUtils.EMPTY;

        if ( criteria != null && !criteria.isEmpty() )
        {
            sqlHelper = ObjectUtils.firstNonNull( sqlHelper, new SqlHelper() );

            for ( String filter : criteria.keySet() )
            {
                sql += sqlHelper.whereAnd() + " " + statementBuilder.columnQuote( filter ) + "='" + criteria.get( filter ) + "' ";
            }
        }

        return sql;
    }
    
    @Override
    public void validateSqlView( SqlView sqlView, Map<String, String> criteria, Map<String, String> variables )
        throws IllegalQueryException
    {
        String violation = null;
        
        if ( sqlView == null || sqlView.getSqlQuery() == null )
        {
            throw new IllegalQueryException( "SQL query is null" );
        }
        
        final Set<String> sqlVars = SqlViewUtils.getVariables( sqlView.getSqlQuery() );
        final String sql = sqlView.getSqlQuery().replaceAll("\\r|\\n"," ").toLowerCase();
        
        if ( !SELECT_PATTERN.matcher( sql ).matches() )
        {
            violation = "SQL query must be a select query";
        }
        
        if ( sql.contains( ";" ) && !sql.trim().endsWith( ";" ) )
        {
            violation = "SQL query can only contain a single semi-colon at the end of the query";
        }
        
        if ( variables != null && variables.keySet().contains( null ) )
        {
            violation = "Variables contains null key";
        }

        if ( variables != null && variables.values().contains( null ) )
        {
            violation = "Variables contains null value";
        }

        if ( variables != null && !SqlView.getInvalidQueryParams( variables.keySet() ).isEmpty() )
        {
            violation = "Variable params are invalid: " + SqlView.getInvalidQueryParams( variables.keySet() );
        }
        
        if ( variables != null && !SqlView.getInvalidQueryValues( variables.values() ).isEmpty() )
        {
            violation = "Variables are invalid: " + SqlView.getInvalidQueryValues( variables.values() );
        }

        if ( sqlView.isQuery() && !sqlVars.isEmpty() && ( variables == null || !variables.keySet().containsAll( sqlVars ) ) )
        {
            violation = "SQL query contains variables which were not provided in request: " + sqlVars;
        }

        if ( criteria != null && !SqlView.getInvalidQueryParams( criteria.keySet() ).isEmpty() )
        {
            violation = "Criteria params are invalid: " + SqlView.getInvalidQueryParams( criteria.keySet() );
        }
        
        if ( criteria != null && !SqlView.getInvalidQueryValues( criteria.values() ).isEmpty() )
        {
            violation = "Criteria values are invalid: " + SqlView.getInvalidQueryValues( criteria.values() );
        }

        if ( sql.matches( SqlView.getProtectedTablesRegex() ) )
        {
            violation = "SQL query contains references to protected tables";
        }

        if ( sql.matches( SqlView.getIllegalKeywordsRegex() ) )
        {
            violation = "SQL query contains illegal keywords";
        }
        
        if ( violation != null )
        {
            log.warn( String.format( "Validation failed for SQL view '%s': %s", sqlView.getUid(), violation ) );
            
            throw new IllegalQueryException( violation );
        }
    }

    @Override
    public String testSqlGrammar( String sql )
    {
        return sqlViewStore.testSqlGrammar( sql );
    }

    @Override
    public void dropViewTable( SqlView sqlView )
    {
        sqlViewStore.dropViewTable( sqlView );
    }

    @Override
    public boolean refreshMaterializedView( SqlView sqlView )
    {
        if ( sqlView == null || !sqlView.isMaterializedView() )
        {
            return false;
        }
        
        return sqlViewStore.refreshMaterializedView( sqlView );
    }
}
