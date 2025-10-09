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
package org.hisp.dhis.analytics.table.scheduling;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.hisp.dhis.util.DateUtils.toLongDate;

import java.util.Date;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.common.TableInfoReader;
import org.hisp.dhis.analytics.util.DatabaseUtils;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.ContinuousAnalyticsJobParameters;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * Job for continuous update of analytics tables. Performs analytics table update on a schedule
 * where the full analytics table update is done once per day, and the latest analytics partition
 * update is done with a fixed delay.
 *
 * <p>When to run the full update is determined by {@link
 * ContinuousAnalyticsJobParameters#getHourOfDay()}, which specifies the hour of day to run the full
 * update. The next scheduled full analytics table update time is persisted using a system setting.
 * A full analytics table update is performed when the current time is after the next scheduled full
 * update time. Otherwise, a partial update of the latest analytics partition table is performed.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContinuousAnalyticsTableJob implements Job {
  private static final int DEFAULT_HOUR_OF_DAY = 0;

  private final AnalyticsTableGenerator analyticsTableGenerator;

  private final SystemSettingsService settingsService;

  private final TableInfoReader tableInfoReader;

  private final DatabaseUtils databaseUtils;

  @Override
  public JobType getJobType() {
    return JobType.CONTINUOUS_ANALYTICS_TABLE;
  }

  @Override
  public void execute(JobConfiguration jobConfiguration, JobProgress progress) {
    ContinuousAnalyticsJobParameters parameters =
        (ContinuousAnalyticsJobParameters) jobConfiguration.getJobParameters();

    if (!checkJobOutliersConsistency(parameters)) {
      log.info(
          "Updating analytics table is currently not feasible, job parameters not aligned with existing outlier data");
      return;
    }

    final int fullUpdateHourOfDay =
        firstNonNull(parameters.getFullUpdateHourOfDay(), DEFAULT_HOUR_OF_DAY);
    final Date startTime = new Date();

    log.info(
        "Starting continuous analytics table update, current time: '{}'", toLongDate(startTime));

    if (runFullUpdate(startTime)) {
      log.info("Performing full analytics table update");

      AnalyticsTableUpdateParams params =
          AnalyticsTableUpdateParams.newBuilder()
              .skipResourceTables(false)
              .skipOutliers(parameters.getSkipOutliers() || !databaseUtils.supportsOutliers())
              .skipTableTypes(parameters.getSkipTableTypes())
              .jobId(jobConfiguration)
              .startTime(startTime)
              .build();

      try {
        analyticsTableGenerator.generateAnalyticsTables(params, progress);
      } finally {
        Date nextUpdate = DateUtils.getNextDate(fullUpdateHourOfDay, startTime);
        settingsService.put("keyNextAnalyticsTableUpdate", nextUpdate);
        log.info("Next full analytics table update: '{}'", toLongDate(nextUpdate));
      }
    } else {
      log.info("Performing latest analytics table partition update");

      AnalyticsTableUpdateParams params =
          AnalyticsTableUpdateParams.newBuilder()
              .skipResourceTables(true)
              .skipOutliers(parameters.getSkipOutliers() || databaseUtils.supportsOutliers())
              .skipTableTypes(parameters.getSkipTableTypes())
              .jobId(jobConfiguration)
              .startTime(startTime)
              .build()
              .withLatestPartition();

      analyticsTableGenerator.generateAnalyticsTables(params, progress);
    }
  }

  /**
   * Indicates whether a full table update should be run. If the next full update time is not set,
   * it indicates that a full update has never been run for this job, and a full update should be
   * run immediately. Otherwise, a full update is run if the job start time argument is after the
   * next full update time.
   *
   * @param startTime the job start time.
   * @return true if a full table update should be run.
   */
  boolean runFullUpdate(Date startTime) {
    Objects.requireNonNull(startTime);

    Date nextFullUpdate = settingsService.getCurrentSettings().getNextAnalyticsTableUpdate();

    return startTime.after(nextFullUpdate);
  }

  private boolean checkJobOutliersConsistency(ContinuousAnalyticsJobParameters parameters) {
    boolean analyticsTableWithOutliers =
        tableInfoReader.getInfo("analytics").getColumns().stream()
            .anyMatch("sourceid"::equalsIgnoreCase);
    boolean outliersRequired = !parameters.getSkipOutliers();

    return outliersRequired && analyticsTableWithOutliers
        || !outliersRequired && !analyticsTableWithOutliers;
  }
}
