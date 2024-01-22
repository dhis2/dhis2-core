/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.PAGER;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.ENROLLMENTDATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.EXECUTIONDATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension.INCIDENTDATE;
import static org.hisp.dhis.analytics.orgunit.OrgUnitHelper.getActiveOrganisationUnits;
import static org.hisp.dhis.analytics.util.AnalyticsOrganisationUnitUtils.getUserOrganisationUnitItems;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.common.MetadataInfo;
import org.hisp.dhis.analytics.common.params.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.params.CommonParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * Component that, internally, handles all data structure and maps required by the metadata object.
 * It works on top of common objects, so it can be reused by different analytics services/endpoints.
 *
 * <p>This class and methods were pulled from other part of the code, so we could have a centralized
 * way to generate and keep the logic related to analytics metadata elements. Light changes were
 * applied to make the code slightly cleaner. Major structural changes were not applied to reduce
 * the risk of bugs.
 */
@Component
public class MetadataParamsHandler {
  private static final String DOT = ".";
  private static final String ORG_UNIT_DIM = "ou";

  private static final Map<StaticDimension, Function<DimensionIdentifier<DimensionParam>, String>>
      STATIC_DIMENSION_MAPPER =
          Map.of(
              ENROLLMENTDATE,
              di -> di.getProgram().getElement().getEnrollmentDateLabel(),
              INCIDENTDATE,
              di -> di.getProgram().getElement().getIncidentDateLabel(),
              EXECUTIONDATE,
              di -> di.getProgramStage().getElement().getExecutionDateLabel());

  /**
   * Appends the metadata to the given {@link Grid} based on the given arguments.
   *
   * @param grid the current {@link Grid}.
   * @param commonParams the {@link CommonParams}.
   * @param rowsCount the total of rows found for the current query.
   */
  public void handle(Grid grid, CommonParams commonParams, User user, long rowsCount) {
    if (!commonParams.isSkipMeta()) {

      // Dimensions.
      List<AnalyticsMetaDataKey> userOrgUnitMetaDataKeys =
          getUserOrgUnitsMetadataKeys(commonParams);
      Map<String, Object> items =
          new HashMap<>(new MetadataItemsHandler().handle(grid, commonParams));

      commonParams
          .getAllDimensionIdentifiers()
          .forEach(
              dimensionIdentifier ->
                  addDimensionIdentifierToItemsIfNeeded(dimensionIdentifier, items));

      getUserOrganisationUnitItems(user, userOrgUnitMetaDataKeys).forEach(items::putAll);
      MetadataInfo metadataInfo = new MetadataInfo();
      metadataInfo.put(ITEMS.getKey(), items);

      metadataInfo.put(
          DIMENSIONS.getKey(), new MetadataDimensionsHandler().handle(grid, commonParams));

      // Org. Units.
      boolean hierarchyMeta = commonParams.isHierarchyMeta();
      boolean showHierarchy = commonParams.isShowHierarchy();

      if (hierarchyMeta || showHierarchy) {
        List<OrganisationUnit> activeOrgUnits = getActiveOrgUnits(grid, commonParams);
        Set<OrganisationUnit> roots = getUserOrgUnits(user);

        if (hierarchyMeta) {
          metadataInfo.put(ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap(activeOrgUnits, roots));
        }

        if (showHierarchy) {
          metadataInfo.put(
              ORG_UNIT_NAME_HIERARCHY.getKey(), getParentNameGraphMap(activeOrgUnits, roots, true));
        }
      }

      // Paging.
      AnalyticsPagingParams pagingParams = commonParams.getPagingParams();

      if (pagingParams.isPaging()) {
        metadataInfo.put(
            PAGER.getKey(), new MetadataPagingHandler().handle(grid, pagingParams, rowsCount));
      }

      grid.setMetaData(metadataInfo.getMap());
    }
  }

  /**
   * Adds an extra entry to metadata items if needed, i.e. if the dimension identifier is a date
   * dimension which supports custom labels (enrollmentdate, incidentdate, executiondate) and for
   * any specified dimension identifier that has a prefix.
   *
   * @param dimensionIdentifier the dimension identifier
   * @param items the metadata items
   */
  private void addDimensionIdentifierToItemsIfNeeded(
      DimensionIdentifier<DimensionParam> dimensionIdentifier, Map<String, Object> items) {

    // for dates supporting custom labels, it will add the custom label to items using the full
    // dimension uid as key
    if (supportsCustomLabel(dimensionIdentifier)) {
      items.put(dimensionIdentifier.getKey(), getCustomLabel(dimensionIdentifier));
    }

    // for entries like "abc", it will add a duplicate using dimensionuid (example
    // "program.programstage.abc")
    // from dimensionIdentifier as key
    items.putAll(
        items.entrySet().stream()
            .filter(entry -> isSameDimension(dimensionIdentifier, entry))
            .map(entry -> asEntryWithFullPrefix(dimensionIdentifier, entry))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
  }

  /**
   * Returns the custom label for the given dimension identifier. Dimension identifier is a date
   * dimension which supports custom labels (enrollmentdate, incidentdate, executiondate)
   *
   * @param dimensionIdentifier the dimension identifier
   * @return the custom label
   */
  private MetadataItem getCustomLabel(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    return new MetadataItem(
        STATIC_DIMENSION_MAPPER.entrySet().stream()
            .filter(
                entry -> entry.getKey() == dimensionIdentifier.getDimension().getStaticDimension())
            .findFirst()
            .map(
                entry ->
                    defaultIfBlank(
                        entry.getValue().apply(dimensionIdentifier),
                        entry.getKey().getHeaderColumnName()))
            .orElse(null));
  }

  private boolean supportsCustomLabel(DimensionIdentifier<DimensionParam> dimensionIdentifier) {
    DimensionParam dimensionParam = dimensionIdentifier.getDimension();
    return dimensionParam.getStaticDimension() != null
        && (dimensionParam.getStaticDimension() == ENROLLMENTDATE
            || dimensionParam.getStaticDimension() == INCIDENTDATE
            || dimensionParam.getStaticDimension() == EXECUTIONDATE);
  }

  private static Entry<String, Object> asEntryWithFullPrefix(
      DimensionIdentifier<DimensionParam> dimensionIdentifier, Entry<String, Object> entry) {
    return Map.entry(dimensionIdentifier.getKey(), entry.getValue());
  }

  private static boolean isSameDimension(
      DimensionIdentifier<DimensionParam> dimensionIdentifier, Entry<String, Object> entry) {
    return entry.getKey().equals(dimensionIdentifier.getDimension().getUid());
  }

  private Set<OrganisationUnit> getUserOrgUnits(User user) {
    return user != null ? user.getOrganisationUnits() : emptySet();
  }

  /**
   * Returns only the Org. Units currently present in the current grid rows.
   *
   * @param grid the current {@link Grid} object.
   * @param commonParams the {@link CommonParams}.
   */
  private List<OrganisationUnit> getActiveOrgUnits(Grid grid, CommonParams commonParams) {
    List<DimensionalItemObject> orgUnitDimensionOrFilterItems =
        commonParams.delegate().getOrgUnitDimensionOrFilterItems();

    List<OrganisationUnit> organisationUnits = asTypedList(orgUnitDimensionOrFilterItems);

    return getActiveOrganisationUnits(grid, organisationUnits);
  }

  /**
   * Retrieve the analytics metadata keys belong to user organisation unit dimension group
   *
   * @param commonParams the {@link CommonParams}.
   * @return list of the {@link AnalyticsMetaDataKey}
   */
  private static List<AnalyticsMetaDataKey> getUserOrgUnitsMetadataKeys(CommonParams commonParams) {
    return commonParams.getDimensionIdentifiers().stream()
        .filter(dimensionIdentifier -> dimensionIdentifier.toString().equals(ORG_UNIT_DIM))
        .flatMap(dimensionIdentifier -> dimensionIdentifier.getDimension().getItems().stream())
        .flatMap(item -> item.getValues().stream())
        .filter(
            item ->
                item.equals(AnalyticsMetaDataKey.USER_ORGUNIT.getKey())
                    || item.equals(AnalyticsMetaDataKey.USER_ORGUNIT_CHILDREN.getKey())
                    || item.equals(AnalyticsMetaDataKey.USER_ORGUNIT_GRANDCHILDREN.getKey()))
        .map(
            item -> {
              if (item.equals(AnalyticsMetaDataKey.USER_ORGUNIT.getKey())) {
                return AnalyticsMetaDataKey.USER_ORGUNIT;
              }
              if (item.equals(AnalyticsMetaDataKey.USER_ORGUNIT_CHILDREN.getKey())) {
                return AnalyticsMetaDataKey.USER_ORGUNIT_CHILDREN;
              }
              return AnalyticsMetaDataKey.USER_ORGUNIT_GRANDCHILDREN;
            })
        .toList();
  }

  /**
   * Returns the query {@link QueryItem} identifier. It may be prefixed with its program stage
   * identifier (if one exists).
   *
   * @param item the {@link QueryItem}.
   * @return the {@link QueryItem} uid with a prefix (if applicable).
   */
  static String getItemUid(QueryItem item) {
    String uid = item.getItem().getUid();

    if (item.hasProgramStage()) {
      uid = joinWith(DOT, item.getProgramStage().getUid(), uid);
    }

    return uid;
  }
}
