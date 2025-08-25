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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** Tests the {@link PredictorController} using (mocked) REST requests. */
@Transactional
class PredictorControllerTest extends PostgresControllerIntegrationTestBase {

  @Test
  void testGetExpressionDescription() {
    JsonWebMessage response =
        POST("/predictors/expression/description", "0").content().as(JsonWebMessage.class);
    assertWebMessage("OK", 200, "OK", "Valid", response);
    assertEquals("0", response.getDescription());
  }

  @Test
  void testGetExpressionDescription_InvalidExpression() {
    JsonWebMessage response =
        POST("/predictors/expression/description", "1 <> 1").content().as(JsonWebMessage.class);
    assertWebMessage("OK", 200, "ERROR", "Expression is not well-formed", response);
    assertNull(response.getDescription());
  }

  @Test
  void testGetSkipTestDescription() {
    JsonWebMessage response =
        POST("/predictors/skipTest/description", "1 != 1").content().as(JsonWebMessage.class);
    assertWebMessage("OK", 200, "OK", "Valid", response);
    assertEquals("1 != 1", response.getDescription());
  }

  @Test
  void testGetSkipTestDescription_InvalidExpression() {
    JsonWebMessage response =
        POST("/predictors/skipTest/description", "1 <> 1").content().as(JsonWebMessage.class);
    assertWebMessage("OK", 200, "ERROR", "Expression is not well-formed", response);
    assertNull(response.getDescription());
  }

  @Test
  void testRunPredictor() {
    String pId = postNewPredictor();

    User actingUser = getAdminUser();
    UserDetails currentUserDetails = UserDetails.fromUser(actingUser);
    currentUserDetails.setId(0L);
    injectSecurityContext(currentUserDetails);

    assertWebMessage(
        "OK",
        200,
        "OK",
        "Generated 0 predictions",
        POST("/predictors/" + pId + "/run?startDate=2020-01-01&endDate=2021-01-01").content());
  }

  @Test
  void testRunPredictors() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Generated 0 predictions",
        POST("/predictors/run?startDate=2020-01-01&endDate=2021-01-01").content());
  }

  private String postNewPredictor() {
    String ccId =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0)
            .getString("id")
            .string();
    String deId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements/",
                "{'name':'My data element', 'shortName':'DE1', 'code':'DE1', 'valueType':'INTEGER', "
                    + "'aggregationType':'SUM', 'zeroIsSignificant':false, 'domainType':'AGGREGATE', "
                    + "'categoryCombo': {'id': '"
                    + ccId
                    + "'}}"));
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/predictors/",
            "{'name':'Pred1','shortName':'Pred1',"
                + "'output': {'id':'"
                + deId
                + "'}, "
                + "'generator': {'expression': '1 != 1'},"
                + "'periodType': 'Monthly',"
                + "'sequentialSampleCount':4,"
                + "'annualSampleCount':3,"
                + "'organisationUnitLevels': [],"
                + "'organisationUnitDescendants': 'SELECTED'"
                + " }"));
  }
}
