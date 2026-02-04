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

  private final EventHookService eventHookService;
  private final JdbcTemplate jdbcTemplate;

  @Override
  public JobType getJobType() {
    return JobType.OUTBOX_ROTATION;
  }

  @Override
  public void execute(JobEntry config, JobProgress progress) {
    for (EventHook eventHook : eventHookService.getAll()) {
      List<Map<String, Object>> partitions =
          jdbcTemplate.queryForList(
              String.format(
                  "SELECT relname, n_live_tup FROM pg_stat_user_tables WHERE relname LIKE '%s\\_%%' ORDER BY relname DESC",
                  "outbox\\_" + eventHook.getUid()));
      if (!partitions.isEmpty()) {
        int partitionRange = eventHookService.getPartitionRange();

        if (((long) partitions.get(0).get("n_live_tup")) > (partitionRange / 2)) {
          String partitionOutboxTableName = partitions.get(0).get("relname").toString();
          long lastPartitionIndex =
              Long.parseLong(
                  partitionOutboxTableName.substring(
                      partitionOutboxTableName.lastIndexOf("_") + 1));
          long nextPartitionIndex = lastPartitionIndex + 1;
          eventHookService.addOutboxPartition(
              eventHook,
              nextPartitionIndex,
              nextPartitionIndex * partitionRange,
              (nextPartitionIndex * partitionRange) + partitionRange);
        }

        if (partitions.size() > 2) {
          for (Map<String, Object> partition : partitions.subList(2, partitions.size())) {
            jdbcTemplate.execute(String.format("DROP TABLE \"%s\"", partition.get("relname")));
          }
        }
      }
    }
  }
}
