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
package org.hisp.dhis.datastore;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;

/**
 * Details of a query as it can be performed to fetch {@link DatastoreEntry}s.
 *
 * @author Jan Bernitt
 */
@ToString
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DatastoreQuery {
  /**
   * Filters allow values with {@code [a,b]} syntax where a comma occurs as part of the value. These
   * commas need to be ignored when splitting parameter list.
   */
  private static final String FILTER_SPLIT = ",(?![^\\[\\]]*\\]|[^\\(\\)]*\\))";

  public static final Order KEY_ASC = new Order("_", Direction.ASC);

  private final String namespace;

  /**
   * By default, only entries which have at least one non-null value for the extracted fields are
   * returned. If all are included even matches with only null values are included in the result
   * list.
   */
  private final boolean includeAll;

  private final boolean headless;

  /**
   * Use OR instead of AND between filters so that any match for one of the filters is a match.
   * Default false.
   */
  private final boolean anyFilter;

  @Builder.Default private final boolean paging = true;

  @Builder.Default private final int page = 1;

  @Builder.Default private final int pageSize = 50;

  @Builder.Default private final List<Field> fields = emptyList();

  @Builder.Default private final List<Filter> filters = emptyList();

  @Builder.Default private final Order order = KEY_ASC;

  public enum Direction {
    ASC,
    DESC,
    NASC,
    NDESC;

    @Override
    public String toString() {
      return name().toLowerCase();
    }

    public boolean isNumeric() {
      return this == NASC || this == NDESC;
    }
  }

  public interface HasPath {
    String getPath();

    default boolean isKeyPath() {
      return getPath().equals("_");
    }

    default boolean isValuePath() {
      return getPath().equals(".");
    }
  }

  @Getter
  public static final class Order implements HasPath {
    private final String path;

    private final Direction direction;

    public Order(String path, Direction direction) {
      this.path = normalisePath(path);
      this.direction = direction;
    }

    public static Order parse(String order) {
      String[] parts = order.split("(::|:|~|@)");
      if (parts.length == 1) {
        return new Order(order, Direction.ASC);
      }
      if (parts.length == 2) {
        return new Order(parts[0], Direction.valueOf(parts[1].toUpperCase()));
      }
      throw new IllegalArgumentException("Not a valid order expression: " + order);
    }

    @Override
    public String toString() {
      return path + ":" + direction;
    }
  }

  @Getter
  public static final class Field implements HasPath {
    private final String path;

    private final String alias;

    public Field(String path) {
      this(path, null);
    }

    public Field(String path, String alias) {
      this.path = normalisePath(path);
      this.alias = alias != null ? alias : ".".equals(path) ? "value" : path;
    }

    @Override
    public String toString() {
      return alias.equals(path) ? path : path + "(" + alias + ")";
    }
  }

  @Getter
  public static final class Filter implements HasPath {
    private final String path;

    private final Comparison operator;

    private final String value;

    public Filter(String path, Comparison operator, String value) {
      this.path = normalisePath(path);
      this.operator = operator;
      this.value = value;
    }

    @Override
    public String toString() {
      return value.isEmpty() ? path + ":" + operator : path + ":" + operator + ":" + value;
    }

    public boolean isNullValue() {
      return "null".equals(value);
    }
  }

  public enum Comparison {
    // unary
    NULL("null"),
    NOT_NULL("!null"),
    EMPTY("empty"),
    NOT_EMPTY("!empty"),

    IN("in"),
    NOT_IN("!in"),

    // equality
    EQUAL("eq"),
    NOT_EQUAL("!eq", "neq", "ne"),

    // numeric or alphabetic
    LESS_THAN("lt"),
    LESS_THAN_OR_EQUAL("lte", "le"),
    GREATER_THAN("gt"),
    GREATER_THAN_OR_EQUAL("gte", "ge"),

    // case-sensitive pattern matching
    LIKE("like"),
    NOT_LIKE("!like"),
    STARTS_LIKE("$like"),
    NOT_STARTS_LIKE("!$like"),
    ENDS_LIKE("like$"),
    NOT_ENDS_LIKE("!like$"),

    // case-insensitive pattern matching
    IEQ("ieq"),
    ILIKE("ilike"),
    NOT_ILIKE("!ilike"),
    STARTS_WITH("$ilike", "startswith"),
    NOT_STARTS_WITH("!$ilike", "!startswith"),
    ENDS_WITH("ilike$", "endswith"),
    NOT_ENDS_WITH("!ilike$", "!endswith");

    private Set<String> operators;

    Comparison(String... operators) {
      this.operators = Set.of(operators);
    }

    public boolean isUnary() {
      return ordinal() >= NULL.ordinal() && ordinal() <= NOT_EMPTY.ordinal();
    }

    public boolean isCaseInsensitive() {
      return ordinal() >= IEQ.ordinal();
    }

    public boolean isIn() {
      return this == IN || this == NOT_IN;
    }

    public boolean isTextBased() {
      return ordinal() >= LIKE.ordinal();
    }

    public boolean isStartFlexible() {
      return isTextBased()
          && !Set.of(IEQ, STARTS_LIKE, STARTS_WITH, NOT_STARTS_LIKE, NOT_STARTS_WITH)
              .contains(this);
    }

    public boolean isEndFlexible() {
      return isTextBased()
          && !Set.of(IEQ, ENDS_LIKE, ENDS_WITH, NOT_ENDS_LIKE, NOT_ENDS_WITH).contains(this);
    }

    public static Comparison parse(String operator) {
      for (Comparison c : values()) {
        if (c.operators.contains(operator.toLowerCase())) {
          return c;
        }
      }
      throw new IllegalQueryException(
          "Unknown operator: `"
              + operator
              + "` Use one of "
              + stream(values()).flatMap(e -> e.operators.stream()).collect(toSet()));
    }

    @Override
    public String toString() {
      return operators.iterator().next();
    }
  }

  public DatastoreQuery with(DatastoreParams params) {
    int pageNo = max(1, params.getPage());
    int size = max(1, min(1000, params.getPageSize()));
    boolean isPaging = params.isPaging();
    return toBuilder()
        .headless(params.isHeadless() || !isPaging)
        .anyFilter(params.getRootJunction() == DatastoreParams.Junction.OR)
        .order(Order.parse(params.getOrder()))
        .paging(isPaging)
        .page(pageNo)
        .pageSize(size)
        .filters(parseFilters(getFilters(params.getFilter())))
        .build();
  }

  private static List<String> getFilters(String value) {
    if (value == null || value.isEmpty()) {
      return emptyList();
    }
    return asList(value.split(FILTER_SPLIT));
  }

  /**
   * Parses the fields URL parameter form to a list of {@link Field}s.
   *
   * <p>In text form fields can describe nested fields in two forms:
   *
   * <pre>
   *   root[child]
   *   root[child1,child2]
   *   root[level1[level2]
   * </pre>
   *
   * which is similar to the second form using dot
   *
   * <pre>
   *   root.child
   *   root.child1,root.child2
   *   root.level1.level2
   * </pre>
   *
   * E leaf in this text form can be given an alias in round braces:
   *
   * <pre>
   *   root(alias)
   *   root[child(alias)]
   * </pre>
   *
   * @param fields a comma separated list of fields
   * @return the object form of the text representation given if valid
   * @throws IllegalQueryException in case the provided text form is not valid
   */
  public static List<Field> parseFields(String fields) {
    final List<Field> flat = new ArrayList<>();
    final int len = fields.length();
    String parentPath = "";
    int start = 0;
    while (start < len) {
      int end = findNameEnd(fields, start);
      String field = fields.substring(start, end);
      start = end + 1;
      if (end >= len) {
        addNonEmptyTo(flat, parentPath, field);
        return flat;
      }
      char next = fields.charAt(end);
      if (next == ',') {
        addNonEmptyTo(flat, parentPath, field);
      } else if (next == '[' || next == '(') {
        parentPath += field + ".";
      } else if (next == ']' || next == ')') {
        addNonEmptyTo(flat, parentPath, field);
        parentPath =
            parentPath.substring(0, parentPath.lastIndexOf('.', parentPath.length() - 2) + 1);
      } else {
        throw new IllegalQueryException(new ErrorMessage(ErrorCode.E7651, end, next));
      }
    }
    return flat;
  }

  public static List<Filter> parseFilters(List<String> filterExpressions) {
    Function<String, Filter> toFilter =
        expr -> {
          String[] parts = expr.split("(::|:|~|@)");
          if (parts.length < 2) {
            throw new IllegalQueryException(
                new ErrorMessage(
                    ErrorCode.E7652,
                    expr,
                    "expected expression of pattern: <property>:<operator>[:<value>]"));
          }
          String value = stream(parts).skip(2).collect(Collectors.joining(":"));
          return new Filter(parts[0], Comparison.parse(parts[1].toUpperCase()), value);
        };
    return filterExpressions.stream().map(toFilter).collect(toList());
  }

  /**
   * Adds a {@link Field} to the provided fields list of the leave field name is not empty (which
   * would indicate that we are not at the end of a new field in the parsing process).
   *
   * @param fields list of fields to add to
   * @param parent parent path (might contain dotted segments)
   * @param field leaf path (no dotted segments)
   */
  private static void addNonEmptyTo(List<Field> fields, String parent, String field) {
    if (!field.isEmpty()) {
      int aliasStart = field.indexOf("~hoist(");
      String name = aliasStart > 0 ? field.substring(0, aliasStart) : field;
      String alias = aliasStart > 0 ? field.substring(aliasStart + 7, field.length() - 1) : null;
      fields.add(new Field(parent + name, alias));
    }
  }

  /**
   * @param fields search text
   * @param start start index in the search text
   * @return first index in the fields string that is not a valid name character starting from the
   *     start position.
   */
  private static int findNameEnd(String fields, int start) {
    int pos = start;
    while (pos < fields.length() && isNameCharacter(fields.charAt(pos))) {
      pos++;
    }
    return findAliasEnd(fields, pos);
  }

  /**
   * @param fields search text
   * @param start start position in search text
   * @return first index in the fields string that is after a potential alias. This assumes the
   *     start position must point to the start of an alias or no alias is present.
   */
  private static int findAliasEnd(String fields, int start) {
    if (start >= fields.length() || fields.charAt(start) != '~') {
      return start;
    }
    return fields.indexOf(')', start) + 1;
  }

  private static boolean isNameCharacter(char c) {
    return isLetterOrDigit(c) || c == '.';
  }

  /**
   * A valid path can have up to 5 levels each with an alphanumeric name between 1 and 32 characters
   * long and levels being separated by a dot.
   *
   * <p>The path needs to be protected since it becomes part of the SQL when the path is extracted
   * from the JSON values. Therefore, the limitations on the path are quite strict even if this will
   * not allow some corner case names to be used that would be valid JSON member names.
   */
  private static final String PATH_PATTERN = "^[-_a-zA-Z0-9]{1,32}(?:\\.[-_a-zA-Z0-9]{1,32}){0,5}$";

  static String normalisePath(String path) {
    if (path == null) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E7650, "(null)"));
    }
    String normalised = path.replaceAll("\\[(\\d+)]", ".$1");
    if (!".".equals(path) && !normalised.matches(PATH_PATTERN)) {
      throw new IllegalQueryException(new ErrorMessage(ErrorCode.E7650, path));
    }
    return normalised;
  }
}
