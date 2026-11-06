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

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Response-level coverage of baseline headers and handler policy precedence.
 *
 * @author Austin McGee
 * @author Morten Svanæs
 */
class CspInterceptorTest {
  @ParameterizedTest
  @CsvSource({
    "true,true",
    "TRUE,true",
    "on,true",
    "ON,true",
    "false,false",
    "FALSE,false",
    "off,false",
    "OFF,false"
  })
  void uploadedContentRemainsIsolated(String setting, boolean enabled) throws Exception {
    MockMvc mvc = mvc(setting);
    for (String path : List.of("/upload", "/app/upload", "/upload/openapi", "/upload/error")) {
      MockHttpServletResponse response = mvc.perform(get(path)).andReturn().getResponse();
      assertEquals(path.endsWith("error") ? 403 : 200, response.getStatus());
      String policy = response.getHeader("Content-Security-Policy");
      assertTrue(policy != null && policy.contains("default-src 'none';"), path + ": " + policy);
      assertEquals(1, response.getHeaders("Content-Security-Policy").size());
      assertTrue(policy.contains("object-src 'none';"));
      assertTrue(policy.contains("base-uri 'self';"));
      assertTrue(policy.contains("form-action 'self';"));
      assertFalse(policy.contains("upgrade-insecure-requests;"));
      assertTrue(
          policy.endsWith(
              enabled
                  ? "frame-ancestors 'self' https://embed.example;"
                  : "frame-ancestors 'self';"));
      assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
      assertEquals(enabled ? null : "SAMEORIGIN", response.getHeader("X-Frame-Options"));
    }
  }

  @ParameterizedTest
  @CsvSource({"on,true", "off,false"})
  void generalPoliciesRetainOptionalEnablement(String setting, boolean enabled) throws Exception {
    MockMvc mvc = mvc(setting);
    for (String path : List.of("/plain", "/app", "/openapi", "/missing")) {
      MockHttpServletResponse response = mvc.perform(get(path)).andReturn().getResponse();
      assertEquals(path.equals("/missing") ? 404 : 200, response.getStatus());
      assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
      if (enabled) {
        String policy = response.getHeader("Content-Security-Policy");
        assertEquals(1, response.getHeaders("Content-Security-Policy").size());
        assertTrue(policy.contains("default-src 'self';"));
        assertEquals(
            path.equals("/openapi"), policy.contains("script-src 'self' 'unsafe-inline';"));
        assertEquals(path.equals("/app"), policy.contains("child-src 'self' blob:;"));
        assertNull(response.getHeader("X-Frame-Options"));
      } else {
        assertNull(response.getHeader("Content-Security-Policy"));
        assertEquals("SAMEORIGIN", response.getHeader("X-Frame-Options"));
      }
    }
  }

  private MockMvc mvc(String setting) {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    when(config.getProperty(any()))
        .thenAnswer(
            invocation -> {
              ConfigurationKey key = invocation.getArgument(0);
              return key == CSP_ENABLED ? setting : key.getDefaultValue();
            });
    when(config.isEnabled(any())).thenCallRealMethod();
    ConfigurationService configuration = mock(ConfigurationService.class);
    when(configuration.getCorsWhitelist()).thenReturn(Set.of("https://embed.example"));
    CspPolicyService policies = new CspPolicyService(config, configuration);
    return MockMvcBuilders.standaloneSetup(new Plain(), new App(), new Upload())
        .addFilters(new CspBaselineFilter(policies))
        .addInterceptors(new CspInterceptor(policies))
        .build();
  }

  @RestController
  static class Plain {
    @GetMapping("/plain")
    String plain() {
      return "plain";
    }

    @CspUserUploadedContent
    @CspAppHost
    @CspOpenApiDocs
    @GetMapping("/upload")
    String upload() {
      return "upload";
    }

    @CspOpenApiDocs
    @GetMapping("/openapi")
    String openapi() {
      return "docs";
    }
  }

  @RestController
  @CspAppHost
  static class App {
    @GetMapping("/app")
    String app() {
      return "app";
    }

    @CspUserUploadedContent
    @GetMapping("/app/upload")
    String upload() {
      return "upload";
    }
  }

  @RestController
  @CspUserUploadedContent
  static class Upload {
    @CspOpenApiDocs
    @GetMapping("/upload/openapi")
    String upload() {
      return "upload";
    }

    @GetMapping("/upload/error")
    ResponseEntity<Void> error() {
      return ResponseEntity.status(403).build();
    }
  }
}
