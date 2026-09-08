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
package org.hisp.dhis.webapi.controller.trackedentity;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.processing.CommonQueryRequestValidator;
import org.hisp.dhis.analytics.common.processing.Parser;
import org.hisp.dhis.analytics.common.processing.Validator;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityAggregateService;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityAnalyticsDimensionsService;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityAnalyticsQueryService;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryRequestMapper;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.webapi.dimension.DimensionFilteringAndPagingService;
import org.hisp.dhis.webapi.dimension.DimensionMapperService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test proving that requests routed through the {@code aggregate/{tet}} endpoints reach the
 * {@link TrackedEntityAggregateService} with the aggregate flag set on the query params.
 */
@ExtendWith(MockitoExtension.class)
class TrackedEntityAnalyticsControllerTest {

  @Mock private TrackedEntityAnalyticsQueryService trackedEntityAnalyticsQueryService;

  @Mock private Parser<CommonRequestParams, CommonParsedParams> commonRequestParamsParser;

  @Mock private CommonQueryRequestValidator commonQueryRequestValidator;

  @Mock private Validator<TrackedEntityRequestParams> trackedEntityQueryRequestValidator;

  @Mock private DimensionFilteringAndPagingService dimensionFilteringAndPagingService;

  @Mock private DimensionMapperService dimensionMapperService;

  @Mock private TrackedEntityAnalyticsDimensionsService trackedEntityAnalyticsDimensionsService;

  @Mock private TrackedEntityQueryRequestMapper mapper;

  @Mock private ContextUtils contextUtils;

  @Mock private TrackedEntityAggregateService trackedEntityAggregateService;

  private TrackedEntityAnalyticsController controller;

  @BeforeEach
  void setUp() {
    controller =
        new TrackedEntityAnalyticsController(
            trackedEntityAnalyticsQueryService,
            commonRequestParamsParser,
            commonQueryRequestValidator,
            trackedEntityQueryRequestValidator,
            dimensionFilteringAndPagingService,
            dimensionMapperService,
            trackedEntityAnalyticsDimensionsService,
            mapper,
            contextUtils,
            trackedEntityAggregateService);
  }

  @Test
  void aggregateEndpointSetsAggregateFlagOnQueryParams() {
    TrackedEntityType tet = new TrackedEntityType();
    when(mapper.map(eq("tetUid"), any()))
        .thenReturn(TrackedEntityQueryParams.builder().trackedEntityType(tet).build());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>> captor =
        ArgumentCaptor.forClass(ContextParams.class);
    when(trackedEntityAggregateService.getGrid(captor.capture())).thenReturn(new ListGrid());

    controller.aggregate("tetUid", new TrackedEntityRequestParams(null), new CommonRequestParams());

    assertTrue(captor.getValue().getTypedParsed().isAggregate());
  }
}
