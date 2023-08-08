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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link PredictionController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class PredictionControllerTest extends DhisControllerConvenienceTest {

  @Test
  void testRunPredictors() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        null,
        POST("/38/predictions?startDate=2020-01-01&endDate=2021-01-01").content(HttpStatus.OK));
  }

  @Test
  void testRunPredictors_Pre38() {
    JsonObject summary =
        POST("/37/predictions?startDate=2020-01-01&endDate=2021-01-01").content(HttpStatus.OK);
    assertEquals("SUCCESS", summary.getString("status").string());
    assertEquals(0, summary.getNumber("predictors").intValue());
  }

  @Test
  void testRunPredictors_Async() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Initiated inMemoryPrediction",
        POST("/predictions?startDate=2020-01-01&endDate=2021-01-01&async=true")
            .content(HttpStatus.OK));
  }
}
