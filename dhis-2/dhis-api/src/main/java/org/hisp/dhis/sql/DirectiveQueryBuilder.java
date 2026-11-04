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

/**
 * A directive-driven builder for native SELECT queries. A single SQL text serves both fetch
 * and count; optional filters, joins, order items, and dynamic clauses are erased or replaced
 * at assembly time based on which values were actually supplied.
 *
 * <p>Erasure is explicit: comment lines of the form {@code -- @directive} on their own line
 * mark the boundaries. Text between markers is emitted verbatim, so the raw SQL remains
 * executable in any SQL console.
 *
 * <h2>Directives</h2>
 * <dl>
 *   <dt>{@code -- @if :name}</dt>
 *   <dd>Opens a block, emitted iff {@code name} is a non-null non-empty parameter or a
 *       truthy condition. Blocks nest; the matching {@code @else}/{@code @endif} refer
 *       to the innermost open block.</dd>
 *   <dt>{@code -- @else}</dt>
 *   <dd>Inverts emission of the innermost block. At most one per block.</dd>
 *   <dt>{@code -- @endif}</dt>
 *   <dd>Closes the innermost block.</dd>
 *   <dt>{@code -- @endif --@if :name}</dt>
 *   <dd>The only allowed two-directive line. Closes the current block and opens a
 *       sibling {@code @if} at the same nesting level. Note {@code --@if} (dash-dash
 *       directly before {@code @if}).</dd>
 *   <dt>{@code -- @replace :name}</dt>
 *   <dd>Substitutes raw SQL registered via {@link #setReplace}; erases the line when
 *       unset.</dd>
 *   <dt>{@code -- @count}</dt>
 *   <dd>Paired marker, honored only at the top level. In count mode the region between
 *       pairs becomes {@code count(*)}; in fetch mode both markers are dropped.
 *       {@code @count} in a nested frame is silently ignored.</dd>
 *   <dt>{@code -- @order} … {@code -- @order}</dt>
 *   <dd>Bare toggles open/close a dynamic ORDER BY section. Only {@code @order} items
 *       may appear inside.</dd>
 *   <dt>{@code -- @order :name}</dt>
 *   <dd>Marks an order item inside an {@code @order} section.</dd>
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

  private final String template;
  private final SQL.QueryAPI api;

  private final Map<String, SQL.Param> params = new HashMap<>();
  private final Map<String, Boolean> conditions = new HashMap<>();
  private final Map<String, String> replaces = new HashMap<>();
  private final Map<String, OrderSpec> orders = new LinkedHashMap<>();

  private Integer limit;
  private Integer offset;

  private record OrderSpec(int position, boolean ascending) {}

  private record OrderItem(String name, String sql, int templateIndex) {}

  private enum EndType { ELSE, ENDIF, EOF }

  private record End(EndType type, int pos) {}

  private static final class OrderSection {
    final boolean collecting;
    final StringBuilder prefix = new StringBuilder();
    final List<OrderItem> items = new ArrayList<>();
    final Set<String> names = new HashSet<>();
    String currentName;
    StringBuilder currentSql;

    OrderSection(boolean collecting) {
      this.collecting = collecting;
    }

    void startItem(String name) {
      flush();
      currentName = name;
      currentSql = new StringBuilder();
    }

    void appendLine(String line) {
      if (currentName == null) prefix.append(line).append('\n');
      else currentSql.append(line).append('\n');
    }

    void flush() {
      if (currentName != null) {
        items.add(new OrderItem(currentName, currentSql.toString().strip(), items.size()));
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
      @Nonnull String name, @CheckForNull Object value, @Nonnull SQL.Param.Type type) {
    conditions.remove(name);
    params.put(name, new SQL.Param(type, name, value));
    return this;
  }

  // ============================== Conditions ==============================

  /**
   * Sets an <em>unbound</em> boolean consulted by {@code @if}. Conditions never reach the
   * JDBC driver; the latest setter for a name wins (a later {@code setParameter} shadows
   * a condition, and vice versa).
   */
  public DirectiveQueryBuilder setCondition(@Nonnull String name, boolean value) {
    params.remove(name);
    conditions.put(name, value);
    return this;
  }

  // ============================== Replacements ==============================

  /** SQL substituted for {@code -- @replace :name}; {@code null}/empty erases the line. */
  public DirectiveQueryBuilder setReplace(@Nonnull String name, @CheckForNull String sql) {
    if (sql == null || sql.isEmpty()) replaces.remove(name);
    else replaces.put(name, sql);
    return this;
  }

  // ============================== Orders ==============================

  public DirectiveQueryBuilder setOrder(String name, int position) {
    return setOrder(name, position, true);
  }

  /**
   * Marks {@code name} as present at {@code position}. Direction is used only when the
   * template item does not itself carry {@code ASC}/{@code DESC} (the template wins).
   */
  public DirectiveQueryBuilder setOrder(String name, int position, boolean ascending) {
    orders.put(name, new OrderSpec(position, ascending));
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

  public boolean isNullParameter(String name) {
    SQL.Param p = params.get(name);
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
    SQL.Query query = api.createQuery(toSQL(false));
    params.values().forEach(query::setParameter);
    if (offset != null) query.setOffset(offset);
    if (limit != null) query.setLimit(limit);
    return query;
  }

  public int count() {
    SQL.Query query = api.createQuery(toSQL(true));
    params.values().forEach(query::setParameter);
    return query.count();
  }

  public Map<String, String> listAsStringsMap() {
    return stream().collect(toMap(row -> (String) row[0], row -> (String) row[1]));
  }

  // ============================== Assembly ==============================

  private String toSQL(boolean forCount) {
    StringBuilder out = new StringBuilder(template.length());
    End end = processBlock(template, 0, template.length(), out, true, forCount);
    if (end.type() != EndType.EOF) {
      throw new IllegalStateException(
          "Unbalanced directive at top level: " + end.type().name().toLowerCase());
    }
    return out.toString();
  }

  private End processBlock(
      String input, int startPos, int endPos, StringBuilder out, boolean emit, boolean forCount) {

    OrderSection order = null;
    int pos = startPos;
    int countMarkers = 0;
    boolean inCountSkip = false;

    outer:
    while (pos < endPos) {
      int nl = input.indexOf('\n', pos);
      int lineEnd = (nl < 0 || nl >= endPos) ? endPos : nl;
      int nextLineStart = (nl < 0 || nl >= endPos) ? endPos : nl + 1;
      String line = input.substring(pos, lineEnd);

      String name = parseDirectiveName(line);
      if (name == null) {
        if (order != null) {
          if (order.collecting) order.appendLine(line);
        } else if (emit && !inCountSkip) {
          out.append(line).append('\n');
        }
        pos = nextLineStart;
        continue;
      }

      if (order != null && !name.equals("order")) {
        throw new IllegalStateException(
            "Only @order directives may appear inside @order section: " + line);
      }

      int nameStart = line.indexOf("@" + name);
      int afterName = nameStart + 1 + name.length();
      String rest = line.substring(afterName).strip();

      switch (name) {
        case "if" -> {
          String arg = requireArg(rest, name, line);
          boolean cond = evalCondition(arg);
          End inner = processBlock(input, nextLineStart, endPos, out, emit && cond, false);
          if (inner.type() == EndType.ENDIF) {
            pos = inner.pos();
            continue outer;
          }
          if (inner.type() == EndType.ELSE) {
            End after =
                processBlock(input, inner.pos(), endPos, out, emit && !cond, false);
            if (after.type() == EndType.ENDIF) {
              pos = after.pos();
              continue outer;
            }
            throw new IllegalStateException("Duplicate or unclosed @else in: " + line);
          }
          throw new IllegalStateException("Unclosed @if in: " + line);
        }
        case "else" -> {
          requireNoArg(rest, name, line);
          return new End(EndType.ELSE, nextLineStart);
        }
        case "endif" -> {
          if (rest.isEmpty()) {
            return new End(EndType.ENDIF, nextLineStart);
          }
          if (!rest.equals("--@if") && !rest.startsWith("--@if ")) {
            throw new IllegalStateException(
                "Only '--@if' may follow '@endif' on the same line: " + line);
          }
          int ifPos = line.indexOf("--@if", afterName);
          return new End(EndType.ENDIF, pos + ifPos);
        }
        case "replace" -> {
          String arg = requireArg(rest, name, line);
          if (emit && !inCountSkip) {
            String clause = replaces.get(arg);
            if (clause != null) out.append(clause).append('\n');
          }
        }
        case "count" -> {
          requireNoArg(rest, name, line);
          if (forCount) {
            countMarkers++;
            if (countMarkers % 2 == 1) {
              inCountSkip = true;
            } else {
              out.append("count(*)\n");
              inCountSkip = false;
            }
          }
        }
        case "order" -> {
          String arg;
          if (rest.isEmpty()) {
            arg = "";
          } else if (rest.startsWith(":") && !rest.contains("--@")) {
            arg = rest.substring(1);
          } else {
            throw new IllegalStateException("Invalid @order argument: " + line);
          }

          if (arg.isEmpty()) {
            if (order == null) {
              order = new OrderSection(emit);
            } else {
              order.flush();
              if (order.collecting) emitOrderSection(out, order);
              order = null;
            }
          } else {
            if (order == null) {
              throw new IllegalStateException(
                  "@order :" + arg + " outside @order section: " + line);
            }
            if (order.collecting) {
              if (!order.names.add(arg)) {
                throw new IllegalStateException("Duplicate @order :" + arg + ": " + line);
              }
              order.startItem(arg);
            }
          }
        }
        default ->
            throw new IllegalStateException("Unknown directive @" + name + ": " + line);
      }
      pos = nextLineStart;
    }

    if (order != null) throw new IllegalStateException("Unclosed @order section");
    if (forCount && countMarkers % 2 != 0) {
      throw new IllegalStateException("Odd number of @count markers: " + countMarkers);
    }
    return new End(EndType.EOF, endPos);
  }

  /**
   * Returns the name of the directive at the start of {@code line}, or {@code null} if the
   * line is not a directive line. A directive line is one whose first non-whitespace
   * characters are {@code --}, optionally followed by whitespace, then {@code @}, then a
   * non-empty identifier. Lines that begin with {@code --@} but have no name (e.g. an
   * ordinary SQL comment such as {@code -- @}) return {@code null}.
   */
  private static String parseDirectiveName(String line) {
    int i = 0;
    int n = line.length();
    while (i < n && Character.isWhitespace(line.charAt(i))) i++;
    if (i + 1 >= n || line.charAt(i) != '-' || line.charAt(i + 1) != '-') return null;
    i += 2;
    while (i < n && Character.isWhitespace(line.charAt(i))) i++;
    if (i >= n || line.charAt(i) != '@') return null;
    i++;
    int start = i;
    while (i < n && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_')) i++;
    return i == start ? null : line.substring(start, i);
  }

  private static String requireArg(String rest, String name, String line) {
    if (rest.isEmpty()) {
      throw new IllegalStateException("@" + name + " requires ':name' argument: " + line);
    }
    if (!rest.startsWith(":")) {
      throw new IllegalStateException(
          "@" + name + " argument must start with ':': " + line);
    }
    String arg = rest.substring(1);
    if (arg.isEmpty() || arg.contains("--@")) {
      throw new IllegalStateException("Invalid @" + name + " argument: " + line);
    }
    return arg;
  }

  private static void requireNoArg(String rest, String name, String line) {
    if (!rest.isEmpty()) {
      throw new IllegalStateException("@" + name + " takes no argument: " + line);
    }
  }

  private void emitOrderSection(StringBuilder out, OrderSection section) {
    record Selected(OrderItem item, OrderSpec spec) {}

    List<Selected> selected = new ArrayList<>();
    for (OrderItem item : section.items) {
      OrderSpec spec = orders.get(item.name());
      if (spec != null) selected.add(new Selected(item, spec));
    }
    if (selected.isEmpty()) return;

    String prefix = section.prefix.toString().stripTrailing();
    if (prefix.isEmpty()) {
      throw new IllegalStateException(
          "@order section has items but empty prefix (missing ORDER BY?)");
    }
    selected.sort(
        Comparator.comparingInt((Selected s) -> s.spec.position())
            .thenComparingInt(s -> s.item.templateIndex()));

    out.append(prefix).append('\n');
    for (int i = 0; i < selected.size(); i++) {
      if (i > 0) out.append(",\n");
      String sql = selected.get(i).item.sql();
      if (!hasTrailingDirection(sql) && !selected.get(i).spec.ascending()) {
        sql = sql + " DESC";
      }
      out.append(sql);
    }
    out.append('\n');
  }

  private boolean evalCondition(String name) {
    SQL.Param p = params.get(name);
    if (p != null) return !isNullish(p.value());
    Boolean c = conditions.get(name);
    return c != null && c;
  }

  private static boolean hasTrailingDirection(String sql) {
    return TRAILING_DIRECTION.matcher(sql).find();
  }

  private static boolean isNullish(Object v) {
    if (v == null) return true;
    if (v instanceof Object[] arr) return arr.length == 0;
    if (v instanceof long[] arr) return arr.length == 0;
    return false;
  }
}