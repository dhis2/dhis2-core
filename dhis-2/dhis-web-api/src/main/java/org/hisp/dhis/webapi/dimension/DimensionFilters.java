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
package org.hisp.dhis.webapi.dimension;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DimensionFilters implements Predicate<DimensionResponse> {

  public static final DimensionFilters EMPTY_DATA_DIMENSION_FILTER =
      new DimensionFilters() {
        @Override
        public boolean test(DimensionResponse dimensionResponse) {
          return true;
        }
      };

  private Collection<SingleFilter> filters;

  public static DimensionFilters of(Collection<String> filterStrings) {
    if (Objects.isNull(filterStrings) || filterStrings.isEmpty()) {
      return EMPTY_DATA_DIMENSION_FILTER;
    }
    List<SingleFilter> filters =
        filterStrings.stream()
            .map(String::trim)
            .map(SingleFilter::of)
            .filter(Objects::nonNull)
            .toList();

    if (filters.isEmpty()) {
      return EMPTY_DATA_DIMENSION_FILTER;
    }
    return new DimensionFilters(filters);
  }

  @Override
  public boolean test(DimensionResponse dimensionResponse) {
    return filters.stream().allMatch(filter -> filter.test(dimensionResponse));
  }

  @Getter
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  private static class SingleFilter implements Predicate<DimensionResponse> {
    private static final Map<String, Function<DimensionResponse, ?>> FIELD_EXTRACTORS =
        Map.of(
            "id", DimensionResponse::getId,
            "uid", DimensionResponse::getUid,
            "code", DimensionResponse::getCode,
            "valueType", DimensionResponse::getValueType,
            "aggregationType", DimensionResponse::getAggregationType,
            "name", DimensionResponse::getName,
            "dimensionType", DimensionResponse::getDimensionType,
            "displayName", DimensionResponse::getDisplayName,
            "displayShortName", DimensionResponse::getDisplayShortName);

    private static final Map<String, Function<String, Predicate<String>>> OPERATOR_MAP =
        new HashMap<>();

    static {
      putOperator("startsWith", String::startsWith, true);
      putOperator("endsWith", String::endsWith, true);
      putOperator("eq", String::equals, true);
      putOperator("ieq", String::equalsIgnoreCase);
      putOperator("ne", (fv, v) -> !fv.equals(v));
      putOperator("like", String::contains, true);
      putOperator("ilike", (fv, v) -> fv.toLowerCase().contains(v.toLowerCase()), true);
      OPERATOR_MAP.put("in", SingleFilter::compileInPredicate);
    }

    private static void putOperator(String operator, BiPredicate<String, String> function) {
      putOperator(operator, function, false);
    }

    private static void putOperator(
        String operator, BiPredicate<String, String> function, boolean negateAlso) {
      OPERATOR_MAP.put(operator, value -> fieldValue -> function.test(fieldValue, value));
      if (negateAlso) {
        OPERATOR_MAP.put("!" + operator, value -> fieldValue -> !function.test(fieldValue, value));
      }
    }

    private String field;

    private Predicate<String> valuePredicate;

    private static SingleFilter of(String filter) {
      String[] filterParts = filter.split(":", 3);
      if (filterParts.length == 3) {
        String field = filterParts[0].trim();
        String operator = filterParts[1].trim();
        String value = filterParts[2].trim();

        if (!FIELD_EXTRACTORS.containsKey(field) || !OPERATOR_MAP.containsKey(operator)) {
          return null;
        }

        Predicate<String> valuePredicate = compileValuePredicate(operator, value);

        if (Objects.nonNull(valuePredicate)) {
          return new SingleFilter(field, valuePredicate);
        }
        return null;
      }
      return null;
    }

    @Override
    public boolean test(DimensionResponse dimension) {
      return Optional.ofNullable(FIELD_EXTRACTORS.get(field))
          .map(
              baseDimensionalItemObjectFunction ->
                  baseDimensionalItemObjectFunction.apply(dimension))
          .map(Object::toString)
          .map(valuePredicate::test)
          .orElse(false);
    }

    private static Predicate<String> compileValuePredicate(String operator, String value) {
      if (value.isEmpty()) {
        return null;
      }
      return Optional.ofNullable(OPERATOR_MAP.get(operator))
          .map(factory -> factory.apply(value))
          .orElse(null);
    }

    private static Predicate<String> compileInPredicate(String value) {
      Set<String> values = parseInValues(value);
      return values.isEmpty() ? null : values::contains;
    }

    private static Set<String> parseInValues(String value) {
      String trimmedValue = value.trim();
      if (!trimmedValue.startsWith("[") || !trimmedValue.endsWith("]")) {
        return Set.of();
      }
      String listValue = StringUtils.substringBetween(trimmedValue, "[", "]");
      String[] values = StringUtils.split(listValue, ",");
      if (Objects.isNull(values)) {
        return Set.of();
      }
      return Arrays.stream(values)
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .collect(Collectors.toSet());
    }
  }
}
