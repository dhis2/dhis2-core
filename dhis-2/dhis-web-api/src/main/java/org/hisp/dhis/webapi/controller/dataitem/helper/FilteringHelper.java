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
package org.hisp.dhis.webapi.controller.dataitem.helper;

import static com.google.common.base.Enums.getIfPresent;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.EnumUtils.getEnumMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.wrap;
import static org.hisp.dhis.common.ValueType.fromString;
import static org.hisp.dhis.common.ValueType.getAggregatables;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.DISPLAY_NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.DISPLAY_SHORT_NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.IDENTIFIABLE_TOKEN_COMPARISON;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.PROGRAM_ID;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.ROOT_JUNCTION;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.SHORT_NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.UID;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.USER_GROUP_UIDS;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.VALUE_TYPES;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.addIlikeReplacingCharacters;
import static org.hisp.dhis.feedback.ErrorCode.E2014;
import static org.hisp.dhis.feedback.ErrorCode.E2016;
import static org.hisp.dhis.query.operators.TokenUtils.getTokens;
import static org.hisp.dhis.user.UserSettingKey.DB_LOCALE;
import static org.hisp.dhis.user.UserSettingKey.UI_LOCALE;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.DIMENSION_TYPE_EQUAL;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.DIMENSION_TYPE_IN;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.DISPLAY_NAME_ILIKE;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.DISPLAY_SHORT_NAME_ILIKE;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.IDENTIFIABLE_TOKEN;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.ID_EQUAL;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.NAME_ILIKE;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.PROGRAM_ID_EQUAL;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.SHORT_NAME_ILIKE;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.VALUE_TYPE_EQUAL;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.VALUE_TYPE_IN;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.containsFilterWithAnyOfPrefixes;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.filterHasPrefix;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataitem.query.QueryableDataItem;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.dataitem.Filter;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Helper class responsible for reading and extracting the URL filters.
 *
 * @author maikel arabori
 */
@NoArgsConstructor(access = PRIVATE)
public class FilteringHelper {
  /**
   * This method will return the respective BaseDimensionalItemObject class from the filter
   * provided.
   *
   * @param filter should have the format of "dimensionItemType:in:[INDICATOR,DATA_SET,...]", where
   *     INDICATOR and DATA_SET represents the BaseDimensionalItemObject. The valid types are found
   *     at {@link org.hisp.dhis.common.DataDimensionItemType}
   * @return the respective classes associated with the given IN filter
   * @throws IllegalQueryException if the filter points to a non supported class/entity
   */
  public static Set<Class<? extends BaseIdentifiableObject>> extractEntitiesFromInFilter(
      String filter) {
    Set<Class<? extends BaseIdentifiableObject>> dataItemsEntity = new HashSet<>();

    if (contains(filter, DIMENSION_TYPE_IN.getCombination())) {
      String[] dimensionItemTypesInFilter =
          split(deleteWhitespace(substringBetween(filter, "[", "]")), ",");

      if (isNotEmpty(dimensionItemTypesInFilter)) {
        for (String dimensionItem : dimensionItemTypesInFilter) {
          dataItemsEntity.add(entityClassFromString(dimensionItem));
        }
      } else {
        throw new IllegalQueryException(new ErrorMessage(E2014, filter));
      }
    }

    return dataItemsEntity;
  }

  /**
   * This method will return the respective BaseDimensionalItemObject class from the filter
   * provided.
   *
   * @param filter should have the format of "dimensionItemType:eq:INDICATOR", where INDICATOR
   *     represents the BaseDimensionalItemObject. It could be any value represented by {@link
   *     org.hisp.dhis.common.DataDimensionItemType}
   * @return the respective class associated with the given filter
   * @throws IllegalQueryException if the filter points to a non supported class/entity
   */
  public static Class<? extends BaseIdentifiableObject> extractEntityFromEqualFilter(
      String filter) {
    final byte DIMENSION_TYPE = 2;
    Class<? extends BaseIdentifiableObject> entity = null;

    if (filterHasPrefix(filter, DIMENSION_TYPE_EQUAL.getCombination())) {
      String[] dimensionFilterPair = filter.split(":");
      boolean hasDimensionType = dimensionFilterPair.length == 3;

      if (hasDimensionType) {
        entity = entityClassFromString(dimensionFilterPair[DIMENSION_TYPE]);
      } else {
        throw new IllegalQueryException(new ErrorMessage(E2014, filter));
      }
    }

    return entity;
  }

  /**
   * This method will return ALL respective ValueType's from the filter. It will merge both EQ and
   * IN conditions into a single Set object.
   *
   * @param filters coming from the URL params/filters
   * @return all respective value type's associated with the given filter
   * @throws IllegalQueryException if the filter points to a non supported value type
   */
  public static Set<String> extractAllValueTypesFromFilters(Set<String> filters) {
    Set<String> valueTypes = new HashSet<>();

    Iterator<String> iterator = filters.iterator();

    while (iterator.hasNext()) {
      String filter = iterator.next();
      Set<String> multipleValueTypes = extractValueTypesFromInFilter(filter);
      String singleValueType = extractValueTypeFromEqualFilter(filter);

      if (CollectionUtils.isNotEmpty(multipleValueTypes)) {
        valueTypes.addAll(multipleValueTypes);
      }

      if (singleValueType != null) {
        valueTypes.add(singleValueType);
      }
    }

    return valueTypes;
  }

  /**
   * Extracts the actual value, from the set of filters, that matches the given combination.
   *
   * <p>ie.: from a list of filters: "dimensionItemType:eq:INDICATOR", "name:ilike:john", extract
   * the value from the combination NAME_ILIKE( "name:ilike:" ). This will return "john".
   *
   * @param filters
   * @param filterCombination
   * @return the value extracted from the respective filter combination
   */
  public static String extractValueFromFilter(
      Set<String> filters, Filter.Combination filterCombination) {
    final byte FILTER_VALUE = 2;

    if (CollectionUtils.isNotEmpty(filters)) {
      for (String filter : filters) {
        if (filterHasPrefix(filter, filterCombination.getCombination())) {
          String[] array = filter.split(":");
          boolean hasValue = array.length == 3;

          if (hasValue) {
            return array[FILTER_VALUE];
          } else {
            throw new IllegalQueryException(new ErrorMessage(E2014, filter));
          }
        }
      }
    }

    return EMPTY;
  }

  /**
   * Extracts the actual value (trimmed or not), from the set of filters, that matches the given
   * combination.
   *
   * <p>ie.: from a list of filters: "dimensionItemType:eq:INDICATOR", "name:ilike:john", extract
   * the value from the combination NAME_ILIKE( "name:ilike:" ). This will return "john".
   *
   * @param filters
   * @param filterCombination
   * @param trimmed automatically trims the extracted value returned when this flag is set to true
   * @return the value extracted from the respective filter combination
   */
  public static String extractValueFromFilter(
      Set<String> filters, Filter.Combination filterCombination, boolean trimmed) {
    String value = extractValueFromFilter(filters, filterCombination);

    return trimmed ? trimToEmpty(value) : value;
  }

  /**
   * Sets the filtering defined by filters list into the paramsMap.
   *
   * @param filters the source of filtering params
   * @param paramsMap the map that will receive the filtering params
   * @param currentUser the current user logged
   */
  public static void setFilteringParams(
      Set<String> filters, WebOptions options, MapSqlParameterSource paramsMap, User currentUser) {
    Locale currentLocale =
        ObjectUtils.defaultIfNull(
            CurrentUserUtil.getUserSetting(DB_LOCALE), CurrentUserUtil.getUserSetting(UI_LOCALE));

    if (currentLocale != null && isNotBlank(currentLocale.getLanguage())) {
      paramsMap.addValue(LOCALE, trimToEmpty(currentLocale.getLanguage()));
    }

    String ilikeName = extractValueFromFilter(filters, NAME_ILIKE);
    addIlikeComparatorIfNotEmpty(paramsMap, NAME, ilikeName);

    String ilikeDisplayName = extractValueFromFilter(filters, DISPLAY_NAME_ILIKE);
    addIlikeComparatorIfNotEmpty(paramsMap, DISPLAY_NAME, ilikeDisplayName);

    String ilikeShortName = extractValueFromFilter(filters, SHORT_NAME_ILIKE);
    addIlikeComparatorIfNotEmpty(paramsMap, SHORT_NAME, ilikeShortName);

    String ilikeDisplayShortName = extractValueFromFilter(filters, DISPLAY_SHORT_NAME_ILIKE);
    addIlikeComparatorIfNotEmpty(paramsMap, DISPLAY_SHORT_NAME, ilikeDisplayShortName);

    String equalId = extractValueFromFilter(filters, ID_EQUAL, true);
    addIfNotBlank(paramsMap, UID, equalId);

    String rootJunction = options.getRootJunction().name();
    addIfNotBlank(paramsMap, ROOT_JUNCTION, rootJunction);

    String identifiableToken = extractValueFromFilter(filters, IDENTIFIABLE_TOKEN);

    if (identifiableToken != null) {
      List<String> wordsAsTokens = getTokens(identifiableToken);

      if (CollectionUtils.isNotEmpty(wordsAsTokens)) {
        paramsMap.addValue(IDENTIFIABLE_TOKEN_COMPARISON, StringUtils.join(wordsAsTokens, ","));
      }
    }

    if (containsFilterWithAnyOfPrefixes(
        filters, VALUE_TYPE_EQUAL.getCombination(), VALUE_TYPE_IN.getCombination())) {
      Set<String> valueTypesFilter = extractAllValueTypesFromFilters(filters);
      assertThatValueTypeFilterHasOnlyAggregatableTypes(valueTypesFilter, filters);

      paramsMap.addValue(VALUE_TYPES, extractAllValueTypesFromFilters(filters));
    } else {
      // Includes all value types.
      paramsMap.addValue(VALUE_TYPES, getAggregatables().stream().map(Enum::name).collect(toSet()));
    }

    // Add program id filtering id, if present.
    String programId = extractValueFromFilter(filters, PROGRAM_ID_EQUAL, true);
    addIfNotBlank(paramsMap, PROGRAM_ID, programId);

    // Add user group filtering, when present.
    if (currentUser != null && CollectionUtils.isNotEmpty(currentUser.getGroups())) {
      Set<String> userGroupUids =
          currentUser.getGroups().stream()
              .filter(Objects::nonNull)
              .map(group -> trimToEmpty(group.getUid()))
              .collect(toSet());
      paramsMap.addValue(USER_GROUP_UIDS, "{" + join(",", userGroupUids) + "}");
    }
  }

  private static void addIlikeComparatorIfNotEmpty(
      MapSqlParameterSource paramsMap, String key, String value) {
    if (StringUtils.isNotEmpty(value)) {
      paramsMap.addValue(key, wrap(addIlikeReplacingCharacters(value), "%"));
    }
  }

  private static void addIfNotBlank(MapSqlParameterSource paramsMap, String key, String value) {
    if (isNotBlank(value)) {
      paramsMap.addValue(key, value);
    }
  }

  /**
   * Simply checks if the given set of ValueType names contains a valid value type filter. Only
   * aggregatable types are considered valid for this case.
   *
   * @param valueTypeNames
   * @throws IllegalQueryException if the given Set<String> contains non-aggregatable value types
   */
  public static void assertThatValueTypeFilterHasOnlyAggregatableTypes(
      Set<String> valueTypeNames, Set<String> filters) {
    if (CollectionUtils.isNotEmpty(valueTypeNames)) {
      List<String> aggregatableTypes =
          getAggregatables().stream().map(Enum::name).collect(toList());

      for (String valueType : valueTypeNames) {
        if (!aggregatableTypes.contains(valueType)) {
          throw new IllegalQueryException(
              new ErrorMessage(E2016, valueType, filters, ValueType.getAggregatables()));
        }
      }
    }
  }

  /**
   * This method will return the respective ValueType from the filter provided.
   *
   * @param filter should have the format of "valueType:in:[TEXT,BOOLEAN,NUMBER,...]", where TEXT
   *     and BOOLEAN represents the ValueType. The valid types are found at {@link ValueType}
   * @return the respective classes associated with the given IN filter
   * @throws IllegalQueryException if the filter points to a non supported value type
   */
  private static Set<String> extractValueTypesFromInFilter(String filter) {
    Set<String> valueTypes = new HashSet<>();

    if (contains(filter, VALUE_TYPE_IN.getCombination())) {
      String[] valueTypesInFilter =
          split(deleteWhitespace(substringBetween(filter, "[", "]")), ",");

      if (isNotEmpty(valueTypesInFilter)) {
        for (String valueType : valueTypesInFilter) {
          valueTypes.add(getValueTypeOrThrow(valueType));
        }
      } else {
        throw new IllegalQueryException(new ErrorMessage(E2014, filter));
      }
    }

    return valueTypes;
  }

  /**
   * This method will return the respective ValueType from the filter provided.
   *
   * @param filter should have the format of "valueType:eq:NUMBER", where NUMBER represents the
   *     ValueType. It could be any value represented by {@link ValueType}
   * @return the respective value type associated with the given filter
   * @throws IllegalQueryException if the filter points to a non supported value type
   */
  private static String extractValueTypeFromEqualFilter(String filter) {
    final byte VALUE_TYPE = 2;
    String valueType = null;

    if (filterHasPrefix(filter, VALUE_TYPE_EQUAL.getCombination())) {
      String[] array = filter.split(":");
      boolean hasValueType = array.length == 3;

      if (hasValueType) {
        valueType = getValueTypeOrThrow(array[VALUE_TYPE]);
      } else {
        throw new IllegalQueryException(new ErrorMessage(E2014, filter));
      }
    }

    return valueType;
  }

  private static String getValueTypeOrThrow(String valueType) {
    try {
      return fromString(trimToEmpty(valueType)).name();
    } catch (IllegalArgumentException e) {
      throw new IllegalQueryException(
          new ErrorMessage(E2016, valueType, "valueType", ValueType.getAggregatables()));
    }
  }

  private static Class<? extends BaseIdentifiableObject> entityClassFromString(
      String dimensionItem) {
    QueryableDataItem item = getIfPresent(QueryableDataItem.class, dimensionItem).orNull();

    if (item == null) {
      throw new IllegalQueryException(
          new ErrorMessage(
              E2016,
              dimensionItem,
              "dimensionItemType",
              getEnumMap(QueryableDataItem.class).keySet()));
    }

    return item.getEntity();
  }
}
