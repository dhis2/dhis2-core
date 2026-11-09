/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.jsontree.TextTemplate;

/**
 * A directive-driven builder for native SELECT queries. A single SQL text serves both fetch and
 * count; optional filters, joins and order items are erased, replaced or reordered at assembly time
 * based on which values were actually supplied.
 *
 * <p>All directive processing is delegated to {@link TextTemplate}. This class supplies the
 * four callbacks that bind the template to the query's parameter and condition state, plus a small
 * amount of vocabulary on top (the reserved {@code count} condition, the {@code setOrder}/{@code
 * setInsert} inputs).
 *
 * <h2>Supported directives</h2>
 *
 * See {@link TextTemplate} for the general set ({@code @if}, {@code @else}, {@code @end},
 * {@code @insert}, {@code @replace}, {@code @reorder}). In this class they map to:
 *
 * <dl>
 *   <dt>{@code @if :name} / {@code @else} / {@code @end}
 *   <dd>Consult {@link #setCondition} and {@link #setParameter}. Parameter truthiness is non-null
 *       and non-empty (arrays included).
 *   <dt>{@code @insert :name}
 *   <dd>Substitutes SQL registered via {@link #setInsert}; erased when unset.
 *   <dt>{@code @replace :name repl} … {@code @end}
 *   <dd>Replacement block. The reserved condition {@code count} is true in {@link #count()} mode
 *       and false in fetch mode, so {@code -- @replace :count count(*)} replaces a select list with
 *       {@code count(*)} in count mode.
 *   <dt>{@code @reorder :name} … {@code @end}
 *   <dd>Consecutive siblings sorted by {@link #setOrderBy}. Direction is inline in the template;
 *       for dynamic direction, register an {@code @insert} whose text includes the direction and
 *       reference it inside the block. Blocks without a {@link #setOrderBy} entry sort to the end
 *       (in template order), so wrap the whole sequence in {@code @if} to drop it entirely when no
 *       orders are chosen.
 * </dl>
 *
 * <h2>Bound vs. unbound values</h2>
 *
 * {@link #setParameter} values are forwarded to the JDBC driver. {@link #setCondition} values are
 * only consulted by directives and never reach the driver. Both share one namespace.
 *
 * @since 2.44
 * @author Jan Bernitt
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class DirectiveQueryBuilder {

  private final Text template;
  private final SQL.QueryAPI api;

  private final Map<String, SQL.Param> params = new HashMap<>();
  private final Map<String, Boolean> conditions = new HashMap<>();
  private final Map<String, Text> inserts = new HashMap<>();
  private final Map<String, Integer> orders = new HashMap<>();

  private Integer limit;
  private Integer offset;

  // ============================== Parameters ==============================

  public <T> DirectiveQueryBuilder setParameter(
      String name, Collection<T> value, Function<T, String> f) {
    return setParameter(name, value == null ? Stream.<T>empty() : value.stream(), f);
  }

  public <T> DirectiveQueryBuilder setParameter(
      String name, Stream<T> value, Function<T, String> f) {
    String[] arr =
        value
            .filter(Objects::nonNull)
            .map(f)
            .filter(Objects::nonNull)
            .distinct()
            .toArray(String[]::new);
    return setParameter(name, arr, SQL.Param.Type.STRING_ARRAY);
  }

  public DirectiveQueryBuilder setParameter(String name, Collection<UID> value) {
    return setParameter(name, value, UID::getValue);
  }

  public DirectiveQueryBuilder setParameter(String name, Stream<UID> value) {
    return setParameter(name, value, UID::getValue);
  }

  public DirectiveQueryBuilder setParameter(String name, String... value) {
    return setParameter(name, value, SQL.Param.Type.STRING_ARRAY);
  }

  public DirectiveQueryBuilder setParameter(String name, long... value) {
    return setParameter(name, value, SQL.Param.Type.LONG_ARRAY);
  }

  public DirectiveQueryBuilder setParameter(String name, UID value) {
    return setParameter(name, value == null ? null : value.getValue(), SQL.Param.Type.STRING);
  }

  public DirectiveQueryBuilder setParameter(String name, String value) {
    return setParameter(name, value, SQL.Param.Type.STRING);
  }

  public DirectiveQueryBuilder setParameter(String name, Date value) {
    return setParameter(name, value, SQL.Param.Type.DATE);
  }

  public DirectiveQueryBuilder setParameter(String name, Integer value) {
    return setParameter(name, value, SQL.Param.Type.INTEGER);
  }

  public DirectiveQueryBuilder setParameter(String name, Boolean value) {
    return setParameter(name, value, SQL.Param.Type.BOOLEAN);
  }

  private DirectiveQueryBuilder setParameter(
      @Nonnull CharSequence name, @CheckForNull Object value, @Nonnull SQL.Param.Type type) {
    String n = name.toString();
    conditions.remove(n);
    params.put(n, new SQL.Param(type, n, value));
    return this;
  }

  // ============================== Conditions ==============================

  /**
   * Sets an <em>unbound</em> boolean consulted by {@code @if}. Conditions never reach the JDBC
   * driver; the latest setter for a name wins (a later {@code setParameter} shadows a condition,
   * and vice versa).
   */
  public DirectiveQueryBuilder setCondition(@Nonnull CharSequence name, boolean value) {
    String n = name.toString();
    params.remove(n);
    conditions.put(n, value);
    return this;
  }

  // ============================== Insertions ==============================

  /** SQL substituted for {@code -- @insert :name}; {@code null}/empty erases the line. */
  public DirectiveQueryBuilder setInsert(
      @Nonnull CharSequence name, @CheckForNull CharSequence sql) {
    String n = name.toString();
    if (sql == null || sql.isEmpty()) inserts.remove(n);
    else inserts.put(n, Text.of(sql));
    return this;
  }

  // ============================== Orders ==============================

  /**
   * Marks {@code name} as present at {@code position} for sorting {@code @reorder} blocks.
   * Direction is inline in the template. To omit a block, wrap the sequence in {@code @if} and pass
   * {@code false} via {@link #setCondition}; to change direction dynamically, register an
   * {@code @insert} whose text includes the direction and reference it inside the block.
   */
  public DirectiveQueryBuilder setOrderBy(@Nonnull CharSequence name, int position) {
    orders.put(name.toString(), position);
    return this;
  }

  public DirectiveQueryBuilder setOrderBy(@Nonnull CharSequence... names) {
    for (int i = 0; i < names.length; i++) setOrderBy(names[i], i);
    return this;
  }

  private static final Text DESC = Text.of("DESC");

  public <T> DirectiveQueryBuilder setOrderBy(
      List<T> order, Function<T, String> name, Predicate<T> isAscending) {
    for (int i = 0; i < order.size(); i++) {
      T e = order.get(i);
      String n = name.apply(e);
      setOrderBy(n, i);
      if (!isAscending.test(e)) inserts.put(n, DESC);
    }
    return this;
  }

  public DirectiveQueryBuilder setLimit(@CheckForNull Integer limit) {
    this.limit = limit == null ? null : Math.abs(limit);
    return this;
  }

  public DirectiveQueryBuilder setOffset(@CheckForNull Integer offset) {
    this.offset = offset == null ? null : Math.abs(offset);
    return this;
  }

  public boolean isNullParameter(CharSequence name) {
    SQL.Param p = params.get(name.toString());
    return p != null && isNullish(p.value());
  }

  // ============================== Execution ==============================

  public Stream<Object[]> stream() {
    return stream(Object[].class);
  }

  public <T> Stream<T> stream(Class<T> rowType) {
    return fetchQuery().stream(rowType);
  }

  public <T> Stream<T> stream(Function<SQL.Row, T> map) {
    return fetchQuery().stream(map);
  }

  public Map<String, String> listAsStringsMap() {
    return stream().collect(toMap(row -> (String) row[0], row -> (String) row[1]));
  }

  @Nonnull
  private SQL.Query fetchQuery() {
    conditions.put("count", false);
    SQL.Query query = api.createQuery(toSQL());
    params.values().stream().filter(p -> !isNullish(p.value())).forEach(query::setParameter);
    if (offset != null) query.setOffset(offset);
    if (limit != null) query.setLimit(limit);
    return query;
  }

  public int count() {
    conditions.put("count", true);
    SQL.Query query = api.createQuery(toSQL());
    params.values().stream().filter(p -> !isNullish(p.value())).forEach(query::setParameter);
    return query.count();
  }

  // ============================== Assembly ==============================

  private String toSQL() {
    return new TextTemplate(
            "--",
            this::isTrueCondition,
            inserts::get,
            this::getOrderByNo)
        .render(template)
        .toString();
  }

  private int getOrderByNo(String name) {
    Integer pos = orders.get(name);
    return pos != null ? pos : Integer.MAX_VALUE;
  }

  private boolean isTrueCondition(String name) {
    SQL.Param p = params.get(name);
    if (p != null) return !isNullish(p.value());
    Boolean c = conditions.get(name);
    return c != null && c;
  }

  private static boolean isNullish(Object v) {
    if (v == null) return true;
    if (v instanceof Object[] arr) return arr.length == 0;
    if (v instanceof long[] arr) return arr.length == 0;
    return false;
  }
}
