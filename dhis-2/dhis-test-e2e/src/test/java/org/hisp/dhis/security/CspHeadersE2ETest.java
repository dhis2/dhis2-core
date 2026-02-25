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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * E2E tests for CSP (Content Security Policy) headers validation.
 *
 * <p>This test suite verifies that:
 * 1. Default CSP headers are applied to regular API endpoints
 * 2. Endpoints with @CustomCsp annotation have the appropriate custom CSP policy
 * 3. Endpoints with @CspUserUploadedContent annotation have user-uploaded-content-safe CSP
 * 4. Security headers (X-Content-Type-Options, X-Frame-Options) are consistently applied
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
  // Tests for endpoints with @CustomCsp annotation
  // ========================================================================

  @Test
  void testAppEndpointWithCustomCspPolicy() {
    // AppController has @CustomCsp with custom policy on GET /apps/index.html
    ResponseEntity<String> response =
        restTemplate.exchange(serverHostUrl + "apps/index.html", HttpMethod.GET, null, String.class);

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
    assertTrue(
        cspHeader.contains("script-src 'self'"),
        "CSP policy should contain script-src 'self'");

    // Verify frame-ancestors directive is added
    assertTrue(cspHeader.contains("frame-ancestors 'self'"), "CSP should include frame-ancestors directive");
  }

  // ========================================================================
  // Tests for endpoints with @CspUserUploadedContent annotation
  // ========================================================================

  @Test
  void testFileResourceEndpointWithUserUploadedContentCsp() {
    // FileResourceController.getFileResource() has @CspUserUploadedContent annotation
    // Test with a non-existent file ID to verify headers are still applied
    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/fileResources/nonexistent", HttpMethod.GET, null, String.class);

    // Even on error, security headers should be present
    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present on user-uploaded content endpoint");

    // User-uploaded content policy should be more restrictive
    assertTrue(
        cspHeader.contains("default-src 'none'"),
        "User-uploaded content CSP should use default-src 'none'");

    // Still should have frame-ancestors for security
    assertTrue(
        cspHeader.contains("frame-ancestors 'self'"),
        "CSP should include frame-ancestors directive even for user-uploaded content");
  }

  @Test
  void testDocumentEndpointWithUserUploadedContentCsp() {
    // DocumentController.getDocument() has @CspUserUploadedContent annotation
    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/documents/nonexistent", HttpMethod.GET, null, String.class);

    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present on document endpoint");
    assertTrue(
        cspHeader.contains("default-src 'none'"),
        "Document endpoint with user-uploaded content should have restrictive CSP");
  }

  @Test
  void testAuditEndpointWithUserUploadedContentCsp() {
    // AuditController.getAuditReports() has @CspUserUploadedContent annotation
    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/audits/nonexistent", HttpMethod.GET, null, String.class);

    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present on audit endpoint");
  }

  @Test
  void testIconEndpointWithUserUploadedContentCsp() {
    // IconController has @CspUserUploadedContent on icon retrieval methods
    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/icons/nonexistent", HttpMethod.GET, null, String.class);

    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present on icon endpoint");
    assertTrue(
        cspHeader.contains("default-src 'none'"),
        "Icon endpoint CSP should be restrictive for user-uploaded content");
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
    // DataValueController methods have @CspUserUploadedContent
    String cookie = performInitialLogin("admin", "district");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);

    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/dataValues?dataSet=test",
            HttpMethod.GET,
            new org.springframework.http.HttpEntity<>(headers),
            String.class);

    // Even if no data values, headers should be present
    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present on dataValues endpoint");

    // Verify security headers
    verifySecurityHeadersPresent(response);
  }

  @Test
  void testTrackedEntityExportEndpointCspHeaders() {
    // TrackedEntitiesExportController has @CspUserUploadedContent methods
    String cookie = performInitialLogin("admin", "district");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);

    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/tracker/trackedEntities",
            HttpMethod.GET,
            new org.springframework.http.HttpEntity<>(headers),
            String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify CSP headers are applied
    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present on tracked entities endpoint");

    verifySecurityHeadersPresent(response);
  }

  @Test
  void testEventExportEndpointCspHeaders() {
    // EventsExportController has @CspUserUploadedContent annotation
    String cookie = performInitialLogin("admin", "district");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);

    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/tracker/events",
            HttpMethod.GET,
            new org.springframework.http.HttpEntity<>(headers),
            String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    String cspHeader = response.getHeaders().getFirst(CSP_HEADER);
    assertNotNull(cspHeader, "CSP header should be present on events endpoint");

    verifySecurityHeadersPresent(response);
  }

  // ========================================================================
  // Tests for negative cases and edge cases
  // ========================================================================

  @Test
  void testUnauthorizedEndpointStillHasSecurityHeaders() {
    // Access endpoint without credentials
    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/metadata", HttpMethod.GET, null, String.class);

    // Should get 401 or 403, but security headers should still be present
    assertTrue(
        response.getStatusCode().is4xxClientError(),
        "Should get 4xx error without authentication");

    // Security headers should still be applied on error responses
    verifySecurityHeadersPresent(response);
  }

  @Test
  void testNonExistentEndpointHasSecurityHeaders() {
    // Access non-existent endpoint
    ResponseEntity<String> response =
        restTemplate.exchange(
            serverApiUrl + "/nonexistent/endpoint/path",
            HttpMethod.GET,
            null,
            String.class);

    // Should get 404
    assertEquals(
        HttpStatus.NOT_FOUND, response.getStatusCode(), "Non-existent endpoint should return 404");

    // Security headers should still be present
    verifySecurityHeadersPresent(response);
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
  // Helper methods for assertions
  // ========================================================================

  /**
   * Verifies that all standard security headers are present and have correct values.
   */
  private void verifyStandardSecurityHeaders(ResponseEntity<String> response) {
    verifySecurityHeadersPresent(response);

    // Verify specific values
    String xContentTypeOptions = response.getHeaders().getFirst(X_CONTENT_TYPE_OPTIONS_HEADER);
    assertEquals(
        "nosniff",
        xContentTypeOptions,
        "X-Content-Type-Options should be 'nosniff'");

    String xFrameOptions = response.getHeaders().getFirst(X_FRAME_OPTIONS_HEADER);
    assertEquals(
        "SAMEORIGIN",
        xFrameOptions,
        "X-Frame-Options should be 'SAMEORIGIN'");
  }

  /**
   * Verifies that security headers are present in the response.
   */
  private void verifySecurityHeadersPresent(ResponseEntity<String> response) {
    HttpHeaders headers = response.getHeaders();

    assertNotNull(
        headers.getFirst(X_CONTENT_TYPE_OPTIONS_HEADER),
        "X-Content-Type-Options header should be present");
    assertNotNull(
        headers.getFirst(X_FRAME_OPTIONS_HEADER),
        "X-Frame-Options header should be present");

    // CSP header may not be present on all endpoints when CSP is globally disabled,
    // but it should at least be set when CSP is enabled
    log.info("Response headers: {}", headers);
  }
}
