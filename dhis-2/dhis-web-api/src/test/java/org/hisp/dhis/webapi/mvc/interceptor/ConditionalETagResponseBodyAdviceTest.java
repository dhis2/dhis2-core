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
package org.hisp.dhis.webapi.mvc.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.service.ConditionalETagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

class ConditionalETagResponseBodyAdviceTest {

  private final ETagService eTagService = mock(ETagService.class);
  private final SchemaService schemaService = mock(SchemaService.class);

  private MockMvc mockMvc;
  private TestController controller;

  @BeforeEach
  void setUp() {
    UserDetails userDetails = mock(UserDetails.class);
    lenient().when(userDetails.getUid()).thenReturn("userUid123");

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, List.of()));

    when(eTagService.isEnabled()).thenReturn(true);
    when(eTagService.getTtlMinutes()).thenReturn(60);
    lenient().when(eTagService.getEntityTypeVersion(any())).thenReturn(1L);

    ConditionalETagService conditionalETagService = new ConditionalETagService(eTagService);
    controller = new TestController();
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .addInterceptors(
                new ConditionalETagInterceptor(conditionalETagService, schemaService, null, null))
            .setControllerAdvice(new ConditionalETagResponseBodyAdvice(conditionalETagService))
            .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void testGetApiMeSettingsIncludesETagHeaders() throws Exception {
    mockMvc
        .perform(get("/api/me/settings"))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"))
        .andExpect(header().string("Vary", "Cookie, Authorization"))
        .andExpect(
            header()
                .string(
                    "Cache-Control",
                    CacheControl.noCache().cachePrivate().mustRevalidate().getHeaderValue()));
  }

  @Test
  void testGetApiSystemSettingsKeyWithContextPathIncludesETagHeaders() throws Exception {
    mockMvc
        .perform(
            get("/server1/api/systemSettings/applicationTitle")
                .contextPath("/server1")
                .param("fields", "id"))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"))
        .andExpect(header().string("Vary", "Cookie, Authorization"))
        .andExpect(
            header()
                .string(
                    "Cache-Control",
                    CacheControl.noCache().cachePrivate().mustRevalidate().getHeaderValue()));
  }

  @Test
  void testMatchingETagReturns304AndSkipsController() throws Exception {
    MvcResult firstResult =
        mockMvc.perform(get("/api/me/settings")).andExpect(status().isOk()).andReturn();
    String etag = firstResult.getResponse().getHeader("ETag");

    assertEquals(1, controller.meSettingsResponses.get());

    mockMvc
        .perform(get("/api/me/settings").header("If-None-Match", etag))
        .andExpect(status().isNotModified())
        .andExpect(header().string("ETag", etag));

    assertEquals(1, controller.meSettingsResponses.get());
  }

  @Test
  void testNonCompositeEndpointDoesNotGetAutomaticETagHeaders() throws Exception {
    mockMvc
        .perform(get("/api/ping"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("ETag"))
        .andExpect(header().doesNotExist("Vary"));
  }

  @Test
  void testNon2xxResponseDoesNotGetAutomaticETagHeaders() throws Exception {
    mockMvc
        .perform(get("/api/me/settings").param("error", "true"))
        .andExpect(status().isBadRequest())
        .andExpect(header().doesNotExist("ETag"))
        .andExpect(header().doesNotExist("Vary"));
  }

  @Test
  void testHeadApiMeSettingsIncludesETagHeaders() throws Exception {
    mockMvc
        .perform(head("/api/me/settings"))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"))
        .andExpect(header().string("Vary", "Cookie, Authorization"))
        .andExpect(
            header()
                .string(
                    "Cache-Control",
                    CacheControl.noCache().cachePrivate().mustRevalidate().getHeaderValue()));
  }

  @Controller
  private static class TestController {
    private final AtomicInteger meSettingsResponses = new AtomicInteger();

    @GetMapping("/api/me/settings")
    public @ResponseBody ResponseEntity<Map<String, String>> meSettings(
        @RequestParam(required = false) Boolean error) {
      if (Boolean.TRUE.equals(error)) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "bad"));
      }

      meSettingsResponses.incrementAndGet();
      return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/api/systemSettings/applicationTitle")
    public @ResponseBody ResponseEntity<Map<String, String>> systemSetting() {
      return ResponseEntity.ok(Map.of("status", "application-title"));
    }

    @GetMapping("/api/ping")
    public @ResponseBody ResponseEntity<Map<String, String>> ping() {
      return ResponseEntity.ok(Map.of("status", "pong"));
    }
  }
}
