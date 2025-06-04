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

import static org.hisp.dhis.web.HttpStatus.CONFLICT;
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
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonDataElement;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonImportSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataElementControllerTest extends DhisControllerConvenienceTest {

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
        GET("/dataElements?fields=id,name,categoryCombo")
            .content(org.hisp.dhis.web.HttpStatus.OK)
            .getArray("dataElements");

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
            .content(org.hisp.dhis.web.HttpStatus.OK)
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
      "Creating a data element with missing locale should show correct ignored stats value")
  void dataElementValidationIgnoredValueTest() {
    JsonImportSummary summary =
        POST(
                "/metadata",
                """
    {
        "dataElements": [
            {
                "id": "DeUid000015",
                "aggregationType": "DEFAULT",
                "domainType": "AGGREGATE",
                "name": "test de 1",
                "shortName": "test DE 1",
                "valueType": "NUMBER",
                "translations": [
                    {
                        "property": "name",
                        "value": "french name"
                    }
                ]
            }
        ]
    }
    """)
            .content(CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);

    assertEquals(1, summary.getStats().getIgnored());
    assertEquals(0, summary.getStats().getCreated());

    JsonErrorReport errorReport =
        summary.find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E4000);
    assertNotNull(errorReport);
    assertEquals("Missing required property `locale`", errorReport.getMessage());
  }
}
