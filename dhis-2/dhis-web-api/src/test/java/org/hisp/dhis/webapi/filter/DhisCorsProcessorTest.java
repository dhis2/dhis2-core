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
package org.hisp.dhis.webapi.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.hisp.dhis.setting.SystemSettings;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DhisCorsProcessorTest {

  @Test
  void processRequest_allowsOriginFromSystemSettingsCorsWhitelist() {
    DhisCorsProcessor processor = processor(Set.of("https://allowed.example.org"));
    MockHttpServletRequest request = request("https://allowed.example.org");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(processor.processRequest(null, request, response));

    assertEquals("https://allowed.example.org", response.getHeader("Access-Control-Allow-Origin"));
  }

  @Test
  void processRequest_rejectsOriginMissingFromSystemSettingsCorsWhitelist() {
    DhisCorsProcessor processor = processor(Set.of("https://allowed.example.org"));
    MockHttpServletRequest request = request("https://denied.example.org");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertFalse(processor.processRequest(null, request, response));
  }

  private static DhisCorsProcessor processor(Set<String> corsWhitelist) {
    SystemSettings settings =
        SystemSettings.of(
            Map.of("corsWhitelist", SystemSettings.encodeCorsWhitelist(corsWhitelist)));
    return new DhisCorsProcessor(() -> settings);
  }

  private static MockHttpServletRequest request(String origin) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
    request.setScheme("https");
    request.setServerName("dhis.example.org");
    request.setServerPort(443);
    request.addHeader(DhisCorsProcessor.CORS_ORIGIN, origin);
    return request;
  }
}
