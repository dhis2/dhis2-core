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
package org.hisp.dhis.webapi.filter;

import static org.hisp.dhis.http.HttpClientAdapter.Header;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Tests that {@link RequestIdFilter} properly captures X-Request-ID header, validates and sanitizes
 * it, and makes it available via MDC.
 */
@ContextConfiguration(classes = RequestIdFilterTest.TestConfig.class)
class RequestIdFilterTest extends H2ControllerIntegrationTestBase {

  @Test
  void shouldAddRequestIdToMdcAndCleanup() {
    // First request with X-Request-ID header
    JsonObject response1 = GET("/test/requestId", Header("X-Request-ID", "request-1")).content();
    assertEquals("request-1", response1.getString("xRequestID").string());

    // Second request with different X-Request-ID
    JsonObject response2 = GET("/test/requestId", Header("X-Request-ID", "request-2")).content();
    assertEquals("request-2", response2.getString("xRequestID").string());

    // Third request without X-Request-ID header (should be null/cleaned up)
    JsonObject response3 = GET("/test/requestId").content();
    assertNull(response3.getString("xRequestID").string());
  }

  @Test
  void shouldAcceptValidUuidAndUid() {
    // UUID is valid
    String uuid = "550e8400-e29b-41d4-a716-446655440000";
    JsonObject response1 = GET("/test/requestId", Header("X-Request-ID", uuid)).content();
    assertEquals(uuid, response1.getString("xRequestID").string());

    // DHIS2 UID is valid
    String uid = "a1b2c3d4e5f";
    JsonObject response2 = GET("/test/requestId", Header("X-Request-ID", uid)).content();
    assertEquals(uid, response2.getString("xRequestID").string());
  }

  @Test
  void shouldSanitizeInvalidRequestIds() {
    // Request with newline injection attempt
    JsonObject response1 =
        GET("/test/requestId", Header("X-Request-ID", "first\nsecond")).content();
    assertEquals("(illegal)", response1.getString("xRequestID").string());

    // Request with too long ID (>36 chars)
    JsonObject response2 =
        GET(
                "/test/requestId",
                Header("X-Request-ID", "this-is-way-too-long-to-be-valid-request-id-123456789"))
            .content();
    assertEquals("(illegal)", response2.getString("xRequestID").string());

    // Request with special characters (quotes)
    JsonObject response3 =
        GET("/test/requestId", Header("X-Request-ID", "\"malicious\"")).content();
    assertEquals("(illegal)", response3.getString("xRequestID").string());

    // Request with spaces
    JsonObject response4 =
        GET("/test/requestId", Header("X-Request-ID", "no - not having it")).content();
    assertEquals("(illegal)", response4.getString("xRequestID").string());
  }

  @Configuration
  static class TestConfig {}

  @Controller
  static class TestRequestIdController {

    @GetMapping("/api/test/requestId")
    @ResponseBody
    public String getRequestInfo() {
      String xRequestID = MDC.get("xRequestID");

      return """
          {
            "xRequestID": %s
          }
          """
          .formatted(xRequestID != null ? "\"" + xRequestID + "\"" : "null");
    }
  }
}
