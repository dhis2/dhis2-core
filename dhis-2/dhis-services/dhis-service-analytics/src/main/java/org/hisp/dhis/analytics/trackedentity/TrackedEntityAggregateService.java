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

import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toCollection;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.trackedentity.query.TrackedEntityFields.getAggregateGridHeaders;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getRoundedValueObject;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.withExceptionHandling;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.QueryExecutor;
import org.hisp.dhis.analytics.common.SqlQuery;
import org.hisp.dhis.analytics.common.SqlQueryResult;
import org.hisp.dhis.analytics.common.params.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.processing.MetadataParamsHandler;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.AggregateQueryBuilder;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreator;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlQueryCreatorService;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.ExecutionPlan;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

/**
 * Executes tracked-entity aggregate (grouped) queries and assembles the grouped grid.
 *
 * <p>The grid carries one column per grouped dimension holding the dimension-item UID, plus a
 * trailing numeric {@code value} column. Grouped org unit UIDs are resolved to display names in
 * {@code metaData.items}. Because the grid is assembled directly (bypassing the row-level {@code
 * GridAdaptor} post-processing), the following request parameters are not applied to aggregate
 * responses:
 *
 * <ul>
 *   <li>{@code outputIdScheme} / {@code dataIdScheme} — rows always carry raw dimension-item UIDs
 *   <li>option-set / legend-set value mapping — attribute columns show the stored value, not the
 *       option or legend representation
 *   <li>{@code hierarchyMeta} / {@code showHierarchy} — the org unit parent-graph maps are not
 *       built
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TrackedEntityAggregateService {

  private static final GridHeader VALUE_HEADER =
      new GridHeader("value", "Value", ValueType.NUMBER, false, false);

  private final QueryExecutor<SqlQuery, SqlQueryResult> queryExecutor;

  private final SqlQueryCreatorService sqlQueryCreatorService;

  private final ExecutionPlanStore executionPlanStore;

  private final CommonParamsSecurityManager securityManager;

  private final MetadataParamsHandler metadataParamsHandler;

  private final UserService userService;

  private final OrganisationUnitService organisationUnitService;

  public Grid getGrid(
      @Nonnull ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    CommonParsedParams commonParams = contextParams.getCommonParsed();
    TrackedEntityQueryParams teParams = contextParams.getTypedParsed();

    securityManager.decideAccess(commonParams, singleton(teParams.getTrackedEntityType()));
    securityManager.applyOrganisationUnitConstraint(commonParams);
    securityManager.applyDimensionConstraints(commonParams);

    validateSorting(contextParams);

    SqlQueryCreator queryCreator = sqlQueryCreatorService.getSqlQueryCreator(contextParams);

    Optional<SqlQueryResult> result =
        withExceptionHandling(() -> queryExecutor.find(queryCreator.createForSelect()));

    long rowsCount = 0;
    AnalyticsPagingParams paging = commonParams.getPagingParams();
    if (paging.showTotalPages()) {
      rowsCount =
          withExceptionHandling(() -> queryExecutor.count(queryCreator.createForCount()))
              .orElse(0L);
    }

    Grid grid = new ListGrid();
    getAggregateGridHeaders(contextParams).forEach(grid::addHeader);
    grid.addHeader(VALUE_HEADER);

    result.ifPresent(
        r -> addGroupedRows(grid, r.result(), contextParams.getCommonRaw().isSkipRounding()));

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    metadataParamsHandler.handle(
        grid, withGroupedDimensionsOnly(contextParams), currentUser, rowsCount);
    addGroupedOrgUnitMetadata(grid, contextParams);
    return grid;
  }

  /**
   * Scopes the metadata inputs to the dimensions the aggregate query actually groups by. The TE
   * mapper injects all the tracked entity type's attributes into the parsed dimensions for
   * row-level display; without this filter the shared {@link MetadataParamsHandler} would contain
   * those non-grouped attributes in {@code metaData.dimensions}/{@code metaData.items} even though
   * they are absent from the aggregate headers and rows. Grouped dimensions come from the single
   * source of truth in {@link AggregateQueryBuilder#getGroupedDimensionKeys}.
   */
  private ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams>
      withGroupedDimensionsOnly(
          ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    Set<String> groupedKeys = AggregateQueryBuilder.getGroupedDimensionKeys(contextParams);
    CommonParsedParams commonParsed = contextParams.getCommonParsed();

    CommonParsedParams scoped =
        commonParsed.toBuilder()
            .dimensionIdentifiers(
                commonParsed.getDimensionIdentifiers().stream()
                    .filter(dimension -> groupedKeys.contains(dimension.getKey()))
                    .toList())
            .parsedHeaders(
                commonParsed.getParsedHeaders().stream()
                    .filter(dimension -> groupedKeys.contains(dimension.getKey()))
                    .collect(toCollection(LinkedHashSet::new)))
            .build();

    return contextParams.toBuilder().commonParsed(scoped).build();
  }

  /**
   * Rejects sort parameters that reference a dimension the aggregate query does not group by.
   * Sorting on a non-grouped column produces invalid grouped SQL (an {@code ORDER BY} on a column
   * absent from {@code GROUP BY}). The grouped dimensions are the single source of truth in {@link
   * AggregateQueryBuilder#getGroupedDimensionKeys}.
   */
  private void validateSorting(
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    List<AnalyticsSortingParams> orderParams = contextParams.getCommonParsed().getOrderParams();
    if (orderParams.isEmpty()) {
      return;
    }

    Set<String> groupedKeys = AggregateQueryBuilder.getGroupedDimensionKeys(contextParams);
    for (AnalyticsSortingParams orderParam : orderParams) {
      String key = orderParam.getOrderBy().getKey();
      if (!groupedKeys.contains(key)) {
        throw new IllegalQueryException(new ErrorMessage(ErrorCode.E7252, key));
      }
    }
  }

  /**
   * Resolves the org units grouped by the {@code ou} dimension into the grid metaData. A bare
   * {@code ou} dimension is parsed as a static dimension (no {@link
   * org.hisp.dhis.common.DimensionalObject}), so the shared {@link MetadataParamsHandler} never
   * emits it. This adds {@code metaData.dimensions.ou} (the grouped org unit uids) and {@code
   * metaData.items} entries mapping each uid to its display name, so clients can resolve org unit
   * uids to names. The org unit uids are read from the {@code ou} column of the grid rows and
   * resolved to {@link OrganisationUnit} objects.
   */
  private void addGroupedOrgUnitMetadata(
      Grid grid,
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    GridMetadata metadata = getGridMetadata(grid);
    if (metadata == null || metadata.isEmpty()) {
      return;
    }

    int ouColumnIndex = grid.getIndexOfHeader(ORGUNIT_DIM_ID);
    if (ouColumnIndex < 0) {
      return;
    }

    Set<String> orgUnitUids = getGroupedOrgUnitUids(grid, ouColumnIndex);
    if (orgUnitUids.isEmpty()) {
      return;
    }

    DisplayProperty displayProperty = contextParams.getCommonRaw().getDisplayProperty();
    boolean includeMetadataDetails = contextParams.getCommonRaw().isIncludeMetadataDetails();

    Map<String, OrganisationUnit> orgUnitsByUid = getOrgUnitsByUid(orgUnitUids);
    addOrgUnitItems(
        metadata.items(), orgUnitUids, orgUnitsByUid, displayProperty, includeMetadataDetails);
    addOrgUnitDimension(metadata.dimensions(), orgUnitUids);
  }

  private Set<String> getGroupedOrgUnitUids(Grid grid, int ouColumnIndex) {
    Set<String> orgUnitUids = new LinkedHashSet<>();
    for (List<Object> row : grid.getRows()) {
      Object value = row.get(ouColumnIndex);
      if (value != null) {
        orgUnitUids.add(String.valueOf(value));
      }
    }
    return orgUnitUids;
  }

  private Map<String, OrganisationUnit> getOrgUnitsByUid(Set<String> orgUnitUids) {
    Map<String, OrganisationUnit> orgUnitsByUid = new LinkedHashMap<>();
    organisationUnitService
        .getOrganisationUnitsByUid(orgUnitUids)
        .forEach(orgUnit -> orgUnitsByUid.put(orgUnit.getUid(), orgUnit));
    return orgUnitsByUid;
  }

  private void addOrgUnitItems(
      Map<String, Object> items,
      Set<String> orgUnitUids,
      Map<String, OrganisationUnit> orgUnitsByUid,
      DisplayProperty displayProperty,
      boolean includeMetadataDetails) {
    if (items == null) {
      return;
    }

    for (String uid : orgUnitUids) {
      OrganisationUnit orgUnit = orgUnitsByUid.get(uid);
      if (orgUnit != null) {
        items.put(
            uid,
            new MetadataItem(
                orgUnit.getDisplayProperty(displayProperty),
                includeMetadataDetails ? orgUnit : null));
      }
    }
  }

  private void addOrgUnitDimension(Map<String, Object> dimensions, Set<String> orgUnitUids) {
    if (dimensions == null) {
      return;
    }

    dimensions.put(ORGUNIT_DIM_ID, List.copyOf(orgUnitUids));
  }

  private GridMetadata getGridMetadata(Grid grid) {
    Map<String, Object> metaData = grid.getMetaData();
    if (metaData == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> items = (Map<String, Object>) metaData.get(ITEMS.getKey());
    @SuppressWarnings("unchecked")
    Map<String, Object> dimensions = (Map<String, Object>) metaData.get(DIMENSIONS.getKey());
    return new GridMetadata(items, dimensions);
  }

  private record GridMetadata(Map<String, Object> items, Map<String, Object> dimensions) {
    boolean isEmpty() {
      return items == null && dimensions == null;
    }
  }

  private void addGroupedRows(Grid grid, SqlRowSet rs, boolean skipRounding) {
    List<String> columns = grid.getHeaders().stream().map(GridHeader::getName).toList();
    DataQueryParams rounding = DataQueryParams.newBuilder().withSkipRounding(skipRounding).build();
    String valueColumn = VALUE_HEADER.getName();
    while (rs.next()) {
      grid.addRow();
      columns.forEach(
          col ->
              grid.addValue(
                  valueColumn.equals(col)
                      ? getRoundedValueObject(rounding, toDouble(rs.getObject(col)))
                      : rs.getObject(col)));
    }
  }

  /**
   * Normalizes a decimal-typed aggregate (PostgreSQL returns numeric aggregates as BigDecimal) to a
   * Double so the shared rounding applies; integral counts pass through unchanged.
   */
  private static Object toDouble(Object value) {
    return value instanceof BigDecimal decimal ? decimal.doubleValue() : value;
  }

  public Grid getGridExplain(
      @Nonnull ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams) {
    validateSorting(contextParams);

    Grid grid = new ListGrid();
    String explainId = randomUUID().toString();
    SqlQueryCreator queryCreator = sqlQueryCreatorService.getSqlQueryCreator(contextParams);

    withExceptionHandling(
        () -> executionPlanStore.addExecutionPlan(explainId, queryCreator.createForSelect()));
    if (contextParams.getCommonParsed().getPagingParams().showTotalPages()) {
      withExceptionHandling(
          () -> executionPlanStore.addExecutionPlan(explainId, queryCreator.createForCount()));
    }
    List<ExecutionPlan> plans = executionPlanStore.getExecutionPlans(explainId);
    grid.addPerformanceMetrics(plans);
    return grid;
  }
}
