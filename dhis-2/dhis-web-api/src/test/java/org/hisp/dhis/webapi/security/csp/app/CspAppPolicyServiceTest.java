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
package org.hisp.dhis.webapi.security.csp.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.webapi.security.csp.CspPolicyService;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.Draft;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.EntrypointOverride;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.SourceChanges;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs
 */
class CspAppPolicyServiceTest {
  private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

  private DhisConfigurationProvider config;
  private CspAppPolicyService service;
  private boolean https;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    https = true;
    config = mock(DhisConfigurationProvider.class);
    when(config.isEnabled(any()))
        .thenAnswer(i -> i.getArgument(0) != ConfigurationKey.SERVER_HTTPS || https);
    ConfigurationService configuration = mock(ConfigurationService.class);
    when(configuration.getCorsWhitelist()).thenReturn(Set.of("https://embed.example.org"));
    Cache<Object> cache = mock(Cache.class);
    when(cache.get(anyString(), any()))
        .thenAnswer(i -> ((Function<String, Object>) i.getArgument(1)).apply(i.getArgument(0)));
    CacheProvider cacheProvider = mock(CacheProvider.class);
    when(cacheProvider.createCspAppOverrideCache()).thenReturn(cache);
    CspAppOverrideStore store = mock(CspAppOverrideStore.class);
    when(store.get(anyString())).thenReturn(Optional.empty());
    service =
        new CspAppPolicyService(
            new CspPolicyService(config, configuration),
            store,
            new DefaultCspReportUriProvider(config),
            cacheProvider);
  }

  private CspAppPolicyService.Effective compose(String app, String ep, CspAppOverride o) {
    return service.compose(app, ep, o, NOW, "");
  }

  private static CspAppOverride override(Map<String, EntrypointOverride> entrypoints) {
    return new CspAppOverride(
        1, "x", true, null, null, null, null, false, false, List.of(), entrypoints);
  }

  private static EntrypointOverride add(String directive, String... values) {
    return new EntrypointOverride(
        null, new SourceChanges(Map.of(directive, List.of(values)), List.of()), null);
  }

  private static Set<String> sources(String policy, String directive) {
    return Arrays.stream(policy.split(";"))
        .map(String::trim)
        .filter(d -> d.startsWith(directive + " "))
        .findFirst()
        .map(d -> Set.of(d.substring(directive.length()).trim().split(" ")))
        .orElse(Set.of());
  }

  @Test
  void nonMapsApps_getNoMapsHosts_mapsAppAndPluginDo() {
    for (String ep : CspEntrypoints.KEYS) {
      String policy = compose("dashboard", ep, null).contentSecurityPolicy();
      assertFalse(policy.contains("openstreetmap"), policy);
      assertFalse(policy.contains("openfreemap"), policy);
    }
    for (String ep : List.of("app", "plugin")) {
      String policy = compose("maps", ep, null).contentSecurityPolicy();
      for (String directive : List.of("img-src", "connect-src")) {
        assertTrue(
            sources(policy, directive)
                .containsAll(
                    List.of(
                        "https://tiles.openfreemap.org",
                        "https://tile.openstreetmap.org",
                        "https://a.tile.openstreetmap.org",
                        "https://cartodb-basemaps-c.global.ssl.fastly.net",
                        "https://tiles.maps.eox.at")),
            policy);
      }
      assertFalse(policy.contains("http://"), policy);
    }
    assertFalse(compose("maps", "worker", null).contentSecurityPolicy().contains("openfreemap"));
  }

  @Test
  void mapsBaseOverride_addsHttpVariantsWhileHttpsIsOff() {
    https = false;
    String policy = compose("maps", "app", null).contentSecurityPolicy();

    assertTrue(sources(policy, "img-src").contains("http://tiles.openfreemap.org"), policy);
    assertFalse(policy.contains("upgrade-insecure-requests"), policy);
  }

  @Test
  void footer_isAlwaysLast_withFrameAncestorsAndReportUri() {
    String policy = compose("maps", "plugin", null).contentSecurityPolicy();

    assertTrue(
        policy.endsWith(
            " upgrade-insecure-requests; frame-ancestors 'self' https://embed.example.org;"
                + " report-uri /api/security/csp/report?app=maps&ep=plugin;"),
        policy);
  }

  @Test
  void specificEntrypoint_mergesOntoAll_andSeedsMissingDirectivesFromTheirFallback() {
    CspAppOverride o =
        override(
            Map.of(
                "*", add("img-src", "https://all.example.org"),
                "plugin", add("frame-src", "https://embed.example.org")));

    String plugin = compose("my-app", "plugin", o).contentSecurityPolicy();
    String app = compose("my-app", "app", o).contentSecurityPolicy();

    assertTrue(sources(plugin, "img-src").contains("https://all.example.org"));
    assertEquals(
        Set.of("'self'", "blob:", "https://embed.example.org"), sources(plugin, "frame-src"));
    assertTrue(sources(app, "img-src").contains("https://all.example.org"));
    assertTrue(sources(app, "frame-src").isEmpty(), app);
  }

  @Test
  void scriptSrc_unsafeAdditionIsSeededWithSelf() {
    String policy =
        compose("my-app", "app", override(Map.of("app", add("script-src", "'unsafe-eval'"))))
            .contentSecurityPolicy();

    assertEquals(Set.of("'self'", "'unsafe-eval'"), sources(policy, "script-src"));
  }

  @Test
  void disabledBaseSources_removeOnlyForThatEntrypoint() {
    EntrypointOverride plugin =
        new EntrypointOverride(
            null, new SourceChanges(Map.of(), List.of("img-src https://tiles.maps.eox.at")), null);
    CspAppOverride o = override(Map.of("plugin", plugin));

    assertFalse(
        sources(compose("maps", "plugin", o).contentSecurityPolicy(), "img-src")
            .contains("https://tiles.maps.eox.at"));
    assertTrue(
        sources(compose("maps", "plugin", o).contentSecurityPolicy(), "connect-src")
            .contains("https://tiles.maps.eox.at"));
    assertTrue(
        sources(compose("maps", "app", o).contentSecurityPolicy(), "img-src")
            .contains("https://tiles.maps.eox.at"));
  }

  @Test
  void storedValuesThatFailValidation_neverReachTheHeader() {
    // e.g. written straight into the datastore by a superuser
    CspAppOverride o =
        override(
            Map.of(
                "*",
                new EntrypointOverride(
                    null,
                    new SourceChanges(
                        Map.of(
                            "default-src", List.of("*"),
                            "frame-ancestors", List.of("https://evil.example.org"),
                            "object-src", List.of("*"),
                            "img-src",
                                List.of(
                                    "https://ok.example.org",
                                    "http://insecure.example.org",
                                    "x; script-src *")),
                        List.of()),
                    null)));

    String policy = compose("my-app", "app", o).contentSecurityPolicy();

    assertTrue(policy.startsWith("default-src 'self';"), policy);
    assertTrue(policy.contains("object-src 'none';"), policy);
    assertTrue(policy.contains("frame-ancestors 'self' https://embed.example.org;"), policy);
    assertFalse(policy.contains("evil.example.org"), policy);
    assertFalse(policy.contains("insecure.example.org"), policy);
    assertFalse(policy.contains("script-src"), policy);
    assertTrue(sources(policy, "img-src").contains("https://ok.example.org"), policy);
  }

  @Test
  void disabledOverride_isIgnored() {
    CspAppOverride o =
        new CspAppOverride(
            1,
            "x",
            false,
            null,
            null,
            null,
            null,
            false,
            false,
            List.of(),
            Map.of("*", add("img-src", "https://all.example.org")));

    assertFalse(compose("my-app", "app", o).contentSecurityPolicy().contains("all.example.org"));
  }

  @Test
  void reportOnlyExempt_enforcesOnlyFrameAncestors_forThatEntrypointOnly() {
    CspAppOverride o =
        override(
            Map.of(
                "plugin",
                new EntrypointOverride(CspEnforcementMode.REPORT_ONLY_EXEMPT, null, null)));

    CspAppPolicyService.Effective plugin = compose("my-app", "plugin", o);
    CspAppPolicyService.Effective app = compose("my-app", "app", o);

    assertEquals(CspEnforcementMode.REPORT_ONLY_EXEMPT, plugin.enforcementMode());
    assertEquals(
        "frame-ancestors 'self' https://embed.example.org;", plugin.contentSecurityPolicy());
    assertTrue(plugin.contentSecurityPolicyReportOnly().startsWith("default-src 'self';"));
    assertTrue(
        plugin
            .contentSecurityPolicyReportOnly()
            .endsWith("report-uri /api/security/csp/report?app=my-app&ep=plugin;"));
    assertFalse(plugin.contentSecurityPolicyReportOnly().contains("frame-ancestors"));
    assertEquals(CspEnforcementMode.ENFORCE, app.enforcementMode());
    assertNull(app.contentSecurityPolicyReportOnly());
  }

  @Test
  void specificEnforceMode_overridesAnExemptAll() {
    CspAppOverride o =
        override(
            Map.of(
                "*", new EntrypointOverride(CspEnforcementMode.REPORT_ONLY_EXEMPT, null, null),
                "app", new EntrypointOverride(CspEnforcementMode.ENFORCE, null, null)));

    assertEquals(CspEnforcementMode.ENFORCE, compose("my-app", "app", o).enforcementMode());
    assertEquals(
        CspEnforcementMode.REPORT_ONLY_EXEMPT, compose("my-app", "plugin", o).enforcementMode());
  }

  @Test
  void draft_isReportOnly_untilTheTestWindowEnforcesItsAdditions() {
    Draft draft =
        new Draft(true, null, Map.of("img-src", List.of("https://draft.example.org")), List.of());
    CspAppOverride o = override(Map.of("app", new EntrypointOverride(null, null, draft)));

    CspAppPolicyService.Effective reportOnly = compose("my-app", "app", o);
    assertFalse(reportOnly.contentSecurityPolicy().contains("draft.example.org"));
    assertTrue(reportOnly.contentSecurityPolicyReportOnly().contains("https://draft.example.org"));
    assertTrue(reportOnly.draftActive());
    assertFalse(reportOnly.draftTestActive());

    Draft testing =
        new Draft(
            true,
            NOW.plusSeconds(3600).toString(),
            Map.of("img-src", List.of("https://draft.example.org")),
            List.of());
    CspAppPolicyService.Effective tested =
        compose(
            "my-app", "app", override(Map.of("app", new EntrypointOverride(null, null, testing))));
    assertTrue(tested.contentSecurityPolicy().contains("https://draft.example.org"));
    assertNull(tested.contentSecurityPolicyReportOnly());
    assertTrue(tested.draftTestActive());

    Draft expired =
        new Draft(
            true,
            NOW.minusSeconds(1).toString(),
            Map.of("img-src", List.of("https://draft.example.org")),
            List.of());
    CspAppPolicyService.Effective after =
        compose(
            "my-app", "app", override(Map.of("app", new EntrypointOverride(null, null, expired))));
    assertFalse(after.contentSecurityPolicy().contains("draft.example.org"));
    assertTrue(after.contentSecurityPolicyReportOnly().contains("draft.example.org"));
  }

  @Test
  void storedDocument_roundTripsThroughJson() {
    CspAppOverride o =
        new CspAppOverride(
            1,
            "maps",
            true,
            NOW.toString(),
            "admin",
            null,
            "OPS-1",
            true,
            true,
            List.of("app script-src 'unsafe-eval'"),
            Map.of(
                "app",
                new EntrypointOverride(
                    CspEnforcementMode.REPORT_ONLY_EXEMPT,
                    new SourceChanges(
                        Map.of("script-src", List.of("'unsafe-eval'")),
                        List.of("img-src https://tiles.maps.eox.at")),
                    new Draft(true, NOW.plusSeconds(60).toString(), Map.of(), List.of()))));

    assertEquals(o, CspAppPolicyService.parse("maps", CspAppPolicyService.toJson(o)));
    assertNull(CspAppPolicyService.parse("maps", "{not json"));
  }

  @Test
  void reportUri_isOmittedWhenReportingIsOff() {
    when(config.isEnabled(ConfigurationKey.CSP_REPORT_ENABLED)).thenReturn(false);

    assertFalse(compose("maps", "app", null).contentSecurityPolicy().contains("report-uri"));
  }

  @Test
  void policy_neverDeclaresADirectiveTwice() {
    CspAppOverride o =
        override(
            Map.of(
                "*", add("img-src", "https://a.example.org"),
                "app", add("img-src", "https://tiles.openfreemap.org", "https://a.example.org")));
    String policy = compose("maps", "app", o).contentSecurityPolicy();

    Map<String, Long> counts =
        Arrays.stream(policy.split(";"))
            .map(String::trim)
            .filter(d -> !d.isEmpty())
            .map(d -> d.split(" ")[0])
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    counts.forEach((d, c) -> assertEquals(1L, c, d + " in " + policy));
  }
}
