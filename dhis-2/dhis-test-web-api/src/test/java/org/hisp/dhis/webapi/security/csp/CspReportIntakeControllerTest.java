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
package org.hisp.dhis.webapi.security.csp;

import static java.nio.file.Files.createTempDirectory;
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.http.HttpMethod;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.config.TestDhisConfigurationProvider;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.webapi.security.csp.CspReportIntakeControllerTest.DhisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the CSP violation report intake and the report views.
 *
 * @author Morten Svanæs
 */
@ContextConfiguration(classes = {DhisConfig.class})
@Transactional
class CspReportIntakeControllerTest extends H2ControllerIntegrationTestBase {
  static class DhisConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      return new TestDhisConfigurationProvider("appControllerBaseTestDhis.conf");
    }
  }

  static {
    try {
      ClassPathResource conf = new ClassPathResource("appControllerBaseTestDhis.conf");
      Path tempDir = createTempDirectory("cspReportFiles").toAbsolutePath();
      try (InputStream in = conf.getInputStream()) {
        Files.copy(in, tempDir.resolve("dhis.conf"));
      }
      System.setProperty("dhis2.home", tempDir.toString());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Autowired private AppManager appManager;

  private static final String WMS_REPORT =
      """
      {"csp-report":{"document-uri":"http://localhost/api/apps/maps/index.html?redirect=false",
       "violated-directive":"img-src","effective-directive":"img-src","disposition":"enforce",
       "blocked-uri":"https://wms.example.org/geoserver/wms?SERVICE=WMS&token=secret",
       "line-number":4,"column-number":10595,
       "source-file":"http://localhost/api/apps/maps/assets/maps-gl.js?v=2","status-code":200}}
      """;

  @BeforeEach
  void setUp() throws IOException {
    if (appManager.getApp("maps") == null) {
      File zip = Files.createTempFile("maps", ".zip").toFile();
      try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
        for (Map.Entry<String, String> e :
            Map.of(
                    "manifest.webapp",
                    "{\"appType\":\"APP\",\"short_name\":\"maps\",\"name\":\"maps\",\"version\":\"1.0.0\",\"launch_path\":\"index.html\",\"plugin_launch_path\":\"plugin.html\"}",
                    "index.html",
                    "<!doctype html><html><head></head><body></body></html>",
                    "plugin.html",
                    "<!doctype html><html><head></head><body></body></html>")
                .entrySet()) {
          out.putNextEntry(new ZipEntry(e.getKey()));
          out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
          out.closeEntry();
        }
      }
      appManager.installApp(zip);
    }
    DELETE("/security/csp/apps/maps/reports");
    DELETE("/security/csp/apps/maps");
  }

  private HttpResponse report(String query, String contentType, String body) {
    return perform(
        HttpMethod.POST, "/security/csp/report" + query, ContentType(contentType), Body(body));
  }

  private List<JsonObject> mapsViolations() {
    return GET("/security/csp/apps/maps/reports")
        .content(HttpStatus.OK)
        .getList("violations", JsonObject.class)
        .stream()
        .toList();
  }

  @Test
  void reportUriPayload_isDeduplicated_suggestable_andRedacted() {
    for (int i = 0; i < 3; i++) {
      assertEquals(
          HttpStatus.NO_CONTENT,
          report("?app=maps&ep=app", "application/csp-report", WMS_REPORT).status());
    }

    List<JsonObject> violations = mapsViolations();
    assertEquals(1, violations.size());
    JsonObject v = violations.get(0);
    assertEquals(3, v.getNumber("count").intValue());
    assertTrue(v.getBoolean("suggestable").booleanValue());
    assertEquals("img-src", v.getString("suggestedDirective").string());
    assertEquals("https://wms.example.org", v.getString("suggestedSource").string());
    assertEquals("https://wms.example.org", v.getString("blocked").string());
    assertEquals(
        "http://localhost/api/apps/maps/index.html", v.getString("sampleDocumentUri").string());
    assertEquals(
        "http://localhost/api/apps/maps/assets/maps-gl.js",
        v.getString("sampleSourceFile").string());
    assertFalse(v.toJson().contains("secret"), v.toJson());

    JsonObject summary =
        GET("/security/csp/apps").content(HttpStatus.OK).getList("apps", JsonObject.class).stream()
            .filter(a -> a.getString("appKey").string().equals("maps"))
            .findFirst()
            .orElseThrow();
    assertEquals(3, summary.getObject("violations24h").getNumber("total").intValue());
    assertEquals(
        1,
        GET("/security/csp/apps/maps")
            .content(HttpStatus.OK)
            .getList("recentViolations", JsonObject.class)
            .size());
  }

  @Test
  void reportingApiPayload_isAccepted() {
    String body =
        """
        [{"type":"csp-violation","url":"http://localhost/api/apps/maps/plugin.html",
          "body":{"documentURL":"http://localhost/api/apps/maps/plugin.html","blockedURL":"eval",
          "effectiveDirective":"script-src","disposition":"report","lineNumber":1}}]
        """;
    assertEquals(
        HttpStatus.NO_CONTENT,
        report("?app=maps&ep=plugin", "application/reports+json", body).status());

    JsonObject v = mapsViolations().get(0);
    assertEquals("EVAL", v.getString("violationKind").string());
    assertEquals("report", v.getString("disposition").string());
    assertEquals("plugin", v.getString("entrypoint").string());
    assertFalse(v.getBoolean("suggestable").booleanValue());
  }

  @Test
  void oversizedBody_is413_beforeParsing() {
    String big = "{\"csp-report\":{\"x\":\"" + "a".repeat(9000) + "\"}}";
    assertEquals(
        HttpStatus.PAYLOAD_TOO_LARGE,
        report("?app=maps&ep=app", "application/csp-report", big).status());
    assertTrue(mapsViolations().isEmpty());
  }

  @Test
  void unknownAppOrEntrypoint_andExtensionNoise_areDroppedWith204() {
    assertEquals(
        HttpStatus.NO_CONTENT,
        report("?app=no-such-app&ep=app", "application/csp-report", WMS_REPORT).status());
    assertEquals(
        HttpStatus.NO_CONTENT,
        report("?app=maps&ep=index.html", "application/csp-report", WMS_REPORT).status());
    assertEquals(
        HttpStatus.NO_CONTENT,
        report(
                "?app=maps&ep=app",
                "application/csp-report",
                WMS_REPORT.replace(
                    "http://localhost/api/apps/maps/assets/maps-gl.js?v=2",
                    "chrome-extension://abc/inject.js"))
            .status());
    assertEquals(
        HttpStatus.NO_CONTENT,
        report("?app=maps&ep=app", "application/csp-report", "not json").status());
    assertTrue(mapsViolations().isEmpty());
  }

  @Test
  void intake_needsNoAuthority() {
    switchToNewUser("csp-anyone");
    assertEquals(
        HttpStatus.NO_CONTENT,
        report("?app=maps&ep=app", "application/csp-report", WMS_REPORT).status());
    switchToAdminUser();
    assertEquals(1, mapsViolations().size());
  }

  @Test
  void deleteReports_clearsTheApp() {
    report("?app=maps&ep=app", "application/csp-report", WMS_REPORT);
    assertEquals(HttpStatus.NO_CONTENT, DELETE("/security/csp/apps/maps/reports").status());
    assertTrue(mapsViolations().isEmpty());
  }

  @Test
  void addingTheSuggestedSource_marksTheViolationAllowedNow() {
    report("?app=maps&ep=app", "application/csp-report", WMS_REPORT);
    PUT(
            "/security/csp/apps/maps",
            "{\"entrypoints\":{\"app\":{\"enforced\":{\"add\":{\"img-src\":[\"https://wms.example.org\"]}}}}}")
        .content(HttpStatus.OK);

    assertEquals("ALLOWED_NOW", mapsViolations().get(0).getString("status").string());
  }

  @Test
  void reportOnlyDraft_emitsBothHeaders_andTestWindowEnforcesTheDraft() {
    PUT(
            "/security/csp/apps/maps",
            "{\"entrypoints\":{\"app\":{\"reportOnlyDraft\":{\"enabled\":true,\"add\":{\"img-src\":[\"https://wms.example.org\"]}}}}}")
        .content(HttpStatus.OK);
    HttpResponse draft = GET("/apps/maps/index.html");
    assertFalse(draft.header("Content-Security-Policy").contains("wms.example.org"));
    assertTrue(
        draft.header("Content-Security-Policy-Report-Only").contains("https://wms.example.org"));
    assertTrue(draft.header("Content-Security-Policy-Report-Only").contains("report-uri"));

    String expires = Instant.now().plusSeconds(3600).toString();
    PUT(
            "/security/csp/apps/maps",
            "{\"entrypoints\":{\"app\":{\"reportOnlyDraft\":{\"enabled\":true,\"expiresAt\":\""
                + expires
                + "\",\"add\":{\"img-src\":[\"https://wms.example.org\"]}}}}}")
        .content(HttpStatus.OK);
    HttpResponse testing = GET("/apps/maps/index.html");
    assertTrue(testing.header("Content-Security-Policy").contains("https://wms.example.org"));
  }
}
