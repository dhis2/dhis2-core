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
package org.hisp.dhis.webapi.mvc.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.userdatastore.UserDatastoreEntry;
import org.hisp.dhis.webapi.service.ConditionalETagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link ConditionalETagInterceptor}.
 *
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class ConditionalETagInterceptorTest {

  @Mock private ConditionalETagService conditionalETagService;
  @Mock private SchemaService schemaService;

  private ConditionalETagInterceptor interceptor;

  private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    interceptor = new ConditionalETagInterceptor(conditionalETagService, schemaService, null, null);

    userDetails = mock(UserDetails.class);
    lenient().when(userDetails.getUid()).thenReturn("userUid123");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void testExtractApiRelativePathStandard() {
    assertEquals(
        "organisationUnits",
        ConditionalETagInterceptor.extractApiRelativePath("/api/organisationUnits"));
  }

  @Test
  void testExtractApiRelativePathWithVersion() {
    assertEquals(
        "organisationUnits/abc1234567",
        ConditionalETagInterceptor.extractApiRelativePath("/api/41/organisationUnits/abc1234567"));
  }

  @Test
  void testExtractApiRelativePathIgnoresQueryParameters() {
    assertEquals(
        "systemSettings/applicationTitle",
        ConditionalETagInterceptor.extractApiRelativePath(
            "/api/systemSettings/applicationTitle?fields=id"));
  }

  @Test
  void testExtractApiRelativePathTrimsTrailingSlash() {
    assertEquals(
        "systemSettings/applicationTitle",
        ConditionalETagInterceptor.extractApiRelativePath("/api/systemSettings/applicationTitle/"));
  }

  @Test
  void testExtractResourceNameStandard() {
    assertEquals(
        "organisationUnits",
        ConditionalETagInterceptor.extractResourceName("/api/organisationUnits"));
  }

  @Test
  void testExtractResourceNameWithVersion() {
    assertEquals(
        "organisationUnits",
        ConditionalETagInterceptor.extractResourceName("/api/41/organisationUnits"));
  }

  @Test
  void testExtractResourceNameWithIdSuffix() {
    assertEquals(
        "organisationUnits",
        ConditionalETagInterceptor.extractResourceName("/api/41/organisationUnits/abc1234567"));
  }

  @Test
  void testExtractResourceNameMeSettings() {
    assertEquals("me", ConditionalETagInterceptor.extractResourceName("/api/me/settings"));
  }

  @Test
  void testBuildMetadataEndpointTypesUsersIncludesTrackedReferencesAndAuthDependencies() {
    Map<String, Set<Class<?>>> metadataEndpointTypes =
        ConditionalETagInterceptor.buildMetadataEndpointTypes(
            List.of(
                metadataSchema(
                    "users",
                    User.class,
                    Set.of(
                        UserRole.class,
                        UserGroup.class,
                        OrganisationUnit.class,
                        Category.class,
                        CategoryOptionGroupSet.class,
                        FileResource.class),
                    true,
                    false,
                    false)));

    assertEquals(
        Set.of(
            User.class,
            UserRole.class,
            UserGroup.class,
            OrganisationUnit.class,
            Category.class,
            CategoryOptionGroupSet.class,
            FileResource.class),
        ConditionalETagInterceptor.resolveMetadataEndpointTypes("users", metadataEndpointTypes));
  }

  @Test
  void testBuildMetadataEndpointTypesShareableSchemaAddsUserGroup() {
    Map<String, Set<Class<?>>> metadataEndpointTypes =
        ConditionalETagInterceptor.buildMetadataEndpointTypes(
            List.of(
                metadataSchema(
                    "organisationUnits", OrganisationUnit.class, Set.of(), true, false, false)));

    assertEquals(
        Set.of(OrganisationUnit.class, User.class, UserRole.class, UserGroup.class),
        ConditionalETagInterceptor.resolveMetadataEndpointTypes(
            "organisationUnits", metadataEndpointTypes));
  }

  @Test
  void testBuildMetadataEndpointTypesNonShareableSchemaSkipsUserGroup() {
    Map<String, Set<Class<?>>> metadataEndpointTypes =
        ConditionalETagInterceptor.buildMetadataEndpointTypes(
            List.of(
                metadataSchema(
                    "organisationUnits", OrganisationUnit.class, Set.of(), false, false, false)));

    assertEquals(
        Set.of(OrganisationUnit.class, User.class, UserRole.class),
        ConditionalETagInterceptor.resolveMetadataEndpointTypes(
            "organisationUnits", metadataEndpointTypes));
  }

  @Test
  void testBuildMetadataEndpointTypesAttributeValuesAddsAttribute() {
    Map<String, Set<Class<?>>> metadataEndpointTypes =
        ConditionalETagInterceptor.buildMetadataEndpointTypes(
            List.of(
                metadataSchema("programs", OrganisationUnit.class, Set.of(), true, false, true)));

    assertEquals(
        Set.of(
            OrganisationUnit.class, User.class, UserRole.class, UserGroup.class, Attribute.class),
        ConditionalETagInterceptor.resolveMetadataEndpointTypes("programs", metadataEndpointTypes));
  }

  @Test
  void testBuildMetadataEndpointTypesMapsAndVisualizationsApplyOverrides() {
    Map<String, Set<Class<?>>> metadataEndpointTypes =
        ConditionalETagInterceptor.buildMetadataEndpointTypes(
            List.of(
                metadataSchema("maps", User.class, Set.of(), true, false, false),
                metadataSchema("visualizations", User.class, Set.of(), true, false, false)));

    assertEquals(
        Set.of(
            User.class, UserRole.class, UserGroup.class, OrganisationUnit.class, Attribute.class),
        ConditionalETagInterceptor.resolveMetadataEndpointTypes("maps", metadataEndpointTypes));
    assertEquals(
        Set.of(User.class, UserRole.class, UserGroup.class, OrganisationUnit.class),
        ConditionalETagInterceptor.resolveMetadataEndpointTypes(
            "visualizations", metadataEndpointTypes));
  }

  @Test
  void testResolveCompositeEndpointTypesMatchesConfiguredRoutes() {
    assertCompositeMatch("systemSettings", "/api/systemSettings");
    assertCompositeMatch("systemSettings/*", "/api/41/systemSettings/applicationTitle?fields=id");
    assertCompositeMatch("userSettings", "/api/userSettings");
    assertCompositeMatch("userSettings/*", "/api/userSettings/keyStyle?userId=abc123");
    assertCompositeMatch("userDataStore/**", "/api/userDataStore/namespace/key");
    assertCompositeMatch("me/settings", "/api/me/settings?paging=false");
    assertCompositeMatch("me/settings/*", "/api/41/me/settings/keyStyle");
    assertCompositeMatch("me/authorization", "/api/me/authorization");
    assertCompositeMatch("me/authorization/*", "/api/me/authorization/F_SYSTEM_SETTING");
    assertCompositeMatch("me/authorities", "/api/me/authorities");
    assertCompositeMatch("me/authorities/*", "/api/me/authorities/F_SYSTEM_SETTING");
    assertCompositeMatch("me/dataApprovalLevels", "/api/me/dataApprovalLevels");
    assertCompositeMatch("me/dataApprovalWorkflows", "/api/me/dataApprovalWorkflows");
    assertCompositeMatch("dimensions", "/api/dimensions");
    assertCompositeMatch("dimensions/constraints", "/api/dimensions/constraints");
    assertCompositeMatch("dimensions/dataSet/*", "/api/dimensions/dataSet/abc123");
    assertCompositeMatch("dataStatistics", "/api/dataStatistics");
    assertCompositeMatch("dataStatistics/favorites/*", "/api/dataStatistics/favorites/abc123");
    assertCompositeMatch("loginConfig", "/api/loginConfig");
  }

  @Test
  void testResolveCompositeEndpointTypesSkippedRoutesReturnEmpty() {
    // /api/me is now a composite endpoint — moved to testResolveCompositeEndpointTypes_me
    assertTrue(
        ConditionalETagInterceptor.resolveCompositeEndpointTypes("/api/me/dashboard").isEmpty());
    assertTrue(
        ConditionalETagInterceptor.resolveCompositeEndpointTypes("/api/system/info").isEmpty());
    assertTrue(
        ConditionalETagInterceptor.resolveCompositeEndpointTypes("/api/system/uid").isEmpty());
    assertTrue(
        ConditionalETagInterceptor.resolveCompositeEndpointTypes("/api/dashboards/search")
            .isEmpty());
    assertTrue(
        ConditionalETagInterceptor.resolveCompositeEndpointTypes("/api/dimensions/recommendations")
            .isEmpty());
    assertTrue(
        ConditionalETagInterceptor.resolveCompositeEndpointTypes("/api/dataStatistics/favorites")
            .isEmpty());
    assertTrue(
        ConditionalETagInterceptor.resolveCompositeEndpointTypes("/api/messageConversations")
            .isEmpty());
  }

  @Test
  void testResolveCompositeEndpointTypesExactRouteDoesNotMatchSubPath() {
    assertTrue(
        ConditionalETagInterceptor.resolveCompositeEndpointTypes("/api/me/dataApprovalLevels/extra")
            .isEmpty());
    assertTrue(
        ConditionalETagInterceptor.resolveCompositeEndpointTypes("/api/loginConfig/details")
            .isEmpty());
  }

  @Test
  void testResolveCompositeEndpointTypesSingleSegmentWildcardMatchesSingleSegmentOnly() {
    Set<Class<?>> wildcardTypes = Set.of(Configuration.class);
    Map<String, Set<Class<?>>> compositeEndpoints = Map.of("system/*", wildcardTypes);

    assertEquals(
        wildcardTypes,
        ConditionalETagInterceptor.resolveCompositeEndpointTypes(
            "/api/system/info?fields=id", compositeEndpoints));
    assertTrue(
        ConditionalETagInterceptor.resolveCompositeEndpointTypes(
                "/api/system/tasks/foo", compositeEndpoints)
            .isEmpty());
  }

  @Test
  void testResolveCompositeEndpointTypesDoubleWildcardMatchesNestedSegments() {
    Set<Class<?>> wildcardTypes = Set.of(Configuration.class);

    assertEquals(
        wildcardTypes,
        ConditionalETagInterceptor.resolveCompositeEndpointTypes(
            "/api/system/tasks/foo", Map.of("system/**", wildcardTypes)));
  }

  @Test
  void testResolveCompositeEndpointTypesExactMatchWinsOverWildcard() {
    Set<Class<?>> exactTypes = Set.of(Configuration.class);
    Set<Class<?>> wildcardTypes = Set.of(User.class);
    Map<String, Set<Class<?>>> compositeEndpoints =
        Map.of("system/info", exactTypes, "system/*", wildcardTypes);

    assertEquals(
        exactTypes,
        ConditionalETagInterceptor.resolveCompositeEndpointTypes(
            "/api/system/info", compositeEndpoints));
  }

  @Test
  void testExtractResourceNameNull() {
    assertNull(ConditionalETagInterceptor.extractResourceName(null));
  }

  @Test
  void testExtractResourceNameNoMatch() {
    assertNull(ConditionalETagInterceptor.extractResourceName("/something"));
  }

  @Test
  void testSkipsNonGetRequests() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/organisationUnits");
    request.setRequestURI("/api/organisationUnits");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verifyNoInteractions(schemaService);
  }

  @Test
  void testSkipsWhenDisabled() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/organisationUnits");
    request.setRequestURI("/api/organisationUnits");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(false);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verifyNoInteractions(schemaService);
  }

  @Test
  void testSkipsNonMetadataEndpoint() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dataValues");
    request.setRequestURI("/api/dataValues");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);
    when(schemaService.getMetadataSchemas()).thenReturn(List.of());

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class));
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class), any(Class.class));
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class), any(Set.class));
  }

  @Test
  void testSkipsNonSchemaEndpoint() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
    request.setRequestURI("/api/ping");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);
    when(schemaService.getMetadataSchemas()).thenReturn(List.of());

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class));
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class), any(Class.class));
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class), any(Set.class));
  }

  @Test
  void testSkipsExplicitlyUncachedMetadataSubPath() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboards/search");
    request.setRequestURI("/api/dashboards/search");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class), any(Class.class));
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class), any(Set.class));
  }

  @Test
  void testReturns304ForMatchingMetadataETag() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
    request.setRequestURI("/api/users");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    List<Schema> metadataSchemas =
        List.of(
            metadataSchema(
                "users",
                User.class,
                Set.of(UserRole.class, UserGroup.class, OrganisationUnit.class),
                true,
                false,
                false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);

    Set<Class<?>> expectedTypes =
        ConditionalETagInterceptor.resolveMetadataEndpointTypes(
            "users", ConditionalETagInterceptor.buildMetadataEndpointTypes(metadataSchemas));
    String etag = "userUid123-c-42-7";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertFalse(result);
    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    verify(conditionalETagService).setETagHeaders(response, etag);
  }

  @Test
  void testStoresMetadataETagOnSuccessForUsers() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
    request.setRequestURI("/api/users");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    List<Schema> metadataSchemas =
        List.of(
            metadataSchema(
                "users",
                User.class,
                Set.of(UserRole.class, UserGroup.class, OrganisationUnit.class),
                true,
                false,
                false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);

    Set<Class<?>> expectedTypes =
        ConditionalETagInterceptor.resolveMetadataEndpointTypes(
            "users", ConditionalETagInterceptor.buildMetadataEndpointTypes(metadataSchemas));
    String etag = "userUid123-c-42-7";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    assertEquals(etag, ConditionalETagInterceptor.getStoredETag(request));
    verify(conditionalETagService, never())
        .setETagHeaders(any(HttpServletResponse.class), anyString());
  }

  @Test
  void testCompositeEndpointReturns304() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me/settings");
    request.setRequestURI("/api/me/settings");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes =
        ConditionalETagInterceptor.getCompositeEndpointTypes("me/settings");
    String etag = "userUid123-c-100-42";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertFalse(result);
    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    verify(conditionalETagService).setETagHeaders(response, etag);
    verifyNoInteractions(schemaService);
  }

  @Test
  void testCompositeEndpointStoresETagOnSuccess() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me/authorization");
    request.setRequestURI("/api/me/authorization");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes =
        ConditionalETagInterceptor.getCompositeEndpointTypes("me/authorization");
    String etag = "userUid123-c-100-42";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    assertEquals(etag, ConditionalETagInterceptor.getStoredETag(request));
    verify(conditionalETagService, never())
        .setETagHeaders(any(HttpServletResponse.class), anyString());
    verifyNoInteractions(schemaService);
  }

  @Test
  void testCompositeSlashEndpointWithContextPathStoresETagOnSuccess() {
    setUpSecurityContext();

    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/server1/api/systemSettings/applicationTitle");
    request.setContextPath("/server1");
    request.setRequestURI("/server1/api/systemSettings/applicationTitle");
    request.setQueryString("fields=id");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes =
        ConditionalETagInterceptor.getCompositeEndpointTypes("systemSettings/*");
    String etag = "userUid123-c-200-84";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(any(), anyString())).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    String storedETag = ConditionalETagInterceptor.getStoredETag(request);
    assertNotEquals(
        etag, storedETag, "Stored ETag should differ from base when query string is present");
    verifyNoInteractions(schemaService);
  }

  @Test
  void testCompositeDoubleWildcardEndpointStoresETagOnSuccess() {
    setUpSecurityContext();

    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/userDataStore/namespace/key");
    request.setRequestURI("/api/userDataStore/namespace/key");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes =
        ConditionalETagInterceptor.getCompositeEndpointTypes("userDataStore/**");
    assertEquals(Set.of(UserDatastoreEntry.class), expectedTypes);
    String etag = "userUid123-UserDatastoreEntry-300-21";
    when(conditionalETagService.generateETag(userDetails, UserDatastoreEntry.class))
        .thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    assertEquals(etag, ConditionalETagInterceptor.getStoredETag(request));
    verifyNoInteractions(schemaService);
  }

  @Test
  void testMetadataEndpointWithContextPathStoresETagOnSuccess() {
    setUpSecurityContext();

    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/server1/api/41/organisationUnits");
    request.setContextPath("/server1");
    request.setRequestURI("/server1/api/41/organisationUnits");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    List<Schema> metadataSchemas =
        List.of(
            metadataSchema(
                "organisationUnits", OrganisationUnit.class, Set.of(), false, false, false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);

    Set<Class<?>> expectedTypes =
        ConditionalETagInterceptor.resolveMetadataEndpointTypes(
            "organisationUnits",
            ConditionalETagInterceptor.buildMetadataEndpointTypes(metadataSchemas));
    String etag = "userUid123-c-42-7";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    assertEquals(etag, ConditionalETagInterceptor.getStoredETag(request));
  }

  @Test
  void testMetadataEndpointWithQueryParametersStillUsesResourceSegment() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/41/organisationUnits");
    request.setRequestURI("/api/41/organisationUnits");
    request.setQueryString("fields=id");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    List<Schema> metadataSchemas =
        List.of(
            metadataSchema(
                "organisationUnits", OrganisationUnit.class, Set.of(), false, false, false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);

    Set<Class<?>> expectedTypes =
        ConditionalETagInterceptor.resolveMetadataEndpointTypes(
            "organisationUnits",
            ConditionalETagInterceptor.buildMetadataEndpointTypes(metadataSchemas));
    String etag = "userUid123-c-42-7";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(any(), anyString())).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    String storedETag = ConditionalETagInterceptor.getStoredETag(request);
    assertNotEquals(
        etag, storedETag, "Stored ETag should differ from base when query string is present");
  }

  @Test
  void testDifferentQueryParametersProduceDifferentETags() {
    setUpSecurityContext();

    List<Schema> metadataSchemas =
        List.of(
            metadataSchema(
                "organisationUnits", OrganisationUnit.class, Set.of(), false, false, false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);
    when(conditionalETagService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes =
        ConditionalETagInterceptor.resolveMetadataEndpointTypes(
            "organisationUnits",
            ConditionalETagInterceptor.buildMetadataEndpointTypes(metadataSchemas));
    String baseEtag = "userUid123-c-42-7";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(baseEtag);
    when(conditionalETagService.checkNotModified(any(), anyString())).thenReturn(false);

    MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/api/organisationUnits");
    request1.setRequestURI("/api/organisationUnits");
    request1.setQueryString("fields=id");
    interceptor.preHandle(request1, new MockHttpServletResponse(), new Object());

    MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/api/organisationUnits");
    request2.setRequestURI("/api/organisationUnits");
    request2.setQueryString("fields=id,name,children");
    interceptor.preHandle(request2, new MockHttpServletResponse(), new Object());

    MockHttpServletRequest request3 = new MockHttpServletRequest("GET", "/api/organisationUnits");
    request3.setRequestURI("/api/organisationUnits");
    interceptor.preHandle(request3, new MockHttpServletResponse(), new Object());

    String etag1 = ConditionalETagInterceptor.getStoredETag(request1);
    String etag2 = ConditionalETagInterceptor.getStoredETag(request2);
    String etag3 = ConditionalETagInterceptor.getStoredETag(request3);

    // Different query strings must produce different ETags
    assertNotEquals(etag1, etag2, "Different query params should produce different ETags");
    // No query string should produce the base ETag unchanged
    assertEquals(baseEtag, etag3, "No query string should produce the base ETag");
    // Query-string ETags should differ from the base (full re-hash, no prefix relationship)
    assertNotEquals(baseEtag, etag1, "Query-string ETag should differ from base");
    assertNotEquals(baseEtag, etag2, "Query-string ETag should differ from base");
  }

  @Test
  void testMetadataEndpointWithOverrideUsesVisualizationsDependencySet() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/visualizations");
    request.setRequestURI("/api/visualizations");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    List<Schema> metadataSchemas =
        List.of(metadataSchema("visualizations", User.class, Set.of(), true, false, false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);

    Set<Class<?>> expectedTypes =
        ConditionalETagInterceptor.resolveMetadataEndpointTypes(
            "visualizations",
            ConditionalETagInterceptor.buildMetadataEndpointTypes(metadataSchemas));
    String etag = "userUid123-c-99-9";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    assertEquals(etag, ConditionalETagInterceptor.getStoredETag(request));
    assertTrue(expectedTypes.contains(OrganisationUnit.class));
  }

  private void assertCompositeMatch(String pattern, String uri) {
    assertEquals(
        ConditionalETagInterceptor.getCompositeEndpointTypes(pattern),
        ConditionalETagInterceptor.resolveCompositeEndpointTypes(uri));
  }

  @SuppressWarnings("unchecked")
  private Schema metadataSchema(
      String plural,
      Class<?> klass,
      Set<Class<?>> references,
      boolean shareable,
      boolean dataShareable,
      boolean hasAttributeValues) {
    Schema schema = mock(Schema.class);
    when(schema.getPlural()).thenReturn(plural);
    when(schema.getKlass()).thenReturn((Class) klass);
    when(schema.getReferences()).thenReturn((Set) references);
    when(schema.isShareable()).thenReturn(shareable);
    lenient().when(schema.isDataShareable()).thenReturn(dataShareable);
    when(schema.hasAttributeValues()).thenReturn(hasAttributeValues);
    return schema;
  }

  private void setUpSecurityContext() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(userDetails, null, java.util.List.of()));
  }

  // ========== Named-Key Endpoint Tests ==========

  @Test
  void testNamedEndpointSchemas() {
    ConditionalETagInterceptor.NamedEndpointDeps deps =
        ConditionalETagInterceptor.getNamedEndpointDeps("schemas");
    assertNotNull(deps);
    assertTrue(deps.entityTypes().isEmpty(), "schemas should have no entity type dependencies");
    assertTrue(deps.namedKeys().isEmpty(), "schemas should have no named key dependencies");
  }

  @Test
  void testNamedEndpointAppsMenu() {
    ConditionalETagInterceptor.NamedEndpointDeps deps =
        ConditionalETagInterceptor.getNamedEndpointDeps("apps/menu");
    assertNotNull(deps);
    assertTrue(deps.entityTypes().contains(User.class));
    assertTrue(deps.entityTypes().contains(UserRole.class));
    assertTrue(deps.namedKeys().contains("installedApps"));
  }
}
