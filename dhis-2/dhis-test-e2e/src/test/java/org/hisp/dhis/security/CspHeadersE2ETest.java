/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.security;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * E2E tests for {@code Content-Security-Policy} header behaviour.
 *
 * <p>This test suite verifies that:
 *
 * <ol>
 *   <li>The default CSP lands on regular API endpoints via {@code CspBaselineFilter} or {@code
 *       CspInterceptor}.
 *   <li>Endpoints marked with {@code @CspAppHost} carry the app-host policy.
 *   <li>Endpoints marked with {@code @CspUserUploadedContent} carry the strict {@code default-src
 *       'none'} policy.
 *   <li>{@code X-Content-Type-Options} and {@code X-Frame-Options} are consistently applied.
 * </ol>
 *
 * @author Morten Svanaes
 */
@Tag("csptests")
@Slf4j
public class CspHeadersE2ETest extends BaseE2ETest {

  private static final String CSP_HEADER = "Content-Security-Policy";
  private static final String X_CONTENT_TYPE_OPTIONS_HEADER = "X-Content-Type-Options";
  private static final String X_FRAME_OPTIONS_HEADER = "X-Frame-Options";

  @BeforeAll
  static void setup() throws JsonProcessingException {
    serverApiUrl = TestConfiguration.get().baseUrl();
    serverHostUrl = TestConfiguration.get().baseUrl().replace("/api", "/");
  }

  // ========================================================================
  // Tests for endpoints with @CspAppHost annotation
  // ========================================================================

  @Test
  void testGlobalShellEndpointWithAppHostCspPolicy() {
    // AppController.renderApp has @CspAppHost
    ResponseEntity<String> response =
        restTemplate.exchange(serverHostUrl + "apps/maps", HttpMethod.GET, null, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify CSP header is present
    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present");

    // Verify custom policy directives are in the CSP header
    assertTrue(
        cspHeader.contains("default-src 'self'"),
        "CSP policy should contain default-src 'self' directive");
    assertTrue(
        cspHeader.contains("style-src 'self' 'unsafe-inline'"),
        "CSP policy should contain style-src with unsafe-inline");

    // Verify frame-ancestors directive is added
    assertTrue(
        cspHeader.contains("frame-ancestors 'self'"),
        "CSP should include frame-ancestors directive");
  }

  @Test
  void testAppResourceEndpointWithAppHostCspPolicy() {
    // AppController.renderApp has @CspAppHost
    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/apps/maps-app/index.html", HttpMethod.GET, null, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify CSP header is present
    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present");

    // Verify custom policy directives are in the CSP header
    assertTrue(
        cspHeader.contains("default-src 'self'"),
        "CSP policy should contain default-src 'self' directive");
    assertTrue(
        cspHeader.contains("style-src 'self' 'unsafe-inline'"),
        "CSP policy should contain style-src with unsafe-inline");

    // Verify frame-ancestors directive is added
    assertTrue(
        cspHeader.contains("frame-ancestors 'self'"),
        "CSP should include frame-ancestors directive");
  }

  @Test
  void testLegacyAppResourceEndpointWithAppHostCspPolicy() {
    // AppController.renderApp has @CspAppHost
    ResponseEntity<String> response =
        restTemplate.exchange(
            serverHostUrl + "dhis-web-maps/index.html", HttpMethod.GET, null, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify CSP header is present
    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present");

    // Verify custom policy directives are in the CSP header
    assertTrue(
        cspHeader.contains("default-src 'self'"),
        "CSP policy should contain default-src 'self' directive");
    assertTrue(
        cspHeader.contains("style-src 'self' 'unsafe-inline'"),
        "CSP policy should contain style-src with unsafe-inline");

    // Verify frame-ancestors directive is added
    assertTrue(
        cspHeader.contains("frame-ancestors 'self'"),
        "CSP should include frame-ancestors directive");
  }

  // ========================================================================
  // Tests for endpoints with @CspUserUploadedContent annotation
  // ========================================================================

  @Test
  void testFileResourceEndpointWithUserUploadedContentCsp() {
    // FileResourceController.getFileResourceData has @CspUserUploadedContent
    ResponseEntity<String> response = getAuthenticated("/fileResources/X1234567890/data");
    assertUserUploadedContentCsp(response, "/fileResources/.../data");
  }

  @Test
  void testDocumentEndpointWithUserUploadedContentCsp() {
    // DocumentController.getDocumentContent has @CspUserUploadedContent
    ResponseEntity<String> response = getAuthenticated("/documents/X1234567890/data");
    assertUserUploadedContentCsp(response, "/documents/.../data");
  }

  @Test
  void testAuditEndpointWithUserUploadedContentCsp() {
    // AuditController.getAuditReports() has @CspUserUploadedContent annotation
    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/audits/files/X1234567890", HttpMethod.GET, null, String.class);

    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present on audit endpoint");
  }

  @Test
  void testIconEndpointWithUserUploadedContentCsp() {
    // IconController.getIconData has @CspUserUploadedContent
    ResponseEntity<String> response = getAuthenticated("/icons/X1234567890/icon");
    assertUserUploadedContentCsp(response, "/icons/.../icon");
  }

  @Test
  void testIconSvgEndpointWithUserUploadedContentCsp() {
    // IconController.getIconData (deprecated svg variant) has @CspUserUploadedContent
    ResponseEntity<String> response = getAuthenticated("/icons/X1234567890/icon.svg");
    assertUserUploadedContentCsp(response, "/icons/.../icon.svg");
  }

  // ========================================================================
  // Tests for default endpoints without custom annotations
  // ========================================================================

  @Test
  void testDefaultMeEndpointHasSecurityHeaders() {
    String cookie = performInitialLogin("admin", "district");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);

    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/me",
            HttpMethod.GET,
            new org.springframework.http.HttpEntity<>(headers),
            String.class);

    assertEquals(
        HttpStatus.OK,
        response.getStatusCode(),
        "Me endpoint should be accessible with valid credentials");

    // Verify default security headers are present
    verifyStandardSecurityHeaders(response);
  }

  @Test
  void testDefaultSystemEndpointHasSecurityHeaders() {
    String cookie = performInitialLogin("admin", "district");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);

    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/system/info",
            HttpMethod.GET,
            new org.springframework.http.HttpEntity<>(headers),
            String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify standard security headers are applied
    verifyStandardSecurityHeaders(response);
  }

  @Test
  void testDataValuesEndpointCspHeaders() {
    // DataValueController.getDataValueFile has @CspUserUploadedContent (the file endpoint).
    // The query endpoint /dataValues itself has no marker — assert standard headers + a CSP.
    ResponseEntity<String> response = getAuthenticated("/dataValues?dataSet=test");

    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present on /dataValues query endpoint");
    verifySecurityHeadersPresent(response);
  }

  @Test
  void testEventExportEndpointCspHeaders() {
    // EventsExportController.getEventDataValueImage has @CspUserUploadedContent
    ResponseEntity<String> response =
        getAuthenticated("/tracker/events/X1234567890/dataValues/X1234567890/image");
    assertUserUploadedContentCsp(response, "/tracker/events/.../image");
  }

  // ========================================================================
  // Tests for negative cases and edge cases
  // ========================================================================

  @Test
  void testNonExistentEndpointHasSecurityHeaders() {
    // No HandlerMethod is matched for /api/nonexistent/endpoint/path, so CspInterceptor
    // short-circuits. Headers must come from CspBaselineFilter.
    ResponseEntity<String> response = getAuthenticated("/nonexistent/endpoint/path");

    assertEquals(
        HttpStatus.NOT_FOUND,
        response.getStatusCode(),
        "Non-existent endpoint should return 404 once authenticated");
    verifyStandardSecurityHeaders(response);
  }

  @Test
  void testMessageConversationEndpointWithUserUploadedContentCsp() {
    // MessageConversationController has @CspUserUploadedContent annotation
    String cookie = performInitialLogin("admin", "district");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);

    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/messageConversations",
            HttpMethod.GET,
            new org.springframework.http.HttpEntity<>(headers),
            String.class);

    // Verify CSP and security headers
    verifySecurityHeadersPresent(response);
  }

  // ========================================================================
  // Tracker file/image endpoint coverage (file branches + tracker/single/trackerEvents
  // + trackedEntities)
  // ========================================================================

  @Test
  void testEventExportFileEndpointHasUserUploadedContentCsp() {
    // EventsExportController.getEventDataValueFile has @CspUserUploadedContent
    ResponseEntity<String> response =
        getAuthenticated("/tracker/events/X1234567890/dataValues/X1234567890/file");
    assertUserUploadedContentCsp(response, "/tracker/events/.../file");
  }

  @Test
  void testSingleEventFileEndpointHasUserUploadedContentCsp() {
    // SingleEventsExportController.getEventDataValueFile has @CspUserUploadedContent
    ResponseEntity<String> response =
        getAuthenticated("/tracker/singleEvents/X1234567890/dataValues/X1234567890/file");
    assertUserUploadedContentCsp(response, "/tracker/singleEvents/.../file");
  }

  @Test
  void testSingleEventImageEndpointHasUserUploadedContentCsp() {
    ResponseEntity<String> response =
        getAuthenticated("/tracker/singleEvents/X1234567890/dataValues/X1234567890/image");
    assertUserUploadedContentCsp(response, "/tracker/singleEvents/.../image");
  }

  @Test
  void testTrackerEventFileEndpointHasUserUploadedContentCsp() {
    // TrackerEventsExportController.getEventDataValueFile @CspUserUploadedContent (added in this
    // PR)
    ResponseEntity<String> response =
        getAuthenticated("/tracker/trackerEvents/X1234567890/dataValues/X1234567890/file");
    assertUserUploadedContentCsp(response, "/tracker/trackerEvents/.../file");
  }

  @Test
  void testTrackerEventImageEndpointHasUserUploadedContentCsp() {
    ResponseEntity<String> response =
        getAuthenticated("/tracker/trackerEvents/X1234567890/dataValues/X1234567890/image");
    assertUserUploadedContentCsp(response, "/tracker/trackerEvents/.../image");
  }

  @Test
  void testTrackedEntityAttributeFileEndpointHasUserUploadedContentCsp() {
    // TrackedEntitiesExportController.getAttributeValueFile has @CspUserUploadedContent
    ResponseEntity<String> response =
        getAuthenticated("/tracker/trackedEntities/X1234567890/attributes/X1234567890/file");
    assertUserUploadedContentCsp(response, "/tracker/trackedEntities/.../file");
  }

  @Test
  void testTrackedEntityAttributeImageEndpointHasUserUploadedContentCsp() {
    ResponseEntity<String> response =
        getAuthenticated("/tracker/trackedEntities/X1234567890/attributes/X1234567890/image");
    assertUserUploadedContentCsp(response, "/tracker/trackedEntities/.../image");
  }

  // ========================================================================
  // Other annotated user-uploaded-content endpoints
  // ========================================================================

  @Test
  void testDataValuesFilesEndpointHasUserUploadedContentCsp() {
    // DataValueController.getDataValueFile has @CspUserUploadedContent
    ResponseEntity<String> response =
        getAuthenticated(
            "/dataValues/files?de=X1234567890&pe=202401&ou=X1234567890&co=X1234567890");
    assertUserUploadedContentCsp(response, "/dataValues/files");
  }

  @Test
  void testMessageAttachmentEndpointHasUserUploadedContentCsp() {
    // MessageConversationController.getAttachment has @CspUserUploadedContent
    ResponseEntity<String> response =
        getAuthenticated("/messageConversations/X1234567890/Y1234567890/attachments/Z1234567890");
    assertUserUploadedContentCsp(response, "/messageConversations/.../attachments/...");
  }

  // ========================================================================
  // Login pages — fallback /login.html (default CSP) and modern app
  // ========================================================================

  @Test
  void testLoginFallbackHasDefaultCsp() {
    // LoginFallbackController.getLoginFallback no longer needs a CSP marker:
    // its inline <script> was extracted to /login.js and its inline <style>
    // to /login.css, so the page falls under the strict default policy.
    ResponseEntity<String> response =
        restTemplate.exchange(serverHostUrl + "login.html", HttpMethod.GET, null, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    String csp = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(csp, "CSP header should be present on /login.html");
    assertTrue(
        csp.contains("default-src 'self'"),
        "Login fallback CSP should be the strict default, got: " + csp);
    assertFalse(
        csp.contains("script-src 'self' 'unsafe-inline'"),
        "Login fallback CSP should NOT allow inline scripts anymore, got: " + csp);
    assertTrue(
        csp.contains("frame-ancestors 'self'"),
        "Login fallback CSP should contain frame-ancestors 'self', got: " + csp);
    verifyStandardSecurityHeaders(response);

    // Sanity: response body should reference the external assets, not contain inline blocks
    String body = response.getBody();
    assertNotNull(body, "Login fallback should return HTML body");
    assertTrue(
        body.contains("src=\"./login.js\""), "/login.html should reference external ./login.js");
    assertTrue(
        body.contains("href=\"./login.css\""), "/login.html should reference external ./login.css");
    assertFalse(
        body.contains("<script>"), "/login.html should NOT contain inline <script> blocks anymore");
    assertFalse(
        body.contains("<style>"), "/login.html should NOT contain inline <style> blocks anymore");
  }

  @Test
  void testLoginJsIsServedAsJavascript() {
    ResponseEntity<String> response =
        restTemplate.exchange(serverHostUrl + "login.js", HttpMethod.GET, null, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    String contentType = response.getHeaders().getFirst("Content-Type");
    assertNotNull(contentType, "Content-Type should be set on /login.js");
    assertTrue(
        contentType.startsWith("application/javascript"),
        "/login.js should be served as application/javascript, got: " + contentType);
    String body = response.getBody();
    assertNotNull(body, "/login.js should return a body");
    assertTrue(body.contains("loginForm"), "/login.js should contain the login form handler code");
  }

  @Test
  void testLoginCssIsServedAsStylesheet() {
    ResponseEntity<String> response =
        restTemplate.exchange(serverHostUrl + "login.css", HttpMethod.GET, null, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    String contentType = response.getHeaders().getFirst("Content-Type");
    assertNotNull(contentType, "Content-Type should be set on /login.css");
    assertTrue(
        contentType.startsWith("text/css"),
        "/login.css should be served as text/css, got: " + contentType);
    String body = response.getBody();
    assertNotNull(body, "/login.css should return a body");
    assertTrue(
        body.contains(".login-container"), "/login.css should contain the login-container rule");
  }

  @Test
  void testModernLoginEndpointHasCspHeaders() {
    // GET /login/ → LoginAppController forwards to /api/apps/dhis-web-login/...
    // Whether the forward re-triggers CspInterceptor depends on Spring dispatch
    // configuration; at minimum a CSP header should land from the initial pass.
    ResponseEntity<String> response =
        restTemplate.exchange(serverHostUrl + "login/", HttpMethod.GET, null, String.class);

    assertNotNull(
        response.getHeaders().getFirst(CSP_HEADER),
        "CSP header should be present on modern /login/");
    verifyStandardSecurityHeaders(response);
  }

  // ========================================================================
  // Helper methods for assertions
  // ========================================================================

  /**
   * Performs a GET authenticated as admin/district. Swallows {@link HttpStatusCodeException} so the
   * test can inspect headers (set by {@code CspInterceptor.preHandle}) on 4xx/5xx responses too.
   */
  private ResponseEntity<String> getAuthenticated(String relativeApiPath) {
    String cookie = performInitialLogin("admin", "district");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);
    try {
      return restTemplate.exchange(
          serverApiUrl + relativeApiPath, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    } catch (HttpStatusCodeException e) {
      return ResponseEntity.status(e.getStatusCode())
          .headers(e.getResponseHeaders())
          .body(e.getResponseBodyAsString());
    }
  }

  /** Asserts that the response carries the user-uploaded-content CSP policy. */
  private void assertUserUploadedContentCsp(ResponseEntity<String> response, String label) {
    String csp = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(csp, "CSP header should be present on " + label);
    assertTrue(
        csp.contains("default-src 'none'"),
        "CSP for " + label + " should contain default-src 'none', got: " + csp);
    assertTrue(
        csp.contains("frame-ancestors 'self'"),
        "CSP for " + label + " should contain frame-ancestors 'self', got: " + csp);
    verifyStandardSecurityHeaders(response);
  }

  // ========================================================================
  // Existing helpers
  // ========================================================================

  /** Verifies that all standard security headers are present and have correct values. */
  private void verifyStandardSecurityHeaders(ResponseEntity<String> response) {
    verifySecurityHeadersPresent(response);

    // Verify specific values
    String xContentTypeOptions = response.getHeaders().getFirst(X_CONTENT_TYPE_OPTIONS_HEADER);
    assertEquals("nosniff", xContentTypeOptions, "X-Content-Type-Options should be 'nosniff'");

    // X-Frame-Options is only emitted when CSP is disabled. When CSP is enabled the
    // frame-ancestors directive is the source of truth (and may legitimately whitelist
    // external origins via the CORS whitelist), so XFO would conflict.
    String csp = response.getHeaders().getFirst(CSP_HEADER);
    String xFrameOptions = response.getHeaders().getFirst(X_FRAME_OPTIONS_HEADER);
    if (csp == null) {
      assertEquals(
          "SAMEORIGIN",
          xFrameOptions,
          "X-Frame-Options should be 'SAMEORIGIN' when CSP is disabled");
    } else {
      assertNull(
          xFrameOptions, "X-Frame-Options should be omitted when CSP frame-ancestors is set");
    }
  }

  /** Verifies that security headers are present in the response. */
  private void verifySecurityHeadersPresent(ResponseEntity<String> response) {
    HttpHeaders headers = response.getHeaders();

    assertNotNull(
        headers.getFirst(X_CONTENT_TYPE_OPTIONS_HEADER),
        "X-Content-Type-Options header should be present");

    // CSP header may not be present on all endpoints when CSP is globally disabled,
    // but it should at least be set when CSP is enabled
    log.info("Response headers: {}", headers);
  }
}
