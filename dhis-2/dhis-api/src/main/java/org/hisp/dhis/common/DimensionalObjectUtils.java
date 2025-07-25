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

import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.common.DimensionalObject.ITEM_SEP;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.hisp.dhis.common.comparator.ObjectStringValueComparator;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.eventvisualization.Attribute;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

/**
 * @author Lars Helge Overland
 */
@NoArgsConstructor(access = PRIVATE)
public class DimensionalObjectUtils {
  public static final String COMPOSITE_DIM_OBJECT_ESCAPED_SEP = "\\.";

  public static final String COMPOSITE_DIM_OBJECT_PLAIN_SEP = ".";

  public static final String TITLE_ITEM_SEP = ", ";

  public static final String NULL_REPLACEMENT = "[n/a]";

  public static final String NAME_SEP = "_";

  public static final String COL_SEP = " ";

  /**
   * Matching data element operand, program data element, program attribute, data set reporting rate
   * metric, program data element option, etc. ie: IpHINAT79UW.UuL3eX8KJHY.uODmvdTEeMr.fgffggdf
   */
  private static final Pattern COMPOSITE_DIM_OBJECT_PATTERN =
      Pattern.compile(
          "(?<id1>\\w+)\\.(?<id2>\\w+|\\*)(\\.(?<id3>\\w+|\\*)(\\.(?<id4>\\w+|\\*))?)?");

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
        list.add(copyOf(dimension));
      }
    }

    return list;
  }

  public static DimensionalObject copyOf(DimensionalObject sample) {
    BaseDimensionalObject object =
        new BaseDimensionalObject(
            sample.getUid(),
            sample.getDimensionType(),
            sample.getDimensionName(),
            sample.getDimensionDisplayName(),
            sample.getItems(),
            sample.isAllItems());

    object.setLegendSet(sample.getLegendSet());
    object.setAggregationType(sample.getAggregationType());
    object.setFilter(sample.getFilter());
    object.setDataDimension(sample.isDataDimension());
    object.setFixed(sample.isFixed());
    object.setDimensionalKeywords(sample.getDimensionItemKeywords());
    return object;
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
   * This method links existing associations between objects. This is mainly needed in cases where
   * attributes need to be programmatically associated to fulfill client requirements.
   *
   * @param eventAnalyticalObject the source object
   * @param dimensionalObject where the associations will happen
   * @param parent the parent attribute, where the association object should be appended to
   * @return the dimensional object containing the correct associations.
   */
  public static DimensionalObject linkAssociations(
      EventAnalyticalObject eventAnalyticalObject,
      DimensionalObject dimensionalObject,
      Attribute parent) {
    List<EventRepetition> repetitions = eventAnalyticalObject.getEventRepetitions();

    if (isNotEmpty(repetitions)) {
      for (EventRepetition eventRepetition : repetitions) {
        String dimension = dimensionalObject.getDimension();
        String qualifiedDim = getQualifiedDimension(dimensionalObject);

        if (isNotBlank(dimension)) {
          associateEventRepetitionDimensions(
              eventRepetition,
              dimensionalObject.getProgram(),
              dimensionalObject.getProgramStage(),
              parent,
              dimension);

          boolean associationFound =
              qualifiedDim.equals(eventRepetition.qualifiedDimension())
                  || dimension.equals(eventRepetition.qualifiedDimension());

          if (associationFound) {
            dimensionalObject.setEventRepetition(eventRepetition);
          }
        }
      }
    }

    return dimensionalObject;
  }

  /**
   * This method will split the given dimension into individual "uid" and create each object {@link
   * Triple} based on their respective "uid".
   *
   * @param qualifiedDimension the dimension. It can be a simple uid like "dimUid", or a qualified
   *     value like "programUid.stageUid.dimUid".
   * @return a {@link Triple} of {@link Program}, {@link ProgramStage} and {@link
   *     BaseDimensionalObject}.
   */
  public static Triple<Program, ProgramStage, DimensionalObject> asBaseObjects(
      String qualifiedDimension) {
    String[] uids = qualifiedDimension.split("\\.");
    BaseDimensionalObject dimensionalObject = new BaseDimensionalObject();

    if (uids.length == 1) {
      dimensionalObject.setUid(qualifiedDimension);
      return Triple.of(null, null, dimensionalObject);
    } else if (uids.length == 2) {
      dimensionalObject.setUid(uids[1]);

      Program p = new Program();
      p.setUid(uids[0]);

      return Triple.of(p, null, dimensionalObject);
    } else if (uids.length == 3) {
      dimensionalObject.setUid(uids[2]);

      Program p = new Program();
      p.setUid(uids[0]);

      ProgramStage ps = new ProgramStage();
      ps.setUid(uids[1]);

      return Triple.of(p, ps, dimensionalObject);
    }

    return Triple.of(null, null, null);
  }

  /**
   * This method will associate the given objects with the given event repetition, populating the
   * respective objects in the event repetition.
   *
   * @param eventRepetition the {@link EventRepetition}.
   * @param program the {@link Program} to be associated.
   * @param programStage the {@link ProgramStage} to be associated.
   * @param parent the parent {@link Attribute}
   * @param dimension the dimension to be associated.
   */
  private static void associateEventRepetitionDimensions(
      EventRepetition eventRepetition,
      Program program,
      ProgramStage programStage,
      Attribute parent,
      String dimension) {
    boolean belongsToProgram =
        program != null && program.getUid().equals(eventRepetition.getProgram());
    boolean belongsToProgramStage =
        programStage != null && programStage.getUid().equals(eventRepetition.getProgramStage());
    boolean belongsToDimension =
        dimension != null
            && dimension.equals(eventRepetition.getDimension())
            && parent == eventRepetition.getParent();
    boolean hasNoProgramOrStage = program == null && programStage == null;

    if (belongsToDimension) {
      eventRepetition.setParent(parent);

      if (hasNoProgramOrStage) {
        eventRepetition.setDimension(dimension);
      }

      if (belongsToProgram) {
        eventRepetition.setProgram(program.getUid());
      }

      if (belongsToProgramStage) {
        eventRepetition.setProgramStage(programStage.getUid());
      }
    }
  }

  /**
   * This method will iterate through each {@link DimensionalObject} in the given list and join the
   * "program" + "stage" + "dimension" present in the respective {@link DimensionalObject}. This
   * will result in a list of qualified dimensions in the format:
   * "programUid.stageUid.dimensionUid".
   *
   * @param dimensionalObjects the list of {@link DimensionalObject}.
   * @return the list of qualified dimensions.
   */
  public static List<String> getQualifiedDimensions(List<DimensionalObject> dimensionalObjects) {
    List<String> dims = new ArrayList<>();

    if (dimensionalObjects != null) {
      for (DimensionalObject dimension : dimensionalObjects) {
        dims.add(getQualifiedDimension(dimension));
      }
    }

    return dims;
  }

  /**
   * This method takes a {@link DimensionalObject} and join the "program" + "stage" + "dimension"
   * present in the {@link DimensionalObject}. This will result in a list of qualified dimensions in
   * the format: "programUid.stageUid.dimensionUid" or "programUid.dimensionUid". If there is no
   * program and no stage, a simple dimension is returned, ie: "dimensionUid".
   *
   * @param dimensionalObject the {@link DimensionalObject}.
   * @return the qualified dimension.
   */
  public static String getQualifiedDimension(DimensionalObject dimensionalObject) {
    String programUid =
        dimensionalObject.getProgram() != null ? dimensionalObject.getProgram().getUid() : null;
    String programStageUid = null;

    if (dimensionalObject.hasProgramStage()) {
      programStageUid = dimensionalObject.getProgramStage().getUid();

      if (dimensionalObject.getProgramStage().getProgram() != null) {
        programUid = dimensionalObject.getProgramStage().getProgram().getUid();
      }
    }

    return asQualifiedDimension(dimensionalObject.getDimension(), programUid, programStageUid);
  }

  /**
   * Based on the given input dimensions, this method will simply join them together using "." as
   * separator. ie: "programUid.programStageUid.dimensionUid" or "programUid.dimensionUid". If there
   * is no program and no stage, a simple dimension is returned, ie: "dimensionUid".
   *
   * @param dimension the representation of a dimension uid.
   * @param program the representation of a program uid.
   * @param programStage the representation of a program stage uid.
   * @return the qualified dimension.
   */
  public static String asQualifiedDimension(String dimension, String program, String programStage) {
    return Stream.of(program, programStage, dimension)
        .filter(StringUtils::isNotBlank)
        .collect(joining(COMPOSITE_DIM_OBJECT_PLAIN_SEP));
  }

  /**
   * Simply removes any prefix from the qualified dimension, and returns only the actual dimension,
   * represented by the last "dimensionUid" of the argument.
   *
   * @param qualifiedDimension the full dimension, ie. "programUid.programStageUid.dimensionUid"
   * @return the uid of the actual dimension, ie: "dimensionUid", or itself if the given argument is
   *     blank/null/empty.
   */
  public static String asActualDimension(String qualifiedDimension) {
    return defaultIfBlank(
        substringAfterLast(qualifiedDimension, COMPOSITE_DIM_OBJECT_PLAIN_SEP), qualifiedDimension);
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
      // Extracts dimension items by removing dimension name and separator.
      String dimensionItems = param.substring(param.indexOf(DIMENSION_NAME_SEP) + 1);

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
   * Retrieves the uid from the given org. unit group parameter.
   *
   * @param ouGroupParam the org. unit group param in the format OU_GROUP-<uid>
   * @return the uid of the param
   */
  public static String getUidFromGroupParam(String ouGroupParam) {
    return getValueFromKeywordParam(ouGroupParam);
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

    DimensionalObject dim = dimension;

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
  public static String getFirstIdentifier(String compositeItem) {
    if (compositeItem == null) {
      return null;
    }

    Matcher matcher = COMPOSITE_DIM_OBJECT_PATTERN.matcher(compositeItem);
    return matcher.matches() ? matcher.group("id1") : null;
  }

  /**
   * Returns the second identifier in a composite dimension object identifier.
   *
   * @param compositeItem the composite dimension object identifier.
   * @return the second identifier, or null if thr composite identifier is not valid or do not
   *     match.
   */
  public static String getSecondIdentifier(String compositeItem) {
    if (compositeItem == null) {
      return null;
    }

    Matcher matcher = COMPOSITE_DIM_OBJECT_PATTERN.matcher(compositeItem);
    return matcher.matches() ? matcher.group("id2") : null;
  }

  /**
   * Returns the third identifier in a composite dimension object identifier.
   *
   * @param compositeItem the composite dimension object identifier.
   * @return the third identifier, or null if thr composite identifier is not valid or do not match.
   */
  public static String getThirdIdentifier(String compositeItem) {
    if (compositeItem == null) {
      return null;
    }

    Matcher matcher = COMPOSITE_DIM_OBJECT_PATTERN.matcher(compositeItem);

    return matcher.matches() ? matcher.group("id3") : null;
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
        .collect(joining(NAME_SEP))
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
    return objects.stream().map(DimensionalItemObject::getShortName).collect(joining(COL_SEP));
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
