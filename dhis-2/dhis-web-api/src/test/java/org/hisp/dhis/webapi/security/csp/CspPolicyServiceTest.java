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

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_MAP_SOURCES;
import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_UPGRADE_INSECURE_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.SERVER_HTTPS;
import static org.hisp.dhis.security.utils.CspConstants.APP_HOST_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.DEFAULT_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.MAPS_BASEMAP_HTTP_ORIGINS;
import static org.hisp.dhis.security.utils.CspConstants.MAPS_BASEMAP_ORIGINS;
import static org.hisp.dhis.security.utils.CspConstants.OPENAPI_DOCS_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.USER_UPLOADED_CONTENT_CSP_POLICY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

/**
 * Unit tests for {@link CspPolicyService}.
 *
 * @author Austin McGee
 */
@ExtendWith(MockitoExtension.class)
class CspPolicyServiceTest {

  @Mock(lenient = true)
  private DhisConfigurationProvider dhisConfig;

  @Mock(lenient = true)
  private ConfigurationService configurationService;

  private CspPolicyService cspPolicyService;

  private Set<String> corsWhitelist;

  @BeforeEach
  void setUp() {
    when(dhisConfig.isEnabled(any())).thenReturn(true);
    corsWhitelist = Set.of();
    when(configurationService.getCorsWhitelist()).thenAnswer(invocation -> corsWhitelist);

    cspPolicyService = new CspPolicyService(dhisConfig, configurationService);
  }

  @Test
  void constructDefaultPolicy_appendsFrameAncestorsSelf() {
    String result = cspPolicyService.constructDefaultCspPolicy();

    assertEquals(
        DEFAULT_CSP_POLICY + " upgrade-insecure-requests; frame-ancestors 'self';", result);
  }

  @Test
  void userUploadedContentHeaders_includeFrameAncestorsSelf() {
    String result =
        cspPolicyService
            .getUserUploadedContentSecurityHeaders()
            .getFirst("Content-Security-Policy");

    assertEquals(
        USER_UPLOADED_CONTENT_CSP_POLICY + " upgrade-insecure-requests; frame-ancestors 'self';",
        result);
  }

  @Test
  void constructAppHostPolicy_serverHttpsOn_appendsFrameAncestorsSelfWithHttpsOnly() {
    // setUp mocks dhisConfig.isEnabled(any()) -> true, so SERVER_HTTPS is on.
    String result = cspPolicyService.constructAppHostCspPolicy();

    assertEquals(
        APP_HOST_CSP_POLICY + " upgrade-insecure-requests; frame-ancestors 'self';", result);
    assertFalse(
        result.contains("http://"),
        "https-on policy should not contain plain-http origins, got: " + result);
  }

  @Test
  void constructAppHostPolicy_allowsBundledBasemapOriginsInImgAndConnectSrcOnly() {
    // The bundled Maps app's built-in basemaps: CartoDB (OSM Light), OpenStreetMap (OSM Detailed)
    // and the EOX Sentinel-2 WMS. All three must work without any configuration.
    List<String> builtIn =
        List.of(
            "https://cartodb-basemaps-a.global.ssl.fastly.net",
            "https://cartodb-basemaps-b.global.ssl.fastly.net",
            "https://cartodb-basemaps-c.global.ssl.fastly.net",
            "https://a.tile.openstreetmap.org",
            "https://b.tile.openstreetmap.org",
            "https://c.tile.openstreetmap.org",
            "https://tiles.maps.eox.at");

    String result = cspPolicyService.constructAppHostCspPolicy();

    assertTrue(sources(result, "img-src").containsAll(builtIn), result);
    assertTrue(sources(result, "connect-src").containsAll(builtIn), result);
    for (String policy : nonAppHostPolicies()) {
      for (String origin : builtIn) {
        assertFalse(policy.contains(origin), policy);
      }
    }
  }

  @Test
  void constructAppHostPolicy_serverHttpsOff_includesHttpBasemapOrigins() {
    // Plain HTTP deployments retain the Maps app's HTTP tile origins without upgrading requests.
    when(dhisConfig.isEnabled(SERVER_HTTPS)).thenReturn(false);

    String result = cspPolicyService.constructAppHostCspPolicy();

    // both img-src and connect-src should now allow the http variants
    List<String> httpOrigins = Arrays.asList(MAPS_BASEMAP_HTTP_ORIGINS.split(" "));
    assertTrue(sources(result, "img-src").containsAll(httpOrigins), result);
    assertTrue(sources(result, "connect-src").containsAll(httpOrigins), result);
    assertTrue(
        sources(result, "img-src").containsAll(Arrays.asList(MAPS_BASEMAP_ORIGINS.split(" "))),
        result);
  }

  @Test
  void constructOpenApiDocsPolicy_appendsFrameAncestorsSelf() {
    String result = cspPolicyService.constructOpenApiDocsCspPolicy();

    assertEquals(
        OPENAPI_DOCS_CSP_POLICY + " upgrade-insecure-requests; frame-ancestors 'self';", result);
  }

  @Test
  void allEmittedPolicies_includeCommonHardeningDirectives() {
    // base-uri, form-action, object-src are policy-level directives that don't fall back to
    // default-src per the CSP spec, so every emitted policy must declare them explicitly.
    // Regression guard.
    String[] policies = {
      cspPolicyService.constructDefaultCspPolicy(),
      cspPolicyService.getUserUploadedContentSecurityHeaders().getFirst("Content-Security-Policy"),
      cspPolicyService.constructAppHostCspPolicy(),
      cspPolicyService.constructOpenApiDocsCspPolicy(),
    };
    for (String policy : policies) {
      assertTrue(policy.contains("base-uri 'self'"), "missing base-uri 'self' in: " + policy);
      assertTrue(policy.contains("form-action 'self'"), "missing form-action 'self' in: " + policy);
      assertTrue(policy.contains("object-src 'none'"), "missing object-src 'none' in: " + policy);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "true,true,true",
    "TRUE,TRUE,true",
    "on,on,true",
    "ON,ON,true",
    "false,on,false",
    "FALSE,ON,false",
    "off,true,false",
    "OFF,TRUE,false",
    "on,false,false",
    "ON,FALSE,false",
    "true,off,false",
    "TRUE,OFF,false"
  })
  void upgradeInsecureRequests_requiresHttpsAndEnabledSetting(
      String https, String upgrade, boolean expected) {
    when(dhisConfig.getProperty(SERVER_HTTPS)).thenReturn(https);
    when(dhisConfig.getProperty(CSP_UPGRADE_INSECURE_ENABLED)).thenReturn(upgrade);
    when(dhisConfig.isEnabled(SERVER_HTTPS)).thenCallRealMethod();
    when(dhisConfig.isEnabled(CSP_UPGRADE_INSECURE_ENABLED)).thenCallRealMethod();

    for (String policy :
        new String[] {
          cspPolicyService.constructDefaultCspPolicy(),
          cspPolicyService.constructAppHostCspPolicy(),
          cspPolicyService.constructOpenApiDocsCspPolicy(),
          cspPolicyService
              .getUserUploadedContentSecurityHeaders()
              .getFirst("Content-Security-Policy")
        }) {
      assertEquals(expected, policy.contains("upgrade-insecure-requests;"), policy);
    }
  }

  @Test
  void appImageSources_doNotRelaxOtherPoliciesOrScriptSources() {
    String appPolicy = cspPolicyService.constructAppHostCspPolicy();
    String imageDirective =
        Arrays.stream(appPolicy.split(";"))
            .map(String::trim)
            .filter(directive -> directive.startsWith("img-src "))
            .findFirst()
            .orElseThrow();
    assertTrue(imageDirective.contains("blob:"));
    assertTrue(imageDirective.contains("https://apps.dhis2.org"));
    // Scripts still fall back to self; image allowances must not authorize executable content.
    assertTrue(appPolicy.startsWith("default-src 'self';"));
    assertFalse(appPolicy.contains("script-src"));
    assertFalse(appPolicy.contains("'unsafe-eval'"));
    for (String policy :
        new String[] {
          cspPolicyService.constructDefaultCspPolicy(),
          cspPolicyService.constructOpenApiDocsCspPolicy(),
          cspPolicyService
              .getUserUploadedContentSecurityHeaders()
              .getFirst("Content-Security-Policy")
        }) {
      assertFalse(policy.contains("blob:"), policy);
      assertFalse(policy.contains("https://apps.dhis2.org"), policy);
    }
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
            + " upgrade-insecure-requests;"
            + " frame-ancestors 'self' https://alpha.example.com https://mid.example.com"
            + " https://zeta.example.com;",
        result);
  }

  @Test
  void getSecurityHeaders_cspEnabled_setsCspAndContentTypeButNoXFrameOptions() {
    HttpHeaders headers = cspPolicyService.getSecurityHeaders("script-src 'self';");

    assertNotNull(headers);
    assertTrue(headers.containsHeader("Content-Security-Policy"));
    assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
    // X-Frame-Options is omitted when CSP is enabled because the frame-ancestors
    // directive in the CSP is the source of truth and may legitimately whitelist
    // external origins.
    assertFalse(headers.containsHeader("X-Frame-Options"));
  }

  @Test
  void getSecurityHeaders_cspDisabled_omitsCspAndFallsBackToXFrameOptions() {
    when(dhisConfig.isEnabled(any())).thenReturn(false);

    HttpHeaders headers = cspPolicyService.getSecurityHeaders("script-src 'self';");

    assertFalse(headers.containsHeader("Content-Security-Policy"));
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
        DEFAULT_CSP_POLICY + " upgrade-insecure-requests; frame-ancestors 'self';",
        headers.getFirst("Content-Security-Policy"));
    assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
    assertFalse(headers.containsHeader("X-Frame-Options"));
  }

  @Test
  void getSecurityHeaders_policyAlwaysEndsWithSemicolon() {
    HttpHeaders headers = cspPolicyService.getSecurityHeaders("script-src 'self'");
    String value = headers.getFirst("Content-Security-Policy");

    assertNotNull(value);
    assertTrue(value.endsWith(";"));
  }

  @Test
  void configuredMapSources_allowedOriginReachesImgAndConnectSrcOnly() {
    when(dhisConfig.getProperty(CSP_MAP_SOURCES))
        .thenReturn("https://wms.example.org,https://tiles.example.org:8443");

    String result = cspPolicyService.constructAppHostCspPolicy();

    // exactly the configured origins reach both directives, nothing derived from them
    Set<String> expected = Set.of("https://wms.example.org", "https://tiles.example.org:8443");
    assertEquals(expected, configuredOrigins(sources(result, "img-src")));
    assertEquals(expected, configuredOrigins(sources(result, "connect-src")));
    // no other directive of the app-host policy gains the configured origins
    for (String directive : directiveNames(result)) {
      if (!directive.equals("img-src") && !directive.equals("connect-src")) {
        assertFalse(sources(result, directive).contains("https://wms.example.org"), result);
      }
    }
    // and no other policy does either
    for (String policy : nonAppHostPolicies()) {
      assertFalse(policy.contains("example.org"), policy);
    }
  }

  @Test
  void configuredMapSources_mixedValidAndInvalidEntries_keepOnlyBareOrigins() {
    when(dhisConfig.getProperty(CSP_MAP_SOURCES))
        .thenReturn(
            " https://good.example.org ,"
                + "https://path.example.org/tiles,"
                + "https://trailing.example.org/,"
                + "https://user:secret@userinfo.example.org,"
                + "https://*.wildcard.example.org,"
                + "*,"
                + "'unsafe-inline',"
                + "data:,"
                + "javascript://script.example.org,"
                + "https://inject.example.org; script-src 'unsafe-inline',"
                + "https://port.example.org:70000,"
                + ","
                + "https://good.example.org");

    String result = cspPolicyService.constructAppHostCspPolicy();

    Set<String> imgSources = sources(result, "img-src");
    assertTrue(imgSources.contains("https://good.example.org"), result);
    // the repeated entry is emitted once per directive, so twice in the whole policy
    assertEquals(
        2,
        Arrays.stream(result.split("[ ;]")).filter("https://good.example.org"::equals).count(),
        result);
    for (String rejected :
        List.of(
            "path.example.org",
            "trailing.example.org",
            "userinfo.example.org",
            "wildcard.example.org",
            "script.example.org",
            "inject.example.org",
            "port.example.org")) {
      assertFalse(result.contains(rejected), result);
    }
    assertFalse(result.contains("script-src"), result);
    assertFalse(imgSources.contains("*"), result);
    assertFalse(imgSources.contains("'unsafe-inline'"), result);
    // the invalid entries neither add nor remove a directive
    assertEquals(
        directiveNames(APP_HOST_CSP_POLICY + " upgrade-insecure-requests; frame-ancestors 'self';"),
        directiveNames(result),
        result);
  }

  @Test
  void configuredMapSources_exactOriginIsEmittedVerbatim() {
    when(dhisConfig.getProperty(CSP_MAP_SOURCES)).thenReturn("https://tiles.example.org");

    Set<String> imgSources = sources(cspPolicyService.constructAppHostCspPolicy(), "img-src");

    // no host wildcard, no path suffix and no scheme-relative form is derived from the entry
    assertEquals(Set.of("https://tiles.example.org"), configuredOrigins(imgSources));
  }

  @Test
  void configuredMapSources_httpEntry_rejectedOnHttpsDeploymentAndKeptWhenHttpsOff() {
    when(dhisConfig.getProperty(CSP_MAP_SOURCES)).thenReturn("http://intranet.example.org:8080");

    assertFalse(
        cspPolicyService.constructAppHostCspPolicy().contains("intranet.example.org"),
        "plain HTTP map sources must not be allowed while server.https is on");

    when(dhisConfig.isEnabled(SERVER_HTTPS)).thenReturn(false);
    CspPolicyService httpService = new CspPolicyService(dhisConfig, configurationService);

    String httpPolicy = httpService.constructAppHostCspPolicy();
    assertTrue(
        sources(httpPolicy, "img-src").contains("http://intranet.example.org:8080"), httpPolicy);
    assertTrue(
        sources(httpPolicy, "connect-src").contains("http://intranet.example.org:8080"),
        httpPolicy);
  }

  /** The test origins, isolated from the built-in basemap origins of the policy. */
  private static Set<String> configuredOrigins(Set<String> sources) {
    return sources.stream()
        .filter(source -> source.contains("example.org"))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private String[] nonAppHostPolicies() {
    return new String[] {
      cspPolicyService.constructDefaultCspPolicy(),
      cspPolicyService.constructOpenApiDocsCspPolicy(),
      cspPolicyService.getUserUploadedContentSecurityHeaders().getFirst("Content-Security-Policy")
    };
  }

  private static Set<String> directiveNames(String policy) {
    return Arrays.stream(policy.split(";"))
        .map(String::trim)
        .filter(directive -> !directive.isEmpty())
        .map(directive -> directive.split(" ")[0])
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static Set<String> sources(String policy, String directive) {
    return Arrays.stream(policy.split(";"))
        .map(String::trim)
        .filter(d -> d.equals(directive) || d.startsWith(directive + " "))
        .findFirst()
        .map(d -> d.substring(directive.length()).trim())
        .map(
            values ->
                Arrays.stream(values.split(" "))
                    .filter(value -> !value.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new)))
        .orElseThrow(() -> new AssertionError("no " + directive + " directive in: " + policy));
  }
}
