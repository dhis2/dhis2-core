/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.analytics.AnalyticsTableType.ENROLLMENT;
import static org.hisp.dhis.analytics.AnalyticsTableType.EVENT;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.common.collection.CollectionUtils.emptyIfNull;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;
import static org.hisp.dhis.util.DateUtils.toLongDate;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.cache.OutliersCache;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.tablereplication.TableReplicationService;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service("org.hisp.dhis.analytics.AnalyticsTableGenerator")
@RequiredArgsConstructor
public class DefaultAnalyticsTableGenerator implements AnalyticsTableGenerator {
  private final List<AnalyticsTableService> analyticsTableServices;

  private final ResourceTableService resourceTableService;

  private final TableReplicationService tableReplicationService;

  private final SystemSettingsService settingsService;

  private final AnalyticsTableSettings settings;

  private final AnalyticsCache analyticsCache;

  private final OutliersCache outliersCache;

  @Override
  public void generateAnalyticsTables(AnalyticsTableUpdateParams params0, JobProgress progress) {
    final Clock clock = new Clock(log).startClock();
    final SystemSettings systemSettings = settingsService.getCurrentSettings();
    final Date lastSuccessfulUpdate = systemSettings.getLastSuccessfulAnalyticsTablesUpdate();
    final AnalyticsTableUpdateParams params =
        params0.toBuilder().lastSuccessfulUpdate(lastSuccessfulUpdate).build();
    final Set<AnalyticsTableType> skipTypes = emptyIfNull(params.getSkipTableTypes());

    log.info("Found analytics table types: {}", getAvailableTableTypes());
    log.info("Analytics table update params: {}", params);
    log.info("Last successful analytics table update: {}", toLongDate(lastSuccessfulUpdate));
    log.info("Analytics database: {}", settings.isAnalyticsDatabase());
    log.info("Skipping table types: {}", skipTypes);

    progress.startingProcess(
        "Analytics table update process{}", (params.isLatestUpdate() ? " (latest partition)" : ""));

    if (!params.isSkipResourceTables() && !params.isLatestUpdate()) {
      generateResourceTablesInternal(progress);

      if (settings.isAnalyticsDatabase()) {
        log.info("Replicating resource tables in analytics database");
        resourceTableService.replicateAnalyticsResourceTables();
      }
    }

    if (!params.isLatestUpdate() && settings.isAnalyticsDatabase()) {
      if (!skipTypes.containsAll(Set.of(EVENT, ENROLLMENT, TRACKED_ENTITY_INSTANCE))) {
        log.info("Replicating tracked entity attribute value table");
        tableReplicationService.replicateTrackedEntityAttributeValue();
      }
    }

    for (AnalyticsTableService service : analyticsTableServices) {
      AnalyticsTableType tableType = service.getAnalyticsTableType();
      if (!skipTypes.contains(tableType)) {
        service.create(params, progress);
      }
    }

    progress.startingStage("Updating system settings");
    progress.runStage(() -> updateLastSuccessfulSystemSettings(params, clock));

    progress.startingStage("Invalidate analytics caches", SKIP_STAGE);
    progress.runStage(analyticsCache::invalidateAll);
    progress.runStage(outliersCache::invalidateAll);
    clock.logTime("Analytics tables updated");
    progress.completedProcess("Analytics tables updated: {}", clock.time());
  }

  /**
   * Updates the system settings related to last successful analytics table update.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param clock the {@link Clock}.
   */
  private void updateLastSuccessfulSystemSettings(AnalyticsTableUpdateParams params, Clock clock) {
    if (params.isLatestUpdate()) {
      settingsService.put("keyLastSuccessfulLatestAnalyticsPartitionUpdate", params.getStartTime());
      settingsService.put("keyLastSuccessfulLatestAnalyticsPartitionRuntime", clock.time());
    } else {
      settingsService.put("keyLastSuccessfulAnalyticsTablesUpdate", params.getStartTime());
      settingsService.put("keyLastSuccessfulAnalyticsTablesRuntime", clock.time());
    }
  }

  @Override
  public void generateResourceTables(JobProgress progress) {
    final Clock clock = new Clock().startClock();

    progress.startingProcess("Generating resource tables");

    try {
      generateResourceTablesInternal(progress);

      progress.completedProcess("Resource tables generated: {}", clock.time());
    } catch (RuntimeException ex) {
      progress.failedProcess("Resource tables generation: {}", ex.getMessage());
      throw ex;
    }
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Generates resource tables.
   *
   * @param progress the {@link JobProgress}.
   */
  private void generateResourceTablesInternal(JobProgress progress) {
    resourceTableService.dropAllSqlViews(progress);

    Map<String, Runnable> generators = new LinkedHashMap<>();
    generators.put("Generating resource tables", resourceTableService::generateResourceTables);
    progress.startingStage("Generating resource tables", generators.size(), SKIP_STAGE);
    progress.runStage(generators);

    resourceTableService.createAllSqlViews(progress);

    settingsService.put("keyLastSuccessfulResourceTablesUpdate", new Date());
  }

  /**
   * Returns the available analytics table types.
   *
   * @return a set of {@link AnalyticsTableType}.
   */
  private Set<AnalyticsTableType> getAvailableTableTypes() {
    return analyticsTableServices.stream()
        .map(AnalyticsTableService::getAnalyticsTableType)
        .collect(Collectors.toSet());
  }
}
