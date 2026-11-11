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
import java.util.List;
import java.util.Set;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.LocalCache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.webapi.security.csp.CspPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs
 */
class CspViolationServiceTest {
  private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

  private CspViolationService service;
  private CspAppPolicyService appPolicyService;

  @BeforeEach
  void setUp() {
    CacheProvider cacheProvider = mock(CacheProvider.class);
    when(cacheProvider.createCspViolationCache())
        .thenReturn(new LocalCache<>(new SimpleCacheBuilder<>().withMaximumSize(1000)));
    appPolicyService = mock(CspAppPolicyService.class);
    when(appPolicyService.enforcedPolicy(anyString(), anyString(), any()))
        .thenReturn(
            CspPolicy.parse("default-src 'self'; img-src 'self' https://allowed.example.org;"));
    CspPolicyService cspPolicyService = mock(CspPolicyService.class);
    when(cspPolicyService.isHttpsEnabled()).thenReturn(true);
    service =
        new CspViolationService(
            cacheProvider,
            appPolicyService,
            cspPolicyService,
            mock(DhisConfigurationProvider.class));
  }

  private static CspViolationService.Report report(String app, String directive, String blocked) {
    return new CspViolationService.Report(
        app,
        "app",
        "enforce",
        directive,
        blocked,
        "http://h/api/apps/" + app + "/index.html",
        null,
        3,
        16);
  }

  @Test
  void sameFingerprint_isCountedOnce_withFirstAndLastSeen() {
    service.record(report("maps", "img-src", "https://wms.example.org"), NOW);
    service.record(report("maps", "img-src", "https://wms.example.org"), NOW.plusSeconds(60));
    service.record(report("maps", "connect-src", "https://wms.example.org"), NOW.plusSeconds(61));

    List<CspViolationService.Violation> list = service.list("maps", NOW.plusSeconds(62));

    assertEquals(2, list.size());
    CspViolationService.Violation img = list.get(1);
    assertEquals(2, img.count());
    assertEquals(NOW.toString(), img.firstSeen());
    assertEquals(NOW.plusSeconds(60).toString(), img.lastSeen());
    assertTrue(img.suggestable());
    assertEquals("img-src", img.suggestedDirective());
    assertEquals("https://wms.example.org", img.suggestedSource());
    assertEquals("EXTERNAL", img.violationKind());
    assertEquals("NEW", img.status());
  }

  @Test
  void perApp_atMost100Fingerprints_leastRecentlySeenEvicted() {
    for (int i = 0; i < 105; i++) {
      service.record(
          report("maps", "img-src", "https://h" + i + ".example.org"), NOW.plusSeconds(i));
    }
    // touching h5 keeps it, h0..h4 and h6..h9 are the oldest
    service.record(report("maps", "img-src", "https://h5.example.org"), NOW.plusSeconds(200));
    service.record(report("maps", "img-src", "https://new.example.org"), NOW.plusSeconds(201));

    List<String> blocked =
        service.list("maps", NOW.plusSeconds(202)).stream()
            .map(CspViolationService.Violation::blocked)
            .toList();

    assertEquals(100, blocked.size());
    assertTrue(blocked.contains("https://h5.example.org"));
    assertTrue(blocked.contains("https://new.example.org"));
    assertFalse(blocked.contains("https://h0.example.org"));
    assertFalse(blocked.contains("https://h6.example.org"));
  }

  @Test
  void entries_expire24HoursAfterLastSeen() {
    service.record(report("maps", "img-src", "https://old.example.org"), NOW);
    service.record(report("maps", "img-src", "https://fresh.example.org"), NOW.plusSeconds(3600));

    List<CspViolationService.Violation> list = service.list("maps", NOW.plusSeconds(24 * 3600 + 1));

    assertEquals(
        List.of("https://fresh.example.org"), list.stream().map(v -> v.blocked()).toList());
  }

  @Test
  void classification_inlineEvalAndLockedDirectives_areNotSuggestable() {
    service.record(report("maps", "script-src", "inline"), NOW);
    service.record(report("maps", "script-src", "eval"), NOW);
    service.record(report("maps", "object-src", "https://flash.example.org"), NOW);
    service.record(report("maps", "img-src", "http://insecure.example.org"), NOW);

    for (CspViolationService.Violation v : service.list("maps", NOW)) {
      assertFalse(v.suggestable(), v.toString());
      assertNull(v.suggestedSource());
    }
    Set<String> kinds =
        Set.copyOf(service.list("maps", NOW).stream().map(v -> v.violationKind()).toList());
    assertEquals(Set.of("INLINE", "EVAL", "EXTERNAL"), kinds);
    assertTrue(
        service.list("maps", NOW).stream()
            .filter(v -> v.violationKind().equals("EVAL"))
            .findFirst()
            .orElseThrow()
            .remediation()
            .contains("'unsafe-eval'"));
  }

  @Test
  void status_isAllowedNow_whenTheSourceIsInTheCurrentPolicy() {
    service.record(report("maps", "img-src", "https://allowed.example.org"), NOW);

    assertEquals("ALLOWED_NOW", service.list("maps", NOW).get(0).status());
  }

  @Test
  void counts_splitEnforceAndReport_andClearRemovesAnApp() {
    service.record(report("maps", "img-src", "https://a.example.org"));
    service.record(
        new CspViolationService.Report(
            "maps", "app", "report", "img-src", "https://a.example.org", "d", null, null, null));
    service.record(report("dashboard", "img-src", "https://b.example.org"));

    CspViolationService.Counts maps = service.counts24h().get("maps");
    assertEquals(new CspViolationService.Counts(2, 1, 1, 2), maps);

    service.clear("maps");
    assertTrue(service.list("maps").isEmpty());
    assertEquals(1, service.list(null).size());
  }
}
