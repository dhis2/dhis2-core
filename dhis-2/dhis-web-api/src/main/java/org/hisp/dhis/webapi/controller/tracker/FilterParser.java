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
package org.hisp.dhis.webapi.controller.tracker;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;

/** Parser for the filter request parameter. */
public class FilterParser {
  private FilterParser() {
    throw new IllegalStateException("Utility class");
  }

  private static final String INVALID_FILTER = "Query item or filter is invalid: ";

  private static final char COMMA_SEPARATOR = ',';

  private static final char ESCAPE = '/';

  /**
   * Negative lookahead to avoid wrong split of comma-separated list of filters when one or more
   * filter value contain comma. It skips comma escaped by slash
   */
  private static final Pattern FILTER_LIST_SPLIT =
      Pattern.compile("(?<!" + ESCAPE + ")" + COMMA_SEPARATOR);

  /**
   * Negative lookahead to avoid wrong split when filter value contains colon. It skips colon
   * escaped by slash
   */
  public static final Pattern FILTER_ITEM_SPLIT =
      Pattern.compile("(?<!" + ESCAPE + ")" + DIMENSION_NAME_SEP);

  private static final String COMMA_STRING = Character.toString(COMMA_SEPARATOR);

  private static final String ESCAPE_COMMA = ESCAPE + COMMA_STRING;

  private static final String ESCAPE_COLON = ESCAPE + DIMENSION_NAME_SEP;

  /**
   * Parse given {@code input} string representing a filter for an object referenced by a UID like a
   * tracked entity attribute. Refer to {@link #parseSanitizedFilters(Map, String)}} for details on
   * the expected input format.
   *
   * @return filters by UIDs
   */
  public static Map<UID, List<QueryFilter>> parseFilters(String input) throws BadRequestException {
    Map<UID, List<QueryFilter>> result = new HashMap<>();
    if (StringUtils.isBlank(input)) {
      return result;
    }

    for (String uidOperatorValue : filterList(input)) {
      parseSanitizedFilters(result, uidOperatorValue);
    }
    return result;
  }

  /**
   * Accumulate {@link QueryFilter}s per TEA UID by parsing given input string of format
   * {uid}[:{operator}:{value}]. Only the TEA UID is mandatory. Multiple operator:value pairs are
   * allowed. A {@link QueryFilter} for each operator:value pair is added to the corresponding TEA
   * UID.
   *
   * @throws BadRequestException filter is neither multiple nor single operator:value format
   */
  private static void parseSanitizedFilters(Map<UID, List<QueryFilter>> result, String input)
      throws BadRequestException {
    int uidIndex = input.indexOf(DIMENSION_NAME_SEP) + 1;

    if (uidIndex == 0 || input.length() == uidIndex) {
      UID uid = UID.of(input.replace(DIMENSION_NAME_SEP, ""));
      result.putIfAbsent(uid, new ArrayList<>());
      return;
    }

    UID uid = UID.of(input.substring(0, uidIndex - 1));
    result.putIfAbsent(uid, new ArrayList<>());

    String[] filters = FILTER_ITEM_SPLIT.split(input.substring(uidIndex));
    validateFilterLength(filters, result, uid, input);
  }

  private static void validateFilterLength(
      String[] filters, Map<UID, List<QueryFilter>> result, UID uid, String input)
      throws BadRequestException {
    switch (filters.length) {
      case 1 -> addQueryFilter(result, uid, filters[0], null, input);
      case 2 -> handleOperators(filters, result, uid, input);
      case 3 -> handleMixedOperators(filters, result, uid, input);
      case 4 -> handleMultipleBinaryOperators(filters, result, uid, input);
      default -> throw new BadRequestException(INVALID_FILTER + input);
    }
  }

  private static void addQueryFilter(
      Map<UID, List<QueryFilter>> result, UID uid, String operator, String value, String input)
      throws BadRequestException {
    result.get(uid).add(operatorValueQueryFilter(operator, value, input));
  }

  private static void handleOperators(
      String[] filters, Map<UID, List<QueryFilter>> result, UID uid, String input)
      throws BadRequestException {
    QueryOperator firstOperator =
        findQueryOperatorFromFilter(filters[0])
            .orElseThrow(
                () ->
                    new BadRequestException(
                        String.format("'%s' is not a valid operator: %s", filters[0], input)));

    if (!firstOperator.isUnary()) {
      addQueryFilter(result, uid, filters[0], filters[1], input);
      return;
    }

    QueryOperator secondOperator =
        findQueryOperatorFromFilter(filters[1])
            .orElseThrow(
                () ->
                    new BadRequestException(
                        String.format(
                            "Operator '%s' in filter can't be used with a value: %s",
                            filters[0], input)));

    if (secondOperator.isUnary()) {
      addQueryFilter(result, uid, filters[0], null, input);
      addQueryFilter(result, uid, filters[1], null, input);
    }
  }

  private static void handleMixedOperators(
      String[] filters, Map<UID, List<QueryFilter>> result, UID uid, String input)
      throws BadRequestException {
    Optional<QueryOperator> firstOperator = findQueryOperatorFromFilter(filters[0]);
    if (firstOperator.map(QueryOperator::isUnary).orElse(false)) {
      addQueryFilter(result, uid, filters[0], null, input);
      addQueryFilter(result, uid, filters[1], filters[2], input);
      return;
    }

    Optional<QueryOperator> thirdOperator = findQueryOperatorFromFilter(filters[2]);
    if (thirdOperator.map(QueryOperator::isUnary).orElse(false)) {
      addQueryFilter(result, uid, filters[0], filters[1], input);
      addQueryFilter(result, uid, filters[2], null, input);
      return;
    }

    throw new BadRequestException(INVALID_FILTER + input);
  }

  private static void handleMultipleBinaryOperators(
      String[] filters, Map<UID, List<QueryFilter>> result, UID uid, String input)
      throws BadRequestException {

    List<String> unaryOperators = getUnaryOperatorsInFilter(filters);
    switch (unaryOperators.size()) {
      case 0 -> {
        for (int i = 0; i < filters.length; i += 2) {
          addQueryFilter(result, uid, filters[i], filters[i + 1], input);
        }
      }
      case 1 ->
          throw new BadRequestException(
              String.format(
                  "Operator '%s' in filter can't be used with a value: %s",
                  unaryOperators.get(0), input));
      default ->
          throw new BadRequestException(
              String.format("A maximum of two operators can be used in a filter: %s", input));
    }
  }

  private static Optional<QueryOperator> findQueryOperatorFromFilter(String filter) {
    return Arrays.stream(QueryOperator.values())
        .filter(qo -> qo.name().equalsIgnoreCase(filter.replace("!", "n")))
        .findFirst();
  }

  private static List<String> getUnaryOperatorsInFilter(String[] filters) {
    Set<String> unaryOperators =
        Arrays.stream(QueryOperator.values())
            .filter(QueryOperator::isUnary)
            .map(qo -> qo.name().toLowerCase())
            .collect(Collectors.toSet());

    return Arrays.stream(filters)
        .map(f -> f.toLowerCase().replace("!", "n"))
        .filter(unaryOperators::contains)
        .toList();
  }

  private static QueryFilter operatorValueQueryFilter(String operator, String value, String filter)
      throws BadRequestException {
    if (StringUtils.isEmpty(operator)) {
      throw new BadRequestException(INVALID_FILTER + filter);
    }

    QueryOperator queryOperator;
    try {
      queryOperator = QueryOperator.fromString(operator);
    } catch (IllegalArgumentException exception) {
      throw new BadRequestException(INVALID_FILTER + filter);
    }

    if (queryOperator == null) {
      throw new BadRequestException(INVALID_FILTER + filter);
    }

    if (queryOperator.isUnary()) {
      if (!StringUtils.isEmpty(value)) {
        throw new BadRequestException(
            String.format(
                "Operator %s in filter can't be used with a value: %s",
                queryOperator.name(), filter));
      }
      return new QueryFilter(queryOperator);
    }

    if (StringUtils.isEmpty(value)) {
      throw new BadRequestException("Operator in filter must be be used with a value: " + filter);
    }

    return new QueryFilter(queryOperator, escapedFilterValue(value));
  }

  /** Replace escaped comma or colon */
  private static String escapedFilterValue(String value) {
    return value.replace(ESCAPE_COMMA, COMMA_STRING).replace(ESCAPE_COLON, DIMENSION_NAME_SEP);
  }

  /**
   * Given an attribute filter list, first, it removes the escape chars in order to be able to split
   * by comma and collect the filter list. Then, it recreates the original filters by restoring the
   * escapes chars if any.
   *
   * @return a filter list split by comma
   */
  private static List<String> filterList(String filterItem) {
    Map<Integer, Boolean> escapesToRestore = new HashMap<>();

    StringBuilder filterListToEscape = new StringBuilder(filterItem);

    List<String> filters = new LinkedList<>();

    for (int i = 0; i < filterListToEscape.length() - 1; i++) {
      if (filterListToEscape.charAt(i) == ESCAPE && filterListToEscape.charAt(i + 1) == ESCAPE) {
        filterListToEscape.delete(i, i + 2);
        escapesToRestore.put(i, false);
      }
    }

    String[] escapedFilterList = FILTER_LIST_SPLIT.split(filterListToEscape);

    int beginning = 0;

    for (String escapedFilter : escapedFilterList) {
      filters.add(
          restoreEscape(
              escapesToRestore,
              new StringBuilder(escapedFilter),
              beginning,
              escapedFilter.length()));
      beginning += escapedFilter.length() + 1;
    }

    return filters;
  }

  /**
   * Restores the escape char in a filter based on the position in the original filter. It uses a
   * pad as in a filter there can be more than one escape char removed.
   *
   * @return a filter with restored escape chars
   */
  private static String restoreEscape(
      Map<Integer, Boolean> escapesToRestore, StringBuilder filter, int beginning, int end) {
    int pad = 0;
    for (Map.Entry<Integer, Boolean> slashPositionInFilter : escapesToRestore.entrySet()) {
      if (!slashPositionInFilter.getValue()
          && slashPositionInFilter.getKey() <= (beginning + end)) {
        filter.insert(slashPositionInFilter.getKey() - beginning + pad++, ESCAPE);
        escapesToRestore.put(slashPositionInFilter.getKey(), true);
      }
    }

    return filter.toString();
  }
}
