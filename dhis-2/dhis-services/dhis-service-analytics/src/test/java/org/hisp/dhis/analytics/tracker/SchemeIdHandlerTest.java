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
package org.hisp.dhis.analytics.tracker;

import static org.hisp.dhis.analytics.event.EventQueryParams.fromDataQueryParams;
import static org.hisp.dhis.common.IdScheme.CODE;
import static org.hisp.dhis.common.IdScheme.ID;
import static org.hisp.dhis.common.IdScheme.NAME;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.common.scheme.SchemeInfo.Settings;
import org.hisp.dhis.analytics.data.handler.SchemeIdResponseMapper;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SchemeIdHandlerTest {
  private final OrganisationUnitService organisationUnitService =
      mock(OrganisationUnitService.class);
  private final SchemeIdHandler handler =
      new SchemeIdHandler(
          new SchemeIdResponseMapper(mock(I18nManager.class)), organisationUnitService);

  @Test
  void schemeSettingsReturnsUIDWhenID() {
    EventQueryParams params =
        fromDataQueryParams(DataQueryParams.newBuilder().withOutputIdScheme(ID).build());

    Settings settings = handler.schemeSettings(params);

    assertEquals(UID, settings.getOutputIdScheme());
  }

  @ParameterizedTest
  @MethodSource("outputSchemes")
  void mapsRegistrationOuWithoutOrdinaryOuDimension(IdScheme scheme, String expected) {
    OrganisationUnit ou = registrationOu();
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withRegistrationOuDimension(List.of(ou))
            .withOutputIdScheme(scheme)
            .build();
    Grid grid = registrationGrid(ou.getUid());
    Map<String, Object> metadata = Map.of("items", Map.of(ou.getUid(), Map.of("name", "Bo")));
    grid.getMetaData().putAll(metadata);

    handler.applyScheme(grid, params);

    assertEquals(List.of(List.of(expected)), grid.getRows());
    assertEquals(metadata, grid.getMetaData());
    verifyNoInteractions(organisationUnitService);
  }

  static Stream<Arguments> outputSchemes() {
    return Stream.of(
        Arguments.of(CODE, "OU_264"),
        Arguments.of(NAME, "Bo"),
        Arguments.of(UID, "O6uvpzGd5pu"),
        Arguments.of(ID, "O6uvpzGd5pu"),
        Arguments.of(null, "O6uvpzGd5pu"));
  }

  @ParameterizedTest
  @MethodSource("orgUnitSchemes")
  void respectsOrgUnitSchemePrecedence(IdScheme general, IdScheme orgUnit, String expected) {
    OrganisationUnit ou = registrationOu();
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withRegistrationOuDimension(List.of(ou))
            .withOutputIdScheme(general)
            .build();
    params.setOutputOrgUnitIdScheme(orgUnit);
    Grid grid = registrationGrid(ou.getUid());

    handler.applyScheme(grid, params);

    assertEquals(List.of(List.of(expected)), grid.getRows());
    verifyNoInteractions(organisationUnitService);
  }

  static Stream<Arguments> orgUnitSchemes() {
    return Stream.of(
        Arguments.of(CODE, UID, "O6uvpzGd5pu"),
        Arguments.of(UID, CODE, "OU_264"),
        Arguments.of(NAME, CODE, "OU_264"),
        Arguments.of(null, NAME, "Bo"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void resolvesDistinctReturnedFacilitiesInOneBatch(boolean bareDimension) {
    OrganisationUnit first = createOrganisationUnit('B');
    first.setCode("FACILITY_B");
    OrganisationUnit second = createOrganisationUnit('C');
    second.setCode("FACILITY_C");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withRegistrationOuDimension(bareDimension ? List.of() : List.of(registrationOu()))
            .withOutputIdScheme(CODE)
            .build();
    when(organisationUnitService.getOrganisationUnitsByUid(Set.of(first.getUid(), second.getUid())))
        .thenReturn(List.of(first, second));
    Grid grid = registrationGrid(first.getUid(), second.getUid(), first.getUid(), null);

    handler.applyScheme(grid, params);

    assertEquals(
        List.of("FACILITY_B", "FACILITY_C", "FACILITY_B"), grid.getColumn(0).subList(0, 3));
    assertNull(grid.getValue(3, 0));
    verify(organisationUnitService)
        .getOrganisationUnitsByUid(Set.of(first.getUid(), second.getUid()));
    verifyNoMoreInteractions(organisationUnitService);
  }

  @Test
  void reusesRegistrationFilterAndOrdinaryOuItems() {
    OrganisationUnit filterOu = registrationOu();
    OrganisationUnit ordinaryOu = createOrganisationUnit('B');
    ordinaryOu.setCode("FACILITY_B");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withRegistrationOuDimension(List.of())
            .withRegistrationOuFilter(List.of(filterOu))
            .withOrganisationUnits(List.of(ordinaryOu))
            .withOutputIdScheme(NAME)
            .build();
    params.setOutputOrgUnitIdScheme(CODE);
    Grid grid = registrationGrid(filterOu.getUid(), ordinaryOu.getUid());

    handler.applyScheme(grid, params);

    assertEquals(List.of(List.of("OU_264"), List.of("FACILITY_B")), grid.getRows());
    verifyNoInteractions(organisationUnitService);
  }

  @Test
  void overlappingOrdinaryAndRegistrationOuHaveTheSameEncoding() {
    OrganisationUnit ou = registrationOu();
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withOrganisationUnits(List.of(ou))
            .withRegistrationOuDimension(List.of(ou))
            .withOutputIdScheme(CODE)
            .build();
    Grid grid = registrationGrid(ou.getUid());

    handler.applyScheme(grid, params);

    assertEquals(List.of(List.of("OU_264")), grid.getRows());
    verifyNoInteractions(organisationUnitService);
  }

  @Test
  void preservesUidWhenCodeOrReturnedObjectIsMissing() {
    OrganisationUnit ou = registrationOu();
    ou.setCode(null);
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withRegistrationOuDimension(List.of(ou))
            .withOutputIdScheme(CODE)
            .build();
    String unknownUid = "unknownOu01";
    when(organisationUnitService.getOrganisationUnitsByUid(Set.of(unknownUid)))
        .thenReturn(List.of());
    Grid grid = registrationGrid(ou.getUid(), unknownUid);

    handler.applyScheme(grid, params);

    assertEquals(List.of(List.of(ou.getUid()), List.of(unknownUid)), grid.getRows());
  }

  @ParameterizedTest
  @MethodSource("unmappedSchemes")
  void doesNotFetchFacilitiesWhenNoOuConversionIsNeeded(IdScheme general, IdScheme orgUnit) {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withRegistrationOuDimension(List.of())
            .withOutputIdScheme(general)
            .build();
    params.setOutputOrgUnitIdScheme(orgUnit);
    Grid grid = registrationGrid("facility001");

    handler.applyScheme(grid, params);

    assertEquals(List.of(List.of("facility001")), grid.getRows());
    verifyNoInteractions(organisationUnitService);
  }

  static Stream<Arguments> unmappedSchemes() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(UID, null),
        Arguments.of(ID, null),
        Arguments.of(CODE, UID));
  }

  @Test
  void doesNotFetchFacilitiesForSkippedMetadataOrMissingColumn() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withRegistrationOuDimension(List.of())
            .withOutputIdScheme(CODE)
            .build();
    handler.applyScheme(new ListGrid(), params);
    handler.applyScheme(
        registrationGrid("facility001"),
        new EventQueryParams.Builder(params).withSkipMeta(true).build());
    verifyNoInteractions(organisationUnitService);
  }

  private OrganisationUnit registrationOu() {
    OrganisationUnit ou = createOrganisationUnit('A');
    ou.setUid("O6uvpzGd5pu");
    ou.setName("Bo");
    ou.setCode("OU_264");
    return ou;
  }

  private Grid registrationGrid(String... uids) {
    Grid grid =
        new ListGrid()
            .addHeader(
                new GridHeader("registrationou", "Registration org unit", TEXT, false, true));
    for (String uid : uids) grid.addRow().addValue(uid);
    return grid;
  }
}
