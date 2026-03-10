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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.UserDetails;
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
    interceptor = new ConditionalETagInterceptor(conditionalETagService, schemaService);

    userDetails = mock(UserDetails.class);
    lenient().when(userDetails.getUid()).thenReturn("userUid123");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // --- extractResourceName tests (static, no mocking needed) ---

  @Test
  void testExtractResourceName_standard() {
    assertEquals(
        "organisationUnits",
        ConditionalETagInterceptor.extractResourceName("/api/organisationUnits"));
  }

  @Test
  void testExtractResourceName_withVersion() {
    assertEquals(
        "organisationUnits",
        ConditionalETagInterceptor.extractResourceName("/api/41/organisationUnits"));
  }

  @Test
  void testExtractResourceName_withIdSuffix() {
    assertEquals(
        "organisationUnits",
        ConditionalETagInterceptor.extractResourceName("/api/41/organisationUnits/abc1234567"));
  }

  @Test
  void testExtractResourceName_me() {
    assertEquals("me", ConditionalETagInterceptor.extractResourceName("/api/me"));
  }

  @Test
  void testExtractCompositeEndpointName_me() {
    assertEquals("me", ConditionalETagInterceptor.extractCompositeEndpointName("/api/me"));
  }

  @Test
  void testExtractCompositeEndpointName_meWithVersion() {
    assertEquals("me", ConditionalETagInterceptor.extractCompositeEndpointName("/api/41/me"));
  }

  @Test
  void testExtractCompositeEndpointName_subPathDoesNotMatch() {
    assertNull(ConditionalETagInterceptor.extractCompositeEndpointName("/api/me/settings"));
  }

  @Test
  void testExtractCompositeEndpointName_configurationSubPathDoesNotMatch() {
    assertNull(
        ConditionalETagInterceptor.extractCompositeEndpointName("/api/configuration/systemId"));
  }

  @Test
  void testExtractResourceName_null() {
    assertNull(ConditionalETagInterceptor.extractResourceName(null));
  }

  @Test
  void testExtractResourceName_noMatch() {
    assertNull(ConditionalETagInterceptor.extractResourceName("/something"));
  }

  // --- preHandle tests ---

  @Test
  void testSkipsNonGetRequests() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/organisationUnits");
    request.setRequestURI("/api/organisationUnits");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verifyNoInteractions(schemaService);
  }

  @Test
  void testSkipsWhenDisabled() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/organisationUnits");
    request.setRequestURI("/api/organisationUnits");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(false);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verifyNoInteractions(schemaService);
  }

  @Test
  void testSkipsNonMetadataEndpoint() throws Exception {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dataValues");
    request.setRequestURI("/api/dataValues");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    Schema schema = mock(Schema.class);
    when(schema.isMetadata()).thenReturn(false);
    when(schemaService.getSchemaByPluralName("dataValues")).thenReturn(schema);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class));
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class), any(Class.class));
  }

  @Test
  void testSkipsNonSchemaEndpoint() throws Exception {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
    request.setRequestURI("/api/ping");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);
    when(schemaService.getSchemaByPluralName("ping")).thenReturn(null);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class));
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class), any(Class.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testReturns304ForMatchingETag() throws Exception {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/41/organisationUnits");
    request.setRequestURI("/api/41/organisationUnits");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    Schema schema = mock(Schema.class);
    when(schema.isMetadata()).thenReturn(true);
    when(schema.getKlass()).thenReturn((Class) OrganisationUnit.class);
    when(schemaService.getSchemaByPluralName("organisationUnits")).thenReturn(schema);

    String etag = "userUid123-OrganisationUnit-42-7";
    when(conditionalETagService.generateETag(userDetails, OrganisationUnit.class)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertFalse(result);
    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    verify(conditionalETagService).setETagHeaders(response, etag);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testStoresETagOnSuccess() throws Exception {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/41/organisationUnits");
    request.setRequestURI("/api/41/organisationUnits");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    Schema schema = mock(Schema.class);
    when(schema.isMetadata()).thenReturn(true);
    when(schema.getKlass()).thenReturn((Class) OrganisationUnit.class);
    when(schemaService.getSchemaByPluralName("organisationUnits")).thenReturn(schema);

    String etag = "userUid123-OrganisationUnit-42-7";
    when(conditionalETagService.generateETag(userDetails, OrganisationUnit.class)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(false);

    // preHandle stores etag in request attribute
    boolean preResult = interceptor.preHandle(request, response, new Object());
    assertTrue(preResult);
    assertEquals(etag, ConditionalETagInterceptor.getStoredETag(request));
    verify(conditionalETagService, never()).setETagHeaders(any(HttpServletResponse.class), anyString());
  }

  // --- composite endpoint tests ---

  @Test
  void testCompositeEndpointReturns304() throws Exception {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
    request.setRequestURI("/api/me");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes = ConditionalETagInterceptor.getCompositeEndpointTypes("me");
    String etag = "userUid123-c-100-42";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertFalse(result);
    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    verify(conditionalETagService).setETagHeaders(response, etag);
  }

  @Test
  void testCompositeEndpointStoresETagOnSuccess() throws Exception {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
    request.setRequestURI("/api/me");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);

    Set<Class<?>> expectedTypes = ConditionalETagInterceptor.getCompositeEndpointTypes("me");
    String etag = "userUid123-c-100-42";
    when(conditionalETagService.generateETag(userDetails, expectedTypes)).thenReturn(etag);
    when(conditionalETagService.checkNotModified(request, etag)).thenReturn(false);

    boolean preResult = interceptor.preHandle(request, response, new Object());
    assertTrue(preResult);
    assertEquals(etag, ConditionalETagInterceptor.getStoredETag(request));
    verify(conditionalETagService, never()).setETagHeaders(any(HttpServletResponse.class), anyString());
  }

  @Test
  void testCompositeEndpointDoesNotMatchSubPath() throws Exception {
    setUpSecurityContext();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me/settings");
    request.setRequestURI("/api/me/settings");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);
    when(schemaService.getSchemaByPluralName("me")).thenReturn(null);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    assertNull(ConditionalETagInterceptor.getStoredETag(request));
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class), any(Set.class));
  }

  @Test
  void testConfigurationSubPathDoesNotMatchCompositeEndpoint() throws Exception {
    setUpSecurityContext();

    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/configuration/systemId");
    request.setRequestURI("/api/configuration/systemId");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(conditionalETagService.isEnabled()).thenReturn(true);
    when(schemaService.getSchemaByPluralName("configuration")).thenReturn(null);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    assertNull(ConditionalETagInterceptor.getStoredETag(request));
    verify(conditionalETagService, never()).generateETag(any(UserDetails.class), any(Set.class));
  }

  private void setUpSecurityContext() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(userDetails, null, java.util.List.of()));
  }
}
