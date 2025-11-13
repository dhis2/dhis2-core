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
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.datavalue.DataDumpService;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.http.HttpClientAdapter;
import org.hisp.dhis.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for data elements which have no data values associated with them {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/data_elements/aggregate_des_nodata.yaml}
 *
 * @implNote Note that we clear the database session prior to the actual test. This seems to be
 *     required for the data to actually be flushed to the datavalue table.
 * @author Jason P. Pickering
 */
class DataIntegrityDataElementsNoDataControllerTest extends AbstractDataIntegrityIntegrationTest {

  private static final String check = "data_elements_aggregate_no_data";
  private static final String detailsIdType = "dataElements";
  private static final String period = "202212";

  @Autowired private DataDumpService dataDumpService;

  private String dataElementA;
  private String dataElementB;
  private String orgUnitId;

  @Test
  void testDataElementsHaveData() throws Exception {

    setUpTest();
    // Add some data to dataElementB
    assertEquals(
        1,
        dataDumpService.upsertValues(
            new DataEntryValue.Input(
                dataElementB, orgUnitId, null, null, period, "10", "Test Data")));

    // Add some data to dataElementA
    assertEquals(
        1,
        dataDumpService.upsertValues(
            new DataEntryValue.Input(
                dataElementA, orgUnitId, null, null, period, "10", "Test Data")));

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testDataElementsDoNotHaveData() throws Exception {

    setUpTest();

    // Add some data to dataElementB
    assertEquals(
        1,
        dataDumpService.upsertValues(
            new DataEntryValue.Input(
                dataElementB, orgUnitId, null, null, period, "10", "Test Data")));
    dbmsManager.clearSession();
    // One of the data elements should not have data
    assertHasDataIntegrityIssues(detailsIdType, check, 50, dataElementA, "ANC1", null, true);
  }

  @Test
  void testDataElementsNoDataRuns() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }

  void setUpTest() {

    dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    dataElementB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC2', 'shortName': 'ANC2', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    orgUnitId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));

    // add OU to users hierarchy
    assertStatus(
        HttpStatus.OK,
        POST(
            "/users/{id}/organisationUnits",
            getCurrentUser().getUid(),
            HttpClientAdapter.Body("{'additions':[{'id':'" + orgUnitId + "'}]}")));
  }
}
