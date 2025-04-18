/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.http.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Checks for organisation units with no groups. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/orgunits/orgunits_no_groups.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitsNoGroupsControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private String orgunitA;

  private String orgunitB;

  private static final String check = "organisation_units_without_groups";

  private static final String detailsIdType = "organisationUnits";

  @Test
  void testOrgunitsNoGroups() {

    orgunitA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}"));

    orgunitB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Pizza District', 'shortName': 'Pizza District', 'openingDate' : '2022-01-01'}"));

    // Create an orgunit group
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnitGroups",
            "{'name': 'Type A', 'shortName': 'Type A', 'organisationUnits' : [{'id' : '"
                + orgunitA
                + "'}]}"));
    assertHasDataIntegrityIssues(
        detailsIdType, check, 50, orgunitB, "Pizza District", "Type", true);
  }

  @Test
  void testOrgunitsInGroups() {

    orgunitA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}"));

    orgunitB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Pizza District', 'shortName': 'Pizza District', 'openingDate' : '2022-01-01'}"));

    // Create an orgunit group
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnitGroups",
            "{'name': 'Type A', 'shortName': 'Type A', 'organisationUnits' : [{'id' : '"
                + orgunitA
                + "'}, {'id' : '"
                + orgunitB
                + "'}]}"));
    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testOrgUnitsNoGroupsDivideByZero() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }
}
