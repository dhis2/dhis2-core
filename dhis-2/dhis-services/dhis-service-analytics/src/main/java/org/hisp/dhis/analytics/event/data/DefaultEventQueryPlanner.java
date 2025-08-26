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
package org.hisp.dhis.analytics.event.data;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.fromAggregationType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.data.QueryPlannerUtils;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.Partitions;
import org.hisp.dhis.analytics.table.util.PartitionUtils;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Service("org.hisp.dhis.analytics.event.EventQueryPlanner")
@RequiredArgsConstructor
public class DefaultEventQueryPlanner implements EventQueryPlanner {
  private final QueryPlanner queryPlanner;

  private final PartitionManager partitionManager;

  // -------------------------------------------------------------------------
  // EventQueryPlanner implementation
  // -------------------------------------------------------------------------

  @Override
  public List<EventQueryParams> planAggregateQuery(EventQueryParams params) {
    List<EventQueryParams> queries = Lists.newArrayList(params);

    List<Function<EventQueryParams, List<EventQueryParams>>> groupers =
        new ImmutableList.Builder<Function<EventQueryParams, List<EventQueryParams>>>()
            .add(this::groupByQueryItems)
            .add(this::groupByTimeDimensions)
            .add(this::groupByOrgUnitLevel)
            .add(this::groupByPeriodType)
            .add(this::groupByPeriod)
            .build();

    for (Function<EventQueryParams, List<EventQueryParams>> grouper : groupers) {
      List<EventQueryParams> currentQueries = Lists.newArrayList(queries);
      queries.clear();

      currentQueries.forEach(query -> queries.addAll(grouper.apply(query)));
    }

    return withTableNameAndPartitions(queries);
  }

  @Override
  public EventQueryParams planEventQuery(EventQueryParams params) {
    return withTableNameAndPartitions(params);
  }

  @Override
  public EventQueryParams planEnrollmentQuery(EventQueryParams params) {
    return new EventQueryParams.Builder(params)
        .withTableName(
            AnalyticsTable.getTableName(AnalyticsTableType.ENROLLMENT, params.getProgram()))
        .build();
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Sets table name and partitions on the given query.
   *
   * @param params the event query parameters.
   * @return a {@link EventQueryParams}.
   */
  private EventQueryParams withTableNameAndPartitions(EventQueryParams params) {
    Partitions partitions =
        params.hasStartEndDate()
            ? PartitionUtils.getPartitions(params.getStartDate(), params.getEndDate())
            : PartitionUtils.getPartitions(params.getAllPeriods());

    AnalyticsTableType tableType =
        params.hasEnrollmentProgramIndicatorDimension() || params.isAggregatedEnrollments()
            ? AnalyticsTableType.ENROLLMENT
            : AnalyticsTableType.EVENT;

    String tableName = AnalyticsTable.getTableName(tableType, params.getProgram());

    if (params.getCurrentUser() != null) {
      partitionManager.filterNonExistingPartitions(partitions, tableName);
    }

    return new EventQueryParams.Builder(params)
        .withTableName(tableName)
        .withPartitions(partitions)
        .build();
  }

  /**
   * Sets table name and partition on each query in the given list.
   *
   * @param queries the list of queries.
   * @return a list of {@link EventQueryParams}.
   */
  private List<EventQueryParams> withTableNameAndPartitions(List<EventQueryParams> queries) {
    List<EventQueryParams> list = new ArrayList<>();

    boolean isMultipleQueries = queries.size() > 1;

    queries.forEach(
        query ->
            list.add(withMultipleQueries(isMultipleQueries, withTableNameAndPartitions(query))));

    return list;
  }

  /**
   * Sets the "multipleQueries" flag in EventParams and builds it
   *
   * @param isMultipleQueries flag to detect if multiple queries are to be run
   * @param eventQueryParams the eventQueryParams template
   * @return an eventQueryParams with proper "multipleQueries" flag set
   */
  private EventQueryParams withMultipleQueries(
      boolean isMultipleQueries, EventQueryParams eventQueryParams) {
    return new EventQueryParams.Builder(eventQueryParams)
        .withMultipleQueries(isMultipleQueries)
        .build();
  }

  /**
   * Groups by organisation unit level.
   *
   * @param params the event data query parameters.
   * @return a list of {@link EventQueryParams}.
   */
  private List<EventQueryParams> groupByOrgUnitLevel(EventQueryParams params) {
    return QueryPlannerUtils.convert(queryPlanner.groupByOrgUnitLevel(params));
  }

  /**
   * Groups by period types.
   *
   * @param params the event data query parameters.
   * @return a list of {@link EventQueryParams}.
   */
  private List<EventQueryParams> groupByPeriodType(EventQueryParams params) {
    return QueryPlannerUtils.convert(queryPlanner.groupByPeriodType(params));
  }

  /**
   * Groups by query item and set the value property to each item and item filter if exists and
   * query is for aggregate data. Groups by program indicator if exists and query is for aggregate
   * data.
   *
   * <p>Groups by items if query items are to be collapsed in order to aggregate each item
   * individually. Sets program on the given parameters.
   *
   * @param params the event query parameters.
   * @return a list of {@link EventQueryParams}.
   */
  private List<EventQueryParams> groupByQueryItems(EventQueryParams params) {
    List<EventQueryParams> queries = new ArrayList<>();

    if (params.isAggregateData()) {
      for (QueryItem item : params.getItemsAndItemFilters()) {
        AnalyticsAggregationType aggregationType =
            firstNonNull(
                params.getAggregationType(), fromAggregationType(item.getAggregationType()));

        EventQueryParams.Builder query =
            new EventQueryParams.Builder(params)
                .removeItems()
                .removeItemProgramIndicators()
                .withValue(item.getItem())
                .withOption(item.getOption())
                .withAggregationType(aggregationType);

        if (item.hasProgram()) {
          query.withProgram(item.getProgram());
        }

        queries.add(query.build());
      }

      for (ProgramIndicator programIndicator : params.getItemProgramIndicators()) {
        EventQueryParams query =
            new EventQueryParams.Builder(params)
                .removeItems()
                .removeItemProgramIndicators()
                .withProgramIndicator(programIndicator)
                .withProgram(programIndicator.getProgram())
                .withAggregationType(
                    fromAggregationType(programIndicator.getAggregationTypeFallback()))
                .withOrgUnitField(new OrgUnitField(programIndicator.getOrgUnitField()))
                .build();

        queries.add(query);
      }
    } else if (params.isCollapseDataDimensions() && !params.getItems().isEmpty()) {
      for (QueryItem item : params.getItems()) {
        EventQueryParams.Builder query =
            new EventQueryParams.Builder(params).removeItems().addItem(item);

        if (item.hasProgram()) {
          query.withProgram(item.getProgram());
        }

        queries.add(query.build());
      }
    } else {
      queries.add(new EventQueryParams.Builder(params).build());
    }

    return queries;
  }

  /**
   * Groups the given query in sub queries for each dimension period. This applies if the
   * aggregation type is {@link AggregationType#LAST} or {@link
   * AggregationType#LAST_AVERAGE_ORG_UNIT}. It also applies if the query includes a {@link
   * ProgramIndicator} that does not use default analytics period boundaries: {@link
   * EventQueryParams#hasNonDefaultBoundaries()}. In this case, each period must be aggregated
   * individually.
   *
   * @param params the data query parameters.
   * @return a list of {@link EventQueryParams}.
   */
  private List<EventQueryParams> groupByPeriod(EventQueryParams params) {
    List<EventQueryParams> queries = new ArrayList<>();

    boolean isFirstOrLast = params.isFirstOrLastPeriodAggregationType();
    boolean isOwnership = params.getOrgUnitField().getType().isOwnership();
    boolean useIndividual = params.useIndividualQuery();
    boolean hasPeriods = !params.getPeriods().isEmpty();

    if ((isFirstOrLast || isOwnership || useIndividual) && hasPeriods) {
      for (DimensionalItemObject period : params.getPeriods()) {
        String periodType = ((Period) period).getPeriodType().getName().toLowerCase();

        EventQueryParams query =
            new EventQueryParams.Builder(params)
                .withPeriods(Lists.newArrayList(period), periodType)
                .build();

        queries.add(query);
      }
    } else {
      queries.add(params);
    }

    return queries;
  }

  /**
   * Groups queries by active time dimensions and their period types. This method handles:
   *
   * <p>- Multiple time dimensions (eventDate + enrollmentDate) - splits by time dimension
   *
   * <p>- Mixed period types within single time dimension (eventDate=THIS_YEAR;2021S1) - splits by
   * period type
   *
   * @param params the event query parameters.
   * @return a list of {@link EventQueryParams}.
   */
  private List<EventQueryParams> groupByTimeDimensions(EventQueryParams params) {
    List<EventQueryParams> queries = new ArrayList<>();

    if (params.hasMultipleTimeDimensions()) {
      // Multiple time dimensions - split into individual queries
      for (TimeField timeField : params.getActiveTimeDimensions()) {
        EventQueryParams singleTimeDimensionQuery = params.withSingleTimeDimension(timeField);
        queries.add(singleTimeDimensionQuery);
      }
      return queries;
    }

    // Single time dimension - check for mixed period types within date ranges
    if (params.getActiveTimeDimensionCount() == 1) {
      TimeField timeField = params.getActiveTimeDimensions().iterator().next();
      List<org.hisp.dhis.common.DateRange> dateRanges = params.getTimeDateRanges().get(timeField);

      if (dateRanges != null && dateRanges.size() > 1) {
        // Check if date ranges represent different period types by analyzing date patterns
        boolean hasMixedTypes = hasMixedPeriodTypes(dateRanges);

        if (hasMixedTypes) {
          // Split into separate queries for each date range
          for (org.hisp.dhis.common.DateRange dateRange : dateRanges) {
            EventQueryParams splitQuery =
                createQueryForSingleDateRange(params, timeField, dateRange);
            queries.add(splitQuery);
          }
          return queries;
        }
      }
    }

    // No splitting needed - single time dimension with homogeneous period types
    queries.add(params);
    return queries;
  }

  /**
   * Checks if the provided date ranges represent different period types. This is a heuristic based
   * on date range duration and patterns.
   */
  private boolean hasMixedPeriodTypes(List<org.hisp.dhis.common.DateRange> dateRanges) {
    if (dateRanges.size() <= 1) {
      return false;
    }

    // Simple heuristic: if date ranges have significantly different durations,
    // they likely represent different period types
    long firstDuration =
        dateRanges.get(0).getEndDate().getTime() - dateRanges.get(0).getStartDate().getTime();

    for (int i = 1; i < dateRanges.size(); i++) {
      long duration =
          dateRanges.get(i).getEndDate().getTime() - dateRanges.get(i).getStartDate().getTime();
      // If durations differ by more than 50%, consider them different period types
      double ratio = (double) Math.abs(duration - firstDuration) / firstDuration;
      if (ratio > 0.5) {
        return true;
      }
    }

    return false;
  }

  /** Creates a new EventQueryParams with a single date range for the specified time field. */
  private EventQueryParams createQueryForSingleDateRange(
      EventQueryParams params, TimeField timeField, org.hisp.dhis.common.DateRange dateRange) {
    EventQueryParams newParams =
        new EventQueryParams.Builder(params).withTimeField(timeField.name()).build();

    // Set only the single date range
    newParams.getTimeDateRanges().clear();
    newParams.getTimeDateRanges().put(timeField, List.of(dateRange));

    return newParams;
  }
}
