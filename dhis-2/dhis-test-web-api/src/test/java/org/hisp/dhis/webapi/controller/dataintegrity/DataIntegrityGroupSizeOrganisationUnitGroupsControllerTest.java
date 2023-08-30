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

import java.util.Set;
import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Test for metadata check for orgunit groups with fewer than two members. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/groups/group_size_organisation_unit_groups.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityGroupSizeOrganisationUnitGroupsControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private static final String check = "orgunit_groups_scarce";

  private static final String detailsIdType = "organisationUnitGroups";

  private String orgunitB;

  @Test
  void testOrgunitGroupSizeTooLow() {

    setUpTest();

    String testOrgUnitGroupB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnitGroups",
                "{'name': 'Type B', 'shortName': 'Type B', 'organisationUnits' : [{'id' : '"
                    + orgunitB
                    + "'}]}"));

    String testOrgUnitGroupC =
        assertStatus(
            HttpStatus.CREATED,
            POST("/organisationUnitGroups", "{'name': 'Type C', 'shortName': 'Type C' }"));

    assertHasDataIntegrityIssues(
        detailsIdType,
        check,
        66,
        Set.of(testOrgUnitGroupB, testOrgUnitGroupC),
        Set.of("Type B", "Type C"),
        Set.of("0", "1"),
        true);
  }

  @Test
  void testOrgunitGroupSizeOK() {

    setUpTest();

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testOrgunitGroupSizeRuns() {

    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }

  void setUpTest() {
    String orgunitA =
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

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnitGroups",
            "{'name': 'Type A', 'shortName': 'Type A', 'organisationUnits' : [{'id' : '"
                + orgunitA
                + "'}, {'id' : '"
                + orgunitB
                + "'}]}"));
  }
}
