/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.analytics.common.ColumnHeader.*;
import static org.hisp.dhis.analytics.event.LabelMapper.getEnrollmentDateLabel;
import static org.hisp.dhis.analytics.event.LabelMapper.getEventDateLabel;
import static org.hisp.dhis.analytics.event.LabelMapper.getEventLabel;
import static org.hisp.dhis.analytics.event.LabelMapper.getIncidentDateLabel;
import static org.hisp.dhis.analytics.event.LabelMapper.getOrgUnitLabel;
import static org.hisp.dhis.analytics.event.LabelMapper.getProgramStageLabel;
import static org.hisp.dhis.analytics.event.LabelMapper.getScheduledDateLabel;
import static org.hisp.dhis.analytics.tracker.HeaderHelper.addCommonHeaders;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.addPaging;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.applyHeaders;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.getDimensionsKeywords;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.setRowContextColumns;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.ValueType.DATETIME;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.feedback.ErrorCode.E7218;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.tracker.MetadataItemsHandler;
import org.hisp.dhis.analytics.tracker.SchemeIdHandler;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.util.Timer;
import org.springframework.stereotype.Service;

/** Service responsible for querying events. */
@Service
@RequiredArgsConstructor
public class EventQueryService {

  private final AnalyticsSecurityManager securityManager;

  private final EventQueryValidator queryValidator;

  private final EventAnalyticsManager eventAnalyticsManager;

  private final EventQueryPlanner queryPlanner;

  private final MetadataItemsHandler metadataHandler;

  private final SchemeIdHandler schemeIdHandler;

  private final SqlBuilder sqlBuilder;

  /**
   * Returns a list of events matching the given query.
   *
   * @param params the event query parameters.
   * @return events as a {@Grid} object.
   */
  public Grid getEvents(EventQueryParams params) {
    // Security

    securityManager.decideAccessEventQuery(params);
    params = securityManager.withUserConstraints(params);

    // Validation

    queryValidator.validate(params);

    List<Keyword> keywords = getDimensionsKeywords(params);

    params = new EventQueryParams.Builder(params).withStartEndDatesForPeriods().build();

    // Headers

    Grid grid = createGridWithHeaders(params);
    addCommonHeaders(grid, params, List.of());

    // Data

    long count = 0;

    if (!params.isSkipData() || params.analyzeOnly()) {
      count = addData(grid, params);
    }

    // Metadata

    metadataHandler.addMetadata(grid, params, keywords);
    schemeIdHandler.applyScheme(grid, params);

    // Paging

    addPaging(params, count, grid);
    applyHeaders(grid, params);
    setRowContextColumns(grid);

    return grid;
  }

  /**
   * Returns a list of event clusters matching the given query.
   *
   * @param params the event query parameters.
   * @return event clusters as a {@link Grid} object.
   */
  public Grid getEventClusters(EventQueryParams params) {
    if (!isGeospatialSupport()) {
      throwIllegalQueryEx(E7218);
    }

    params =
        new EventQueryParams.Builder(params)
            .withGeometryOnly(true)
            .withStartEndDatesForPeriods()
            .build();

    securityManager.decideAccessEventQuery(params);

    queryValidator.validate(params);

    Grid grid =
        new ListGrid()
            .addHeader(new GridHeader(COUNT.getItem(), COUNT.getName(), NUMBER, false, false))
            .addHeader(new GridHeader(CENTER.getItem(), CENTER.getName(), TEXT, false, false))
            .addHeader(new GridHeader(EXTENT.getItem(), EXTENT.getName(), TEXT, false, false))
            .addHeader(new GridHeader(POINTS.getItem(), POINTS.getName(), TEXT, false, false));

    params = queryPlanner.planEventQuery(params);

    eventAnalyticsManager.getEventClusters(params, grid, queryValidator.getMaxLimit());

    return grid;
  }

  /**
   * Returns a Rectangle with information about event count and extent of the spatial rectangle for
   * the given query.
   *
   * @param params the event query parameters.
   * @return event clusters as a {@link Grid} object.
   */
  public Rectangle getRectangle(EventQueryParams params) {
    if (!isGeospatialSupport()) {
      throwIllegalQueryEx(E7218);
    }

    params =
        new EventQueryParams.Builder(params)
            .withGeometryOnly(true)
            .withStartEndDatesForPeriods()
            .build();

    securityManager.decideAccessEventQuery(params);

    queryValidator.validate(params);

    params = queryPlanner.planEventQuery(params);

    return eventAnalyticsManager.getRectangle(params);
  }

  /**
   * Creates a grid with headers.
   *
   * @param params the {@link EventQueryParams}.
   */
  private Grid createGridWithHeaders(EventQueryParams params) {
    Grid grid = new ListGrid();

    grid.addHeader(
            new GridHeader(
                EVENT.getItem(),
                getEventLabel(params.getProgramStage(), EVENT.getName()),
                TEXT,
                false,
                true))
        .addHeader(
            new GridHeader(
                PROGRAM_STAGE.getItem(),
                getProgramStageLabel(params.getProgramStage(), PROGRAM_STAGE.getName()),
                TEXT,
                false,
                true))
        .addHeader(
            new GridHeader(
                EVENT_DATE.getItem(),
                getEventDateLabel(params.getProgramStage(), EVENT_DATE.getName()),
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
        .addHeader(
            new GridHeader(
                SCHEDULED_DATE.getItem(),
                getScheduledDateLabel(params.getProgramStage(), SCHEDULED_DATE.getName()),
                DATETIME,
                false,
                true));

    if (params.getProgram() != null && params.getProgram().isRegistration()) {
      grid.addHeader(
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
          .addHeader(
              new GridHeader(TRACKED_ENTITY.getItem(), TRACKED_ENTITY.getName(), TEXT, false, true))
          .addHeader(
              new GridHeader(
                  PROGRAM_INSTANCE.getItem(), PROGRAM_INSTANCE.getName(), TEXT, false, true));
    }

    if (isGeospatialSupport()) {
      grid.addHeader(new GridHeader(GEOMETRY.getItem(), GEOMETRY.getName(), TEXT, false, true))
          .addHeader(
              new GridHeader(
                  ENROLLMENT_GEOMETRY.getItem(), ENROLLMENT_GEOMETRY.getName(), TEXT, false, true))
          .addHeader(new GridHeader(LONGITUDE.getItem(), LONGITUDE.getName(), NUMBER, false, true))
          .addHeader(new GridHeader(LATITUDE.getItem(), LATITUDE.getName(), NUMBER, false, true));
    }

    grid.addHeader(
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
            new GridHeader(PROGRAM_STATUS.getItem(), PROGRAM_STATUS.getName(), TEXT, false, true))
        .addHeader(
            new GridHeader(EVENT_STATUS.getItem(), EVENT_STATUS.getName(), TEXT, false, true));

    return grid;
  }

  /**
   * Adds event data to the given grid. Returns the number of events matching the given event query.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   * @return the count of events.
   */
  private long addData(Grid grid, EventQueryParams params) {
    Timer timer = new Timer().start().disablePrint();

    params = queryPlanner.planEventQuery(params);

    timer.getSplitTime("Planned event query, got partitions: {}", params.getPartitions());

    long count = 0;
    EventQueryParams immutableParams = new EventQueryParams.Builder(params).build();

    if (params.getPartitions().hasAny() || params.isSkipPartitioning()) {
      eventAnalyticsManager.getEvents(immutableParams, grid, queryValidator.getMaxLimit());

      if (params.isPaging() && params.isTotalPages()) {
        count = eventAnalyticsManager.getEventCount(immutableParams);
      }

      timer.getTime("Got events " + grid.getHeight());
    }

    return count;
  }

  /**
   * Indicates whether the DBMS supports geospatial data types and functions.
   *
   * @return true if the DBMS supports geospatial data types and functions.
   */
  private boolean isGeospatialSupport() {
    return sqlBuilder.supportsGeospatialData();
  }
}
