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
package org.hisp.dhis.predictor;

import static org.hisp.dhis.common.DimensionConstants.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;

/**
 * Fetches analytics data for predictions.
 *
 * <p>This class can fetch values from analytics and return them for a list of organisation units.
 *
 * <p>When making disaggregated predictions, program indicators with a non-default category combo
 * are fetched with their category option combos, so their values can be included in the
 * disaggregated predictions according to their category options. These are fetched in data value
 * set format, which returns the category option combo and the attribute option combo (if any) as
 * mapped by the program indicator's category mappings.
 *
 * @author Jim Grace
 */
@RequiredArgsConstructor
public class PredictionAnalyticsDataFetcher {
  private final AnalyticsService analyticsService;

  private final CategoryService categoryService;

  /** Whether we are generating disaggregated predictions. */
  private final boolean disagPredictions;

  private Set<Period> periods;

  private Map<String, Period> periodLookup;

  private Set<DimensionalItemObject> attributeOptionItems;

  private Set<DimensionalItemObject> nonAttributeOptionItems;

  private Set<DimensionalItemObject> categoryOptionItems;

  private Map<String, DimensionalItemObject> analyticsItemsLookup;

  private CachingMap<String, CategoryOptionCombo> cocLookup;

  private Map<String, OrganisationUnit> orgUnitLookup;

  /**
   * Initializes for fetching analytics data.
   *
   * @param periods periods to fetch
   * @param analyticsItems analytics items to fetch
   */
  public void init(Set<Period> periods, Set<DimensionalItemObject> analyticsItems) {
    this.periods = periods;

    categorizeAnalyticsItems(analyticsItems);

    periodLookup = periods.stream().collect(Collectors.toMap(Period::getIsoDate, p -> p));

    analyticsItemsLookup =
        analyticsItems.stream()
            .collect(Collectors.toMap(DimensionalItemObject::getDimensionItem, d -> d));

    cocLookup = new CachingMap<>();
  }

  /**
   * Gets analytics values.
   *
   * @param orgUnits organisation units to get data for
   * @return values as fetched from analytics
   */
  public List<FoundDimensionItemValue> getValues(List<OrganisationUnit> orgUnits) {
    orgUnitLookup = orgUnits.stream().collect(Collectors.toMap(OrganisationUnit::getUid, o -> o));

    List<FoundDimensionItemValue> values = getValuesInternal(orgUnits, attributeOptionItems, true);

    values.addAll(getValuesInternal(orgUnits, nonAttributeOptionItems, false));

    for (DimensionalItemObject item : categoryOptionItems) {
      values.addAll(getCategoryOptionValues(orgUnits, item));
    }

    return values;
  }

  // -------------------------------------------------------------------------
  // Supportive Methods
  // -------------------------------------------------------------------------

  /**
   * Categorizes analytics items according to whether they are fetched with category option combos,
   * or else whether or not they are stored with an attribute option combo.
   */
  private void categorizeAnalyticsItems(Set<DimensionalItemObject> analyticsItems) {
    attributeOptionItems = new HashSet<>();
    nonAttributeOptionItems = new HashSet<>();
    categoryOptionItems = new HashSet<>();

    for (DimensionalItemObject o : analyticsItems) {
      if (disagPredictions && hasCategoryOptions(o)) {
        categoryOptionItems.add(o);
      } else if (hasAttributeOptions(o)) {
        attributeOptionItems.add(o);
      } else {
        nonAttributeOptionItems.add(o);
      }
    }
  }

  /** Checks to see if a dimensional item object is a disaggregated program indicator */
  private boolean hasCategoryOptions(DimensionalItemObject o) {
    return o instanceof ProgramIndicator pi
        && pi.getCategoryCombo() != null
        && !pi.getCategoryCombo().isDefault();
  }

  /**
   * Checks to see if a dimensional item object has values stored in the database by attribute
   * option combo
   */
  private boolean hasAttributeOptions(DimensionalItemObject o) {
    return o.getDimensionItemType() != DimensionItemType.PROGRAM_INDICATOR
        || ((ProgramIndicator) o).getAnalyticsType() != AnalyticsType.ENROLLMENT;
  }

  /** Queries analytics for data. */
  private List<FoundDimensionItemValue> getValuesInternal(
      List<OrganisationUnit> orgUnits,
      Set<DimensionalItemObject> dimensionItems,
      boolean hasAttributeOptions) {
    List<FoundDimensionItemValue> values = new ArrayList<>();

    if (dimensionItems.isEmpty()) {
      return values;
    }

    DataQueryParams.Builder paramsBuilder = getParamsBuilder(orgUnits, dimensionItems);

    if (hasAttributeOptions) {
      paramsBuilder.withAttributeOptionCombos(Collections.emptyList());
    }

    Grid grid = analyticsService.getAggregatedDataValues(paramsBuilder.build());

    int peInx = grid.getIndexOfHeader(PERIOD_DIM_ID);
    int dxInx = grid.getIndexOfHeader(DATA_X_DIM_ID);
    int ouInx = grid.getIndexOfHeader(ORGUNIT_DIM_ID);
    int aoInx = hasAttributeOptions ? grid.getIndexOfHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID) : 0;
    int vlInx = grid.getWidth() - 1;

    for (List<Object> row : grid.getRows()) {
      String pe = (String) row.get(peInx);
      String dx = (String) row.get(dxInx);
      String ou = (String) row.get(ouInx);
      String ao = hasAttributeOptions ? (String) row.get(aoInx) : null;

      Period period = periodLookup.get(pe);
      DimensionalItemObject item = analyticsItemsLookup.get(dx);
      OrganisationUnit orgUnit = orgUnitLookup.get(ou);
      CategoryOptionCombo attributeOptionCombo = lookUpCoc(ao);
      Object value = getDoubleValue(row.get(vlInx));

      values.add(
          new FoundDimensionItemValue(orgUnit, period, null, attributeOptionCombo, item, value));
    }

    return values;
  }

  /**
   * Queries analytics for a disaggregated program indicator's data by category option combo.
   *
   * <p>This is queried in data value set format so analytics will return the category option combo
   * and attribute option combo (if any) of each value. Each item is queried separately because in
   * this format analytics may return the item's aggregate export data element instead of the item
   * itself.
   */
  private List<FoundDimensionItemValue> getCategoryOptionValues(
      List<OrganisationUnit> orgUnits, DimensionalItemObject item) {
    List<FoundDimensionItemValue> values = new ArrayList<>();

    Grid grid =
        analyticsService.getAggregatedDataValuesGrid(
            getParamsBuilder(orgUnits, Set.of(item)).build());

    int peInx = grid.getIndexOfHeader(PERIOD_DIM_ID);
    int ouInx = grid.getIndexOfHeader(ORGUNIT_DIM_ID);
    int coInx = grid.getIndexOfHeader(CATEGORYOPTIONCOMBO_DIM_ID);
    int aoInx = grid.getIndexOfHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID);
    int vlInx = grid.getWidth() - 1;

    for (List<Object> row : grid.getRows()) {
      Period period = periodLookup.get(row.get(peInx));
      OrganisationUnit orgUnit = orgUnitLookup.get(row.get(ouInx));
      CategoryOptionCombo categoryOptionCombo = lookUpCoc((String) row.get(coInx));
      CategoryOptionCombo attributeOptionCombo = lookUpCoc((String) row.get(aoInx));
      Object value = getDoubleValue(row.get(vlInx));

      values.add(
          new FoundDimensionItemValue(
              orgUnit, period, categoryOptionCombo, attributeOptionCombo, item, value));
    }

    return values;
  }

  /** Gets the analytics query parameters builder for some items. */
  private DataQueryParams.Builder getParamsBuilder(
      List<OrganisationUnit> orgUnits, Set<DimensionalItemObject> dimensionItems) {
    return DataQueryParams.newBuilder()
        .withPeriods(periods.stream().map(PeriodDimension::of).toList())
        .withDataDimensionItems(Lists.newArrayList(dimensionItems))
        .withOrganisationUnits(orgUnits);
  }

  /** Finds a category option combo from its UID (if not null), using a local cache. */
  private CategoryOptionCombo lookUpCoc(String uid) {
    return (uid == null)
        ? null
        : cocLookup.get(uid, () -> categoryService.getCategoryOptionCombo(uid));
  }

  /** Converts any non-Double numeric value to Double. */
  private Object getDoubleValue(Object value) {
    return (value instanceof Number n && !(value instanceof Double)) ? n.doubleValue() : value;
  }
}
