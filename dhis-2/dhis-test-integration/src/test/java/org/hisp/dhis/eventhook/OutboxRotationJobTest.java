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
package org.hisp.dhis.eventhook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutboxRotationJobTest extends PostgresIntegrationTestBase {

  @Autowired private OutboxRotationJob outboxRotationJob;

  @Autowired private EventHookService eventHookService;

  @Autowired private EventHookStore eventHookStore;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void beforeEach() {
    eventHookService.setPartitionRange(10);
  }

  @Test
  void testExecuteAddsEmptyPartitionWhenPartitionsAreMissing() {
    EventHook eventHook = new EventHook();
    Source source = new Source();
    eventHook.setName("Foobar");
    eventHook.setSource(source);
    eventHook.setTargets(List.of(new WebhookTarget()));

    eventHookStore.save(eventHook);
    assertEquals(0, outboxRotationJob.getOutboxPartitions(eventHook).size());

    outboxRotationJob.execute(null, JobProgress.noop());

    List<Map<String, Object>> outboxPartitions = outboxRotationJob.getOutboxPartitions(eventHook);
    assertEquals(EventHookService.MIN_PARTITIONS, outboxPartitions.size());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_2\"",
        outboxPartitions.get(0).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_1\"",
        outboxPartitions.get(1).get("partition_name").toString());
  }

  @Test
  void testExecuteAddsEmptyPartitionsWhenLastPartitionIsNotEmpty() {
    EventHook eventHook = new EventHook();
    Source source = new Source();
    eventHook.setName("Bar");
    eventHook.setSource(source);
    eventHook.setTargets(List.of(new WebhookTarget()));

    eventHookStore.save(eventHook);
    eventHookService.createOutbox(eventHook);

    int outboxMessageCount =
        (eventHookService.getPartitionRange() * (EventHookService.MIN_PARTITIONS - 1));
    addOutboxMessages(eventHook, outboxMessageCount);
    EventHookOutboxLog eventHookOutboxLog = new EventHookOutboxLog();
    eventHookOutboxLog.setNextOutboxMessageId(outboxMessageCount);
    eventHookOutboxLog.setOutboxTableName(
        EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID());
    eventHookOutboxLog.setEventHook(eventHook);
    entityManager.merge(eventHookOutboxLog);

    assertEquals(
        EventHookService.MIN_PARTITIONS, outboxRotationJob.getOutboxPartitions(eventHook).size());
    outboxRotationJob.execute(null, JobProgress.noop());

    List<Map<String, Object>> outboxPartitions = outboxRotationJob.getOutboxPartitions(eventHook);
    assertEquals(EventHookService.MIN_PARTITIONS + 1, outboxPartitions.size());
    assertEquals(
        "\"outbox_" + eventHook.getUid() + "_4\"",
        outboxPartitions.get(0).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUid() + "_3\"",
        outboxPartitions.get(1).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUid() + "_2\"",
        outboxPartitions.get(2).get("partition_name").toString());
  }

  @Test
  void testExecuteDoesNotAddEmptyPartitionsWhenLastPartitionIsEmpty() {
    EventHook eventHook = new EventHook();
    Source source = new Source();
    eventHook.setName("Quuz");
    eventHook.setSource(source);
    eventHook.setTargets(List.of(new WebhookTarget()));

    eventHookStore.save(eventHook);
    eventHookService.createOutbox(eventHook);

    int outboxMessageCount =
        (eventHookService.getPartitionRange() * (EventHookService.MIN_PARTITIONS - 1)) - 1;
    addOutboxMessages(eventHook, outboxMessageCount);
    EventHookOutboxLog eventHookOutboxLog = new EventHookOutboxLog();
    eventHookOutboxLog.setNextOutboxMessageId(outboxMessageCount);
    eventHookOutboxLog.setOutboxTableName(
        EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID());
    eventHookOutboxLog.setEventHook(eventHook);
    entityManager.merge(eventHookOutboxLog);

    assertEquals(
        EventHookService.MIN_PARTITIONS, outboxRotationJob.getOutboxPartitions(eventHook).size());
    outboxRotationJob.execute(null, JobProgress.noop());

    List<Map<String, Object>> outboxPartitions = outboxRotationJob.getOutboxPartitions(eventHook);
    assertEquals(2, outboxPartitions.size());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_2\"",
        outboxPartitions.get(0).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_1\"",
        outboxPartitions.get(1).get("partition_name").toString());
  }

  @Test
  void testExecutePrunesExpiredPartitions() {
    EventHook eventHook = new EventHook();
    Source source = new Source();
    eventHook.setName("Foo");
    eventHook.setSource(source);
    eventHook.setTargets(List.of(new WebhookTarget()));

    eventHookStore.save(eventHook);
    eventHookService.createOutbox(eventHook);

    assertEquals(2, outboxRotationJob.getOutboxPartitions(eventHook).size());

    addOutboxMessages(eventHook, eventHookService.getPartitionRange());
    EventHookOutboxLog eventHookOutboxLog = new EventHookOutboxLog();
    eventHookOutboxLog.setNextOutboxMessageId(eventHookService.getPartitionRange());
    eventHookOutboxLog.setOutboxTableName(
        EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID());
    eventHookOutboxLog.setEventHook(eventHook);
    entityManager.merge(eventHookOutboxLog);

    outboxRotationJob.execute(null, JobProgress.noop());

    List<Map<String, Object>> outboxPartitions = outboxRotationJob.getOutboxPartitions(eventHook);
    assertEquals(3, outboxPartitions.size());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_4\"",
        outboxPartitions.get(0).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_3\"",
        outboxPartitions.get(1).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_2\"",
        outboxPartitions.get(2).get("partition_name").toString());
  }

  @Test
  void testExecuteDoesNotPruneEmptyPartitions() {
    EventHook eventHook = new EventHook();
    Source source = new Source();
    eventHook.setName("Foo");
    eventHook.setSource(source);
    eventHook.setTargets(List.of(new WebhookTarget()));

    eventHookStore.save(eventHook);
    eventHookService.createOutbox(eventHook);

    assertEquals(2, outboxRotationJob.getOutboxPartitions(eventHook).size());

    EventHookOutboxLog eventHookOutboxLog = new EventHookOutboxLog();
    eventHookOutboxLog.setNextOutboxMessageId(eventHookService.getPartitionRange());
    eventHookOutboxLog.setOutboxTableName(
        EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID());
    eventHookOutboxLog.setEventHook(eventHook);
    entityManager.merge(eventHookOutboxLog);

    outboxRotationJob.execute(null, JobProgress.noop());

    List<Map<String, Object>> outboxPartitions = outboxRotationJob.getOutboxPartitions(eventHook);
    assertEquals(2, outboxPartitions.size());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_2\"",
        outboxPartitions.get(0).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_1\"",
        outboxPartitions.get(1).get("partition_name").toString());
  }

  @Test
  void testExecuteDoesNotPrunePartitionsWhenItHasUndeliveredOutboxMessages() {
    EventHook eventHook = new EventHook();
    Source source = new Source();
    eventHook.setName("FooBarQuuz");
    eventHook.setSource(source);
    eventHook.setTargets(List.of(new WebhookTarget()));

    eventHookStore.save(eventHook);
    eventHookService.createOutbox(eventHook);

    addOutboxMessages(eventHook, eventHookService.getPartitionRange());
    outboxRotationJob.execute(null, JobProgress.noop());
    addOutboxMessages(eventHook, eventHookService.getPartitionRange());
    outboxRotationJob.execute(null, JobProgress.noop());
    addOutboxMessages(eventHook, eventHookService.getPartitionRange());
    outboxRotationJob.execute(null, JobProgress.noop());
    addOutboxMessages(eventHook, eventHookService.getPartitionRange());
    outboxRotationJob.execute(null, JobProgress.noop());

    assertEquals(6, outboxRotationJob.getOutboxPartitions(eventHook).size());
    List<Map<String, Object>> outboxPartitions = outboxRotationJob.getOutboxPartitions(eventHook);
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_6\"",
        outboxPartitions.get(0).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_5\"",
        outboxPartitions.get(1).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_4\"",
        outboxPartitions.get(2).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_3\"",
        outboxPartitions.get(3).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_2\"",
        outboxPartitions.get(4).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_1\"",
        outboxPartitions.get(5).get("partition_name").toString());

    EventHookOutboxLog eventHookOutboxLog = new EventHookOutboxLog();
    eventHookOutboxLog.setNextOutboxMessageId(39);
    eventHookOutboxLog.setOutboxTableName(
        EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID());
    eventHookOutboxLog.setEventHook(eventHook);
    entityManager.merge(eventHookOutboxLog);

    outboxRotationJob.execute(null, JobProgress.noop());

    outboxPartitions = outboxRotationJob.getOutboxPartitions(eventHook);
    assertEquals(3, outboxPartitions.size());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_6\"",
        outboxPartitions.get(0).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_5\"",
        outboxPartitions.get(1).get("partition_name").toString());
    assertEquals(
        "\"outbox_" + eventHook.getUID() + "_4\"",
        outboxPartitions.get(2).get("partition_name").toString());
  }

  @Test
  void testExecuteDoesNotAddEmptyPartitionsWhenMaxPartitionsReached() {
    EventHook eventHook = new EventHook();
    Source source = new Source();
    eventHook.setName("BarQuuz");
    eventHook.setSource(source);
    eventHook.setTargets(List.of(new WebhookTarget()));

    eventHookStore.save(eventHook);
    eventHookService.createOutbox(eventHook);

    for (int i = 0; i < EventHookService.MAX_PARTITIONS; i++) {
      int outboxMessageCount = eventHookService.getPartitionRange();
      if (i == EventHookService.MAX_PARTITIONS - 1) {
        outboxMessageCount--;
      }
      addOutboxMessages(eventHook, outboxMessageCount);
      outboxRotationJob.execute(null, JobProgress.noop());
    }

    assertEquals(
        EventHookService.MAX_PARTITIONS, outboxRotationJob.getOutboxPartitions(eventHook).size());
  }

  private void addOutboxMessages(EventHook eventHook, int outboxMessageCount) {
    List<String> inserts = new ArrayList<>();
    for (int i = 0; i < outboxMessageCount; i++) {
      inserts.add(
          String.format(
              "INSERT INTO \"outbox_%s\"" + " (payload) VALUES ('{}'::JSONB)", eventHook.getUID()));
    }
    jdbcTemplate.batchUpdate(inserts.toArray(new String[] {}));
    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();
    jdbcTemplate.execute(String.format("ANALYZE \"outbox_%s\"", eventHook.getUID()));
  }
}
