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
package org.hisp.dhis.sqlview;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;

/**
 * @author Dang Duy Hieu
 * @version $Id SqlViewService.java July 06, 2010$
 */
public interface SqlViewService {
  String ID = SqlViewService.class.getName();

  String SELECT_EXPRESSION = "^(?i)\\s*(select|with)\\s+.+";

  Pattern SELECT_PATTERN = Pattern.compile(SELECT_EXPRESSION, Pattern.DOTALL);

  // -------------------------------------------------------------------------
  // CRUD
  // -------------------------------------------------------------------------

  long saveSqlView(SqlView sqlView);

  void deleteSqlView(SqlView sqlView);

  void updateSqlView(SqlView sqlView);

  int getSqlViewCount();

  SqlView getSqlView(long viewId);

  SqlView getSqlViewByUid(String uid);

  SqlView getSqlView(String viewName);

  List<SqlView> getAllSqlViews();

  List<SqlView> getAllSqlViewsNoAcl();

  // -------------------------------------------------------------------------
  // SQL view
  // -------------------------------------------------------------------------

  boolean viewTableExists(String viewTableName);

  /**
   * Creates the SQL view in the database. Checks if the SQL query is valid.
   *
   * @param sqlView the SQL view.
   * @return null if the view was created successfully, a non-null error message if the operation
   *     failed.
   * @throws {@link IllegalQueryException} if the SQL query is invalid.
   */
  String createViewTable(SqlView sqlView);

  void dropViewTable(SqlView sqlView);

  /**
   * Returns the SQL view as a grid. Checks if the SQL query is valid.
   *
   * @param sqlView the SQL view to render.
   * @param criteria the criteria on the format key:value, will be applied as criteria on the SQL
   *     result set.
   * @param variables the variables on the format key:value, will be substituted with variables
   *     inside the SQL view.
   * @return a grid.
   * @throws {@link IllegalQueryException} if the SQL query is invalid.
   */
  Grid getSqlViewGrid(
      SqlView sqlView,
      Map<String, String> criteria,
      Map<String, String> variables,
      List<String> filters,
      List<String> fields);

  /**
   * Validates the given SQL view. Checks include:
   *
   * <ul>
   *   <li>All necessary variables are supplied.
   *   <li>Variable keys and values do not contain null values.
   *   <li>Invalid tables are not present in SQL query.
   * </ul>
   *
   * @param sqlView the SQL view.
   * @param criteria the criteria.
   * @param variables the variables.
   * @throws IllegalQueryException if SQL view is invalid.
   */
  void validateSqlView(SqlView sqlView, Map<String, String> criteria, Map<String, String> variables)
      throws IllegalQueryException;

  /**
   * Tests whether the given SQL view syntax is valid.
   *
   * @param sql the SQL view.
   * @return null if valid, a non-null descriptive string if invalid.
   */
  String testSqlGrammar(String sql);

  /**
   * Refreshes the materialized view.
   *
   * @param sqlView the SQL view.
   * @return true if the materialized view was refreshed, false if not.
   */
  boolean refreshMaterializedView(SqlView sqlView);
}
