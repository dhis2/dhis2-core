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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.QueryExecutor;
import org.hisp.dhis.analytics.common.SqlQuery;
import org.hisp.dhis.analytics.common.SqlQueryResult;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.params.dimension.StringUid;
import org.hisp.dhis.analytics.common.processing.CommonRequestParamsParser;
import org.hisp.dhis.analytics.common.processing.DimensionIdentifierConverter;
import org.hisp.dhis.analytics.common.processing.MetadataParamsHandler;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreator;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreatorService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * Grounded test for the org unit metaData contract of the tracked entity aggregate endpoint. Drives
 * the real {@link CommonRequestParamsParser} for a {@code dimension=ou} request and the real {@link
 * MetadataParamsHandler} through {@link TrackedEntityAggregateService#getGrid}, verifying that the
 * assembled grid exposes the grouped org units in {@code metaData.dimensions.ou} and {@code
 * metaData.items}.
 */
class TrackedEntityAggregateOuMetadataTest {
  private final SystemSettingsProvider settingsProvider = mock(SystemSettingsProvider.class);
  private final SystemSettings settings = mock(SystemSettings.class);
  private final DataQueryService dataQueryService = mock(DataQueryService.class);
  private final EventDataQueryService eventDataQueryService = mock(EventDataQueryService.class);
  private final ProgramService programService = mock(ProgramService.class);
  private final DimensionIdentifierConverter dimensionIdentifierConverter =
      mock(DimensionIdentifierConverter.class);

  private final CommonRequestParamsParser parser =
      new CommonRequestParamsParser(
          settingsProvider,
          dataQueryService,
          eventDataQueryService,
          programService,
          dimensionIdentifierConverter);

  @SuppressWarnings("unchecked")
  private final QueryExecutor<SqlQuery, SqlQueryResult> queryExecutor = mock(QueryExecutor.class);

  private final SqlQueryCreatorService sqlQueryCreatorService = mock(SqlQueryCreatorService.class);
  private final SqlQueryCreator queryCreator = mock(SqlQueryCreator.class);
  private final ExecutionPlanStore executionPlanStore = mock(ExecutionPlanStore.class);
  private final CommonParamsSecurityManager securityManager =
      mock(CommonParamsSecurityManager.class);
  private final UserService userService = mock(UserService.class);
  private final OrganisationUnitService organisationUnitService =
      mock(OrganisationUnitService.class);

  private final TrackedEntityAggregateService service =
      new TrackedEntityAggregateService(
          queryExecutor,
          sqlQueryCreatorService,
          executionPlanStore,
          securityManager,
          new MetadataParamsHandler(),
          userService,
          organisationUnitService);

  @BeforeEach
  void setUp() {
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(settings.getAnalyticsMaxLimit()).thenReturn(1000);
    CurrentUserUtil.injectUserInSecurityContext(UserDetails.empty().username("tester").build());
  }

  @AfterEach
  void tearDown() {
    CurrentUserUtil.clearSecurityContext();
  }

  @Test
  void metadataExposesGroupedOrgUnitsForBareOuDimension() {
    OrganisationUnit ngelehun = orgUnit("Ngelehun CHC", "a04CZxe0PSe", null);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParamsForOuDimension(new CommonRequestParams().withDimension(Set.of("ou")));

    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"ou", "value"}, List.<Object[]>of(new Object[] {"a04CZxe0PSe", 42}));

    stubQuery(ctx, rowSet);
    when(organisationUnitService.getOrganisationUnitsByUid(Set.of("a04CZxe0PSe")))
        .thenReturn(List.of(ngelehun));

    Grid grid = service.getGrid(ctx);

    Map<String, List<String>> dimensions = getDimensions(grid);
    assertTrue(
        dimensions.containsKey("ou"),
        "metaData.dimensions must contain 'ou'; was: " + dimensions.keySet());
    assertEquals(List.of("a04CZxe0PSe"), dimensions.get("ou"));

    Map<String, MetadataItem> items = getItems(grid);
    assertTrue(
        items.containsKey("a04CZxe0PSe"),
        "metaData.items must map the grouped org unit uid; was: " + items.keySet());
    assertEquals("Ngelehun CHC", items.get("a04CZxe0PSe").getName());
  }

  @Test
  void metadataKeepsGroupedOrgUnitOrderAndDetailsForResolvedUnitsOnly() {
    OrganisationUnit ngelehun = orgUnit("Ngelehun CHC", "a04CZxe0PSe", "NGELEHUN");
    OrganisationUnit mabesseneh = orgUnit("Mabesseneh CHP", "b04CZxe0PSe", "MABESS");
    CommonRequestParams request =
        new CommonRequestParams().withDimension(Set.of("ou")).withIncludeMetadataDetails(true);
    ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx =
        contextParamsForOuDimension(request);

    SqlRowSet rowSet =
        fakeRowSet(
            new String[] {"ou", "value"},
            List.of(
                new Object[] {"a04CZxe0PSe", 42},
                new Object[] {"b04CZxe0PSe", 13},
                new Object[] {"a04CZxe0PSe", 7},
                new Object[] {null, 3},
                new Object[] {"unresolved01", 1}));

    stubQuery(ctx, rowSet);
    when(organisationUnitService.getOrganisationUnitsByUid(
            Set.of("a04CZxe0PSe", "b04CZxe0PSe", "unresolved01")))
        .thenReturn(List.of(mabesseneh, ngelehun));

    Grid grid = service.getGrid(ctx);

    assertEquals(
        List.of("a04CZxe0PSe", "b04CZxe0PSe", "unresolved01"), getDimensions(grid).get("ou"));

    Map<String, MetadataItem> items = getItems(grid);
    assertTrue(items.containsKey("a04CZxe0PSe"));
    assertTrue(items.containsKey("b04CZxe0PSe"));
    assertFalse(items.containsKey("unresolved01"));
    assertEquals("Ngelehun CHC", items.get("a04CZxe0PSe").getName());
    assertEquals("a04CZxe0PSe", items.get("a04CZxe0PSe").getUid());
    assertEquals("NGELEHUN", items.get("a04CZxe0PSe").getCode());
    assertEquals("Mabesseneh CHP", items.get("b04CZxe0PSe").getName());
    assertEquals("b04CZxe0PSe", items.get("b04CZxe0PSe").getUid());
    assertEquals("MABESS", items.get("b04CZxe0PSe").getCode());
  }

  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>
      contextParamsForOuDimension(CommonRequestParams request) {
    when(dimensionIdentifierConverter.fromString(anyList(), eq("ou")))
        .thenReturn(
            DimensionIdentifier.of(
                ElementWithOffset.emptyElementWithOffset(),
                ElementWithOffset.emptyElementWithOffset(),
                StringUid.of("ou")));

    CommonParsedParams commonParsed = parser.parse(request);
    return ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
        .typedParsed(TrackedEntityQueryParams.builder().aggregate(true).build())
        .commonRaw(request)
        .commonParsed(commonParsed)
        .build();
  }

  private void stubQuery(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> ctx, SqlRowSet rowSet) {
    when(sqlQueryCreatorService.getSqlQueryCreator(ctx)).thenReturn(queryCreator);
    when(queryCreator.createForSelect()).thenReturn(mock(SqlQuery.class));
    when(queryExecutor.find(any())).thenReturn(new SqlQueryResult(rowSet));
  }

  private OrganisationUnit orgUnit(String name, String uid, String code) {
    OrganisationUnit organisationUnit = new OrganisationUnit(name);
    organisationUnit.setUid(uid);
    organisationUnit.setCode(code);
    return organisationUnit;
  }

  private Map<String, List<String>> getDimensions(Grid grid) {
    Map<String, Object> metaData = grid.getMetaData();
    assertNotNull(metaData, "metaData must be present");

    @SuppressWarnings("unchecked")
    Map<String, List<String>> dimensions = (Map<String, List<String>>) metaData.get("dimensions");
    assertNotNull(dimensions, "metaData.dimensions must be present");
    return dimensions;
  }

  private Map<String, MetadataItem> getItems(Grid grid) {
    Map<String, Object> metaData = grid.getMetaData();
    assertNotNull(metaData, "metaData must be present");

    @SuppressWarnings("unchecked")
    Map<String, MetadataItem> items = (Map<String, MetadataItem>) metaData.get("items");
    assertNotNull(items, "metaData.items must be present");
    return items;
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
