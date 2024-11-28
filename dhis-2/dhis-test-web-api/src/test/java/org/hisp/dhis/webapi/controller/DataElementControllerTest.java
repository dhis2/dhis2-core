/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonDataElement;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataElementControllerTest extends H2ControllerIntegrationTestBase {

  CategoryCombo catComboA;
  CategoryCombo catComboB;
  CategoryCombo catComboC;

  @Autowired private CategoryService categoryService;
  @Autowired private DataElementService dataElementService;

  @BeforeEach
  void setUp() {
    catComboA = createCategoryCombo('A');
    catComboB = createCategoryCombo('B');
    catComboC = createCategoryCombo('C');
    categoryService.addCategoryCombo(catComboA);
    categoryService.addCategoryCombo(catComboB);
    categoryService.addCategoryCombo(catComboC);

    DataElement deA = createDataElement('A', catComboA);
    DataElement deB = createDataElement('B', catComboB);
    DataElement deC = createDataElement('C', catComboC);
    DataElement deZ = createDataElement('Z');
    dataElementService.addDataElement(deA);
    dataElementService.addDataElement(deB);
    dataElementService.addDataElement(deC);
    dataElementService.addDataElement(deZ);
  }

  @Test
  @DisplayName(
      "DataElement with default categoryCombo should be present in payload when defaults are INCLUDE by default")
  void getAllDataElementsWithCatComboFieldsIncludingDefaultsTest() {
    JsonArray dataElements =
        GET("/dataElements?fields=id,name,categoryCombo").content(OK).getArray("dataElements");

    assertEquals(
        Set.of(catComboA.getUid(), catComboB.getUid(), catComboC.getUid(), "bjDvmb4bfuf"),
        dataElements.stream()
            .map(jde -> jde.as(JsonDataElement.class))
            .map(JsonDataElement::getCategoryCombo)
            .map(JsonIdentifiableObject::getId)
            .collect(Collectors.toSet()),
        "Returned cat combo IDs equal custom cat combos and default cat combo Ids");
  }

  @Test
  @DisplayName(
      "DataElements in payload should not include the default categoryCombo when EXCLUDE used")
  void dataElementsExcludingDefaultCatComboTest() {
    JsonArray dataElements =
        GET("/dataElements?fields=id,name,categoryCombo&defaults=EXCLUDE")
            .content(OK)
            .getArray("dataElements");

    // get map of data elements with/without cat combo
    Map<Boolean, List<JsonValue>> deWithCatCombo =
        dataElements.stream()
            .collect(
                Collectors.partitioningBy(
                    jv -> {
                      JsonDataElement jsonDataElement = jv.as(JsonDataElement.class);
                      return jsonDataElement.getCategoryCombo() != null;
                    }));

    assertEquals(
        3,
        deWithCatCombo.get(true).size(),
        "There should be 3 dataElements with a cat combo field");

    assertEquals(
        1,
        deWithCatCombo.get(false).size(),
        "There should be 1 dataElement without a cat combo field");

    assertEquals(
        Set.of(catComboA.getUid(), catComboB.getUid(), catComboC.getUid()),
        deWithCatCombo.get(true).stream()
            .map(jde -> jde.as(JsonDataElement.class))
            .map(JsonDataElement::getCategoryCombo)
            .map(JsonIdentifiableObject::getId)
            .collect(Collectors.toSet()),
        "Returned cat combo IDs equal custom cat combos Ids only");
  }

  @Test
  @DisplayName(
      "Changing a data element's value type is prohibited when it has associated data values - DataElement API")
  void prohibitValueTypeChangeWhenHasDataDeApiTest() {
    // create metadata
    POST(
            "/metadata",
            """
            {
                "dataElements": [
                    {
                        "id": "DeUid000001",
                        "aggregationType": "DEFAULT",
                        "domainType": "AGGREGATE",
                        "name": "test de 1",
                        "shortName": "test DE 1",
                        "valueType": "NUMBER"
                    }
                ],
                "organisationUnits": [
                    {
                        "id": "OrgUnitUid1",
                        "name": "test org 1",
                        "shortName": "test org 1",
                        "openingDate": "2023-06-15"
                    }
                ],
                "dataSets": [
                    {
                        "id": "DsUid000001",
                        "name": "ds 1",
                        "shortName": "ds 1",
                        "periodType": "Monthly",
                        "dataSetElements": [
                            {
                                "dataElement": {
                                    "id": "DeUid000001"
                                }
                            }
                        ]
                    }
                ]
            }
            """)
        .content(OK);

    // add org unit to user for data entry
    PATCH(
            "/users/" + ADMIN_USER_UID,
            """
            [
                {
                    "op": "add",
                    "path": "/organisationUnits",
                    "value": [
                        {
                            "id": "OrgUnitUid1"
                        }
                    ]
                }
            ]
            """)
        .content(OK);

    // add data value for data element
    POST(
            "/dataValueSets",
            """
            {
              "dataSet": "DsUid000001",
              "period": "202311",
              "orgUnit": "OrgUnitUid1",
              "completedDate": "2023-11-05",
              "dataValues": [
                {
                  "dataElement": "DeUid000001",
                  "period": "202311",
                  "orgUnit": "OrgUnitUid1",
                  "value": "2000",
                  "followup": false
                }
              ]
            }""")
        .content(OK);

    // try update data element with new value type (TEXT)
    JsonWebMessage validationErrorMsg =
        assertWebMessage(
            "Conflict",
            409,
            "ERROR",
            "One or more errors occurred, please see full details in import report.",
            PUT(
                    "/dataElements/DeUid000001",
                    """
                    {
                      "id": "DeUid000001",
                      "aggregationType": "DEFAULT",
                      "domainType": "AGGREGATE",
                      "name": "test de 1",
                      "shortName": "test DE 1",
                      "valueType": "TEXT"
                    }""")
                .content(HttpStatus.CONFLICT));

    JsonErrorReport errorReport =
        validationErrorMsg.find(
            JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E1121);
    assertNotNull(errorReport);
    assertEquals(
        "Data element `DeUid000001` value type cannot be changed as it has associated data values",
        errorReport.getMessage());
    JsonDataElement updatedDataElement =
        GET("/dataElements/DeUid000001").content(OK).as(JsonDataElement.class);
    assertEquals(NUMBER, updatedDataElement.getValueType(), "value type should be NUMBER");
  }

  @Test
  @DisplayName(
      "Changing a data element's value type is prohibited when it has associated data values - metadata API")
  void prohibitValueTypeChangeWhenHasDataMetadataApiTest() {
    // create metadata
    POST(
            "/metadata",
            """
        {
            "dataElements": [
                {
                    "id": "DeUid000003",
                    "aggregationType": "DEFAULT",
                    "domainType": "AGGREGATE",
                    "name": "test de 3",
                    "shortName": "test DE 3",
                    "valueType": "NUMBER"
                }
            ],
            "organisationUnits": [
                {
                    "id": "OrgUnitUid3",
                    "name": "test org 3",
                    "shortName": "test org 3",
                    "openingDate": "2023-06-15"
                }
            ],
            "dataSets": [
                {
                    "id": "DsUid000003",
                    "name": "ds 3",
                    "shortName": "ds 3",
                    "periodType": "Monthly",
                    "dataSetElements": [
                        {
                            "dataElement": {
                                "id": "DeUid000003"
                            }
                        }
                    ]
                }
            ]
        }
        """)
        .content(OK);

    // add org unit to user for data entry
    PATCH(
            "/users/" + ADMIN_USER_UID,
            """
        [
            {
                "op": "add",
                "path": "/organisationUnits",
                "value": [
                    {
                        "id": "OrgUnitUid3"
                    }
                ]
            }
        ]
        """)
        .content(OK);

    // add data value for data element
    POST(
            "/dataValueSets",
            """
            {
              "dataSet": "DsUid000003",
              "period": "202311",
              "orgUnit": "OrgUnitUid3",
              "completedDate": "2023-11-05",
              "dataValues": [
                {
                  "dataElement": "DeUid000003",
                  "period": "202311",
                  "orgUnit": "OrgUnitUid3",
                  "value": "2000",
                  "followup": false
                }
              ]
            }""")
        .content(OK);

    // try update data element with new value type (TEXT)
    JsonWebMessage validationErrorMsg =
        assertWebMessage(
            "Conflict",
            409,
            "WARNING",
            "One or more errors occurred, please see full details in import report.",
            POST(
                    "/metadata?importStrategy=UPDATE",
                    """
                    {
                        "dataElements":[
                        {
                            "id": "DeUid000003",
                            "aggregationType": "DEFAULT",
                            "domainType": "AGGREGATE",
                            "name": "test de 3",
                            "shortName": "test DE 3",
                            "valueType": "TEXT"
                          }
                        ]
                    }
                    """)
                .content(HttpStatus.CONFLICT));

    JsonErrorReport errorReport =
        validationErrorMsg.find(
            JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E1121);
    assertNotNull(errorReport);
    assertEquals(
        "Data element `DeUid000003` value type cannot be changed as it has associated data values",
        errorReport.getMessage());

    JsonDataElement updatedDataElement =
        GET("/dataElements/DeUid000003").content(OK).as(JsonDataElement.class);
    assertEquals(NUMBER, updatedDataElement.getValueType(), "value type should be NUMBER");
  }

  @Test
  @DisplayName(
      "Changing a data element's value type is allowed when it has no associated data values")
  void allowValueTypeChangeWhenHasNoDataTest() {
    // create metadata
    POST(
            "/metadata",
            """
            {
                "dataElements": [
                    {
                        "id": "DeUid000002",
                        "aggregationType": "DEFAULT",
                        "domainType": "AGGREGATE",
                        "name": "test de 2",
                        "shortName": "test DE 2",
                        "valueType": "NUMBER"
                    }
                ],
                "organisationUnits": [
                    {
                        "id": "OrgUnitUid2",
                        "name": "test org 2",
                        "shortName": "test org 2",
                        "openingDate": "2023-06-15"
                    }
                ],
                "dataSets": [
                    {
                        "id": "DsUid000002",
                        "name": "ds 2",
                        "shortName": "ds 2",
                        "periodType": "Monthly",
                        "dataSetElements": [
                            {
                                "dataElement": {
                                    "id": "DeUid000002"
                                }
                            }
                        ]
                    }
                ]
            }
            """)
        .content(OK);

    // update data element with new value type (TEXT)
    PUT(
            "/dataElements/DeUid000002",
            """
            {
              "id": "DeUid000002",
              "aggregationType": "DEFAULT",
              "domainType": "AGGREGATE",
              "name": "test de 2",
              "shortName": "test DE 2",
              "valueType": "TEXT"
            }""")
        .content(OK);

    JsonDataElement updatedDataElement =
        GET("/dataElements/DeUid000002").content(OK).as(JsonDataElement.class);
    assertEquals(TEXT, updatedDataElement.getValueType(), "value type should be updated to TEXT");
  }
}
