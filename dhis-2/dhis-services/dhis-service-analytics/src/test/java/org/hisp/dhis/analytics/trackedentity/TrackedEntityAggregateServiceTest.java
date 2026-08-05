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

import static org.hisp.dhis.common.IdScheme.UID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.QueryExecutor;
import org.hisp.dhis.analytics.common.SqlQuery;
import org.hisp.dhis.analytics.common.SqlQueryResult;
import org.hisp.dhis.analytics.common.params.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.processing.MetadataParamsHandler;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreator;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreatorService;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.support.rowset.SqlRowSet;

@ExtendWith(MockitoExtension.class)
class TrackedEntityAggregateServiceTest {
  @Mock private QueryExecutor<SqlQuery, SqlQueryResult> queryExecutor;
  @Mock private SqlQueryCreatorService sqlQueryCreatorService;
  @Mock private ExecutionPlanStore executionPlanStore;
  @Mock private CommonParamsSecurityManager securityManager;
  @Mock private MetadataParamsHandler metadataParamsHandler;
  @Mock private UserService userService;
  @Mock private SqlQueryCreator queryCreator;

  @InjectMocks private TrackedEntityAggregateService service;

  @BeforeEach
  void setUp() {
    CurrentUserUtil.injectUserInSecurityContext(UserDetails.empty().username("tester").build());
  }

  @AfterEach
  void tearDown() {
    CurrentUserUtil.clearSecurityContext();
  }

  @Test
  void getGridBuildsOuAndValueHeadersAndMapsGroupedRows() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParamsWithOuDimension();

    SqlQuery selectQuery = mock(SqlQuery.class);
    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"ou", "value"},
            List.of(new Object[] {"OU_UID_1", 42}, new Object[] {"OU_UID_2", 7}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(selectQuery);
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    Grid grid = service.getGrid(ctx);

    assertEquals(
        List.of("ou", "value"), grid.getHeaders().stream().map(GridHeader::getName).toList());
    assertEquals(2, grid.getHeight());
    assertEquals("OU_UID_1", grid.getRow(0).get(0));
    assertEquals(42, grid.getRow(0).get(1));
    verify(securityManager).decideAccess(any(), any());
    verify(metadataParamsHandler).handle(eq(grid), any(), any(), eq(0L));
  }

  @Test
  void getGridAppendsValueHeaderLastForOuPlusAttribute() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParamsWithOuAndAttribute("w75KJ2mc4zz");
    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"ou", "w75KJ2mc4zz", "value"},
            List.<Object[]>of(new Object[] {"OU1", "M", 5}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    Grid grid = service.getGrid(ctx);

    List<String> names = grid.getHeaders().stream().map(GridHeader::getName).toList();
    assertEquals("value", names.get(names.size() - 1)); // value always last
    assertEquals(List.of("ou", "w75KJ2mc4zz", "value"), names);
  }

  @Test
  void getGridCountsGroupsWhenShowTotalPages() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParamsWithOuDimensionShowTotalPages();
    SqlRowSet rowSet =
        fakeRowSet(new String[] {"ou", "value"}, List.<Object[]>of(new Object[] {"OU1", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryCreator.createForCount()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));
    when(queryExecutor.count(any())).thenReturn(9L);

    service.getGrid(ctx);

    verify(queryExecutor).count(any());
    verify(metadataParamsHandler).handle(any(), any(), any(), eq(9L));
  }

  @Test
  void getGridMetadataExcludesInjectedNonGroupedDimensions() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParamsWithInjectedAttribute("w75KJ2mc4zz");
    SqlRowSet rowSet =
        fakeRowSet(new String[] {"ou", "value"}, List.<Object[]>of(new Object[] {"OU1", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    service.getGrid(ctx);

    ArgumentCaptor<ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>> captor =
        ArgumentCaptor.forClass(ContextParams.class);
    verify(metadataParamsHandler).handle(any(), captor.capture(), any(), anyLong());

    List<String> metadataDimensionKeys =
        captor.getValue().getCommonParsed().getDimensionIdentifiers().stream()
            .map(DimensionIdentifier::getKey)
            .toList();
    assertEquals(List.of("ou"), metadataDimensionKeys);

    List<String> metadataHeaderKeys =
        captor.getValue().getCommonParsed().getParsedHeaders().stream()
            .map(DimensionIdentifier::getKey)
            .toList();
    assertEquals(List.of("ou"), metadataHeaderKeys);
  }

  @Test
  void getGridRoundsDecimalValueByDefault() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParamsWithOuDimension();
    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"ou", "value"},
            List.of(
                new Object[] {"OU1", new BigDecimal("10.126")},
                new Object[] {"OU2", new BigDecimal("7.000")}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    Grid grid = service.getGrid(ctx);

    assertEquals(10.13, grid.getRow(0).get(1));
    assertEquals(7L, grid.getRow(1).get(1));
  }

  @Test
  void getGridKeepsPrecisionWhenSkipRounding() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParamsWithOuDimension();
    ctx.getCommonRaw().setSkipRounding(true);
    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"ou", "value"},
            List.<Object[]>of(new Object[] {"OU1", new BigDecimal("10.123456789012345")}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    Grid grid = service.getGrid(ctx);

    assertEquals(10.123456789, grid.getRow(0).get(1));
  }

  @Test
  void getGridRejectsSortOnNonGroupedDimension() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParamsGroupedByOuSortedBy(stubAttributeDimension("w75KJ2mc4zz"));

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.getGrid(ctx));
    assertEquals(ErrorCode.E7252, ex.getErrorCode());
  }

  @Test
  void getGridAllowsSortOnGroupedDimension() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParamsGroupedByOuSortedBy(stubOuDimension("ou1"));
    SqlRowSet rowSet =
        fakeRowSet(new String[] {"ou", "value"}, List.<Object[]>of(new Object[] {"OU1", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    assertDoesNotThrow(() -> service.getGrid(ctx));
  }

  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>
      aggregateContextParamsWithInjectedAttribute(String attribute) {
    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().aggregate(true).build();

    // Only `ou` is explicitly requested; the attribute is injected into the parsed dimensions
    // upstream (as the TE mapper does for all TET attributes) and must not surface in metaData.
    return ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
        .typedParsed(trackedEntityQueryParams)
        .commonRaw(new CommonRequestParams().withDimension(Set.of("ou")))
        .commonParsed(
            CommonParsedParams.builder()
                .dimensionIdentifiers(
                    List.of(stubOuDimension("ou1"), stubAttributeDimension(attribute)))
                .parsedHeaders(
                    new LinkedHashSet<>(
                        List.of(stubOuDimension("ou1"), stubAttributeDimension(attribute))))
                .build())
        .build();
  }

  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>
      aggregateContextParamsGroupedByOuSortedBy(DimensionIdentifier<DimensionParam> orderBy) {
    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().aggregate(true).build();

    return ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
        .typedParsed(trackedEntityQueryParams)
        .commonRaw(new CommonRequestParams().withDimension(Set.of("ou")))
        .commonParsed(
            CommonParsedParams.builder()
                .dimensionIdentifiers(List.of(stubOuDimension("ou1")))
                .orderParams(
                    List.of(
                        AnalyticsSortingParams.builder()
                            .orderBy(orderBy)
                            .sortDirection(SortDirection.DESC)
                            .index(0)
                            .build()))
                .build())
        .build();
  }

  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>
      aggregateContextParamsWithOuDimension() {
    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().aggregate(true).build();

    return ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
        .typedParsed(trackedEntityQueryParams)
        .commonRaw(new CommonRequestParams().withDimension(Set.of("ou")))
        .commonParsed(
            CommonParsedParams.builder()
                .dimensionIdentifiers(List.of(stubOuDimension("ou1")))
                .build())
        .build();
  }

  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>
      aggregateContextParamsWithOuAndAttribute(String attribute) {
    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().aggregate(true).build();

    return ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
        .typedParsed(trackedEntityQueryParams)
        .commonRaw(new CommonRequestParams().withDimension(Set.of("ou", attribute)))
        .commonParsed(
            CommonParsedParams.builder()
                .dimensionIdentifiers(
                    List.of(stubOuDimension("ou1"), stubAttributeDimension(attribute)))
                .build())
        .build();
  }

  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>
      aggregateContextParamsWithOuDimensionShowTotalPages() {
    TrackedEntityQueryParams trackedEntityQueryParams =
        TrackedEntityQueryParams.builder().aggregate(true).build();

    return ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
        .typedParsed(trackedEntityQueryParams)
        .commonRaw(new CommonRequestParams().withDimension(Set.of("ou")))
        .commonParsed(
            CommonParsedParams.builder()
                .dimensionIdentifiers(List.of(stubOuDimension("ou1")))
                .pagingParams(AnalyticsPagingParams.builder().totalPages(true).build())
                .build())
        .build();
  }

  private DimensionIdentifier<DimensionParam> stubOuDimension(String ou) {
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid(ou);
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(orgUnit)),
            DimensionParamType.DIMENSIONS,
            UID,
            List.of(ou));
    return DimensionIdentifier.of(
            ElementWithOffset.emptyElementWithOffset(),
            ElementWithOffset.emptyElementWithOffset(),
            dimensionParam)
        .withDefaultGroupId();
  }

  private DimensionIdentifier<DimensionParam> stubAttributeDimension(String attribute) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject(attribute, DimensionType.PROGRAM_ATTRIBUTE, List.of()),
            DimensionParamType.DIMENSIONS,
            UID,
            List.of());
    return DimensionIdentifier.of(
            ElementWithOffset.emptyElementWithOffset(),
            ElementWithOffset.emptyElementWithOffset(),
            dimensionParam)
        .withDefaultGroupId();
  }

  private SqlRowSet fakeRowSet(String[] columns, List<Object[]> rows) {
    SqlRowSet rowSet = mock(SqlRowSet.class);
    int[] currentRow = {-1};
    when(rowSet.next())
        .thenAnswer(
            invocation -> {
              currentRow[0]++;
              return currentRow[0] < rows.size();
            });
    when(rowSet.getObject(anyString()))
        .thenAnswer(
            invocation -> {
              String column = invocation.getArgument(0);
              int columnIndex = List.of(columns).indexOf(column);
              return rows.get(currentRow[0])[columnIndex];
            });
    return rowSet;
  }
}
