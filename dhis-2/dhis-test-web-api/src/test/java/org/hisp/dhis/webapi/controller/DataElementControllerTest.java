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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.test.web.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonDataElement;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataElementControllerTest extends H2ControllerIntegrationTestBase {

  @BeforeEach
  void setUp() {
    createCatCombos();
    createDataElementsWithCatCombos();
  }

  @Test
  @DisplayName(
      "DataElement with default categoryCombo should be present in payload when defaults are INCLUDE by default")
  void getAllDataElementsWithCatComboFieldsIncludingDefaultsTest() {
    JsonArray dataElements =
        GET("/dataElements?fields=id,name,categoryCombo")
            .content(HttpStatus.OK)
            .getArray("dataElements");

    assertTrue(
        Set.of("CatComUid00", "CatComUid01", "CatComUid02", "bjDvmb4bfuf")
            .containsAll(
                dataElements.stream()
                    .map(jde -> jde.as(JsonDataElement.class))
                    .map(JsonDataElement::getCategoryCombo)
                    .map(JsonIdentifiableObject::getId)
                    .collect(Collectors.toSet())),
        "Returned cat combo IDs include custom cat combos and default cat combo");
  }

  @Test
  @DisplayName(
      "DataElements in payload should not include the default categoryCombo field when EXCLUDE used")
  void dataElementsExcludingDefaultCatComboTest() {
    JsonArray dataElements =
        GET("/dataElements?fields=id,name,categoryCombo&defaults=EXCLUDE")
            .content(HttpStatus.OK)
            .getArray("dataElements");

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
    assertTrue(
        Set.of("CatComUid00", "CatComUid01", "CatComUid02")
            .containsAll(
                deWithCatCombo.get(true).stream()
                    .map(jde -> jde.as(JsonDataElement.class))
                    .map(JsonDataElement::getCategoryCombo)
                    .map(JsonIdentifiableObject::getId)
                    .collect(Collectors.toSet())),
        "Returned cat combo IDs include custom cat combos only");
  }

  private void createCatCombos() {
    for (int i = 0; i < 3; i++) {
      POST(
              "/categoryCombos",
              """
              {
                'id': 'CatComUid0%d',
                'name': 'cat combo %d',
                'dataDimensionType': "DISAGGREGATION'
              }
              """
                  .formatted(i, i))
          .content(HttpStatus.CREATED);
    }
  }

  private void createDataElementsWithCatCombos() {
    for (int i = 0; i < 3; i++) {
      POST(
              "/dataElements",
              """
              {
                'id': 'DeUid00000%d',
                'name': 'de %d',
                'shortName': 'de %d',
                'valueType': 'TEXT',
                'domainType': 'AGGREGATE',
                'aggregationType': 'DEFAULT',
                'categoryCombo': {
                  'id': 'CatComUid0%d'
                }
              }
              """
                  .formatted(i, i, i, i))
          .content(HttpStatus.CREATED);
    }

    // DE with no cat combo (default cat combo will be assigned)
    POST(
            "/dataElements",
            """
            {
              'id': 'DeUid00000x',
              'name': 'de x',
              'shortName': 'de x',
              'valueType': 'TEXT',
              'domainType': 'AGGREGATE',
              'aggregationType': 'DEFAULT'
            }
            """)
        .content(HttpStatus.CREATED);
  }
}
