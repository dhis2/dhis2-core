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
package org.hisp.dhis.webapi.security.csp.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Payloads are trimmed copies of reports captured from Chromium 158 and Firefox 159.
 *
 * @author Morten Svanæs
 */
class CspReportParserTest {
  private static final ObjectMapper MAPPER = JacksonObjectMapperConfig.staticJsonMapper();

  private static final String CHROMIUM_FETCH =
      """
      {"csp-report":{"document-uri":"http://127.0.0.1:8080/dhis-web-maps/index.html?redirect=false",
       "referrer":"http://127.0.0.1:8080/dhis-web-dashboard/index.html?redirect=false",
       "violated-directive":"connect-src","effective-directive":"connect-src",
       "original-policy":"default-src 'self'","disposition":"enforce",
       "blocked-uri":"https://example.org/csp-probe/fetch?token=secret","line-number":2,
       "column-number":1,"source-file":"http://127.0.0.1:8080/csp-probe/probe.js?v=1#x",
       "status-code":200,"script-sample":""}}
      """;

  private static final String FIREFOX_REPORTING_API =
      """
      [{"age":4,"type":"csp-violation","url":"http://127.0.0.1:8080/dhis-web-maps/index.html",
        "user_agent":"Mozilla/5.0","body":{"documentURL":"http://127.0.0.1:8080/dhis-web-maps/index.html?redirect=false",
        "blockedURL":"inline","referrer":null,"effectiveDirective":"script-src-elem",
        "originalPolicy":"default-src 'self'","sourceFile":"http://127.0.0.1:8080/dhis-web-maps/index.html?redirect=false",
        "sample":null,"disposition":"report","statusCode":200,"lineNumber":3,"columnNumber":16}},
       {"age":4,"type":"deprecation","body":{}}]
      """;

  private static List<CspViolationService.Report> parse(String json) throws IOException {
    return CspReportParser.parse(json.getBytes(StandardCharsets.UTF_8), null, MAPPER).stream()
        .map(raw -> CspReportParser.normalize(raw, "maps", "app").report())
        .toList();
  }

  @Test
  void reportUriFormat_isReducedToFixedFields_andRedacted() throws IOException {
    CspViolationService.Report r = parse(CHROMIUM_FETCH).get(0);

    assertEquals("connect-src", r.effectiveDirective());
    assertEquals("https://example.org", r.blocked());
    assertEquals("http://127.0.0.1:8080/dhis-web-maps/index.html", r.documentUri());
    assertEquals("http://127.0.0.1:8080/csp-probe/probe.js", r.sourceFile());
    assertEquals("enforce", r.disposition());
    assertEquals(2, r.line());
  }

  @Test
  void reportingApiFormat_onlyCspViolations_withElemFolded() throws IOException {
    List<CspViolationService.Report> reports = parse(FIREFOX_REPORTING_API);

    assertEquals(1, reports.size());
    CspViolationService.Report r = reports.get(0);
    assertEquals("script-src", r.effectiveDirective());
    assertEquals("inline", r.blocked());
    assertEquals("report", r.disposition());
    assertEquals("http://127.0.0.1:8080/dhis-web-maps/index.html", r.sourceFile());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "chrome-extension://abcdefgh/content.js",
        "moz-extension://1234-5678/inject.js",
        "safari-web-extension://x/y.js",
        "about"
      })
  void browserExtensionNoise_isDropped(String uri) throws IOException {
    for (String field : List.of("blocked-uri", "source-file", "document-uri")) {
      String json =
          """
          {"csp-report":{"document-uri":"http://h/api/apps/maps/index.html","effective-directive":"script-src",
           "blocked-uri":"https://cdn.example.org/x.js","source-file":"http://h/a.js"}}
          """
              .replace(
                  "\"" + field + "\":\"" + valueOf(field) + "\"",
                  "\"" + field + "\":\"" + uri + "\"");
      CspReportParser.Raw raw =
          CspReportParser.parse(json.getBytes(StandardCharsets.UTF_8), null, MAPPER).get(0);
      CspReportParser.Outcome outcome = CspReportParser.normalize(raw, "maps", "app");
      assertNull(outcome.report(), field + "=" + uri);
      assertEquals("extension", outcome.dropReason());
    }
  }

  private static String valueOf(String field) {
    return switch (field) {
      case "blocked-uri" -> "https://cdn.example.org/x.js";
      case "source-file" -> "http://h/a.js";
      default -> "http://h/api/apps/maps/index.html";
    };
  }

  @Test
  void blockedUri_keywordsAndSchemes() {
    assertEquals("eval", CspReportParser.blocked("eval").orElseThrow());
    assertEquals("inline", CspReportParser.blocked("").orElseThrow());
    assertEquals("data", CspReportParser.blocked("data:image/png;base64,AAAA").orElseThrow());
    assertEquals("blob", CspReportParser.blocked("blob").orElseThrow());
    assertEquals(
        "https://tiles.example.org:8443",
        CspReportParser.blocked("https://user:pw@tiles.example.org:8443/a/b?c#d").orElseThrow());
  }

  @Test
  void stripUri_dropsUserinfoQueryFragmentAndNonHttp() {
    assertEquals(
        "https://h.example.org/p", CspReportParser.stripUri("https://u:p@h.example.org/p?q=1#f"));
    assertNull(CspReportParser.stripUri("javascript:alert(1)"));
    assertNull(CspReportParser.stripUri("https://h.example.org/" + "a".repeat(600)));
  }

  @Test
  void garbageDirective_isInvalid() throws IOException {
    String json =
        """
        {"csp-report":{"effective-directive":"img-src; script-src *","blocked-uri":"https://x.org"}}
        """;
    CspReportParser.Raw raw =
        CspReportParser.parse(json.getBytes(StandardCharsets.UTF_8), null, MAPPER).get(0);
    assertEquals("invalid", CspReportParser.normalize(raw, "maps", "app").dropReason());
  }
}
