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
package org.hisp.dhis.common;

import static org.hisp.dhis.common.DimensionalObject.*;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.comparator.ObjectStringValueComparator;
import org.hisp.dhis.dataelement.DataElementOperand;

/**
 * @author Lars Helge Overland
 */
public class DimensionalObjectUtils {
  public static final String COMPOSITE_DIM_OBJECT_ESCAPED_SEP = "\\.";

  public static final String COMPOSITE_DIM_OBJECT_PLAIN_SEP = ".";

  public static final String TITLE_ITEM_SEP = ", ";

  public static final String NULL_REPLACEMENT = "[n/a]";

  public static final String NAME_SEP = "_";

  public static final String COL_SEP = " ";

  /**
   * Matching data element operand, program data element, program attribute, data set reporting rate
   * metric.
   */
  private static final Pattern COMPOSITE_DIM_OBJECT_PATTERN =
      Pattern.compile("(?<id1>\\w+)\\.(?<id2>\\w+|\\*)(\\.(?<id3>\\w+|\\*))?");

  private static final Set<QueryOperator> IGNORED_OPERATORS =
      Set.of(QueryOperator.LIKE, QueryOperator.IN, QueryOperator.SW, QueryOperator.EW);

  /**
   * Returns a list of copied instances of the given list of {@link DimensionalObject}.
   *
   * @param dimensions the list of {@link DimensionalObject}.
   * @return a list of copied instances.
   */
  public static List<DimensionalObject> getCopies(List<DimensionalObject> dimensions) {
    List<DimensionalObject> list = new ArrayList<>();

    if (dimensions != null) {
      for (DimensionalObject dimension : dimensions) {
        DimensionalObject object = ((BaseDimensionalObject) dimension).instance();
        list.add(object);
      }
    }

    return list;
  }

  /**
   * Creates a list of dimension identifiers based on the given list of DimensionalObjects.
   *
   * @param dimensions the list of DimensionalObjects.
   * @return collection of dimension identifiers.
   */
  public static List<String> getDimensions(Collection<DimensionalObject> dimensions) {
    List<String> dims = new ArrayList<>();

    if (dimensions != null) {
      for (DimensionalObject dimension : dimensions) {
        dims.add(dimension.getDimension());
      }
    }

    return dims;
  }

  /**
   * Creates a two-dimensional array of dimension items based on the list of DimensionalObjects.
   * I.e. the list of items of each DimensionalObject is converted to an array and inserted into the
   * outer array in the same order.
   *
   * @param dimensions the list of DimensionalObjects.
   * @return a two-dimensional array of NameableObjects.
   */
  public static NameableObject[][] getItemArray(List<DimensionalObject> dimensions) {
    List<NameableObject[]> arrays = new ArrayList<>();

    for (DimensionalObject dimension : dimensions) {
      arrays.add(dimension.getItems().toArray(new NameableObject[0]));
    }

    return arrays.toArray(new NameableObject[0][]);
  }

  /**
   * Creates a map based on the given array of elements, where each pair of elements are put on them
   * map as a key-value pair.
   *
   * @param elements the elements to put on the map.
   * @return a map.
   */
  @SafeVarargs
  public static <T> Map<T, T> asMap(final T... elements) {
    Map<T, T> map = new HashMap<>();

    if (elements != null && (elements.length % 2 == 0)) {
      for (int i = 0; i < elements.length; i += 2) {
        map.put(elements[i], elements[i + 1]);
      }
    }

    return map;
  }

  /**
   * Retrieves the dimension name from the given string. Returns the part of the string preceding
   * the dimension name separator, or the whole string if the separator is not present.
   *
   * @param param the parameter.
   */
  public static String getDimensionFromParam(String param) {
    if (param == null) {
      return null;
    }

    return param.split(DIMENSION_NAME_SEP).length > 0 ? param.split(DIMENSION_NAME_SEP)[0] : param;
  }

  /**
   * Retrieves the dimension options from the given string. Looks for the part succeeding the
   * dimension name separator, if exists, splits the string part on the option separator and returns
   * the resulting values. If the dimension name separator does not exist an empty list is returned,
   * indicating that all dimension options should be used.
   */
  public static List<String> getDimensionItemsFromParam(String param) {
    if (param == null) {
      return null;
    }

    if (param.split(DIMENSION_NAME_SEP).length > 1) {
      // Extracts dimension items by removing dimension name and separator
      String dimensionItems = param.substring(param.indexOf(DIMENSION_NAME_SEP) + 1);

      // Returns them as List<String>
      return Arrays.asList(dimensionItems.split(OPTION_SEP));
    }

    return new ArrayList<>();
  }

  /**
   * Splits the given string on the ; character and returns the items in a list. Returns null if the
   * given string is null.
   */
  public static List<String> getItemsFromParam(String param) {
    if (param == null) {
      return null;
    }

    return new ArrayList<>(Arrays.asList(param.split(OPTION_SEP)));
  }

  /** Indicates whether at least one of the given dimenions has at least one item. */
  public static boolean anyDimensionHasItems(Collection<DimensionalObject> dimensions) {
    if (dimensions == null || dimensions.isEmpty()) {
      return false;
    }

    for (DimensionalObject dim : dimensions) {
      if (dim.hasItems()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Retrieves the value from a keyword parameter string, which is on the format {@code
   * <KEYWORD>-<VALUE>}. Returns null of parameter string is not on the correct format.
   *
   * @param param the string parameter.
   */
  public static String getValueFromKeywordParam(String param) {
    if (param == null) {
      return null;
    }

    String[] split = param.split(ITEM_SEP);

    if (split.length > 1 && split[1] != null) {
      return split[1];
    }

    return null;
  }

  /**
   * Retrieves the uid from an org unit group parameter string, which is on the format
   * OU_GROUP-<uid> .
   */
  public static String getUidFromGroupParam(String param) {
    return getValueFromKeywordParam(param);
  }

  /**
   * Sets items on the given dimension based on the unique values of the matching column in the
   * given grid. Items are BaseNameableObjects where the name, code and short name properties are
   * set to the column value. The dimension analytics type must be equal to EVENT.
   *
   * @param dimension the dimension.
   * @param naForNull indicates whether a [n/a] string should be used as replacement for null
   *     values.
   * @param grid the grid with data values.
   */
  public static void setDimensionItemsForFilters(
      DimensionalObject dimension, Grid grid, boolean naForNull) {
    if (dimension == null
        || grid == null
        || !AnalyticsType.EVENT.equals(dimension.getAnalyticsType())) {
      return;
    }

    BaseDimensionalObject dim = (BaseDimensionalObject) dimension;

    List<String> filterItems = dim.getFilterItemsAsList();

    List<Object> values = new ArrayList<>(grid.getUniqueValues(dim.getDimension()));

    values.sort(ObjectStringValueComparator.INSTANCE);

    // Use order of items in filter if specified

    List<?> itemList = filterItems != null ? ListUtils.retainAll(filterItems, values) : values;

    List<DimensionalItemObject> items = getDimensionalItemObjects(itemList, naForNull);

    dim.setItems(items);
  }

  /**
   * Accepts filter strings on the format: <code>operator:filter:operator:filter</code> and returns
   * a pretty print version on the format: <code>operator filter, operator filter</code>
   *
   * @param filter the filter.
   * @return a pretty print version of the filter.
   */
  public static String getPrettyFilter(String filter) {
    if (filter == null || !filter.contains(DIMENSION_NAME_SEP)) {
      return null;
    }

    List<String> filterItems = new ArrayList<>();

    String[] split = filter.split(DIMENSION_NAME_SEP);

    for (int i = 0; i < split.length; i += 2) {
      QueryOperator operator = QueryOperator.fromString(split[i]);
      String value = split[i + 1];

      if (operator != null) {
        boolean ignoreOperator = IGNORED_OPERATORS.contains(operator);

        value = value.replaceAll(QueryFilter.OPTION_SEP, TITLE_ITEM_SEP);

        filterItems.add((ignoreOperator ? StringUtils.EMPTY : (operator.getValue() + " ")) + value);
      }
    }

    return StringUtils.join(filterItems, TITLE_ITEM_SEP);
  }

  /**
   * Indicates whether the given string is a composite dimensional object expression.
   *
   * @param expression the expression.
   * @return true if composite dimensional object expression, false if not.
   */
  public static boolean isCompositeDimensionalObject(String expression) {
    return expression != null && COMPOSITE_DIM_OBJECT_PATTERN.matcher(expression).matches();
  }

  /**
   * Returns the first identifier in a composite dimension object identifier.
   *
   * @param compositeItem the composite dimension object identifier.
   * @return the first identifier, or null if not a valid composite identifier or no match.
   */
  public static String getFirstIdentifer(String compositeItem) {
    Matcher matcher = COMPOSITE_DIM_OBJECT_PATTERN.matcher(compositeItem);
    return matcher.matches() ? matcher.group(1) : null;
  }

  /**
   * Returns the second identifier in a composite dimension object identifier.
   *
   * @param compositeItem the composite dimension object identifier.
   * @return the second identifier, or null if not a valid composite identifier or no match.
   */
  public static String getSecondIdentifer(String compositeItem) {
    Matcher matcher = COMPOSITE_DIM_OBJECT_PATTERN.matcher(compositeItem);
    return matcher.matches() ? matcher.group(2) : null;
  }

  /**
   * Indicates whether the given identifier is a wildcard.
   *
   * @param identifier the identifier.
   * @return true if the given identifier is a wildcard, false if not.
   */
  public static boolean isWildCard(String identifier) {
    return SYMBOL_WILDCARD.equals(identifier);
  }

  /**
   * Returns a list of DimensionalItemObjects.
   *
   * @param objects the DimensionalItemObjects to include in the list.
   * @return a list of DimensionalItemObjects.
   */
  public static List<DimensionalItemObject> getList(DimensionalItemObject... objects) {
    List<DimensionalItemObject> list = new ArrayList<>();

    if (objects != null) {
      Collections.addAll(list, objects);
    }

    return list;
  }

  /**
   * Returns a list with erasure DimensionalItemObject based on the given collection.
   *
   * @param collection the collection.
   * @return a list of DimensionalItemObjects.
   */
  public static List<DimensionalItemObject> asList(
      Collection<? extends DimensionalItemObject> collection) {
    return new ArrayList<>(collection);
  }

  /**
   * Returns a list with erasure DimensionalObject based on the given collection.
   *
   * @param collection the collection.
   * @return a list of DimensionalObjects.
   */
  public static List<DimensionalObject> asDimensionalObjectList(
      Collection<? extends DimensionalObject> collection) {
    return new ArrayList<>(collection);
  }

  /**
   * Returns a list typed with the desired erasure based on the given collection. This operation
   * implies an unchecked cast and it is the responsibility of the caller to make sure the cast is
   * valid. A copy of the given list will be returned.
   *
   * @param collection the collection.
   * @return a list.
   */
  @SuppressWarnings("unchecked")
  public static <T extends DimensionalItemObject> List<T> asTypedList(
      Collection<DimensionalItemObject> collection) {
    List<T> list = new ArrayList<>();

    if (collection != null) {
      for (DimensionalItemObject object : collection) {
        list.add((T) object);
      }
    }

    return list;
  }

  /**
   * Returns a list of BaseNameableObjects based on the given list of values, where the name, code
   * and short name of each BaseNameableObject is set to the value of each list item.
   *
   * @param values the list of object values.
   * @param naForNull indicates whether a [n/a] string should be used as replacement for null
   *     values.
   * @return a list of BaseNameableObejcts.
   */
  public static List<DimensionalItemObject> getDimensionalItemObjects(
      Collection<?> values, boolean naForNull) {
    List<DimensionalItemObject> objects = new ArrayList<>();

    for (Object value : values) {
      if (value == null && naForNull) {
        value = NULL_REPLACEMENT;
      }

      if (value != null) {
        String val = String.valueOf(value);

        BaseDimensionalItemObject nameableObject = new BaseDimensionalItemObject(val);
        nameableObject.setShortName(val);
        objects.add(nameableObject);
      }
    }

    return objects;
  }

  /**
   * Returns dimension item identifiers for the given collection of DimensionalItemObject.
   *
   * @param objects the DimensionalItemObjects.
   * @return a list of dimension item identifiers.
   */
  public static List<String> getDimensionalItemIds(Collection<DimensionalItemObject> objects) {
    return objects.stream()
        .map(DimensionalItemObject::getDimensionItem)
        .collect(Collectors.toList());
  }

  /**
   * Gets a set of unique data elements based on the given collection of operands.
   *
   * @param operands the collection of operands.
   * @return a set of data elements.
   */
  public static Set<DimensionalItemObject> getDataElements(
      Collection<DataElementOperand> operands) {
    return operands.stream().map(DataElementOperand::getDataElement).collect(Collectors.toSet());
  }

  /**
   * Gets a set of unique category option combinations based on the given collection of operands.
   *
   * @param operands the collection of operands.
   * @return a set of category option combinations.
   */
  public static Set<DimensionalItemObject> getCategoryOptionCombos(
      Collection<DataElementOperand> operands) {
    return operands.stream()
        .filter(o -> o.getCategoryOptionCombo() != null)
        .map(DataElementOperand::getCategoryOptionCombo)
        .collect(Collectors.toSet());
  }

  /**
   * Gets a set of unique attribute option combinations based on the given collection of operands.
   *
   * @param operands the collection of operands.
   * @return a set of category option combinations.
   */
  public static Set<DimensionalItemObject> getAttributeOptionCombos(
      Collection<DataElementOperand> operands) {
    return operands.stream()
        .filter(o -> o.getAttributeOptionCombo() != null)
        .map(DataElementOperand::getAttributeOptionCombo)
        .collect(Collectors.toSet());
  }

  /**
   * Returns a mapping between the base dimension item identifier and the dimension item identifier
   * defined by the given identifier scheme.
   *
   * @param objects the dimensional item objects.
   * @param idScheme the identifier scheme.
   * @return a mapping between dimension item identifiers.
   */
  public static Map<String, String> getDimensionItemIdSchemeMap(
      Collection<? extends DimensionalItemObject> objects, IdScheme idScheme) {
    Map<String, String> map = Maps.newHashMap();

    objects.forEach(
        obj -> map.put(obj.getDimensionItem(), obj.getDimensionItem(IdScheme.from(idScheme))));

    return map;
  }

  /**
   * Returns a mapping between the base dimension item identifier and the dimension item identifier
   * defined by the given identifier scheme. For each operand, the data element and category option
   * combo identifiers are included in the mapping, not the operand itself.
   *
   * @param dataElementOperands the data element operands.
   * @param idScheme the identifier scheme.
   * @return a mapping between dimension item identifiers.
   */
  public static Map<String, String> getDataElementOperandIdSchemeMap(
      Collection<DataElementOperand> dataElementOperands, IdScheme idScheme) {
    Map<String, String> map = Maps.newHashMap();

    for (DataElementOperand operand : dataElementOperands) {
      map.put(
          operand.getDataElement().getDimensionItem(),
          operand.getDataElement().getDimensionItem(IdScheme.from(idScheme)));
      map.put(
          operand.getCategoryOptionCombo().getDimensionItem(),
          operand.getCategoryOptionCombo().getDimensionItem(IdScheme.from(idScheme)));
    }

    return map;
  }

  /**
   * Returns a dimension item identifier for the given data set identifier and reporting date
   * metric.
   *
   * @param uid data set identifier.
   * @param metric reporting rate metric.
   * @return a dimension item identifier.
   */
  public static String getDimensionItem(String uid, ReportingRateMetric metric) {
    return uid + COMPOSITE_DIM_OBJECT_PLAIN_SEP + metric.name();
  }

  /**
   * Generates a key based on the given lists of {@link NameableObject}. Uses the identifiers for
   * each nameable object, sorts them and writes them out as a key.
   *
   * @param column list of dimension items representing a column, cannot be null.
   * @param row list of dimension items representing a row, cannot be null.
   * @return an identifier representing a column item and a row item.
   */
  public static String getKey(List<DimensionalItemObject> column, List<DimensionalItemObject> row) {
    List<String> ids = new ArrayList<>();

    List<DimensionalItemObject> dimensions = new ArrayList<>();
    dimensions.addAll(column);
    dimensions.addAll(row);

    for (DimensionalItemObject item : dimensions) {
      ids.add(item.getDimensionItem());
    }

    Collections.sort(ids);

    return StringUtils.join(ids, DIMENSION_SEP);
  }

  /**
   * Returns a map with sorted keys. Keys are sorted by splitting on the '-' character and sorting
   * the components alphabetically.
   *
   * @param valueMap the mapping of keys and values.
   * @return a map with sorted keys.
   */
  public static Map<String, Object> getSortedKeysMap(Map<String, Object> valueMap) {
    Map<String, Object> map = new HashMap<>();

    for (String key : valueMap.keySet()) {
      String sortKey = sortKey(key);

      if (sortKey != null) {
        map.put(sortKey, valueMap.get(key));
      }
    }

    return map;
  }

  /**
   * Sorts the given key by splitting on the '-' character and sorting the components
   * alphabetically.
   *
   * @param key the mapping of keys and values.
   */
  public static String sortKey(String key) {
    if (key != null) {
      List<String> ids = Lists.newArrayList(key.split(DIMENSION_SEP));
      Collections.sort(ids);
      key = StringUtils.join(ids, DIMENSION_SEP);
    }

    return key;
  }

  /**
   * Returns a string suitable as key based on the given list of objects.
   *
   * @param objects the list of {@link DimensionalItemObject}.
   * @return a name string.
   */
  public static String getKey(List<DimensionalItemObject> objects) {
    return objects.stream()
        .map(DimensionalItemObject::getShortName)
        .collect(Collectors.joining(NAME_SEP))
        .replaceAll(" ", NAME_SEP)
        .toLowerCase();
  }

  /**
   * Returns a string suitable as name based on the given list of objects.
   *
   * @param objects the list of {@link DimensionalItemObject}.
   * @return a column name string.
   */
  public static String getName(List<DimensionalItemObject> objects) {
    return objects.stream()
        .map(DimensionalItemObject::getShortName)
        .collect(Collectors.joining(COL_SEP));
  }

  /**
   * Transforms a List of {@see DimensionItemObjectValue} into a Map of {@see DimensionalItemObject}
   * and value
   */
  public static Map<DimensionalItemObject, Object> convertToDimItemValueMap(
      List<DimensionItemObjectValue> dimensionItemObjectValues) {
    return dimensionItemObjectValues.stream()
        .filter(item -> item.getValue() != null)
        .collect(
            Collectors.toMap(
                DimensionItemObjectValue::getDimensionalItemObject,
                DimensionItemObjectValue::getValue));
  }
}
