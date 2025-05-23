/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dataexchange.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class AuthenticationTest {
  @Test
  void testBasicAuthHeaderValue() {
    Authentication auth = new BasicAuthentication("admin", "district");

    HttpHeaders headers = new HttpHeaders();

    auth.withAuthentication(headers);

    assertEquals("Basic YWRtaW46ZGlzdHJpY3Q=", headers.getFirst(HttpHeaders.AUTHORIZATION));
  }

  @Test
  void testAccessTokenHeaderValue() {
    Authentication auth = new AccessTokenAuthentication("d2pat_5xVA12xyUbWNedQxy4ohH77WlxR");

    HttpHeaders headers = new HttpHeaders();

    auth.withAuthentication(headers);

    assertEquals(
        "ApiToken d2pat_5xVA12xyUbWNedQxy4ohH77WlxR", headers.getFirst(HttpHeaders.AUTHORIZATION));
  }

  @Test
  void testCookieValue() {
    Authentication auth = new CookieAuthentication("HKIJ7KJHB3JHG2KJ8PRE7T");

    HttpHeaders headers = new HttpHeaders();

    auth.withAuthentication(headers);

    assertEquals("JSESSIONID=HKIJ7KJHB3JHG2KJ8PRE7T", headers.getFirst(HttpHeaders.COOKIE));
  }
}
