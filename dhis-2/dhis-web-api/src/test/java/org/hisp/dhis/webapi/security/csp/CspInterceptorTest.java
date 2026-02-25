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
package org.hisp.dhis.webapi.security.csp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;

/** Unit tests for {@link CspInterceptor}. */
@ExtendWith(MockitoExtension.class)
class CspInterceptorTest {

  @Mock private CspPolicyService cspPolicyService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @InjectMocks private CspInterceptor cspInterceptor;

  @BeforeEach
  void setUp() {
    doNothing().when(response).addHeader(anyString(), anyString());
  }

  @Test
  void testPreHandle_WithoutHandlerMethod() throws Exception {
    Object handler = "not a handler method";
    boolean result = cspInterceptor.preHandle(request, response, handler);

    assertTrue(result);
    verify(response, never()).addHeader(anyString(), anyString());
  }

  @Test
  void testPreHandle_WithHandlerMethod_NoAnnotations() throws Exception {
    HandlerMethod handlerMethod = createHandlerMethod("methodWithoutAnnotations");
    when(cspPolicyService.constructCustomCspPolicy(null))
        .thenReturn("script-src 'self'; frame-ancestors 'self';");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Security-Policy", "script-src 'self'; frame-ancestors 'self';");
    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("X-Frame-Options", "SAMEORIGIN");
    when(cspPolicyService.getSecurityHeaders(null)).thenReturn(headers);

    boolean result = cspInterceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    verify(cspPolicyService, times(1)).getSecurityHeaders(null);
    verify(response, atLeast(2)).addHeader(anyString(), anyString());
  }

  @Test
  void testPreHandle_WithMethodLevelCustomCspAnnotation() throws Exception {
    TestControllerWithMethodAnnotation controller = new TestControllerWithMethodAnnotation();
    Method method = controller.getClass().getMethod("methodWithCustomCsp");
    HandlerMethod handlerMethod = new HandlerMethod(controller, method);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Security-Policy", "script-src 'unsafe-inline'; frame-ancestors 'self';");
    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("X-Frame-Options", "SAMEORIGIN");
    when(cspPolicyService.constructCustomCspPolicy("script-src 'unsafe-inline'"))
        .thenReturn("script-src 'unsafe-inline'; frame-ancestors 'self';");
    when(cspPolicyService.getSecurityHeaders(any())).thenReturn(headers);

    boolean result = cspInterceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    verify(cspPolicyService, times(1)).constructCustomCspPolicy("script-src 'unsafe-inline'");
    verify(cspPolicyService, times(1)).getSecurityHeaders(any());
  }

  @Test
  void testPreHandle_WithMethodLevelCspUserUploadedContentAnnotation() throws Exception {
    TestControllerWithUserUploadedContent controller = new TestControllerWithUserUploadedContent();
    Method method = controller.getClass().getMethod("methodWithUserUploadedContent");
    HandlerMethod handlerMethod = new HandlerMethod(controller, method);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'self';");
    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("X-Frame-Options", "SAMEORIGIN");
    when(cspPolicyService.constructUserUploadedContentCspPolicy())
        .thenReturn("default-src 'none'; frame-ancestors 'self';");
    when(cspPolicyService.getSecurityHeaders(any())).thenReturn(headers);

    boolean result = cspInterceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    verify(cspPolicyService, times(1)).constructUserUploadedContentCspPolicy();
    verify(cspPolicyService, times(1)).getSecurityHeaders(any());
  }

  @Test
  void testPreHandle_WithClassLevelCustomCspAnnotation() throws Exception {
    TestControllerWithClassAnnotation controller = new TestControllerWithClassAnnotation();
    Method method = controller.getClass().getMethod("methodWithoutAnnotations");
    HandlerMethod handlerMethod = new HandlerMethod(controller, method);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Security-Policy", "script-src 'self' cdn.example.com;");
    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("X-Frame-Options", "SAMEORIGIN");
    when(cspPolicyService.constructCustomCspPolicy("script-src 'self' cdn.example.com"))
        .thenReturn("script-src 'self' cdn.example.com; frame-ancestors 'self';");
    when(cspPolicyService.getSecurityHeaders(any())).thenReturn(headers);

    boolean result = cspInterceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    verify(cspPolicyService, times(1))
        .constructCustomCspPolicy("script-src 'self' cdn.example.com");
  }

  @Test
  void testPreHandle_MethodLevelTakesPrecedenceOverClass() throws Exception {
    TestControllerWithBothAnnotations controller = new TestControllerWithBothAnnotations();
    Method method = controller.getClass().getMethod("methodWithCustomCsp");
    HandlerMethod handlerMethod = new HandlerMethod(controller, method);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Security-Policy", "script-src 'unsafe-eval';");
    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("X-Frame-Options", "SAMEORIGIN");
    when(cspPolicyService.constructCustomCspPolicy("script-src 'unsafe-eval'"))
        .thenReturn("script-src 'unsafe-eval'; frame-ancestors 'self';");
    when(cspPolicyService.getSecurityHeaders(any())).thenReturn(headers);

    boolean result = cspInterceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    // Verify it was called with method-level annotation, not class-level
    verify(cspPolicyService, times(1)).constructCustomCspPolicy("script-src 'unsafe-eval'");
    verify(cspPolicyService, never()).constructCustomCspPolicy("script-src 'self' cdn.example.com");
  }

  @Test
  void testPreHandle_CustomCspOverUserUploadedContent() throws Exception {
    TestControllerWithBothMethodAnnotations controller =
        new TestControllerWithBothMethodAnnotations();
    Method method = controller.getClass().getMethod("methodWithBothAnnotations");
    HandlerMethod handlerMethod = new HandlerMethod(controller, method);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Security-Policy", "style-src 'nonce-abc123';");
    when(cspPolicyService.constructCustomCspPolicy("style-src 'nonce-abc123'"))
        .thenReturn("style-src 'nonce-abc123'; frame-ancestors 'self';");
    when(cspPolicyService.getSecurityHeaders(any())).thenReturn(headers);

    boolean result = cspInterceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    verify(cspPolicyService, times(1)).constructCustomCspPolicy("style-src 'nonce-abc123'");
    verify(cspPolicyService, never()).constructUserUploadedContentCspPolicy();
  }

  @Test
  void testPreHandle_SecurityHeadersApplied() throws Exception {
    TestControllerWithMethodAnnotation controller = new TestControllerWithMethodAnnotation();
    Method method = controller.getClass().getMethod("methodWithCustomCsp");
    HandlerMethod handlerMethod = new HandlerMethod(controller, method);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Security-Policy", "script-src 'unsafe-inline'; frame-ancestors 'self';");
    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("X-Frame-Options", "SAMEORIGIN");
    when(cspPolicyService.constructCustomCspPolicy("script-src 'unsafe-inline'"))
        .thenReturn("script-src 'unsafe-inline'; frame-ancestors 'self';");
    when(cspPolicyService.getSecurityHeaders(any())).thenReturn(headers);

    boolean result = cspInterceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    verify(response, times(3)).addHeader(anyString(), anyString());
  }

  // Test controller classes with various annotation combinations
  private static class TestControllerWithMethodAnnotation {
    @CustomCsp("script-src 'unsafe-inline'")
    public void methodWithCustomCsp() {}
  }

  private static class TestControllerWithUserUploadedContent {
    @CspUserUploadedContent
    public void methodWithUserUploadedContent() {}
  }

  @CustomCsp("script-src 'self' cdn.example.com")
  private static class TestControllerWithClassAnnotation {
    public void methodWithoutAnnotations() {}
  }

  @CustomCsp("script-src 'self' cdn.example.com")
  private static class TestControllerWithBothAnnotations {
    @CustomCsp("script-src 'unsafe-eval'")
    public void methodWithCustomCsp() {}
  }

  private static class TestControllerWithBothMethodAnnotations {
    @CustomCsp("style-src 'nonce-abc123'")
    @CspUserUploadedContent
    public void methodWithBothAnnotations() {}
  }

  /** Helper method to create a HandlerMethod for a method without annotations. */
  private HandlerMethod createHandlerMethod(String methodName) throws Exception {
    TestControllerPlain controller = new TestControllerPlain();
    Method method = controller.getClass().getMethod(methodName);
    return new HandlerMethod(controller, method);
  }

  private static class TestControllerPlain {
    public void methodWithoutAnnotations() {}
  }
}
