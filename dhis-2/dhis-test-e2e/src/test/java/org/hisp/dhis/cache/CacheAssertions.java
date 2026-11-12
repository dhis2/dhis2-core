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
package org.hisp.dhis.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CacheAssertions {
  private CacheAssertions() {}

  static void assertCacheHeaders(CacheProbe.CacheResponse response) {
    assertEquals(200, response.statusCode(), "Expected 200 response");
    assertNotNull(response.etag(), "Expected ETag header");
    assertFalse(response.etag().isBlank(), "Expected non-empty ETag header");
    assertEquals("Cookie, Authorization", response.vary(), "Unexpected Vary header");
    assertNotNull(response.cacheControl(), "Expected Cache-Control header");
  }

  static void assertNotModified(CacheProbe.CacheResponse response, String expectedEtag) {
    assertEquals(304, response.statusCode(), "Expected 304 response");
    assertEquals("", response.body(), "304 responses should not contain a body");
    assertEquals(expectedEtag, response.etag(), "Expected the same ETag on 304 responses");
  }

  static void assertHeadMirrorsGet(
      CacheProbe.CacheResponse getResponse, CacheProbe.CacheResponse headResponse) {
    assertEquals(200, headResponse.statusCode(), "Expected 200 response for HEAD");
    assertEquals(getResponse.etag(), headResponse.etag(), "HEAD ETag should match GET");
    assertEquals(getResponse.vary(), headResponse.vary(), "HEAD Vary should match GET");
    assertEquals(
        getResponse.cacheControl(),
        headResponse.cacheControl(),
        "HEAD Cache-Control should match GET");
    assertEquals("", headResponse.body(), "HEAD responses should not contain a body");
  }

  /**
   * No smart (version-based) cache headers: either no ETag at all or only the body-hash fallback of
   * {@code ETagFilter}, and no smart {@code Vary}.
   */
  static void assertNoSmartCacheHeaders(CacheProbe.CacheResponse response) {
    String etag = response.etag();
    assertTrue(
        etag == null || etag.isBlank() || isBodyHashETag(etag),
        "Unexpected smart ETag header: " + etag);
    assertTrue(
        response.vary() == null || response.vary().isBlank(),
        "Unexpected Vary header: " + response.vary());
  }

  /** The body-hash fallback ETag of {@code ETagFilter}: a quoted "0" plus a hex MD5. */
  static void assertBodyHashETag(CacheProbe.CacheResponse response) {
    assertEquals(200, response.statusCode(), "Expected 200 response");
    assertTrue(isBodyHashETag(response.etag()), "Expected a body-hash ETag: " + response.etag());
  }

  private static boolean isBodyHashETag(String etag) {
    return etag != null && etag.matches("\"0[0-9a-f]{32}\"");
  }
}
