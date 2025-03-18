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

import static org.hisp.dhis.analytics.DataQueryParams.VALUE_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.analytics.tracker.HeaderHelper.addCommonHeaders;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.UNLIMITED_PAGING;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.addPaging;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.applyHeaders;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.getDimensionsKeywords;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.commons.util.TextUtils.EMPTY;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.tracker.MetadataItemsHandler;
import org.hisp.dhis.analytics.tracker.SchemeIdHandler;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.util.Timer;
import org.springframework.stereotype.Service;

/** This service is responsible for retrieving aggregated enrollments data. */
@Service
@RequiredArgsConstructor
public class EnrollmentAggregateService {

  private final EnrollmentAnalyticsManager enrollmentAnalyticsManager;

  private final EventQueryPlanner queryPlanner;

  private final AnalyticsSecurityManager securityManager;

  private final EventQueryValidator queryValidator;

  private final MetadataItemsHandler metadataHandler;

  private final SchemeIdHandler schemeIdHandler;

  /**
   * Returns the aggregated data related to enrollments, that matches the given query.
   *
   * @param params the {@link EventQueryParams} parameters.
   * @return enrollments data as a {@link Grid} object.
   */
  public Grid getEnrollments(EventQueryParams params) {
    // Check access/constraints.
    securityManager.decideAccessEventQuery(params);
    params = securityManager.withUserConstraints(params);

    // Validate request.
    queryValidator.validate(params);

    List<Keyword> keywords = getDimensionsKeywords(params);
    List<DimensionalObject> periods = getPeriods(params);

    // Set periods.
    params = new EventQueryParams.Builder(params).withStartEndDatesForPeriods().build();

    // Populate headers.
    Grid grid = createGridWithHeaders();
    addCommonHeaders(grid, params, periods);

    // Add data.
    if (!params.isSkipData() || params.analyzeOnly()) {
      if (!periods.isEmpty()) {
        params =
            new EventQueryParams.Builder(params)
                .withPeriods(periods.stream().flatMap(p -> p.getItems().stream()).toList(), EMPTY)
                .build();
      }

      addData(grid, params);
    }

    // Set response info.
    metadataHandler.addMetadata(grid, params, keywords);
    schemeIdHandler.applyScheme(grid, params);

    addPaging(params, UNLIMITED_PAGING, grid);
    applyHeaders(grid, params);

    return grid;
  }

  /**
   * Creates a {@link Grid} object with default headers.
   *
   * @return the {@link Grid} with initial headers.
   */
  private Grid createGridWithHeaders() {
    return new ListGrid()
        .addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, NUMBER, false, false));
  }

  /**
   * Adds data into the given grid, based on the given params.
   *
   * @param grid {@link Grid}.
   * @param params the {@link EventQueryParams}. @@param maxLimit the max number of records to
   *     retrieve.
   */
  private void addData(Grid grid, EventQueryParams params) {
    Timer timer = new Timer().start().disablePrint();

    List<EventQueryParams> paramsList = queryPlanner.planAggregateQuery(params);

    for (EventQueryParams queryParams : paramsList) {
      timer.getSplitTime(
          "Planned enrollment query, got partitions: {}", queryParams.getPartitions());

      int maxLimit =
          params.isAggregatedEnrollments() ? UNLIMITED_PAGING : queryValidator.getMaxLimit();

      enrollmentAnalyticsManager.getEnrollments(queryParams, grid, maxLimit);

      timer.getTime("Got enrollments " + grid.getHeight());
    }
  }

  private List<DimensionalObject> getPeriods(EventQueryParams params) {
    return params.getDimensions().stream().filter(d -> d.getDimensionType() == PERIOD).toList();
  }
}
