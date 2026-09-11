/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.tracker;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.hisp.dhis.analytics.common.ColumnHeader.REGISTRATION_OU;
import static org.hisp.dhis.common.IdScheme.ID;
import static org.hisp.dhis.common.IdScheme.NAME;
import static org.hisp.dhis.common.IdScheme.UID;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Data;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Settings;
import org.hisp.dhis.analytics.data.handler.SchemeIdResponseMapper;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemeIdHandler {
  private final SchemeIdResponseMapper schemeIdResponseMapper;
  private final OrganisationUnitService organisationUnitService;

  /**
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   */
  public void applyScheme(Grid grid, EventQueryParams params) {
    if (params.hasDataIdScheme()) {
      schemeIdResponseMapper.applyOptionAndLegendSetMapping(NAME, grid);
      schemeIdResponseMapper.applyBooleanMapping(params.getDataIdScheme(), grid);
    }

    if (!params.isSkipMeta()) {
      Settings settings = schemeSettings(params);
      SchemeInfo schemeInfo =
          new SchemeInfo(settings, schemeData(params, registrationOuItems(params, grid, settings)));
      schemeIdResponseMapper.applyCustomIdScheme(schemeInfo, grid);
    }
  }

  /** Returns the mapping for resolved request items, used to match aggregate export cells. */
  public Map<String, String> getSchemeIdResponseMap(EventQueryParams params) {
    return schemeIdResponseMapper.getSchemeIdResponseMap(
        new SchemeInfo(
            schemeSettings(params),
            schemeData(params, new LinkedHashSet<>(params.getAllRegistrationOuItems()))));
  }

  private Data schemeData(EventQueryParams params, Set<OrganisationUnit> registrationOus) {
    Set<DimensionalItemObject> dimensionalItems =
        new LinkedHashSet<>(params.getAllDimensionItems());
    dimensionalItems.addAll(registrationOus);
    Set<DimensionalItemObject> organisationUnits =
        new LinkedHashSet<>(params.getOrganisationUnits());
    organisationUnits.addAll(registrationOus);

    return Data.builder()
        .dataElements(params.getAllDataElements())
        .dimensionalItemObjects(dimensionalItems)
        .dataElementOperands(params.getDataElementOperands())
        .options(params.getItemOptions())
        .organizationUnits(List.copyOf(organisationUnits))
        .program(params.getProgram())
        .programStage(params.getProgramStage())
        .indicators(params.getIndicators())
        .programIndicators(params.getProgramIndicators())
        .build();
  }

  private Set<OrganisationUnit> registrationOuItems(
      EventQueryParams params, Grid grid, Settings settings) {
    Set<OrganisationUnit> registrationOus = new LinkedHashSet<>(params.getAllRegistrationOuItems());
    IdScheme scheme =
        firstNonNull(
            settings.getOutputOrgUnitIdScheme(),
            settings.getOutputIdScheme(),
            settings.getDataIdScheme(),
            UID);
    int column = grid.getIndexOfHeader(REGISTRATION_OU.getItem());
    if (!params.hasRegistrationOuDimension()
        || column < 0
        || !settings.hasCustomIdSchemeSet()
        || UID.equals(scheme)
        || scheme.isNull()) {
      return registrationOus;
    }

    Map<String, OrganisationUnit> resolved = new LinkedHashMap<>();
    params.getAllDimensionItems().stream()
        .filter(OrganisationUnit.class::isInstance)
        .map(OrganisationUnit.class::cast)
        .forEach(ou -> resolved.put(ou.getUid(), ou));
    registrationOus.forEach(ou -> resolved.put(ou.getUid(), ou));

    // Query results contain actual registration facilities, not necessarily the requested
    // ancestors.
    Set<String> missing = new LinkedHashSet<>();
    for (Object value : grid.getColumn(column)) {
      if (value instanceof String uid) {
        OrganisationUnit ou = resolved.get(uid);
        if (ou != null) {
          registrationOus.add(ou);
        } else {
          missing.add(uid);
        }
      }
    }
    if (!missing.isEmpty()) {
      registrationOus.addAll(organisationUnitService.getOrganisationUnitsByUid(missing));
    }
    return registrationOus;
  }

  Settings schemeSettings(EventQueryParams params) {
    return Settings.builder()
        .dataIdScheme(getValidScheme(params.getDataIdScheme()))
        .outputDataElementIdScheme(getValidScheme(params.getOutputDataElementIdScheme()))
        .outputDataItemIdScheme(getValidScheme(params.getOutputDataItemIdScheme()))
        .outputIdScheme(getValidScheme(params.getOutputIdScheme()))
        .outputOrgUnitIdScheme(getValidScheme(params.getOutputOrgUnitIdScheme()))
        .outputFormat(params.getOutputFormat())
        .build();
  }

  /**
   * It returns a valid IdScheme, switching ID by UID if applicable. IDs cannot be exposed to users
   * as they are internal PKs at database level.
   *
   * @param idScheme the {@link IdScheme}.
   * @return the {@link IdScheme}.
   */
  public static IdScheme getValidScheme(IdScheme idScheme) {
    if (idScheme == ID) {
      return UID;
    }

    return idScheme;
  }
}
