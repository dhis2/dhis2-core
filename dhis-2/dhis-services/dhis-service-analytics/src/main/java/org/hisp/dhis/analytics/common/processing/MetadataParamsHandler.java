/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.PAGER;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getCustomLabelOrHeaderColumnName;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.supportsCustomLabel;
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
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.MetadataInfo;
import org.hisp.dhis.analytics.common.params.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
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

  /**
   * Appends the metadata to the given {@link Grid} based on the given arguments.
   *
   * @param grid the current {@link Grid}.
   * @param contextParams the {@link ContextParams}.
   * @param contextParams the total of rows found for the current query.
   * @param rowsCount the total of rows found.
   */
  public void handle(
      Grid grid,
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams,
      User user,
      long rowsCount) {
    CommonRequestParams commonRequest = contextParams.getCommonRaw();
    CommonParsedParams commonParsed = contextParams.getCommonParsed();

    if (!commonRequest.isSkipMeta()) {
      // Dimensions.
      List<AnalyticsMetaDataKey> userOrgUnitMetaDataKeys =
          getUserOrgUnitsMetadataKeys(commonParsed);
      Map<String, Object> items =
          new HashMap<>(new MetadataItemsHandler().handle(grid, commonParsed, commonRequest));

      commonParsed
          .getAllDimensionIdentifiers()
          .forEach(
              dimensionIdentifier ->
                  addDimensionIdentifierToItemsIfNeeded(dimensionIdentifier, items));

      getUserOrganisationUnitItems(user, userOrgUnitMetaDataKeys).forEach(items::putAll);
      MetadataInfo metadataInfo = new MetadataInfo();
      metadataInfo.put(ITEMS.getKey(), items);

      metadataInfo.put(
          DIMENSIONS.getKey(), new MetadataDimensionsHandler().handle(grid, commonParsed));

      // Org. Units.
      boolean hierarchyMeta = commonRequest.isHierarchyMeta();
      boolean showHierarchy = commonRequest.isShowHierarchy();

      if (hierarchyMeta || showHierarchy) {
        List<OrganisationUnit> activeOrgUnits = getActiveOrgUnits(grid, commonParsed);
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
      AnalyticsPagingParams pagingParams = commonParsed.getPagingParams();

      if (pagingParams.isPaging()) {
        metadataInfo.put(
            PAGER.getKey(), new MetadataPagingHandler().handle(grid, pagingParams, rowsCount));
      }

      grid.setMetaData(metadataInfo.getMap());
    }
  }

  /**
   * Adds an extra entry to metadata items if needed, i.e. if the dimension identifier is a date
   * dimension which supports custom labels (enrollmentdate, occurreddate) and for any specified
   * dimension identifier that has a prefix.
   *
   * @param dimId the dimension identifier
   * @param items the metadata items
   */
  private void addDimensionIdentifierToItemsIfNeeded(
      DimensionIdentifier<DimensionParam> dimId, Map<String, Object> items) {

    // for dates supporting custom labels, it will add the custom label to items using the full
    // dimension uid as key
    if (supportsCustomLabel(dimId)) {
      items.put(dimId.getKeyNoOffset(), getMetadataWithCustomLabel(dimId));
    }

    // for entries like "abc", it will add a duplicate using dimensionuid (example
    // "program.programstage.abc")
    // from dimensionIdentifier as key
    items.putAll(
        items.entrySet().stream()
            .filter(entry -> isSameDimension(dimId, entry))
            .map(entry -> asEntryWithFullPrefix(dimId, entry))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
  }

  /**
   * Returns the custom label for the given static dimension identifier. Dimension identifier is
   * dimension which supports custom labels (enrollmentdate, occurreddate, ouname)
   *
   * @param dimensionIdentifier the dimension identifier
   * @return the custom label
   */
  private MetadataItem getMetadataWithCustomLabel(
      DimensionIdentifier<DimensionParam> dimensionIdentifier) {

    String customLabel = getCustomLabelOrHeaderColumnName(dimensionIdentifier, false, true);

    MetadataItem metadataItem = new MetadataItem(customLabel);
    metadataItem.setDimensionType(
        dimensionIdentifier.getDimension().getDimensionParamObjectType().getDimensionType());

    return metadataItem;
  }

  private static Entry<String, Object> asEntryWithFullPrefix(
      DimensionIdentifier<DimensionParam> dimensionIdentifier, Entry<String, Object> entry) {
    return Map.entry(dimensionIdentifier.getKeyNoOffset(), entry.getValue());
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
   * @param commonParsed the {@link CommonParsedParams}.
   */
  private List<OrganisationUnit> getActiveOrgUnits(Grid grid, CommonParsedParams commonParsed) {
    List<DimensionalItemObject> orgUnitDimensionOrFilterItems =
        commonParsed.delegate().getOrgUnitsInDimensionOrFilterItems();

    List<OrganisationUnit> organisationUnits = asTypedList(orgUnitDimensionOrFilterItems);

    return getActiveOrganisationUnits(grid, organisationUnits);
  }

  /**
   * Retrieve the analytics metadata keys belong to user organisation unit dimension group
   *
   * @param commonParsed the {@link CommonParsedParams}.
   * @return list of the {@link AnalyticsMetaDataKey}
   */
  private static List<AnalyticsMetaDataKey> getUserOrgUnitsMetadataKeys(
      CommonParsedParams commonParsed) {
    return commonParsed.getDimensionIdentifiers().stream()
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
