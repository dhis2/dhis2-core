/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.hisp.dhis.analytics.AnalyticsConstants.KEY_LEVEL;
import static org.hisp.dhis.analytics.AnalyticsConstants.KEY_ORGUNIT_GROUP;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.hisp.dhis.common.IdentifiableObjectUtils.SEPARATOR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.data.DimensionalObjectProvider;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OrganisationUnitResolver {

  private final DimensionalObjectProvider dimensionalObjectProducer;

  private final OrganisationUnitService organisationUnitService;

  private final IdentifiableObjectManager idObjectManager;

  /**
   * Resolve organisation units like ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN;LEVEL-XXX;OUGROUP-XXX
   * into a list of organisation unit dimension uids.
   *
   * @param queryFilter the query filter containing the organisation unit filter
   * @param userOrgUnits the user organisation units
   * @return the organisation unit dimension uids
   */
  public String resolveOrgUnits(QueryFilter queryFilter, List<OrganisationUnit> userOrgUnits) {
    List<String> filterItem = QueryFilter.getFilterItems(queryFilter.getFilter());
    List<String> orgUnitDimensionUid =
        dimensionalObjectProducer.getOrgUnitDimensionUid(filterItem, userOrgUnits);
    return String.join(OPTION_SEP, orgUnitDimensionUid);
  }

  /**
   * Returns a map of metadata item identifiers and {@link MetadataItem} for organisation unit data
   * elements.
   *
   * @param params the {@link EventQueryParams}.
   * @return a map.
   */
  public Map<String, MetadataItem> getMetadataItemsForOrgUnitDataElements(EventQueryParams params) {
    List<String> orgUnitIds = new ArrayList<>();
    for (QueryItem queryItem : params.getItems()) {
      if (queryItem.getValueType().isOrganisationUnit()) {
        for (QueryFilter queryFilter : queryItem.getFilters()) {
          String resolveOrgUnits = resolveOrgUnits(queryFilter, params.getUserOrgUnits());
          if (StringUtils.isNotBlank(resolveOrgUnits)) {
            orgUnitIds.addAll(Arrays.asList(resolveOrgUnits.split(OPTION_SEP)));
          }
        }
      }
    }

    if (orgUnitIds.isEmpty()) {
      return Map.of();
    }

    return organisationUnitService.getOrganisationUnitsByUid(orgUnitIds).stream()
        .collect(
            Collectors.toMap(OrganisationUnit::getUid, orgUnit -> toMetadataItem(orgUnit, params)));
  }

  /**
   * Returns a {@link MetadataItem} based on the given organisation unit and query parameters.
   *
   * @param orgUnit the {@link OrganisationUnit}.
   * @param params the {@link EventQueryParams}.
   * @return a {@link MetadataItem}.
   */
  private MetadataItem toMetadataItem(OrganisationUnit orgUnit, EventQueryParams params) {
    return new MetadataItem(
        orgUnit.getDisplayProperty(params.getDisplayProperty()),
        params.isIncludeMetadataDetails() ? orgUnit.getUid() : null,
        orgUnit.getCode());
  }

  /**
   * Resolve organisation units like ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN;LEVEL-XXX;OUGROUP-XXX
   * into a list of organisation unit dimension uids.
   *
   * @param params the event query parameters
   * @param item the query item
   * @return the list of organisation unit dimension uids
   */
  public List<String> resolveOrgUnis(EventQueryParams params, QueryItem item) {
    return item.getFilters().stream()
        .map(queryFilter -> resolveOrgUnits(queryFilter, params.getUserOrgUnits()))
        .map(s -> s.split(OPTION_SEP))
        .flatMap(Arrays::stream)
        .distinct()
        .toList();
  }

  /**
   * This method loads an OU dimension {@link DimensionalItemObject} based on the given dimension
   * identifier. It returns an instance of {@link BaseDimensionalItemObject}, {@link
   * OrganisationUnitGroup} and {@link OrganisationUnit}, depending on the identifier.
   *
   * @param dimensionUid the dimension uid related to the OU dimension. ie: Vth0fbpFcsO,
   *     OU_GROUP-CXw2yu5fodb.
   * @param idScheme the {@link IdScheme}.
   * @return the OU {@link DimensionalItemObject} or null if no object can be loaded.
   */
  public DimensionalItemObject loadOrgUnitDimensionalItem(
      @Nonnull String dimensionUid, @Nonnull IdScheme idScheme) {
    if (dimensionUid.startsWith(KEY_LEVEL)) {
      OrganisationUnitLevel level =
          idObjectManager.getObject(
              OrganisationUnitLevel.class, idScheme, substringAfterLast(dimensionUid, SEPARATOR));

      if (level != null) {
        BaseDimensionalItemObject dim = new BaseDimensionalItemObject();
        dim.setUid(level.getUid());
        dim.setName(level.getDisplayName());
        dim.setCode(level.getCode());

        return dim;
      }
    } else if (dimensionUid.startsWith(KEY_ORGUNIT_GROUP)) {
      return idObjectManager.getObject(
          OrganisationUnitGroup.class, idScheme, substringAfterLast(dimensionUid, SEPARATOR));
    } else if (isValidUid(dimensionUid)) {
      return idObjectManager.getObject(OrganisationUnit.class, idScheme, dimensionUid);
    }

    return null;
  }
}
