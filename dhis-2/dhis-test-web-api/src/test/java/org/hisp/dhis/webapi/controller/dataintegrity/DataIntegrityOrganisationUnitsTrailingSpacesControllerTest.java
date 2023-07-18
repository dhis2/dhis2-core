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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for organisation units with trailing spaces. Currently, the API should trim trailing spaces
 * from organisation units but this may still be an issue with legacy databases.* {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/orgunits/orgunits_trailing_spaces.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitsTrailingSpacesControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  @Autowired private OrganisationUnitService orgUnitService;

  private static final String unitAName = "Space District   ";

  private static final String unitBName = "Spaced Out District";

  private static final String check = "orgunits_trailing_spaces";

  private static final String detailsIdType = "organisationUnits";

  @Test
  void DataIntegrityOrganisationUnitsTrailingSpacesTest() {

    OrganisationUnit unitA = createOrganisationUnit('A');
    unitA.setName(unitAName);
    unitA.setShortName(unitAName);
    unitA.setOpeningDate(getDate("2022-01-01"));
    orgUnitService.addOrganisationUnit(unitA);

    OrganisationUnit unitB = createOrganisationUnit('B');
    unitB.setName(unitBName);
    unitB.setShortName(unitBName + "    ");
    unitB.setOpeningDate(getDate("2022-01-01"));
    orgUnitService.addOrganisationUnit(unitB);

    OrganisationUnit unitC = createOrganisationUnit('C');
    unitC.setName("NoSpaceDistrict");
    unitC.setShortName("NoSpaceDistrict");
    unitC.setOpeningDate(getDate("2022-01-01"));
    orgUnitService.addOrganisationUnit(unitC);
    dbmsManager.clearSession();

    JsonResponse json_unitA =
        GET("/organisationUnits/" + unitA.getUid()).content().as(JsonResponse.class);
    assertEquals(unitAName, json_unitA.getString("name").string());

    Set<String> orgUnitUIDs = Set.of(unitA.getUid(), unitB.getUid());
    Set<String> orgunitNames = Set.of(unitA.getName(), unitB.getName());

    assertHasDataIntegrityIssues(
        detailsIdType, check, 66, orgUnitUIDs, orgunitNames, Set.of(), true);
  }

  @Test
  void orgunitsNoTrailingSpaces() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{ 'name': 'NospaceDistrict', 'shortName': 'NospaceDistrict', 'openingDate' : '2022-01-01'}"));

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testOrgunitsTrailingSpacesZeroCase() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }
}
