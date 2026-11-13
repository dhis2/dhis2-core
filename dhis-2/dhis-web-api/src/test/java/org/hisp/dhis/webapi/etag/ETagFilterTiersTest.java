/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.etag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.setting.ThreadUserSettings;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.mvc.interceptor.ETagConditionalInterceptor;
import org.hisp.dhis.webapi.mvc.interceptor.ETagConditionalResponseBodyAdvice;
import org.hisp.dhis.webapi.service.ETagConditionalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link ETagFilter} together with the smart tier ({@link ETagConditionalInterceptor} and {@link
 * ETagConditionalResponseBodyAdvice}): which ETag each kind of response ends up with.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ETagFilterTiersTest {

  @BeforeEach
  void setUp() {
    UserDetails user = mock(UserDetails.class);
    when(user.getUid()).thenReturn("userUid123");
    when(user.getUsername()).thenReturn("etag-tier-user");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    ThreadUserSettings.clear();
  }

  @Test
  void coveredEndpointGetsTheSmartETag() throws Exception {
    MockHttpServletResponse response = perform(enabledService(), "/api/systemSettings");

    String etag = response.getHeader(HttpHeaders.ETAG);
    assertNotNull(etag);
    assertEquals(66, etag.length(), "quoted SHA-256 smart ETag: " + etag);
  }

  @Test
  void uncoveredEndpointGetsTheBodyHash() throws Exception {
    MockHttpServletResponse response = perform(enabledService(), "/api/system/info");

    assertEquals(bodyHash(response), response.getHeader(HttpHeaders.ETAG));
  }

  @Test
  void coveredEndpointThatBypassesTheAdviceGetsTheBodyHash() throws Exception {
    MockHttpServletResponse response = perform(enabledService(), "/api/periodTypes");

    assertEquals(bodyHash(response), response.getHeader(HttpHeaders.ETAG));
  }

  @Test
  void forcedOffCoveredEndpointGetsTheBodyHash() throws Exception {
    MockHttpServletResponse response = perform(forcedOffService(), "/api/systemSettings");

    assertEquals(bodyHash(response), response.getHeader(HttpHeaders.ETAG));
  }

  @Test
  void matchingBodyHashGets304WhenForcedOff() throws Exception {
    ETagService off = forcedOffService();
    String etag = perform(off, "/api/systemSettings").getHeader(HttpHeaders.ETAG);

    MockHttpServletResponse second =
        mvc(off)
            .perform(get("/api/systemSettings").header(HttpHeaders.IF_NONE_MATCH, etag))
            .andReturn()
            .getResponse();

    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, second.getStatus());
    assertTrue(second.getContentAsString().isEmpty());
  }

  private static ETagService enabledService() {
    ETagService service = mock(ETagService.class);
    when(service.isEnabled()).thenReturn(true);
    when(service.getTtlMinutes()).thenReturn(10);
    when(service.getStaleWhileRevalidateSeconds()).thenReturn(60);
    return service;
  }

  /**
   * Answers like {@code ETagNoOpService}, the bean active when the cache is off or forced off (that
   * class lives in a module dhis-web-api may only use at runtime).
   */
  private static ETagService forcedOffService() {
    ETagService service = mock(ETagService.class);
    when(service.isEnabled()).thenReturn(false);
    return service;
  }

  private static MockHttpServletResponse perform(ETagService eTagService, String path)
      throws Exception {
    return mvc(eTagService).perform(get(path)).andReturn().getResponse();
  }

  private static MockMvc mvc(ETagService eTagService) {
    ETagConditionalService conditional = new ETagConditionalService(eTagService);
    SchemaService schemaService = mock(SchemaService.class);
    ETagConditionalInterceptor interceptor =
        new ETagConditionalInterceptor(
            conditional, schemaService, new ETagFieldsHopAnalyzer(schemaService), null, null);
    return MockMvcBuilders.standaloneSetup(new TestController())
        .addInterceptors(interceptor)
        .setControllerAdvice(new ETagConditionalResponseBodyAdvice(conditional))
        .addFilters(new ETagFilter())
        .build();
  }

  private static String bodyHash(MockHttpServletResponse response) {
    return "\"0" + DigestUtils.md5DigestAsHex(response.getContentAsByteArray()) + "\"";
  }

  @RestController
  static class TestController {
    @GetMapping("/api/systemSettings")
    Map<String, String> systemSettings() {
      return Map.of("keyStyle", "light_blue");
    }

    @GetMapping("/api/system/info")
    Map<String, String> systemInfo() {
      return Map.of("version", "2.44");
    }

    /** Writes the body directly, so the response advice never adds the stored smart ETag. */
    @GetMapping("/api/periodTypes")
    void periodTypes(HttpServletResponse response) throws IOException {
      response.setContentType("application/json");
      response.getOutputStream().write("[\"Monthly\"]".getBytes(StandardCharsets.UTF_8));
    }
  }
}
