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

import java.time.ZonedDateTime;
import java.util.Date;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.datavalue.DataDumpService;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.http.HttpClientAdapter;
import org.hisp.dhis.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for data elements which have been abandoned. This is taken to mean that there is no data
 * recorded against them, and they have not been updated in the last hundred days.
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

  @Autowired private DataDumpService dataDumpService;

  private String orgUnitId;

  @Test
  void testDataElementsNotAbandoned() throws Exception {

    setUpTest();

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testDataElementsAbandonedDividedByZero() {

    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }

  @Test
  void testDataElementsAbandoned() throws Exception {

    setUpTest();

    // Create a data element that is 100 days old but this one has no data
    DataElement dataElementD = createDataElementDaysAgo("D", 100);

    // Out of the four data elements created, only this one should be flagged as abandoned
    assertHasDataIntegrityIssues(
        detailsIdType, check, 25, dataElementD.getUid(), dataElementD.getName(), null, true);
  }

  DataElement createDataElementDaysAgo(String uniqueCharacter, Integer daysAgo) {
    DataElement dataElement = new DataElement();
    dataElement.setUid(BASE_DE_UID + uniqueCharacter);
    dataElement.setName("DataElement" + uniqueCharacter);
    dataElement.setShortName("DataElementShort" + uniqueCharacter);
    dataElement.setCode("DataElementCode" + uniqueCharacter);
    dataElement.setDescription("DataElementDescription" + uniqueCharacter);
    dataElement.setValueType(ValueType.INTEGER);
    dataElement.setDomainType(DataElementDomain.AGGREGATE);
    dataElement.setAggregationType(AggregationType.SUM);
    dataElement.setZeroIsSignificant(false);
    dataElement.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    // Set the lastupdated to the number of days ago
    Date numberDaysAgo = Date.from(ZonedDateTime.now().minusDays(daysAgo).toInstant());
    dataElement.setLastUpdated(numberDaysAgo);
    dataElement.setCreated(numberDaysAgo);
    manager.persist(dataElement);
    dbmsManager.flushSession();
    dbmsManager.clearSession();

    return dataElement;
  }

  void setUpTest() throws Exception {

    // Create some data elements. created and lastUpdated default to now
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
    // Add some data to dataElementB
    assertEquals(
        1,
        dataDumpService.upsertValues(
            new DataEntryValue.Input(
                dataElementB, orgUnitId, null, null, period, "10", "Test Data")));

    // Create a data element that is 100 days old and give it some data
    DataElement dataElementC = createDataElementDaysAgo("A", 100);

    assertEquals(
        1,
        dataDumpService.upsertValues(
            new DataEntryValue.Input(
                dataElementC.getUid(), orgUnitId, null, null, period, "10", "Test Data")));
  }
}
