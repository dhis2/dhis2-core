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
  @DisplayName("Should set headers from an already computed ETag value")
  void testSetETagHeaders_WithStoredValue() {
    when(eTagService.getStaleWhileRevalidateSeconds()).thenReturn(60);
    MockHttpServletResponse response = new MockHttpServletResponse();

    service.setETagHeaders(response, "stored-etag-value");

    assertEquals("\"stored-etag-value\"", response.getHeader(HttpHeaders.ETAG));
    assertEquals("Cookie, Authorization", response.getHeader(HttpHeaders.VARY));
    assertEquals(
        "max-age=0, private, stale-while-revalidate=60",
        response.getHeader(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  @DisplayName("Should handle weak ETag format")
  void testCheckNotModified_WeakETag() {
    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(99L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    String currentETag = service.generateETag(userDetails, OrganisationUnit.class);

    MockHttpServletRequest request = new MockHttpServletRequest();
    // Weak ETag format with W/ prefix
    request.addHeader(HttpHeaders.IF_NONE_MATCH, "W/\"" + currentETag + "\"");

    assertTrue(service.checkNotModified(request, currentETag));
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
  @DisplayName("Changing all-cache version should invalidate entity and composite ETags")
  void testAllCacheVersionChangesAllEtagFamilies() {

    when(eTagService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(10L);
    when(eTagService.getEntityTypeVersion(User.class)).thenReturn(20L);
    when(eTagService.getTtlMinutes()).thenReturn(60);

    when(eTagService.getAllCacheVersion()).thenReturn(7L);
    String entityEtag1 = service.generateETag(userDetails, OrganisationUnit.class);
    String compositeEtag1 =
        service.generateETag(userDetails, Set.of(OrganisationUnit.class, User.class));

    when(eTagService.getAllCacheVersion()).thenReturn(8L);
    String entityEtag2 = service.generateETag(userDetails, OrganisationUnit.class);
    String compositeEtag2 =
        service.generateETag(userDetails, Set.of(OrganisationUnit.class, User.class));

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

  @Test
  @DisplayName("Should generate deterministic ETag for named-key endpoint with no dependencies")
  void testGenerateETag_NamedKeyNoDeps() {
    when(eTagService.getTtlMinutes()).thenReturn(60);

    String etag = service.generateETag(userDetails, Set.of(), Set.of());

    assertNotNull(etag);
    assertEquals(64, etag.length());
    assertEquals(etag, service.generateETag(userDetails, Set.of(), Set.of()));
  }

  @Test
  @DisplayName("Should generate different ETags for different named keys")
  void testGenerateETag_NamedKeyDifferentKeys() {
    when(eTagService.getTtlMinutes()).thenReturn(60);
    when(eTagService.getNamedVersion("installedApps")).thenReturn(1L);
    when(eTagService.getNamedVersion("otherKey")).thenReturn(1L);

    String etag1 = service.generateETag(userDetails, Set.of(), Set.of("installedApps"));
    String etag2 = service.generateETag(userDetails, Set.of(), Set.of("otherKey"));

    assertNotEquals(etag1, etag2);
  }

  @Test
  @DisplayName("Should invalidate when named version is incremented")
  void testGenerateETag_NamedKeyVersionChange() {
    when(eTagService.getTtlMinutes()).thenReturn(60);
    when(eTagService.getNamedVersion("installedApps")).thenReturn(1L);

    String etag1 = service.generateETag(userDetails, Set.of(), Set.of("installedApps"));

    when(eTagService.getNamedVersion("installedApps")).thenReturn(2L);
    String etag2 = service.generateETag(userDetails, Set.of(), Set.of("installedApps"));

    assertNotEquals(etag1, etag2);
  }

  @Test
  @DisplayName("Should combine entity types and named keys in ETag")
  void testGenerateETag_NamedKeyWithEntityTypes() {
    when(eTagService.getTtlMinutes()).thenReturn(60);
    when(eTagService.getEntityTypeVersion(User.class)).thenReturn(10L);
    when(eTagService.getEntityTypeVersion(UserRole.class)).thenReturn(20L);
    when(eTagService.getNamedVersion("installedApps")).thenReturn(5L);

    String etag =
        service.generateETag(
            userDetails, Set.of(User.class, UserRole.class), Set.of("installedApps"));

    assertNotNull(etag);
    assertEquals(64, etag.length());

    // Changing entity type version should invalidate
    when(eTagService.getEntityTypeVersion(User.class)).thenReturn(11L);
    String etag2 =
        service.generateETag(
            userDetails, Set.of(User.class, UserRole.class), Set.of("installedApps"));
    assertNotEquals(etag, etag2);
  }
}
