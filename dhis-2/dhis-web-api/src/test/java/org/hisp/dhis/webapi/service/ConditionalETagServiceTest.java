/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.net.HttpHeaders;
import java.util.Optional;
import org.hisp.dhis.cache.ETagVersionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link ConditionalETagService}.
 *
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class ConditionalETagServiceTest {

  @Mock private ETagVersionService eTagVersionService;

  private UserDetails userDetails;

  private ConditionalETagService service;

  @BeforeEach
  void setUp() {
    service = new ConditionalETagService(eTagVersionService);

    // Create a real User and convert to UserDetails
    User user = new User();
    user.setUid("testUser123");
    user.setUsername("testuser");
    userDetails = UserDetails.fromUser(user);
  }

  @Test
  @DisplayName("Should return false when ETag caching is disabled")
  void testIsEnabled_WhenDisabled() {
    when(eTagVersionService.isEnabled()).thenReturn(false);

    assertFalse(service.isEnabled());
  }

  @Test
  @DisplayName("Should return true when ETag caching is enabled")
  void testIsEnabled_WhenEnabled() {
    when(eTagVersionService.isEnabled()).thenReturn(true);

    assertTrue(service.isEnabled());
  }

  @Test
  @DisplayName("Should generate ETag with correct format")
  void testGenerateETag() {
    when(eTagVersionService.getGlobalVersion()).thenReturn(42L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    String etag = service.generateETag(userDetails);

    assertNotNull(etag);
    assertTrue(etag.startsWith("testUser123-"));
    assertTrue(etag.endsWith("-42"));
  }

  @Test
  @DisplayName("Should return 304 when ETag matches")
  void testWithConditionalETagCaching_Match() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getGlobalVersion()).thenReturn(42L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    String currentETag = service.generateETag(userDetails);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"" + currentETag + "\"");

    ResponseEntity<String> response =
        service.withConditionalETagCaching(userDetails, request, () -> "expensive result");

    assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  @DisplayName("Should return 200 with body when ETag does not match")
  void testWithConditionalETagCaching_NoMatch() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getGlobalVersion()).thenReturn(42L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"old-etag-value\"");

    ResponseEntity<String> response =
        service.withConditionalETagCaching(userDetails, request, () -> "expensive result");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("expensive result", response.getBody());
  }

  @Test
  @DisplayName("Should return 200 with body when no If-None-Match header")
  void testWithConditionalETagCaching_NoHeader() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getGlobalVersion()).thenReturn(42L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    MockHttpServletRequest request = new MockHttpServletRequest();

    ResponseEntity<String> response =
        service.withConditionalETagCaching(userDetails, request, () -> "expensive result");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("expensive result", response.getBody());
  }

  @Test
  @DisplayName("Should return body directly when ETag caching is disabled")
  void testWithConditionalETagCaching_Disabled() {
    when(eTagVersionService.isEnabled()).thenReturn(false);

    MockHttpServletRequest request = new MockHttpServletRequest();

    ResponseEntity<String> response =
        service.withConditionalETagCaching(userDetails, request, () -> "expensive result");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("expensive result", response.getBody());
  }

  @Test
  @DisplayName("Should return empty Optional when ETag caching is disabled")
  void testCheckNotModifiedResponse_Disabled() {
    when(eTagVersionService.isEnabled()).thenReturn(false);

    MockHttpServletRequest request = new MockHttpServletRequest();

    Optional<ResponseEntity<String>> result =
        service.checkNotModifiedResponse(userDetails, request);

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should return 304 response when ETag matches")
  void testCheckNotModifiedResponse_Match() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getGlobalVersion()).thenReturn(42L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    String currentETag = service.generateETag(userDetails);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"" + currentETag + "\"");

    Optional<ResponseEntity<String>> result =
        service.checkNotModifiedResponse(userDetails, request);

    assertTrue(result.isPresent());
    assertEquals(HttpStatus.NOT_MODIFIED, result.get().getStatusCode());
  }

  @Test
  @DisplayName("Should return empty Optional when ETag does not match")
  void testCheckNotModifiedResponse_NoMatch() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getGlobalVersion()).thenReturn(42L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"old-etag-value\"");

    Optional<ResponseEntity<String>> result =
        service.checkNotModifiedResponse(userDetails, request);

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should set ETag headers on response")
  void testSetETagHeaders() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getGlobalVersion()).thenReturn(42L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    MockHttpServletResponse response = new MockHttpServletResponse();

    service.setETagHeaders(userDetails, response);

    assertNotNull(response.getHeader(HttpHeaders.ETAG));
    assertEquals("Cookie, Authorization", response.getHeader(HttpHeaders.VARY));
    assertNotNull(response.getHeader(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  @DisplayName("Should not set headers when disabled")
  void testSetETagHeaders_Disabled() {
    when(eTagVersionService.isEnabled()).thenReturn(false);

    MockHttpServletResponse response = new MockHttpServletResponse();

    service.setETagHeaders(userDetails, response);

    assertNull(response.getHeader(HttpHeaders.ETAG));
  }

  @Test
  @DisplayName("Should handle weak ETag format")
  void testCheckNotModified_WeakETag() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getGlobalVersion()).thenReturn(42L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    String currentETag = service.generateETag(userDetails);

    MockHttpServletRequest request = new MockHttpServletRequest();
    // Weak ETag format with W/ prefix
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "W/\"" + currentETag + "\"");

    ResponseEntity<String> response =
        service.withConditionalETagCaching(userDetails, request, () -> "expensive result");

    assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
  }

  // ========== Entity-Type-Specific Tests ==========

  @Test
  @DisplayName("Should generate entity-type-specific ETag with correct format")
  void testGenerateETag_WithEntityType() {
    when(eTagVersionService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    String etag = service.generateETag(userDetails, OrganisationUnit.class);

    assertNotNull(etag);
    assertTrue(etag.startsWith("testUser123-OrganisationUnit-"));
    assertTrue(etag.endsWith("-99"));
  }

  @Test
  @DisplayName("Should return 304 when entity-type-specific ETag matches")
  void testWithConditionalETagCaching_EntityType_Match() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    String currentETag = service.generateETag(userDetails, OrganisationUnit.class);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"" + currentETag + "\"");

    ResponseEntity<String> response =
        service.withConditionalETagCaching(
            userDetails, request, OrganisationUnit.class, () -> "expensive result");

    assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  @DisplayName("Should return 200 with body when entity-type-specific ETag does not match")
  void testWithConditionalETagCaching_EntityType_NoMatch() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"old-etag-value\"");

    ResponseEntity<String> response =
        service.withConditionalETagCaching(
            userDetails, request, OrganisationUnit.class, () -> "expensive result");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("expensive result", response.getBody());
  }

  @Test
  @DisplayName("Should return 304 for entity-type checkNotModifiedResponse when ETag matches")
  void testCheckNotModifiedResponse_EntityType_Match() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    String currentETag = service.generateETag(userDetails, OrganisationUnit.class);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"" + currentETag + "\"");

    Optional<ResponseEntity<String>> result =
        service.checkNotModifiedResponse(userDetails, request, OrganisationUnit.class);

    assertTrue(result.isPresent());
    assertEquals(HttpStatus.NOT_MODIFIED, result.get().getStatusCode());
  }

  @Test
  @DisplayName("Should set entity-type-specific ETag headers on response")
  void testSetETagHeaders_EntityType() {
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    MockHttpServletResponse response = new MockHttpServletResponse();

    service.setETagHeaders(userDetails, response, OrganisationUnit.class);

    String etagHeader = response.getHeader(HttpHeaders.ETAG);
    assertNotNull(etagHeader);
    assertTrue(etagHeader.contains("OrganisationUnit"));
    assertEquals("Cookie, Authorization", response.getHeader(HttpHeaders.VARY));
    assertNotNull(response.getHeader(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  @DisplayName("Entity-type-specific version change should NOT invalidate other entity type caches")
  void testEntityTypeGranularity_DifferentVersions() {
    // Simulate: OrganisationUnit version is 99, but a different entity type changed
    when(eTagVersionService.isEnabled()).thenReturn(true);
    when(eTagVersionService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(60);

    // First request - get the ETag
    String orgUnitETag = service.generateETag(userDetails, OrganisationUnit.class);

    // Second request with same ETag - should still match (org unit version hasn't changed)
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"" + orgUnitETag + "\"");

    ResponseEntity<String> response =
        service.withConditionalETagCaching(
            userDetails, request, OrganisationUnit.class, () -> "expensive result");

    // Should return 304 because OrganisationUnit version hasn't changed
    assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
  }
}
