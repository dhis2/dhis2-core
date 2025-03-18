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
package org.hisp.dhis.webapi.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;

class ExcludableShallowEtagHeaderFilterTest {

  private static ExcludableShallowEtagHeaderFilter filter;

  @BeforeAll
  static void setup() {
    filter = spy(ExcludableShallowEtagHeaderFilter.class);

    FilterConfig filterConfig = mock(FilterConfig.class);
    when(filterConfig.getInitParameter(
            ExcludableShallowEtagHeaderFilter.EXCLUDE_URI_REGEX_VAR_NAME))
        .thenReturn(ExcludableShallowEtagHeaderFilter.ENDPOINTS);
    when(filter.getFilterConfig()).thenReturn(filterConfig);
    filter.initFilterBean();
  }

  @Test
  void shouldHaveAsyncSupportEnabled() {
    assertTrue(
        ExcludableShallowEtagHeaderFilter.class.getAnnotation(WebFilter.class).asyncSupported());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/dataValues",
        "/api/41/dataValues",
        "/api/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/file",
        "/api/41/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/file",
        "/api/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/image",
        "/api/41/tracker/events/RkV9CZzmV2E/dataValues/q33Wv8jNvFA/image",
        "/api/tracker/trackedEntities/vOxUH373fy5/attributes/nDlikr3TUS6/file",
        "/api/41/tracker/trackedEntities/vOxUH373fy5/attributes/nDlikr3TUS6/file",
        "/api/tracker/trackedEntities/vOxUH373fy5/attributes/nDlikr3TUS6/image",
        "/api/41/tracker/trackedEntities/vOxUH373fy5/attributes/nDlikr3TUS6/image"
      })
  void shouldNotAddEtagHeader(String URI) throws Exception {
    final MockHttpServletRequest request = new MockHttpServletRequest("GET", URI);
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain =
        (filterRequest, filterResponse) -> {
          ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
          filterResponse.setContentType(MediaType.TEXT_PLAIN_VALUE);
          FileCopyUtils.copy("Hello World".getBytes(), filterResponse.getOutputStream());
        };

    filter.doFilter(request, response, filterChain);

    assertNull(response.getHeader("Etag"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/dataElementGroups.xml?links=false&paging=false",
        "/api/tracker/events/V9bOpWXppIo",
        "/api/41/tracker/events/V9bOpWXppIo"
      })
  void shouldAddEtagHeader(String URI) throws Exception {
    final MockHttpServletRequest request = new MockHttpServletRequest("GET", URI);
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain =
        (filterRequest, filterResponse) -> {
          ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
          filterResponse.setContentType(MediaType.TEXT_PLAIN_VALUE);
          FileCopyUtils.copy("Hello World".getBytes(), filterResponse.getOutputStream());
        };

    filter.doFilter(request, response, filterChain);

    assertEquals("\"0b10a8db164e0754105b7a99be72e3fe5\"", response.getHeader("Etag"));
  }
}
