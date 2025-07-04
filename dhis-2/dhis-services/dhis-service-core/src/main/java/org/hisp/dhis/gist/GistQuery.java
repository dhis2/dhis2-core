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
package org.hisp.dhis.gist;

import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.schema.annotation.Gist.Transform;

/**
 * Description of the gist query that should be run.
 *
 * <p>There are two essential types of queries:
 *
 * <ul>
 *   <li>owner property list query ({@link #owner} is non-null)
 *   <li>direct list query ({@link #owner} is null)
 * </ul>
 *
 * @author Jan Bernitt
 */
@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class GistQuery {
  /**
   * Fields allow {@code property[sub,sub]} syntax where a comma occurs as part of the property
   * name. These commas need to be ignored when splitting a {@code fields} parameter list.
   */
  static final String FIELD_SPLIT = ",(?![^\\[\\]]*\\]|[^\\(\\)]*\\)|([a-zA-Z0-9]+,?)+\\))";

  /** Query properties about the owner of the collection property. */
  @Getter
  @Builder
  @AllArgsConstructor
  public static final class Owner {

    /** The object type that has the collection */
    private final Class<? extends PrimaryKeyObject> type;

    /** Id of the collection owner object. */
    private final String id;

    /** Name of the collection property in the {@link #type}. */
    private final String collectionProperty;

    @Override
    public String toString() {
      return type.getSimpleName() + "[" + id + "]." + collectionProperty;
    }
  }

  private final Owner owner;

  private final Class<? extends PrimaryKeyObject> elementType;

  private final boolean paging;

  @JsonProperty private final int pageOffset;

  @JsonProperty private final int pageSize;

  /** Include total match count in pager? Default false. */
  @JsonProperty private final boolean total;

  private final String contextRoot;
  private final String requestURL;

  private final Locale translationLocale;

  /** Not the elements contained in the collection but those not contained (yet). Default false. */
  @JsonProperty private final boolean inverse;

  /** Apply translations to translatable properties? Default true. */
  @JsonProperty private final boolean translate;

  /**
   * Use absolute URLs when referring to other APIs in pager and {@code apiEndpoints}? Default
   * false.
   */
  @JsonProperty private final boolean absoluteUrls;

  /** Return plain result list (without pager wrapper). Default false. */
  @JsonProperty private final boolean headless;

  /**
   * Use OR instead of AND between filters so that any match for one of the filters is a match.
   * Default false.
   */
  @JsonProperty private final boolean anyFilter;

  /**
   * Mode where no actual query is performed - instead the output and query is described similar to
   * "SQL describe".
   */
  private final boolean describe;

  /** Weather or not to include the API endpoints references */
  @JsonProperty private final boolean references;

  private final boolean typedAttributeValues;

  /** The extend to which fields are included by default */
  @JsonProperty(value = "auto")
  private final GistAutoType autoType;

  /** Names of those properties that should be included in the response. */
  @JsonProperty @Builder.Default private final List<Field> fields = emptyList();

  /**
   * List of filter property expressions. An expression has the format {@code
   * property:operator:value} or {@code property:operator}.
   */
  @JsonProperty @Builder.Default private final List<Filter> filters = emptyList();

  @JsonProperty @Builder.Default private final List<Order> orders = emptyList();

  public List<String> getFieldNames() {
    return fields.stream().map(Field::getName).collect(toList());
  }

  public Transform getDefaultTransformation() {
    return getAutoType() == null ? Transform.AUTO : getAutoType().getDefaultTransformation();
  }

  public String getEndpointRoot() {
    return isAbsoluteUrls() ? getContextRoot() + "/api" : "/api";
  }

  public boolean hasFilterGroups() {
    return filters.size() > 1 && filters.stream().anyMatch(f -> f.getGroup() >= 0);
  }

  public GistQuery with(GistParams params) throws BadRequestException {
    int page = abs(params.getPage());
    int size = Math.min(1000, abs(params.getPageSize()));
    boolean tree = params.isOrgUnitsTree();
    boolean offline = params.isOrgUnitsOffline();
    String order = tree ? "path" : params.getOrder();
    if (offline && (order == null || order.isEmpty())) order = "level,name";
    String fields = offline ? "path,displayName,children::isNotEmpty" : params.getFields();
    // ensure tree always includes path in fields
    if (tree) {
      if (fields == null || fields.isEmpty()) {
        fields = "path";
      } else if (!(fields.contains(",path,"))
          || fields.startsWith("path,")
          || fields.endsWith(",path")) fields += ",path";
    }
    return toBuilder()
        .paging(!offline)
        .pageSize(size)
        .pageOffset(Math.max(0, page - 1) * size)
        .translate(params.isTranslate())
        .inverse(params.isInverse())
        .total(params.isCountTotalPages())
        .absoluteUrls(params.isAbsoluteUrls())
        .headless(params.isHeadless())
        .describe(params.isDescribe())
        .references(!tree && !offline && params.isReferences())
        .anyFilter(params.getRootJunction() == Junction.Type.OR)
        .fields(getStrings(fields, FIELD_SPLIT).stream().map(Field::parse).toList())
        .filters(
            getStrings(params.getFilter(), FIELD_SPLIT).stream()
                .map(Filter::parse)
                .collect(toList()))
        .orders(getStrings(order, ",").stream().map(Order::parse).toList())
        .build();
  }

  private static List<String> getStrings(String value, String splitRegex) {
    if (value == null || value.isEmpty()) {
      return emptyList();
    }
    return asList(value.split(splitRegex));
  }

  public GistQuery withOwner(Owner owner) {
    return toBuilder().owner(owner).build();
  }

  public GistQuery withFilter(Filter filter) {
    return withAddedItem(filter, getFilters(), GistQueryBuilder::filters);
  }

  public GistQuery withOrder(Order order) {
    return withAddedItem(order, getOrders(), GistQueryBuilder::orders);
  }

  public GistQuery withField(String path) {
    return withAddedItem(
        new Field(path, getDefaultTransformation()), getFields(), GistQueryBuilder::fields);
  }

  public GistQuery withFields(List<Field> fields) {
    return toBuilder().fields(fields).build();
  }

  public GistQuery withFilters(List<Filter> filters) {
    return toBuilder().filters(filters).build();
  }

  private <E> GistQuery withAddedItem(
      E e, List<E> collection, BiFunction<GistQueryBuilder, List<E>, GistQueryBuilder> setter) {
    List<E> plus1 = new ArrayList<>(collection);
    plus1.add(e);
    return setter.apply(toBuilder(), plus1).build();
  }

  public enum Direction {
    ASC,
    DESC
  }

  public enum Comparison {
    // identity/numeric comparison
    NULL("is null", "null"),
    NOT_NULL("is not null", "!null"),
    EQ("=", "eq"),
    IEQ("=", "ieq"),
    NE("!=", "!eq", "ne", "neq"),

    // numeric comparison
    LT("<", "lt"),
    LE("<=", "le", "lte"),
    GT(">", "gt"),
    GE(">=", "ge", "gte"),

    // collection operations
    IN("in", "in"),
    NOT_IN("not in", "!in"),
    EMPTY("= 0", "empty"),
    NOT_EMPTY("> 0", "!empty"),

    // string comparison
    LIKE("like", "like"),
    NOT_LIKE("not like", "!like"),
    STARTS_LIKE("like", "$like"),
    NOT_STARTS_LIKE("not like", "!$like"),
    ENDS_LIKE("like", "like$"),
    NOT_ENDS_LIKE("not like", "!like$"),
    ILIKE("like", "ilike"),
    NOT_ILIKE("not like", "!ilike"),
    STARTS_WITH("like", "$ilike", "startswith"),
    NOT_STARTS_WITH("not like", "!$ilike", "!startswith"),
    ENDS_WITH("like", "ilike$", "endswith"),
    NOT_ENDS_WITH("not like", "!ilike$", "!endswith"),

    // access checks
    CAN_READ("", "canread"),
    CAN_WRITE("", "canwrite"),
    CAN_DATA_READ("", "candataread"),
    CAN_DATA_WRITE("", "candatawrite"),
    CAN_ACCESS("", "canaccess");

    @Getter private final String sql;
    private final String[] symbols;

    Comparison(String sql, String... symbols) {
      this.sql = sql;
      this.symbols = symbols;
    }

    public static Comparison parse(String symbol) {
      String s = symbol.toLowerCase();
      for (Comparison op : values()) {
        if (asList(op.symbols).contains(s)) {
          return op;
        }
      }
      throw new IllegalArgumentException("Not an comparison operator symbol: " + symbol);
    }

    public boolean isUnary() {
      return this == NULL || this == NOT_NULL || this == EMPTY || this == NOT_EMPTY;
    }

    public boolean isMultiValue() {
      return this == IN || this == NOT_IN || this == CAN_ACCESS;
    }

    public boolean isIdentityCompare() {
      return this == NULL || this == NOT_NULL || this == EQ || this == IEQ || this == NE;
    }

    public boolean isOrderCompare() {
      return this == EQ || this == IEQ || this == NE || isNumericCompare();
    }

    public boolean isNumericCompare() {
      return this == LT || this == LE || this == GE || this == GT;
    }

    public boolean isCollectionCompare() {
      return isContainsCompare() || isEmptinessCompare();
    }

    public boolean isEmptinessCompare() {
      return this == EMPTY || this == NOT_EMPTY;
    }

    public boolean isStringCompare() {
      return ordinal() >= LIKE.ordinal() && ordinal() < CAN_READ.ordinal();
    }

    public boolean isContainsCompare() {
      return this == IN || this == NOT_IN;
    }

    public boolean isAccessCompare() {
      return ordinal() >= CAN_READ.ordinal();
    }

    public boolean isCaseInsensitive() {
      return this == IEQ || ordinal() >= ILIKE.ordinal() && ordinal() <= NOT_ENDS_WITH.ordinal();
    }
  }

  @Getter
  @Builder(toBuilder = true)
  @AllArgsConstructor
  public static final class Field {

    public static final String REFS_PATH = "__refs__";
    public static final String ALL_PATH = "*";
    public static final Field ALL = new Field(ALL_PATH, Transform.NONE);

    @JsonProperty private final String propertyPath;
    @JsonProperty private final Transform transformation;
    @JsonProperty private final String alias;
    @JsonProperty private final String transformationArgument;
    @JsonProperty private final boolean translate;
    @JsonProperty private final boolean attribute;

    public Field(String propertyPath, Transform transformation) {
      this(propertyPath, transformation, "", null, false, false);
    }

    @JsonProperty
    public String getName() {
      return alias.isEmpty() ? propertyPath : alias;
    }

    public Field withTransformation(Transform transform) {
      return toBuilder().transformation(transform).build();
    }

    public Field withPropertyPath(String path) {
      return toBuilder().propertyPath(path).build();
    }

    public Field withAlias(String alias) {
      return toBuilder().alias(alias).build();
    }

    public Field withTranslate() {
      return toBuilder().translate(true).build();
    }

    public Field asAttribute() {
      return toBuilder().attribute(true).build();
    }

    @Override
    public String toString() {
      return transformation == Transform.NONE
          ? propertyPath
          : propertyPath + "::" + transformation.name().toLowerCase().replace('_', '-');
    }

    public static Field parse(String field) {
      String[] parts = field.split("(?:::|~|@)(?![^\\[\\]]*\\])");
      if (parts.length == 1) {
        return new Field(field, Transform.AUTO);
      }
      Transform transform = Transform.AUTO;
      String alias = "";
      String arg = null;
      for (int i = 1; i < parts.length; i++) {
        String part = parts[i];
        if (part.startsWith("rename")) {
          alias = parseArgument(part);
        } else {
          transform = Transform.parse(part);
          if (part.indexOf('(') >= 0) {
            arg = parseArgument(part);
          }
        }
      }
      return new Field(parts[0], transform, alias, arg, false, false);
    }

    private static String parseArgument(String part) {
      return part.substring(part.indexOf('(') + 1, part.lastIndexOf(')'));
    }
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static final class Order {
    @JsonProperty private final String propertyPath;

    @JsonProperty @Builder.Default private final Direction direction = Direction.ASC;

    public static Order parse(String order) {
      String[] parts = order.split("(?:::|:|~|@)");
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
      return propertyPath + " " + direction.name();
    }
  }

  @Getter
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Filter {

    @JsonProperty private final int group;
    @JsonProperty private final String propertyPath;
    @JsonProperty private final Comparison operator;
    @JsonProperty private final String[] value;
    @JsonProperty private final boolean attribute;
    @JsonProperty private final boolean subSelect;

    public Filter(String propertyPath, Comparison operator, String... value) {
      this(0, propertyPath, operator, value, false, false);
    }

    public Filter withPropertyPath(String path) {
      return new Filter(path, operator, value);
    }

    public Filter withValue(String... value) {
      return new Filter(propertyPath, operator, value);
    }

    /**
     * @return A new filter modified to be considered an attribute value match for the UID given by
     *     the path
     */
    public Filter asAttribute() {
      return new Filter(group, propertyPath, operator, value, true, subSelect);
    }

    /**
     * @return a new filter modified to use a sub-select query to match
     */
    public Filter asSubSelect() {
      return new Filter(group, propertyPath, operator, value, attribute, true);
    }

    public Filter inGroup(int group) {
      return group == this.group
          ? this
          : new Filter(group, propertyPath, operator, value, attribute, subSelect);
    }

    /**
     * Expression syntax is (with : being any of the allowed delimiters)
     *
     * <pre>
     *   [group : ] name : op [ : value ]
     * </pre>
     */
    private static final Pattern FILTER_SPLIT =
        Pattern.compile("(?:(\\d+)[:~@]{1,2})?([^:~@]+)[:~@]{1,2}([^:~@]+)(?:[:~@]{1,2}(.*))?");

    public static Filter parse(String filter) {
      Matcher m = FILTER_SPLIT.matcher(filter);
      if (!m.matches())
        throw new IllegalArgumentException("Not a valid filter expression: " + filter);
      String group = m.group(1);
      if (group == null || group.isEmpty()) group = "-1";
      String name = m.group(2);
      String op = m.group(3);
      String value = m.group(4);
      if (value == null || value.isEmpty())
        return new Filter(name, Comparison.parse(op)).inGroup(parseInt(group));
      if (!value.startsWith("[") || !value.endsWith("]"))
        return new Filter(name, Comparison.parse(op), value).inGroup(parseInt(group));
      String[] values = value.substring(1, value.length() - 1).split(",");
      return new Filter(name, Comparison.parse(op), values).inGroup(parseInt(group));
    }

    @Override
    public String toString() {
      return propertyPath + ":" + operator.symbols[0] + ":" + Arrays.toString(value);
    }
  }
}
