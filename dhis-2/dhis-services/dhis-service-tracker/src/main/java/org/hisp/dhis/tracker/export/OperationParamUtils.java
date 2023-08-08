/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.util.CheckedFunction;

public class OperationParamUtils {

  private OperationParamUtils() {
    throw new IllegalStateException("Utility class");
  }

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
   * Parse request parameter to filter tracked entity attributes using UID, operator and values.
   * Refer to {@link #parseQueryItem(String, CheckedFunction)} for details on the expected item
   * format.
   *
   * @param filterItem query item strings each composed of UID, operator and value
   * @param attributes tracked entity attribute map from UIDs to attributes
   * @return query items each of a tracked entity attribute with attached query filters
   */
  public static List<QueryItem> parseAttributeQueryItems(
      String filterItem, Map<String, TrackedEntityAttribute> attributes)
      throws BadRequestException {
    if (StringUtils.isEmpty(filterItem)) {
      return new ArrayList<>();
    }

    List<String> uidOperatorValues = filterList(filterItem);

    List<QueryItem> itemList = new ArrayList<>();
    for (String uidOperatorValue : uidOperatorValues) {
      itemList.add(parseQueryItem(uidOperatorValue, id -> attributeToQueryItem(id, attributes)));
    }

    return itemList;
  }

  /**
   * Creates a QueryItem with QueryFilters from the given item string. Expected item format is
   * {uid}:{operator}:{value}[:{operator}:{value}]. Only the UID is mandatory. Multiple
   * operator:value pairs are allowed.
   *
   * <p>The UID is passed to given map function which translates UID to a QueryItem. A QueryFilter
   * for each operator:value pair is then added to this QueryItem.
   *
   * @throws BadRequestException filter is neither multiple nor single operator:value format
   */
  public static QueryItem parseQueryItem(
      String items, CheckedFunction<String, QueryItem> uidToQueryItem) throws BadRequestException {
    int uidIndex = items.indexOf(DIMENSION_NAME_SEP) + 1;

    if (uidIndex == 0 || items.length() == uidIndex) {
      return uidToQueryItem.apply(items.replace(DIMENSION_NAME_SEP, ""));
    }

    QueryItem queryItem = uidToQueryItem.apply(items.substring(0, uidIndex - 1));

    String[] filters = FILTER_ITEM_SPLIT.split(items.substring(uidIndex));

    // single operator
    if (filters.length == 2) {
      queryItem.getFilters().add(operatorValueQueryFilter(filters[0], filters[1], items));
    }
    // multiple operator
    else if (filters.length == 4) {
      for (int i = 0; i < filters.length; i += 2) {
        queryItem.getFilters().add(operatorValueQueryFilter(filters[i], filters[i + 1], items));
      }
    } else {
      throw new BadRequestException("Query item or filter is invalid: " + items);
    }

    return queryItem;
  }

  /**
   * Creates a QueryFilter from the given query string. Query is on format
   * {operator}:{filter-value}. Only the filter-value is mandatory. The EQ QueryOperator is used as
   * operator if not specified. We split the query at the first delimiter, so the filter value can
   * be any sequence of characters
   *
   * @throws BadRequestException given invalid query string
   */
  public static QueryFilter parseQueryFilter(String filter) throws BadRequestException {
    if (StringUtils.isEmpty(filter)) {
      return null;
    }

    if (!filter.contains(DimensionalObject.DIMENSION_NAME_SEP)) {
      return new QueryFilter(QueryOperator.EQ, filter);
    }

    return operatorValueQueryFilter(FILTER_ITEM_SPLIT.split(filter), filter);
  }

  private static QueryFilter operatorValueQueryFilter(String[] operatorValue, String filter)
      throws BadRequestException {
    if (null == operatorValue || operatorValue.length < 2) {
      throw new BadRequestException("Query item or filter is invalid: " + filter);
    }

    return operatorValueQueryFilter(operatorValue[0], operatorValue[1], filter);
  }

  public static QueryFilter operatorValueQueryFilter(String operator, String value, String filter)
      throws BadRequestException {
    if (StringUtils.isEmpty(operator) || StringUtils.isEmpty(value)) {
      throw new BadRequestException("Query item or filter is invalid: " + filter);
    }

    try {
      return new QueryFilter(QueryOperator.fromString(operator), escapedFilterValue(value));

    } catch (IllegalArgumentException exception) {
      throw new BadRequestException("Query item or filter is invalid: " + filter);
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
  public static List<String> filterList(String filterItem) {
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

  private static QueryItem attributeToQueryItem(
      String uid, Map<String, TrackedEntityAttribute> attributes) throws BadRequestException {
    if (attributes.isEmpty()) {
      throw new BadRequestException("Attribute does not exist: " + uid);
    }

    TrackedEntityAttribute at = attributes.get(uid);
    if (at == null) {
      throw new BadRequestException("Attribute does not exist: " + uid);
    }

    return new QueryItem(
        at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet(), at.isUnique());
  }
}
