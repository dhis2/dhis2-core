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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;

/**
 * RequestParamUtils are functions used to parse and transform tracker request parameters. This
 * class is intended to only house functions without any dependencies on services or components.
 */
public class RequestParamsValidator {
  private RequestParamsValidator() {
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
   * Helps us transition request parameters that contained semicolon separated UIDs (deprecated) to
   * comma separated UIDs in a backwards compatible way.
   *
   * @param deprecatedParamName request parameter name of deprecated semi-colon separated parameter
   * @param deprecatedParamUids semicolon separated uids
   * @param newParamName new request parameter replacing deprecated request parameter
   * @param newParamUids new request parameter uids
   * @return uids from the request parameter containing uids
   * @throws BadRequestException when both deprecated and new request parameter contain uids
   */
  public static Set<UID> validateDeprecatedUidsParameter(
      String deprecatedParamName,
      String deprecatedParamUids,
      String newParamName,
      Set<UID> newParamUids)
      throws BadRequestException {
    Set<String> deprecatedParamParsedUids = parseUids(deprecatedParamUids);
    if (!deprecatedParamParsedUids.isEmpty() && !newParamUids.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "Only one parameter of '%s' (deprecated; semicolon separated UIDs) and '%s' (comma separated UIDs) must be specified. Prefer '%s' as '%s' will be removed.",
              deprecatedParamName, newParamName, newParamName, deprecatedParamName));
    }

    return !deprecatedParamParsedUids.isEmpty()
        ? deprecatedParamParsedUids.stream().map(UID::of).collect(Collectors.toSet())
        : newParamUids;
  }

  /**
   * Helps us transition request parameters from a deprecated to a new one.
   *
   * @param deprecatedParamName request parameter name of deprecated parameter
   * @param deprecatedParam value of deprecated request parameter
   * @param newParamName new request parameter replacing deprecated request parameter
   * @param newParam value of the request parameter
   * @return value of the one request parameter that is non-null
   * @throws BadRequestException when both deprecated and new request parameter are non-null
   */
  public static <T> T validateDeprecatedParameter(
      String deprecatedParamName, T deprecatedParam, String newParamName, T newParam)
      throws BadRequestException {
    if (newParam != null && deprecatedParam != null) {
      throw new BadRequestException(
          String.format(
              "Only one parameter of '%s' and '%s' must be specified. Prefer '%s' as '%s' will be removed.",
              deprecatedParamName, newParamName, newParamName, deprecatedParamName));
    }

    return newParam != null ? newParam : deprecatedParam;
  }

  /**
   * Helps us transition mandatory request parameters from a deprecated to a new one. At least one
   * parameter must be non-empty as the deprecated one was mandatory.
   *
   * @param deprecatedParamName request parameter name of deprecated parameter
   * @param deprecatedParam value of deprecated request parameter
   * @param newParamName new request parameter replacing deprecated request parameter
   * @param newParam value of the request parameter
   * @return value of the one request parameter that is non-empty
   * @throws BadRequestException when both deprecated and new request parameter are non-null
   * @throws BadRequestException when both deprecated and new request parameter are null
   */
  public static UID validateMandatoryDeprecatedUidParameter(
      String deprecatedParamName, UID deprecatedParam, String newParamName, UID newParam)
      throws BadRequestException {
    UID uid =
        validateDeprecatedParameter(deprecatedParamName, deprecatedParam, newParamName, newParam);

    if (uid == null) {
      throw new BadRequestException(
          String.format("Required request parameter '%s' is not present", newParamName));
    }

    return uid;
  }

  /**
   * Parse semicolon separated string of UIDs.
   *
   * @param input string to parse
   * @return set of uids
   */
  private static Set<String> parseUids(String input) {
    return parseUidString(input).collect(Collectors.toSet());
  }

  private static Stream<String> parseUidString(String input) {
    return CollectionUtils.emptyIfNull(TextUtils.splitToSet(input, TextUtils.SEMICOLON)).stream();
  }

  /**
   * Validate the {@code order} request parameter in tracker exporters. Allowed order values are
   * {@code supportedFieldNames} and UIDs which represent {@code uidMeaning}. Every field name or
   * UID can be specified at most once.
   */
  public static void validateOrderParams(
      List<OrderCriteria> order, Set<String> supportedFieldNames, String uidMeaning)
      throws BadRequestException {
    if (order == null || order.isEmpty()) {
      return;
    }

    Set<String> invalidOrderComponents =
        order.stream().map(OrderCriteria::getField).collect(Collectors.toSet());
    invalidOrderComponents.removeAll(supportedFieldNames);
    Set<String> uids =
        invalidOrderComponents.stream()
            .filter(CodeGenerator::isValidUid)
            .collect(Collectors.toSet());
    invalidOrderComponents.removeAll(uids);

    String errorSuffix =
        String.format(
            "Supported are %s UIDs and fields '%s'. All of which can at most be specified once.",
            uidMeaning, String.join(", ", supportedFieldNames.stream().sorted().toList()));
    if (!invalidOrderComponents.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "order parameter is invalid. Cannot order by '%s'. %s",
              String.join(", ", invalidOrderComponents), errorSuffix));
    }

    validateOrderParamsContainNoDuplicates(order, errorSuffix);
  }

  private static void validateOrderParamsContainNoDuplicates(
      List<OrderCriteria> order, String errorSuffix) throws BadRequestException {
    Set<String> duplicateOrderComponents =
        order.stream()
            .map(OrderCriteria::getField)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(Entry::getKey)
            .collect(Collectors.toSet());

    if (!duplicateOrderComponents.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "order parameter is invalid. '%s' are repeated. %s",
              String.join(", ", duplicateOrderComponents), errorSuffix));
    }
  }

  /**
   * Validate the {@code order} request parameter in tracker exporters. Allowed order values are
   * {@code supportedFieldNames}. Every field name can be specified at most once. If the endpoint
   * supports field names and UIDs use {@link #validateOrderParams(List, Set, String)}.
   */
  public static void validateOrderParams(List<OrderCriteria> order, Set<String> supportedFieldNames)
      throws BadRequestException {
    if (order == null || order.isEmpty()) {
      return;
    }

    Set<String> invalidOrderComponents =
        order.stream().map(OrderCriteria::getField).collect(Collectors.toSet());
    invalidOrderComponents.removeAll(supportedFieldNames);

    String errorSuffix =
        String.format(
            "Supported fields are '%s'. All of which can at most be specified once.",
            String.join(", ", supportedFieldNames.stream().sorted().toList()));
    if (!invalidOrderComponents.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "order parameter is invalid. Cannot order by '%s'. %s",
              String.join(", ", invalidOrderComponents), errorSuffix));
    }

    validateOrderParamsContainNoDuplicates(order, errorSuffix);
  }

  /**
   * Parse given {@code input} string representing a filter for an object referenced by a UID like a
   * tracked entity attribute or data element. Refer to {@link #parseSanitizedFilters(Map, String)}}
   * for details on the expected input format.
   *
   * @return filters by UIDs
   */
  public static Map<String, List<QueryFilter>> parseFilters(String input)
      throws BadRequestException {
    Map<String, List<QueryFilter>> result = new HashMap<>();
    if (StringUtils.isBlank(input)) {
      return result;
    }

    for (String uidOperatorValue : filterList(input)) {
      parseSanitizedFilters(result, uidOperatorValue);
    }
    return result;
  }

  /**
   * Accumulate {@link QueryFilter}s per UID by parsing given input string of format
   * {uid}[:{operator}:{value}]. Only the UID is mandatory. Multiple operator:value pairs are
   * allowed. A {@link QueryFilter} for each operator:value pair is added to the corresponding UID.
   *
   * @throws BadRequestException filter is neither multiple nor single operator:value format
   */
  private static void parseSanitizedFilters(Map<String, List<QueryFilter>> result, String input)
      throws BadRequestException {
    int uidIndex = input.indexOf(DIMENSION_NAME_SEP) + 1;

    if (uidIndex == 0 || input.length() == uidIndex) {
      String uid = input.replace(DIMENSION_NAME_SEP, "");
      result.putIfAbsent(uid, new ArrayList<>());
      return;
    }

    String uid = input.substring(0, uidIndex - 1);
    result.putIfAbsent(uid, new ArrayList<>());

    String[] filters = FILTER_ITEM_SPLIT.split(input.substring(uidIndex));

    // single operator
    if (filters.length == 2) {
      result.get(uid).add(operatorValueQueryFilter(filters[0], filters[1], input));
    }
    // multiple operator
    else if (filters.length == 4) {
      for (int i = 0; i < filters.length; i += 2) {
        result.get(uid).add(operatorValueQueryFilter(filters[i], filters[i + 1], input));
      }
    } else {
      throw new BadRequestException(INVALID_FILTER + input);
    }
  }

  /**
   * Validates that no org unit is present if the ou mode is ACCESSIBLE or CAPTURE. If it is, an
   * exception will be thrown. If the org unit mode is not defined, SELECTED will be used by default
   * if an org unit is present. Otherwise, ACCESSIBLE will be the default.
   *
   * @param orgUnits list of org units to be validated
   * @return a valid org unit mode
   * @throws BadRequestException if a wrong combination of org unit and org unit mode supplied
   */
  public static OrganisationUnitSelectionMode validateOrgUnitMode(
      Set<UID> orgUnits, OrganisationUnitSelectionMode orgUnitMode) throws BadRequestException {

    if (orgUnitMode == null) {
      return orgUnits.isEmpty() ? ACCESSIBLE : SELECTED;
    }

    if (!orgUnits.isEmpty()
        && (orgUnitMode == ACCESSIBLE || orgUnitMode == CAPTURE || orgUnitMode == ALL)) {
      throw new BadRequestException(
          String.format(
              "orgUnitMode %s cannot be used with orgUnits. Please remove the orgUnit parameter and try again.",
              orgUnitMode));
    }

    if ((orgUnitMode == CHILDREN || orgUnitMode == SELECTED || orgUnitMode == DESCENDANTS)
        && orgUnits.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "At least one org unit is required for orgUnitMode: %s. Please add an orgUnit or use a different orgUnitMode.",
              orgUnitMode));
    }

    return orgUnitMode;
  }

  /**
   * Validates that the org unit is not present if the ou mode is ACCESSIBLE or CAPTURE. If it is,
   * an exception will be thrown. If the org unit mode is not defined, SELECTED will be used by
   * default if an org unit is present. Otherwise, ACCESSIBLE will be the default.
   *
   * @param orgUnit the org unit to validate
   * @return a valid org unit mode
   * @throws BadRequestException if a wrong combination of org unit and org unit mode supplied
   */
  public static OrganisationUnitSelectionMode validateOrgUnitMode(
      UID orgUnit, OrganisationUnitSelectionMode orgUnitMode) throws BadRequestException {

    if (orgUnitMode == null) {
      orgUnitMode = orgUnit != null ? SELECTED : ACCESSIBLE;
    }

    if ((orgUnitMode == ACCESSIBLE || orgUnitMode == CAPTURE) && orgUnit != null) {
      throw new BadRequestException(
          String.format(
              "orgUnitMode %s cannot be used with orgUnits. Please remove the orgUnit parameter and try again.",
              orgUnitMode));
    }

    if ((orgUnitMode == CHILDREN || orgUnitMode == SELECTED || orgUnitMode == DESCENDANTS)
        && orgUnit == null) {
      throw new BadRequestException(
          String.format(
              "orgUnit is required for orgUnitMode: %s. Please add an orgUnit or use a different orgUnitMode.",
              orgUnitMode));
    }

    return orgUnitMode;
  }

  public static void validatePaginationParameters(PageRequestParams params)
      throws BadRequestException {
    if (Boolean.TRUE.equals(params.getSkipPaging())
        && (ObjectUtils.firstNonNull(params.getPage(), params.getPageSize()) != null
            || Boolean.TRUE.equals(params.getTotalPages()))) {
      throw new BadRequestException(
          "Paging cannot be skipped with isSkipPaging=true while also requesting a paginated response with page, pageSize and/or totalPages=true");
    }

    if (lessThan(params.getPage(), 1)) {
      throw new BadRequestException("page must be greater than or equal to 1 if specified");
    }

    if (lessThan(params.getPageSize(), 1)) {
      throw new BadRequestException("pageSize must be greater than or equal to 1 if specified");
    }
  }

  private static boolean lessThan(Integer a, int b) {
    return a != null && a < b;
  }

  private static QueryFilter operatorValueQueryFilter(String operator, String value, String filter)
      throws BadRequestException {
    if (StringUtils.isEmpty(operator) || StringUtils.isEmpty(value)) {
      throw new BadRequestException(INVALID_FILTER + filter);
    }

    try {
      return new QueryFilter(QueryOperator.fromString(operator), escapedFilterValue(value));

    } catch (IllegalArgumentException exception) {
      throw new BadRequestException(INVALID_FILTER + filter);
    }
  }

  /**
   * Replace escaped comma or colon
   *
   * @param value
   * @return
   */
  private static String escapedFilterValue(String value) {
    return value.replace(ESCAPE_COMMA, COMMA_STRING).replace(ESCAPE_COLON, DIMENSION_NAME_SEP);
  }

  /**
   * Given an attribute filter list, first, it removes the escape chars in order to be able to split
   * by comma and collect the filter list. Then, it recreates the original filters by restoring the
   * escapes chars if any.
   *
   * @param filterItem
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
   * @param escapesToRestore
   * @param filter
   * @param beginning
   * @param end
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
