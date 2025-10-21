/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.sqlview.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import com.google.common.collect.Lists;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.TransactionMode;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewStore;
import org.hisp.dhis.sqlview.SqlViewType;
import org.hisp.dhis.system.util.SqlUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

/**
 * @author Dang Duy Hieu
 */
@Slf4j
@Repository("org.hisp.dhis.sqlview.SqlViewStore")
public class HibernateSqlViewStore extends HibernateIdentifiableObjectStore<SqlView>
    implements SqlViewStore {

  private final JdbcTemplate readOnlyJdbcTemplate;

  private final SystemSettingsProvider settingsProvider;

  public HibernateSqlViewStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService,
      @Qualifier("readOnlyJdbcTemplate") JdbcTemplate readOnlyJdbcTemplate,
      SystemSettingsProvider settingsProvider) {
    super(entityManager, jdbcTemplate, publisher, SqlView.class, aclService, false);

    checkNotNull(readOnlyJdbcTemplate);
    checkNotNull(settingsProvider);

    this.readOnlyJdbcTemplate = readOnlyJdbcTemplate;
    this.settingsProvider = settingsProvider;
  }

  // -------------------------------------------------------------------------
  // Implementing methods
  // -------------------------------------------------------------------------

  private boolean viewTableExists(SqlViewType type, String viewName) {
    String sql =
        type == SqlViewType.MATERIALIZED_VIEW
            ? "select count(*) from pg_matviews where schemaname = 'public' and matviewname = :name"
            : "select count(*) from pg_views where schemaname = 'public' and viewname = :name";

    Number count =
        (Number)
            getSession().createNativeQuery(sql).setParameter("name", viewName).getSingleResult();
    return count != null && count.intValue() > 0;
  }

  @Override
  public String createViewTable(SqlView sqlView) {
    checkIsDatabaseView(sqlView);
    try {
      createViewTable(sqlView.getType(), sqlView.getViewName(), sqlView.getSqlQuery());
      return null;
    } catch (BadSqlGrammarException ex) {
      return ex.getCause().getMessage();
    }
  }

  private void createViewTable(SqlViewType type, String viewName, String viewQuery) {
    dropViewTable(type, viewName);

    String sql =
        type == SqlViewType.MATERIALIZED_VIEW
            ? "CREATE MATERIALIZED VIEW %s AS %s"
            : "CREATE VIEW %s AS %s";
    sql = format(sql, SqlUtils.quote(viewName), viewQuery);

    log.debug("Create view SQL: " + sql);

    jdbcTemplate.execute(sql);
  }

  @Override
  public void populateSqlViewGrid(
      Grid grid, String sql, Object[] args, TransactionMode transactionMode) {
    SqlRowSet rs =
        switch (transactionMode) {
          case READ -> readOnlyJdbcTemplate.queryForRowSet(sql, args);
          case WRITE -> jdbcTemplate.queryForRowSet(sql, args);
        };

    int maxLimit = settingsProvider.getCurrentSettings().getSqlViewMaxLimit();

    log.debug("Get view SQL: " + sql + ", max limit: " + maxLimit);

    grid.addHeaders(rs);
    grid.addRows(rs, maxLimit);
  }

  @Override
  public void dropViewTable(SqlView sqlView) {
    checkIsDatabaseView(sqlView);
    dropViewTable(sqlView.getType(), sqlView.getViewName());
  }

  public void dropViewTable(SqlViewType type, String viewName) {
    if (!viewTableExists(type, viewName)) {
      return;
    }
    String sql =
        type == SqlViewType.MATERIALIZED_VIEW ? "DROP MATERIALIZED VIEW %s" : "DROP VIEW %s";
    sql = format(sql, SqlUtils.quote(viewName));

    log.debug("Drop view SQL: " + sql);
    try {
      jdbcTemplate.update(sql);
    } catch (Exception ex) {
      log.warn("Could not drop view: " + viewName, ex);
    }
  }

  @Override
  public boolean refreshMaterializedView(SqlView sqlView) {
    SqlViewType type = sqlView.getType();
    if (type != SqlViewType.MATERIALIZED_VIEW) {
      throw new IllegalArgumentException("Cannot refresh a view of type: " + type);
    }
    if (!viewTableExists(type, sqlView.getViewName())) {
      return createViewTable(sqlView) == null;
    }
    final String sql = "REFRESH MATERIALIZED VIEW " + sqlView.getViewName();

    log.debug("Refresh materialized view: " + sql);

    try {
      jdbcTemplate.update(sql);

      return true;
    } catch (Exception ex) {
      log.warn("Could not refresh materialized view: " + sqlView.getViewName(), ex);

      return false;
    }
  }

  private void checkIsDatabaseView(SqlView sqlView) {
    if (sqlView.isQuery()) {
      throw new IllegalArgumentException("Cannot create a view for a QUERY type view.");
    }
  }

  /**
   * Converts a filter operator and value into a parameterized filter with the value.
   *
   * <p>Example: {@code parseFilterOperator('eq', 5)} will return a record with values: ['= ?', 5]
   *
   * @param operator the filter operator.
   * @param value value of the current SQL query condition.
   * @return {@link OperatorWithPlaceHolderAndArg} record containing the operator with a
   *     placeholder, and the arg to be used in the placeholder.
   */
  public static OperatorWithPlaceHolderAndArg parseFilterOperator(String operator, String value) {

    if (StringUtils.isEmpty(operator)) {
      throw new QueryParserException("Filter Operator is null");
    }

    return switch (operator) {
      case "eq" -> new OperatorWithPlaceHolderAndArg(" = ? ", parseStringValue(value));
      case "ieq" -> new OperatorWithPlaceHolderAndArg(" ilike ? ", value);
      case "!eq", "ne", "neq" ->
          new OperatorWithPlaceHolderAndArg(" != ? ", parseStringValue(value));
      case "gt" -> new OperatorWithPlaceHolderAndArg(" > ? ", parseStringValue(value));
      case "lt" -> new OperatorWithPlaceHolderAndArg(" < ? ", parseStringValue(value));
      case "gte", "ge" -> new OperatorWithPlaceHolderAndArg(" >= ? ", parseStringValue(value));
      case "lte", "le" -> new OperatorWithPlaceHolderAndArg(" <= ? ", parseStringValue(value));
      case "like" -> new OperatorWithPlaceHolderAndArg(" like ? ", "%" + value + "%");
      case "!like" -> new OperatorWithPlaceHolderAndArg(" not like ? ", "%" + value + "%");
      case "^like" -> new OperatorWithPlaceHolderAndArg(" like ? ", value + "%");
      case "!^like" -> new OperatorWithPlaceHolderAndArg(" not like ? ", value + "%");
      case "$like" -> new OperatorWithPlaceHolderAndArg(" like ? ", "%" + value);
      case "!$like" -> new OperatorWithPlaceHolderAndArg(" not like ? ", "%" + value);
      case "ilike" -> new OperatorWithPlaceHolderAndArg(" ilike ? ", "%" + value + "%");
      case "!ilike" -> new OperatorWithPlaceHolderAndArg(" not ilike ? ", "%" + value + "%");
      case "^ilike" -> new OperatorWithPlaceHolderAndArg(" ilike ? ", value + "%");
      case "!^ilike" -> new OperatorWithPlaceHolderAndArg(" not ilike ? ", value + "%");
      case "$ilike" -> new OperatorWithPlaceHolderAndArg(" ilike ? ", "%" + value);
      case "!$ilike" -> new OperatorWithPlaceHolderAndArg(" not ilike ? ", "%" + value);
      case "in" -> {
        List<Object> objects = convertToCollectionArgs(value);
        yield new OperatorWithPlaceHolderAndArg(
            " in (" + String.join(",", Collections.nCopies(objects.size(), "?")) + ") ", objects);
      }
      case "!in" -> {
        List<Object> objects = convertToCollectionArgs(value);
        yield new OperatorWithPlaceHolderAndArg(
            " not in (" + String.join(",", Collections.nCopies(objects.size(), "?")) + ") ",
            objects);
      }
      case "null" -> new OperatorWithPlaceHolderAndArg("is null ", null);
      case "!null" -> new OperatorWithPlaceHolderAndArg("is not null ", null);
      default -> throw new QueryParserException("`" + operator + "` is not a valid operator.");
    };
  }

  /**
   * Converts a String with JSON format [x,y,z] into a list of Objects
   *
   * @param value a string contains a collection with JSON format [x,y,z].
   * @return as a list of objects e.g. List.of(x,y,z)
   */
  public static List<Object> convertToCollectionArgs(String value) {
    if (StringUtils.isEmpty(value)) {
      throw new QueryParserException("Value is null");
    }

    if (!value.startsWith("[") || !value.endsWith("]")) {
      throw new QueryParserException("Invalid query value");
    }

    String[] split = value.substring(1, value.length() - 1).split(",");
    List<String> items = Lists.newArrayList(split);
    List<Object> args = new ArrayList<>();

    for (String s : items) {
      Object item = parseStringValue(s);
      if (item != null) {
        args.add(item);
      }
    }
    return args;
  }

  /**
   * Parses the provided string value to return either null, a number or a string value
   *
   * @param value string value to parse
   * @return null,number or string
   */
  @CheckForNull
  public static Object parseStringValue(String value) {
    if (value == null || StringUtils.isEmpty(value)) {
      return null;
    }
    if (NumberUtils.isCreatable(value)) {
      try {
        return NumberUtils.createNumber(value);
      } catch (NumberFormatException e) {
        throw new QueryParserException("Could not parse number from value: %s".formatted(value));
      }
    }
    return value;
  }

  /**
   * Convert a List of select fields into a string as in SQL select query.
   *
   * <p>If input is null, return "*" means the query will select all fields.
   *
   * @param fields list of fields in a select query.
   * @return a string which is concatenated of list fields, separate by comma.
   */
  public static String parseSelectFields(List<String> fields) {
    if (fields == null || fields.isEmpty()) {
      return " * ";
    }
    StringBuilder str = new StringBuilder(StringUtils.EMPTY);
    for (int i = 0; i < fields.size(); i++) {
      String field = fields.get(i);
      if (field.equals("*")) {
        str.append("*");
      } else {
        str.append(SqlUtils.quote(field));
      }
      if (i < fields.size() - 1) {
        str.append(",");
      }
    }
    return str.toString();
  }

  /**
   * @param placeholderQuery SQL query with '?' placeholders for args
   * @param args the args to be supplied to the placeholders
   */
  public record PlaceholderQueryWithArgs(String placeholderQuery, List<Object> args) {}

  /**
   * @param operatorWithPlaceholder SQL operator with a '?' placeholder (e.g. 'like ?', '> ?')
   * @param arg the arg to be supplied to the placeholder
   */
  public record OperatorWithPlaceHolderAndArg(String operatorWithPlaceholder, Object arg) {}
}
