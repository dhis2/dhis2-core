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
package org.hisp.dhis.sql;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;

/**
 * A simpler (to use), safer wrapper around low level native APIs to do SELECT queries.
 *
 * <p>The API is specifically designed to work nicely when SQL should support a predefined set of
 * filters and orders. The SQL provided should then simply apply them all, those not used based on
 * bindings are erased. It is not meant to be used for dynamically composed SQL. Instead, it is
 * provided with the SQL that would apply all possible filters and orders and then erases those
 * bound to a null (or empty array) value. That does not mean named parameters can be left unbound.
 * It means when they are bound to {@code null} (or empty array) this qualifies the parameter for
 * erasure if and only if (and when) {@link #eraseNullParameterLines()} is called.
 *
 * <p>The fundamental assumption of erasure is that the SQL provided can be processed line by line
 * using simple string find + replace techniques. The input SQL is NOT parsed into an AST and
 * recomposed from an updated AST. It is a strict line-pattern based substitution that can be
 * exploited by an author to get a convenient erasure. Formatting the SQL properly for this
 * conveniently also makes for very readable and reasonably formatted SQL.
 *
 * @since 2.43
 * @author Jan Bernitt
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class QueryBuilder {

  /**
   * The pattern used to transform...
   *
   * <pre>
   *   WHERE AND => WHERE
   *   WHERE 1=1 AND => WHERE
   * </pre>
   */
  private static final Pattern WHERE_AND =
      Pattern.compile("([\n\t ]+)WHERE[\n\t ]+(?:1=1)?[\n\t ]+AND[\n\t ]+");

  private final String sql;

  private final SQL.QueryAPI api;
  private final Map<String, SQL.Param> params = new HashMap<>();
  private final Map<String, Boolean> orders = new HashMap<>();
  private final Map<String, String> clauses = new HashMap<>();
  private final Map<String, Set<String>> erasedJoins = new HashMap<>();
  private final Set<String> erasedOrders = new HashSet<>();
  private final Set<String> erasedClause = new HashSet<>();
  private final Set<String> nullParams = new HashSet<>();
  private final Set<String> erasedParams = new HashSet<>();
  private final Set<String> eqParams = new HashSet<>();
  private Integer limit;
  private Integer offset;

  public <T> QueryBuilder setParameter(String name, Collection<T> value, Function<T, String> f) {
    return setParameter(name, value == null ? Stream.empty() : value.stream(), f);
  }

  public <T> QueryBuilder setParameter(String name, Stream<T> value, Function<T, String> f) {
    String[] arr =
        value
            .filter(Objects::nonNull) // from was null
            .map(f)
            .filter(Objects::nonNull) // to was null
            .distinct()
            .toArray(String[]::new);
    return setParameter(name, arr, SQL.Param.Type.STRING_ARRAY);
  }

  public QueryBuilder setParameter(String name, Collection<UID> value) {
    return setParameter(name, value, UID::getValue);
  }

  public QueryBuilder setParameter(String name, Stream<UID> value) {
    return setParameter(name, value, UID::getValue);
  }

  public QueryBuilder setParameter(String name, String... value) {
    return setParameter(name, value, SQL.Param.Type.STRING_ARRAY);
  }

  public QueryBuilder setParameter(String name, Long... value) {
    return setParameter(name, value, SQL.Param.Type.LONG_ARRAY);
  }

  public QueryBuilder setParameter(String name, UID value) {
    return setParameter(name, value == null ? null : value.getValue(), SQL.Param.Type.STRING);
  }

  public QueryBuilder setParameter(String name, String value) {
    return setParameter(name, value, SQL.Param.Type.STRING);
  }

  public QueryBuilder setParameter(String name, Date value) {
    return setParameter(name, value, SQL.Param.Type.DATE);
  }

  public QueryBuilder setParameter(String name, Integer value) {
    return setParameter(name, value, SQL.Param.Type.INTEGER);
  }

  public QueryBuilder setParameter(String name, Boolean value) {
    return setParameter(name, value, SQL.Param.Type.BOOLEAN);
  }

  private QueryBuilder setParameter(
      @Nonnull String name, @CheckForNull Object value, @Nonnull SQL.Param.Type type) {
    params.put(name, new SQL.Param(type, name, value));
    if (value == null || value instanceof Object[] arr && arr.length == 0) nullParams.add(name);
    return this;
  }

  public <T> QueryBuilder setOrders(
      @CheckForNull List<T> orders,
      @Nonnull Function<T, String> property,
      @Nonnull Predicate<T> ascending,
      @Nonnull Map<String, String> propToColumn) {
    if (orders == null || orders.isEmpty() || propToColumn.isEmpty()) return this;
    for (T order : orders) {
      String column = propToColumn.get(property.apply(order));
      if (column != null) this.orders.put(column, ascending.test(order));
    }
    return this;
  }

  /**
   * A dynamic SQL clause exploits the fact that a named parameter placeholder could be understood
   * as a boolean variable. Just that in case of dynamic clauses it is not bound as a named
   * parameter but replaced with the provided SQL. If that SQL is null or empty the line containing
   * the named placeholder is always erased.
   *
   * <p>The SQL provided could potentially enable SQL injection attacks, so callers have to be
   * careful to only provide SQL that is considered safe in that regard.
   */
  public QueryBuilder setDynamicClause(@Nonnull String name, @CheckForNull String sql) {
    if (sql == null || sql.isEmpty()) {
      erasedClause.add(name);
    } else {
      clauses.put(name, sql);
    }
    return this;
  }

  /**
   * @implNote Assumes that given qualified name occurs somewhere in the ORDER BY clause of the
   *     provided SQL. When #erase is true the ORDER BY clause is modified to not include the named
   *     order. For that the qualified named given in the ORDER BY clause has to match the provided
   *     name exactly. It can be followed by ASC or DESC.
   *     <p>This means the priority (sequence) and direction of orders is always given by the ORDER
   *     BY clause of the provided SQL.
   */
  public QueryBuilder eraseOrder(String qname, boolean erase) {
    if (erase) erasedOrders.add(qname);
    return this;
  }

  public QueryBuilder setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public QueryBuilder setOffset(Integer offset) {
    this.offset = offset;
    return this;
  }

  /**
   * When this method is called all null (or empty array) parameters set thus far will be erased.
   * Null parameters set afterward are kept to allow using a mix where some parameter can be set to
   * null and kept.
   *
   * <p>This means: when erasure should apply to all named parameters this method must be called
   * after all set parameter calls.
   *
   * @implNote
   *     <p>Splits the SQL into lines, finds the lines containing named parameters and removes them
   *     if the parameter is set to null (or an empty array). It is up to the author of the SQL to
   *     write the SQL in a way that leaves the SQL in a correct state when this erasure is
   *     performed. This also means the author has to pick names that do not wrongly match SQL that
   *     isn't actually a named parameter. Note that such accidents would only be possible if the
   *     SQL contains {@code :} used in other roles than named parameters, for example in a string
   *     literal, type cast, or other operator.
   *     <p>Empty arrays here are always considered to be equivalent to null in their semantic since
   *     the caller should have short-circuited the entire query if the empty array parameter should
   *     mean an impossible condition like {@code x IN ()}
   */
  public QueryBuilder eraseNullParameterLines() {
    erasedParams.addAll(nullParams);
    return this;
  }

  /**
   * Erase a JOIN clause to a named table given by alias in case all the given parameters are indeed
   * considered null and therefore erased when calling {@link #eraseNullParameterLines()} making the
   * JOIN unnecessary.
   *
   * @implNote As the name indicates this will always erase an entire line should it be identified
   *     as the only using the given alias.
   * @param alias table name alias of the tabled joined
   * @param nullParams names of the parameters that require the join
   * @return this for chaining
   */
  public QueryBuilder eraseNullParameterJoinLine(String alias, String... nullParams) {
    erasedJoins.put(alias, Set.of(nullParams));
    return this;
  }

  /**
   * For each of the given named parameters a SQL {@code IN(:name)} or {@code ANY(:name)} is
   * replaced with {@code = :name} if the current value for {@code name} is a single value.
   *
   * @param params names of the parameters that allow IN/ANY to {@code =} replacement
   * @return this for chaining
   */
  public QueryBuilder useEqualsOverInForParameters(String... params) {
    eqParams.addAll(List.of(params));
    return this;
  }

  /**
   * @apiNote The API intentionally does NOT provide a {@code list()} method as more often than not
   *     results are further transformed which is better done using stream processing to avoid
   *     unnecessary intermediate collections (high memory load). The absence of {@code list()}
   *     should enforce callers to at least consider this options. It is always easy enough to call
   *     {@link Stream#toList()} on the returned stream if lists are required.
   * @return a stream of result rows
   */
  public Stream<Object[]> stream() {
    return stream(Object[].class);
  }

  public <T> Stream<T> stream(Class<T> rowType) {
    return fetchQuery().stream(rowType);
  }

  public <T> Stream<T> stream(Function<SQL.Row, T> map) {
    return fetchQuery().stream(map);
  }

  @Nonnull
  private SQL.Query fetchQuery() {
    String minSql = toSQL(false);
    SQL.Query query = api.createQuery(minSql);
    params.values().stream()
        .filter(p -> !erasedParams.contains(p.name()))
        .forEach(query::setParameter);
    if (offset != null) query.setOffset(offset);
    if (limit != null) query.setLimit(limit);
    return query;
  }

  /**
   * Count does allow to use the same provided SQL used for fetch as it erases the SELECT list of
   * the main query and replaces it with {@code count(*)}.
   *
   * <p>It also allows to call {@link #setLimit(Integer)} and/or {@link #setOffset(Integer)} without
   * affecting the count.
   */
  public int count() {
    SQL.Query query = api.createQuery(toSQL(true));
    params.values().stream()
        .filter(p -> !erasedParams.contains(p.name()))
        .forEach(query::setParameter);
    return query.count();
  }

  public Map<String, String> listAsStringsMap() {
    return stream().collect(toMap(row -> (String) row[0], row -> (String) row[1]));
  }

  private String toSQL(boolean forCount) {
    String sql = eraseNullParams(this.sql);
    sql = eraseNullClauses(sql);
    sql = eraseNullJoins(sql);
    sql = eraseOrders(sql, forCount);
    sql = eraseComments(sql);
    sql = replaceDynamicClauses(sql);
    sql = simplifyWhere(sql);
    sql = simplifyIn(sql);
    sql = addOrderBy(sql);
    return forCount ? replaceSelect(sql) : sql;
  }

  private String eraseNullParams(String sql) {
    if (erasedParams.isEmpty()) return sql;
    return sql.lines().filter(not(this::containsErasedParameter)).collect(joining("\n"));
  }

  private String eraseOrders(String sql, boolean forCount) {
    if (forCount || !orders.isEmpty()) return eraseAllOrders(sql);
    if (erasedOrders.isEmpty()) return sql;
    return sql.lines().map(this::replaceOrders).collect(joining("\n"));
  }

  private String eraseAllOrders(String sql) {
    return sql.lines().filter(not(this::isOrderBy)).collect(joining("\n"));
  }

  private String eraseNullJoins(String sql) {
    if (erasedJoins.isEmpty() || erasedParams.isEmpty()) return sql;
    return sql.lines().filter(not(this::containsErasedJoin)).collect(joining("\n"));
  }

  private String eraseNullClauses(String sql) {
    if (erasedClause.isEmpty()) return sql;
    return sql.lines().filter(not(this::containsErasedClause)).collect(joining("\n"));
  }

  private String eraseComments(String sql) {
    return sql.lines()
        .map(this::replaceComments)
        .filter(not(String::isBlank))
        .collect(joining("\n"));
  }

  private String simplifyWhere(String sql) {
    return WHERE_AND.matcher(sql).replaceAll("$1WHERE ");
  }

  private String simplifyIn(String sql) {
    if (eqParams.isEmpty()) return sql;
    for (String name : eqParams) {
      SQL.Param param = params.get(name);
      if (param != null && param.value() instanceof Object[] arr && arr.length == 1) {
        String newSql =
            sql.replaceAll(
                    "([\n\t ]+)IN[\n\t ]*\\([\n\t ]*:" + name + "[\n\t ]*\\)", "$1= :" + name + " ")
                .replaceAll(
                    "([\n\t ]+)ANY[\n\t ]*\\([\n\t ]*:" + name + "[\n\t ]*\\)", "$1:" + name + " ");
        if (!newSql.equals(sql)) {
          sql = newSql;
          params.put(name, new SQL.Param(param.type().elementType(), name, arr[0]));
        }
      }
    }
    // remove trailing whitespace
    return sql.replaceAll("(?m)[ \\t]+$", "");
  }

  private boolean containsErasedParameter(String line) {
    return erasedParams.stream().anyMatch(name -> containsNamedPlaceholder(line, name));
  }

  private boolean containsNamedPlaceholder(String line, String name) {
    int iName = line.indexOf(":" + name);
    if (iName < 0) return false;
    int iAfter = iName + 1 + name.length();
    return iAfter >= line.length() || !isAlphanumericChar(line.charAt(iAfter));
  }

  private boolean containsErasedJoin(String line) {
    int iJoin = line.indexOf("JOIN ");
    if (iJoin < 0) return false;
    int iOn = line.indexOf(" ON ", iJoin + 5);
    if (iOn < 0) return false; // give up
    int iSpace = line.lastIndexOf(' ', iOn - 1);
    String alias = line.substring(iSpace, iOn).trim();
    if (!erasedJoins.containsKey(alias)) return false;
    return erasedParams.containsAll(erasedJoins.get(alias));
  }

  private boolean containsErasedClause(String line) {
    return erasedClause.stream().anyMatch(name -> containsNamedPlaceholder(line, name));
  }

  private boolean containsErasedOrder(String order) {
    return erasedOrders.stream().anyMatch(order::startsWith);
  }

  private String replaceDynamicClauses(String sql) {
    if (clauses.isEmpty()) return sql;
    return sql.lines()
        .map(
            line -> {
              for (Map.Entry<String, String> clause : clauses.entrySet()) {
                String name = clause.getKey();
                if (containsNamedPlaceholder(line, name)) {
                  line = line.replace(":" + name, clause.getValue());
                }
              }
              return line;
            })
        .collect(joining("\n"));
  }

  private String replaceComments(String line) {
    if (!line.contains("--")) return line;
    // from back of the line walk only alphanumeric and whitespace and see of there is a --
    char[] chars = line.toCharArray();
    int i = line.length() - 1;
    while (i >= 0) {
      while (i >= 0 && isCommentChar(chars[i])) i--;
      if (i < 0) return line;
      if (chars[i] == '-' && i > 0 && chars[i - 1] == '-') {
        // found a comment
        i--;
        while (i > 1 && " \t".indexOf(chars[i - 1]) >= 0) i--;
        return line.substring(0, i);
      }
      i--;
    }
    return line;
  }

  private String addOrderBy(String sql) {
    if (orders.isEmpty()) return sql;
    return sql
        + "\nORDER BY "
        + orders.entrySet().stream()
            .map(e -> e.getKey() + (e.getValue() ? "" : " DESC"))
            .collect(joining(", "));
  }

  private static boolean isCommentChar(char c) {
    return isAlphanumericChar(c) || "+:;,._ \t".indexOf(c) >= 0;
  }

  private static boolean isAlphanumericChar(char c) {
    return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
  }

  /**
   * @implNote Assumes the main ORDER BY clause is on a separate line and that all orders are listed
   *     on the same line. Orders may have ASC or DESC or none (default). If all orders get erased
   *     the entire line will be erased.
   */
  private String replaceOrders(String line) {
    if (!isOrderBy(line)) return line;
    int idx = line.indexOf("ORDER BY ");
    String[] allOrders = line.substring(idx + 9).split("\\s*,\\s*");
    List<String> keptOrders = Stream.of(allOrders).filter(not(this::containsErasedOrder)).toList();
    if (keptOrders.isEmpty()) return ""; // erase entire ORDER BY line
    return line.substring(0, idx + 9) + String.join(" , ", keptOrders);
  }

  private boolean isOrderBy(String line) {
    int idx = line.indexOf("ORDER BY ");
    if (idx < 0) return false;
    // make sure it is not a nested order
    return line.trim().startsWith("ORDER BY ");
  }

  /**
   * @implNote Assumes the main SELECT starts on a line (no indent), in which case it replaces the
   *     list of extracted fields with {@code count(*)}.
   */
  private String replaceSelect(String sql) {
    List<String> lines = sql.lines().toList();
    // look for line starts SELECT (no indent to avoid accidentally modifying sub-selects as well)
    int n = lines.size();
    int si = 0;
    while (si < n && !lines.get(si).startsWith("SELECT")) si++;
    if (si >= n) return sql; // give up
    // found the SELECT, look for FROM
    int fi = si;
    while (fi < n && !lines.get(fi).contains("FROM")) fi++;
    if (fi >= n) return sql; // give up
    String preLines = si == 0 ? "" : String.join("\n", lines.subList(0, si));
    String postLines = String.join("\n", lines.subList(fi, n));
    if (si == fi) {
      // SELECT ... FROM in one line
      String line = lines.get(si);
      return preLines
          + "SELECT count(*)"
          + line.substring(line.indexOf(" FROM"))
          + "\n"
          + postLines;
    }
    // SELECT
    // ...
    // FROM
    return preLines + "SELECT count(*)\n" + postLines;
  }
}
