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
package org.hisp.dhis.analytics.data.handler;

import static com.google.common.collect.ImmutableMap.copyOf;
import static java.util.stream.Collectors.toMap;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.analytics.common.processing.MetadataDimensionsHandler.getDistinctPeriodUids;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getCocNameMap;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getDimensionMetadataItemMap;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.handleGridForDataValueSet;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Data;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Settings;
import org.hisp.dhis.analytics.orgunit.OrgUnitHelper;
import org.hisp.dhis.analytics.util.AnalyticsOrganisationUnitUtils;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Component that populates the Grid metadata. */
@Component
@RequiredArgsConstructor
public class MetadataHandler {
  private final DataQueryService dataQueryService;

  private final SchemeIdResponseMapper schemeIdResponseMapper;

  private final UserService userService;

  /**
   * Adds meta data values to the given grid based on the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the {@link Grid}.
   */
  @Transactional(readOnly = true)
  public void addMetaData(DataQueryParams params, Grid grid) {
    if (!params.isSkipMeta()) {
      Map<String, Object> metaData = new HashMap<>();
      Map<String, Object> internalMetaData = new HashMap<>();

      // -----------------------------------------------------------------
      // Items / names element
      // -----------------------------------------------------------------

      Map<String, Object> items = new HashMap<>(getDimensionMetadataItemMap(params, grid));

      User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
      AnalyticsOrganisationUnitUtils.getUserOrganisationUnitItems(
              currentUser, params.getUserOrganisationUnitsCriteria())
          .forEach(items::putAll);

      metaData.put(ITEMS.getKey(), items);

      // -----------------------------------------------------------------
      // Item order elements
      // -----------------------------------------------------------------
      Map<String, String> cocNameMap = getCocNameMap(params);

      Map<String, Object> dimensionItems = new HashMap<>();

      dimensionItems.put(
          PERIOD_DIM_ID, getDistinctPeriodUids(params.getDimensionOrFilterItems(PERIOD_DIM_ID)));
      dimensionItems.put(CATEGORYOPTIONCOMBO_DIM_ID, Sets.newHashSet(cocNameMap.keySet()));

      for (DimensionalObject dim : params.getDimensionsAndFilters()) {
        if (!dimensionItems.containsKey(dim.getDimension())) {
          dimensionItems.put(dim.getDimension(), getDimensionalItemIds(dim.getItems()));
        }
      }

      metaData.put(DIMENSIONS.getKey(), dimensionItems);

      // -----------------------------------------------------------------
      // Organisation unit hierarchy
      // -----------------------------------------------------------------

      List<OrganisationUnit> organisationUnits =
          asTypedList(params.getDimensionOrFilterItems(ORGUNIT_DIM_ID));

      List<OrganisationUnit> roots = dataQueryService.getUserOrgUnits(params, null);

      List<OrganisationUnit> activeOrgUnits =
          OrgUnitHelper.getActiveOrganisationUnits(grid, organisationUnits);

      if (params.isHierarchyMeta()) {
        metaData.put(ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap(activeOrgUnits, roots));
      }

      if (params.isShowHierarchy()) {
        Map<Object, List<?>> ancestorMap =
            (params.isDownload() ? organisationUnits : activeOrgUnits)
                .stream()
                    .collect(
                        toMap(OrganisationUnit::getUid, ou -> ou.getAncestorNames(roots, true)));

        internalMetaData.put(ORG_UNIT_ANCESTORS.getKey(), ancestorMap);
        metaData.put(
            ORG_UNIT_NAME_HIERARCHY.getKey(), getParentNameGraphMap(activeOrgUnits, roots, true));
      }

      grid.setMetaData(copyOf(metaData));
      grid.setInternalMetaData(copyOf(internalMetaData));
    }
  }

  /**
   * Prepares the given grid to be converted to a data value set, given that the output format is of
   * type DATA_VALUE_SET.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the {@link Grid}.
   */
  void handleDataValueSet(DataQueryParams params, Grid grid) {
    if (params.isOutputFormat(DATA_VALUE_SET) && !params.isSkipHeaders()) {
      handleGridForDataValueSet(params, grid);
    }
  }

  /**
   * Substitutes the metadata of the grid with the identifier scheme meta data property indicated in
   * the query.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the {@link Grid}.
   */
  void applyIdScheme(DataQueryParams params, Grid grid) {
    if (!params.isSkipMeta() && params.hasCustomIdSchemeSet()) {
      SchemeInfo schemeInfo = new SchemeInfo(schemeSettings(params), schemeData(params));
      grid.substituteMetaData(schemeIdResponseMapper.getSchemeIdResponseMap(schemeInfo));
    }
  }

  private Data schemeData(DataQueryParams params) {
    return Data.builder()
        .dataElements(params.getAllDataElements())
        .dimensionalItemObjects(new LinkedHashSet<>(params.getAllDimensionItems()))
        .dataElementOperands(params.getDataElementOperands())
        .organizationUnits(params.getOrganisationUnits())
        .program(params.getProgram())
        .programStage(params.getProgramStage())
        .indicators(params.getIndicators())
        .programIndicators(params.getProgramIndicators())
        .build();
  }

  private Settings schemeSettings(DataQueryParams params) {
    return Settings.builder()
        .outputDataElementIdScheme(params.getOutputDataElementIdScheme())
        .outputDataItemIdScheme(params.getOutputDataItemIdScheme())
        .outputIdScheme(params.getOutputIdScheme())
        .outputOrgUnitIdScheme(params.getOutputOrgUnitIdScheme())
        .outputFormat(params.getOutputFormat())
        .build();
  }
}
