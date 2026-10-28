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

import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTableReplicationService implements TableReplicationService {
  private static final String KEY_LAST_SYNC =
      "keyLastSuccessfulTrackedEntityAttributeValueReplicaSync";

  private static final String TRACKED_ENTITY_ID = "trackedentityid";

  /** Re-syncs tracked entities changed shortly before the last sync, see the design notes. */
  private static final Duration SYNC_OVERLAP = Duration.ofMinutes(30);

  private static final int BATCH_SIZE = 5_000;

  /** Above this number of changed tracked entities, the whole table is copied again instead. */
  private static final int MAX_INCREMENTAL_TRACKED_ENTITIES = 200_000;

  private static final Table TABLE_TRACKED_ENTIY_ATTRIBUTE_VALUE =
      new Table(
          "trackedentityattributevalue",
          List.of(
              new Column("trackedentityid", DataType.BIGINT, Nullable.NOT_NULL),
              new Column("trackedentityattributeid", DataType.BIGINT, Nullable.NOT_NULL),
              new Column("value", DataType.TEXT, Nullable.NULL)),
          List.of("trackedentityid", "trackedentityattributeid"));

  private final TableReplicationStore store;

  private final SystemSettingsService settingsService;

  private final IdentifiableObjectManager idObjectManager;

  private final SqlBuilder sqlBuilder;

  private final TransactionTemplate transactionTemplate;

  @Override
  public void replicateTrackedEntityAttributeValue() {
    Date syncStart = new Date();
    store.replicateAnalyticsDatabaseTable(TABLE_TRACKED_ENTIY_ATTRIBUTE_VALUE);
    settingsService.put(KEY_LAST_SYNC, syncStart);
  }

  @Override
  public ReplicaSyncOutcome syncTrackedEntityAttributeValue(AnalyticsTableUpdateParams params) {
    if (!sqlBuilder.supportsContinuousAnalytics()) {
      return ReplicaSyncOutcome.NOT_SUPPORTED;
    }

    // Program attributes are lazy collections, so read them inside a transaction; the analytics job
    // thread has no session of its own
    Boolean hasAttributes =
        transactionTemplate.execute(
            status -> hasProgramWithAnalyzableAttributes(params.getSkipPrograms()));

    if (!Boolean.TRUE.equals(hasAttributes)) {
      return ReplicaSyncOutcome.NO_ANALYZABLE_ATTRIBUTES;
    }

    Date lastSync =
        settingsService
            .getCurrentSettings()
            .getLastSuccessfulTrackedEntityAttributeValueReplicaSync();

    if (lastSync.getTime() == 0L || !store.tableExists(TABLE_TRACKED_ENTIY_ATTRIBUTE_VALUE)) {
      log.info("No previous sync of tracked entity attribute value copy, copying whole table");
      replicateTrackedEntityAttributeValue();
      return ReplicaSyncOutcome.FULL;
    }

    Clock clock = new Clock().startClock();
    Date syncStart = new Date();
    Date since = new Date(lastSync.getTime() - SYNC_OVERLAP.toMillis());
    List<Long> ids =
        store.getTrackedEntityIdsUpdatedSince(since, MAX_INCREMENTAL_TRACKED_ENTITIES + 1);

    if (ids.size() > MAX_INCREMENTAL_TRACKED_ENTITIES) {
      log.info(
          "More than {} tracked entities changed since {}, copying whole table",
          MAX_INCREMENTAL_TRACKED_ENTITIES,
          since);
      replicateTrackedEntityAttributeValue();
      return ReplicaSyncOutcome.FULL;
    }

    if (ids.isEmpty()) {
      settingsService.put(KEY_LAST_SYNC, syncStart);
      return ReplicaSyncOutcome.NO_CHANGES;
    }

    List<List<Long>> batches = Lists.partition(ids, BATCH_SIZE);

    for (List<Long> batch : batches) {
      store.deleteRows(TABLE_TRACKED_ENTIY_ATTRIBUTE_VALUE, TRACKED_ENTITY_ID, batch);
      store.replicateRows(TABLE_TRACKED_ENTIY_ATTRIBUTE_VALUE, TRACKED_ENTITY_ID, batch);
    }

    settingsService.put(KEY_LAST_SYNC, syncStart);

    log.info(
        "Synced tracked entity attribute value copy for {} tracked entities in {} batches: {}",
        ids.size(),
        batches.size(),
        clock.time());

    return ReplicaSyncOutcome.INCREMENTAL;
  }

  /**
   * Indicates whether any tracker program not skipped has tracked entity attributes which are part
   * of analytics, i.e. whether event analytics tables read the copy at all. Uses the same attribute
   * selection as the event analytics table attribute join.
   *
   * @param skipPrograms the uids of programs to skip.
   * @return true if at least one such program exists.
   */
  private boolean hasProgramWithAnalyzableAttributes(Set<String> skipPrograms) {
    return idObjectManager.getAllNoAcl(Program.class).stream()
        .filter(program -> !skipPrograms.contains(program.getUid()))
        .filter(Program::isRegistration)
        .anyMatch(program -> !program.getAnalyzableTrackedEntityAttributes().isEmpty());
  }
}
