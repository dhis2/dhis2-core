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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.hisp.dhis.dataintegrity.DataIntegritySeverity;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.test.config.TestDhisConfigurationProvider;
import org.hisp.dhis.test.webapi.json.domain.JsonDataIntegrityCheck;
import org.hisp.dhis.test.webapi.json.domain.JsonDataIntegrityDetails;
import org.hisp.dhis.test.webapi.json.domain.JsonDataIntegrityDetails.JsonDataIntegrityIssue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the programmatic {@code server_base_url_not_configured} data integrity check, which reports
 * a missing or invalid {@code server.base.url} in {@code dhis.conf}.
 */
class DataIntegrityServerBaseUrlControllerTest extends AbstractDataIntegrityIntegrationTest {

  private static final String CHECK = "server_base_url_not_configured";

  private static final String KEY = ConfigurationKey.SERVER_BASE_URL.getKey();

  @Autowired private DhisConfigurationProvider config;

  private String originalBaseUrl;

  @BeforeEach
  void saveBaseUrl() {
    originalBaseUrl = properties().getProperty(KEY);
  }

  @AfterEach
  void restoreBaseUrl() {
    if (originalBaseUrl == null) {
      properties().remove(KEY);
    } else {
      properties().setProperty(KEY, originalBaseUrl);
    }
  }

  @Test
  void testCheckMetadata() {
    JsonList<JsonDataIntegrityCheck> checks =
        GET("/dataIntegrity?checks=" + CHECK).content().asList(JsonDataIntegrityCheck.class);
    assertEquals(1, checks.size());
    JsonDataIntegrityCheck check = checks.get(0);
    assertEquals(CHECK, check.getName());
    assertEquals("SBUNC", check.getCode());
    assertEquals("Server base URL is missing or invalid", check.getDisplayName());
    assertEquals("System configuration", check.getSection());
    assertEquals(DataIntegritySeverity.SEVERE, check.getSeverity());
    assertTrue(check.getIsProgrammatic());
    assertFalse(check.getIsSlow(), "the check is cheap and should run by default");
  }

  @Test
  void testBaseUrlNotSet() {
    properties().remove(KEY);
    assertSingleIssue("'server.base.url' is not set in dhis.conf.");
  }

  @Test
  void testBaseUrlInvalid() {
    properties().setProperty(KEY, "not-a-url");
    assertSingleIssue("'server.base.url' is not a valid URL: 'not-a-url'.");
  }

  @Test
  void testBaseUrlTrailingSlash() {
    properties().setProperty(KEY, "https://dhis2.example.org/dhis/");
    assertSingleIssue(
        "'server.base.url' must not end with a trailing slash: 'https://dhis2.example.org/dhis/'.");
  }

  @Test
  void testBaseUrlValid() {
    properties().setProperty(KEY, "https://dhis2.example.org/dhis");
    checkDataIntegritySummary(CHECK, 0, 0, false);

    postDetails(CHECK);
    assertTrue(getDetails(CHECK).getIssues().isEmpty());
  }

  @Test
  void testCheckRunsWhenNoChecksAreNamed() {
    // seed the cached result with a clean run so a stale result can't make this pass
    properties().setProperty(KEY, "https://dhis2.example.org/dhis");
    checkDataIntegritySummary(CHECK, 0, 0, false);

    properties().remove(KEY);
    assertStatus(HttpStatus.OK, POST("/dataIntegrity/summary"));
    assertEquals(1, getSummary(CHECK).getCount());
  }

  private void assertSingleIssue(String expectedComment) {
    checkDataIntegritySummary(CHECK, 1, 0, false);

    postDetails(CHECK);
    JsonDataIntegrityDetails details = getDetails(CHECK);
    JsonList<JsonDataIntegrityIssue> issues = details.getIssues();
    assertEquals(1, issues.size());
    JsonDataIntegrityIssue issue = issues.get(0);
    assertEquals("server.base.url", issue.getName());
    assertEquals(expectedComment, issue.getComment().string());
  }

  private Properties properties() {
    return ((TestDhisConfigurationProvider) config).getProperties();
  }
}
