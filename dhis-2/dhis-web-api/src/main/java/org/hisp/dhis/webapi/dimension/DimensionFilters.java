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
package org.hisp.dhis.webapi.dimension;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
            .collect(Collectors.toList());

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
            "name", DimensionResponse::getName,
            "dimensionType", DimensionResponse::getDimensionType,
            "displayName", DimensionResponse::getDisplayName,
            "displayShortName", DimensionResponse::getDisplayShortName);

    private static final Map<String, BiFunction<String, String, Boolean>> OPERATOR_MAP =
        new HashMap<>();

    static {
      putOperator("startsWith", String::startsWith, true);
      putOperator("endsWith", String::endsWith, true);
      putOperator("eq", String::equals);
      putOperator("ieq", String::equalsIgnoreCase);
      putOperator("ne", (fv, v) -> !fv.equals(v));
      putOperator("like", String::contains, true);
      putOperator("ilike", (fv, v) -> fv.toLowerCase().contains(v.toLowerCase()), true);
    }

    private static void putOperator(String operator, BiFunction<String, String, Boolean> function) {
      putOperator(operator, function, false);
    }

    private static void putOperator(
        String operator, BiFunction<String, String, Boolean> function, boolean negateAlso) {
      OPERATOR_MAP.put(operator, function);
      if (negateAlso) {
        OPERATOR_MAP.put("!" + operator, (s, s2) -> !function.apply(s, s2));
      }
    }

    private String field;

    private String operator;

    private String value;

    private static SingleFilter of(String filter) {
      StringTokenizer filterTokenizer = new StringTokenizer(filter, ":");
      if (filterTokenizer.countTokens() == 3) {
        String field = filterTokenizer.nextToken().trim();
        String operator = filterTokenizer.nextToken().trim();
        String value = filterTokenizer.nextToken().trim();

        if (FIELD_EXTRACTORS.containsKey(field) && OPERATOR_MAP.containsKey(operator)) {
          return new SingleFilter(field, operator, value);
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
          .map(this::applyOperator)
          .orElse(false);
    }

    private boolean applyOperator(String fieldValue) {
      return Optional.ofNullable(OPERATOR_MAP.get(operator))
          .map(operation -> operation.apply(fieldValue, value))
          .orElse(false);
    }
  }
}
