/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link
 * org.hisp.dhis.webapi.controller.outlierdetection.AnalyticsOutlierDetectionController} using
 * (mocked) REST requests.
 *
 * @author david mackessy
 */
@Transactional
class AnalyticsOutlierDetectionControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  @DisplayName("Class-level authority check fails when no required auth")
  void testClassLevelAuthCheckNoAuth() {
    switchToNewUser("noAuth", "NoAuth");
    JsonMixed mergeResponse = GET("/analytics/outlierDetection").content(HttpStatus.FORBIDDEN);
    assertEquals("Forbidden", mergeResponse.getString("httpStatus").string());
    assertEquals("ERROR", mergeResponse.getString("status").string());
    assertEquals(
        "Access is denied, requires one Authority from [F_RUN_VALIDATION]",
        mergeResponse.getString("message").string());
  }

  @Test
  @DisplayName("Class-level authority check succeeds when having required auth")
  void testClassLevelAuthCheckHasAuth() {
    switchToNewUser("hasAuth", "F_RUN_VALIDATION");
    JsonMixed mergeResponse = GET("/analytics/outlierDetection").content(HttpStatus.CONFLICT);
    assertEquals("Conflict", mergeResponse.getString("httpStatus").string());
    assertEquals("ERROR", mergeResponse.getString("status").string());
    assertEquals(
        "The analytics outliers data does not exist. Please ensure analytics job was run and did not skip the outliers",
        mergeResponse.getString("message").string());
  }

  @Test
  @DisplayName("Method-level authority check fails when no required auth")
  void testMethodLevelAuthCheckNoAuth() {
    switchToNewUser("noAuth", "NoAuth");
    JsonMixed mergeResponse =
        GET("/analytics/outlierDetection/explain").content(HttpStatus.FORBIDDEN);
    assertEquals("Forbidden", mergeResponse.getString("httpStatus").string());
    assertEquals("ERROR", mergeResponse.getString("status").string());
    assertEquals(
        "Access is denied, requires one Authority from [F_PERFORM_ANALYTICS_EXPLAIN]",
        mergeResponse.getString("message").string());
  }

  @Test
  @DisplayName("Method-level authority check succeeds when having required auth")
  void testMethodLevelAuthCheckHasAuth() {
    switchToNewUser("hasAuth", "F_PERFORM_ANALYTICS_EXPLAIN");
    JsonMixed mergeResponse =
        GET("/analytics/outlierDetection/explain").content(HttpStatus.CONFLICT);
    assertEquals("Conflict", mergeResponse.getString("httpStatus").string());
    assertEquals("ERROR", mergeResponse.getString("status").string());
    assertEquals(
        "The analytics outliers data does not exist. Please ensure analytics job was run and did not skip the outliers",
        mergeResponse.getString("message").string());
  }
}
