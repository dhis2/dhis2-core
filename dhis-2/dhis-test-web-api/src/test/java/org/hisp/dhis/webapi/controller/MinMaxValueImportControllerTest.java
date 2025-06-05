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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.http.HttpStatus.CREATED;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpClientAdapter;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonImportSuccessResponse;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Test the bulk import and delete endpoints of the {@link MinMaxDataElementController}.
 *
 * @author Jan Bernitt
 */
class MinMaxValueImportControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private ObjectMapper jsonMapper;

  private String ds, de, coc, ou1, ou2, ou3, ou4;

  @BeforeEach
  void setUp() throws Exception {
    this.de = assertStatus(CREATED, POST("/dataElements", toJson(createDataElement('A'))));
    TestCategoryMetadata catData = setupCategoryMetadata("C");
    this.coc = catData.coc1().getUid();
    String cc = catData.cc1().getUid();
    String dsId = generateUid();
    @Language("json")
    String dsJson =
        """
      {
      "id": "%s",
      "name": "My data set",
      "shortName": "MDS",
      "periodType": "Monthly",
      "categoryCombo": { "id": "%s"},
      "dataSetElements": [{"dataSet": {"id": "%s"}, "dataElement": { "id": "%s"}}]}""";
    this.ds = assertStatus(CREATED, POST("/dataSets", dsJson.formatted(dsId, cc, dsId, de)));
    this.ou1 =
        assertStatus(CREATED, POST("/organisationUnits", toJson(createOrganisationUnit('X'))));
    this.ou2 =
        assertStatus(CREATED, POST("/organisationUnits", toJson(createOrganisationUnit('Y'))));
    this.ou3 =
        assertStatus(CREATED, POST("/organisationUnits", toJson(createOrganisationUnit('Z'))));
    this.ou4 =
        assertStatus(CREATED, POST("/organisationUnits", toJson(createOrganisationUnit('W'))));
  }

  private String toJson(IdentifiableObject de) throws JsonProcessingException {
    return jsonMapper.writeValueAsString(de);
  }

  @Language("json")
  private final String json =
      """
      { "dataSet": "%s",
        "values" : [{
            "dataElement": "%s",
            "orgUnit": "%s",
            "optionCombo": "%s",
              "minValue": 10,
              "maxValue": 100
          }]
      }""";

  @Language("csv")
  private final String csv =
      """
    dataElement,orgUnit,optionCombo,minValue,maxValue
    %1$s,%3$s,%2$s,0,10
    %1$s,%4$s,%2$s,0,10
    %1$s,%5$s,%2$s,0,10
    %1$s,%6$s,%2$s,0,10
    """;

  private static byte[] gzip(String in) {
    try {
      java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
      try (java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(bos)) {
        gz.write(in.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      return bos.toByteArray();
    } catch (java.io.IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Test
  void testBulkImportJson() {
    JsonImportSuccessResponse response =
        POST("/minMaxDataElements/upsert", json.formatted(ds, de, ou1, coc))
            .content(OK)
            .as(JsonImportSuccessResponse.class);
    assertEquals(1, response.getSuccessful());
  }

  @Test
  void testBulkImportJsonGzip() {
    JsonImportSuccessResponse response =
        POST(
                "/minMaxDataElements/upsert-gzip-json",
                HttpClientAdapter.BinaryBody(gzip(json.formatted(ds, de, ou1, coc))),
                HttpClientAdapter.ContentType("application/gzip"))
            .content(OK)
            .as(JsonImportSuccessResponse.class);
    assertEquals(1, response.getSuccessful());
  }

  @Test
  void testBulkImportCsv() {
    MockMultipartFile file =
        new MockMultipartFile("file", csv.formatted(de, coc, ou1, ou2, ou3, ou4).getBytes());
    JsonImportSuccessResponse response =
        POST_MULTIPART("/minMaxDataElements/upsert?dataSet=" + ds, file)
            .content(OK)
            .as(JsonImportSuccessResponse.class);
    assertEquals(4, response.getSuccessful());
  }

  @Test
  void testBulkImportJson_IgnoresNonExisting() {
    JsonImportSuccessResponse response =
        POST("/minMaxDataElements/upsert", json.formatted(ds, de, "ou123456789", coc))
            .content(OK)
            .as(JsonImportSuccessResponse.class);
    assertEquals(0, response.getSuccessful());
  }

  @Test
  void testBulkImportJson_MaxValueUndefined() {
    @Language("json")
    String json =
        """
      {
        "dataSet": "%s",
        "values": [{
          "dataElement": "%s",
          "orgUnit": "%s",
          "optionCombo": "%s",
          "minValue": 10
        }]
      }""";
    JsonWebMessage response =
        POST("/minMaxDataElements/upsert", json.formatted(ds, de, ou1, coc))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2043, response.getErrorCode());
  }

  @Test
  void testBulkImportJson_MinValueUndefined() {
    @Language("json")
    String json =
        """
       {
        "dataSet": "%s",
        "values": [{
          "dataElement": "%s",
          "orgUnit": "%s",
          "optionCombo": "%s",
          "maxValue": 10
        }]
      }""";
    JsonWebMessage response =
        POST("/minMaxDataElements/upsert", json.formatted(ds, de, ou1, coc))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2042, response.getErrorCode());
  }

  @Test
  void testBulkImportJson_MinEqualMaxValue() {
    @Language("json")
    String json =
        """
      {
        "dataSet": "%s",
        "values": [{
          "dataElement": "%s",
          "orgUnit": "%s",
          "optionCombo": "%s",
          "minValue": 10,
          "maxValue": 10
        }]
      }""";
    JsonWebMessage response =
        POST("/minMaxDataElements/upsert", json.formatted(ds, de, ou1, coc))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2044, response.getErrorCode());
  }

  @Test
  void testBulkImportJson_EmptyValues() {
    @Language("json")
    String json =
        """
        { "dataSet": "%s", "values": [] }""";
    JsonImportSuccessResponse response =
        POST("/minMaxDataElements/upsert", json.formatted(ds))
            .content(OK)
            .as(JsonImportSuccessResponse.class);
    assertEquals(0, response.getSuccessful());
    assertEquals(0, response.getIgnored());
  }

  @Test
  void testBulkDeleteJson() {
    assertStatus(OK, POST("/minMaxDataElements/upsert", json.formatted(ds, de, ou1, coc)));
    JsonImportSuccessResponse response =
        POST("/minMaxDataElements/delete", json.formatted(ds, de, ou1, coc))
            .content(OK)
            .as(JsonImportSuccessResponse.class);
    assertEquals(1, response.getSuccessful());
  }

  @Test
  void testBulkDeleteJson_IgnoresNonExisting() {
    assertStatus(OK, POST("/minMaxDataElements/upsert", json.formatted(ds, de, ou1, coc)));
    JsonImportSuccessResponse response =
        POST("/minMaxDataElements/delete", json.formatted(ds, de, "ou123456789", coc))
            .content(OK)
            .as(JsonImportSuccessResponse.class);
    assertEquals(0, response.getSuccessful());
    assertEquals(1, response.getIgnored());
  }

  @Test
  void testBulkDeleteCsv() {
    MockMultipartFile file =
        new MockMultipartFile("file", csv.formatted(de, coc, ou1, ou2, ou3, ou4).getBytes());
    JsonImportSuccessResponse response =
        POST_MULTIPART("/minMaxDataElements/upsert?dataSet=" + ds, file)
            .content(OK)
            .as(JsonImportSuccessResponse.class);
    assertEquals(4, response.getSuccessful());
    response =
        POST_MULTIPART("/minMaxDataElements/delete?dataSet=" + ds, file)
            .content(OK)
            .as(JsonImportSuccessResponse.class);
    assertEquals(4, response.getSuccessful());
  }

  @Test
  void testBulkDeleteCsv_NoMinValueColumn() {
    @Language("csv")
    String csv =
        """
      dataElement,orgUnit,optionCombo,maxValue
      %1$s,%3$s,%2$s,10
      """;
    MockMultipartFile file = new MockMultipartFile("file", csv.formatted(de, coc, ou1).getBytes());
    JsonWebMessage response =
        POST_MULTIPART("/minMaxDataElements/upsert?dataSet=" + ds, file)
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2046, response.getErrorCode());
    assertEquals(
        "Error parsing CSV file: Required columns missing: [minValue]", response.getMessage());
  }
}
