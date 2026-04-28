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

import static org.hisp.dhis.security.utils.CspConstants.APP_HOST_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.DEFAULT_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.LEGACY_LOGIN_FALLBACK_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.USER_UPLOADED_CONTENT_CSP_POLICY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

/** Unit tests for {@link CspPolicyService}. */
@ExtendWith(MockitoExtension.class)
class CspPolicyServiceTest {

  @Mock(lenient = true)
  private DhisConfigurationProvider dhisConfig;

  @Mock(lenient = true)
  private ConfigurationService configurationService;

  @Mock(lenient = true)
  private CacheProvider cacheProvider;

  @Mock(lenient = true)
  @SuppressWarnings("unchecked")
  private Cache<Set<String>> corsWhitelistCache;

  private CspPolicyService cspPolicyService;

  private Set<String> corsWhitelist;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    when(dhisConfig.isEnabled(any())).thenReturn(true);
    corsWhitelist = Set.of();

    Configuration configuration = new Configuration();
    when(configurationService.getConfiguration())
        .thenAnswer(invocation -> withWhitelist(configuration));

    when(cacheProvider.<Set<String>>createCorsWhitelistCache()).thenReturn(corsWhitelistCache);
    when(corsWhitelistCache.get(anyString(), any(Function.class)))
        .thenAnswer(
            invocation -> {
              Function<String, Set<String>> loader = invocation.getArgument(1);
              return loader.apply(invocation.getArgument(0));
            });

    cspPolicyService = new CspPolicyService(dhisConfig, configurationService, cacheProvider);
  }

  private Configuration withWhitelist(Configuration configuration) {
    configuration.setCorsWhitelist(corsWhitelist);
    return configuration;
  }

  @Test
  void constructDefaultPolicy_appendsFrameAncestorsSelf() {
    String result = cspPolicyService.constructDefaultCspPolicy();

    assertEquals(DEFAULT_CSP_POLICY + " frame-ancestors 'self';", result);
  }

  @Test
  void constructUserUploadedContentPolicy_appendsFrameAncestorsSelf() {
    String result = cspPolicyService.constructUserUploadedContentCspPolicy();

    assertEquals(USER_UPLOADED_CONTENT_CSP_POLICY + " frame-ancestors 'self';", result);
  }

  @Test
  void constructAppHostPolicy_appendsFrameAncestorsSelf() {
    String result = cspPolicyService.constructAppHostCspPolicy();

    assertEquals(APP_HOST_CSP_POLICY + " frame-ancestors 'self';", result);
  }

  @Test
  void constructLegacyLoginFallbackPolicy_appendsFrameAncestorsSelf() {
    String result = cspPolicyService.constructLegacyLoginFallbackCspPolicy();

    assertEquals(LEGACY_LOGIN_FALLBACK_CSP_POLICY + " frame-ancestors 'self';", result);
  }

  @Test
  void corsWhitelist_isSortedDeterministically() {
    Set<String> whitelist = new LinkedHashSet<>();
    whitelist.add("https://zeta.example.com");
    whitelist.add("https://alpha.example.com");
    whitelist.add("https://mid.example.com");
    corsWhitelist = whitelist;

    String result = cspPolicyService.constructDefaultCspPolicy();

    assertEquals(
        DEFAULT_CSP_POLICY
            + " frame-ancestors 'self' https://alpha.example.com https://mid.example.com"
            + " https://zeta.example.com;",
        result);
  }

  @Test
  void getSecurityHeaders_cspEnabled_setsAllThreeHeaders() {
    HttpHeaders headers = cspPolicyService.getSecurityHeaders("script-src 'self';");

    assertNotNull(headers);
    assertTrue(headers.containsKey("Content-Security-Policy"));
    assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
    assertEquals("SAMEORIGIN", headers.getFirst("X-Frame-Options"));
  }

  @Test
  void getSecurityHeaders_cspDisabled_omitsCspButKeepsStandardHeaders() {
    when(dhisConfig.isEnabled(any())).thenReturn(false);

    HttpHeaders headers = cspPolicyService.getSecurityHeaders("script-src 'self';");

    assertFalse(headers.containsKey("Content-Security-Policy"));
    assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
    assertEquals("SAMEORIGIN", headers.getFirst("X-Frame-Options"));
  }

  @Test
  void getSecurityHeaders_nullPolicy_throws() {
    assertThrows(IllegalArgumentException.class, () -> cspPolicyService.getSecurityHeaders(null));
  }

  @Test
  void getSecurityHeaders_blankPolicy_throws() {
    assertThrows(IllegalArgumentException.class, () -> cspPolicyService.getSecurityHeaders("   "));
  }

  @Test
  void getDefaultSecurityHeaders_returnsDefaultPolicy() {
    HttpHeaders headers = cspPolicyService.getDefaultSecurityHeaders();

    assertEquals(
        DEFAULT_CSP_POLICY + " frame-ancestors 'self';",
        headers.getFirst("Content-Security-Policy"));
    assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
    assertEquals("SAMEORIGIN", headers.getFirst("X-Frame-Options"));
  }

  @Test
  void getSecurityHeaders_policyAlwaysEndsWithSemicolon() {
    HttpHeaders headers = cspPolicyService.getSecurityHeaders("script-src 'self'");
    String value = headers.getFirst("Content-Security-Policy");

    assertNotNull(value);
    assertTrue(value.endsWith(";"));
  }
}
