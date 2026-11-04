/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 * ... (license header unchanged) ...
 */
package org.hisp.dhis.sql;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.jsontree.TextBuilder;

/**
 * A directive-driven builder for native SELECT queries. A single SQL text serves both fetch
 * and count; optional filters, joins, order items, and dynamic clauses are erased or replaced
 * at assembly time based on which values were actually supplied.
 *
 * <p>Generic directives ({@code @if}/{@code @else}/{@code @endif}, {@code @insert},
 * {@code @replace}) are processed by {@link DirectiveTemplate}. The only directive handled
 * here is {@code @order}, which is extracted in a pre-pass because its block semantics don't
 * fit the generic open/close model.
 *
 * <h2>Directives</h2>
 * <dl>
 *   <dt>{@code -- @if :name} / {@code -- @else} / {@code -- @endif}</dt>
 *   <dd>Conditional block. See {@link DirectiveTemplate}.</dd>
 *   <dt>{@code -- @endif --@name ...}</dt>
 *   <dd>Resumes the enclosing frame at {@code --@name} on the same line.</dd>
 *   <dt>{@code -- @insert :name}</dt>
 *   <dd>Substitutes SQL registered via {@link #setInsert}; erases the line if unset.</dd>
 *   <dt>{@code -- @replace :condition <replacement>} … {@code -- @replace}</dt>
 *   <dd>Replaces the block body with {@code <replacement>} iff the condition holds;
 *       otherwise keeps the body. The reserved condition {@code count} is true in
 *       {@link #count()} mode and false in fetch mode.</dd>
 *   <dt>{@code -- @order} … {@code -- @order}</dt>
 *   <dd>Dynamic ORDER BY section. Only {@code @order} items may appear inside.</dd>
 *   <dt>{@code -- @order :name}</dt>
 *   <dd>Marks an order item; the SQL that follows (up to the next directive) is the item.</dd>
 * </dl>
 *
 * <h2>Bound vs. unbound values</h2>
 * {@link #setParameter} values are forwarded to the JDBC driver. {@link #setCondition}
 * values are only consulted by directives and never reach the driver. Both share one
 * namespace, so {@code -- @if name} works regardless of how {@code name} was set.
 *
 * @since 2.44
 * @author Jan Bernitt
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class DirectiveQueryBuilder {

  private static final Pattern TRAILING_DIRECTION =
      Pattern.compile("(?i)\\s+(ASC|DESC)(\\s+NULLS\\s+(FIRST|LAST))?\\s*$");

  private final Text template;
  private final SQL.QueryAPI api;

  private final Map<String, SQL.Param> params = new HashMap<>();
  private final Map<String, Boolean> conditions = new HashMap<>();
  private final Map<String, Text> inserts = new HashMap<>();
  private final Map<String, OrderSpec> orders = new LinkedHashMap<>();

  private Integer limit;
  private Integer offset;

  private record OrderSpec(int position, boolean ascending) {}

  private record OrderItem(String name, Text sql, int templateIndex) {}

  private static final class OrderSection {
    final TextBuilder prefix = new TextBuilder();
    final List<OrderItem> items = new ArrayList<>();
    final Set<String> names = new HashSet<>();
    String currentName;
    TextBuilder currentSql;

    void startItem(String name) {
      flush();
      currentName = name;
      currentSql = new TextBuilder();
    }

    void appendLine(Text line) {
      if (currentName == null) {
        prefix.appendEncoded(line);
        prefix.appendAscii('\n');
      } else {
        currentSql.appendEncoded(line);
        currentSql.appendAscii('\n');
      }
    }

    void flush() {
      if (currentName != null) {
        // Text.trim() does not strip line-break characters (\n, \r). Round-trip through
        // String to get strip() semantics. flush() runs once per item; the allocation is
        // negligible and the resulting Text stays usable for downstream appendEncoded.
        Text stripped = Text.of(currentSql.textValue().toString().strip());
        items.add(new OrderItem(currentName, stripped, items.size()));
        currentName = null;
        currentSql = null;
      }
    }
  }

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
   * Sets an <em>unbound</em> boolean consulted by {@code @if}. Conditions never reach the
   * JDBC driver; the latest setter for a name wins (a later {@code setParameter} shadows
   * a condition, and vice versa).
   */
  public DirectiveQueryBuilder setCondition(@Nonnull CharSequence name, boolean value) {
    String n = name.toString();
    params.remove(n);
    conditions.put(n, value);
    return this;
  }

  // ============================== Replacements ==============================

  /** SQL substituted for {@code -- @insert :name}; {@code null}/empty erases the line. */
  public DirectiveQueryBuilder setInsert(
      @Nonnull CharSequence name, @CheckForNull CharSequence sql) {
    String n = name.toString();
    if (sql == null || sql.isEmpty()) inserts.remove(n);
    else inserts.put(n, Text.of(sql));
    return this;
  }

  // ============================== Orders ==============================

  public DirectiveQueryBuilder setOrder(CharSequence name, int position) {
    return setOrder(name, position, true);
  }

  /**
   * Marks {@code name} as present at {@code position}. Direction is used only when the
   * template item does not itself carry {@code ASC}/{@code DESC} (the template wins).
   */
  public DirectiveQueryBuilder setOrder(CharSequence name, int position, boolean ascending) {
    orders.put(name.toString(), new OrderSpec(position, ascending));
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

  @Nonnull
  private SQL.Query fetchQuery() {
    conditions.put("count", false);
    SQL.Query query = api.createQuery(toSQL());
    params.values().forEach(query::setParameter);
    if (offset != null) query.setOffset(offset);
    if (limit != null) query.setLimit(limit);
    return query;
  }

  public int count() {
    conditions.put("count", true);
    SQL.Query query = api.createQuery(toSQL());
    params.values().forEach(query::setParameter);
    return query.count();
  }

  public Map<String, String> listAsStringsMap() {
    return stream().collect(toMap(row -> (String) row[0], row -> (String) row[1]));
  }

  // ============================== Assembly ==============================

  private String toSQL() {
    Text ordered = extractOrderSections(template);
    DirectiveTemplate t = new DirectiveTemplate(this::isDirectiveLine, this::isTrueCondition, inserts::get);
    return t.render(ordered).toString();
  }

  private boolean isDirectiveLine(Text line) {
    int len = line.length();
    int i = 0;
    // skip space
    while (i < len && line.charAt(i) == ' ') i++;
    if (i+2 >= len || line.charAt(i) != '-' || line.charAt(i+1) != '-') return false;
    i+=2; // skip --
    // skip space again
    while (i < len && line.charAt(i) == ' ') i++;
    return i < len && line.charAt(i) == '@';
  }

  private boolean isTrueCondition(String name) {
    SQL.Param p = params.get(name);
    if (p != null) return !isNullish(p.value());
    Boolean c = conditions.get(name);
    return c != null && c;
  }

  /**
   * Pass 1: replaces every {@code @order} section with the SQL it would emit (possibly
   * empty) and leaves every other line untouched, including other directives. Extraction
   * is unconditional — sections inside a false {@code @if} are extracted here and then
   * discarded by pass 2 — so no coordination with the {@code @if} state is needed.
   */
  private Text extractOrderSections(Text input) {
    if (!input.contains("@order")) return input; // fast path

    TextBuilder out = new TextBuilder();
    OrderSection order = null;
    int pos = 0;
    int len = input.length();
    while (pos < len) {
      int nl = input.indexOf('\n', pos);
      int lineEnd = nl < 0 ? len : nl;
      int nextLineStart = nl < 0 ? len : nl + 1;
      Text line = input.subSequence(pos, lineEnd);

      String name = DirectiveTemplate.directive(line);
      if (order != null && name != null && !name.contentEquals("order")) {
        throw new IllegalStateException(
            "Only @order directives may appear inside @order section: " + line);
      }

      if (name != null && name.contentEquals("order")) {
        int nameStart = line.indexOf('@');
        int argStart =
            Math.max(nameStart + 1 + name.length(),
                DirectiveTemplate.indexOfWhitespace(line, nameStart) + 1);
        Text rest = line.subSequence(Math.min(argStart, line.length()), line.length()).trim();

        String arg;
        if (rest.isEmpty()) {
          arg = "";
        } else if (rest.startsWith(":")) {
          arg = rest.subSequence(1, rest.length()).toString().strip();
        } else {
          throw new IllegalStateException("Invalid @order argument: " + line);
        }

        if (arg.isEmpty()) {
          if (order == null) {
            order = new OrderSection();
          } else {
            order.flush();
            emitOrderSection(out, order);
            order = null;
          }
        } else {
          if (order == null) {
            throw new IllegalStateException(
                "@order :" + arg + " outside @order section: " + line);
          }
          if (!order.names.add(arg)) {
            throw new IllegalStateException("Duplicate @order :" + arg + ": " + line);
          }
          order.startItem(arg);
        }
      } else if (order != null) {
        order.appendLine(line);
      } else {
        out.appendEncoded(line);
        out.appendAscii('\n');
      }
      pos = nextLineStart;
    }
    if (order != null) throw new IllegalStateException("Unclosed @order section");
    return out.textValue();
  }

  private void emitOrderSection(TextBuilder out, OrderSection section) {
    record Selected(OrderItem item, OrderSpec spec) {}

    List<Selected> selected = new ArrayList<>();
    for (OrderItem item : section.items) {
      OrderSpec spec = orders.get(item.name());
      if (spec != null) selected.add(new Selected(item, spec));
    }
    if (selected.isEmpty()) return;

    // stripTrailing() via String because Text has no stripTrailing and Text.trim() keeps
    // line-breaks. The prefix is one block per @order section, so the allocation is fine.
    String prefixStr = section.prefix.textValue().toString().stripTrailing();
    if (prefixStr.isEmpty()) {
      throw new IllegalStateException(
          "@order section has items but empty prefix (missing ORDER BY?)");
    }
    selected.sort(
        Comparator.comparingInt((Selected s) -> s.spec.position())
            .thenComparingInt(s -> s.item.templateIndex()));

    out.appendEncoded(Text.of(prefixStr));
    out.appendAscii('\n');
    for (int i = 0; i < selected.size(); i++) {
      if (i > 0) {
        out.appendAscii(',');
        out.appendAscii('\n');
      }
      Text sql = selected.get(i).item.sql();
      out.appendEncoded(sql);
      if (!hasTrailingDirection(sql) && !selected.get(i).spec.ascending()) {
        out.appendEncoded(Text.of(" DESC"));
      }
    }
    out.appendAscii('\n');
  }

  private static boolean hasTrailingDirection(Text sql) {
    return TRAILING_DIRECTION.matcher(sql).find();
  }

  private static boolean isNullish(Object v) {
    if (v == null) return true;
    if (v instanceof Object[] arr) return arr.length == 0;
    if (v instanceof long[] arr) return arr.length == 0;
    return false;
  }
}