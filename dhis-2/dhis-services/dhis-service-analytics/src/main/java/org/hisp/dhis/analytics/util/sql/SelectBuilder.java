/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.sql;

import static org.hisp.dhis.analytics.util.sql.QuoteUtils.unquote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A fluent builder for creating SQL SELECT queries. Supports common SQL features including CTEs,
 * JOINs, WHERE conditions, GROUP BY, HAVING, ORDER BY, and pagination.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * String sql = new SelectBuilder()
 *     .withCTE("active_users", "SELECT id FROM users WHERE active = true")
 *     .addColumn("u.name")
 *     .addColumn("COUNT(o.id)", "order_count")
 *     .from("users", "u")
 *     .leftJoin("orders", "o", alias -> alias + ".user_id = u.id")
 *     .where(Condition.raw("u.id IN (SELECT id FROM active_users)"))
 *     .groupBy("u.name")
 *     .having(Condition.raw("COUNT(o.id) > 0"))
 *     .orderBy("order_count", "DESC", "NULLS LAST")
 *     .limit(10)
 *     .build();
 * }</pre>
 */
public class SelectBuilder {
  /** Maximum limit for pagination to prevent excessive resource usage */
  private static final int DEFAULT_MAX_LIMIT = 5000;

  private final List<CommonTableExpression> ctes = new ArrayList<>();
  private final List<Column> columns = new ArrayList<>();
  private String fromTable;
  private String fromAlias;
  private final List<Join> joins = new ArrayList<>();
  private Condition whereCondition;
  private final List<String> groupByClauses = new ArrayList<>();
  private final List<Condition> havingConditions = new ArrayList<>();
  private final List<OrderByClause> orderByClauses = new ArrayList<>();
  private Integer limit;
  private Integer offset;
  private static final Pattern ORDER_BY_PATTERN = Pattern.compile("^(?i)order\\s+by\\s+");

  public enum JoinType {
    INNER("inner join"),
    LEFT("left join"),
    CROSS("cross join");

    private final String sql;

    JoinType(String sql) {
      this.sql = sql;
    }

    public String toSql() {
      return sql;
    }
  }

  /**
   * Represents a column in the SELECT clause of a SQL query. Handles column expressions with
   * optional table prefix and column aliases.
   *
   * <p>Examples:
   *
   * <pre>{@code
   * // Simple column with table prefix
   * new Column("name", "u", null)               -> "u.name"
   *
   * // Column with table prefix and alias
   * new Column("first_name", "u", "name")       -> "u.first_name AS name"
   *
   * // Aggregate function with alias
   * new Column("COUNT(*)", null, "total")       -> "COUNT(*) AS total"
   *
   * // Expression with alias
   * new Column("COALESCE(name, 'Unknown')", "u", "display_name")
   *     -> "u.COALESCE(name, 'Unknown') AS display_name"
   * }</pre>
   */
  public record Column(String expression, String tablePrefix, String alias) {
    /**
     * Creates a column with just an expression.
     *
     * @param expression the column expression
     * @return a new Column without prefix or alias
     */
    public static Column of(String expression) {
      return new Column(expression, null, null);
    }

    /**
     * Creates a column with an expression and table prefix.
     *
     * @param expression the column expression
     * @param tablePrefix the table prefix/alias
     * @return a new Column with prefix
     */
    public static Column withPrefix(String expression, String tablePrefix) {
      return new Column(expression, tablePrefix, null);
    }

    /**
     * Creates a column with an expression and alias.
     *
     * @param expression the column expression
     * @param alias the column alias
     * @return a new Column with alias
     */
    public static Column withAlias(String expression, String alias) {
      return new Column(expression, null, alias);
    }

    /**
     * Converts the column definition to its SQL string representation.
     *
     * @return the SQL string representation of the column
     */
    public String toSql() {
      StringBuilder sql = new StringBuilder();

      // Add table prefix if present
      if (tablePrefix != null && !tablePrefix.isEmpty()) {
        sql.append(tablePrefix).append(".");
      }

      // Add the expression
      sql.append(expression);

      // Add alias if present
      if (alias != null && !alias.isEmpty()) {
        sql.append(" AS ").append(alias);
      }

      return sql.toString();
    }
  }

  /**
   * Represents a Common Table Expression (CTE). CTEs are temporary named result sets that exist for
   * the duration of the query.
   *
   * <p>Example:
   *
   * <pre>{@code
   * new CommonTableExpression("active_users",
   *     "SELECT id FROM users WHERE status = 'ACTIVE'")
   *     -> "active_users AS (
   *         SELECT id FROM users WHERE status = 'ACTIVE'
   *         )"
   * }</pre>
   */
  public record CommonTableExpression(String name, String query) {
    public String toSql() {
      return name + " AS (\n" + query + "\n)";
    }
  }

  /**
   * Represents a LEFT JOIN clause. Includes the table name, alias, and join condition.
   *
   * <p>Example:
   *
   * <pre>{@code
   * new Join("orders", "o", "o.user_id = u.id")
   *     -> "LEFT JOIN orders o ON o.user_id = u.id"
   * }</pre>
   */
  public record Join(JoinType type, String table, String alias, String condition) {
    public String toSql() {
      if (type == JoinType.CROSS) {
        return String.format("%s %s as %s", type.toSql(), table, alias);
      }
      return String.format("%s %s %s on %s", type.toSql(), table, alias, condition);
    }
  }

  /**
   * Represents an ORDER BY clause. Supports direction (ASC/DESC) and NULL handling (NULLS
   * FIRST/LAST).
   *
   * <p>Examples:
   *
   * <pre>{@code
   * new OrderByClause("name", "ASC", null)           -> "name ASC"
   * new OrderByClause("age", "DESC", "NULLS LAST")   -> "age DESC NULLS LAST"
   * new OrderByClause("status", null, "NULLS FIRST") -> "status NULLS FIRST"
   * }</pre>
   */
  public record OrderByClause(String column, String direction, String nullHandling) {
    public String toSql() {
      StringBuilder sb = new StringBuilder(column);
      if (direction != null) {
        sb.append(" ").append(direction);
      }
      if (nullHandling != null) {
        sb.append(" ").append(nullHandling);
      }
      return sb.toString();
    }
  }

  /**
   * Adds a Common Table Expression (CTE) to the query.
   *
   * @param name the name of the CTE
   * @param query the SELECT query that defines the CTE
   * @return this builder instance
   *     <p>Example:
   *     <pre>{@code
   * builder.withCTE("active_users",
   *     "SELECT id FROM users WHERE status = 'ACTIVE'")
   * }</pre>
   */
  public SelectBuilder withCTE(String name, String query) {
    ctes.add(new CommonTableExpression(name, query));
    return this;
  }

  /**
   * Adds a column with table prefix.
   *
   * @param expression the column expression
   * @param tablePrefix the table prefix/alias
   * @return this builder instance
   */
  public SelectBuilder addColumn(String expression, String tablePrefix) {
    columns.add(Column.withPrefix(expression, tablePrefix));
    return this;
  }

  public List<String> getColumnNames() {
    return columns.stream().map(Column::expression).toList();
  }

  /**
   * Adds a column with an alias.
   *
   * @param expression the column expression
   * @param tablePrefix the table prefix/alias
   * @param alias the column alias
   * @return this builder instance
   */
  public SelectBuilder addColumn(String expression, String tablePrefix, String alias) {
    columns.add(new Column(expression, tablePrefix, alias));
    return this;
  }

  /**
   * Adds a simple column without prefix or alias.
   *
   * @param expression the column expression
   * @return this builder instance
   */
  public SelectBuilder addColumn(String expression) {
    columns.add(Column.of(expression));
    return this;
  }

  public SelectBuilder addColumnIfNotExist(String expression) {
    String flattenedColumns =
        columns.stream().map(Column::expression).collect(Collectors.joining(","));

    if (!flattenedColumns.contains(unquote(expression))) {
      columns.add(Column.of(expression));
    }
    return this;
  }

  /**
   * Sets the FROM clause table without an alias.
   *
   * @param table the table name
   * @return this builder instance
   */
  public SelectBuilder from(String table) {
    this.fromTable = sanitizeFromClause(table);
    return this;
  }

  /**
   * Sets the FROM clause table with an alias.
   *
   * @param table the table name
   * @param alias the table alias
   * @return this builder instance
   *     <p>Example:
   *     <pre>{@code
   * builder.from("users", "u")
   * }</pre>
   */
  public SelectBuilder from(String table, String alias) {
    this.fromTable = sanitizeFromClause(table);
    this.fromAlias = alias;
    return this;
  }

  /**
   * Adds a LEFT JOIN clause to the query.
   *
   * @param table the table to join
   * @param alias the alias for the joined table
   * @param condition the join condition builder
   * @return this builder instance
   *     <p>Example:
   *     <pre>{@code
   * builder.leftJoin("orders", "o",
   *     alias -> alias + ".user_id = u.id")
   * }</pre>
   */
  public SelectBuilder leftJoin(String table, String alias, JoinCondition condition) {
    joins.add(new Join(JoinType.LEFT, table, alias, condition.build(alias)));
    return this;
  }

  /**
   * Adds a CROSS JOIN clause to the query.
   *
   * @param table the table to join
   * @param alias the alias for the joined table
   * @return this builder instance
   */
  public SelectBuilder crossJoin(String table, String alias) {
    joins.add(new Join(JoinType.CROSS, table, alias, null));
    return this;
  }

  /**
   * Adds a INNER JOIN clause to the query.
   *
   * @param table the table to join
   * @param alias the alias for the joined table
   * @param condition the join condition builder
   * @return this builder instance
   *     <p>Example:
   *     <pre>{@code
   * builder.innerJoin("orders", "o",
   *     alias -> alias + ".user_id = u.id")
   * }</pre>
   */
  public SelectBuilder innerJoin(String table, String alias, JoinCondition condition) {
    joins.add(new Join(JoinType.INNER, table, alias, condition.build(alias)));
    return this;
  }

  /**
   * Sets the WHERE clause condition.
   *
   * @param condition the WHERE condition
   * @return this builder instance
   *     <p>Example:
   *     <pre>{@code
   * builder.where(Condition.and(
   *     Condition.raw("active = true"),
   *     Condition.raw("age >= 18")
   * ))
   * }</pre>
   */
  public SelectBuilder where(Condition condition) {
    this.whereCondition = condition;
    return this;
  }

  /**
   * Adds a HAVING clause condition. Multiple conditions are combined with AND.
   *
   * @param condition the HAVING condition
   * @return this builder instance
   *     <p>Example:
   *     <pre>{@code
   * builder.having(Condition.raw("COUNT(*) > 0"))
   * }</pre>
   */
  public SelectBuilder having(Condition condition) {
    havingConditions.add(condition);
    return this;
  }

  /**
   * Adds GROUP BY columns.
   *
   * @param columns the columns to group by
   * @return this builder instance
   *     <p>Example:
   *     <pre>{@code
   * builder.groupBy("department", "status")
   * }</pre>
   */
  public SelectBuilder groupBy(String... columns) {
    groupByClauses.addAll(Arrays.asList(columns));
    return this;
  }

  /**
   * Adds a GROUP BY column.
   *
   * @param column the column to group by
   * @return this builder instance
   */
  public SelectBuilder groupBy(String column) {
    groupByClauses.add(column);
    return this;
  }

  /**
   * Adds an ORDER BY clause with direction.
   *
   * @param column the column to sort by
   * @param direction the sort direction ("ASC" or "DESC")
   * @return this builder instance
   */
  public SelectBuilder orderBy(String column, String direction) {
    return orderBy(column, direction, null);
  }

  /**
   * Adds an ORDER BY clause with direction and NULL handling.
   *
   * @param column the column to sort by
   * @param direction the sort direction ("ASC" or "DESC")
   * @param nullHandling the NULL handling ("NULLS FIRST" or "NULLS LAST")
   * @return this builder instance
   *     <p>Example:
   *     <pre>{@code
   * builder.orderBy("last_updated", "DESC", "NULLS LAST")
   * }</pre>
   */
  public SelectBuilder orderBy(String column, String direction, String nullHandling) {
    orderByClauses.add(new OrderByClause(column, direction, nullHandling));
    return this;
  }

  /**
   * Parses and adds ORDER BY clauses from a raw SQL string. Handles complex expressions including
   * CASE statements.
   *
   * @param rawSortClause the raw ORDER BY clause
   * @return this builder instance
   *     <p>Example:
   *     <pre>{@code
   * builder.orderBy("name ASC, created_at DESC NULLS LAST")
   * builder.orderBy("CASE WHEN active THEN 1 ELSE 2 END DESC")
   * }</pre>
   */
  public SelectBuilder orderBy(String rawSortClause) {
    if (rawSortClause == null || rawSortClause.trim().isEmpty()) {
      return this;
    }

    // Remove "order by" prefix if present
    String cleaned = ORDER_BY_PATTERN.matcher(rawSortClause.trim()).replaceFirst("");

    // Split by commas, but not commas within CASE statements
    List<String> parts = splitPreservingCaseStatements(cleaned);

    for (String part : parts) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        // Extract direction and null handling from the end
        String[] directionParts = extractDirectionAndNulls(trimmed);
        String column = directionParts[0];
        // check if order by column is part of a cte, if it is must be unquoted
        String unquotedColumn = unquote(column);
        List<String> cteNames = ctes.stream().map(c -> c.name).toList();
        if (cteNames.contains(unquotedColumn)) {
          column = unquotedColumn;
        }
        String direction = directionParts[1];
        String nullHandling = directionParts[2];

        orderByClauses.add(new OrderByClause(column, direction, nullHandling));
      }
    }

    return this;
  }

  /**
   * Adds multiple ORDER BY clauses.
   *
   * @param clauses the list of ORDER BY clauses
   * @return this builder instance
   */
  public SelectBuilder orderBy(List<OrderByClause> clauses) {
    orderByClauses.addAll(clauses);
    return this;
  }

  /**
   * Sets the LIMIT clause with a maximum value of {@value DEFAULT_MAX_LIMIT}.
   *
   * @param limit the maximum number of rows to return
   * @return this builder instance
   */
  public SelectBuilder limit(int limit) {
    this.limit = Math.min(limit, DEFAULT_MAX_LIMIT);
    return this;
  }

  /**
   * Sets the LIMIT clause to the specified value plus one. Useful for detecting if there are more
   * rows available.
   *
   * @param limit the base limit value
   * @return this builder instance
   */
  public SelectBuilder limitPlusOne(int limit) {
    this.limit = limit + 1;
    return this;
  }

  /**
   * Sets the LIMIT clause with a specified maximum value.
   *
   * @param limit the desired limit
   * @param maxLimit the maximum allowed limit
   * @return this builder instance
   */
  public SelectBuilder limitWithMax(int limit, int maxLimit) {
    this.limit = Math.min(limit, maxLimit);
    return this;
  }

  /**
   * Sets the LIMIT clause to the minimum of the specified limit and maxLimit, plus one.
   *
   * @param limit the desired limit
   * @param maxLimit the maximum allowed limit
   * @return this builder instance
   */
  public SelectBuilder limitWithMaxPlusOne(int limit, int maxLimit) {
    this.limit = Math.min(limit, maxLimit) + 1;
    return this;
  }

  /**
   * Sets the OFFSET clause.
   *
   * @param offset the number of rows to skip
   * @return this builder instance
   */
  public SelectBuilder offset(int offset) {
    this.offset = offset;
    return this;
  }

  /**
   * Builds the SQL query string with all keywords in lowercase.
   *
   * @return the complete SQL query string
   */
  public String build() {
    return SqlFormatter.lowercase(buildQuery());
  }

  public String getWhereClause() {
    return whereCondition.toSql();
  }

  /**
   * Builds the SQL query string with formatting for readability.
   *
   * @return the formatted SQL query string
   */
  public String buildPretty() {
    return SqlFormatter.prettyPrint(build());
  }

  private String buildQuery() {
    StringBuilder sql = new StringBuilder();
    appendCTEs(sql);
    appendSelectClause(sql);
    appendFromClause(sql);
    appendJoins(sql);
    appendWhereClause(sql);
    appendGroupByClause(sql);
    appendHavingClause(sql);
    appendOrderByClause(sql);
    appendPagination(sql);
    return sql.toString();
  }

  private void appendCTEs(StringBuilder sql) {
    if (ctes.isEmpty()) {
      return;
    }
    sql.append("with ")
        .append(ctes.stream().map(CommonTableExpression::toSql).collect(Collectors.joining(", ")))
        .append(" ");
  }

  private void appendSelectClause(StringBuilder sql) {
    sql.append("select ")
        .append(columns.stream().map(Column::toSql).collect(Collectors.joining(", ")));
  }

  private void appendFromClause(StringBuilder sql) {
    sql.append(" from ").append(fromTable);

    if (fromAlias != null) {
      sql.append(" as ").append(fromAlias);
    }
  }

  private void appendJoins(StringBuilder sql) {
    if (joins.isEmpty()) {
      return;
    }
    sql.append(" ").append(joins.stream().map(Join::toSql).collect(Collectors.joining(" ")));
  }

  private void appendWhereClause(StringBuilder sql) {
    if (whereCondition != null) {
      String whereSql = whereCondition.toSql();
      sql.append(whereSql.isEmpty() ? "" : " where " + whereSql);
    }
  }

  private void appendGroupByClause(StringBuilder sql) {
    if (groupByClauses.isEmpty()) {
      return;
    }
    sql.append(" group by ").append(String.join(", ", groupByClauses));
  }

  private void appendHavingClause(StringBuilder sql) {
    if (havingConditions.isEmpty()) {
      return;
    }
    sql.append(" having ")
        .append(
            havingConditions.stream().map(Condition::toSql).collect(Collectors.joining(" and ")));
  }

  private void appendOrderByClause(StringBuilder sql) {
    if (orderByClauses.isEmpty()) {
      return;
    }
    sql.append(" order by ")
        .append(
            orderByClauses.stream().map(OrderByClause::toSql).collect(Collectors.joining(", ")));
  }

  private void appendPagination(StringBuilder sql) {
    if (limit != null) {
      sql.append(" limit ").append(limit);
    }
    if (offset != null) {
      sql.append(" offset ").append(offset);
    }
  }

  private List<String> splitPreservingCaseStatements(String input) {
    List<String> results = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    int depth = 0;
    boolean inCase = false;

    for (char c : input.toCharArray()) {
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
      } else if (c == 'C' && current.toString().trim().isEmpty()) {
        // Potential start of CASE
        inCase = true;
      } else if (inCase && current.toString().trim().endsWith("END")) {
        // End of CASE statement
        inCase = false;
      }

      if (c == ',' && depth == 0 && !inCase) {
        results.add(current.toString());
        current = new StringBuilder();
      } else {
        current.append(c);
      }
    }

    if (!current.isEmpty()) {
      results.add(current.toString());
    }

    return results;
  }

  private String[] extractDirectionAndNulls(String expr) {
    String column = expr.trim();
    String direction = null;
    String nullHandling = null;

    // Extract NULLS FIRST/LAST if present
    String[] parts = extractNullHandling(column);
    column = parts[0];
    nullHandling = parts[1];

    // Extract direction if present
    parts = extractDirection(column);
    column = parts[0];
    direction = parts[1];

    return new String[] {column, direction, nullHandling};
  }

  private String[] extractNullHandling(String expr) {
    String column = expr;
    String nullHandling = null;

    String upperExpr = expr.toUpperCase();
    if (upperExpr.endsWith("NULLS LAST") || upperExpr.endsWith("NULLS FIRST")) {
      int nullsIndex = upperExpr.lastIndexOf("NULLS");
      nullHandling = expr.substring(nullsIndex).trim();
      column = expr.substring(0, nullsIndex).trim();
    }

    return new String[] {column, nullHandling};
  }

  private String[] extractDirection(String expr) {
    String column = expr;
    String direction = null;

    int lastSpace = expr.lastIndexOf(' ');
    if (lastSpace > 0) {
      String lastWord = expr.substring(lastSpace + 1).trim().toUpperCase();
      if (lastWord.equals("ASC") || lastWord.equals("DESC")) {
        direction = lastWord;
        column = expr.substring(0, lastSpace).trim();
      }
    }

    return new String[] {column, direction};
  }

  /**
   * Sanitizes the FROM clause by removing any leading or trailing "FROM" keyword.
   *
   * @param input the input string
   * @return the sanitized string
   */
  private String sanitizeFromClause(String input) {
    if (input == null) {
      return null;
    }

    // Trim whitespace and remove leading/trailing "FROM" keyword (case-insensitive)
    String sanitized = input.trim();
    if (sanitized.toUpperCase().startsWith("FROM ")) {
      sanitized = sanitized.substring(5).trim();
    }
    if (sanitized.toUpperCase().endsWith(" FROM")) {
      sanitized = sanitized.substring(0, sanitized.length() - 5).trim();
    }

    return sanitized;
  }
}
