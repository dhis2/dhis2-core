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
import org.hisp.dhis.web.WebClient;
import org.junit.jupiter.api.Test;

/**
 * Test for data elements which have been abandoned. This is taken to mean that there is no data
 * recorded against them, and they have not been updated in the last hundred days.
 *
 * <p>It is not possible to manually set the lastUpdate field for data elements, so it is not
 * possible to create a proper unit test for the scenario of identifying abandoned data elements.
 *
 * <p>{@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/data_elements/aggregate_des_abandoned.yaml
 * }
 *
 * @author Jason P. Pickering
 */
class DataIntegrityDataElementsAbandonedControllerTest
    extends AbstractDataIntegrityIntegrationTest {
  private static final String check = "data_elements_aggregate_abandoned";

  private static final String detailsIdType = "dataElements";

  private static final String period = "202212";

  @Test
  void testDataElementsNotAbandoned() {

    setUpTest();

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testDataElementsAbandonedDividedByZero() {

    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }

  void setUpTest() {

    String dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC1', 'shortName': 'ANC1', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));
    String dataElementB =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC2', 'shortName': 'ANC2', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    String orgUnitId =
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
            WebClient.Body("{'additions':[{'id':'" + orgUnitId + "'}]}")));
    // Add some data to dataElementB
    assertStatus(
        HttpStatus.CREATED,
        postNewDataValue(period, "10", "Test Data", false, dataElementB, orgUnitId));
    /* Both data elements should have data now */
    assertStatus(
        HttpStatus.CREATED,
        postNewDataValue(period, "20", "Test Data", false, dataElementA, orgUnitId));
  }
}
