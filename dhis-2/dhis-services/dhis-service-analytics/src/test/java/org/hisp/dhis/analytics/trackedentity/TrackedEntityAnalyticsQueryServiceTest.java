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
package org.hisp.dhis.analytics.trackedentity;

import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamType.DIMENSIONS;
import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.feedback.ErrorCode.E7246;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.GridAdaptor;
import org.hisp.dhis.analytics.common.QueryExecutor;
import org.hisp.dhis.analytics.common.SqlQuery;
import org.hisp.dhis.analytics.common.SqlQueryResult;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreatorService;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackedEntityAnalyticsQueryServiceTest {

  @Mock private QueryExecutor<SqlQuery, SqlQueryResult> queryExecutor;

  @Mock private GridAdaptor gridAdaptor;

  @Mock private SqlQueryCreatorService sqlQueryCreatorService;

  @Mock private ExecutionPlanStore executionPlanStore;

  @Mock private CommonParamsSecurityManager securityManager;

  @Mock private UserService userService;

  private TrackedEntityAnalyticsQueryService service;

  @BeforeEach
  void setUp() {
    service =
        new TrackedEntityAnalyticsQueryService(
            queryExecutor,
            gridAdaptor,
            sqlQueryCreatorService,
            executionPlanStore,
            securityManager,
            userService);
  }

  @Test
  void getGridExplainRejectsReservedNoValueKeywordOnNonOptionSetDimension() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .commonParsed(
                CommonParsedParams.builder()
                    .dimensionIdentifiers(List.of(nonOptionSetDimension("IN:D2__NOVALUE")))
                    .build())
            .build();

    IllegalQueryException error =
        assertThrows(IllegalQueryException.class, () -> service.getGridExplain(contextParams));

    assertEquals(E7246, error.getErrorCode());
    verifyNoInteractions(sqlQueryCreatorService, executionPlanStore);
  }

  private DimensionIdentifier<DimensionParam> nonOptionSetDimension(String filter) {
    DataElement dataElement = new DataElement("Age");
    dataElement.setUid("de123456789");
    dataElement.setValueType(ValueType.NUMBER);

    QueryItem queryItem = new QueryItem(dataElement, null, ValueType.NUMBER, null, null);

    return DimensionIdentifier.of(
        emptyElementWithOffset(),
        emptyElementWithOffset(),
        DimensionParam.ofObject(queryItem, DIMENSIONS, UID, List.of(filter)));
  }
}
