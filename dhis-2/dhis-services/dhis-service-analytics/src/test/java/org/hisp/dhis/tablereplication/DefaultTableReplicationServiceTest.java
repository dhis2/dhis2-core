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
package org.hisp.dhis.tablereplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests the incremental sync of the analytics database copy of the tracked entity attribute value
 * table in {@link DefaultTableReplicationService}.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class DefaultTableReplicationServiceTest {

  private static final String KEY = "keyLastSuccessfulTrackedEntityAttributeValueReplicaSync";

  private static final long OVERLAP_MILLIS = 30 * 60 * 1000L;

  @Mock private TableReplicationStore store;

  @Mock private SystemSettingsService settingsService;

  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private SqlBuilder sqlBuilder;

  @Mock private TransactionTemplate transactionTemplate;

  private boolean inTransaction;

  private DefaultTableReplicationService service;

  private final AnalyticsTableUpdateParams params =
      AnalyticsTableUpdateParams.newBuilder().build().withLatestPartition();

  @BeforeEach
  void setUp() {
    service =
        new DefaultTableReplicationService(
            store, settingsService, idObjectManager, sqlBuilder, transactionTemplate);
    lenient()
        .when(transactionTemplate.execute(any()))
        .thenAnswer(
            invocation -> {
              inTransaction = true;
              try {
                return invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null);
              } finally {
                inTransaction = false;
              }
            });
    lenient().when(sqlBuilder.supportsContinuousAnalytics()).thenReturn(true);
    lenient().when(store.tableExists(any())).thenReturn(true);
  }

  private static Program trackerProgram(String uid, boolean skipAnalytics) {
    TrackedEntityAttribute attribute = new TrackedEntityAttribute();
    attribute.setSkipAnalytics(skipAnalytics);
    Program program = new Program();
    program.setUid(uid);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setProgramAttributes(List.of(new ProgramTrackedEntityAttribute(program, attribute)));
    return program;
  }

  private void givenPrograms(Program... programs) {
    lenient().when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programs));
  }

  private Date givenWatermark() {
    SystemSettings settings = SystemSettings.of(Map.of(KEY, "2026-10-28T10:00:00.000"));
    lenient().when(settingsService.getCurrentSettings()).thenReturn(settings);
    return settings.getLastSuccessfulTrackedEntityAttributeValueReplicaSync();
  }

  private void givenChangedTrackedEntities(long count) {
    List<Long> ids = LongStream.rangeClosed(1, count).boxed().toList();
    when(store.getTrackedEntityIdsUpdatedSince(any(), anyInt())).thenReturn(ids);
  }

  @Test
  void programsAreReadInsideTransaction() {
    givenWatermark();
    givenChangedTrackedEntities(0);
    when(idObjectManager.getAllNoAcl(Program.class))
        .thenAnswer(
            invocation -> {
              assertTrue(inTransaction, "programs must be read inside a transaction");
              return List.of(trackerProgram("programA001", false));
            });

    assertEquals(ReplicaSyncOutcome.NO_CHANGES, service.syncTrackedEntityAttributeValue(params));
  }

  @Test
  void notSupportedEngineDoesNothing() {
    when(sqlBuilder.supportsContinuousAnalytics()).thenReturn(false);

    assertEquals(ReplicaSyncOutcome.NOT_SUPPORTED, service.syncTrackedEntityAttributeValue(params));
    verifyNoInteractions(store);
  }

  @Test
  void noTrackerProgramWithAttributesDoesNothing() {
    Program eventProgram = new Program();
    eventProgram.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    givenPrograms(eventProgram);

    assertEquals(
        ReplicaSyncOutcome.NO_ANALYZABLE_ATTRIBUTES,
        service.syncTrackedEntityAttributeValue(params));
    verifyNoInteractions(store);
  }

  @Test
  void programWithOnlyExcludedAttributesDoesNothing() {
    givenPrograms(trackerProgram("programA001", true));

    assertEquals(
        ReplicaSyncOutcome.NO_ANALYZABLE_ATTRIBUTES,
        service.syncTrackedEntityAttributeValue(params));
    verifyNoInteractions(store);
  }

  @Test
  void programWithOnlyConfidentialAttributesDoesNothing() {
    Program program = trackerProgram("programA001", false);
    program.getTrackedEntityAttributes().forEach(attribute -> attribute.setConfidential(true));
    givenPrograms(program);

    assertEquals(
        ReplicaSyncOutcome.NO_ANALYZABLE_ATTRIBUTES,
        service.syncTrackedEntityAttributeValue(params));
    verifyNoInteractions(store);
  }

  @Test
  void skippedProgramDoesNotCount() {
    givenPrograms(trackerProgram("programA001", false));
    AnalyticsTableUpdateParams skipping =
        AnalyticsTableUpdateParams.newBuilder()
            .skipPrograms(Set.of("programA001"))
            .build()
            .withLatestPartition();

    assertEquals(
        ReplicaSyncOutcome.NO_ANALYZABLE_ATTRIBUTES,
        service.syncTrackedEntityAttributeValue(skipping));
    verifyNoInteractions(store);
  }

  @Test
  void missingWatermarkDoesFullCopy() {
    givenPrograms(trackerProgram("programA001", false));
    when(settingsService.getCurrentSettings()).thenReturn(SystemSettings.of(Map.of()));

    assertEquals(ReplicaSyncOutcome.FULL, service.syncTrackedEntityAttributeValue(params));
    verify(store).replicateAnalyticsDatabaseTable(any());
    verify(store, never()).getTrackedEntityIdsUpdatedSince(any(), anyInt());
    verify(settingsService).put(eq(KEY), any(Date.class));
  }

  @Test
  void missingTableDoesFullCopy() {
    givenPrograms(trackerProgram("programA001", false));
    givenWatermark();
    when(store.tableExists(any())).thenReturn(false);

    assertEquals(ReplicaSyncOutcome.FULL, service.syncTrackedEntityAttributeValue(params));
    verify(store).replicateAnalyticsDatabaseTable(any());
    verify(settingsService).put(eq(KEY), any(Date.class));
  }

  @Test
  void noChangedTrackedEntitiesAdvancesWatermarkOnly() {
    givenPrograms(trackerProgram("programA001", false));
    givenWatermark();
    givenChangedTrackedEntities(0);

    assertEquals(ReplicaSyncOutcome.NO_CHANGES, service.syncTrackedEntityAttributeValue(params));
    verify(store, never()).deleteRows(any(), anyString(), anyList());
    verify(store, never()).replicateRows(any(), anyString(), anyList());
    verify(settingsService).put(eq(KEY), any(Date.class));
  }

  @Test
  void queriesFromWatermarkMinusOverlap() {
    givenPrograms(trackerProgram("programA001", false));
    Date watermark = givenWatermark();
    givenChangedTrackedEntities(0);

    service.syncTrackedEntityAttributeValue(params);

    verify(store)
        .getTrackedEntityIdsUpdatedSince(new Date(watermark.getTime() - OVERLAP_MILLIS), 200_001);
  }

  @Test
  void changesAtLimitSyncIncrementallyInBatches() {
    givenPrograms(trackerProgram("programA001", false));
    givenWatermark();
    givenChangedTrackedEntities(200_000);

    assertEquals(ReplicaSyncOutcome.INCREMENTAL, service.syncTrackedEntityAttributeValue(params));
    verify(store, times(40)).deleteRows(any(), eq("trackedentityid"), anyList());
    verify(store, times(40)).replicateRows(any(), eq("trackedentityid"), anyList());
    verify(store, never()).replicateAnalyticsDatabaseTable(any());
    InOrder order = inOrder(store, settingsService);
    order
        .verify(store)
        .deleteRows(any(), anyString(), eq(LongStream.rangeClosed(1, 5_000).boxed().toList()));
    order
        .verify(store)
        .replicateRows(any(), anyString(), eq(LongStream.rangeClosed(1, 5_000).boxed().toList()));
    order.verify(settingsService).put(eq(KEY), any(Date.class));
  }

  @Test
  void changesAboveLimitDoFullCopy() {
    givenPrograms(trackerProgram("programA001", false));
    givenWatermark();
    givenChangedTrackedEntities(200_001);

    assertEquals(ReplicaSyncOutcome.FULL, service.syncTrackedEntityAttributeValue(params));
    verify(store).replicateAnalyticsDatabaseTable(any());
    verify(store, never()).deleteRows(any(), anyString(), anyList());
    verify(settingsService).put(eq(KEY), any(Date.class));
  }

  @Test
  void batchFailureKeepsWatermark() {
    givenPrograms(trackerProgram("programA001", false));
    givenWatermark();
    givenChangedTrackedEntities(10_000);
    doNothing()
        .doThrow(new IllegalStateException("Doris unavailable"))
        .when(store)
        .replicateRows(any(), anyString(), anyList());

    assertThrows(
        IllegalStateException.class, () -> service.syncTrackedEntityAttributeValue(params));
    verify(settingsService, never()).put(eq(KEY), any());
  }

  @Test
  void fullReplicationRecordsWatermarkTakenBeforeCopy() {
    Date[] copyStarted = new Date[1];
    doAnswer(
            invocation -> {
              copyStarted[0] = new Date();
              Thread.sleep(5);
              return null;
            })
        .when(store)
        .replicateAnalyticsDatabaseTable(any());

    service.replicateTrackedEntityAttributeValue();

    ArgumentCaptor<Date> watermark = ArgumentCaptor.forClass(Date.class);
    verify(settingsService).put(eq(KEY), watermark.capture());
    assertFalse(watermark.getValue().after(copyStarted[0]));
    assertTrue(watermark.getValue().getTime() > 0);
  }
}
