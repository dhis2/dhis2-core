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
package org.hisp.dhis.analytics.data;

import static org.hisp.dhis.analytics.DataQueryParams.LEVEL_PREFIX;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.util.DateUtils.getEarliest;
import static org.hisp.dhis.util.DateUtils.getLatest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryGroups;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.QueryPlannerParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.analytics.util.PeriodOffsetUtils;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.commons.collection.PaginatedList;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Component("org.hisp.dhis.analytics.QueryPlanner")
@RequiredArgsConstructor
public class DefaultQueryPlanner implements QueryPlanner {
  private final PartitionManager partitionManager;

  // -------------------------------------------------------------------------
  // QueryPlanner implementation
  // -------------------------------------------------------------------------

  @Override
  public DataQueryGroups planQuery(DataQueryParams params, QueryPlannerParams plannerParams) {
    params = PeriodOffsetUtils.addShiftedPeriods(params);

    // ---------------------------------------------------------------------
    // Group queries which can be executed together
    // ---------------------------------------------------------------------

    params = withTableNameAndPartitions(params, plannerParams);

    partitionManager.filterNonExistingPartitions(
        params.getPartitions(), plannerParams.getTableName());

    final List<DataQueryParams> queries = Lists.newArrayList(params);

    List<Function<DataQueryParams, List<DataQueryParams>>> groupers =
        new ImmutableList.Builder<Function<DataQueryParams, List<DataQueryParams>>>()
            .add(q -> groupByOrgUnitLevel(q))
            .add(q -> groupByPeriodType(q))
            .add(q -> groupByDataType(q))
            .add(q -> groupByQueryMods(q))
            .add(q -> groupByAggregationType(q))
            .add(q -> groupByDaysInPeriod(q))
            .add(q -> groupByDataPeriodType(q))
            .add(q -> groupByPeriod(q))
            .addAll(plannerParams.getQueryGroupers())
            .build();

    for (Function<DataQueryParams, List<DataQueryParams>> grouper : groupers) {
      List<DataQueryParams> currentQueries = Lists.newArrayList(queries);
      queries.clear();

      currentQueries.forEach(query -> queries.addAll(grouper.apply(query)));
    }

    // ---------------------------------------------------------------------
    // Split queries until optimal number
    // ---------------------------------------------------------------------

    DataQueryGroups queryGroups = DataQueryGroups.newBuilder().withQueries(queries).build();

    if (queryGroups.isOptimal(plannerParams.getOptimalQueries())) {
      return queryGroups;
    }

    List<String> splitDimensions = Lists.newArrayList(DATA_X_DIM_ID, ORGUNIT_DIM_ID);

    for (String dim : splitDimensions) {
      queryGroups = splitByDimension(queryGroups, dim, plannerParams.getOptimalQueries());

      if (queryGroups.isOptimal(plannerParams.getOptimalQueries())) {
        break;
      }
    }

    return queryGroups;
  }

  @Override
  public DataQueryParams withTableNameAndPartitions(
      DataQueryParams params, QueryPlannerParams plannerParams) {
    Partitions partitions = PartitionUtils.getPartitions(params, plannerParams.getTableType());

    if (params.getCurrentUser() != null) {
      partitionManager.filterNonExistingPartitions(partitions, plannerParams.getTableName());
    }

    return DataQueryParams.newBuilder(params)
        .withTableName(plannerParams.getTableName())
        .withPartitions(partitions)
        .build();
  }

  @Override
  public DataQueryParams assignPartitionsFromQueryPeriods(
      DataQueryParams params, AnalyticsTableType tableType) {
    Partitions partitions = PartitionUtils.getPartitions(params, tableType);

    if (params.getTableName() != null) {
      partitionManager.filterNonExistingPartitions(partitions, params.getTableName());
    }

    return DataQueryParams.newBuilder(params).withPartitions(partitions).build();
  }

  // -------------------------------------------------------------------------
  // Supportive split methods
  // -------------------------------------------------------------------------

  /**
   * Splits the given list of queries in sub queries on the given dimension.
   *
   * @param queryGroups {@link {@link DataQueryGroups}.
   * @param dimension the dimension identifier.
   * @param optimalQueries the number of optimal queries.
   * @return a {@link DataQueryGroups}.
   */
  private DataQueryGroups splitByDimension(
      DataQueryGroups queryGroups, String dimension, int optimalQueries) {
    int optimalForSubQuery =
        MathUtils.divideToFloor(optimalQueries, queryGroups.getLargestGroupSize());

    List<DataQueryParams> subQueries = new ArrayList<>();

    for (DataQueryParams query : queryGroups.getAllQueries()) {
      DimensionalObject dim = query.getDimension(dimension);

      List<DimensionalItemObject> values;

      if (dim == null || (values = dim.getItems()) == null || values.isEmpty()) {
        subQueries.add(DataQueryParams.newBuilder(query).build());
        continue;
      }

      List<List<DimensionalItemObject>> valuePages =
          new PaginatedList<>(values).setNumberOfPages(optimalForSubQuery).getPages();

      for (List<DimensionalItemObject> valuePage : valuePages) {
        DataQueryParams subQuery =
            DataQueryParams.newBuilder(query)
                .withDimensionOptions(dim.getDimension(), valuePage)
                .build();

        subQueries.add(subQuery);
      }
    }

    if (subQueries.size() > queryGroups.getAllQueries().size()) {
      log.debug(
          String.format(
              "Split on dimension %s: %d",
              dimension, (subQueries.size() / queryGroups.getAllQueries().size())));
    }

    return DataQueryGroups.newBuilder().withQueries(subQueries).build();
  }

  // -------------------------------------------------------------------------
  // Supportive group by methods
  // -------------------------------------------------------------------------

  /**
   * If periods appear as dimensions in the given query, groups the query into sub queries based on
   * the period type of the periods. Sets the period type name on each query. If periods appear as
   * filters, replaces the period filter with one filter for each period type. Sets the dimension
   * names and filter names respectively.
   *
   * @param params the {@link DataQueryParams} object.
   * @return a list of {@link DataQueryParams}.
   */
  @Override
  public List<DataQueryParams> groupByPeriodType(DataQueryParams params) {
    List<DataQueryParams> queries = new ArrayList<>();

    if (params.isSkipPartitioning()) {
      queries.add(params);
    } else if (!params.getPeriods().isEmpty()) {
      ListMap<String, DimensionalItemObject> periodTypePeriodMap =
          PartitionUtils.getPeriodTypePeriodMap(params.getPeriods());

      for (String periodType : periodTypePeriodMap.keySet()) {
        DataQueryParams query =
            DataQueryParams.newBuilder(params)
                .addOrSetDimensionOptions(
                    PERIOD_DIM_ID,
                    DimensionType.PERIOD,
                    periodType.toLowerCase(),
                    periodTypePeriodMap.get(periodType))
                .withPeriodType(periodType)
                .build();

        queries.add(query);
      }
    } else if (!params.getFilterPeriods().isEmpty()) {
      DimensionalObject filter = params.getFilter(PERIOD_DIM_ID);

      ListMap<String, DimensionalItemObject> periodTypePeriodMap =
          PartitionUtils.getPeriodTypePeriodMap(filter.getItems());

      // Use first period type
      DataQueryParams.Builder query =
          DataQueryParams.newBuilder(params)
              .removeFilter(PERIOD_DIM_ID)
              .withPeriodType(periodTypePeriodMap.keySet().iterator().next());

      for (String periodType : periodTypePeriodMap.keySet()) {
        query.addFilter(
            new BaseDimensionalObject(
                filter.getDimension(),
                filter.getDimensionType(),
                periodType.toLowerCase(),
                filter.getDimensionDisplayName(),
                periodTypePeriodMap.get(periodType)));
      }

      queries.add(query.build());
    } else {
      queries.add(DataQueryParams.newBuilder(params).build());
      return queries;
    }

    logQuerySplit(queries, "period type");

    return queries;
  }

  @Override
  public List<DataQueryParams> groupByOrgUnitLevel(DataQueryParams params) {
    List<DataQueryParams> queries = new ArrayList<>();

    if (!params.getOrganisationUnits().isEmpty()) {
      ListMap<Integer, DimensionalItemObject> levelOrgUnitMap =
          QueryPlannerUtils.getLevelOrgUnitMap(params.getOrganisationUnits());

      for (Integer level : levelOrgUnitMap.keySet()) {
        DataQueryParams query =
            DataQueryParams.newBuilder(params)
                .addOrSetDimensionOptions(
                    ORGUNIT_DIM_ID,
                    DimensionType.ORGANISATION_UNIT,
                    LEVEL_PREFIX + level,
                    levelOrgUnitMap.get(level))
                .build();

        queries.add(query);
      }
    } else if (!params.getFilterOrganisationUnits().isEmpty()) {
      ListMap<Integer, DimensionalItemObject> levelOrgUnitMap =
          QueryPlannerUtils.getLevelOrgUnitMap(params.getFilterOrganisationUnits());

      DimensionalObject filter = params.getFilter(ORGUNIT_DIM_ID);

      DataQueryParams.Builder query =
          DataQueryParams.newBuilder(params).removeFilter(ORGUNIT_DIM_ID);

      for (Integer level : levelOrgUnitMap.keySet()) {
        query.addFilter(
            new BaseDimensionalObject(
                filter.getDimension(),
                filter.getDimensionType(),
                LEVEL_PREFIX + level,
                filter.getDimensionDisplayName(),
                levelOrgUnitMap.get(level)));
      }

      queries.add(query.build());
    } else {
      queries.add(DataQueryParams.newBuilder(params).build());
      return queries;
    }

    logQuerySplit(queries, "organisation unit level");

    return queries;
  }

  @Override
  public List<DataQueryParams> groupByStartEndDateRestriction(DataQueryParams params) {
    List<DataQueryParams> queries = new ArrayList<>();

    if (!params.getPeriods().isEmpty()) {
      for (DimensionalItemObject item : params.getPeriods()) {
        Period period = (Period) item;

        DataQueryParams query =
            DataQueryParams.newBuilder(params)
                .withStartDateRestriction(period.getStartDate())
                .withEndDateRestriction(period.getEndDate())
                .build();

        BaseDimensionalObject staticPeriod =
            (BaseDimensionalObject) query.getDimension(PERIOD_DIM_ID);
        staticPeriod.setDimensionName(period.getIsoDate());
        staticPeriod.setFixed(true);

        queries.add(query);
      }
    } else if (!params.getFilterPeriods().isEmpty()) {
      Period period = (Period) params.getFilterPeriods().get(0);

      DataQueryParams query =
          DataQueryParams.newBuilder(params)
              .withStartDateRestriction(period.getStartDate())
              .withEndDateRestriction(period.getEndDate())
              .removeFilter(PERIOD_DIM_ID)
              .build();

      queries.add(query);
    } else {
      throwIllegalQueryEx(ErrorCode.E7104);
    }

    logQuerySplit(queries, "period start and end date");

    return queries;
  }

  /**
   * Groups queries by their data type.
   *
   * @param params the {@link DataQueryParams}.
   * @return a list of {@link DataQueryParams}.
   */
  private List<DataQueryParams> groupByDataType(DataQueryParams params) {
    List<DataQueryParams> queries = new ArrayList<>();

    if (!params.getDataElements().isEmpty()) {
      ListMap<DataType, DimensionalItemObject> dataTypeDataElementMap =
          QueryPlannerUtils.getDataTypeDataElementMap(params.getDataElements());

      for (DataType dataType : dataTypeDataElementMap.keySet()) {
        DataQueryParams query =
            DataQueryParams.newBuilder(params)
                .withDataElements(dataTypeDataElementMap.get(dataType))
                .withDataType(dataType)
                .build();

        queries.add(query);
      }
    } else {
      DataQueryParams query =
          DataQueryParams.newBuilder(params).withDataType(DataType.NUMERIC).build();

      queries.add(query);
    }

    logQuerySplit(queries, "data type");

    return queries;
  }

  /**
   * Groups queries by their query modifiers.
   *
   * @param params the {@link DataQueryParams}.
   * @return a list of {@link DataQueryParams}.
   */
  private List<DataQueryParams> groupByQueryMods(DataQueryParams params) {
    List<DataQueryParams> queries = new ArrayList<>();

    if (!params.getDataElements().isEmpty()) {
      ListMap<QueryModifiers, DimensionalItemObject> queryModsElementMap =
          QueryPlannerUtils.getQueryModsElementMap(params.getDataElements());

      for (QueryModifiers queryMods : queryModsElementMap.keySet()) {
        DataElement dataElement =
            (DataElement) queryModsElementMap.get(queryMods).iterator().next();

        DataQueryParams query =
            DataQueryParams.newBuilder(params)
                .withDataElements(queryModsElementMap.get(queryMods))
                .withQueryModsId(queryMods.getQueryModsId())
                .withStartDate(getLatest(params.getStartDate(), queryMods.getMinDate()))
                .withEndDate(getEarliest(params.getEndDate(), queryMods.getMaxDate()))
                .withValueColumn(dataElement.getValueColumn())
                .withAggregationType(
                    (queryMods.getAggregationType() != null)
                        ? AnalyticsAggregationType.fromAggregationType(
                            queryMods.getAggregationType())
                        : params.getAggregationType())
                .build();

        queries.add(query);
      }
    } else {
      queries.add(params);
    }

    logQuerySplit(queries, "minDate/maxDate query modifiers");

    return queries;
  }

  /**
   * Groups the given query in sub queries based on the aggregation type of its data elements. The
   * aggregation type can be sum, average aggregation or average disaggregation. Sum means that the
   * data elements have sum aggregation operator. Average aggregation means that the data elements
   * have the average aggregation operator and that the period type of the data elements have higher
   * or equal frequency than the aggregation period type. Average disaggregation means that the data
   * elements have the average aggregation operator and that the period type of the data elements
   * have lower frequency than the aggregation period type. Average bool means that the data
   * elements have the average aggregation operator and the bool value type.
   *
   * <p>If no data elements are present, the aggregation type will be determined based on the first
   * data element in the first data element group in the first data element group set in the query.
   *
   * <p>If the aggregation type is already set/overridden in the request, the query will be returned
   * unchanged. If there are no data elements or data element group sets specified the aggregation
   * type will fall back to sum.
   *
   * @param params the {@link DataQueryParams}.
   * @return a list of {@link DataQueryParams}.
   */
  private List<DataQueryParams> groupByAggregationType(DataQueryParams params) {
    List<DataQueryParams> queries = new ArrayList<>();

    if (!params.getDataElements().isEmpty()) {
      ListMap<AnalyticsAggregationType, DimensionalItemObject> aggregationTypeDataElementMap =
          QueryPlannerUtils.getAggregationTypeDataElementMap(
              params.getDataElements(), params.getAggregationType(), params.getPeriodType());

      for (AnalyticsAggregationType aggregationType : aggregationTypeDataElementMap.keySet()) {
        DataQueryParams query =
            DataQueryParams.newBuilder(params)
                .withDataElements(aggregationTypeDataElementMap.get(aggregationType))
                .withAggregationType(aggregationType)
                .build();

        queries.add(query);
      }
    } else if (!params.getDataElementGroupSets().isEmpty()) {
      DataElementGroup deg = params.getFirstDataElementGroup();

      AnalyticsAggregationType aggregationType =
          ObjectUtils.firstNonNull(params.getAggregationType(), AnalyticsAggregationType.SUM);

      if (deg != null && !deg.getMembers().isEmpty()) {
        PeriodType periodType = PeriodType.getPeriodTypeByName(params.getPeriodType());
        AnalyticsAggregationType degAggType =
            AnalyticsAggregationType.fromAggregationType(deg.getAggregationType());

        aggregationType =
            ObjectUtils.firstNonNull(
                params.getAggregationType(), degAggType, AnalyticsAggregationType.SUM);
        aggregationType =
            QueryPlannerUtils.getAggregationType(
                aggregationType, deg.getValueType(), periodType, deg.getPeriodType());
      }

      DataQueryParams query =
          DataQueryParams.newBuilder(params).withAggregationType(aggregationType).build();

      queries.add(query);
    } else if (filterHasDataElementsOfSameAggregationTypeAndValueType(params)) {
      ListMap<AnalyticsAggregationType, DimensionalItemObject> aggregationTypeDataElementMap =
          QueryPlannerUtils.getAggregationTypeDataElementMap(
              params.getFilterOptions(DATA_X_DIM_ID, DataDimensionItemType.DATA_ELEMENT),
              params.getAggregationType(),
              params.getPeriodType());

      for (AnalyticsAggregationType aggregationType : aggregationTypeDataElementMap.keySet()) {
        DataQueryParams query =
            DataQueryParams.newBuilder(params).withAggregationType(aggregationType).build();

        queries.add(query);
      }
    } else {
      DataQueryParams query =
          DataQueryParams.newBuilder(params)
              .withAggregationType(
                  ObjectUtils.firstNonNull(
                      params.getAggregationType(), AnalyticsAggregationType.SUM))
              .build();

      queries.add(query);
    }

    logQuerySplit(queries, "aggregation type");

    return queries;
  }

  /**
   * Check if the filter of this {@link DataQueryParams} contains Data Elements having the same
   * Aggregation Type and the same Value Type. If the DataQueryParams has the aggregationType set,
   * then the DataQueryParams aggregationType overrides all the Data Elements' aggregation types.
   *
   * @param params DataQueryParams object.
   * @return true if filter has data elements of same aggregation type and value.
   */
  private boolean filterHasDataElementsOfSameAggregationTypeAndValueType(DataQueryParams params) {
    List<DimensionalItemObject> filterDataElements =
        params.getFilterOptions(DATA_X_DIM_ID, DataDimensionItemType.DATA_ELEMENT);

    if (filterDataElements.size() >= 1) {
      DataElement firstDataElement = (DataElement) filterDataElements.get(0);
      return (params.getAggregationType() != null
              || filterDataElements.stream()
                  .allMatch(
                      de ->
                          de.getAggregationType()
                              .name()
                              .equals(firstDataElement.getAggregationType().name())))
          && filterDataElements.stream()
              .allMatch(
                  de ->
                      ((DataElement) de)
                          .getValueType()
                          .name()
                          .equals(firstDataElement.getValueType().name()));
    } else {
      return false;
    }
  }

  /**
   * Groups the given query in sub queries based on the number of days in the aggregation period.
   * This only applies if the aggregation type is SUM, the period dimension aggregation type is
   * AVERAGE, the data type is NUMERIC and the query has at least one period as dimension option.
   * This is necessary since the number of days in the aggregation period is part of the expression
   * for aggregating the value.
   *
   * @param params the {@link DataQueryParams}.
   * @return a list of {@link DataQueryParams}.
   */
  private List<DataQueryParams> groupByDaysInPeriod(DataQueryParams params) {
    List<DataQueryParams> queries = new ArrayList<>();

    AnalyticsAggregationType type = params.getAggregationType();

    boolean sumAvgNumeric =
        AggregationType.SUM == type.getAggregationType()
            && AggregationType.AVERAGE == type.getPeriodAggregationType()
            && DataType.NUMERIC == type.getDataType();

    if (params.getPeriods().isEmpty() || !sumAvgNumeric) {
      queries.add(DataQueryParams.newBuilder(params).build());
      return queries;
    }

    ListMap<Integer, DimensionalItemObject> daysPeriodMap =
        QueryPlannerUtils.getDaysPeriodMap(params.getPeriods());

    DimensionalObject periodDim = params.getDimension(PERIOD_DIM_ID);

    for (Integer days : daysPeriodMap.keySet()) {
      DataQueryParams query =
          DataQueryParams.newBuilder(params)
              .addOrSetDimensionOptions(
                  periodDim.getDimension(),
                  periodDim.getDimensionType(),
                  periodDim.getDimensionName(),
                  daysPeriodMap.get(days))
              .build();

      queries.add(query);
    }

    logQuerySplit(queries, "days in period");

    return queries;
  }

  /**
   * Groups the given query in sub queries based on the period type of its data elements. Sets the
   * data period type on each query. This only applies if the aggregation type of the query involves
   * disaggregation.
   *
   * @param params the {@link DataQueryParams}.
   * @return a list of {@link DataQueryParams}.
   */
  private List<DataQueryParams> groupByDataPeriodType(DataQueryParams params) {
    List<DataQueryParams> queries = new ArrayList<>();

    if (params.isDisaggregation() && !params.getDataElements().isEmpty()) {
      ListMap<PeriodType, DimensionalItemObject> periodTypeDataElementMap =
          QueryPlannerUtils.getPeriodTypeDataElementMap(params.getDataElements());

      for (PeriodType periodType : periodTypeDataElementMap.keySet()) {
        DataQueryParams query =
            DataQueryParams.newBuilder(params)
                .withDataElements(periodTypeDataElementMap.get(periodType))
                .withDataPeriodType(periodType)
                .build();

        queries.add(query);
      }
    } else if (params.isDisaggregation() && !params.getDataElementGroupSets().isEmpty()) {
      DataElementGroup deg = params.getFirstDataElementGroup();
      PeriodType periodType = deg != null ? deg.getPeriodType() : null;

      queries.add(DataQueryParams.newBuilder(params).withDataPeriodType(periodType).build());
    } else {
      queries.add(DataQueryParams.newBuilder(params).build());
    }

    logQuerySplit(queries, "data period type");

    return queries;
  }

  /**
   * Groups the given query in sub queries for each dimension period. This only applies if the
   * aggregation type is {@link AggregationType#LAST} or {@link
   * AggregationType#LAST_AVERAGE_ORG_UNIT}. In this case, each period must be aggregated
   * individually.
   *
   * @param params the {@link DataQueryParams}.
   * @return a list of {@link DataQueryParams}.
   */
  private List<DataQueryParams> groupByPeriod(DataQueryParams params) {
    List<DataQueryParams> queries = new ArrayList<>();

    if (params.isFirstOrLastOrLastInPeriodAggregationType() && !params.getPeriods().isEmpty()) {
      for (DimensionalItemObject period : params.getPeriods()) {
        String periodType = ((Period) period).getPeriodType().getName().toLowerCase();

        DataQueryParams query =
            DataQueryParams.newBuilder(params)
                .withPeriods(Lists.newArrayList(period), periodType)
                .build();

        queries.add(query);
      }
    } else {
      queries.add(params);
    }

    logQuerySplit(queries, "period");

    return queries;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Log query split operation.
   *
   * @param queries the list of queries.
   * @param splitCriteria the name of the query split criteria.
   */
  private void logQuerySplit(List<DataQueryParams> queries, String splitCriteria) {
    if (queries.size() > 1) {
      log.debug(String.format("Split on '%s': %d", splitCriteria, queries.size()));
    }
  }
}
