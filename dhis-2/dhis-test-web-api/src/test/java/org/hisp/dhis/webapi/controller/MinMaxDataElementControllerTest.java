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
package org.hisp.dhis.webapi.controller;

import static java.util.Collections.singleton;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests the {@link MinMaxDataElementController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class MinMaxDataElementControllerTest extends AbstractDataValueControllerTest {

  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private OrganisationUnitService organisationUnitService;
  @Autowired private DataSetService dataSetService;
  @Autowired private DataElementService dataElementService;

  @Autowired private PeriodService periodService;

  private static String fakeDataSetID = "xcTWJYxFyE2";
  private static String fakeOrgUnitID = "CAdEJWs42WP";
  private static String fakeDataElementID = "vZjiu94f5f5";
  private static String fakeCategoryOptionComboID = "XxiuH64Tyl6";

  private OrganisationUnit organisationUnitA;
  private DataSet dataSetA;
  private DataElement dataElementA;
  private CategoryOptionCombo defaultOptionCombo;

  @Test
  void testPostJsonObject() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        null,
        POST(
                "/minMaxDataElements/",
                "{"
                    + "'source':{'id':'"
                    + orgUnitId
                    + "'},"
                    + "'dataElement':{'id':'"
                    + dataElementId
                    + "'},"
                    + "'optionCombo':{'id':'"
                    + categoryOptionComboId
                    + "'},"
                    + "'min':1,"
                    + "'max':42"
                    + "}")
            .content(HttpStatus.CREATED));
  }

  @Test
  void testDeleteObject() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/minMaxDataElements/",
            """
                {
                    'source':{'id':'%s'},'dataElement':{'id':'%s'},
                    'optionCombo':{'id':'%s'},'min':1,'max':42
                }"""
                .formatted(orgUnitId, dataElementId, categoryOptionComboId)));

    assertWebMessage(
        "OK",
        200,
        "OK",
        "MinMaxDataElement deleted.",
        DELETE(
                "/minMaxDataElements/",
                """
                {
                    'source':{'id':'%s'},'dataElement':{'id':'%s'},
                    'optionCombo':{'id':'%s'}
                }"""
                    .formatted(orgUnitId, dataElementId, categoryOptionComboId))
            .content(HttpStatus.OK));
  }

  @Test
  void testDeleteObject_NoSuchObject() {

    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "Can not find MinMaxDataElement.",
        DELETE(
                "/minMaxDataElements/",
                """
                {
                    'source':{'id':'%s'},'dataElement':{'id':'%s'},
                    'optionCombo':{'id':'%s'}
                }"""
                    .formatted(orgUnitId, dataElementId, categoryOptionComboId))
            .content(HttpStatus.NOT_FOUND));
  }

  private void setupBulkTest() {

    CategoryCombo defCatCombo = categoryService.getDefaultCategoryCombo();
    defaultOptionCombo = categoryService.getDefaultCategoryOptionCombo();
    PeriodType periodType =
        periodService.reloadPeriodType(PeriodType.getPeriodTypeByName("Monthly"));

    transactionTemplate.execute(
        status -> {
          organisationUnitA = createOrganisationUnit('A');
          organisationUnitService.addOrganisationUnit(organisationUnitA);
          createUserAndInjectSecurityContext(singleton(organisationUnitA), true);
          dataSetA = createDataSet('A', periodType, defCatCombo);
          dataSetService.addDataSet(dataSetA);
          dataElementA =
              createDataElement(
                  'A', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.AGGREGATE);
          dataElementService.addDataElement(dataElementA);
          dbmsManager.flushSession();
          dbmsManager.clearSession();
          return null;
        });
  }

  private void tearDownBulkTest() {
    transactionTemplate.execute(
        status -> {
          if (organisationUnitA != null) {
            organisationUnitService.deleteOrganisationUnit(organisationUnitA);
          }
          if (dataSetA != null) {
            dataSetService.deleteDataSet(dataSetA);
          }
          if (dataElementA != null) {
            dataElementService.deleteDataElement(dataElementA);
          }
          dbmsManager.flushSession();
          dbmsManager.clearSession();
          return null;
        });
  }

  @Disabled("Issues with transaction isolation here...")
  void testBulkPostJson_DefaultGeneratedTrue() {
    setupBulkTest();
    String payload =
"""
{ "dataset": "%s",
    "orgunit": "%s",
 "values" : [{
    "dataElement": "%s",
    "orgUnit": "%s",
    "categoryOptionCombo": "%s",
    "minValue": 10,
    "maxValue": 100
  }
]
}
"""
            .formatted(
                dataSetA.getUid(),
                organisationUnitA.getUid(),
                dataElementA.getUid(),
                organisationUnitA.getUid(),
                defaultOptionCombo.getUid());
    assertStatus(HttpStatus.OK, POST("/minMaxDataElements/values", payload));
    tearDownBulkTest();
  }

  private static Stream<Arguments> provideTestCases() {
    return Stream.of(
        arguments(
            "Missing required field(s) in: dataElement=%s, orgUnit=%s, categoryOptionCombo=%s, min=10, max=null"
                .formatted(fakeDataElementID, fakeOrgUnitID, fakeCategoryOptionComboID)
                .trim(),
            """
                {
                  "dataset": "%s",
                  "orgunit": "%s",
                  "values": [{
                    "dataElement": "%s",
                    "orgUnit": "%s",
                    "categoryOptionCombo": "%s",
                    "minValue": 10
                  }]
                }
                """
                .formatted(
                    fakeDataSetID,
                    fakeOrgUnitID,
                    fakeDataElementID,
                    fakeOrgUnitID,
                    fakeCategoryOptionComboID),
            HttpStatus.BAD_REQUEST),
        arguments(
            "Missing required field(s) in: dataElement=%s, orgUnit=%s, categoryOptionCombo=%s, min=null, max=10"
                .formatted(fakeDataElementID, fakeOrgUnitID, fakeCategoryOptionComboID)
                .trim(),
            """
                {
                  "dataset": "%s",
                  "orgunit": "%s",
                  "values": [{
                    "dataElement": "%s",
                    "orgUnit": "%s",
                    "categoryOptionCombo": "%s",
                    "maxValue": 10
                  }]
                }
                """
                .formatted(
                    fakeDataSetID,
                    fakeOrgUnitID,
                    fakeDataElementID,
                    fakeOrgUnitID,
                    fakeCategoryOptionComboID),
            HttpStatus.BAD_REQUEST),
        arguments(
            "Min value is greater than or equal to Max value for: dataElement=%s, orgUnit=%s, categoryOptionCombo=%s, min=10, max=10"
                .formatted(fakeDataElementID, fakeOrgUnitID, fakeCategoryOptionComboID)
                .trim(),
            """
                {
                  "dataset": "%s",
                  "orgunit": "%s",
                  "values": [{
                    "dataElement": "%s",
                    "orgUnit": "%s",
                    "categoryOptionCombo": "%s",
                    "minValue": 10,
                    "maxValue": 10
                  }]
                }
                """
                .formatted(
                    fakeDataSetID,
                    fakeOrgUnitID,
                    fakeDataElementID,
                    fakeOrgUnitID,
                    fakeCategoryOptionComboID),
            HttpStatus.BAD_REQUEST));
  }

  @ParameterizedTest
  @MethodSource("provideTestCases")
  void testBulkPostJson(String expectedMessage, String payload, HttpStatus expectedStatus) {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        expectedMessage,
        POST("/minMaxDataElements/values", payload).content(expectedStatus));
  }
}
