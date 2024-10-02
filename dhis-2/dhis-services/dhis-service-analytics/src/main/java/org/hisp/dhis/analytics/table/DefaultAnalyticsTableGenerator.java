/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import static java.util.Map.entry;
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
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.Settings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.util.Clock;
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

  private final SystemSettingsService settingsService;

  private final AnalyticsCache analyticsCache;

  private final OutliersCache outliersCache;

  // TODO introduce last successful timestamps per table type

  @Override
  public void generateAnalyticsTables(AnalyticsTableUpdateParams params0, JobProgress progress) {
    Clock clock = new Clock(log).startClock();
    Date lastSuccessfulUpdate =
        settingsService.getCurrentSettings().getLastSuccessfulAnalyticsTablesUpdate();

    Set<AnalyticsTableType> availableTypes =
        analyticsTableServices.stream()
            .map(AnalyticsTableService::getAnalyticsTableType)
            .collect(Collectors.toSet());

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder(params0)
            .withLastSuccessfulUpdate(lastSuccessfulUpdate)
            .build();

    log.info("Found {} analytics table types: {}", availableTypes.size(), availableTypes);
    log.info("Analytics table update: {}", params);
    log.info("Last successful analytics table update: {}", toLongDate(lastSuccessfulUpdate));

    progress.startingProcess(
        "Analytics table update process{}", (params.isLatestUpdate() ? " (latest partition)" : ""));

    if (!params.isSkipResourceTables() && !params.isLatestUpdate()) {
      generateResourceTablesInternal(progress);
    }

    Set<AnalyticsTableType> skipTypes = emptyIfNull(params.getSkipTableTypes());

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
    progress.completedProcess("Analytics tables updated: {}", clock.time());
  }

  private void updateLastSuccessfulSystemSettings(AnalyticsTableUpdateParams params, Clock clock) {
    if (params.isLatestUpdate()) {
      settingsService.putAll(
          Map.ofEntries(
              entry(
                  "keyLastSuccessfulLatestAnalyticsPartitionUpdate",
                  Settings.valueOf(params.getStartTime())),
              entry("keyLastSuccessfulLatestAnalyticsPartitionRuntime", clock.time())));
    } else {
      settingsService.putAll(
          Map.ofEntries(
              entry(
                  "keyLastSuccessfulAnalyticsTablesUpdate",
                  Settings.valueOf(params.getStartTime())),
              entry("keyLastSuccessfulAnalyticsTablesRuntime", clock.time())));
    }
  }

  @Override
  public void generateResourceTables(JobProgress progress) {
    Clock clock = new Clock().startClock();

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

  private void generateResourceTablesInternal(JobProgress progress) {
    resourceTableService.dropAllSqlViews(progress);

    Map<String, Runnable> generators = new LinkedHashMap<>();
    generators.put("Generating resource tables", resourceTableService::generateResourceTables);
    progress.startingStage("Generating resource tables", generators.size(), SKIP_STAGE);
    progress.runStage(generators);

    resourceTableService.createAllSqlViews(progress);

    settingsService.put("keyLastSuccessfulResourceTablesUpdate", new Date());
  }
}
