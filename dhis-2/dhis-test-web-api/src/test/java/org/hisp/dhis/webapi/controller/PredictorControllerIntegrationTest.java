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

import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PredictorControllerIntegrationTest extends H2ControllerIntegrationTestBase {

  @Test
  @DisplayName("Running predictors with a start date before the end date executes normally")
  void runPredictorsStartDateBeforeEndDateTest() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Generated 0 predictions",
        POST("/predictors/run?startDate=2020-01-01&endDate=2021-01-01").content());
  }

  @Test
  @DisplayName("Running predictors with a start date equaling the end date executes normally")
  void runPredictorsStartDateSameAsEndDateTest() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Generated 0 predictions",
        POST("/predictors/run?startDate=2020-01-01&endDate=2020-01-01").content());
  }

  @Test
  @DisplayName("Running predictors with a start date after the end date returns an error")
  void runPredictorsStartDateAfterEndDateTest() {
    JsonWebMessage jsonWebMessage =
        POST("/predictors/run?startDate=2021-01-01&endDate=2020-01-01")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);

    assertTrue(jsonWebMessage.getMessage().contains("Start date is after end date"));
  }

  @Test
  @DisplayName("Running a predictor with a start date after the end date returns an error")
  void runPredictorStartDateAfterEndDateTest() {
    JsonWebMessage jsonWebMessage =
        POST("/predictors/predUid0001/run?startDate=2021-01-01&endDate=2020-01-01")
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);

    assertTrue(jsonWebMessage.getMessage().contains("Start date is after end date"));
  }
}
