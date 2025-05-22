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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.http.HttpStatus.CREATED;
import static org.hisp.dhis.http.HttpStatus.NOT_FOUND;
import static org.hisp.dhis.http.HttpStatus.NO_CONTENT;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.dataentry.MinMaxValueController} endpoints.
 *
 * @author Jan Bernitt
 */
class MinMaxValueControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private ObjectMapper jsonMapper;

  private String de, ou, coc;

  @BeforeEach
  void setUp() throws Exception {
    this.de = assertStatus(CREATED, POST("/dataElements", toJson(createDataElement('A'))));
    this.ou =
        assertStatus(CREATED, POST("/organisationUnits", toJson(createOrganisationUnit('B'))));
    this.coc = setupCategoryMetadata("C").coc1().getUid();
  }

  private String toJson(IdentifiableObject de) throws JsonProcessingException {
    return jsonMapper.writeValueAsString(de);
  }

  @Language("json")
  private final String json =
      """
      {
      "dataElement": "%s",
      "orgUnit": "%s",
      "optionCombo": "%s",
      "minValue": 1,
      "maxValue": 100
      }
      """;

  @Test
  void testSaveOrUpdateMinMaxValue() {
    assertStatus(OK, POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
  }

  @Test
  void testSaveOrUpdateMinMaxValue_Alias() {
    String json =
        """
      {
      "dataElement": "%s",
      "orgUnit": "%s",
      "categoryOptionCombo": "%s",
      "minValue": 1,
      "maxValue": 100
      }
      """;
    assertStatus(OK, POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
  }

  @Test
  void testRemoveMinMaxValue() {
    assertStatus(OK, POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
    assertStatus(
        NO_CONTENT, DELETE("/dataEntry/minMaxValues?de={de}&ou={ou}&co={coc}", de, ou, coc));
  }

  @Test
  void testRemoveMinMaxValue_WithBody() {
    assertStatus(OK, POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
    assertStatus(NO_CONTENT, DELETE("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
  }

  @Test
  void testSaveOrUpdateMinMaxValue_DeDoesNotExist() {
    JsonWebMessage response =
        POST("/dataEntry/minMaxValues", json.formatted("de123456789", ou, coc))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2047, response.getErrorCode());
  }

  @Test
  void testSaveOrUpdateMinMaxValue_OuDoesNotExist() {
    JsonWebMessage response =
        POST("/dataEntry/minMaxValues", json.formatted(de, "ou123456789", coc))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2047, response.getErrorCode());
  }

  @Test
  void testSaveOrUpdateMinMaxValue_CocDoesNotExist() {
    JsonWebMessage response =
        POST("/dataEntry/minMaxValues", json.formatted(de, ou, "coc23456789"))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2047, response.getErrorCode());
  }

  @Test
  void testSaveOrUpdateMinMaxValue_DeUndefined() {
    @Language("json")
    String json =
        """
        {
        "orgUnit": "%s",
        "optionCombo": "%s",
        "minValue": 1,
        "maxValue": 100
        }
        """;
    JsonWebMessage response =
        POST("/dataEntry/minMaxValues", json.formatted(ou, coc))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E1100, response.getErrorCode());
  }

  @Test
  void testSaveOrUpdateMinMaxValue_OuUndefined() {
    @Language("json")
    String json =
        """
        {
        "dataElement": "%s",
        "optionCombo": "%s",
        "minValue": 1,
        "maxValue": 100
        }
        """;
    JsonWebMessage response =
        POST("/dataEntry/minMaxValues", json.formatted(de, coc))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E1102, response.getErrorCode());
  }

  @Test
  void testSaveOrUpdateMinMaxValue_CocUndefined() {
    @Language("json")
    String json =
        """
        {
        "dataElement": "%s",
        "orgUnit": "%s",
        "minValue": 1,
        "maxValue": 100
        }
        """;
    JsonWebMessage response =
        POST("/dataEntry/minMaxValues", json.formatted(de, ou))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E1103, response.getErrorCode());
  }

  @Test
  void testSaveOrUpdateMinMaxValue_MinValueUndefined() {
    @Language("json")
    String json =
        """
        {
        "dataElement": "%s",
        "orgUnit": "%s",
        "optionCombo": "%s",
        "maxValue": 100
        }
        """;
    JsonWebMessage response =
        POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2042, response.getErrorCode());
  }

  @Test
  void testSaveOrUpdateMinMaxValue_MaxValueUndefined() {
    @Language("json")
    String json =
        """
        {
        "dataElement": "%s",
        "orgUnit": "%s",
        "optionCombo": "%s",
        "minValue": 100
        }
        """;
    JsonWebMessage response =
        POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2043, response.getErrorCode());
  }

  @Test
  void testSaveOrUpdateMinMaxValue_MinNoBelowMax() {
    @Language("json")
    String json =
        """
        {
        "dataElement": "%s",
        "orgUnit": "%s",
        "optionCombo": "%s",
        "minValue": 100,
        "maxValue": 100
        }
        """;
    JsonWebMessage response =
        POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc))
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2044, response.getErrorCode());
  }

  @Test
  void testRemoveMinMaxValue_DeUndefined() {
    assertStatus(OK, POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
    JsonWebMessage response =
        DELETE("/dataEntry/minMaxValues?ou={ou}&co={coc}", ou, coc)
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E1100, response.getErrorCode());
  }

  @Test
  void testRemoveMinMaxValue_OuUndefined() {
    assertStatus(OK, POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
    JsonWebMessage response =
        DELETE("/dataEntry/minMaxValues?de={de}&co={coc}", de, coc)
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E1102, response.getErrorCode());
  }

  @Test
  void testRemoveMinMaxValue_CocUndefined() {
    assertStatus(OK, POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
    JsonWebMessage response =
        DELETE("/dataEntry/minMaxValues?de={de}&ou={ou}", de, ou)
            .content(BAD_REQUEST)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E1103, response.getErrorCode());
  }

  @Test
  void testRemoveMinMaxValue_DeDoesNotExist() {
    assertStatus(OK, POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
    JsonWebMessage response =
        DELETE("/dataEntry/minMaxValues?de={de}&ou={ou}&co={coc}", "de123456789", ou, coc)
            .content(NOT_FOUND)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2047, response.getErrorCode());
  }

  @Test
  void testRemoveMinMaxValue_OuDoesNotExist() {
    assertStatus(OK, POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
    JsonWebMessage response =
        DELETE("/dataEntry/minMaxValues?de={de}&ou={ou}&co={coc}", de, "ou123456789", coc)
            .content(NOT_FOUND)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2047, response.getErrorCode());
  }

  @Test
  void testRemoveMinMaxValue_CocDoesNotExist() {
    assertStatus(OK, POST("/dataEntry/minMaxValues", json.formatted(de, ou, coc)));
    JsonWebMessage response =
        DELETE("/dataEntry/minMaxValues?de={de}&ou={ou}&co={coc}", de, ou, "coc23456789")
            .content(NOT_FOUND)
            .as(JsonWebMessage.class);
    assertEquals(ErrorCode.E2047, response.getErrorCode());
  }
}
