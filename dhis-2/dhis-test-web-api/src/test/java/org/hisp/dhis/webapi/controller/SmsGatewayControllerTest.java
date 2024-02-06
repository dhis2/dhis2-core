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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.sms.config.ClickatellGatewayConfig;
import org.hisp.dhis.sms.config.GatewayAdministrationService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.sms.SmsGatewayController} using (mocked) REST
 * requests.
 *
 * @author Jan Bernitt
 */
class SmsGatewayControllerTest extends DhisControllerConvenienceTest {

  @Autowired private GatewayAdministrationService gatewayAdministrationService;

  private String uid;

  @AfterEach
  void tearDown() {
    JsonArray gateways = GET("/gateways").content().getArray("gateways");
    for (JsonObject gateway : gateways.asList(JsonObject.class)) {
      assertStatus(HttpStatus.OK, DELETE("/gateways/" + gateway.getString("uid").string()));
    }
    assertTrue(GET("/gateways").content().getArray("gateways").isEmpty());
  }

  @Test
  void testSetDefault() {
    String json = "{'name':'test', 'username':'user', 'password':'pwd', 'type':'http'}";
    uid = assertStatus(HttpStatus.OK, POST("/gateways", json));

    assertWebMessage(
        "OK",
        200,
        "OK",
        "test is set to default",
        PUT("/gateways/default/" + uid).content(HttpStatus.OK));
  }

  @Test
  void testSetDefault_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "SmsGatewayConfig with id xyz could not be found.",
        PUT("/gateways/default/xyz").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testUpdateGateway() {
    // language=JSON
    String json = """
      {"name":"test", "username":"user", "password":"pwd", "type":"http"}""";
    uid = assertStatus(HttpStatus.OK, POST("/gateways", json));

    JsonObject gateway = GET("/gateways/{uid}", uid).content();
    JsonObject gatewayNoPass = JsonMixed.of(gateway.node().removeMembers(Set.of("password")));
    assertTrue(gatewayNoPass.getString("password").isUndefined());

    assertWebMessage(
        "OK",
        200,
        "OK",
        "Gateway with uid: " + uid + " has been updated",
        PUT("/gateways/" + uid, gatewayNoPass.toJson()).content(HttpStatus.OK));

    String password =
        gatewayAdministrationService.getByUid(gateway.getString("uid").string()).getPassword();
    assertNotNull(password, "internally the password should still be set");
    assertNotEquals("pwd", password, "password should be stored encoded but was plain");
  }

  @Test
  void testUpdateGateway_Clickatell() {
    // language=JSON
    String json =
        """
      {"name":"testclick", "username":"user", "password":"pwd", "type":"clickatell", "authToken": "token"}""";
    uid = assertStatus(HttpStatus.OK, POST("/gateways", json));

    JsonObject gateway = GET("/gateways/{uid}", uid).content();
    JsonObject gatewayNoAuth = JsonMixed.of(gateway.node().removeMembers(Set.of("password", "authToken")));

    assertTrue(gatewayNoAuth.getString("password").isUndefined());
    assertTrue(gatewayNoAuth.getString("authToken").isUndefined());

    assertWebMessage(
        "OK",
        200,
        "OK",
        "Gateway with uid: " + uid + " has been updated",
        PUT("/gateways/" + uid, gatewayNoAuth.toJson()).content(HttpStatus.OK));

    ClickatellGatewayConfig config =
        (ClickatellGatewayConfig)
            gatewayAdministrationService.getByUid(gateway.getString("uid").string());
    assertNotNull(config.getPassword(), "internally the password should still be set");
    assertNotEquals("pwd", config.getPassword(), "password should be stored encoded but was plain");
    assertNotNull(config.getAuthToken(), "internally the authToken should still be set");
  }

  @Test
  void testUpdateGateway_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "SmsGatewayConfig with id xyz could not be found.",
        PUT("/gateways/xyz").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testAddGateway() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Gateway configuration added",
        POST("/gateways", "{'name':'test', 'username':'user', 'password':'pwd', 'type':'http'}")
            .content(HttpStatus.OK));
  }

  @Test
  void testRemoveGateway() {
    uid =
        assertStatus(
            HttpStatus.OK,
            POST(
                "/gateways",
                "{'name':'test', 'username':'user', 'password':'pwd', 'type':'http'}"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Gateway removed successfully",
        DELETE("/gateways/" + uid).content(HttpStatus.OK));
  }

  @Test
  void testRemoveGateway_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "SmsGatewayConfig with id xyz could not be found.",
        DELETE("/gateways/xyz").content(HttpStatus.NOT_FOUND));
  }
}
