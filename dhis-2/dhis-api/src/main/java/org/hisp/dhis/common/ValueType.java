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
package org.hisp.dhis.common;

import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.opengis.geometry.primitive.Point;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement(localName = "valueType", namespace = DxfNamespaces.DXF_2_0)
public enum ValueType {
  TEXT(String.class, true),
  LONG_TEXT(String.class, true),
  MULTI_TEXT(String.class, true),
  LETTER(String.class, true),
  PHONE_NUMBER(String.class, true),
  EMAIL(String.class, true),
  BOOLEAN(Boolean.class, true),
  TRUE_ONLY(Boolean.class, true),
  DATE(LocalDate.class, false),
  DATETIME(LocalDateTime.class, false),
  TIME(String.class, false),
  NUMBER(Double.class, true),
  UNIT_INTERVAL(Double.class, true),
  PERCENTAGE(Double.class, true),
  INTEGER(Integer.class, true),
  INTEGER_POSITIVE(Integer.class, true),
  INTEGER_NEGATIVE(Integer.class, true),
  INTEGER_ZERO_OR_POSITIVE(Integer.class, true),
  TRACKER_ASSOCIATE(TrackedEntity.class, false),
  USERNAME(String.class, true),
  COORDINATE(Point.class, true),
  ORGANISATION_UNIT(OrganisationUnit.class, false),
  REFERENCE(Reference.class, false),
  AGE(Date.class, false),
  URL(String.class, true),
  FILE_RESOURCE(String.class, true, FileTypeValueOptions.class),
  IMAGE(String.class, false, FileTypeValueOptions.class),
  GEOJSON(String.class, false);

  /** The character used to separate values in a multi-text value. */
  public static final String MULTI_TEXT_SEPARATOR = ",";

  public static List<String> splitMultiText(String value) {
    return value == null ? List.of() : List.of(value.split(MULTI_TEXT_SEPARATOR));
  }

  private static final Set<ValueType> INTEGER_TYPES =
      Set.of(INTEGER, INTEGER_POSITIVE, INTEGER_NEGATIVE, INTEGER_ZERO_OR_POSITIVE);

  private static final Set<ValueType> DECIMAL_TYPES = Set.of(NUMBER, UNIT_INTERVAL, PERCENTAGE);

  private static final Set<ValueType> BOOLEAN_TYPES = Set.of(BOOLEAN, TRUE_ONLY);

  public static final Set<ValueType> TEXT_TYPES =
      Set.of(TEXT, LONG_TEXT, LETTER, TIME, USERNAME, EMAIL, PHONE_NUMBER, URL);

  public static final Set<ValueType> DATE_TYPES = Set.of(DATE, DATETIME, AGE);

  private static final Set<ValueType> FILE_TYPES = Set.of(FILE_RESOURCE, IMAGE);

  private static final Set<ValueType> GEO_TYPES = Set.of(COORDINATE, GEOJSON);

  private static final Set<ValueType> JSON_TYPES = Set.of(GEOJSON);

  public static final Set<ValueType> NUMERIC_TYPES =
      Stream.concat(INTEGER_TYPES.stream(), DECIMAL_TYPES.stream())
          .collect(Collectors.toUnmodifiableSet());

  /**
   * Maps the Java classes to their corresponding PostgreSQL <a
   * href="https://www.postgresql.org/docs/current/datatype.html">data types</a>. Unfortunately,
   * there is no public utility provided to us by the PostgreSQL JDBC driver. There are some
   * locations in the driver code that we can refer to though such as <a
   * href="https://github.com/pgjdbc/pgjdbc/blob/156d724e1d95052b41a19fb568b2f81919ae2197/pgjdbc/src/main/java/org/postgresql/jdbc/TypeInfoCache.java#L84">TypeInfoCache</a>.
   *
   * <p>Casting DB values or converting user input values to types other than {@code String}/{@code
   * text} allows users to filter/compare using the semantic of a value type like numeric instead of
   * only text. So far only numeric data values are cast/converted to one of <a
   * href="https://www.postgresql.org/docs/current/datatype-numeric.html">PostgreSQL Numeric</a> in
   * Tracker.
   */
  public static final Map<Class<?>, SqlType<?>> JAVA_TO_SQL_TYPES =
      Map.of(
          Integer.class, new SqlType<>(Types.INTEGER, "integer", Integer.class, Integer::valueOf),
          Double.class,
              new SqlType<>(
                  Types.NUMERIC, "numeric", java.math.BigDecimal.class, java.math.BigDecimal::new),
          String.class, new SqlType<>(Types.VARCHAR, "text", String.class, Function.identity()));

  /**
   * @param type java.sql.Types
   * @param postgresName name of the postgres data type representing the java.sql.Types
   * @param postgresClass java class used by the postgres jdbc driver to represent such a
   *     java.sql.Type
   * @param producer converts a string value of a value type into a value of the postgres class
   */
  public record SqlType<T>(
      int type,
      String postgresName,
      Class<T> postgresClass,
      java.util.function.Function<String, T> producer) {}

  @Deprecated private final Class<?> javaClass;

  private boolean aggregatable;

  private Class<? extends ValueTypeOptions> valueTypeOptionsClass;

  ValueType() {
    this.javaClass = null;
  }

  ValueType(Class<?> javaClass, boolean aggregateable) {
    this.javaClass = javaClass;
    this.aggregatable = aggregateable;
    this.valueTypeOptionsClass = null;
  }

  ValueType(
      Class<?> javaClass,
      boolean aggregatable,
      Class<? extends ValueTypeOptions> valueTypeOptionsClass) {
    this(javaClass, aggregatable);
    this.valueTypeOptionsClass = valueTypeOptionsClass;
  }

  public Class<?> getJavaClass() {
    return javaClass;
  }

  public boolean isInteger() {
    return INTEGER_TYPES.contains(this);
  }

  public boolean isDecimal() {
    return DECIMAL_TYPES.contains(this);
  }

  public boolean isBoolean() {
    return BOOLEAN_TYPES.contains(this);
  }

  public boolean isText() {
    return TEXT_TYPES.contains(this);
  }

  public boolean isDate() {
    return DATE_TYPES.contains(this);
  }

  public boolean isFile() {
    return FILE_TYPES.contains(this);
  }

  public boolean isGeo() {
    return GEO_TYPES.contains(this);
  }

  public boolean isOrganisationUnit() {
    return ORGANISATION_UNIT == this;
  }

  public boolean isReference() {
    return REFERENCE == this;
  }

  /** Includes integer and decimal types. */
  public boolean isNumeric() {
    return NUMERIC_TYPES.contains(this);
  }

  public boolean isJson() {
    return JSON_TYPES.contains(this);
  }

  public boolean isAggregatable() {
    return aggregatable;
  }

  public boolean isAggregatable(AggregationType aggregationType) {
    if (!aggregationType.isAggregatable() || !isAggregatable()) {
      return false;
    }

    if (this == FILE_RESOURCE && aggregationType != AggregationType.COUNT) {
      return false;
    }

    if (TEXT_TYPES.contains(this)) {
      return true;
    }

    return aggregationType != AggregationType.NONE;
  }

  public boolean isMultiText() {
    return MULTI_TEXT == this;
  }

  public Class<? extends ValueTypeOptions> getValueTypeOptionsClass() {
    return this.valueTypeOptionsClass;
  }

  /**
   * Returns a simplified value type. As an example, if the value type is any numeric type such as
   * integer, percentage, then {@link ValueType#NUMBER} is returned. Can return any of:
   *
   * <ul>
   *   <li>{@link ValueType#NUMBER} for any numeric types.
   *   <li>{@link ValueType#BOOLEAN} for any boolean types.
   *   <li>{@link ValueType#DATE} for any date / time types.
   *   <li>{@link ValueType#FILE_RESOURCE} for any file types.
   *   <li>{@link ValueType#COORDINATE} for any geometry types.
   *   <li>{@link ValueType#TEXT} for any textual types.
   *   <li>{@link ValueType#MULTI_TEXT} if it is that type.
   * </ul>
   *
   * @return a simplified value type.
   */
  public ValueType toSimplifiedValueType() {
    if (isNumeric()) {
      return ValueType.NUMBER;
    }
    if (isBoolean()) {
      return ValueType.BOOLEAN;
    }
    if (isDate()) {
      return ValueType.DATE;
    }
    if (isFile()) {
      return ValueType.FILE_RESOURCE;
    }
    if (isGeo()) {
      return ValueType.COORDINATE;
    }
    if (this == MULTI_TEXT) {
      return this;
    }
    return ValueType.TEXT;
  }

  /**
   * Get the SQL type the data value's Java class maps to. Defaults to the {@code String.class}
   * representation.
   */
  @Nonnull
  public SqlType<?> getSqlType() {
    SqlType<?> sqlType = JAVA_TO_SQL_TYPES.get(javaClass);
    if (sqlType == null) {
      sqlType = JAVA_TO_SQL_TYPES.get(String.class);
    }
    return sqlType;
  }

  /**
   * Returns a valid ValueType based on the given SQL type.
   *
   * @see java.sql.Types for valid integer values.
   * @param type of the java.sql.Types
   * @return the respective ValueType
   */
  public static ValueType getValueTypeFromSqlType(int type) {
    return switch (type) {
      case Types.INTEGER -> ValueType.INTEGER;
      case Types.DECIMAL, Types.DOUBLE, Types.NUMERIC, Types.BIGINT, Types.FLOAT ->
          ValueType.NUMBER;
      case Types.BOOLEAN -> ValueType.BOOLEAN;
      case Types.DATE -> ValueType.DATE;
      case Types.TIMESTAMP_WITH_TIMEZONE -> ValueType.DATETIME;
      case Types.TIME, Types.TIME_WITH_TIMEZONE -> ValueType.TIME;
      default -> ValueType.TEXT;
    };
  }

  public static ValueType fromString(String valueType) throws IllegalArgumentException {
    return Arrays.stream(ValueType.values())
        .filter(v -> v.toString().equals(valueType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown value: " + valueType));
  }

  public static Set<ValueType> getAggregatables() {
    return Arrays.stream(ValueType.values())
        .filter(v -> Arrays.stream(AggregationType.values()).anyMatch(v::isAggregatable))
        .collect(toSet());
  }
}
