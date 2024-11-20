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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.common.ColumnHeader.CREATED_BY_DISPLAY_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.ENROLLMENT;
import static org.hisp.dhis.analytics.common.ColumnHeader.ENROLLMENT_DATE;
import static org.hisp.dhis.analytics.common.ColumnHeader.GEOMETRY;
import static org.hisp.dhis.analytics.common.ColumnHeader.INCIDENT_DATE;
import static org.hisp.dhis.analytics.common.ColumnHeader.LAST_UPDATED;
import static org.hisp.dhis.analytics.common.ColumnHeader.LAST_UPDATED_BY_DISPLAY_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.LATITUDE;
import static org.hisp.dhis.analytics.common.ColumnHeader.LONGITUDE;
import static org.hisp.dhis.analytics.common.ColumnHeader.ORG_UNIT_CODE;
import static org.hisp.dhis.analytics.common.ColumnHeader.ORG_UNIT_NAME;
import static org.hisp.dhis.analytics.common.ColumnHeader.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.common.ColumnHeader.PROGRAM_STATUS;
import static org.hisp.dhis.analytics.common.ColumnHeader.STORED_BY;
import static org.hisp.dhis.analytics.common.ColumnHeader.TRACKED_ENTITY;
import static org.hisp.dhis.analytics.event.LabelMapper.getEnrollmentDateLabel;
import static org.hisp.dhis.analytics.event.LabelMapper.getEnrollmentLabel;
import static org.hisp.dhis.analytics.event.LabelMapper.getIncidentDateLabel;
import static org.hisp.dhis.analytics.event.LabelMapper.getOrgUnitLabel;
import static org.hisp.dhis.analytics.tracker.HeaderHelper.addCommonHeaders;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.UNLIMITED_PAGING;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.addPaging;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.applyHeaders;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.getDimensionsKeywords;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.setRowContextColumns;
import static org.hisp.dhis.common.ValueType.DATETIME;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;

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
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.util.Timer;
import org.springframework.stereotype.Service;

/** This service is responsible for querying enrollments. */
@Service
@RequiredArgsConstructor
public class EnrollmentQueryService {

  private final EnrollmentAnalyticsManager enrollmentAnalyticsManager;

  private final EventQueryPlanner queryPlanner;

  private final AnalyticsSecurityManager securityManager;

  private final EventQueryValidator queryValidator;

  private final MetadataItemsHandler metadataHandler;

  private final SchemeIdHandler schemeIdHandler;

  /**
   * Returns a list of enrollments matching the given query.
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

    // Set periods.
    params = new EventQueryParams.Builder(params).withStartEndDatesForPeriods().build();

    // Populate headers.
    Grid grid = createGridWithHeaders(params);
    addCommonHeaders(grid, params, List.of());

    // Add data.
    long count = 0;

    if (!params.isSkipData() || params.analyzeOnly()) {
      count = addData(grid, params);
    }

    // Set response info.
    metadataHandler.addMetadata(grid, params, keywords);
    schemeIdHandler.applyScheme(grid, params);

    addPaging(params, count, grid);
    applyHeaders(grid, params);
    setRowContextColumns(grid);

    return grid;
  }

  /**
   * Creates a {@link Grid} object with default headers.
   *
   * @param params the {@link EventQueryParams}.
   * @return the {@link Grid} with initial headers.
   */
  private Grid createGridWithHeaders(EventQueryParams params) {
    return new ListGrid()
        .addHeader(
            new GridHeader(
                ENROLLMENT.getItem(),
                getEnrollmentLabel(params.getProgram(), ENROLLMENT.getName()),
                TEXT,
                false,
                true))
        .addHeader(
            new GridHeader(TRACKED_ENTITY.getItem(), TRACKED_ENTITY.getName(), TEXT, false, true))
        .addHeader(
            new GridHeader(
                ENROLLMENT_DATE.getItem(),
                getEnrollmentDateLabel(params.getProgram(), ENROLLMENT_DATE.getName()),
                DATETIME,
                false,
                true))
        .addHeader(
            new GridHeader(
                INCIDENT_DATE.getItem(),
                getIncidentDateLabel(params.getProgram(), INCIDENT_DATE.getName()),
                DATETIME,
                false,
                true))
        .addHeader(new GridHeader(STORED_BY.getItem(), STORED_BY.getName(), TEXT, false, true))
        .addHeader(
            new GridHeader(
                CREATED_BY_DISPLAY_NAME.getItem(),
                CREATED_BY_DISPLAY_NAME.getName(),
                TEXT,
                false,
                true))
        .addHeader(
            new GridHeader(
                LAST_UPDATED_BY_DISPLAY_NAME.getItem(),
                LAST_UPDATED_BY_DISPLAY_NAME.getName(),
                TEXT,
                false,
                true))
        .addHeader(
            new GridHeader(LAST_UPDATED.getItem(), LAST_UPDATED.getName(), DATETIME, false, true))
        .addHeader(new GridHeader(GEOMETRY.getItem(), GEOMETRY.getName(), TEXT, false, true))
        .addHeader(new GridHeader(LONGITUDE.getItem(), LONGITUDE.getName(), NUMBER, false, true))
        .addHeader(new GridHeader(LATITUDE.getItem(), LATITUDE.getName(), NUMBER, false, true))
        .addHeader(
            new GridHeader(
                ORG_UNIT_NAME.getItem(),
                getOrgUnitLabel(params.getProgram(), ORG_UNIT_NAME.getName()),
                TEXT,
                false,
                true))
        .addHeader(
            new GridHeader(
                ORG_UNIT_NAME_HIERARCHY.getItem(),
                ORG_UNIT_NAME_HIERARCHY.getName(),
                TEXT,
                false,
                true))
        .addHeader(
            new GridHeader(ORG_UNIT_CODE.getItem(), ORG_UNIT_CODE.getName(), TEXT, false, true))
        .addHeader(
            new GridHeader(PROGRAM_STATUS.getItem(), PROGRAM_STATUS.getName(), TEXT, false, true));
  }

  /**
   * Adds data into the given grid, based on the given params.
   *
   * @param grid {@link Grid}.
   * @param params the {@link EventQueryParams}. @@param maxLimit the max number of records to
   *     retrieve.
   */
  private long addData(Grid grid, EventQueryParams params) {
    Timer timer = new Timer().start().disablePrint();

    EventQueryParams queryParams = queryPlanner.planEnrollmentQuery(params);

    long count = 0;

    timer.getSplitTime("Planned enrollment query, got partitions: {}", queryParams.getPartitions());

    if (queryParams.isTotalPages()) {
      count += enrollmentAnalyticsManager.getEnrollmentCount(queryParams);
    }

    int maxLimit =
        params.isAggregatedEnrollments() ? UNLIMITED_PAGING : queryValidator.getMaxLimit();

    enrollmentAnalyticsManager.getEnrollments(queryParams, grid, maxLimit);

    timer.getTime("Got enrollments " + grid.getHeight());

    return count;
  }
}
