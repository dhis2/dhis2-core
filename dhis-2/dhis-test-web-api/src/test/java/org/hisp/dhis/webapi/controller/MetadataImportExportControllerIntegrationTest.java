/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MetadataImportExportControllerIntegrationTest extends PostgresControllerIntegrationTestBase {

  @Test
  void testPostJsonMetadata_Async() {
    JsonWebMessage msg =
        assertWebMessage(
            HttpStatus.OK,
            POST(
                "/metadata?async=true",
                "{'organisationUnits':[{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}]}"));
    assertStartsWith("Initiated METADATA_IMPORT", msg.getMessage());
  }

  @Test
  void testPostCsvMetadata_Async() {
    JsonWebMessage msg =
        assertWebMessage(
            HttpStatus.OK,
            POST(
                "/metadata?async=true&classKey=ORGANISATION_UNIT",
                Body(","),
                ContentType("application/csv")));
    assertStartsWith("Initiated METADATA_IMPORT", msg.getMessage());
  }

  @Test
  void testPostGmlMetadata_Async() {
    JsonWebMessage msg =
        assertWebMessage(
            HttpStatus.OK,
            POST(
                "/metadata/gml?async=true",
                Body("<metadata></metadata>"),
                ContentType("application/xml")));
    assertStartsWith("Initiated METADATA_IMPORT", msg.getMessage());
  }

  @Test
  @DisplayName(
      "Importing an existing CategoryCombo, which has no data values, with an additional Category succeeds")
  void importingCategoryComboNewCategoryTest() {
    // Given an existing CategoryCombo exists with 2 Categories
    JsonImportSummary initialImport =
        POST("/metadata", Body(getCatComboWithCategories()))
            .content()
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals("OK", initialImport.getStatus());
    assertEquals(3, initialImport.getStats().getCreated());

    // When importing the existing CategoryCombo with an additional Category
    // Then the import should succeed with the expected message & stats
    JsonImportSummary updateImport =
        POST("/metadata", Body(getCatComboWithAdditionalCategory()))
            .content()
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals("OK", updateImport.getStatus());
    assertEquals(3, updateImport.getStats().getUpdated());
    assertEquals(1, updateImport.getStats().getCreated());
  }

  private String getCatComboWithCategories() {
    return """
      {
          "categories": [
              {
                  "id": "CategoUid01",
                  "name": "cat 1",
                  "shortName": "cat 1",
                  "dataDimensionType": "DISAGGREGATION",
                  "categoryOptions": []
              },
              {
                  "id": "CategoUid02",
                  "name": "cat 2",
                  "shortName": "cat 2",
                  "dataDimensionType": "DISAGGREGATION",
                  "categoryOptions": []
              }
          ],
          "categoryCombos": [
              {
                  "id": "CatComUid01",
                  "name": "cat combo 1",
                  "dataDimensionType": "DISAGGREGATION",
                  "categories": [
                      {
                          "id": "CategoUid01"
                      },
                      {
                          "id": "CategoUid02"
                      }
                  ]
              }
          ]
      }
      """;
  }

  private String getCatComboWithAdditionalCategory() {
    return """
      {
          "categories": [
              {
                  "id": "CategoUid01",
                  "name": "cat 1",
                  "shortName": "cat 1",
                  "dataDimensionType": "DISAGGREGATION",
                  "categoryOptions": []
              },
              {
                  "id": "CategoUid02",
                  "name": "cat 2",
                  "shortName": "cat 2",
                  "dataDimensionType": "DISAGGREGATION",
                  "categoryOptions": []
              },
               {
                  "id": "CategoUid03",
                  "name": "cat 3",
                  "shortName": "cat 3",
                  "dataDimensionType": "DISAGGREGATION",
                  "categoryOptions": []
              }
          ],
          "categoryCombos": [
              {
                  "id": "CatComUid01",
                  "name": "cat combo 1",
                  "dataDimensionType": "DISAGGREGATION",
                  "categories": [
                      {
                          "id": "CategoUid01"
                      },
                      {
                          "id": "CategoUid02"
                      },
                       {
                          "id": "CategoUid03"
                      }
                  ]
              }
          ]
      }
      """;
  }
}
