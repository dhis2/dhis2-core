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
package org.hisp.dhis.dataintegrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Date;
import java.util.Set;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.external.location.DefaultLocationManager;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.test.cache.TestCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests that summary results are filtered by their age when read, and that results of any age are
 * given when no maximum age is set.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataIntegritySummaryMaxAgeTest {

  private static final String FRESH = "categories_no_options";

  private static final String OLD = "indicator_no_analysis";

  @Mock private CacheProvider cacheProvider;

  @Mock private I18nManager i18nManager;

  @Mock private I18n i18n;

  @Mock private DefaultLocationManager locationManager;

  @InjectMocks private DefaultDataIntegrityService service;

  private final Cache<DataIntegritySummary> summaryCache = new TestCache<>();

  @BeforeEach
  void setUp() {
    when(cacheProvider.<DataIntegritySummary>createDataIntegritySummaryCache())
        .thenReturn(summaryCache);
    when(cacheProvider.<DataIntegrityDetails>createDataIntegrityDetailsCache())
        .thenReturn(new TestCache<>());
    when(i18nManager.getI18n(DataIntegrityService.class)).thenReturn(i18n);
    service.init();

    summaryCache.put(FRESH, summaryFinishedHoursAgo(0));
    summaryCache.put(OLD, summaryFinishedHoursAgo(2));
  }

  @Test
  void testGetSummaries_DefaultMaxAgeHidesOldResults() {
    assertEquals(
        Set.of(FRESH), service.getSummaries(Set.of(FRESH, OLD), 0, Duration.ofHours(1)).keySet());
  }

  @Test
  void testGetSummaries_LargerMaxAgeGivesOldResults() {
    assertEquals(
        Set.of(FRESH, OLD),
        service.getSummaries(Set.of(FRESH, OLD), 0, Duration.ofHours(3)).keySet());
  }

  @Test
  void testGetSummaries_NoMaxAgeGivesAllResults() {
    assertEquals(Set.of(FRESH, OLD), service.getSummaries(Set.of(FRESH, OLD), 0, null).keySet());
  }

  @Test
  void testGetCompletedSummaryChecks_FilteredByMaxAge() {
    assertEquals(Set.of(FRESH), service.getCompletedSummaryChecks(Duration.ofHours(1)));
    assertEquals(Set.of(FRESH, OLD), service.getCompletedSummaryChecks(Duration.ofHours(3)));
    assertEquals(Set.of(FRESH, OLD), service.getCompletedSummaryChecks(null));
  }

  private static DataIntegritySummary summaryFinishedHoursAgo(int hours) {
    Date finished = new Date(System.currentTimeMillis() - Duration.ofHours(hours).toMillis());
    Date started = new Date(finished.getTime() - 1000);
    return new DataIntegritySummary(null, started, finished, null, 0, null);
  }
}
