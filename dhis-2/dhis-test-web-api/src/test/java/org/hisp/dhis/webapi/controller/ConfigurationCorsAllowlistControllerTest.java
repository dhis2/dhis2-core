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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonError;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the CORS allowlist endpoints of {@link ConfigurationController}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class ConfigurationCorsAllowlistControllerTest extends H2ControllerIntegrationTestBase {

  @ParameterizedTest
  @ValueSource(strings = {"/configuration/corsAllowlist", "/configuration/corsWhitelist"})
  void shouldStoreAndReturnValidAllowlistInNormalisedForm(String path) {
    assertStatus(
        HttpStatus.NO_CONTENT,
        POST(
            path,
            "['https://App.Example.org/', 'https://*.example.org', 'http://localhost:3000']"));

    assertEquals(
        List.of("http://localhost:3000", "https://*.example.org", "https://app.example.org"),
        GET(path).content(HttpStatus.OK).stringValues().stream().sorted().toList());
  }

  @ParameterizedTest
  @ValueSource(strings = {"*", "https://*", "*://*", "null", "https://example.org/path"})
  void shouldRejectInvalidEntryAndKeepStoredAllowlist(String invalid) {
    assertStatus(
        HttpStatus.NO_CONTENT,
        POST("/configuration/corsAllowlist", "['https://kept.example.org']"));

    JsonError error =
        POST("/configuration/corsAllowlist", "['https://new.example.org', '%s']".formatted(invalid))
            .error(HttpStatus.CONFLICT);

    assertEquals(ErrorCode.E1136, error.getErrorCode());
    assertTrue(error.getMessage().contains("`" + invalid + "`"), error.getMessage());
    assertEquals(
        List.of("https://kept.example.org"),
        GET("/configuration/corsAllowlist").content(HttpStatus.OK).stringValues());
  }
}
