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
package org.hisp.dhis.route;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.helpers.TestCleanUp;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RouteActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RouteTest extends ApiTest {

  private RouteActions routeActions;

  @BeforeEach
  public void setUp() {
    new TestCleanUp().deleteCreatedEntities("/routes");

    routeActions = new RouteActions();

    LoginActions loginActions = new LoginActions();
    loginActions.loginAsDefaultUser();
  }

  @Test
  void testRunRoute() {
    JsonObject routeJsonObject = new JsonObject();
    routeJsonObject.addProperty("name", "route-under-test");
    routeJsonObject.addProperty("url", "http://web:8080/api/system/info");

    JsonObject authJsonObject = new JsonObject();
    authJsonObject.addProperty("type", "http-basic");
    authJsonObject.addProperty("username", "admin");
    authJsonObject.addProperty("password", "district");

    routeJsonObject.add("auth", authJsonObject);

    ApiResponse postApiResponse = routeActions.post(routeJsonObject);
    postApiResponse.validate().statusCode(201);
    String id = postApiResponse.getBody().getAsJsonObject("response").get("uid").getAsString();

    ApiResponse runApiResponse = routeActions.get(id + "/run");
    runApiResponse.validate().statusCode(200);
    assertNotNull(runApiResponse.getBody().get("version").getAsString());
  }
}
