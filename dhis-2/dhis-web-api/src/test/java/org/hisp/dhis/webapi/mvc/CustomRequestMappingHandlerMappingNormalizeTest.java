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
package org.hisp.dhis.webapi.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import org.hisp.dhis.webapi.view.SuffixMediaTypeContentNegotiationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * Unit coverage for the Spring 7-ready path-extension / trailing-slash fallback in {@link
 * CustomRequestMappingHandlerMapping}.
 *
 * @author Morten Svanæs
 */
class CustomRequestMappingHandlerMappingNormalizeTest {

  private CustomRequestMappingHandlerMapping mapping;

  @BeforeEach
  void setUp() throws Exception {
    mapping = new CustomRequestMappingHandlerMapping();
    mapping.setApplicationContext(new StaticApplicationContext());
    mapping.afterPropertiesSet();

    Method method = SampleController.class.getMethod("list");
    mapping.registerMapping(
        mapping.getMappingForMethod(method, SampleController.class),
        new SampleController(),
        method);

    Method openapi = SampleController.class.getMethod("openapi");
    mapping.registerMapping(
        mapping.getMappingForMethod(openapi, SampleController.class),
        new SampleController(),
        openapi);
  }

  @Test
  void literalExtensionMappingWinsWhenStrippedPathHasNoHandler() throws Exception {
    MockHttpServletRequest request = request("/openapi/openapi.json");
    HandlerMethod handler = mapping.getHandlerInternal(request);
    assertNotNull(handler);
    assertEquals("openapi", handler.getMethod().getName());
    // Registered .json extension is still recorded for content negotiation.
    assertEquals(
        MediaType.APPLICATION_JSON,
        request.getAttribute(
            SuffixMediaTypeContentNegotiationStrategy.SUFFIX_MEDIA_TYPE_ATTRIBUTE));
  }

  @ParameterizedTest
  @CsvSource({
    "/api/dataElements.json, application/json",
    "/api/dataElements.xml, application/xml",
    "/api/dataElements.json.zip, application/json+zip",
    "/api/dataElements.adx.xml, application/adx+xml",
  })
  void registeredSuffixIsStrippedAndMediaTypeRecorded(String path, String mediaType)
      throws Exception {
    MockHttpServletRequest request = request(path);
    HandlerMethod handler = mapping.getHandlerInternal(request);
    assertNotNull(handler, "expected handler for " + path);
    assertEquals("list", handler.getMethod().getName());
    assertEquals(
        MediaType.parseMediaType(mediaType),
        request.getAttribute(
            SuffixMediaTypeContentNegotiationStrategy.SUFFIX_MEDIA_TYPE_ATTRIBUTE));
  }

  @Test
  void trailingSlashIsStrippedForApiPaths() throws Exception {
    MockHttpServletRequest request = request("/api/dataElements/");
    HandlerMethod handler = mapping.getHandlerInternal(request);
    assertNotNull(handler);
    assertEquals("list", handler.getMethod().getName());
  }

  @Test
  void trailingSlashWorksEvenWhenLookupPathCachedFromFirstAttempt() throws Exception {
    MockHttpServletRequest request = request("/api/dataElements/");
    // Simulate DispatcherServlet / first lookup caching the unnormalised path.
    request.setAttribute(HandlerMapping.LOOKUP_PATH, "/api/dataElements/");
    request.setAttribute(UrlPathHelper.PATH_ATTRIBUTE, "/api/dataElements/");
    HandlerMethod handler = mapping.getHandlerInternal(request);
    assertNotNull(handler);
    assertEquals("list", handler.getMethod().getName());
  }

  @Test
  void trailingSlashPlusSuffixIsStripped() throws Exception {
    MockHttpServletRequest request = request("/api/dataElements.json/");
    HandlerMethod handler = mapping.getHandlerInternal(request);
    assertNotNull(handler);
    assertEquals(
        MediaType.APPLICATION_JSON,
        request.getAttribute(
            SuffixMediaTypeContentNegotiationStrategy.SUFFIX_MEDIA_TYPE_ATTRIBUTE));
  }

  @Test
  void unknownExtensionDoesNotMatchFallback() throws Exception {
    MockHttpServletRequest request = request("/api/dataElements.unknown");
    HandlerMethod handler = mapping.getHandlerInternal(request);
    assertNull(handler);
  }

  @Test
  void plainPathMatchesWithoutAttribute() throws Exception {
    MockHttpServletRequest request = request("/api/dataElements");
    HandlerMethod handler = mapping.getHandlerInternal(request);
    assertNotNull(handler);
    assertNull(
        request.getAttribute(
            SuffixMediaTypeContentNegotiationStrategy.SUFFIX_MEDIA_TYPE_ATTRIBUTE));
  }

  @Test
  void normalizeReturnsSameRequestWhenNothingToStrip() throws Exception {
    Method normalize =
        CustomRequestMappingHandlerMapping.class.getDeclaredMethod(
            "normalize", HttpServletRequest.class);
    normalize.setAccessible(true);
    MockHttpServletRequest request = request("/api/dataElements");
    Object result = normalize.invoke(mapping, request);
    assertSame(request, result);
  }

  private static MockHttpServletRequest request(String path) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setServletPath(path);
    request.setPathInfo(null);
    return request;
  }

  @Controller
  @RequestMapping
  static class SampleController {
    @GetMapping("/api/dataElements")
    public String list() {
      return "ok";
    }

    @GetMapping("/openapi/openapi.json")
    public String openapi() {
      return "openapi";
    }
  }
}
