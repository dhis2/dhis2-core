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
import java.util.Set;
import org.hisp.dhis.cache.ETagService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.CacheControl;
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

  @Mock private ETagService eTagService;

  private UserDetails userDetails;

  private ConditionalETagService service;

  @BeforeEach
  void setUp() {
    service = new ConditionalETagService(eTagService);

    // Create a real User and convert to UserDetails
    User user = new User();
    user.setUid("testUser123");
    user.setUsername("testuser");
    userDetails = UserDetails.fromUser(user);
  }

  @Test
  @DisplayName("Should return false when ETag caching is disabled")
  void testIsEnabled_WhenDisabled() {
    when(eTagService.isEnabled()).thenReturn(false);

    assertFalse(service.isEnabled());
  }

  @Test
  @DisplayName("Should return true when ETag caching is enabled")
  void testIsEnabled_WhenEnabled() {
    when(eTagService.isEnabled()).thenReturn(true);

    assertTrue(service.isEnabled());
  }

  @Test
  @DisplayName("Should generate hashed ETag that is deterministic")
  void testGenerateETag() {
    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    String etag = service.generateETag(userDetails);

    assertNotNull(etag);
    // Hashed ETag should be a 64-char hex string (SHA-256)
    assertEquals(64, etag.length());
    assertTrue(etag.matches("[0-9a-f]+"), "ETag should be hex-encoded");
    // Same inputs should produce same ETag
    assertEquals(etag, service.generateETag(userDetails));
  }

  @Test
  @DisplayName("Should return 304 when ETag matches")
  void testWithConditionalETagCaching_Match() {
    when(eTagService.isEnabled()).thenReturn(true);

    when(eTagService.getTtlMinutes()).thenReturn(60);

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
    when(eTagService.isEnabled()).thenReturn(true);

    when(eTagService.getTtlMinutes()).thenReturn(60);

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
    when(eTagService.isEnabled()).thenReturn(true);

    when(eTagService.getTtlMinutes()).thenReturn(60);

    MockHttpServletRequest request = new MockHttpServletRequest();

    ResponseEntity<String> response =
        service.withConditionalETagCaching(userDetails, request, () -> "expensive result");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("expensive result", response.getBody());
  }

  @Test
  @DisplayName("Should return body directly when ETag caching is disabled")
  void testWithConditionalETagCaching_Disabled() {
    when(eTagService.isEnabled()).thenReturn(false);

    MockHttpServletRequest request = new MockHttpServletRequest();

    ResponseEntity<String> response =
        service.withConditionalETagCaching(userDetails, request, () -> "expensive result");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("expensive result", response.getBody());
  }

  @Test
  @DisplayName("Should return empty Optional when ETag caching is disabled")
  void testCheckNotModifiedResponse_Disabled() {
    when(eTagService.isEnabled()).thenReturn(false);

    MockHttpServletRequest request = new MockHttpServletRequest();

    Optional<ResponseEntity<String>> result =
        service.checkNotModifiedResponse(userDetails, request);

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should return 304 response when ETag matches")
  void testCheckNotModifiedResponse_Match() {
    when(eTagService.isEnabled()).thenReturn(true);

    when(eTagService.getTtlMinutes()).thenReturn(60);

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
    when(eTagService.isEnabled()).thenReturn(true);

    when(eTagService.getTtlMinutes()).thenReturn(60);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"old-etag-value\"");

    Optional<ResponseEntity<String>> result =
        service.checkNotModifiedResponse(userDetails, request);

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should set ETag headers on response")
  void testSetETagHeaders() {
    when(eTagService.isEnabled()).thenReturn(true);

    when(eTagService.getTtlMinutes()).thenReturn(60);

    MockHttpServletResponse response = new MockHttpServletResponse();

    service.setETagHeaders(userDetails, response);

    assertNotNull(response.getHeader(HttpHeaders.ETAG));
    assertEquals("Cookie, Authorization", response.getHeader(HttpHeaders.VARY));
    assertNotNull(response.getHeader(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  @DisplayName("Should not set headers when disabled")
  void testSetETagHeaders_Disabled() {
    when(eTagService.isEnabled()).thenReturn(false);

    MockHttpServletResponse response = new MockHttpServletResponse();

    service.setETagHeaders(userDetails, response);

    assertNull(response.getHeader(HttpHeaders.ETAG));
  }

  @Test
  @DisplayName("Should set headers from an already computed ETag value")
  void testSetETagHeaders_WithStoredValue() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    service.setETagHeaders(response, "stored-etag-value");

    assertEquals("\"stored-etag-value\"", response.getHeader(HttpHeaders.ETAG));
    assertEquals("Cookie, Authorization", response.getHeader(HttpHeaders.VARY));
    assertEquals(
        CacheControl.noCache().cachePrivate().mustRevalidate().getHeaderValue(),
        response.getHeader(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  @DisplayName("Should handle weak ETag format")
  void testCheckNotModified_WeakETag() {
    when(eTagService.isEnabled()).thenReturn(true);

    when(eTagService.getTtlMinutes()).thenReturn(60);

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
  @DisplayName("Should generate hashed entity-type-specific ETag that is deterministic")
  void testGenerateETag_WithEntityType() {
    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    String etag = service.generateETag(userDetails, OrganisationUnit.class);

    assertNotNull(etag);
    assertEquals(64, etag.length());
    assertTrue(etag.matches("[0-9a-f]+"), "ETag should be hex-encoded");
    assertEquals(etag, service.generateETag(userDetails, OrganisationUnit.class));
  }

  @Test
  @DisplayName("Should return 304 when entity-type-specific ETag matches")
  void testWithConditionalETagCaching_EntityType_Match() {
    when(eTagService.isEnabled()).thenReturn(true);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

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
    when(eTagService.isEnabled()).thenReturn(true);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

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
    when(eTagService.isEnabled()).thenReturn(true);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

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
    when(eTagService.isEnabled()).thenReturn(true);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    MockHttpServletResponse response = new MockHttpServletResponse();

    service.setETagHeaders(userDetails, response, OrganisationUnit.class);

    String etagHeader = response.getHeader(HttpHeaders.ETAG);
    assertNotNull(etagHeader);
    // ETag is now hashed — just verify it's set and is a quoted hex string
    assertTrue(etagHeader.startsWith("\"") && etagHeader.endsWith("\""));
    assertEquals("Cookie, Authorization", response.getHeader(HttpHeaders.VARY));
    assertNotNull(response.getHeader(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  @DisplayName("Entity-type-specific version change should NOT invalidate other entity type caches")
  void testEntityTypeGranularity_DifferentVersions() {
    // Simulate: OrganisationUnit version is 99, but a different entity type changed
    when(eTagService.isEnabled()).thenReturn(true);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

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

  // ========== Composite Entity-Type Tests ==========

  @Test
  @DisplayName("Should generate hashed composite ETag that is deterministic")
  void testGenerateETag_Composite() {
    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(10L);
    when(eTagService.getEntityTypeVersion(User.class)).thenReturn(20L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    Set<Class<?>> types = Set.of(OrganisationUnit.class, User.class);
    String etag = service.generateETag(userDetails, types);

    assertNotNull(etag);
    assertEquals(64, etag.length());
    assertTrue(etag.matches("[0-9a-f]+"), "ETag should be hex-encoded");
    assertEquals(etag, service.generateETag(userDetails, types));
  }

  @Test
  @DisplayName("Should set composite ETag headers on response")
  void testSetETagHeaders_Composite() {
    when(eTagService.isEnabled()).thenReturn(true);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(10L);
    when(eTagService.getEntityTypeVersion(User.class)).thenReturn(20L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    MockHttpServletResponse response = new MockHttpServletResponse();

    service.setETagHeaders(userDetails, response, Set.of(OrganisationUnit.class, User.class));

    String etagHeader = response.getHeader(HttpHeaders.ETAG);
    assertNotNull(etagHeader);
    // ETag is now hashed — just verify it's set and is a quoted hex string
    assertTrue(etagHeader.startsWith("\"") && etagHeader.endsWith("\""));
    assertEquals("Cookie, Authorization", response.getHeader(HttpHeaders.VARY));
    assertNotNull(response.getHeader(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  @DisplayName("Changing all-cache version should invalidate global, entity, and composite ETags")
  void testAllCacheVersionChangesAllEtagFamilies() {

    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(10L);
    when(eTagService.getEntityTypeVersion(User.class)).thenReturn(20L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    String globalEtag1 = service.generateETag(userDetails);
    String entityEtag1 = service.generateETag(userDetails, OrganisationUnit.class);
    String compositeEtag1 =
        service.generateETag(userDetails, Set.of(OrganisationUnit.class, User.class));

    when(eTagService.getAllCacheVersion()).thenReturn(8L);
    String globalEtag2 = service.generateETag(userDetails);
    String entityEtag2 = service.generateETag(userDetails, OrganisationUnit.class);
    String compositeEtag2 =
        service.generateETag(userDetails, Set.of(OrganisationUnit.class, User.class));

    assertNotEquals(globalEtag1, globalEtag2);
    assertNotEquals(entityEtag1, entityEtag2);
    assertNotEquals(compositeEtag1, compositeEtag2);
  }

  @Test
  @DisplayName("Changing user role version should invalidate metadata-style composite ETags")
  void testMetadataCompositeInvalidatesWhenUserRoleVersionChanges() {
    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    when(eTagService.getEntityTypeVersion(User.class)).thenReturn(10L);
    when(eTagService.getEntityTypeVersion(UserRole.class)).thenReturn(20L);
    when(eTagService.getEntityTypeVersion(UserGroup.class)).thenReturn(30L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    Set<Class<?>> metadataTypes = Set.of(User.class, UserRole.class, UserGroup.class);
    String etag1 = service.generateETag(userDetails, metadataTypes);

    when(eTagService.getEntityTypeVersion(UserRole.class)).thenReturn(21L);
    String etag2 = service.generateETag(userDetails, metadataTypes);

    assertNotEquals(etag1, etag2);
  }

  @Test
  @DisplayName(
      "Changing user group version should invalidate shareable metadata-style composite ETags")
  void testMetadataCompositeInvalidatesWhenUserGroupVersionChanges() {
    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(10L);
    when(eTagService.getEntityTypeVersion(User.class)).thenReturn(20L);
    when(eTagService.getEntityTypeVersion(UserRole.class)).thenReturn(30L);
    when(eTagService.getEntityTypeVersion(UserGroup.class)).thenReturn(40L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    Set<Class<?>> metadataTypes =
        Set.of(OrganisationUnit.class, User.class, UserRole.class, UserGroup.class);
    String etag1 = service.generateETag(userDetails, metadataTypes);

    when(eTagService.getEntityTypeVersion(UserGroup.class)).thenReturn(41L);
    String etag2 = service.generateETag(userDetails, metadataTypes);

    assertNotEquals(etag1, etag2);
  }

  @Test
  @DisplayName("Different TTL window values should produce different ETags")
  void testTtlWindowChangeProducesDifferentETag() {
    when(eTagService.getAllCacheVersion()).thenReturn(7L);

    // With 60-minute TTL
    when(eTagService.getTtlMinutes()).thenReturn(60);
    String etag60 = service.generateETag(userDetails);

    // With 10-minute TTL — different window granularity produces different time buckets
    when(eTagService.getTtlMinutes()).thenReturn(10);
    String etag10 = service.generateETag(userDetails);

    // Different TTL values should produce different ETags (different time window buckets)
    assertNotEquals(etag60, etag10);
  }

  @Test
  @DisplayName("Same TTL window should produce same ETag within same window")
  void testSameTtlWindowProducesSameETag() {
    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    String etag1 = service.generateETag(userDetails);
    String etag2 = service.generateETag(userDetails);

    // Within the same time window, ETags should be identical
    assertEquals(etag1, etag2);
  }

  @Test
  @DisplayName("TTL window also affects entity-type-specific ETags")
  void testTtlWindowAffectsEntityTypeETag() {
    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(10L);

    when(eTagService.getTtlMinutes()).thenReturn(60);
    String etag60 = service.generateETag(userDetails, OrganisationUnit.class);

    when(eTagService.getTtlMinutes()).thenReturn(10);
    String etag10 = service.generateETag(userDetails, OrganisationUnit.class);

    assertNotEquals(etag60, etag10);
  }

  @Test
  @DisplayName("Changing file resource version should invalidate metadata-style composite ETags")
  void testMetadataCompositeInvalidatesWhenFileResourceVersionChanges() {
    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    when(eTagService.getEntityTypeVersion(User.class)).thenReturn(10L);
    when(eTagService.getEntityTypeVersion(UserRole.class)).thenReturn(20L);
    when(eTagService.getEntityTypeVersion(FileResource.class)).thenReturn(30L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    Set<Class<?>> metadataTypes = Set.of(User.class, UserRole.class, FileResource.class);
    String etag1 = service.generateETag(userDetails, metadataTypes);

    when(eTagService.getEntityTypeVersion(FileResource.class)).thenReturn(31L);
    String etag2 = service.generateETag(userDetails, metadataTypes);

    assertNotEquals(etag1, etag2);
  }
}
