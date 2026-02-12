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

import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobEntry;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class OutboxRotationJob implements Job {

  protected static final int MIN_PARITIONS = 2;
  protected static final int MAX_PARTITIONS = 10;

  private final EntityManager entityManager;
  private final EventHookService eventHookService;
  private final JdbcTemplate jdbcTemplate;

  @Override
  public JobType getJobType() {
    return JobType.OUTBOX_ROTATION;
  }

  @Override
  public void execute(JobEntry config, JobProgress progress) {
    for (EventHook eventHook : eventHookService.getAll()) {
      List<Map<String, Object>> outboxPartitions = getOutboxPartitions(eventHook);
      if (outboxPartitions.isEmpty()) {
        eventHookService.createOutbox(eventHook);
      } else if (outboxPartitions.size() < MAX_PARTITIONS) {
        Map<String, Object> lastPartition = outboxPartitions.get(0);

        if (isPartitionHalfFull(lastPartition)) {
          createNextPartition(
              outboxPartitions.get(0),
              eventHook,
              (long) lastPartition.get("upper_bound"),
              eventHookService.getPartitionRange());
        }

        prunePartitions(outboxPartitions, eventHook);
      }
    }
  }

  protected List<Map<String, Object>> getOutboxPartitions(EventHook eventHook) {
    return jdbcTemplate.queryForList(
        String.format(
            "SELECT "
                + "pg_inherits.inhparent::regclass AS table_name, "
                + "pg_inherits.inhrelid::regclass AS partition_name, "
                + "pg_class.reltuples, "
                + "(regexp_matches(pg_get_expr(pg_class.relpartbound, pg_class.oid), '.*\\(\\''?(.*?)\\''?\\).*\\(\\''?(.*?)\\''?\\).*'))[1]::BIGINT AS lower_bound, "
                + "(regexp_matches(pg_get_expr(pg_class.relpartbound, pg_class.oid), '.*\\(\\''?(.*?)\\''?\\).*\\(\\''?(.*?)\\''?\\).*'))[2]::BIGINT AS upper_bound "
                + "FROM pg_inherits "
                + "JOIN pg_class ON pg_inherits.inhrelid = pg_class.oid "
                + "WHERE pg_class.relkind = 'r' AND pg_inherits.inhparent = to_regclass(quote_ident('%s')) "
                + "ORDER BY partition_name DESC",
            EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID()));
  }

  protected void prunePartitions(List<Map<String, Object>> partitions, EventHook eventHook) {
    if (partitions.size() > MIN_PARITIONS) {
      EventHookOutboxLog eventHookOutboxLog =
          entityManager.find(
              EventHookOutboxLog.class,
              EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID());
      for (int i = partitions.size() - 1; i > (MIN_PARITIONS - 1); i--) {
        Map<String, Object> candidatePartition = partitions.get(i);
        Map<String, Object> nextOutboxMessage =
            getNextOutboxMessage(eventHookOutboxLog, candidatePartition);
        if (nextOutboxMessage == null) {
          jdbcTemplate.execute(
              String.format("DROP TABLE IF EXISTS %s", candidatePartition.get("partition_name")));
        } else {
          log.warn(
              String.format(
                  "Undelivered outbox message [{}] for event hook [{}] is causing the event backlog to grow. "
                      + "New events will be discarded when backlog limit is reached. "
                      + "Estimated total no. of undelivered events: %.0f. "
                      + "Estimated no. of future events that can be retained: %.0f. "
                      + "Hint: is event hook subscriber unreachable, returning HTTP errors, or consuming too slowly?",
                  calcBacklogCount(partitions, candidatePartition, i, eventHookOutboxLog),
                  calcRemainingEvents(partitions)),
              eventHookOutboxLog.getNextOutboxMessageId(),
              eventHook.getName());
          break;
        }
      }
    }
  }

  protected float calcRemainingEvents(List<Map<String, Object>> partitions) {
    Map<String, Object> lastPartition = partitions.get(0);
    Map<String, Object> nextToLastPartition = partitions.get(1);
    return ((MAX_PARTITIONS - partitions.size()) * eventHookService.getPartitionRange())
        + ((((long) lastPartition.get("upper_bound") - (long) lastPartition.get("lower_bound"))
                - (float) nextToLastPartition.get("reltuples"))
            + (((long) nextToLastPartition.get("upper_bound")
                    - (long) nextToLastPartition.get("lower_bound"))
                - (float) nextToLastPartition.get("reltuples")));
  }

  protected float calcBacklogCount(
      List<Map<String, Object>> partitions,
      Map<String, Object> candidatePartition,
      int i,
      EventHookOutboxLog eventHookOutboxLog) {
    Float undeliveredOutboxMessageCount =
        partitions.subList(0, i).stream()
            .map(p -> (float) p.get("reltuples"))
            .reduce((float) 0, Float::sum);

    return undeliveredOutboxMessageCount
        + ((long) candidatePartition.get("upper_bound")
            - eventHookOutboxLog.getNextOutboxMessageId());
  }

  protected Map<String, Object> getNextOutboxMessage(
      EventHookOutboxLog eventHookOutboxLog, Map<String, Object> partition) {
    List<Map<String, Object>> nextOutboxMessage;
    if (eventHookOutboxLog != null) {
      nextOutboxMessage =
          jdbcTemplate.queryForList(
              String.format("SELECT id FROM %s WHERE id = ?", partition.get("partition_name")),
              eventHookOutboxLog.getNextOutboxMessageId());
    } else {
      nextOutboxMessage = Collections.emptyList();
    }

    return nextOutboxMessage.isEmpty() ? null : nextOutboxMessage.get(0);
  }

  protected void createNextPartition(
      Map<String, Object> partition,
      EventHook eventHook,
      long lastPartitionUpperBound,
      int nextPartitionUpperBound) {
    String partitionOutboxTableName = partition.get("partition_name").toString();
    long lastPartitionIndex =
        Long.parseLong(
            partitionOutboxTableName.substring(
                partitionOutboxTableName.lastIndexOf("_") + 1,
                partitionOutboxTableName.length() - 1));
    long nextPartitionIndex = lastPartitionIndex + 1;
    eventHookService.addOutboxPartition(
        eventHook,
        nextPartitionIndex,
        lastPartitionUpperBound,
        lastPartitionUpperBound + nextPartitionUpperBound);
  }

  protected boolean isPartitionHalfFull(Map<String, Object> partition) {
    long partitionRange = (long) partition.get("upper_bound") - (long) partition.get("lower_bound");
    return ((float) partition.get("reltuples")) >= ((float) partitionRange / 2);
  }
}
