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
package org.hisp.dhis.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ServerBaseUrlValidatorTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://dhis2.example.org",
        "https://dhis2.example.org/dhis",
        "http://dhis2.example.org:8080/dhis",
        "https://dhis2.moh.local",
        "https://dhis.ministry.internal/dhis",
        "http://dhis.lan",
        "http://localhost:8080",
        "http://dhis-server:8080/dhis",
        "http://dhis2_core:8080",
        "http://10.0.0.5:8080/dhis",
        "http://[::1]:8080/dhis",
        "HTTPS://DHIS2.EXAMPLE.ORG"
      })
  void testValidUrl(String baseUrl) {
    assertTrue(ServerBaseUrlValidator.validate(baseUrl).isEmpty());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  void testMissingUrl(String baseUrl) {
    assertEquals(
        Optional.of("'server.base.url' is not set in dhis.conf."),
        ServerBaseUrlValidator.validate(baseUrl));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not-a-url",
        "dhis2.example.org/dhis",
        "ftp://dhis2.example.org",
        "https://",
        "https:///dhis",
        "https://dhis2 example.org"
      })
  void testInvalidUrl(String baseUrl) {
    assertEquals(
        Optional.of("'server.base.url' is not a valid URL: '" + baseUrl + "'."),
        ServerBaseUrlValidator.validate(baseUrl));
  }

  @Test
  void testTrailingSlash() {
    assertEquals(
        Optional.of(
            "'server.base.url' must not end with a trailing slash: 'https://dhis2.example.org/dhis/'."),
        ServerBaseUrlValidator.validate("https://dhis2.example.org/dhis/"));
  }
}
