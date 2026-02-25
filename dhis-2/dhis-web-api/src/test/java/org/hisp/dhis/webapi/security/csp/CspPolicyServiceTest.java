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

import static org.hisp.dhis.security.utils.CspConstants.DEFAULT_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.USER_UPLOADED_CONTENT_CSP_POLICY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

/** Unit tests for {@link CspPolicyService}. */
@ExtendWith(MockitoExtension.class)
class CspPolicyServiceTest {

  @Mock private DhisConfigurationProvider dhisConfig;

  @Mock private ConfigurationService configurationService;

  @Mock private CacheProvider cacheProvider;

  @Mock(lenient = true)
  private Cache corsWhitelistCache;

  @InjectMocks private CspPolicyService cspPolicyService;

  private Configuration mockConfiguration;

  @BeforeEach
  void setUp() {
    when(cacheProvider.createCorsWhitelistCache()).thenReturn((Cache) corsWhitelistCache);
    mockConfiguration = mock(Configuration.class);
    when(configurationService.getConfiguration()).thenReturn(mockConfiguration);
    this.cspPolicyService = new CspPolicyService(dhisConfig, configurationService, cacheProvider);
  }

  @Test
  void testConstructCustomCspPolicy_WithValidPolicy() {
    String customPolicy = "script-src 'self'";
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    String result = cspPolicyService.constructCustomCspPolicy(customPolicy);

    assertNotNull(result);
    assertTrue(result.contains("script-src 'self'"));
    assertTrue(result.contains("frame-ancestors 'self'"));
  }

  @Test
  void testConstructCustomCspPolicy_WithNullPolicy() {
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    String result = cspPolicyService.constructCustomCspPolicy(null);

    assertNotNull(result);
    assertEquals("frame-ancestors 'self'", result);
  }

  @Test
  void testConstructCustomCspPolicy_WithPolicySemicolon() {
    String customPolicy = "script-src 'self';";
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    String result = cspPolicyService.constructCustomCspPolicy(customPolicy);

    assertNotNull(result);
    assertEquals("script-src 'self'; frame-ancestors 'self';", result);
  }

  @Test
  void testConstructCustomCspPolicy_WithCorsWhitelist() {
    String customPolicy = "script-src 'self'";
    Set<String> corsWhitelist = new HashSet<>();
    corsWhitelist.add("https://example.com");
    corsWhitelist.add("https://trusted.org");

    when(corsWhitelistCache.get(anyString(), any())).thenReturn(corsWhitelist);
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    String result = cspPolicyService.constructCustomCspPolicy(customPolicy);

    assertNotNull(result);
    assertEquals(
        "script-src 'self'; frame-ancestors 'self' https://example.com https://trusted.org;",
        result);
  }

  @Test
  void testConstructDefaultCspPolicy() {
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    String result = cspPolicyService.constructDefaultCspPolicy();

    assertNotNull(result);
    assertEquals(DEFAULT_CSP_POLICY + " frame-ancestors 'self';", result);
  }

  @Test
  void testConstructUserUploadedContentCspPolicy() {
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    String result = cspPolicyService.constructUserUploadedContentCspPolicy();

    assertNotNull(result);
    assertEquals(USER_UPLOADED_CONTENT_CSP_POLICY + " frame-ancestors 'self';", result);
  }

  @Test
  void testGetSecurityHeaders_CspEnabled() {
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    String cspPolicy = "script-src 'self'";
    HttpHeaders headers = cspPolicyService.getSecurityHeaders(cspPolicy);

    assertNotNull(headers);
    assertTrue(headers.containsKey("Content-Security-Policy"));
    assertTrue(headers.containsKey("X-Content-Type-Options"));
    assertTrue(headers.containsKey("X-Frame-Options"));
    assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
    assertEquals("SAMEORIGIN", headers.getFirst("X-Frame-Options"));
  }

  @Test
  void testGetSecurityHeaders_CspDisabled() {
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(false);

    String cspPolicy = "script-src 'self'";
    HttpHeaders headers = cspPolicyService.getSecurityHeaders(cspPolicy);

    assertNotNull(headers);
    assertFalse(headers.containsKey("Content-Security-Policy"));
    assertTrue(headers.containsKey("X-Content-Type-Options"));
    assertTrue(headers.containsKey("X-Frame-Options"));
  }

  @Test
  void testGetSecurityHeaders_WithNullPolicy() {
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    HttpHeaders headers = cspPolicyService.getSecurityHeaders(null);

    assertNotNull(headers);
    assertFalse(headers.containsKey("Content-Security-Policy"));
  }

  @Test
  void testGetSecurityHeaders_WithEmptyPolicy() {
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    HttpHeaders headers = cspPolicyService.getSecurityHeaders("   ");

    assertNotNull(headers);
    assertFalse(headers.containsKey("Content-Security-Policy"));
  }

  @Test
  void testGetSecurityHeaders_PolicyEndsWithSemicolon() {
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    HttpHeaders headers = cspPolicyService.getSecurityHeaders("script-src 'self';");

    assertNotNull(headers);
    String cspHeaderValue = headers.getFirst("Content-Security-Policy");
    assertNotNull(cspHeaderValue);
    assertTrue(cspHeaderValue.endsWith(";"));
  }

  @Test
  void testConstructCustomCspPolicy_EmptyStringPolicy() {
    when(corsWhitelistCache.get(anyString(), any())).thenReturn(new HashSet<>());
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    String result = cspPolicyService.constructCustomCspPolicy("   ");

    assertNotNull(result);
    assertEquals("frame-ancestors 'self'", result);
  }

  @Test
  void testConstructCustomCspPolicy_MultipleCorsOrigins() {
    String customPolicy = "script-src 'self'";
    Set<String> corsWhitelist = new HashSet<>();
    corsWhitelist.add("https://example.com");
    corsWhitelist.add("https://trusted.org");
    corsWhitelist.add("https://partner.io");

    when(corsWhitelistCache.get(anyString(), any())).thenReturn(corsWhitelist);
    when(dhisConfig.isEnabled(any())).thenReturn(true);

    String result = cspPolicyService.constructCustomCspPolicy(customPolicy);

    assertNotNull(result);
    assertTrue(result.contains("script-src 'self';"));
    assertTrue(result.contains("frame-ancestors 'self'"));
    assertTrue(
        result.contains("https://example.com")
            && result.contains("https://trusted.org")
            && result.contains("https://partner.io"));
  }
}
