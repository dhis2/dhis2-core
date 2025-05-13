/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.datavalueset;

import static org.hamcrest.Matchers.equalTo;
import static org.hisp.dhis.tracker.export.FileUtil.mapGzipEntryToStringContent;
import static org.hisp.dhis.tracker.export.FileUtil.mapZipEntryToStringContent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import io.restassured.response.Response;
import java.io.IOException;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.helpers.JsonParserUtils;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataValueSetTest extends ApiTest {

  private RestApiActions dataValueSetActions;
  private MetadataActions metadataApiActions;
  private LoginActions loginActions;
  private static final String ORG_UNIT_UID = "OrgUnitUid7";
  private static final String DATA_ELEMENT_UID = "DataElUID08";
  private static final String DATA_SET_UID = "DataSetUID8";

  @BeforeAll
  public void before() {
    loginActions = new LoginActions();
    metadataApiActions = new MetadataActions();
    dataValueSetActions = new RestApiActions("dataValueSets");
    loginActions.loginAsSuperUser();

    // import metadata & data values
    metadataApiActions.importMetadata(metadata()).validateStatus(200);
    addDataValues();
  }

  @BeforeEach
  public void setup() {
    loginActions.loginAsSuperUser();
  }

  @AfterAll
  public void deleteDataValuesAfter() {
    dataValueSetActions
        .post(dataValueSetImport(), getDataValueQueryParams("DELETE"))
        .validateStatus(200)
        .validate();
  }

  @Test
  @DisplayName("Getting data values in xml zip format returns a valid payload")
  void dataValueXmlZipTest() throws IOException {
    // When a request for data values in xml zip format is sent
    Response zipPayload =
        dataValueSetActions
            .get(dataValueSetXmlZipQueryParams("xml", "zip"))
            .validate()
            .statusCode(200)
            .extract()
            .response();

    // Then the expected headers & values are present
    assertEquals(
        "attachment; filename=dataValues_2024-01-01_2050-01-30.xml.zip",
        zipPayload.getHeader("Content-Disposition"));
    assertEquals("binary", zipPayload.getHeader("Content-Transfer-Encoding"));

    // And when unzipping it has the expected values
    String xmlString =
        mapZipEntryToStringContent(zipPayload.body().asByteArray())
            .get("dataValues_2024-01-01_2050-01-30.xml");

    assertTrue(xmlString.contains("dataValueSet"), "unzipped value should contain 'dataValueSet'");
    assertTrue(xmlString.contains("data value 1"), "unzipped value should contain 'data value 1'");
  }

  @Test
  @DisplayName("Getting data values in adx+xml zip format returns a valid payload")
  void dataValueAdxXmlZipTest() throws IOException {
    // When a request for data values in xml zip format is sent
    Response zipPayload =
        dataValueSetActions
            .get(dataValueSetXmlZipQueryParams("adx+xml", "zip"))
            .validate()
            .statusCode(200)
            .extract()
            .response();

    // Then the expected headers & values are present
    assertEquals(
        "attachment; filename=dataValues_2024-01-01_2050-01-30.xml.zip",
        zipPayload.getHeader("Content-Disposition"));
    assertEquals("binary", zipPayload.getHeader("Content-Transfer-Encoding"));

    // And when unzipping it has the expected values
    String xmlString =
        mapZipEntryToStringContent(zipPayload.body().asByteArray())
            .get("dataValues_2024-01-01_2050-01-30.xml");

    assertTrue(xmlString.contains("adx"), "unzipped value should contain 'adx'");
  }

  @Test
  @DisplayName("Getting data values in xml gzip format returns a valid payload")
  void dataValueXmlGzipTest() throws IOException {
    // When a request for data values in xml gzip format is sent
    Response zipPayload =
        dataValueSetActions
            .get(dataValueSetXmlZipQueryParams("xml", "gzip"))
            .validate()
            .statusCode(200)
            .extract()
            .response();

    // Then the expected headers & values are present
    assertEquals(
        "attachment; filename=dataValues_2024-01-01_2050-01-30.xml.gz",
        zipPayload.getHeader("Content-Disposition"));
    assertEquals("binary", zipPayload.getHeader("Content-Transfer-Encoding"));

    // And when unzipping it has the expected values
    String xmlString = mapGzipEntryToStringContent(zipPayload.body().asByteArray());

    assertTrue(xmlString.contains("dataValueSet"), "unzipped value should contain 'dataValueSet'");
    assertTrue(xmlString.contains("data value 1"), "unzipped value should contain 'data value 1'");
  }

  private void addDataValues() {
    dataValueSetActions
        .post(dataValueSetImport(), getDataValueQueryParams("NEW_AND_UPDATES"))
        .validateStatus(200)
        .validate()
        .body("response.importCount.imported", equalTo(1));
  }

  private QueryParamsBuilder getDataValueQueryParams(String importStrategy) {
    return new QueryParamsBuilder()
        .add("async=false")
        .add("dryRun=false")
        .add("strategy=%s".formatted(importStrategy))
        .add("preheatCache=false")
        .add("dataElementIdScheme=UID")
        .add("orgUnitIdScheme=UID")
        .add("idScheme=UID")
        .add("format=json")
        .add("skipExistingCheck=false");
  }

  private String dataValueSetXmlZipQueryParams(String format, String compression) {
    return new QueryParamsBuilder()
        .add("orgUnit=%s")
        .add("startDate=2024-01-01")
        .add("endDate=2050-01-30")
        .add("dataElement=%s")
        .add("format=%s")
        .add("compression=%s")
        .build()
        .formatted(ORG_UNIT_UID, DATA_ELEMENT_UID, format, compression);
  }

  private JsonObject dataValueSetImport() {
    return JsonParserUtils.toJsonObject(
        """
          {
              "dataValues": [
                  {
                      "dataElement": "%s",
                      "period": "202405",
                      "orgUnit": "%s",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "data value 1",
                      "comment": "data value 1"
                  }
              ]
          }
          """
            .formatted(DATA_ELEMENT_UID, ORG_UNIT_UID));
  }

  private String metadata() {
    return """
          {
              "organisationUnits": [
                  {
                      "id": "%s",
                      "name": "org u 1",
                      "shortName": "org u 1",
                      "openingDate": "2023-06-15",
                      "parent": {
                        "id": "DiszpKrYNg8"
                      }
                  }
              ],
              "dataElements": [
                  {
                     "id": "%s",
                     "aggregationType": "DEFAULT",
                     "domainType": "AGGREGATE",
                     "name": "dataelement08",
                     "shortName": "dataelement08",
                     "valueType": "TEXT"
                 }
              ],
              "dataSets": [
                  {
                      "id": "%s",
                      "name": "dataset08",
                      "shortName": "ds08",
                      "periodType": "Monthly",
                      "dataSetElements": [
                          {
                              "dataElement": {
                                  "id": "%s"
                              }
                          }
                      ]
                  }
              ]
          }
          """
        .formatted(ORG_UNIT_UID, DATA_ELEMENT_UID, DATA_SET_UID, DATA_ELEMENT_UID);
  }
}
