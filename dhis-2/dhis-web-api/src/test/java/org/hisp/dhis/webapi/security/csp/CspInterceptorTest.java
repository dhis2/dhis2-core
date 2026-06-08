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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;

/**
 * Unit tests for {@link CspInterceptor}.
 *
 * <p>These tests cover the annotation→policy-resolution wiring only. The actual header values that
 * land on a real response are exercised by {@code CspHeadersE2ETest}.
 *
 * @author Austin McGee
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class CspInterceptorTest {

  @Mock private CspPolicyService cspPolicyService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @InjectMocks private CspInterceptor cspInterceptor;

  @Test
  void nonHandlerMethod_addsNoHeaders() {
    boolean result = cspInterceptor.preHandle(request, response, "not a handler method");

    assertTrue(result);
    verify(response, never()).setHeader(anyString(), anyString());
  }

  @Test
  void noMarker_leavesBaselineHeadersUntouched() throws Exception {
    boolean result = cspInterceptor.preHandle(request, response, handler(Plain.class, "plain"));

    assertTrue(result);
    verify(cspPolicyService, never()).getSecurityHeaders(anyString());
    verify(cspPolicyService, never()).getDefaultSecurityHeaders();
    verify(response, never()).setHeader(anyString(), anyString());
  }

  @Test
  void methodLevelUserUploadedContent_appliesUserUploadedPolicy() throws Exception {
    when(cspPolicyService.constructUserUploadedContentCspPolicy()).thenReturn("policy");
    when(cspPolicyService.getSecurityHeaders("policy")).thenReturn(headers());

    cspInterceptor.preHandle(request, response, handler(MethodMarked.class, "marked"));

    verify(cspPolicyService, times(1)).constructUserUploadedContentCspPolicy();
    verify(cspPolicyService, never()).constructAppHostCspPolicy();
  }

  @Test
  void classLevelAppHost_appliesAppHostPolicy() throws Exception {
    when(cspPolicyService.constructAppHostCspPolicy()).thenReturn("policy");
    when(cspPolicyService.getSecurityHeaders("policy")).thenReturn(headers());

    cspInterceptor.preHandle(request, response, handler(ClassMarkedAppHost.class, "anyMethod"));

    verify(cspPolicyService, times(1)).constructAppHostCspPolicy();
    verify(cspPolicyService, never()).constructUserUploadedContentCspPolicy();
  }

  @Test
  void methodLevelOpenApiDocs_appliesOpenApiDocsPolicy() throws Exception {
    when(cspPolicyService.constructOpenApiDocsCspPolicy()).thenReturn("policy");
    when(cspPolicyService.getSecurityHeaders("policy")).thenReturn(headers());

    cspInterceptor.preHandle(request, response, handler(MethodMarkedOpenApiDocs.class, "openapi"));

    verify(cspPolicyService, times(1)).constructOpenApiDocsCspPolicy();
    verify(cspPolicyService, never()).constructUserUploadedContentCspPolicy();
    verify(cspPolicyService, never()).constructAppHostCspPolicy();
  }

  @Test
  void userUploadedTakesPrecedenceOverAppHostOnSameElement() throws Exception {
    when(cspPolicyService.constructUserUploadedContentCspPolicy()).thenReturn("policy");
    when(cspPolicyService.getSecurityHeaders("policy")).thenReturn(headers());

    cspInterceptor.preHandle(request, response, handler(BothMarked.class, "both"));

    verify(cspPolicyService, times(1)).constructUserUploadedContentCspPolicy();
    verify(cspPolicyService, never()).constructAppHostCspPolicy();
  }

  @Test
  void securityHeadersAreSetOnResponse() throws Exception {
    HttpHeaders h = new HttpHeaders();
    h.set("Content-Security-Policy", "default-src 'self';");
    h.set("X-Content-Type-Options", "nosniff");
    h.set("X-Frame-Options", "SAMEORIGIN");
    when(cspPolicyService.constructUserUploadedContentCspPolicy()).thenReturn("policy");
    when(cspPolicyService.getSecurityHeaders("policy")).thenReturn(h);

    cspInterceptor.preHandle(request, response, handler(MethodMarked.class, "marked"));

    verify(response).setHeader("Content-Security-Policy", "default-src 'self';");
    verify(response).setHeader("X-Content-Type-Options", "nosniff");
    verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
  }

  private static HttpHeaders headers() {
    HttpHeaders h = new HttpHeaders();
    h.set("Content-Security-Policy", "x");
    return h;
  }

  private static HandlerMethod handler(Class<?> controllerClass, String methodName)
      throws Exception {
    Object controller = controllerClass.getDeclaredConstructor().newInstance();
    Method method = controllerClass.getMethod(methodName);
    return new HandlerMethod(controller, method);
  }

  // The static classes below are test fixtures: each method body is intentionally empty
  // because the interceptor only inspects annotations via reflection; the methods exist
  // solely as targets for HandlerMethod construction.

  public static class Plain {
    public void plain() {
      // empty — fixture target for reflection
    }
  }

  public static class MethodMarked {
    @CspUserUploadedContent
    public void marked() {
      // empty — fixture target for reflection
    }
  }

  @CspAppHost
  public static class ClassMarkedAppHost {
    public void anyMethod() {
      // empty — fixture target for reflection
    }
  }

  public static class BothMarked {
    @CspUserUploadedContent
    @CspAppHost
    public void both() {
      // empty — fixture target for reflection
    }
  }

  public static class MethodMarkedOpenApiDocs {
    @CspOpenApiDocs
    public void openapi() {
      // empty — fixture target for reflection
    }
  }
}
