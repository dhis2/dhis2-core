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
package org.hisp.dhis.analytics.table;

import static org.hisp.dhis.analytics.AnalyticsTableType.DATA_VALUE;
import static org.hisp.dhis.analytics.AnalyticsTableType.ENROLLMENT;
import static org.hisp.dhis.analytics.AnalyticsTableType.EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.cache.OutliersCache;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.tablereplication.TableReplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link DefaultAnalyticsTableGenerator}, in particular the per-{@link
 * org.hisp.dhis.analytics.AnalyticsTableType} update-timestamp settings added for DHIS2-21992
 * (Phase 1: write path only).
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class DefaultAnalyticsTableGeneratorTest {

  @Mock private AnalyticsTableService dataValueTableService;
  @Mock private AnalyticsTableService eventTableService;
  @Mock private ResourceTableService resourceTableService;
  @Mock private TableReplicationService tableReplicationService;
  @Mock private SystemSettingsService settingsService;
  @Mock private AnalyticsTableSettings analyticsTableSettings;
  @Mock private AnalyticsCache analyticsCache;
  @Mock private OutliersCache outliersCache;

  private DefaultAnalyticsTableGenerator generator;

  private void setUp() {
    when(dataValueTableService.getAnalyticsTableType()).thenReturn(DATA_VALUE);
    when(eventTableService.getAnalyticsTableType()).thenReturn(EVENT);
    when(settingsService.getCurrentSettings()).thenReturn(SystemSettings.of(Map.of()));
    generator =
        new DefaultAnalyticsTableGenerator(
            List.of(dataValueTableService, eventTableService),
            resourceTableService,
            tableReplicationService,
            settingsService,
            analyticsTableSettings,
            analyticsCache,
            outliersCache);
  }

  @Test
  void skippedTypeGetsNoPerTypeLatestPartitionKey_processedTypeDoes() {
    setUp();
    Date startTime = new Date();
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .skipResourceTables(true)
            .skipTableTypes(Set.of(EVENT))
            .startTime(startTime)
            .build()
            .withLatestPartition();

    generator.generateAnalyticsTables(params, JobProgress.noop());

    verify(eventTableService, never()).create(any(), any());
    verify(dataValueTableService).create(any(), any());

    verify(settingsService, never())
        .put(eq(SystemSettings.keyLastSuccessfulLatestAnalyticsPartitionUpdate(EVENT)), any());
    verify(settingsService)
        .put(
            eq(SystemSettings.keyLastSuccessfulLatestAnalyticsPartitionUpdate(DATA_VALUE)),
            eq(startTime));
  }

  @Test
  void skippedTypeGetsNoPerTypeFullUpdateKey_processedTypeDoes() {
    setUp();
    Date startTime = new Date();
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .skipResourceTables(true)
            .skipTableTypes(Set.of(EVENT))
            .startTime(startTime)
            .build();

    generator.generateAnalyticsTables(params, JobProgress.noop());

    verify(settingsService, never())
        .put(eq(SystemSettings.keyLastSuccessfulAnalyticsTablesUpdate(EVENT)), any());
    verify(settingsService)
        .put(eq(SystemSettings.keyLastSuccessfulAnalyticsTablesUpdate(DATA_VALUE)), eq(startTime));
  }

  @Test
  void typeWithoutLatestPartitionSupportGetsNoPerTypeKeyEvenWhenProcessed() {
    when(dataValueTableService.getAnalyticsTableType()).thenReturn(ENROLLMENT);
    when(settingsService.getCurrentSettings()).thenReturn(SystemSettings.of(Map.of()));
    generator =
        new DefaultAnalyticsTableGenerator(
            List.of(dataValueTableService),
            resourceTableService,
            tableReplicationService,
            settingsService,
            analyticsTableSettings,
            analyticsCache,
            outliersCache);
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().skipResourceTables(true).build();

    generator.generateAnalyticsTables(params, JobProgress.noop());

    verify(dataValueTableService).create(any(), any());
    // ENROLLMENT has no registered per-type key (AnalyticsTableType.isLatestPartition() == false)
    // - writing it would be silently dropped by SystemSettingsService.put(), so it must not be
    // attempted at all.
    verify(settingsService, never())
        .put(eq("keyLastSuccessfulAnalyticsTablesUpdateEnrollment"), any());
  }

  @Test
  void legacyLatestPartitionKeysStillWrittenUnconditionally_regressionGuard() {
    setUp();
    Date startTime = new Date();
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .skipResourceTables(true)
            .skipTableTypes(Set.of(EVENT, DATA_VALUE))
            .startTime(startTime)
            .build()
            .withLatestPartition();

    generator.generateAnalyticsTables(params, JobProgress.noop());

    verify(settingsService)
        .put(eq("keyLastSuccessfulLatestAnalyticsPartitionUpdate"), eq(startTime));
    verify(settingsService).put(eq("keyLastSuccessfulLatestAnalyticsPartitionRuntime"), any());
  }

  @Test
  void legacyFullUpdateKeysStillWrittenUnconditionally_regressionGuard() {
    setUp();
    Date startTime = new Date();
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .skipResourceTables(true)
            .skipTableTypes(Set.of(EVENT, DATA_VALUE))
            .startTime(startTime)
            .build();

    generator.generateAnalyticsTables(params, JobProgress.noop());

    verify(settingsService).put(eq("keyLastSuccessfulAnalyticsTablesUpdate"), eq(startTime));
    verify(settingsService).put(eq("keyLastSuccessfulAnalyticsTablesRuntime"), any());
  }
}
