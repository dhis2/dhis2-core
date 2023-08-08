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

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Checks for organisation units which have point coordinates which are not contained by their
 * parent organisation unit. This only applies to situations where the parent has geometry of type
 * Polygon or Multipolygon. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/orgunits/orgunits_trailing_spaces.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitsNotContainedByParentControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private String clinicB;

  private String districtA;

  private static final String check = "orgunits_not_contained_by_parent";

  private static final String detailsIdType = "organisationUnits";

  @Test
  void testOrgunitsNotContainedByParent() {

    districtA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'District A', 'shortName': 'District A', "
                    + "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Polygon', 'coordinates' : [[[0,0],[3,0],[3,3],[0,3],[0,0]]]} }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{ 'name': 'Clinic A', 'shortName': 'Clinic A', "
                + "'parent': {'id' : '"
                + districtA
                + "'}, "
                + "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [1, 1]} }"));

    clinicB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Clinic B', 'shortName': 'Clinic B', "
                    + "'parent': {'id' : '"
                    + districtA
                    + "'}, "
                    + "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [5, 5]} }"));

    assertHasDataIntegrityIssues(detailsIdType, check, 50, clinicB, "Clinic B", null, true);
  }

  @Test
  void testOrgunitsContainedByParent() {

    districtA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'District A', 'shortName': 'District A', "
                    + "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Polygon', 'coordinates' : [[[0,0],[3,0],[3,3],[0,3],[0,0]]]} }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{ 'name': 'Clinic A', 'shortName': 'Clinic A', "
                + "'parent': {'id' : '"
                + districtA
                + "'}, "
                + "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [1, 1]} }"));

    clinicB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Clinic B', 'shortName': 'Clinic B', "
                    + "'parent': {'id' : '"
                    + districtA
                    + "'}, "
                    + "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [2, 2]} }"));
    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testOrgunitsContainedByParentDivideRuns() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }
}
