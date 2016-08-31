package org.hisp.dhis.sqlview;

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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;

/**
 * @author Dang Duy Hieu
 * @version $Id SqlViewService.java July 06, 2010$
 */
public interface SqlViewService
{
    String ID = SqlViewService.class.getName();
    
    String VARIABLE_EXPRESSION = "\\$\\{(\\w+)\\}";
    String SELECT_EXPRESSION = "^(?i)\\s*(select|with)\\s+.+";
    
    Pattern VARIABLE_PATTERN = Pattern.compile( VARIABLE_EXPRESSION, Pattern.DOTALL );
    Pattern SELECT_PATTERN = Pattern.compile( SELECT_EXPRESSION, Pattern.DOTALL );
    
    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    int saveSqlView( SqlView sqlView );

    void deleteSqlView( SqlView sqlView );

    void updateSqlView( SqlView sqlView );

    int getSqlViewCount();

    List<SqlView> getSqlViewsBetween( int first, int max );

    List<SqlView> getSqlViewsBetweenByName( String name, int first, int max );

    SqlView getSqlView( int viewId );

    SqlView getSqlViewByUid( String uid );

    SqlView getSqlView( String viewName );

    List<SqlView> getAllSqlViews();
    
    List<SqlView> getAllSqlViewsNoAcl();

    int getSqlViewCountByName( String name );
    
    // -------------------------------------------------------------------------
    // SQL view
    // -------------------------------------------------------------------------

    boolean viewTableExists( String viewTableName );

    String createViewTable( SqlView sqlView );

    void dropViewTable( SqlView sqlView );
    
    /**
    * Returns the SQL view as a grid.
    * 
    * @param sqlView the SQL view to render.
    * @param criteria the criteria on the format key:value, will be applied as
    *        criteria on the SQL result set.
    * @param variables the variables on the format key:value, will be substituted
    *        with variables inside the SQL view.
    * @return a grid.
    */
    Grid getSqlViewGrid( SqlView sqlView, Map<String, String> criteria, Map<String, String> variables );
    
    /**
     * Substitutes the given SQL query string with the given variables. SQL variables
     * are on the format ${key}.
     * 
     * @param sql the SQL string.
     * @param variables the variables.
     * @return the substituted SQL.
     */
    String substituteSql( String sql, Map<String, String> variables );
    
    /**
     * Validates the given SQL view. Checks include:
     * 
     * <ul>
     * <li>All necessary variables are supplied.</li>
     * <li>Variable keys and values do not contain null values.</li>
     * <li>Invalid tables are not present in SQL query.</li>
     * </ul>
     * 
     * @param sqlView the SQL view.
     * @param criteria the criteria.
     * @param variables the variables.
     * @throws IllegalQueryException if SQL view is invalid.
     */
    void validateSqlView( SqlView sqlView, Map<String, String> criteria, Map<String, String> variables )
        throws IllegalQueryException;
    
    /**
     * Returns the variables contained in the given SQL.
     * 
     * @param sql the SQL query string.
     * @return a set of variable keys.
     */
    Set<String> getVariables( String sql );
    
    /**
     * Tests whether the given SQL view syntax is valid.
     * 
     * @param sql the SQL view.
     * @return null if valid, a non-null descriptive string if invalid.
     */
    String testSqlGrammar( String sql );
    
    /**
     * Refreshes the materialized view.
     * 
     * @param sqlView the SQL view.
     * @return true if the materialized view was refreshed, false if not.
     */
    boolean refreshMaterializedView( SqlView sqlView );
}
