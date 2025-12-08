/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Tests that {@link RequestIdFilter} properly captures X-Request-ID header, validates and sanitizes
 * it, and makes it available via MDC.
 */
class RequestIdFilterTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new RequestIdFilterTestController())
            .addFilters(new RequestIdFilter())
            .build();
  }

  @Test
  void shouldAddRequestIdToMdcAndCleanup() throws Exception {
    // First request with X-Request-ID header
    MvcResult result1 =
        mockMvc
            .perform(get("/api/test/requestId").header("X-Request-ID", "request-1"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode json1 = objectMapper.readTree(result1.getResponse().getContentAsString());
    assertEquals("request-1", json1.get("xRequestID").asText());

    // Second request with different X-Request-ID
    MvcResult result2 =
        mockMvc
            .perform(get("/api/test/requestId").header("X-Request-ID", "request-2"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode json2 = objectMapper.readTree(result2.getResponse().getContentAsString());
    assertEquals("request-2", json2.get("xRequestID").asText());

    // Third request without X-Request-ID header (should be null/cleaned up)
    MvcResult result3 =
        mockMvc.perform(get("/api/test/requestId")).andExpect(status().isOk()).andReturn();
    JsonNode json3 = objectMapper.readTree(result3.getResponse().getContentAsString());
    assertNull(json3.get("xRequestID").asText(null));
  }

  @Test
  void shouldAcceptValidUuidAndUid() throws Exception {
    // UUID is valid
    String uuid = "550e8400-e29b-41d4-a716-446655440000";
    MvcResult result1 =
        mockMvc
            .perform(get("/api/test/requestId").header("X-Request-ID", uuid))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode json1 = objectMapper.readTree(result1.getResponse().getContentAsString());
    assertEquals(uuid, json1.get("xRequestID").asText());

    // DHIS2 UID is valid
    String uid = "a1b2c3d4e5f";
    MvcResult result2 =
        mockMvc
            .perform(get("/api/test/requestId").header("X-Request-ID", uid))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode json2 = objectMapper.readTree(result2.getResponse().getContentAsString());
    assertEquals(uid, json2.get("xRequestID").asText());
  }

  @Test
  void shouldSanitizeInvalidRequestIds() throws Exception {
    // Request with newline injection attempt
    MvcResult result1 =
        mockMvc
            .perform(get("/api/test/requestId").header("X-Request-ID", "first\nsecond"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode json1 = objectMapper.readTree(result1.getResponse().getContentAsString());
    assertEquals("(illegal)", json1.get("xRequestID").asText());

    // Request with too long ID (>36 chars)
    MvcResult result2 =
        mockMvc
            .perform(
                get("/api/test/requestId")
                    .header(
                        "X-Request-ID", "this-is-way-too-long-to-be-valid-request-id-123456789"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode json2 = objectMapper.readTree(result2.getResponse().getContentAsString());
    assertEquals("(illegal)", json2.get("xRequestID").asText());

    // Request with special characters (quotes)
    MvcResult result3 =
        mockMvc
            .perform(get("/api/test/requestId").header("X-Request-ID", "\"malicious\""))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode json3 = objectMapper.readTree(result3.getResponse().getContentAsString());
    assertEquals("(illegal)", json3.get("xRequestID").asText());

    // Request with spaces
    MvcResult result4 =
        mockMvc
            .perform(get("/api/test/requestId").header("X-Request-ID", "no - not having it"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode json4 = objectMapper.readTree(result4.getResponse().getContentAsString());
    assertEquals("(illegal)", json4.get("xRequestID").asText());
  }

  @Controller
  static class RequestIdFilterTestController {

    @GetMapping("/api/test/requestId")
    @ResponseBody
    public String getRequestInfo() {
      String xRequestID = MDC.get("xRequestID");
      String value = xRequestID != null ? "\"" + xRequestID + "\"" : "null";
      return String.format("{\n  \"xRequestID\": %s\n}", value);
    }
  }
}
