/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.data.QueryPlannerUtils;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.queryitem.DateFilterHandler;
import org.hisp.dhis.analytics.event.data.stage.StageQueryItemClassifier;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.period.PeriodDimension;
import org.springframework.stereotype.Component;

/**
 * Plans aggregate queries over stage date dimensions with periods of different types, such as
 * {@code stageUid.EVENT_DATE:2022;2022Q1}. An aggregate query groups a stage date by the period
 * column of a single period type, so each planned query carries the periods of one type, restricted
 * to the dates of those periods. An event that falls in periods of different types is counted in
 * each of them. Stage date dimensions are planned in turn, which yields one query per combination
 * of their period types. Filters are left whole, since they restrict without a group-by column.
 */
@Component
@RequiredArgsConstructor
public class StageDatePeriodTypePlanner {
  private final DateFilterHandler dateFilterHandler = new DateFilterHandler();

  private final StageQueryItemClassifier stageQueryItemClassifier;

  /**
   * Returns one query per combination of the period types of the stage date dimensions, or the
   * given query when every stage date dimension has periods of one type.
   *
   * @param params the event query parameters.
   * @return a list of {@link EventQueryParams}.
   */
  public List<EventQueryParams> plan(EventQueryParams params) {
    List<QueryItem> stageDates =
        params.getItems().stream().filter(stageQueryItemClassifier::isStageDate).toList();

    return QueryPlannerUtils.splitInTurn(params, stageDates, this::splitByPeriodType);
  }

  /**
   * Splits the given query into one query per period type of the given stage date dimension.
   *
   * @param item the stage date dimension.
   * @param params the event query parameters.
   * @return one {@link EventQueryParams} per period type, or the given params when there is at most
   *     one, or when a value of the dimension is not a period.
   */
  private List<EventQueryParams> splitByPeriodType(QueryItem item, EventQueryParams params) {
    Optional<Collection<List<String>>> periodsByType = groupByPeriodType(item.getDimensionValues());

    if (periodsByType.isEmpty() || periodsByType.get().size() <= 1) {
      return List.of(params);
    }

    return periodsByType.get().stream()
        .map(periods -> replaceItem(params, item, restrictToPeriods(item, periods)))
        .toList();
  }

  /**
   * Groups the given ISO periods by period type, keeping their order.
   *
   * @return the periods of each type, or empty when a value is not a period.
   */
  private Optional<Collection<List<String>>> groupByPeriodType(List<String> isoPeriods) {
    List<PeriodDimension> periods = isoPeriods.stream().map(PeriodDimension::of).toList();

    if (periods.stream().anyMatch(Objects::isNull)) {
      return Optional.empty();
    }

    return Optional.of(
        periods.stream()
            .collect(
                groupingBy(
                    period -> period.getPeriodType().getName(),
                    LinkedHashMap::new,
                    mapping(PeriodDimension::getIsoDate, toList())))
            .values());
  }

  /** Returns a copy of the given stage date item restricted to the given ISO periods. */
  private QueryItem restrictToPeriods(QueryItem item, List<String> isoPeriods) {
    QueryItem restricted = item.copy();
    restricted.setFilters(new ArrayList<>());
    restricted.setDimensionValues(new ArrayList<>());
    dateFilterHandler.applyPeriods(restricted, isoPeriods);
    return restricted;
  }

  /** Returns a copy of the given query with the given item replaced, keeping the item order. */
  private EventQueryParams replaceItem(
      EventQueryParams params, QueryItem item, QueryItem replacement) {
    EventQueryParams.Builder builder = new EventQueryParams.Builder(params).removeItems();
    params.getItems().forEach(it -> builder.addItem(it == item ? replacement : it));
    return builder.build();
  }
}
