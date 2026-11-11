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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.test.config.TestDhisConfigurationProvider;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.webapi.security.csp.CspAppOverrideControllerTest.DhisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end tests of per-app CSP overrides: the {@code /api/security/csp} API and the headers
 * {@code AppController.renderApp} emits for app entrypoints.
 *
 * @author Morten Svanæs
 */
@ContextConfiguration(classes = {DhisConfig.class})
@Transactional
class CspAppOverrideControllerTest extends H2ControllerIntegrationTestBase {
  static class DhisConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      return new TestDhisConfigurationProvider("appControllerBaseTestDhis.conf");
    }
  }

  static {
    try {
      ClassPathResource conf = new ClassPathResource("appControllerBaseTestDhis.conf");
      Path tempDir = createTempDirectory("cspAppFiles").toAbsolutePath();
      try (InputStream in = conf.getInputStream()) {
        Files.copy(in, tempDir.resolve("dhis.conf"));
      }
      System.setProperty("dhis2.home", tempDir.toString());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final String CSP = "Content-Security-Policy";
  private static final String CSP_RO = "Content-Security-Policy-Report-Only";

  @Autowired private AppManager appManager;
  @Autowired private DhisConfigurationProvider config;

  @BeforeEach
  void installApps() throws IOException {
    if (appManager.getApp("maps") == null) {
      appManager.installApp(app("maps", true));
    }
    if (appManager.getApp("other-app") == null) {
      appManager.installApp(app("other-app", true));
    }
    DELETE("/security/csp/apps/maps");
    DELETE("/security/csp/apps/other-app");
  }

  private static File app(String key, boolean plugin) throws IOException {
    File zip = Files.createTempFile(key, ".zip").toFile();
    String manifest =
        """
        {"appType":"APP","short_name":"%s","name":"%s","version":"1.0.0",
         "launch_path":"index.html"%s}
        """
            .formatted(key, key, plugin ? ",\"plugin_launch_path\":\"plugin.html\"" : "");
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
      for (Map.Entry<String, String> e :
          Map.of(
                  "manifest.webapp", manifest,
                  "index.html", "<!doctype html><html><head></head><body>app</body></html>",
                  "plugin.html", "<!doctype html><html><head></head><body>plugin</body></html>",
                  "service-worker.js", "self.addEventListener('fetch', () => {})",
                  "main.js", "console.log(1)")
              .entrySet()) {
        out.putNextEntry(new ZipEntry(e.getKey()));
        out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
      }
    }
    return zip;
  }

  private String header(String path, String name) {
    HttpResponse response = GET(path);
    assertEquals(HttpStatus.OK, response.status(), path);
    return response.header(name);
  }

  private static List<String> sources(String policy, String directive) {
    for (String part : policy.split(";")) {
      String trimmed = part.trim();
      if (trimmed.startsWith(directive + " ")) {
        return List.of(trimmed.substring(directive.length()).trim().split(" "));
      }
    }
    return List.of();
  }

  // (a) + (b): Maps hosts only for maps app/plugin entrypoints
  @Test
  void mapsHosts_onlyOnMapsAppAndPluginEntrypoints() {
    String mapsApp = header("/apps/maps/index.html", CSP);
    String mapsPlugin = header("/apps/maps/plugin.html", CSP);
    String mapsWorker = header("/apps/maps/service-worker.js", CSP);
    String other = header("/apps/other-app/index.html", CSP);
    String otherPlugin = header("/apps/other-app/plugin.html", CSP);

    for (String policy : List.of(mapsApp, mapsPlugin)) {
      assertTrue(sources(policy, "img-src").contains("https://tiles.openfreemap.org"), policy);
      assertTrue(sources(policy, "connect-src").contains("https://tile.openstreetmap.org"), policy);
    }
    for (String policy : List.of(mapsWorker, other, otherPlugin)) {
      assertFalse(policy.contains("openfreemap"), policy);
      assertFalse(policy.contains("openstreetmap"), policy);
      assertEquals(List.of("'self'"), sources(policy, "connect-src"), policy);
    }
    assertTrue(mapsApp.contains("report-uri /api/security/csp/report?app=maps&ep=app;"), mapsApp);
    assertTrue(mapsPlugin.contains("ep=plugin"), mapsPlugin);
    assertTrue(mapsWorker.contains("ep=worker"), mapsWorker);
    // static assets get the plain app-host baseline without a report-uri
    assertFalse(header("/apps/maps/main.js", CSP).contains("report-uri"));
  }

  @Test
  void cspMapSourcesConfigurationKey_isGone() {
    assertTrue(ConfigurationKey.getByKey("csp.map.sources").isEmpty());
  }

  // (b): detail shows base policy, suggestions and effective headers
  @Test
  void mapsDetail_listsBaseOverridesSuggestionsAndEffectiveHeaders() {
    POST(
            "/externalMapLayers",
            """
            {"name":"WMS test","mapService":"WMS","url":"https://wms.example.org/geoserver/wms?x=1",
             "layers":"a","imageFormat":"PNG","mapLayerPosition":"BASEMAP"}
            """)
        .content(HttpStatus.CREATED);

    JsonObject detail = GET("/security/csp/apps/maps").content(HttpStatus.OK);

    assertTrue(
        detail
            .getObject("baseOverrides")
            .getObject("app")
            .getArray("img-src")
            .stringValues()
            .contains("https://tiles.maps.eox.at"));
    List<String> suggested =
        detail.getList("suggestions", JsonObject.class).stream()
            .map(s -> s.getString("directive").string() + " " + s.getString("source").string())
            .toList();
    assertTrue(suggested.contains("img-src https://wms.example.org"), suggested.toString());
    assertTrue(suggested.contains("connect-src https://wms.example.org"), suggested.toString());
    String effective =
        detail
            .getObject("effective")
            .getObject("plugin")
            .getString("contentSecurityPolicy")
            .string();
    assertTrue(effective.contains("https://tiles.openfreemap.org"), effective);
    assertEquals(
        "ENFORCE",
        detail.getObject("effective").getObject("app").getString("enforcementMode").string());
  }

  // (c): a saved override changes the header on the next request; unsafe needs confirmation
  @Test
  void savingAnOverride_changesTheNextResponse_andRecordsUnsafeTokens() {
    String body =
        """
        {"approvalNote":"OPS-1","entrypoints":{
          "*":{"enforced":{"add":{"img-src":["https://wms.example.org"]}}},
          "app":{"enforced":{"add":{"script-src":["\\u0027unsafe-eval\\u0027"]}}}}}
        """;
    JsonObject gate = PUT("/security/csp/apps/maps", body).content(HttpStatus.CONFLICT);
    assertEquals(
        "CONFIRM_UNSAFE_REQUIRED",
        gate.getList("errors", JsonObject.class).get(0).getString("code").string());
    assertFalse(header("/apps/maps/index.html", CSP).contains("'unsafe-eval'"));

    JsonObject saved =
        PUT(
                "/security/csp/apps/maps",
                body.replace("\"approvalNote\"", "\"confirmUnsafe\":true,\"approvalNote\""))
            .content(HttpStatus.OK);
    assertTrue(saved.getBoolean("hasUnsafeDirectives").booleanValue());
    assertEquals(
        List.of("app script-src 'unsafe-eval'"), saved.getArray("unsafeTokens").stringValues());
    assertEquals("admin", saved.getString("updatedBy").string());

    String app = header("/apps/maps/index.html", CSP);
    assertEquals(List.of("'self'", "'unsafe-eval'"), sources(app, "script-src"), app);
    assertTrue(sources(app, "img-src").contains("https://wms.example.org"), app);
    String plugin = header("/apps/maps/plugin.html", CSP);
    assertTrue(sources(plugin, "img-src").contains("https://wms.example.org"), plugin);
    assertTrue(sources(plugin, "script-src").isEmpty(), plugin);
    assertFalse(header("/apps/other-app/index.html", CSP).contains("wms.example.org"));

    JsonObject summary =
        GET("/security/csp/apps").content(HttpStatus.OK).getList("apps", JsonObject.class).stream()
            .filter(a -> a.getString("appKey").string().equals("maps"))
            .findFirst()
            .orElseThrow();
    assertTrue(summary.getBoolean("hasUnsafeDirectives").booleanValue());

    assertEquals(HttpStatus.NO_CONTENT, DELETE("/security/csp/apps/maps").status());
    assertFalse(header("/apps/maps/index.html", CSP).contains("wms.example.org"));
  }

  // (d): REPORT_ONLY_EXEMPT for one entrypoint only
  @Test
  void reportOnlyExempt_needsConfirmation_andOnlyAffectsThatEntrypoint() {
    String body =
        """
        {"approvalNote":"Vendor app cannot run under CSP, OPS-2",
         "entrypoints":{"plugin":{"enforcementMode":"REPORT_ONLY_EXEMPT"}}}
        """;
    JsonObject gate = PUT("/security/csp/apps/other-app", body).content(HttpStatus.CONFLICT);
    assertEquals(
        "CONFIRM_EXEMPT_REQUIRED",
        gate.getList("errors", JsonObject.class).get(0).getString("code").string());

    JsonObject saved =
        PUT(
                "/security/csp/apps/other-app",
                body.replace("{\"approvalNote\"", "{\"confirmExempt\":true,\"approvalNote\""))
            .content(HttpStatus.OK);
    assertTrue(saved.getBoolean("hasExemptEntrypoint").booleanValue());

    HttpResponse plugin = GET("/apps/other-app/plugin.html");
    assertTrue(plugin.header(CSP).startsWith("frame-ancestors 'self'"), plugin.header(CSP));
    assertFalse(plugin.header(CSP).contains("default-src"));
    assertTrue(plugin.header(CSP_RO).startsWith("default-src 'self';"), plugin.header(CSP_RO));
    assertTrue(plugin.header(CSP_RO).contains("report-uri"), plugin.header(CSP_RO));
    assertEquals("nosniff", plugin.header("X-Content-Type-Options"));

    HttpResponse app = GET("/apps/other-app/index.html");
    assertTrue(app.header(CSP).startsWith("default-src 'self';"), app.header(CSP));
    assertNull(app.header(CSP_RO));
  }

  @Test
  void reportOnlyExempt_withoutApprovalNote_isRejected() {
    JsonObject result =
        PUT(
                "/security/csp/apps/other-app",
                """
                {"confirmExempt":true,"entrypoints":{"app":{"enforcementMode":"REPORT_ONLY_EXEMPT"}}}
                """)
            .content(HttpStatus.BAD_REQUEST);
    assertEquals(
        "APPROVAL_NOTE_REQUIRED",
        result.getList("errors", JsonObject.class).get(0).getString("code").string());
  }

  // (e): hard locks
  @Test
  void hardLocks_cannotBeWeakened() {
    for (String directive :
        List.of("default-src", "object-src", "base-uri", "frame-ancestors", "report-uri")) {
      JsonObject result =
          PUT(
                  "/security/csp/apps/maps",
                  """
                  {"confirmUnsafe":true,"entrypoints":{"*":{"enforced":{"add":{"%s":["*"]}}}}}
                  """
                      .formatted(directive))
              .content(HttpStatus.BAD_REQUEST);
      assertEquals(
          "LOCKED_DIRECTIVE",
          result.getList("errors", JsonObject.class).get(0).getString("code").string());
    }
    JsonObject injection =
        PUT(
                "/security/csp/apps/maps",
                """
                {"entrypoints":{"*":{"enforced":{"add":{"img-src":["https://x.org; script-src *"]}}}}}
                """)
            .content(HttpStatus.BAD_REQUEST);
    assertEquals(
        "INVALID_CHARACTERS",
        injection.getList("errors", JsonObject.class).get(0).getString("code").string());

    String policy = header("/apps/maps/index.html", CSP);
    assertTrue(policy.startsWith("default-src 'self';"), policy);
    assertTrue(policy.contains("object-src 'none';"), policy);
    assertTrue(policy.contains("base-uri 'self';"), policy);
  }

  // (f): authority
  @Test
  void writes_needFSystemSetting_readsAlsoAppManagement() {
    switchToNewUser("csp-reader", Authorities.M_DHIS_WEB_APP_MANAGEMENT.toString());
    assertEquals(HttpStatus.OK, GET("/security/csp/apps").status());
    assertEquals(HttpStatus.OK, GET("/security/csp/apps/maps").status());
    assertEquals(
        HttpStatus.FORBIDDEN, PUT("/security/csp/apps/maps", "{\"entrypoints\":{}}").status());
    assertEquals(HttpStatus.FORBIDDEN, DELETE("/security/csp/apps/maps").status());

    switchToNewUser("csp-nobody");
    assertEquals(HttpStatus.FORBIDDEN, GET("/security/csp/apps").status());

    switchToNewUser("csp-writer", Authorities.F_SYSTEM_SETTING.name());
    assertEquals(
        HttpStatus.OK,
        PUT(
                "/security/csp/apps/maps",
                "{\"entrypoints\":{\"*\":{\"enforced\":{\"add\":{\"img-src\":[\"https://a.example.org\"]}}}}}")
            .status());
  }

  @Test
  void datastoreNamespace_isProtected() {
    PUT(
            "/security/csp/apps/maps",
            "{\"entrypoints\":{\"*\":{\"enforced\":{\"add\":{\"img-src\":[\"https://a.example.org\"]}}}}}")
        .content(HttpStatus.OK);

    switchToNewUser("csp-ds-reader", Authorities.M_DHIS_WEB_APP_MANAGEMENT.toString());
    assertEquals(HttpStatus.OK, GET("/dataStore/csp-app-overrides/maps").status());
    assertEquals(
        HttpStatus.FORBIDDEN,
        PUT("/dataStore/csp-app-overrides/maps", "{\"schemaVersion\":1}").status());

    switchToNewUser("csp-ds-nobody");
    assertEquals(HttpStatus.FORBIDDEN, GET("/dataStore/csp-app-overrides/maps").status());
  }

  @Test
  void validate_isADryRunWithHeaderPreview() {
    JsonObject result =
        POST(
                "/security/csp/apps/maps/validate",
                "{\"entrypoints\":{\"plugin\":{\"enforced\":{\"add\":{\"frame-src\":[\"https://embed.example.org\"]}}}}}")
            .content(HttpStatus.OK);

    assertTrue(result.getBoolean("valid").booleanValue());
    JsonString preview =
        result.getObject("effective").getObject("plugin").getString("contentSecurityPolicy");
    assertTrue(preview.string().contains("frame-src 'self' blob: https://embed.example.org;"));
    assertFalse(header("/apps/maps/plugin.html", CSP).contains("embed.example.org"));
  }

  @Test
  void unknownApp_is404() {
    assertEquals(HttpStatus.NOT_FOUND, GET("/security/csp/apps/no-such-app").status());
    assertEquals(
        HttpStatus.NOT_FOUND,
        PUT("/security/csp/apps/no-such-app", "{\"entrypoints\":{}}").status());
  }

  @Test
  void reportingOff_removesReportUri() {
    Object original = config.getProperties().setProperty("csp.report.enabled", "off");
    try {
      assertFalse(header("/apps/maps/index.html", CSP).contains("report-uri"));
    } finally {
      if (original == null) {
        config.getProperties().remove("csp.report.enabled");
      } else {
        config.getProperties().setProperty("csp.report.enabled", original.toString());
      }
    }
  }
}
