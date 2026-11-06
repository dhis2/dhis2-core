/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.security;

import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.Header;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.http.HttpClientAdapter.RequestComponent;
import org.hisp.dhis.http.HttpMethod;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.AuthenticationApiTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the cross-origin request protection in the real Spring Security filter chain: requests from
 * foreign origins are answered with 403 and never reach the endpoint, while requests from the
 * server's own origin and from allowlisted origins work.
 *
 * <p>MockMvc requests go to {@code http://localhost}, so that is the server's own origin here.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CrossOriginProtectionTest extends AuthenticationApiTestBase {

  private static final String SELF_ORIGIN = "http://localhost";

  private static final String ALLOWLISTED_ORIGIN = "http://app.example.org";

  private static final String FOREIGN_ORIGIN = "http://evil.example";

  private static final String REJECTED_MESSAGE =
      "Cross-origin request rejected: origin is not allowed";

  @Autowired private ConfigurationService configurationService;

  @BeforeEach
  void setUpAllowlist() {
    configurationService.setCorsWhitelist(Set.of(ALLOWLISTED_ORIGIN));
  }

  @ParameterizedTest
  @ValueSource(strings = {SELF_ORIGIN, ALLOWLISTED_ORIGIN})
  void postFromAllowedOriginCreatesObject(String origin) {
    String name = "Constant" + CodeGenerator.generateUid();

    POST(
            "/constants",
            constantBody(name),
            Header("Origin", origin),
            Header("Sec-Fetch-Site", "cross-site"))
        .content(HttpStatus.CREATED);

    assertEquals(1, countConstantsNamed(name));
  }

  @Test
  void postFromAllowlistedOriginGetsCorsHeaders() {
    HttpResponse response =
        POST(
            "/constants",
            constantBody("Constant" + CodeGenerator.generateUid()),
            Header("Origin", ALLOWLISTED_ORIGIN));

    assertEquals(HttpStatus.CREATED, response.status());
    assertEquals(ALLOWLISTED_ORIGIN, response.header("Access-Control-Allow-Origin"));
  }

  @ParameterizedTest
  @ValueSource(strings = {FOREIGN_ORIGIN, "null", "http://localhost:8080", "https://localhost"})
  void postFromForeignOriginIsRejectedAndNotExecuted(String origin) {
    String name = "Constant" + CodeGenerator.generateUid();

    HttpResponse response = POST("/constants", constantBody(name), Header("Origin", origin));

    assertRejected(response);
    assertEquals(0, countConstantsNamed(name));
  }

  @Test
  void crossSitePostWithoutOriginIsRejectedAndNotExecuted() {
    String name = "Constant" + CodeGenerator.generateUid();

    HttpResponse response =
        POST("/constants", constantBody(name), Header("Sec-Fetch-Site", "same-site"));

    assertRejected(response);
    assertEquals(0, countConstantsNamed(name));
  }

  @Test
  void postWithoutOriginAndFetchMetadataCreatesObject() {
    String name = "Constant" + CodeGenerator.generateUid();

    POST("/constants", constantBody(name)).content(HttpStatus.CREATED);

    assertEquals(1, countConstantsNamed(name));
  }

  @Test
  void getFromForeignOriginIsRejected() {
    assertRejected(GET("/me", Header("Origin", FOREIGN_ORIGIN)));
  }

  @Test
  void preflightFromForeignOriginIsRejected() {
    HttpResponse response =
        perform(
            HttpMethod.OPTIONS,
            "/constants",
            Header("Origin", FOREIGN_ORIGIN),
            Header("Access-Control-Request-Method", "POST"));

    assertRejected(response);
    assertNull(response.header("Access-Control-Allow-Origin"));
  }

  @Test
  void preflightFromAllowlistedOriginGetsCorsHeaders() {
    HttpResponse response =
        perform(
            HttpMethod.OPTIONS,
            "/constants",
            Header("Origin", ALLOWLISTED_ORIGIN),
            Header("Access-Control-Request-Method", "POST"));

    assertEquals(HttpStatus.NO_CONTENT, response.status());
    assertEquals(ALLOWLISTED_ORIGIN, response.header("Access-Control-Allow-Origin"));
    assertEquals("POST", response.header("Access-Control-Allow-Methods"));
  }

  private static void assertRejected(HttpResponse response) {
    JsonWebMessage message = response.content(HttpStatus.FORBIDDEN).as(JsonWebMessage.class);
    assertEquals(403, message.getHttpStatusCode());
    assertEquals("ERROR", message.getStatus());
    assertEquals(REJECTED_MESSAGE, message.getMessage());
  }

  private int countConstantsNamed(String name) {
    return GET("/constants?filter=name:eq:{name}&fields=id", name)
        .content(HttpStatus.OK)
        .getArray("constants")
        .size();
  }

  private static RequestComponent constantBody(String name) {
    return Body("{'name':'%s','shortName':'%s','value':42}".formatted(name, name));
  }
}
