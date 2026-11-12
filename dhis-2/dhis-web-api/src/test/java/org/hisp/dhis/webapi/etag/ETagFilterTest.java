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
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;

/**
 * Ported from master's {@code ExcludableShallowEtagHeaderFilterTest}, plus the two-tier behaviour
 * of {@link ETagFilter}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ETagFilterTest {

  private static final String HELLO_WORLD_ETAG = "\"0b10a8db164e0754105b7a99be72e3fe5\"";

  private final ETagFilter filter = new ETagFilter();

  private static final FilterChain HELLO_WORLD =
      (filterRequest, filterResponse) -> {
        ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
        filterResponse.setContentType(MediaType.TEXT_PLAIN_VALUE);
        FileCopyUtils.copy("Hello World".getBytes(), filterResponse.getOutputStream());
      };

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/dataValues",
        "/api/41/dataValues",
        "/api/dataValueSets",
        "/api/41/fileResources/abcdefghijk",
        "/api/dataEntry/metadata",
        "/api/metadata/version/Version_1/data",
        "/api/42/metadata/version/Version_1/data.gz",
        "/api/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/file",
        "/api/41/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/file",
        "/api/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/image",
        "/api/41/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/image",
        "/api/tracker/trackedEntities/vOxUH373fy5/attributes/nDlikr3TUS6/file",
        "/api/41/tracker/trackedEntities/vOxUH373fy5/attributes/nDlikr3TUS6/file",
        "/api/tracker/trackedEntities/vOxUH373fy5/attributes/nDlikr3TUS6/image",
        "/api/41/tracker/trackedEntities/vOxUH373fy5/attributes/nDlikr3TUS6/image"
      })
  void shouldNotAddEtagHeader(String uri) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(new MockHttpServletRequest("GET", uri), response, HELLO_WORLD);

    assertNull(response.getHeader(HttpHeaders.ETAG));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/dataElementGroups.xml?links=false&paging=false",
        "/api/tracker/events/V9bOpWXppIo",
        "/api/41/tracker/events/V9bOpWXppIo"
      })
  void shouldAddEtagHeader(String uri) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(new MockHttpServletRequest("GET", uri), response, HELLO_WORLD);

    assertEquals(HELLO_WORLD_ETAG, response.getHeader(HttpHeaders.ETAG));
  }

  @Test
  void matchingBodyHashGets304WithoutBody() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/info");
    request.addHeader(HttpHeaders.IF_NONE_MATCH, HELLO_WORLD_ETAG);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, HELLO_WORLD);

    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    assertEquals("", response.getContentAsString());
  }

  @Test
  void smartETagSetBeforeTheBodyIsKeptAndTheBodyStreamed() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain smart =
        (filterRequest, filterResponse) -> {
          HttpServletResponse http = (HttpServletResponse) filterResponse;
          http.setStatus(HttpServletResponse.SC_OK);
          http.setHeader(HttpHeaders.ETAG, "\"smart\"");
          FileCopyUtils.copy("Hello World".getBytes(), http.getOutputStream());
        };

    filter.doFilter(new MockHttpServletRequest("GET", "/api/me"), response, smart);

    assertEquals("\"smart\"", response.getHeader(HttpHeaders.ETAG));
    assertEquals("Hello World", response.getContentAsString());
  }

  @Test
  void noStoreResponseGetsNoEtag() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain noStore =
        (filterRequest, filterResponse) -> {
          HttpServletResponse http = (HttpServletResponse) filterResponse;
          http.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
          HELLO_WORLD.doFilter(filterRequest, filterResponse);
        };

    filter.doFilter(new MockHttpServletRequest("GET", "/api/system/info"), response, noStore);

    assertNull(response.getHeader(HttpHeaders.ETAG));
  }

  @Test
  void postGetsNoEtag() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(new MockHttpServletRequest("POST", "/api/dataElements"), response, HELLO_WORLD);

    assertNull(response.getHeader(HttpHeaders.ETAG));
  }

  /**
   * Pins master's exclusion semantics: each pattern is found anywhere in the URI (no anchors), the
   * API version prefix is exactly two digits, and UIDs must start with a letter. Expected values
   * were taken from master's single combined regex.
   */
  @ParameterizedTest
  @CsvSource({
    "/api/dataValueSets, true",
    "/api/dataValueSets.json, true",
    "/api/41/dataValues, true",
    "/api/4/dataValues, false",
    "/api/dataEntry/dataValues, false",
    "/api/fileResources/abcdefghijk/data, true",
    "/api/dataEntry/metadata, true",
    "/api/metadata/version/Version_1/data, true",
    "/api/metadata/version/Version_1/data.gz, true",
    "/api/metadata/version/Version_1, false",
    "/api/metadata/version/data, false",
    "/api/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/file, true",
    "/api/tracker/events/1kV9CZzmV2E/dataValues/q33Wv8jNvFA/file, false",
    "/api/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/video, false",
    "/api/tracker/trackedEntities/vOxUH373fy5/attributes/nDlikr3TUS6/image, true",
    "/api/dataElements, false",
    "/api/tracker/events/V9bOpWXppIo, false",
    "/dhis/api/dataValues, true",
    "/api/dataElementGroups.xml, false"
  })
  void exclusionsMatchMastersPatterns(String uri, boolean excluded) {
    assertEquals(excluded, ETagFilter.isExcluded(uri));
  }
}
