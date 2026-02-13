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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpStatus.BAD_REQUEST;
import static org.hisp.dhis.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonPeriodType;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link PeriodTypeController} using (mocked) REST requests.
 *
 * @author Morten Olav Hansen
 */
@Transactional
class PeriodTypeControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testPeriodTypeDefaults() {
    JsonObject object = GET("/periodTypes").content(HttpStatus.OK).as(JsonObject.class);
    JsonList<JsonPeriodType> periodTypes = object.getList("periodTypes", JsonPeriodType.class);
    assertTrue(periodTypes.exists());
    assertEquals(20, periodTypes.size());
    JsonPeriodType periodType = periodTypes.get(0);
    assertNotNull(periodType.getName());
    assertNotNull(periodType.getIsoDuration());
    assertNotNull(periodType.getIsoFormat());
    assertNotNull(periodType.getFrequencyOrder());
  }

  @Test
  void testPeriodTypeNameIsoFormat() {
    JsonList<JsonPeriodType> periodTypes =
        GET("/periodTypes?fields=name,isoFormat")
            .content(HttpStatus.OK)
            .as(JsonObject.class)
            .getList("periodTypes", JsonPeriodType.class);
    assertTrue(periodTypes.exists());
    assertEquals(20, periodTypes.size());
    JsonPeriodType periodType = periodTypes.get(0);
    assertNotNull(periodType.getName());
    assertNotNull(periodType.getIsoFormat());
    assertNull(periodType.getIsoDuration());
    assertNull(periodType.getFrequencyOrder());
  }

  @Test
  void testPut() {
    // Given
    String body =
        """
          {
            "name": "Daily",
            "displayName": "Daily",
            "isoDuration": "P1D",
            "isoFormat": "yyyyMMdd",
            "frequencyOrder": 1,
            "label": "Daily-test",
            "displayLabel": "Daily"
          }
        """;

    // When
    assertStatus(OK, PUT("/periodTypes/", body));

    // Then
    JsonObject response = GET("/periodTypes").content();

    assertThat(response.get("periodTypes").toString(), containsString("Daily-test"));
  }

  @Test
  void testPutError() {
    // Given
    String body =
        """
          {
            "name": "DailyInvalid",
            "displayName": "Daily",
            "isoDuration": "P1D",
            "isoFormat": "yyyyMMdd",
            "frequencyOrder": 1,
            "label": "Daily-test",
            "displayLabel": "Daily"
          }
        """;

    // Then
    assertStatus(BAD_REQUEST, PUT("/periodTypes/", body));
  }
}
