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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link LegendSetController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class LegendSetControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testPostJsonObject() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        null,
        POST("/legendSets/", "{'name':'LS'}").content(HttpStatus.CREATED));
  }

  @Test
  void testPutJsonObject() {
    String id = assertStatus(HttpStatus.CREATED, POST("/legendSets/", "{'name':'LS'}"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        null,
        PUT("/legendSets/" + id, "{'name':'New LS'}").content(HttpStatus.OK));
  }

  @Test
  void testDeleteObject() {
    String id = assertStatus(HttpStatus.CREATED, POST("/legendSets/", "{'name':'LS'}"));
    assertWebMessage("OK", 200, "OK", null, DELETE("/legendSets/" + id).content(HttpStatus.OK));
  }
}
