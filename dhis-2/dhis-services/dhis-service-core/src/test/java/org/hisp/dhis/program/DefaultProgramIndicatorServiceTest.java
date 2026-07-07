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
package org.hisp.dhis.program;

import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.CacheType;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.setting.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultProgramIndicatorServiceTest {
  private static final String EXPRESSION = "A + B";

  private final RecordingCache analyticsSqlCache = new RecordingCache();

  private DefaultProgramIndicatorService service;

  private ProgramIndicator programIndicator;

  @Mock private ProgramIndicatorStore programIndicatorStore;

  @Mock private IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore;

  @Mock private ProgramStageService programStageService;

  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private ExpressionService expressionService;

  @Mock private DimensionService dimensionService;

  @Mock private I18nManager i18nManager;

  @Mock private CacheProvider cacheProvider;

  @Mock private SqlBuilder sqlBuilder;

  @Mock private SystemSettingsService settingsService;

  @BeforeEach
  void setUp() {
    when(cacheProvider.<String>createAnalyticsSqlCache()).thenReturn(analyticsSqlCache);

    service =
        new DefaultProgramIndicatorService(
            programIndicatorStore,
            programIndicatorGroupStore,
            programStageService,
            idObjectManager,
            expressionService,
            dimensionService,
            i18nManager,
            cacheProvider,
            sqlBuilder,
            settingsService);

    programIndicator = new ProgramIndicator();
    programIndicator.setUid("ProgramIndA");
  }

  @Test
  void cacheKeyIncludesRenderingModes() {
    service.getAnalyticsSql(EXPRESSION, NUMERIC, programIndicator, null, null);
    service.getAnalyticsSqlAllowingNulls(EXPRESSION, NUMERIC, programIndicator, null, null);
    service.getAnalyticsSqlDeferRelationshipCount(
        EXPRESSION, NUMERIC, programIndicator, null, null, "subax");

    List<String> keys = analyticsSqlCache.keysRequested();

    assertEquals(3, keys.size());
    assertNotEquals(keys.get(0), keys.get(1));
    assertNotEquals(keys.get(0), keys.get(2));
    assertNotEquals(keys.get(1), keys.get(2));

    assertTrue(keys.get(0).contains("|nullHandling=REPLACE_NULLS|"));
    assertTrue(keys.get(0).contains("|relationshipCountRendering=EXPAND_CORRELATED"));

    assertTrue(keys.get(1).contains("|nullHandling=PRESERVE_NULLS|"));
    assertTrue(keys.get(1).contains("|relationshipCountRendering=EXPAND_CORRELATED"));

    assertTrue(keys.get(2).contains("|tableAlias=subax|"));
    assertTrue(keys.get(2).contains("|nullHandling=REPLACE_NULLS|"));
    assertTrue(keys.get(2).contains("|relationshipCountRendering=DEFER_PLACEHOLDER"));
  }

  @Test
  void cacheKeyDistinguishesStartDateFromEndDate() {
    Date date = new Date(123456789L);

    service.getAnalyticsSql(EXPRESSION, NUMERIC, programIndicator, date, null);
    service.getAnalyticsSql(EXPRESSION, NUMERIC, programIndicator, null, date);

    List<String> keys = analyticsSqlCache.keysRequested();

    assertEquals(2, keys.size());
    assertNotEquals(keys.get(0), keys.get(1));

    assertTrue(keys.get(0).contains("|startDate=123456789|endDate=|"));
    assertTrue(keys.get(1).contains("|startDate=|endDate=123456789|"));
  }

  private static class RecordingCache implements Cache<String> {
    private final List<String> keysRequested = new ArrayList<>();

    private List<String> keysRequested() {
      return keysRequested;
    }

    @Override
    public Optional<String> getIfPresent(String key) {
      return Optional.empty();
    }

    @Override
    public Optional<String> get(String key) {
      return Optional.empty();
    }

    @Override
    public String get(String key, Function<String, String> mappingFunction) {
      keysRequested.add(key);
      return "cached";
    }

    @Override
    public Stream<String> getAll() {
      return Stream.empty();
    }

    @Override
    public Iterable<String> keys() {
      return keysRequested;
    }

    @Override
    public void put(String key, String value) {}

    @Override
    public void put(String key, String value, long ttlInSeconds) {}

    @Override
    public boolean putIfAbsent(String key, String value) {
      return false;
    }

    @Override
    public void invalidate(String key) {}

    @Override
    public void invalidateAll() {
      keysRequested.clear();
    }

    @Override
    public CacheType getCacheType() {
      return CacheType.IN_MEMORY;
    }
  }
}
