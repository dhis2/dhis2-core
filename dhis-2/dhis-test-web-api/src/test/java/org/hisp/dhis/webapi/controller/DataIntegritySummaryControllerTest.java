/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.dataintegrity.DataIntegrityCheckType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link DataIntegrityController} API with focus API returning {@link
 * org.hisp.dhis.dataintegrity.DataIntegritySummary}.
 *
 * @author Jan Bernitt
 */
class DataIntegritySummaryControllerTest extends AbstractDataIntegrityControllerTest {
  @Test
  void testLegacyChecksHaveSummary() {
    for (DataIntegrityCheckType type : DataIntegrityCheckType.values()) {
      String check = type.getName();
      postSummary(check);
      JsonDataIntegritySummary summary = getSummary(check);
      assertTrue(summary.getCount() >= 0, "summary threw an exception");
    }
  }

  @Test
  void testSingleCheckByPath() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categories",
            "{'name': 'CatDog', 'shortName': 'CD', 'dataDimensionType': 'ATTRIBUTE'}"));

    postSummary("categories-no-options");
    JsonDataIntegritySummary summary =
        GET("/dataIntegrity/categories-no-options/summary")
            .content()
            .as(JsonDataIntegritySummary.class);
    assertTrue(summary.exists());
    assertTrue(summary.isObject());
    assertEquals(1, summary.getCount());
    assertEquals(50, summary.getPercentage().intValue());
    assertNotNull(summary.getStartTime());
    assertNotNull(summary.getCode());
    assertFalse(summary.getStartTime().isAfter(summary.getFinishedTime()));
  }

  @Test
  void testCompletedChecks() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categories",
            "{'name': 'CatDog', 'shortName': 'CD', 'dataDimensionType': 'ATTRIBUTE'}"));

    postSummary("categories-no-options");
    JsonDataIntegritySummary summary =
        GET("/dataIntegrity/categories-no-options/summary")
            .content()
            .as(JsonDataIntegritySummary.class);
    assertNotNull(summary);

    // OBS! The result is based on application scoped map so there might be other values from other
    // tests
    assertTrue(
        GET("/dataIntegrity/summary/completed")
            .content()
            .stringValues()
            .contains("categories_no_options"));
  }

  @Test
  void testRunSummaryCheck_WithBody() {
    JsonObject trigger =
        POST("/dataIntegrity/summary", "['INA']").content(); // indicator_no_analysis
    assertTrue(trigger.isA(JsonWebMessage.class));

    // wait for check to complete
    JsonDataIntegritySummary details =
        GET("/dataIntegrity/IN/summary?timeout=1000").content().as(JsonDataIntegritySummary.class);
    assertTrue(details.isObject());

    assertTrue(
        GET("/dataIntegrity/summary/completed")
            .content()
            .stringValues()
            .contains("indicator_no_analysis"));
  }
}
