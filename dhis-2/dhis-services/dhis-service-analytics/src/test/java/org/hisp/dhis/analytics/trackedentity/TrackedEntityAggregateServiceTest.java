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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
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
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.processing.MetadataParamsHandler;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreator;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreatorService;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
  void getGridChecksDataReadAccessForEventValueProgramStage() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("PsUid000001");
    DataElement dataElement = new DataElement();
    dataElement.setUid("DeUid000001");
    dataElement.setValueType(ValueType.NUMBER);
    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid("TetUid00001");

    TrackedEntityQueryParams teParams =
        TrackedEntityQueryParams.builder()
            .trackedEntityType(trackedEntityType)
            .aggregate(true)
            .eventValue(new EventValue(programStage, dataElement, 0))
            .aggregationType(AggregationType.AVERAGE)
            .build();

    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
            .typedParsed(teParams)
            .commonRaw(new CommonRequestParams().withDimension(Set.of("ou")))
            .commonParsed(
                CommonParsedParams.builder()
                    .dimensionIdentifiers(List.of(stubOuDimension("ou1")))
                    .build())
            .build();

    SqlRowSet rowSet =
        fakeRowSet(new String[] {"ou", "value"}, List.<Object[]>of(new Object[] {"OU1", 5}));
    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    service.getGrid(ctx);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<IdentifiableObject>> captor =
        ArgumentCaptor.forClass(Collection.class);
    verify(securityManager).decideAccess(any(), captor.capture());
    Collection<IdentifiableObject> checked = captor.getValue();
    assertTrue(checked.contains(trackedEntityType), "tracked entity type must be access-checked");
    assertTrue(checked.contains(programStage), "event value program stage must be access-checked");
    // The data element is intentionally not checked: event-data access is governed by the program
    // stage, and data elements are in the security manager's data-read skip set.
    assertFalse(
        checked.contains(dataElement), "event value data element must not be access-checked");
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

  /**
   * An end date is {@code completeddate} on the enrollment table, so the query has no column to
   * group it on and the request is rejected rather than reaching the database.
   */
  @Test
  void getGridRejectsDimensionTheQueryCannotGroupBy() {
    String endDate = "IpHINAT79UW.ENDDATE";
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(
            Set.of("ou", endDate),
            List.of(
                stubOuDimension("ou1"),
                stubProgramScopedStaticDimension(StaticDimension.ENDDATE, List.of())));

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.getGrid(ctx));
    assertEquals(ErrorCode.E7258, ex.getErrorCode());
  }

  /**
   * A stage scoped org unit is grouped on the org unit of the single event chosen for each tracked
   * entity, and is reported under the stage scoped name the request used, matching the enrollment
   * aggregate endpoint.
   */
  @Test
  void getGridGroupsByStageScopedOrgUnit() {
    DimensionIdentifier<DimensionParam> stageOu = stubStageScopedOuDimension();
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(Set.of("A03MvHHogjR.ou:USER_ORGUNIT"), List.of(stageOu));
    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"A03MvHHogjR.ou", "value"}, List.<Object[]>of(new Object[] {"OU1", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    Grid grid = service.getGrid(ctx);

    assertEquals(
        List.of("A03MvHHogjR.ou", "value"),
        grid.getHeaders().stream().map(GridHeader::getName).toList());
    assertEquals(1, grid.getHeight());
  }

  /**
   * A program scoped event status has no enrollment column, so it cannot be grouped. Carrying items
   * does not turn it into a filter: a {@code dimension} the query cannot group on is rejected, so
   * the caller never gets a restricted number with no column to show for it.
   */
  @Test
  void getGridRejectsNonGroupableScopedDimensionCarryingItems() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(
            Set.of("ou", "IpHINAT79UW.EVENT_STATUS:ACTIVE"),
            List.of(
                stubOuDimension("ou1"),
                stubProgramScopedStaticDimension(StaticDimension.EVENT_STATUS, List.of("ACTIVE"))));

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.getGrid(ctx));
    assertEquals(ErrorCode.E7258, ex.getErrorCode());
  }

  /**
   * A stage data element is grouped on the value of the chosen event and reported under its stage
   * scoped name, both when it carries items and when it does not.
   */
  @Test
  void getGridGroupsByStageDataElement() {
    DimensionIdentifier<DimensionParam> dataElement =
        stubStageDataElementDimension("UXz7xuGCEhU", List.of("GT:10"));
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(Set.of("A03MvHHogjR.UXz7xuGCEhU:GT:10"), List.of(dataElement));
    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"A03MvHHogjR.UXz7xuGCEhU", "value"},
            List.<Object[]>of(new Object[] {"3400", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    Grid grid = service.getGrid(ctx);

    assertEquals(
        List.of("A03MvHHogjR.UXz7xuGCEhU", "value"),
        grid.getHeaders().stream().map(GridHeader::getName).toList());
    assertEquals(1, grid.getHeight());
  }

  /**
   * An enrollment or event level static dimension has no column on the tracked entity table, so it
   * cannot be grouped even though it carries no program or stage prefix.
   */
  @Test
  void getGridRejectsAnEnrollmentLevelStaticDimension() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(
            Set.of("enrollmentdate"), List.of(stubStaticDimension(StaticDimension.ENROLLMENTDATE)));

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.getGrid(ctx));
    assertEquals(ErrorCode.E7258, ex.getErrorCode());
  }

  /** The tracked entity table has no period column, so a period dimension cannot be grouped. */
  @Test
  void getGridRejectsAPeriodDimension() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(Set.of("pe"), List.of(stubPeriodDimension()));

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.getGrid(ctx));
    assertEquals(ErrorCode.E7258, ex.getErrorCode());
  }

  /**
   * Every static dimension backed by a tracked entity table column stays groupable. {@code created}
   * and the display name fields have no {@code TrackedEntityStaticField} on their {@link
   * StaticDimension}, so a check based on that field alone would reject them.
   */
  @ParameterizedTest
  @EnumSource(
      value = StaticDimension.class,
      names = {
        "TRACKEDENTITY",
        "GEOMETRY",
        "LONGITUDE",
        "LATITUDE",
        "OUNAME",
        "OUCODE",
        "OUNAMEHIERARCHY",
        "CREATED",
        "LASTUPDATED",
        "CREATEDBYDISPLAYNAME",
        "LASTUPDATEDBYDISPLAYNAME"
      })
  void getGridGroupsByEveryTrackedEntityStaticField(StaticDimension staticDimension) {
    String column = staticDimension.getHeaderName();
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(Set.of(column), List.of(stubStaticDimension(staticDimension)));
    SqlRowSet rowSet =
        fakeRowSet(new String[] {column, "value"}, List.<Object[]>of(new Object[] {"x", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    Grid grid = service.getGrid(ctx);

    assertEquals(
        List.of(column, "value"), grid.getHeaders().stream().map(GridHeader::getName).toList());
  }

  @Test
  void getGridExplainRejectsDimensionTheQueryCannotGroupBy() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(
            Set.of("ou", "IpHINAT79UW.ENDDATE"),
            List.of(
                stubOuDimension("ou1"),
                stubProgramScopedStaticDimension(StaticDimension.ENDDATE, List.of())));

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.getGridExplain(ctx));
    assertEquals(ErrorCode.E7258, ex.getErrorCode());
  }

  @Test
  void getGridAllowsGroupedOrgUnitAndAttributeDimensions() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParamsWithOuAndAttribute("w75KJ2mc4zz");
    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"ou", "w75KJ2mc4zz", "value"},
            List.<Object[]>of(new Object[] {"OU1", "James", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    assertDoesNotThrow(() -> service.getGrid(ctx));
  }

  /**
   * A static dimension is parsed to its canonical header name, so an aliased spelling has to be
   * resolved before a request is matched against the parsed dimensions. Comparing the raw spelling
   * left {@code LAST_UPDATED} outside the grouped set, which dropped its column silently.
   */
  @Test
  void getGridGroupsByAnAliasedStaticDimension() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(
            Set.of("LAST_UPDATED"), List.of(stubStaticDimension(StaticDimension.LASTUPDATED)));
    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"lastupdated", "value"},
            List.<Object[]>of(new Object[] {"2019-08-21 13:29:58.318", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    Grid grid = service.getGrid(ctx);

    assertEquals(
        List.of("lastupdated", "value"),
        grid.getHeaders().stream().map(GridHeader::getName).toList());
    assertEquals(1, grid.getHeight());
  }

  /**
   * {@code ENROLLMENT_OU} is an exact keyword alias of the registration org unit, resolved to
   * {@code ou} while the request is parsed. The grouped set has to see the resolved dimension, or
   * the two spellings of the same dimension behave differently.
   */
  @Test
  void getGridGroupsByOuRequestedWithTheEnrollmentOuKeyword() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(Set.of("ENROLLMENT_OU"), List.of(stubOuDimension("ou1")));
    SqlRowSet rowSet =
        fakeRowSet(new String[] {"ou", "value"}, List.<Object[]>of(new Object[] {"OU1", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    Grid grid = service.getGrid(ctx);

    assertEquals(
        List.of("ou", "value"), grid.getHeaders().stream().map(GridHeader::getName).toList());
    assertEquals(1, grid.getHeight());
  }

  /** {@code enrollmentouname} is an exact keyword alias of {@code ouname}. */
  @Test
  void getGridGroupsByOunameRequestedWithTheEnrollmentOunameKeyword() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(
            Set.of("enrollmentouname"), List.of(stubStaticDimension(StaticDimension.OUNAME)));
    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"ouname", "value"}, List.<Object[]>of(new Object[] {"Ngelehun CHC", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    Grid grid = service.getGrid(ctx);

    assertEquals(
        List.of("ouname", "value"), grid.getHeaders().stream().map(GridHeader::getName).toList());
    assertEquals(1, grid.getHeight());
  }

  /** A filtered item produces no column by design, so it must not be validated as a group by. */
  @Test
  void getGridAllowsFilterOnANonGroupedItem() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(
            Set.of("ou"), List.of(stubOuDimension("ou1"), stubAttributeDimension("cejWyOfXge6")));
    SqlRowSet rowSet =
        fakeRowSet(new String[] {"ou", "value"}, List.<Object[]>of(new Object[] {"OU1", 3}));

    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));

    assertDoesNotThrow(() -> service.getGrid(ctx));
  }

  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>
      aggregateContextParams(
          Set<String> rawDimensions, List<DimensionIdentifier<DimensionParam>> parsedDimensions) {
    return ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
        .typedParsed(TrackedEntityQueryParams.builder().aggregate(true).build())
        .commonRaw(new CommonRequestParams().withDimension(rawDimensions))
        .commonParsed(CommonParsedParams.builder().dimensionIdentifiers(parsedDimensions).build())
        .build();
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

  private DimensionIdentifier<DimensionParam> stubStaticDimension(StaticDimension staticDimension) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            staticDimension.name(), DimensionParamType.DIMENSIONS, UID, List.of());
    return DimensionIdentifier.of(
            ElementWithOffset.emptyElementWithOffset(),
            ElementWithOffset.emptyElementWithOffset(),
            dimensionParam)
        .withDefaultGroupId();
  }

  private DimensionIdentifier<DimensionParam> stubPeriodDimension() {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of()),
            DimensionParamType.DIMENSIONS,
            UID,
            List.of());
    return DimensionIdentifier.of(
            ElementWithOffset.emptyElementWithOffset(),
            ElementWithOffset.emptyElementWithOffset(),
            dimensionParam)
        .withDefaultGroupId();
  }

  /**
   * A stage data element as the request parser produces it: a {@link QueryItem}, which is the shape
   * {@code program.stage.dataElement} falls through to once it matches neither a static dimension
   * nor a dimensional object.
   */
  private DimensionIdentifier<DimensionParam> stubStageDataElementDimension(
      String dataElement, List<String> items) {
    DataElement element = new DataElement();
    element.setUid(dataElement);
    element.setValueType(ValueType.NUMBER);

    Program program = stubProgram();
    ProgramStage programStage = stubProgramStage();

    QueryItem queryItem = new QueryItem(element, program, null, element.getValueType(), null, null);
    queryItem.setProgramStage(programStage);

    DimensionParam dimensionParam =
        DimensionParam.ofObject(queryItem, DimensionParamType.DIMENSIONS, UID, items);
    return DimensionIdentifier.of(
            ElementWithOffset.of(program), ElementWithOffset.of(programStage), dimensionParam)
        .withDefaultGroupId();
  }

  /**
   * A static dimension scoped to a program but not to a stage, which is how a request writes {@code
   * IpHINAT79UW.ENDDATE}. The query reads such a dimension from the enrollment table.
   */
  private DimensionIdentifier<DimensionParam> stubProgramScopedStaticDimension(
      StaticDimension staticDimension, List<String> items) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(staticDimension.name(), DimensionParamType.DIMENSIONS, UID, items);
    return DimensionIdentifier.of(
            ElementWithOffset.of(stubProgram()),
            ElementWithOffset.emptyElementWithOffset(),
            dimensionParam)
        .withDefaultGroupId();
  }

  /**
   * Two offsets of one stage are two dimensions but share one response name, so the rows could not
   * be told apart. The request is rejected rather than answered ambiguously.
   */
  @Test
  void getGridRejectsTwoOffsetsOfTheSameStageDimension() {
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        aggregateContextParams(
            Set.of("A03MvHHogjR.ou:USER_ORGUNIT", "A03MvHHogjR[1].ou:USER_ORGUNIT"),
            List.of(stubStageScopedOuDimension(), stubStageScopedOuDimensionWithOffset(1)));

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.getGrid(ctx));
    assertEquals(ErrorCode.E7259, ex.getErrorCode());
  }

  private DimensionIdentifier<DimensionParam> stubStageScopedOuDimensionWithOffset(int offset) {
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid("ou1");
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(orgUnit)),
            DimensionParamType.DIMENSIONS,
            UID,
            List.of("ou1"));
    return DimensionIdentifier.of(
            ElementWithOffset.of(stubProgram(), offset),
            ElementWithOffset.of(stubProgramStage(), offset),
            dimensionParam)
        .withDefaultGroupId();
  }

  private DimensionIdentifier<DimensionParam> stubStageScopedOuDimension() {
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid("ou1");
    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(orgUnit)),
            DimensionParamType.DIMENSIONS,
            UID,
            List.of("ou1"));
    return DimensionIdentifier.of(
            ElementWithOffset.of(stubProgram()),
            ElementWithOffset.of(stubProgramStage()),
            dimensionParam)
        .withDefaultGroupId();
  }

  private Program stubProgram() {
    Program program = new Program();
    program.setUid("IpHINAT79UW");
    return program;
  }

  private ProgramStage stubProgramStage() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("A03MvHHogjR");
    return programStage;
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
