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
package org.hisp.dhis.webapi.etag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.setting.ThreadUserSettings;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.mvc.interceptor.ETagConditionalInterceptor;
import org.hisp.dhis.webapi.mvc.interceptor.ETagConditionalResponseBodyAdvice;
import org.hisp.dhis.webapi.service.ETagConditionalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The smart tier covers only the list, object and property paths of a metadata resource. Custom
 * sub-endpoints below a resource read or write data its dependency types do not describe, so they
 * must fall to {@link ETagFilter}'s body hash.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ETagSmartTierPathScopeTest {

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    UserDetails user = mock(UserDetails.class);
    when(user.getUid()).thenReturn("userUid123");
    when(user.getUsername()).thenReturn("etag-scope-user");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    mvc = mvc();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    ThreadUserSettings.clear();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/trackedEntityAttributes/abcdefghijk/generate",
        "/api/trackedEntityAttributes/abcdefghijk/generateAndReserve",
        "/api/sqlViews/abcdefghijk/data",
        "/api/dataSets/abcdefghijk/form"
      })
  void customSubEndpointGetsNoSmartETagAndNoSmart304(String path) throws Exception {
    MockHttpServletResponse first = perform(path, null);
    String etag = first.getHeader(HttpHeaders.ETAG);

    MockHttpServletResponse second = perform(path, etag);

    assertEquals(HttpServletResponse.SC_OK, second.getStatus(), "no 304 for changed data");
    assertNotEquals(first.getContentAsString(), second.getContentAsString());
    assertEquals(bodyHash(first), etag, "body-hash ETag expected, not a smart one");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"/api/dataSets", "/api/dataSets/abcdefghijk", "/api/dataSets/abcdefghijk/name"})
  void listObjectAndPropertyPathsKeepTheSmart304(String path) throws Exception {
    MockHttpServletResponse first = perform(path, null);
    String etag = first.getHeader(HttpHeaders.ETAG);
    assertNotNull(etag);
    assertEquals(66, etag.length(), "quoted SHA-256 smart ETag: " + etag);

    MockHttpServletResponse second = perform(path, etag);

    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, second.getStatus());
    assertEquals(etag, second.getHeader(HttpHeaders.ETAG));
  }

  private MockHttpServletResponse perform(String path, String ifNoneMatch) throws Exception {
    var request = get(path);
    if (ifNoneMatch != null) {
      request.header(HttpHeaders.IF_NONE_MATCH, ifNoneMatch);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static MockMvc mvc() {
    ETagService eTagService = mock(ETagService.class);
    when(eTagService.isEnabled()).thenReturn(true);
    when(eTagService.getTtlMinutes()).thenReturn(10);
    ETagConditionalService conditional = new ETagConditionalService(eTagService);

    SchemaService schemaService = mock(SchemaService.class);
    when(schemaService.getMetadataSchemas())
        .thenReturn(
            List.of(
                new Schema(
                    TrackedEntityAttribute.class,
                    "trackedEntityAttribute",
                    "trackedEntityAttributes"),
                new Schema(SqlView.class, "sqlView", "sqlViews"),
                new Schema(DataSet.class, "dataSet", "dataSets")));

    ETagConditionalInterceptor interceptor =
        new ETagConditionalInterceptor(
            conditional, schemaService, new ETagFieldsHopAnalyzer(schemaService), null, null);
    return MockMvcBuilders.standaloneSetup(
            new DataSetController(), new SqlViewController(), new AttributeController())
        .addInterceptors(interceptor)
        .setControllerAdvice(new ETagConditionalResponseBodyAdvice(conditional))
        .addFilters(new ETagFilter())
        .build();
  }

  private static String bodyHash(MockHttpServletResponse response) {
    return "\"0" + DigestUtils.md5DigestAsHex(response.getContentAsByteArray()) + "\"";
  }

  /** The generic read mappings of AbstractFullReadOnlyController, and a custom sub-endpoint. */
  @RestController
  @RequestMapping("/api/dataSets")
  static class DataSetController {
    /** Data values change without a DataSet write. */
    private final AtomicInteger formValue = new AtomicInteger();

    @GetMapping
    Map<String, String> list() {
      return Map.of("dataSets", "list");
    }

    @GetMapping("/{uid:[a-zA-Z0-9]{11}}")
    Map<String, String> object(@PathVariable String uid) {
      return Map.of("id", uid);
    }

    @GetMapping("/{uid:[a-zA-Z0-9]{11}}/{property}")
    Map<String, String> property(@PathVariable String uid, @PathVariable String property) {
      return Map.of(property, uid);
    }

    @GetMapping("/{uid}/form")
    Map<String, Integer> form(@PathVariable String uid) {
      return Map.of("value", formValue.incrementAndGet());
    }
  }

  @RestController
  @RequestMapping("/api/sqlViews")
  static class SqlViewController {
    /** The rows of a SQL view change without a SqlView write. */
    private final AtomicInteger rows = new AtomicInteger();

    @GetMapping("/{uid}/data")
    Map<String, Integer> data(@PathVariable String uid) {
      return Map.of("rows", rows.incrementAndGet());
    }
  }

  @RestController
  @RequestMapping("/api/trackedEntityAttributes")
  static class AttributeController {
    /** Every call reserves a new value. */
    private final AtomicInteger reserved = new AtomicInteger();

    @GetMapping("/{id}/generate")
    Map<String, Integer> generate(@PathVariable String id) {
      return Map.of("value", reserved.incrementAndGet());
    }

    @GetMapping("/{id}/generateAndReserve")
    List<Map<String, Integer>> generateAndReserve(@PathVariable String id) {
      return List.of(Map.of("value", reserved.incrementAndGet()));
    }
  }
}
