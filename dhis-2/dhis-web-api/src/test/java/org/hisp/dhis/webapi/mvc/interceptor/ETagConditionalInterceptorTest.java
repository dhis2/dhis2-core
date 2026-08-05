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
import static org.mockito.ArgumentMatchers.eq;
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
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.userdatastore.UserDatastoreEntry;
import org.hisp.dhis.webapi.etag.ETagFieldsHopAnalyzer;
import org.hisp.dhis.webapi.service.ETagConditionalService;
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
 * Unit tests for {@link ETagConditionalInterceptor}.
 *
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class ETagConditionalInterceptorTest {

  @Mock private ETagConditionalService eTagConditionalService;
  @Mock private SchemaService schemaService;

  private ETagConditionalInterceptor interceptor;

  private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    interceptor =
        new ETagConditionalInterceptor(
            eTagConditionalService,
            schemaService,
            new ETagFieldsHopAnalyzer(schemaService),
            null,
            null);

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
        ETagConditionalInterceptor.extractApiRelativePath("/api/organisationUnits"));
  }

  @Test
  void testExtractApiRelativePathWithVersion() {
    assertEquals(
        "organisationUnits/abc1234567",
        ETagConditionalInterceptor.extractApiRelativePath("/api/41/organisationUnits/abc1234567"));
  }

  @Test
  void testExtractApiRelativePathIgnoresQueryParameters() {
    assertEquals(
        "systemSettings/applicationTitle",
        ETagConditionalInterceptor.extractApiRelativePath(
            "/api/systemSettings/applicationTitle?fields=id"));
  }

  @Test
  void testExtractApiRelativePathTrimsTrailingSlash() {
    assertEquals(
        "systemSettings/applicationTitle",
        ETagConditionalInterceptor.extractApiRelativePath("/api/systemSettings/applicationTitle/"));
  }

  @Test
  void testExtractResourceNameStandard() {
    assertEquals(
        "organisationUnits",
        ETagConditionalInterceptor.extractResourceName("/api/organisationUnits"));
  }

  @Test
  void testExtractResourceNameWithVersion() {
    assertEquals(
        "organisationUnits",
        ETagConditionalInterceptor.extractResourceName("/api/41/organisationUnits"));
  }

  @Test
  void testExtractResourceNameWithIdSuffix() {
    assertEquals(
        "organisationUnits",
        ETagConditionalInterceptor.extractResourceName("/api/41/organisationUnits/abc1234567"));
  }

  @Test
  void testExtractResourceNameMeSettings() {
    assertEquals("me", ETagConditionalInterceptor.extractResourceName("/api/me/settings"));
  }

  @Test
  void testBuildMetadataEndpointTypesUsersIncludesTrackedReferencesAndAuthDependencies() {
    Map<String, Set<Class<?>>> metadataEndpointTypes =
        ETagConditionalInterceptor.buildMetadataEndpointTypes(
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
        ETagConditionalInterceptor.resolveMetadataEndpointTypes("users", metadataEndpointTypes));
  }

  @Test
  void testBuildMetadataEndpointTypesShareableSchemaAddsUserGroup() {
    Map<String, Set<Class<?>>> metadataEndpointTypes =
        ETagConditionalInterceptor.buildMetadataEndpointTypes(
            List.of(
                metadataSchema(
                    "organisationUnits", OrganisationUnit.class, Set.of(), true, false, false)));

    assertEquals(
        Set.of(OrganisationUnit.class, UserGroup.class),
        ETagConditionalInterceptor.resolveMetadataEndpointTypes(
            "organisationUnits", metadataEndpointTypes));
  }

  @Test
  void testBuildMetadataEndpointTypesNonShareableSchemaSkipsUserGroup() {
    Map<String, Set<Class<?>>> metadataEndpointTypes =
        ETagConditionalInterceptor.buildMetadataEndpointTypes(
            List.of(
                metadataSchema(
                    "organisationUnits", OrganisationUnit.class, Set.of(), false, false, false)));

    assertEquals(
        Set.of(OrganisationUnit.class),
        ETagConditionalInterceptor.resolveMetadataEndpointTypes(
            "organisationUnits", metadataEndpointTypes));
  }

  @Test
  void testBuildMetadataEndpointTypesAttributeValuesAddsAttribute() {
    Map<String, Set<Class<?>>> metadataEndpointTypes =
        ETagConditionalInterceptor.buildMetadataEndpointTypes(
            List.of(
                metadataSchema("programs", OrganisationUnit.class, Set.of(), true, false, true)));

    assertEquals(
        Set.of(OrganisationUnit.class, UserGroup.class, Attribute.class),
        ETagConditionalInterceptor.resolveMetadataEndpointTypes("programs", metadataEndpointTypes));
  }

  @Test
  void testBuildMetadataEndpointTypesMapsAndVisualizationsApplyOverrides() {
    Map<String, Set<Class<?>>> metadataEndpointTypes =
        ETagConditionalInterceptor.buildMetadataEndpointTypes(
            List.of(
                metadataSchema("maps", User.class, Set.of(), true, false, false),
                metadataSchema("visualizations", User.class, Set.of(), true, false, false)));

    assertEquals(
        Set.of(User.class, UserGroup.class, OrganisationUnit.class, Attribute.class),
        ETagConditionalInterceptor.resolveMetadataEndpointTypes("maps", metadataEndpointTypes));
    assertEquals(
        Set.of(User.class, UserGroup.class, OrganisationUnit.class),
        ETagConditionalInterceptor.resolveMetadataEndpointTypes(
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
  void testResolveCompositeEndpointTypes_me() {
    // /api/me embeds the user's own profile, roles, groups, settings and org units, so it must
    // invalidate on User, UserRole, UserGroup, UserSetting and OrganisationUnit DML.
    Set<Class<?>> expectedTypes =
        Set.of(
            User.class, UserRole.class, UserGroup.class, UserSetting.class, OrganisationUnit.class);
    assertEquals(expectedTypes, ETagConditionalInterceptor.getCompositeEndpointTypes("me"));
    assertEquals(
        expectedTypes, ETagConditionalInterceptor.resolveCompositeEndpointTypes("/api/me"));
  }

  @Test
  void testResolveCompositeEndpointTypesSkippedRoutesReturnEmpty() {
    // /api/me is now a composite endpoint — moved to testResolveCompositeEndpointTypes_me
    assertTrue(
        ETagConditionalInterceptor.resolveCompositeEndpointTypes("/api/me/dashboard").isEmpty());
    assertTrue(
        ETagConditionalInterceptor.resolveCompositeEndpointTypes("/api/system/info").isEmpty());
    assertTrue(
        ETagConditionalInterceptor.resolveCompositeEndpointTypes("/api/system/uid").isEmpty());
    assertTrue(
        ETagConditionalInterceptor.resolveCompositeEndpointTypes("/api/dashboards/search")
            .isEmpty());
    assertTrue(
        ETagConditionalInterceptor.resolveCompositeEndpointTypes("/api/dimensions/recommendations")
            .isEmpty());
    assertTrue(
        ETagConditionalInterceptor.resolveCompositeEndpointTypes("/api/dataStatistics/favorites")
            .isEmpty());
    assertTrue(
        ETagConditionalInterceptor.resolveCompositeEndpointTypes("/api/messageConversations")
            .isEmpty());
  }

  @Test
  void testResolveCompositeEndpointTypesExactRouteDoesNotMatchSubPath() {
    assertTrue(
        ETagConditionalInterceptor.resolveCompositeEndpointTypes("/api/me/dataApprovalLevels/extra")
            .isEmpty());
    assertTrue(
        ETagConditionalInterceptor.resolveCompositeEndpointTypes("/api/loginConfig/details")
            .isEmpty());
  }

  @Test
  void testResolveCompositeEndpointTypesSingleSegmentWildcardMatchesSingleSegmentOnly() {
    Set<Class<?>> wildcardTypes = Set.of(Configuration.class);
    Map<String, Set<Class<?>>> compositeEndpoints = Map.of("system/*", wildcardTypes);

    assertEquals(
        wildcardTypes,
        ETagConditionalInterceptor.resolveCompositeEndpointTypes(
            "/api/system/info?fields=id", compositeEndpoints));
    assertTrue(
        ETagConditionalInterceptor.resolveCompositeEndpointTypes(
                "/api/system/tasks/foo", compositeEndpoints)
            .isEmpty());
  }

  @Test
  void testResolveCompositeEndpointTypesDoubleWildcardMatchesNestedSegments() {
    Set<Class<?>> wildcardTypes = Set.of(Configuration.class);

    assertEquals(
        wildcardTypes,
        ETagConditionalInterceptor.resolveCompositeEndpointTypes(
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
        ETagConditionalInterceptor.resolveCompositeEndpointTypes(
            "/api/system/info", compositeEndpoints));
  }

  @Test
  void testExtractResourceNameNull() {
    assertNull(ETagConditionalInterceptor.extractResourceName(null));
  }

  @Test
  void testExtractResourceNameNoMatch() {
    assertNull(ETagConditionalInterceptor.extractResourceName("/something"));
  }

  @Test
  void testSkipsNonGetRequests() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/organisationUnits");
    request.setRequestURI("/api/organisationUnits");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verifyNoInteractions(schemaService);
  }

  @Test
  void testSkipsWhenDisabled() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/organisationUnits");
    request.setRequestURI("/api/organisationUnits");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(false);

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

    when(eTagConditionalService.isEnabled()).thenReturn(true);
    when(schemaService.getMetadataSchemas()).thenReturn(List.of());

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(eTagConditionalService, never()).generateETag(any(UserDetails.class), any(Class.class));
    verify(eTagConditionalService, never()).generateETag(any(UserDetails.class), any(Set.class));
  }

  @Test
  void testSkipsNonSchemaEndpoint() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
    request.setRequestURI("/api/ping");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);
    when(schemaService.getMetadataSchemas()).thenReturn(List.of());

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(eTagConditionalService, never()).generateETag(any(UserDetails.class), any(Class.class));
    verify(eTagConditionalService, never()).generateETag(any(UserDetails.class), any(Set.class));
  }

  @Test
  void testSkipsExplicitlyUncachedMetadataSubPath() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboards/search");
    request.setRequestURI("/api/dashboards/search");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(eTagConditionalService, never()).generateETag(any(UserDetails.class), any(Class.class));
    verify(eTagConditionalService, never()).generateETag(any(UserDetails.class), any(Set.class));
  }

  @Test
  void testReturns304ForMatchingMetadataETag() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
    request.setRequestURI("/api/users");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);

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
        ETagConditionalInterceptor.resolveMetadataEndpointTypes(
            "users", ETagConditionalInterceptor.buildMetadataEndpointTypes(metadataSchemas));
    String etag = "userUid123-c-42-7";
    when(eTagConditionalService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(eTagConditionalService.checkNotModified(request, etag)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertFalse(result);
    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    verify(eTagConditionalService).setETagHeaders(response, etag);
  }

  @Test
  void testStoresMetadataETagOnSuccessForUsers() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
    request.setRequestURI("/api/users");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);

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
        ETagConditionalInterceptor.resolveMetadataEndpointTypes(
            "users", ETagConditionalInterceptor.buildMetadataEndpointTypes(metadataSchemas));
    String etag = "userUid123-c-42-7";
    when(eTagConditionalService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(eTagConditionalService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    assertEquals(etag, ETagConditionalInterceptor.getStoredETag(request));
    verify(eTagConditionalService, never())
        .setETagHeaders(any(HttpServletResponse.class), anyString());
  }

  @Test
  void testCompositeEndpointReturns304() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me/settings");
    request.setRequestURI("/api/me/settings");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes =
        ETagConditionalInterceptor.getCompositeEndpointTypes("me/settings");
    String etag = "userUid123-c-100-42";
    when(eTagConditionalService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(eTagConditionalService.checkNotModified(request, etag)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertFalse(result);
    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    verify(eTagConditionalService).setETagHeaders(response, etag);
    verifyNoInteractions(schemaService);
  }

  @Test
  void testCompositeEndpointStoresETagOnSuccess() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me/authorization");
    request.setRequestURI("/api/me/authorization");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes =
        ETagConditionalInterceptor.getCompositeEndpointTypes("me/authorization");
    String etag = "userUid123-c-100-42";
    when(eTagConditionalService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(eTagConditionalService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    assertEquals(etag, ETagConditionalInterceptor.getStoredETag(request));
    verify(eTagConditionalService, never())
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

    when(eTagConditionalService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes =
        ETagConditionalInterceptor.getCompositeEndpointTypes("systemSettings/*");
    String etag = "userUid123-c-200-84";
    when(eTagConditionalService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(eTagConditionalService.checkNotModified(any(), anyString())).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    String storedETag = ETagConditionalInterceptor.getStoredETag(request);
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

    when(eTagConditionalService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes =
        ETagConditionalInterceptor.getCompositeEndpointTypes("userDataStore/**");
    assertEquals(Set.of(UserDatastoreEntry.class), expectedTypes);
    String etag = "userUid123-UserDatastoreEntry-300-21";
    when(eTagConditionalService.generateETag(userDetails, UserDatastoreEntry.class))
        .thenReturn(etag);
    when(eTagConditionalService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    assertEquals(etag, ETagConditionalInterceptor.getStoredETag(request));
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

    when(eTagConditionalService.isEnabled()).thenReturn(true);

    List<Schema> metadataSchemas =
        List.of(
            metadataSchema(
                "organisationUnits", OrganisationUnit.class, Set.of(), false, false, false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);

    // Single-type set → interceptor uses single-class generateETag overload
    String etag = "userUid123-c-42-7";
    when(eTagConditionalService.generateETag(userDetails, OrganisationUnit.class)).thenReturn(etag);
    when(eTagConditionalService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    assertEquals(etag, ETagConditionalInterceptor.getStoredETag(request));
  }

  @Test
  void testMetadataEndpointWithQueryParametersStillUsesResourceSegment() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/41/organisationUnits");
    request.setRequestURI("/api/41/organisationUnits");
    request.setQueryString("fields=id");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);

    List<Schema> metadataSchemas =
        List.of(
            metadataSchema(
                "organisationUnits", OrganisationUnit.class, Set.of(), false, false, false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);

    // Single-type set → interceptor uses single-class generateETag overload
    String etag = "userUid123-c-42-7";
    when(eTagConditionalService.generateETag(userDetails, OrganisationUnit.class)).thenReturn(etag);
    when(eTagConditionalService.checkNotModified(any(), anyString())).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    String storedETag = ETagConditionalInterceptor.getStoredETag(request);
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
    when(eTagConditionalService.isEnabled()).thenReturn(true);

    // Single-type set → interceptor uses single-class generateETag overload
    String baseEtag = "userUid123-c-42-7";
    when(eTagConditionalService.generateETag(userDetails, OrganisationUnit.class))
        .thenReturn(baseEtag);
    when(eTagConditionalService.checkNotModified(any(), anyString())).thenReturn(false);

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

    String etag1 = ETagConditionalInterceptor.getStoredETag(request1);
    String etag2 = ETagConditionalInterceptor.getStoredETag(request2);
    String etag3 = ETagConditionalInterceptor.getStoredETag(request3);

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

    when(eTagConditionalService.isEnabled()).thenReturn(true);

    List<Schema> metadataSchemas =
        List.of(metadataSchema("visualizations", User.class, Set.of(), true, false, false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);

    Set<Class<?>> expectedTypes =
        ETagConditionalInterceptor.resolveMetadataEndpointTypes(
            "visualizations",
            ETagConditionalInterceptor.buildMetadataEndpointTypes(metadataSchemas));
    String etag = "userUid123-c-99-9";
    when(eTagConditionalService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(eTagConditionalService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());

    assertTrue(preResult);
    assertEquals(etag, ETagConditionalInterceptor.getStoredETag(request));
    assertTrue(expectedTypes.contains(OrganisationUnit.class));
  }

  private void assertCompositeMatch(String pattern, String uri) {
    assertEquals(
        ETagConditionalInterceptor.getCompositeEndpointTypes(pattern),
        ETagConditionalInterceptor.resolveCompositeEndpointTypes(uri));
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

  // ========== Deep-Fields Hop Gate Tests ==========

  @Test
  void testDeepFieldsRequestBypassesETagCaching() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
    request.setRequestURI("/api/users");
    request.addParameter("fields", "userGroups[members[name]]"); // two reference hops
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);
    List<Schema> metadataSchemas =
        List.of(
            metadataSchema(
                "users", User.class, Set.of(UserRole.class, UserGroup.class), true, false, false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);
    stubHopAnalyzerSchemas();

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(eTagConditionalService, never()).generateETag(any(UserDetails.class), any(Class.class));
    verify(eTagConditionalService, never()).generateETag(any(UserDetails.class), any(Set.class));
    assertNull(ETagConditionalInterceptor.getStoredETag(request));
  }

  @Test
  void testShallowFieldsRequestKeepsETagCaching() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
    request.setRequestURI("/api/users");
    request.addParameter("fields", "id,name,userGroups[name]"); // one reference hop
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);
    List<Schema> metadataSchemas =
        List.of(
            metadataSchema(
                "users", User.class, Set.of(UserRole.class, UserGroup.class), true, false, false));
    when(schemaService.getMetadataSchemas()).thenReturn(metadataSchemas);
    stubHopAnalyzerSchemas();

    Set<Class<?>> expectedTypes =
        ETagConditionalInterceptor.resolveMetadataEndpointTypes(
            "users", ETagConditionalInterceptor.buildMetadataEndpointTypes(metadataSchemas));
    String etag = "userUid123-c-42-7";
    when(eTagConditionalService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(eTagConditionalService.checkNotModified(eq(request), anyString())).thenReturn(false);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    assertNotNull(ETagConditionalInterceptor.getStoredETag(request));
  }

  @Test
  void testDeepFieldsOnMeBypassesETagCaching() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
    request.setRequestURI("/api/me");
    request.addParameter("fields", "organisationUnits[dataSets[name]]"); // two reference hops
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);
    stubHopAnalyzerSchemas();

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(eTagConditionalService, never()).generateETag(any(UserDetails.class), any(Class.class));
    verify(eTagConditionalService, never()).generateETag(any(UserDetails.class), any(Set.class));
    assertNull(ETagConditionalInterceptor.getStoredETag(request));
  }

  @Test
  void testShallowFieldsOnMeKeepsETagCaching() {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
    request.setRequestURI("/api/me");
    request.addParameter("fields", "id,name,userGroups[name]"); // one reference hop
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);
    stubHopAnalyzerSchemas();
    when(eTagConditionalService.generateETag(eq(userDetails), any(Set.class)))
        .thenReturn("etag-me");
    when(eTagConditionalService.checkNotModified(eq(request), anyString())).thenReturn(false);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    assertNotNull(ETagConditionalInterceptor.getStoredETag(request));
  }

  @Test
  void testDeepFieldsOnRootlessCompositeStaysCached() {
    setUpSecurityContext();

    // dimensions has no designated fields root: the gate must not engage even for deep-looking
    // fields expressions; the endpoint keeps its hand-curated dependency-set caching
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dimensions");
    request.setRequestURI("/api/dimensions");
    request.addParameter("fields", "items[options[name]]");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(eTagConditionalService.isEnabled()).thenReturn(true);
    when(eTagConditionalService.generateETag(eq(userDetails), any(Set.class)))
        .thenReturn("etag-dimensions");
    when(eTagConditionalService.checkNotModified(eq(request), anyString())).thenReturn(false);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    assertNotNull(ETagConditionalInterceptor.getStoredETag(request));
  }

  /**
   * Real (non-mock) schemas for the hop-analyzer walk: {@code User.userGroups -> UserGroup} and
   * {@code UserGroup.members -> User}, so {@code userGroups[members[name]]} is two hops while
   * {@code userGroups[name]} is one.
   */
  private void stubHopAnalyzerSchemas() {
    Schema user = new Schema(User.class, "user", "users");
    user.addProperty(analyzerScalar("id"));
    user.addProperty(analyzerScalar("name"));
    user.addProperty(analyzerCollectionRef("userGroups", UserGroup.class));

    Schema userGroup = new Schema(UserGroup.class, "userGroup", "userGroups");
    userGroup.addProperty(analyzerScalar("id"));
    userGroup.addProperty(analyzerScalar("name"));
    userGroup.addProperty(analyzerCollectionRef("members", User.class));

    user.addProperty(analyzerCollectionRef("organisationUnits", OrganisationUnit.class));
    Schema organisationUnit =
        new Schema(OrganisationUnit.class, "organisationUnit", "organisationUnits");
    organisationUnit.addProperty(analyzerScalar("id"));
    organisationUnit.addProperty(analyzerScalar("name"));
    organisationUnit.addProperty(
        analyzerCollectionRef("dataSets", org.hisp.dhis.dataset.DataSet.class));

    lenient().when(schemaService.getSchemaByPluralName("users")).thenReturn(user);
    lenient().when(schemaService.getSchema(User.class)).thenReturn(user);
    lenient().when(schemaService.getSchema(UserGroup.class)).thenReturn(userGroup);
    lenient().when(schemaService.getSchema(OrganisationUnit.class)).thenReturn(organisationUnit);
  }

  private static Property analyzerScalar(String name) {
    Property p = new Property(String.class);
    p.setName(name);
    p.setPropertyType(PropertyType.TEXT);
    return p;
  }

  private static Property analyzerCollectionRef(String name, Class<?> itemKlass) {
    Property p = new Property(java.util.List.class);
    p.setName(name);
    p.setCollection(true);
    p.setItemKlass(itemKlass);
    p.setItemPropertyType(PropertyType.REFERENCE);
    p.setPropertyType(PropertyType.COLLECTION);
    return p;
  }

  // ========== Named-Key Endpoint Tests ==========

  @Test
  void testNamedEndpointSchemas() {
    ETagConditionalInterceptor.NamedEndpointDeps deps =
        ETagConditionalInterceptor.getNamedEndpointDeps("schemas");
    assertNotNull(deps);
    assertTrue(deps.entityTypes().isEmpty(), "schemas should have no entity type dependencies");
    assertTrue(deps.namedKeys().isEmpty(), "schemas should have no named key dependencies");
  }

  @Test
  void testNamedEndpointAppsMenu() {
    ETagConditionalInterceptor.NamedEndpointDeps deps =
        ETagConditionalInterceptor.getNamedEndpointDeps("apps/menu");
    assertNotNull(deps);
    assertFalse(deps.entityTypes().contains(User.class));
    assertTrue(deps.entityTypes().contains(UserRole.class));
    assertTrue(deps.namedKeys().contains("installedApps"));
  }
}
