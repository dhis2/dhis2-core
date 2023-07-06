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
package org.hisp.dhis.datasetreport.jdbc;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import java.util.*;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.datasetreport.DataSetReportStore;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.filter.AggregatableDataElementFilter;
import org.springframework.stereotype.Repository;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Repository("org.hisp.dhis.datasetreport.DataSetReportStore")
public class AnalyticsDataSetReportStore implements DataSetReportStore {
  private final DataQueryService dataQueryService;

  private final AnalyticsService analyticsService;

  public AnalyticsDataSetReportStore(
      DataQueryService dataQueryService, AnalyticsService analyticsService) {
    checkNotNull(dataQueryService);
    checkNotNull(analyticsService);
    this.dataQueryService = dataQueryService;
    this.analyticsService = analyticsService;
  }

  // -------------------------------------------------------------------------
  // DataSetReportStore implementation
  // -------------------------------------------------------------------------

  @Override
  public Map<String, Object> getAggregatedValues(
      DataSet dataSet, List<Period> periods, OrganisationUnit unit, Set<String> filters) {
    List<DataElement> dataElements = new ArrayList<>(dataSet.getDataElements());

    FilterUtils.filter(dataElements, AggregatableDataElementFilter.INSTANCE);

    if (dataElements.isEmpty()) {
      return new HashMap<>();
    }

    DataQueryParams.Builder params =
        DataQueryParams.newBuilder()
            .withDataElements(dataElements)
            .withPeriods(periods)
            .withOrganisationUnit(unit)
            .withCategoryOptionCombos(Lists.newArrayList());

    if (filters != null) {
      params.addFilters(
          dataQueryService.getDimensionalObjects(filters, null, null, null, false, IdScheme.UID));
    }

    Map<String, Object> map = analyticsService.getAggregatedDataValueMapping(params.build());

    Map<String, Object> dataMap = new HashMap<>();

    for (Entry<String, Object> entry : map.entrySet()) {
      String[] split = entry.getKey().split(SEPARATOR);
      addToMap(dataMap, split[0] + SEPARATOR + split[3], entry.getValue());
    }

    return dataMap;
  }

  @Override
  public Map<String, Object> getAggregatedSubTotals(
      DataSet dataSet, List<Period> periods, OrganisationUnit unit, Set<String> filters) {
    Map<String, Object> dataMap = new HashMap<>();

    for (Section section : dataSet.getSections()) {
      List<DataElement> dataElements = new ArrayList<>(section.getDataElements());
      Set<Category> categories = new HashSet<>();

      for (CategoryCombo categoryCombo : section.getCategoryCombos()) {
        categories.addAll(categoryCombo.getCategories());
      }

      FilterUtils.filter(dataElements, AggregatableDataElementFilter.INSTANCE);

      if (dataElements.isEmpty() || categories == null || categories.isEmpty()) {
        continue;
      }

      for (Category category : categories) {
        if (category.isDefault()) {
          continue; // No need for sub-total for default
        }

        if (!category.isDataDimension()) {
          log.warn(
              "Could not get sub-total for category: "
                  + category.getUid()
                  + " for data set report: "
                  + dataSet
                  + ", not a data dimension");
          continue;
        }

        DataQueryParams.Builder params =
            DataQueryParams.newBuilder()
                .withDataElements(dataElements)
                .withPeriods(periods)
                .withOrganisationUnit(unit)
                .withCategory(category);

        if (filters != null) {
          params.addFilters(
              dataQueryService.getDimensionalObjects(
                  filters, null, null, null, false, IdScheme.UID));
        }

        Map<String, Object> map = analyticsService.getAggregatedDataValueMapping(params.build());

        for (Entry<String, Object> entry : map.entrySet()) {
          String[] split = entry.getKey().split(SEPARATOR);
          addToMap(dataMap, split[0] + SEPARATOR + split[3], entry.getValue());
        }
      }
    }

    return dataMap;
  }

  @Override
  public Map<String, Object> getAggregatedTotals(
      DataSet dataSet, List<Period> periods, OrganisationUnit unit, Set<String> filters) {
    List<DataElement> dataElements = new ArrayList<>(dataSet.getDataElements());

    FilterUtils.filter(dataElements, AggregatableDataElementFilter.INSTANCE);

    if (dataElements.isEmpty()) {
      return new HashMap<>();
    }

    DataQueryParams.Builder params =
        DataQueryParams.newBuilder()
            .withDataElements(dataElements)
            .withPeriods(periods)
            .withOrganisationUnit(unit);

    if (filters != null) {
      params.addFilters(
          dataQueryService.getDimensionalObjects(filters, null, null, null, false, IdScheme.UID));
    }

    Map<String, Object> map = analyticsService.getAggregatedDataValueMapping(params.build());

    Map<String, Object> dataMap = new HashMap<>();

    for (Entry<String, Object> entry : map.entrySet()) {
      String[] split = entry.getKey().split(SEPARATOR);
      addToMap(dataMap, split[0], entry.getValue());
    }

    return dataMap;
  }

  @Override
  public Map<String, Object> getAggregatedIndicatorValues(
      DataSet dataSet, List<Period> periods, OrganisationUnit unit, Set<String> filters) {
    List<Indicator> indicators = new ArrayList<>(dataSet.getIndicators());

    if (indicators.isEmpty()) {
      return new HashMap<>();
    }

    DataQueryParams.Builder params =
        DataQueryParams.newBuilder()
            .withIndicators(indicators)
            .withPeriods(periods)
            .withOrganisationUnit(unit);

    if (filters != null) {
      params.addFilters(
          dataQueryService.getDimensionalObjects(filters, null, null, null, false, IdScheme.UID));
    }

    Map<String, Object> map = analyticsService.getAggregatedDataValueMapping(params.build());

    Map<String, Object> dataMap = new HashMap<>();

    for (Entry<String, Object> entry : map.entrySet()) {
      String[] split = entry.getKey().split(SEPARATOR);
      addToMap(dataMap, split[0], entry.getValue());
      dataMap.put(split[0], entry.getValue());
    }

    return dataMap;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * If values are numeric, sum the values in the map for the same key. If values are non-numeric,
   * add the value to the map. Ignore nulls.
   */
  private void addToMap(Map<String, Object> dataMap, String key, Object value) {
    if (value != null) {
      dataMap.compute(
          key,
          (k, v) ->
              (!(v instanceof Double) || !(value instanceof Double)
                  ? value
                  : (Double) v + (Double) value));
    }
  }
}
