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

import static org.hisp.dhis.query.QueryUtils.parseStringValue;
import static org.hisp.dhis.sqlview.SqlView.CURRENT_USERNAME_VARIABLE;
import static org.hisp.dhis.sqlview.SqlView.CURRENT_USER_ID_VARIABLE;
import static org.hisp.dhis.sqlview.SqlView.STANDARD_VARIABLES;
import static org.hisp.dhis.sqlview.SqlView.getInvalidQueryParams;
import static org.hisp.dhis.sqlview.SqlView.getInvalidQueryValues;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.TransactionMode;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryUtils;
import org.hisp.dhis.query.QueryUtils.OperatorWithPlaceHolderAndArg;
import org.hisp.dhis.query.QueryUtils.PlaceholderQueryWithArgs;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Dang Duy Hieu
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
@Service("org.hisp.dhis.sqlview.SqlViewService")
public class DefaultSqlViewService implements SqlViewService {
  private static final String SELECT_EXPRESSION = "^(?i)\\s*(select|with)\\s+.+";

  private static final Pattern SELECT_PATTERN = Pattern.compile(SELECT_EXPRESSION, Pattern.DOTALL);

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final SqlViewStore sqlViewStore;

  private final StatementBuilder statementBuilder;

  private final DhisConfigurationProvider config;

  private final AclService aclService;

  private final CurrentUserService currentUserService;

  // -------------------------------------------------------------------------
  // CRUD methods
  // -------------------------------------------------------------------------

  @Override
  public long saveSqlView(SqlView sqlView) {
    sqlViewStore.save(sqlView);

    return sqlView.getId();
  }

  @Override
  public void updateSqlView(SqlView sqlView) {
    sqlViewStore.update(sqlView);
  }

  @Override
  public void deleteSqlView(SqlView sqlView) {
    if (!sqlView.isQuery()) {
      dropViewTable(sqlView);
    }

    sqlViewStore.delete(sqlView);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SqlView> getAllSqlViews() {
    return sqlViewStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<SqlView> getAllSqlViewsNoAcl() {
    return sqlViewStore.getAllNoAcl();
  }

  @Override
  @Transactional(readOnly = true)
  public SqlView getSqlView(long viewId) {
    return sqlViewStore.get(viewId);
  }

  @Override
  @Transactional(readOnly = true)
  public SqlView getSqlViewByUid(String uid) {
    return sqlViewStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public SqlView getSqlView(String viewName) {
    return sqlViewStore.getByName(viewName);
  }

  @Override
  @Transactional(readOnly = true)
  public int getSqlViewCount() {
    return sqlViewStore.getCount();
  }

  // -------------------------------------------------------------------------
  // Service methods
  // -------------------------------------------------------------------------

  @Override
  public String createViewTable(SqlView sqlView) {
    validateSqlView(sqlView, null, null);

    return sqlViewStore.createViewTable(sqlView);
  }

  @Override
  @Transactional(readOnly = true)
  public Grid getSqlViewGridReadOnly(
      SqlView sqlView,
      Map<String, String> criteria,
      Map<String, String> variables,
      List<String> filters,
      List<String> fields) {
    return getGridData(sqlView, criteria, variables, filters, fields, TransactionMode.READ);
  }

  @Override
  @Transactional
  public Grid getSqlViewGridWritesAllowed(
      SqlView sqlView,
      Map<String, String> criteria,
      Map<String, String> variables,
      List<String> filters,
      List<String> fields) {
    return getGridData(sqlView, criteria, variables, filters, fields, TransactionMode.WRITE);
  }

  private Grid getGridData(
      SqlView sqlView,
      Map<String, String> criteria,
      Map<String, String> variables,
      List<String> filters,
      List<String> fields,
      TransactionMode transactionMode) {
    canAccess(sqlView);
    validateSqlView(sqlView, criteria, variables);

    Grid grid = new ListGrid();
    grid.setTitle(sqlView.getName());
    grid.setSubtitle(sqlView.getDescription());

    log.info(String.format("Retrieving data for SQL view: '%s'", sqlView.getUid()));

    PlaceholderQueryWithArgs placeholderQueryWithArgs =
        sqlView.isQuery()
            ? getSqlForQuery(sqlView, criteria, variables, filters, fields)
            : getSqlForView(sqlView, criteria, filters, fields);

    sqlViewStore.populateSqlViewGrid(
        grid,
        placeholderQueryWithArgs.getPlaceholderQuery(),
        placeholderQueryWithArgs.getArgs() == null
            ? null
            : placeholderQueryWithArgs.getArgs().toArray(),
        transactionMode);
    return grid;
  }

  private void canAccess(SqlView sqlView) {
    User currentUser = currentUserService.getCurrentUser();
    if (!aclService.canDataRead(currentUser, sqlView)) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E4312, sqlView.getUid()));
    }
  }

  private PlaceholderQueryWithArgs parseFilters(List<String> filters, SqlHelper sqlHelper)
      throws QueryParserException {
    StringBuilder query = new StringBuilder();
    List<Object> queryArgs = new ArrayList<>();

    for (int i = 0; i < filters.size(); i++) {
      String filter = filters.get(i);
      String[] split = filter.split(":");

      if (split.length == 3) {
        int index = split[0].length() + ":".length() + split[1].length() + ":".length();
        OperatorWithPlaceHolderAndArg filterQuery =
            getFilterQuery(sqlHelper, split[0], split[1], filter.substring(index));
        query.append(filterQuery.getOperatorWithPlaceholder());

        // this arg could be a collection, so need to add each (not the collection)
        if (filterQuery.getArg() instanceof Collection<?>) {
          Collection<?> collection = (Collection<?>) filterQuery.getArg();
          queryArgs.addAll(collection);
        } else {
          queryArgs.add(filterQuery.getArg());
        }
      } else if (split.length == 2 && (split[1].equals("null") || split[1].equals("!null"))) {
        query.append(
            getFilterQuery(sqlHelper, split[0], split[1], null).getOperatorWithPlaceholder());
      } else {
        throw new QueryParserException("Invalid filter => " + filter);
      }
    }

    return new PlaceholderQueryWithArgs(query.toString(), queryArgs);
  }

  private OperatorWithPlaceHolderAndArg getFilterQuery(
      SqlHelper sqlHelper, String columnName, String operator, String value) {
    String filter = "";
    OperatorWithPlaceHolderAndArg operatorWithPlaceHolderAndArg =
        QueryUtils.parseFilterOperator(operator, value);

    filter +=
        sqlHelper.whereAnd()
            + " "
            + columnName
            + " "
            + operatorWithPlaceHolderAndArg.getOperatorWithPlaceholder();

    return new OperatorWithPlaceHolderAndArg(filter, operatorWithPlaceHolderAndArg.getArg());
  }

  private PlaceholderQueryWithArgs getSqlForQuery(
      SqlView sqlView,
      Map<String, String> criteria,
      Map<String, String> variables,
      List<String> filters,
      List<String> fields) {
    boolean hasCriteria = criteria != null && !criteria.isEmpty();

    boolean hasFilter = filters != null && !filters.isEmpty();

    String sql = substituteQueryVariables(sqlView, variables);

    List<Object> args = new ArrayList<>();
    if (hasCriteria || hasFilter) {
      sql = SqlViewUtils.removeQuerySeparator(sql);

      String outerSql =
          "select " + QueryUtils.parseSelectFields(fields) + " from " + "(" + sql + ") as qry ";

      SqlHelper sqlHelper = new SqlHelper();

      if (hasCriteria) {
        PlaceholderQueryWithArgs placeholderQueryWithArgs =
            getCriteriaSqlClause(criteria, sqlHelper);
        outerSql += placeholderQueryWithArgs.getPlaceholderQuery();
        args.addAll(placeholderQueryWithArgs.getArgs());
      }

      if (hasFilter) {
        PlaceholderQueryWithArgs placeholderQueryWithArgs = parseFilters(filters, sqlHelper);
        outerSql += placeholderQueryWithArgs.getPlaceholderQuery();
        args.addAll(placeholderQueryWithArgs.getArgs());
        return new PlaceholderQueryWithArgs(outerSql, args);
      }

      return new PlaceholderQueryWithArgs(outerSql, args);
    }

    return new PlaceholderQueryWithArgs(sql, null);
  }

  private String substituteQueryVariables(SqlView sqlView, Map<String, String> variables) {
    String sql = SqlViewUtils.substituteSqlVariables(sqlView.getSqlQuery(), variables);

    User currentUser = currentUserService.getCurrentUser();

    if (currentUser != null) {
      sql =
          SqlViewUtils.substituteSqlVariable(
              sql, CURRENT_USER_ID_VARIABLE, Long.toString(currentUser.getId()));
      sql =
          SqlViewUtils.substituteSqlVariable(
              sql, CURRENT_USERNAME_VARIABLE, currentUser.getUsername());
    }

    return sql;
  }

  private PlaceholderQueryWithArgs getSqlForView(
      SqlView sqlView, Map<String, String> criteria, List<String> filters, List<String> fields) {
    String sql =
        "select "
            + QueryUtils.parseSelectFields(fields)
            + " from "
            + statementBuilder.columnQuote(sqlView.getViewName())
            + " ";

    boolean hasCriteria = criteria != null && !criteria.isEmpty();

    boolean hasFilter = filters != null && !filters.isEmpty();

    List<Object> args = new ArrayList<>();
    if (hasCriteria || hasFilter) {
      SqlHelper sqlHelper = new SqlHelper();

      if (hasCriteria) {
        PlaceholderQueryWithArgs sqlQueryWithArgs = getCriteriaSqlClause(criteria, sqlHelper);
        sql += sqlQueryWithArgs.getPlaceholderQuery();
        args.addAll(sqlQueryWithArgs.getArgs());
      }

      if (hasFilter) {
        PlaceholderQueryWithArgs sqlQueryWithArgs = parseFilters(filters, sqlHelper);
        sql += sqlQueryWithArgs.getPlaceholderQuery();
        args.addAll(sqlQueryWithArgs.getArgs());
      }
      return new PlaceholderQueryWithArgs(sql, args);
    }

    return new PlaceholderQueryWithArgs(sql, null);
  }

  private PlaceholderQueryWithArgs getCriteriaSqlClause(
      Map<String, String> criteria, SqlHelper sqlHelper) {
    StringBuilder sql = new StringBuilder();

    List<Object> args = new ArrayList<>();
    if (criteria != null && !criteria.isEmpty()) {
      sqlHelper = ObjectUtils.firstNonNull(sqlHelper, new SqlHelper());

      for (Map.Entry<String, String> criterion : criteria.entrySet()) {
        sql.append(sqlHelper.whereAnd())
            .append(" ")
            .append(statementBuilder.columnQuote(criterion.getKey()))
            .append(" = ? ");
        args.add(parseStringValue(criterion.getValue()));
      }
    }
    return new PlaceholderQueryWithArgs(sql.toString(), args);
  }

  @Override
  public void validateSqlView(
      SqlView sqlView, Map<String, String> criteria, Map<String, String> variables)
      throws IllegalQueryException {
    ErrorMessage error = null;

    if (sqlView == null || sqlView.getSqlQuery() == null) {
      throw new IllegalQueryException(ErrorCode.E4300);
    }

    final Set<String> sqlVars = SqlViewUtils.getVariables(sqlView.getSqlQuery());
    final String sql = sqlView.getSqlQuery().replaceAll("\\r|\\n", " ").toLowerCase();
    final boolean ignoreSqlViewTableProtection =
        config.isDisabled(ConfigurationKey.SYSTEM_SQL_VIEW_TABLE_PROTECTION);
    final Set<String> allowedVariables =
        variables == null ? STANDARD_VARIABLES : Sets.union(variables.keySet(), STANDARD_VARIABLES);

    if (!SELECT_PATTERN.matcher(sql).matches()) {
      error = new ErrorMessage(ErrorCode.E4301);
    }

    if (sql.contains(";") && !sql.trim().endsWith(";")) {
      error = new ErrorMessage(ErrorCode.E4302);
    }

    if (variables != null && variables.containsKey(null)) {
      error = new ErrorMessage(ErrorCode.E4303);
    }

    if (variables != null && variables.containsValue(null)) {
      error = new ErrorMessage(ErrorCode.E4304);
    }

    if (variables != null && !getInvalidQueryParams(variables.keySet()).isEmpty()) {
      error = new ErrorMessage(ErrorCode.E4305, getInvalidQueryParams(variables.keySet()));
    }

    if (variables != null && !getInvalidQueryValues(variables.values()).isEmpty()) {
      error = new ErrorMessage(ErrorCode.E4306, getInvalidQueryValues(variables.values()));
    }

    if (sqlView.isQuery() && !sqlVars.isEmpty() && (!allowedVariables.containsAll(sqlVars))) {
      error = new ErrorMessage(ErrorCode.E4307, sqlVars);
    }

    if (sqlView.isQuery() && !sqlVars.isEmpty() && !getInvalidQueryParams(sqlVars).isEmpty()) {
      error = new ErrorMessage(ErrorCode.E4313, getInvalidQueryParams(sqlVars));
    }

    if (criteria != null && !getInvalidQueryParams(criteria.keySet()).isEmpty()) {
      error = new ErrorMessage(ErrorCode.E4308, getInvalidQueryParams(criteria.keySet()));
    }

    if (criteria != null && !getInvalidQueryValues(criteria.values()).isEmpty()) {
      error = new ErrorMessage(ErrorCode.E4309, getInvalidQueryValues(criteria.values()));
    }

    if (!ignoreSqlViewTableProtection && sql.matches(SqlView.getProtectedTablesRegex())) {
      error = new ErrorMessage(ErrorCode.E4310);
    }

    if (sql.matches(SqlView.getIllegalKeywordsRegex())) {
      error = new ErrorMessage(ErrorCode.E4311);
    }

    if (error != null) {
      log.warn(
          String.format(
              "Validation failed for SQL view '%s' with code: '%s' and message: '%s'",
              sqlView.getUid(), error.getErrorCode(), error.getMessage()));

      throw new IllegalQueryException(error);
    }
  }

  @Override
  public void dropViewTable(SqlView sqlView) {
    sqlViewStore.dropViewTable(sqlView);
  }

  @Override
  public boolean refreshMaterializedView(SqlView sqlView) {
    if (sqlView == null || !sqlView.isMaterializedView()) {
      return false;
    }

    return sqlViewStore.refreshMaterializedView(sqlView);
  }
}
