/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.data.DimensionalObjectProducer;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OrganisationUnitResolver {

  private final DimensionalObjectProducer dimensionalObjectProducer;

  private final OrganisationUnitService organisationUnitService;

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
}
