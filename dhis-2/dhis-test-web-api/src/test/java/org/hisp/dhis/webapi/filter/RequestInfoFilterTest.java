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

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.RequestInfo;
import org.hisp.dhis.common.RequestInfoService;
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
 * Tests that {@link RequestInfoFilter} properly captures X-Request-ID header and makes it available
 * in the request context via {@link RequestInfoService}.
 */
@ContextConfiguration(classes = RequestInfoFilterTest.TestConfig.class)
class RequestInfoFilterTest extends H2ControllerIntegrationTestBase {

  @Test
  void testRequestInfoFilter_RequestIdAndMdc() {
    // First request with X-Request-ID header
    JsonObject response1 = GET("/test/requestInfo", Header("X-Request-ID", "request-1")).content();
    assertEquals("request-1", response1.getString("requestId").string());
    assertEquals("request-1", response1.getString("mdcValue").string());

    // Second request with different X-Request-ID
    JsonObject response2 = GET("/test/requestInfo", Header("X-Request-ID", "request-2")).content();
    assertEquals("request-2", response2.getString("requestId").string());
    assertEquals("request-2", response2.getString("mdcValue").string());

    // Third request without X-Request-ID header (should be null/cleaned up)
    JsonObject response3 = GET("/test/requestInfo").content();
    assertNull(response3.getString("requestId").string());
    assertNull(response3.getString("mdcValue").string());
  }

  @Configuration
  static class TestConfig {}

  @Controller
  @RequiredArgsConstructor
  static class TestRequestInfoController {
    private final RequestInfoService requestInfoService;

    @GetMapping("/api/test/requestInfo")
    @ResponseBody
    public String getRequestInfo() {
      RequestInfo info = requestInfoService.getCurrentInfo();
      String requestId = info != null ? info.getHeaderXRequestID() : null;
      String mdcValue = MDC.get("xRequestID");

      return """
          {
            "requestId": %s,
            "mdcValue": %s
          }
          """
          .formatted(
              requestId != null ? "\"" + requestId + "\"" : "null",
              mdcValue != null ? "\"" + mdcValue + "\"" : "null");
    }
  }
}
