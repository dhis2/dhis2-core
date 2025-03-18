/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.toAnalyticsFallbackDate;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.Getter;
import org.hisp.dhis.common.ValueType;

/**
 * This enum helps with the mapping between the existing value types defined in {@link ValueType}
 * and the database types.
 *
 * <p>It represents the database types associated with all possible {@link ValueType}, and provides
 * a function where it can be converted into Java types.
 */
public enum ValueTypeMapping {
  NUMERIC(BigInteger::new, Integer.class),
  DECIMAL(BigDecimal::new, Double.class),
  STRING(s -> s),
  TEXT(s -> s),
  DATE(ValueTypeMapping::dateConverter, LocalDate.class, LocalDateTime.class),
  TIME(s -> s, ValueType.TIME, s -> s.replace(".", ":"), "varchar"),
  BOOLEAN(
      ValueTypeMapping::booleanConverter, ValueTypeMapping::booleanJsonExtractor, Boolean.class);

  private static final UnaryOperator<String> BOOLEAN_JSON_EXTRACTOR =
      value -> value.equalsIgnoreCase("true") ? "1" : "0";

  private final Function<String, Object> converter;
  @Getter private final UnaryOperator<String> selectTransformer;
  private final ValueType[] valueTypes;
  @Getter private final UnaryOperator<String> argumentTransformer;
  @Getter private final String postgresCast;

  ValueTypeMapping(Function<String, Object> converter, Class<?>... classes) {
    this.converter = converter;
    this.valueTypes = fromClasses(classes);
    this.selectTransformer = UnaryOperator.identity();
    this.argumentTransformer = UnaryOperator.identity();
    this.postgresCast = name();
  }

  /**
   * Converts the given {@link Class} array into an array of {@link ValueType} based on the
   * supported classes.
   *
   * @param classes the classes to be converted
   * @return the respective {@link ValueType} array
   */
  private ValueType[] fromClasses(Class<?>... classes) {
    return stream(ValueType.values())
        .filter(valueType -> isAssignableFrom(classes, valueType))
        .toArray(ValueType[]::new);
  }

  /**
   * Checks if the given {@link ValueType} is assignable from the given classes.
   *
   * @param classes the classes to be checked
   * @param valueType the {@link ValueType} to be checked
   * @return true if the {@link ValueType} is assignable from the given classes, false otherwise
   */
  private static boolean isAssignableFrom(Class<?>[] classes, ValueType valueType) {
    return stream(classes).anyMatch(valueType.getJavaClass()::isAssignableFrom);
  }

  ValueTypeMapping(
      Function<String, Object> converter,
      UnaryOperator<String> selectTransformer,
      Class<?>... classes) {
    this.converter = converter;
    this.valueTypes = fromClasses(classes);
    this.selectTransformer = s -> Objects.isNull(s) ? null : selectTransformer.apply(s);
    this.argumentTransformer = UnaryOperator.identity();
    this.postgresCast = name();
  }

  ValueTypeMapping(
      Function<String, Object> converter,
      ValueType valueType,
      UnaryOperator<String> argumentTransformer,
      String postgresCast) {
    this.converter = converter;
    this.valueTypes = new ValueType[] {valueType};
    this.selectTransformer = UnaryOperator.identity();
    this.argumentTransformer = s -> Objects.nonNull(s) ? argumentTransformer.apply(s) : null;
    this.postgresCast = postgresCast;
  }

  private static Date dateConverter(String dateAsString) {
    try {
      return toMediumDate(dateAsString);
    } catch (Exception ignore) {
      return toAnalyticsFallbackDate(dateAsString);
    }
  }

  private static Object booleanConverter(String parameterInput) {
    return Optional.ofNullable(parameterInput).map(ValueTypeMapping::isTrue).orElse(false);
  }

  private static boolean isTrue(String value) {
    return "1".equals(value) || "true".equalsIgnoreCase(value);
  }

  private static String booleanJsonExtractor(String value) {
    return BOOLEAN_JSON_EXTRACTOR.apply(value);
  }

  /**
   * Finds the associated {@link ValueTypeMapping} for the given {@link ValueType}.
   *
   * @param valueType the {@link ValueType}.
   * @return the respective ValueTypeMapping, or default to {@link ValueTypeMapping.TEXT}.
   */
  public static ValueTypeMapping fromValueType(ValueType valueType) {
    return stream(values())
        .filter(valueTypeMapping -> valueTypeMapping.supports(valueType))
        .findFirst()
        .orElse(TEXT);
  }

  private boolean supports(ValueType valueType) {
    return stream(valueTypes).anyMatch(vt -> vt == valueType);
  }

  /**
   * Converts the "value" into a Java representation, based on the internal converter function.
   *
   * @param value the value to be converted
   * @return the respective Java object
   */
  public Object convertSingle(String value) {
    return converter.apply(value);
  }

  /**
   * Converts all "values" into a Java representation, based on the internal converter function.
   *
   * @param values the {@link List} of values to be converted
   * @return the respective Java object
   */
  public List<Object> convertMany(List<String> values) {
    return values.stream().map(converter).collect(toList());
  }
}
