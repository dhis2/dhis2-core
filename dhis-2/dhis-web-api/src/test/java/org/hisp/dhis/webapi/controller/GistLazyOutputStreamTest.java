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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Gist writes its body itself. A response that carries the smart API ETag must stay cacheable,
 * every other Gist response stays {@code no-store}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class GistLazyOutputStreamTest {

  @Test
  void responseWithSmartETagKeepsItsCacheControl() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setHeader(HttpHeaders.ETAG, "\"smart\"");
    response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=0, private");

    AbstractGistReadOnlyController.lazyOutputStream("application/json", response).get();

    assertEquals("max-age=0, private", response.getHeader(HttpHeaders.CACHE_CONTROL));
    assertEquals("application/json", response.getContentType());
  }

  @Test
  void responseWithoutETagIsNoStore() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    AbstractGistReadOnlyController.lazyOutputStream("application/json", response).get();

    assertEquals(
        "no-cache, no-store, max-age=0, must-revalidate",
        response.getHeader(HttpHeaders.CACHE_CONTROL));
  }
}
