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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
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
import static org.hisp.dhis.analytics.common.ColumnHeader.TEI;
import static org.hisp.dhis.common.ValueType.DATE;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;

import java.util.List;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.data.handler.SchemeIdResponseMapper;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.event.LabelMapper;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.Timer;
import org.springframework.stereotype.Service;

/**
 * @author Markus Bekken
 */
@Service("org.hisp.dhis.analytics.event.EnrollmentAnalyticsService")
public class DefaultEnrollmentAnalyticsService extends AbstractAnalyticsService
    implements EnrollmentAnalyticsService {

  private final EnrollmentAnalyticsManager enrollmentAnalyticsManager;

  private final EventQueryPlanner queryPlanner;

  public DefaultEnrollmentAnalyticsService(
      EnrollmentAnalyticsManager enrollmentAnalyticsManager,
      AnalyticsSecurityManager securityManager,
      EventQueryPlanner queryPlanner,
      EventQueryValidator queryValidator,
      SchemeIdResponseMapper schemeIdResponseMapper,
      CurrentUserService currentUserService) {
    super(securityManager, queryValidator, schemeIdResponseMapper, currentUserService);

    checkNotNull(enrollmentAnalyticsManager);
    checkNotNull(queryPlanner);
    checkNotNull(schemeIdResponseMapper);

    this.enrollmentAnalyticsManager = enrollmentAnalyticsManager;
    this.queryPlanner = queryPlanner;
  }

  // -------------------------------------------------------------------------
  // EventAnalyticsService implementation
  // -------------------------------------------------------------------------

  @Override
  public Grid getEnrollments(EventQueryParams params) {
    return getGrid(params);
  }

  @Override
  protected Grid createGridWithHeaders(EventQueryParams params) {
    if (params.getEndpointAction() == RequestTypeAware.EndpointAction.AGGREGATE) {
      return new ListGrid()
          .addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, NUMBER, false, false));
    }

    return new ListGrid()
        .addHeader(
            new GridHeader(
                ENROLLMENT.getItem(), ENROLLMENT.getName(), TEXT, false, true))
        .addHeader(new GridHeader(TEI.getItem(), TEI.getName(), TEXT, false, true))
        .addHeader(
            new GridHeader(
                ENROLLMENT_DATE.getItem(),
                LabelMapper.getEnrollmentDateLabel(params.getProgram(), ENROLLMENT_DATE.getName()),
                DATE,
                false,
                true))
        .addHeader(
            new GridHeader(
                INCIDENT_DATE.getItem(),
                LabelMapper.getIncidentDateLabel(params.getProgram(), INCIDENT_DATE.getName()),
                DATE,
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
            new GridHeader(LAST_UPDATED.getItem(), LAST_UPDATED.getName(), DATE, false, true))
        .addHeader(new GridHeader(GEOMETRY.getItem(), GEOMETRY.getName(), TEXT, false, true))
        .addHeader(new GridHeader(LONGITUDE.getItem(), LONGITUDE.getName(), NUMBER, false, true))
        .addHeader(new GridHeader(LATITUDE.getItem(), LATITUDE.getName(), NUMBER, false, true))
        .addHeader(
            new GridHeader(ORG_UNIT_NAME.getItem(), ORG_UNIT_NAME.getName(), TEXT, false, true))
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

  @Override
  protected long addData(Grid grid, EventQueryParams params) {
    Timer timer = new Timer().start().disablePrint();

    List<EventQueryParams> paramsList;

    if (params.getEndpointAction() == RequestTypeAware.EndpointAction.AGGREGATE) {
      paramsList = queryPlanner.planAggregateQuery(params);
    } else {
      paramsList = List.of(queryPlanner.planEnrollmentQuery(params));
    }

    long count = 0;
    for (EventQueryParams queryParams : paramsList) {
      timer.getSplitTime("Planned event query, got partitions: " + queryParams.getPartitions());
      if (queryParams.isTotalPages() && !params.isAggregatedEnrollments()) {
        count += enrollmentAnalyticsManager.getEnrollmentCount(queryParams);
      }

      // maxLimit == 0 means unlimited paging
      int maxLimit = params.isAggregatedEnrollments() ? 0 : queryValidator.getMaxLimit();

      enrollmentAnalyticsManager.getEnrollments(queryParams, grid, maxLimit);

      timer.getTime("Got enrollments " + grid.getHeight());
    }

    return count;
  }

  @Override
  protected List<DimensionalObject> getPeriods(EventQueryParams params) {
    // for aggregated enrollments only
    if (!params.isAggregatedEnrollments()) {
      return List.of();
    }

    return params.getDimensions().stream()
        .filter(d -> d.getDimensionType() == DimensionType.PERIOD)
        .toList();
  }
}
