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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.sms.config.ClickatellGatewayConfig;
import org.hisp.dhis.sms.config.GatewayAdministrationService;
import org.hisp.dhis.sms.config.GenericHttpGatewayConfig;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.sms.SmsGatewayController} using (mocked) REST
 * requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class SmsGatewayControllerTest extends H2ControllerIntegrationTestBase {

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
    String json =
        """
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
    JsonObject gatewayNoAuth =
        JsonMixed.of(gateway.node().removeMembers(Set.of("password", "authToken")));

    assertTrue(gatewayNoAuth.getString("password").isUndefined());
    assertTrue(gatewayNoAuth.getString("authToken").isUndefined());

    assertStatus(HttpStatus.OK, PUT("/gateways/" + uid, gatewayNoAuth.toJson()));

    ClickatellGatewayConfig config =
        (ClickatellGatewayConfig)
            gatewayAdministrationService.getByUid(gateway.getString("uid").string());
    assertNotNull(config.getPassword(), "internally the password should still be set");
    assertNotEquals("pwd", config.getPassword(), "password should be stored encoded but was plain");
    assertNotNull(config.getAuthToken(), "internally the authToken should still be set");
  }

  @Test
  void testUpdateGateway_Generic() {
    // language=JSON
    String json =
        """
        {
        "type": "http",
        "configurationTemplate": "/foo",
        "contentType": "APPLICATION_JSON",
        "name": "test",
        "parameters": [
          {
            "key": "foo",
            "value": "bar",
            "header": false,
            "encode": false,
            "confidential": true
          }
        ],
        "sendUrlParameters": false,
        "urlTemplate": "http://example.com",
        "useGet": false
      }""";
    uid = assertStatus(HttpStatus.OK, POST("/gateways", json));

    JsonObject gateway = GET("/gateways/{uid}", uid).content();
    JsonObject param0 = gateway.getArray("parameters").getObject(0);
    String value = param0.getString("value").string();
    assertNotNull(value);
    assertNotEquals("bar", value, "should be encoded");

    JsonObject gatewayNoParamValue = JsonMixed.of(param0.node().removeMembers(Set.of("value")));
    assertTrue(gatewayNoParamValue.getArray("parameters").getObject(0).isUndefined("value"));

    assertStatus(HttpStatus.OK, PUT("/gateways/" + uid, gatewayNoParamValue.toJson()));
    GenericHttpGatewayConfig config =
        (GenericHttpGatewayConfig)
            gatewayAdministrationService.getByUid(gateway.getString("uid").string());
    String updatedValue = config.getParameters().get(0).getValue();
    assertNotNull(updatedValue);
    assertEquals(value, updatedValue);
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
