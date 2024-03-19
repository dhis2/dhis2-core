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
 * Checks for organisation units which have an invalid geometry. The reasons for this may vary, but
 * in this test case, we look for a polygon with self-intersection. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/orgunits/orgunits_invalid_geometry.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityOrganisationUnitsInvalidGeometryControllerTest
    extends AbstractDataIntegrityIntegrationTest {

  private static final String check = "orgunits_invalid_geometry";

  private static final String detailsIdType = "organisationUnits";

  @Test
  void testOrgunitsInvalidGeometry() {

    String districtA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Bowtie District', 'shortName': 'District A', "
                    + "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Polygon', 'coordinates' : [[[10,20],[10,10],[20,20],[20,10],[10,20]]]} }"));

    createFacilities(districtA);

    assertHasDataIntegrityIssues(
        detailsIdType, check, 33, districtA, "Bowtie District", "Self-intersection", true);
  }

  @Test
  void testOrgunitsValidGeometry() {

    String districtA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'District A', 'shortName': 'District A', "
                    + "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Polygon', 'coordinates' : [[[0,0],[3,0],[3,3],[0,3],[0,0]]]} }"));

    createFacilities(districtA);

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testOrgunitsInvalidGeometryDivideByZero() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }

  private void createFacilities(String districtA) {

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{ 'name': 'Clinic A', 'shortName': 'Clinic A', "
                + "'parent': {'id' : '"
                + districtA
                + "'}, "
                + "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [1, 1]} }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            "{ 'name': 'Clinic B', 'shortName': 'Clinic B', "
                + "'parent': {'id' : '"
                + districtA
                + "'}, "
                + "'openingDate' : '2022-01-01', 'geometry' : {'type' : 'Point', 'coordinates' : [2, 2]} }"));
  }
}
