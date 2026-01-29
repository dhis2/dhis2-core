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

  private static final int MAX_PARITIONS = 3;

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
      } else {
        Map<String, Object> lastPartition = outboxPartitions.get(0);

        if (isPartitionHalfFull(lastPartition)) {
          createNextPartition(
              outboxPartitions.get(0),
              eventHook,
              (long) lastPartition.get("upper_bound"),
              eventHookService.getPartitionRange());
        }

        if (outboxPartitions.size() > MAX_PARITIONS) {
          deleteOldestPartitions(outboxPartitions);
        }
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

  protected void deleteOldestPartitions(List<Map<String, Object>> partitions) {
    for (Map<String, Object> partition : partitions.subList(MAX_PARITIONS, partitions.size())) {
      jdbcTemplate.execute(
          String.format("DROP TABLE IF EXISTS %s", partition.get("partition_name")));
    }
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
