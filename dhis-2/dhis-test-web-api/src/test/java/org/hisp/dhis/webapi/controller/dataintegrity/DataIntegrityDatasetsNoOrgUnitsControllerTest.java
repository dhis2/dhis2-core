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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;

import org.hisp.dhis.http.HttpStatus;
import org.junit.jupiter.api.Test;

/**
 * Tests for aggregate datasets with no data elements. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/analytical_objects/datasets_not_assigned_to_org_units.yaml
 * }
 *
 * @author Jason P. Pickering
 */
class DataIntegrityDatasetsNoOrgUnitsControllerTest extends AbstractDataIntegrityIntegrationTest {

  private static final String check = "datasets_not_assigned_to_org_units";

  private static final String dataSetUID = "CowXAwmulDG";

  @Test
  void testDataSetHasNoOrgUnits() {

    String defaultCatCombo = getDefaultCatCombo();
    String dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataSets",
            "{ 'id' : '"
                + dataSetUID
                + "', 'name': 'Test', 'shortName': 'Test', 'periodType' : 'Monthly', 'categoryCombo' : {'id': '"
                + defaultCatCombo
                + "'}, "
                + " 'dataSetElements': [{ 'dataSet': { 'id': '"
                + dataSetUID
                + "'}, 'dataElement': { 'id': '"
                + dataElementA
                + "'}}]}"));

    assertHasDataIntegrityIssues("dataSets", check, 100, dataSetUID, "Test", null, true);
  }

  @Test
  void testDataSetHasOrgUnits() {

    String defaultCatCombo = getDefaultCatCombo();

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataElements",
            "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    String orgunitA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataSets",
            "{ 'name': 'Test', 'shortName': 'Test', 'periodType' : 'Monthly', 'categoryCombo' : {'id': '"
                + defaultCatCombo
                + "'}, 'organisationUnits' : [{'id': '"
                + orgunitA
                + "'}]}"));

    assertHasNoDataIntegrityIssues("dataSets", check, true);
  }

  @Test
  void testEmptyDataSetsRuns() {
    assertHasNoDataIntegrityIssues("dataSets", check, false);
  }
}
