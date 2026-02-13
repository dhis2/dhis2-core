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
      } else if (outboxPartitions.size() < EventHookService.MAX_PARTITIONS) {
        Map<String, Object> lastPartition = outboxPartitions.get(0);

        if (outboxPartitions.size() < EventHookService.MIN_PARTITIONS
            || isPartitionNotEmpty(lastPartition)) {
          addEmptyPartitions(
              outboxPartitions.get(0), eventHook, (long) lastPartition.get("upper_bound"));
        }
      }
      prunePartitions(getOutboxPartitions(eventHook), eventHook);
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
    if (partitions.size() > 1) {
      EventHookOutboxLog eventHookOutboxLog =
          entityManager.find(
              EventHookOutboxLog.class,
              EventHookService.OUTBOX_PREFIX_TABLE_NAME + eventHook.getUID());
      for (int i = partitions.size() - 1; i > 0; i--) {
        Map<String, Object> candidatePartition = partitions.get(i);
        Map<String, Object> lastOutboxMessage = getLastOutboxMessage(candidatePartition);
        if (isExpired(eventHookOutboxLog, lastOutboxMessage)) {
          jdbcTemplate.execute(
              String.format("DROP TABLE IF EXISTS %s", candidatePartition.get("partition_name")));
        } else {
          if (eventHookOutboxLog != null
              && lastOutboxMessage != null
              && partitions.size() >= EventHookService.MAX_PARTITIONS) {
            log.error(
                String.format(
                    "Undelivered outbox message [{}] for event hook [{}] is causing the event backlog to grow. "
                        + "New events will be discarded when backlog limit is reached. "
                        + "Estimated total no. of undelivered events: %.0f. "
                        + "Estimated no. of future events that can be retained: %.0f. "
                        + "Hint: is event hook subscriber lagging behind? Consider disabling the event hook if the issue persists",
                    calcBacklogCount(partitions.subList(0, i + 1), eventHookOutboxLog),
                    calcRemainingEvents(partitions.subList(0, i + 1))),
                eventHookOutboxLog.getNextOutboxMessageId(),
                eventHook.getName());
          }
          break;
        }
      }
    }
  }

  protected float calcRemainingEvents(List<Map<String, Object>> partitions) {
    return partitions.stream()
        .map(
            p ->
                ((long) p.get("upper_bound") - (long) p.get("lower_bound"))
                    - Math.max(((float) p.get("reltuples")), 0))
        .reduce((float) 0, Float::sum);
  }

  protected float calcBacklogCount(
      List<Map<String, Object>> partitions, EventHookOutboxLog eventHookOutboxLog) {
    Float undeliveredOutboxMessageCount =
        partitions.stream()
            .map(p -> Math.max(((float) p.get("reltuples")), 0))
            .reduce((float) 0, Float::sum);

    return undeliveredOutboxMessageCount
        + ((long) partitions.get(partitions.size() - 1).get("upper_bound")
            - eventHookOutboxLog.getNextOutboxMessageId());
  }

  protected boolean isExpired(
      EventHookOutboxLog eventHookOutboxLog, Map<String, Object> lastOutboxMessage) {
    if (eventHookOutboxLog != null) {
      return lastOutboxMessage != null
          && ((long) lastOutboxMessage.get("id")) < eventHookOutboxLog.getNextOutboxMessageId();
    } else {
      // should not happen
      return true;
    }
  }

  protected void addEmptyPartitions(
      Map<String, Object> partition, EventHook eventHook, long lastPartitionUpperBound) {

    String partitionOutboxTableName = partition.get("partition_name").toString();
    long lastPartitionIndex =
        Long.parseLong(
            partitionOutboxTableName.substring(
                partitionOutboxTableName.lastIndexOf("_") + 1,
                partitionOutboxTableName.length() - 1));
    long nextPartitionIndex = lastPartitionIndex + 1;

    long newLastPartitionLowerBound = lastPartitionUpperBound;
    long newLastPartitionUpperBound =
        lastPartitionUpperBound + eventHookService.getPartitionRange();
    for (long i = nextPartitionIndex;
        i < (nextPartitionIndex + EventHookService.MIN_PARTITIONS);
        i++) {
      eventHookService.addOutboxPartition(
          eventHook, i, newLastPartitionLowerBound, newLastPartitionUpperBound);
      newLastPartitionLowerBound = newLastPartitionUpperBound;
      newLastPartitionUpperBound =
          newLastPartitionLowerBound + eventHookService.getPartitionRange();
    }
  }

  protected boolean isPartitionNotEmpty(Map<String, Object> partition) {
    return ((float) partition.get("reltuples")) > 0;
  }

  protected Map<String, Object> getLastOutboxMessage(Map<String, Object> partition) {
    List<Map<String, Object>> outboxMessages =
        jdbcTemplate.queryForList(
            String.format(
                "SELECT * FROM %s ORDER BY id DESC LIMIT 1", partition.get("partition_name")));

    return outboxMessages.isEmpty() ? null : outboxMessages.get(0);
  }
}
