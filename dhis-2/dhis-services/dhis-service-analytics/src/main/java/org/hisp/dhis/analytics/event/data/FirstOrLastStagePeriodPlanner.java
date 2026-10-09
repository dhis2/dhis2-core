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

import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.EVENT;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.stage.StageQueryItemClassifier;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.period.PeriodDimension;
import org.springframework.stereotype.Component;

/**
 * Plans first/last value queries over stage date periods, such as {@code
 * stageUid.EVENT_DATE:202601;202602}. A stage date dimension gets a separate query for each period.
 * A stage date filter gets one query ending at its latest period, since first/last values look back
 * from the period end and do not use the period start. Each planned query carries its period as a
 * {@link EventQueryParams.FirstOrLastStagePeriod}.
 */
@Component
@RequiredArgsConstructor
public class FirstOrLastStagePeriodPlanner {
  private final StageQueryItemClassifier stageQueryItemClassifier;

  /**
   * Returns one query per period of a stage date dimension, one query for the latest period of a
   * stage date filter, or the given query when it is not a first/last value query over stage date
   * periods.
   *
   * @param params the event query parameters.
   * @return a list of {@link EventQueryParams}.
   */
  public List<EventQueryParams> plan(EventQueryParams params) {
    if (!isFirstOrLastEventValueQuery(params)) {
      return List.of(params);
    }

    Optional<QueryItem> dimension = findStageDateItem(params, params.getItems());
    if (dimension.isPresent()) {
      return planEachPeriod(params, dimension.get());
    }

    return findStageDateItem(params, params.getItemFilters())
        .map(filter -> List.of(planLatestPeriod(params, filter)))
        .orElse(List.of(params));
  }

  private boolean isFirstOrLastEventValueQuery(EventQueryParams params) {
    return params.getEndpointItem() == EVENT
        && params.hasValueDimension()
        && params.isFirstOrLastPeriodAggregationType();
  }

  private Optional<QueryItem> findStageDateItem(
      EventQueryParams params, List<QueryItem> candidates) {
    return candidates.stream()
        .filter(stageQueryItemClassifier::isStageDate)
        .filter(item -> !item.getDimensionValues().isEmpty())
        .filter(item -> isInRequestedStage(params, item))
        .findFirst();
  }

  private boolean isInRequestedStage(EventQueryParams params, QueryItem item) {
    return !params.hasProgramStage() || params.getProgramStage().equals(item.getProgramStage());
  }

  private List<EventQueryParams> planEachPeriod(EventQueryParams params, QueryItem item) {
    return item.getDimensionValues().stream()
        .distinct()
        .map(PeriodDimension::of)
        .map(period -> planPeriod(params, item, period))
        .toList();
  }

  private EventQueryParams planLatestPeriod(EventQueryParams params, QueryItem item) {
    PeriodDimension latest =
        item.getDimensionValues().stream()
            .map(PeriodDimension::of)
            .max(Comparator.comparing(PeriodDimension::getEndDate))
            .orElseThrow();
    return planPeriod(params, item, latest);
  }

  private EventQueryParams planPeriod(
      EventQueryParams params, QueryItem item, PeriodDimension period) {
    EventQueryParams.Builder builder =
        new EventQueryParams.Builder(params).withFirstOrLastStagePeriod(item, period);
    builder.withSkipPartitioning(true);
    return builder.build();
  }
}
