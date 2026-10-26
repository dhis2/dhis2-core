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
package org.hisp.dhis.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CorsAllowlistEntryTest {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          https://example.org                | https://example.org                | false
          https://example.org/               | https://example.org                | false
          '  https://example.org  '          | https://example.org                | false
          HTTPS://Example.ORG                | https://example.org                | false
          http://localhost:3000              | http://localhost:3000              | false
          http://localhost:3000/             | http://localhost:3000              | false
          https://example.org:443            | https://example.org                | false
          http://example.org:80              | http://example.org                 | false
          http://example.org:443             | http://example.org:443             | false
          https://example.org:08443          | https://example.org:8443           | false
          http://127.0.0.1:8080              | http://127.0.0.1:8080              | false
          http://[::1]:3000                  | http://[::1]:3000                  | false
          http://[2001:DB8::1]               | http://[2001:db8::1]               | false
          https://bücher.example             | https://xn--bcher-kva.example      | false
          https://my_host.internal           | https://my_host.internal           | false
          https://*.example.org              | https://*.example.org              | true
          HTTPS://*.Example.org:8443/        | https://*.example.org:8443         | true
          https://*.local.tld                | https://*.local.tld                | true
          """)
  void shouldAcceptAndNormaliseValidEntry(String entry, String origin, boolean wildcard) {
    CorsAllowlistEntry parsed = CorsAllowlistEntry.parse(entry);

    assertEquals(origin, parsed.getOrigin());
    assertEquals(wildcard, parsed.isSubdomainWildcard());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        " ",
        "null",
        "NULL",
        "*",
        "*.*",
        "*://*",
        "http*://*",
        "https://*",
        "https://*.*",
        "https://*.org",
        "https://*.co.uk",
        "https://*.github.io",
        "https://*.10.0.0",
        "https://*.*.example.org",
        "https://**.example.org",
        "https://ex*ample.org",
        "https://example.*",
        "https://sub.*.example.org",
        "https://example.org:*",
        "*.example.org",
        "example.org",
        "//example.org",
        "ftp://example.org",
        "file:///etc/passwd",
        "https://",
        "https://:8080",
        "https://example.org/path",
        "https://example.org//",
        "https://example.org?x=1",
        "https://example.org#fragment",
        "https://user@example.org",
        "https://example.org:0",
        "https://example.org:65536",
        "https://example.org:abc",
        "https://example..org",
        "https://example.org.",
        "https://-example.org",
        "https://exa mple.org",
        "https://example.org|.*",
        "https://[::1",
        "https://[zz::1]",
        "https://[::1]x"
      })
  void shouldRejectInvalidEntry(String entry) {
    assertThrows(IllegalArgumentException.class, () -> CorsAllowlistEntry.parse(entry));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          https://example.org        | https://example.org              | true
          https://example.org        | https://example.org.evil.com     | false
          https://example.org        | https://evil.com/https://example.org | false
          https://example.org        | https://sub.example.org          | false
          https://example.org        | https://evilexample.org          | false
          https://example.org        | http://example.org               | false
          https://example.org        | https://example.org:8443         | false
          https://example.org:443    | https://example.org              | true
          http://localhost:3000/     | http://localhost:3000            | true
          https://*.example.org      | https://app.example.org          | true
          https://*.example.org      | https://a.b.example.org          | true
          https://*.example.org      | https://example.org              | false
          https://*.example.org      | https://evilexample.org          | false
          https://*.example.org      | https://app.example.org.evil.com | false
          https://*.example.org      | http://app.example.org           | false
          https://*.example.org      | https://app.example.org:8443     | false
          https://*.example.org:8443 | https://app.example.org:8443     | true
          """)
  void shouldMatchOnlyTheOriginsTheEntryDescribes(String entry, String origin, boolean expected) {
    assertEquals(expected, CorsAllowlistEntry.parse(entry).matches(origin));
  }

  @ParameterizedTest
  @ValueSource(strings = {"https://example.org", "https://*.example.org"})
  void shouldNotMatchMissingOrigin(String entry) {
    assertFalse(CorsAllowlistEntry.parse(entry).matches(null));
  }
}
