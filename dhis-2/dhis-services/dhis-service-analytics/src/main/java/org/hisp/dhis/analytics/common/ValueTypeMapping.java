/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.common.ValueType.DATETIME;
import static org.hisp.dhis.common.ValueType.INTEGER;
import static org.hisp.dhis.common.ValueType.INTEGER_NEGATIVE;
import static org.hisp.dhis.common.ValueType.INTEGER_POSITIVE;
import static org.hisp.dhis.common.ValueType.INTEGER_ZERO_OR_POSITIVE;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TIME;
import static org.hisp.dhis.common.ValueType.TRUE_ONLY;
import static org.hisp.dhis.util.DateUtils.getMediumDate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
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
  NUMERIC(BigInteger::new, INTEGER, INTEGER_NEGATIVE, INTEGER_POSITIVE, INTEGER_ZERO_OR_POSITIVE),
  DECIMAL(BigDecimal::new, NUMBER),
  STRING(s -> s),
  TEXT(s -> s),
  DATE(ValueTypeMapping::dateConverter, ValueType.DATE, DATETIME, TIME),
  BOOLEAN(
      ValueTypeMapping::booleanConverter,
      ValueTypeMapping::booleanSelectTransformer,
      ValueType.BOOLEAN,
      TRUE_ONLY);

  private static final UnaryOperator<String> BOOLEAN_SELECT_TRANSFORMER =
      columnName ->
          "case when "
              + columnName
              + " = 'true' then 1 when "
              + columnName
              + " = 'false' then 0 end";
  private final Function<String, Object> converter;
  @Getter private final UnaryOperator<String> selectTransformer;
  private final ValueType[] valueTypes;

  ValueTypeMapping(Function<String, Object> converter, ValueType... valueTypes) {
    this.converter = converter;
    this.valueTypes = valueTypes;
    this.selectTransformer = UnaryOperator.identity();
  }

  ValueTypeMapping(
      Function<String, Object> converter,
      UnaryOperator<String> selectTransformer,
      ValueType... valueTypes) {
    this.converter = converter;
    this.valueTypes = valueTypes;
    this.selectTransformer = selectTransformer;
  }

  private static Date dateConverter(String dateAsString) {
    return getMediumDate(dateAsString);
  }

  private static Object booleanConverter(String parameterInput) {
    return Optional.ofNullable(parameterInput).map(ValueTypeMapping::isTrue).orElse(false);
  }

  private static boolean isTrue(String value) {
    return "1".equals(value) || "true".equalsIgnoreCase(value);
  }

  private static String booleanSelectTransformer(String columnName) {
    return BOOLEAN_SELECT_TRANSFORMER.apply(columnName);
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
