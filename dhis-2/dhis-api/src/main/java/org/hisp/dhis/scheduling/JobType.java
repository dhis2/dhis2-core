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
package org.hisp.dhis.scheduling;

import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobType.Defaults.daily1am;
import static org.hisp.dhis.scheduling.JobType.Defaults.daily2am;
import static org.hisp.dhis.scheduling.JobType.Defaults.daily7am;
import static org.hisp.dhis.scheduling.JobType.Defaults.dailyRandomBetween3and5;
import static org.hisp.dhis.scheduling.JobType.Defaults.every;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.scheduling.parameters.AggregateDataExchangeJobParameters;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.ContinuousAnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.DataIntegrityDetailsJobParameters;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters;
import org.hisp.dhis.scheduling.parameters.DataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.DisableInactiveUsersJobParameters;
import org.hisp.dhis.scheduling.parameters.GeoJsonImportJobParams;
import org.hisp.dhis.scheduling.parameters.HtmlPushAnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.LockExceptionCleanupJobParameters;
import org.hisp.dhis.scheduling.parameters.MetadataSyncJobParameters;
import org.hisp.dhis.scheduling.parameters.MockJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.scheduling.parameters.SmsInboundProcessingJobParameters;
import org.hisp.dhis.scheduling.parameters.SmsJobParameters;
import org.hisp.dhis.scheduling.parameters.SqlViewUpdateParameters;
import org.hisp.dhis.scheduling.parameters.TestJobParameters;
import org.hisp.dhis.scheduling.parameters.TrackerTrigramIndexJobParameters;

/**
 * Enum describing the different jobs in the system. Each job has a key, class, configurable status
 * and possibly a map containing relative endpoints for possible parameters.
 *
 * @author Henning Håkonsen
 */
@Getter
public enum JobType {
  /*
  User defined jobs
   */
  DATA_INTEGRITY(DataIntegrityJobParameters.class),
  DATA_INTEGRITY_DETAILS(DataIntegrityDetailsJobParameters.class),
  RESOURCE_TABLE(),
  ANALYTICS_TABLE(AnalyticsJobParameters.class),
  CONTINUOUS_ANALYTICS_TABLE(ContinuousAnalyticsJobParameters.class),
  DATA_SYNC(DataSynchronizationJobParameters.class),
  META_DATA_SYNC(MetadataSyncJobParameters.class),
  AGGREGATE_DATA_EXCHANGE(AggregateDataExchangeJobParameters.class),
  SEND_SCHEDULED_MESSAGE(),
  PROGRAM_NOTIFICATIONS(),
  MONITORING(MonitoringJobParameters.class),
  PUSH_ANALYSIS(PushAnalysisJobParameters.class),
  HTML_PUSH_ANALYTICS(HtmlPushAnalyticsJobParameters.class),
  TRACKER_TRIGRAM_INDEX_MAINTENANCE(TrackerTrigramIndexJobParameters.class),
  PREDICTOR(PredictorJobParameters.class),
  MATERIALIZED_SQL_VIEW_UPDATE(SqlViewUpdateParameters.class),
  DISABLE_INACTIVE_USERS(DisableInactiveUsersJobParameters.class),
  TEST(TestJobParameters.class),
  LOCK_EXCEPTION_CLEANUP(LockExceptionCleanupJobParameters.class),

  /*
  Programmatically used Jobs
  */
  MOCK(MockJobParameters.class),
  SMS_SEND(SmsJobParameters.class),
  SMS_INBOUND_PROCESSING(SmsInboundProcessingJobParameters.class),
  TRACKER_IMPORT_JOB(),
  TRACKER_IMPORT_NOTIFICATION_JOB(),
  TRACKER_IMPORT_RULE_ENGINE_JOB(),
  IMAGE_PROCESSING(),
  COMPLETE_DATA_SET_REGISTRATION_IMPORT(),
  DATAVALUE_IMPORT_INTERNAL(),
  METADATA_IMPORT(),
  DATAVALUE_IMPORT(ImportOptions.class),
  GEOJSON_IMPORT(GeoJsonImportJobParams.class),
  GML_IMPORT(),

  /*
  System Jobs
  */
  HOUSEKEEPING(every(20, "DHIS2rocks1", "Housekeeping")),
  DATA_VALUE_TRIM(daily1am("D2datatrim8", "Data value trim")),
  DATA_SET_NOTIFICATION(daily2am("YvAwAmrqAtN", "Dataset notification")),
  CREDENTIALS_EXPIRY_ALERT(daily2am("sHMedQF7VYa", "Credentials expiry alert")),
  DATA_STATISTICS(daily2am("BFa3jDsbtdO", "Data statistics")),
  FILE_RESOURCE_CLEANUP(every(20, "pd6O228pqr0", "File resource clean up")),
  ACCOUNT_EXPIRY_ALERT(daily2am("fUWM1At1TUx", "User account expiry alert")),
  VALIDATION_RESULTS_NOTIFICATION(daily7am("Js3vHn2AVuG", "Validation result notification")),
  REMOVE_USED_OR_EXPIRED_RESERVED_VALUES(
      daily2am("uwWCT2BMmlq", "Remove expired or used reserved values")),
  SYSTEM_VERSION_UPDATE_CHECK(
      dailyRandomBetween3and5("vt21671bgno", "System version update check notification"));

  /**
   * Any {@link JobType} which has a default will ensure that the {@link JobConfiguration} for that
   * default does exist.
   *
   * @param uid of the {@link JobConfiguration} that either exist or is created
   * @param cronExpression for the {@link JobConfiguration} should it be created
   * @param delay for the {@link JobConfiguration} should it be created
   * @param name for the {@link JobConfiguration} should it be created
   */
  public record Defaults(
      @Nonnull String uid,
      @CheckForNull String cronExpression,
      @CheckForNull Integer delay,
      @Nonnull String name) {

    static Defaults every(int seconds, String uid, String name) {
      return new Defaults(uid, null, seconds, name);
    }

    static Defaults daily2am(String uid, String name) {
      return new Defaults(uid, "0 0 2 ? * *", null, name);
    }

    static Defaults daily1am(String uid, String name) {
      return new Defaults(uid, "0 0 1 ? * *", null, name);
    }

    static Defaults daily7am(String uid, String name) {
      return new Defaults(uid, "0 0 7 ? * *", null, name);
    }

    /**
     * Execute at 3-5AM every night and, use a random min/sec, so we don't have all servers in the
     * world requesting at the same time.
     */
    @SuppressWarnings("java:S2245")
    static Defaults dailyRandomBetween3and5(String uid, String name) {
      ThreadLocalRandom rnd = ThreadLocalRandom.current();
      String cron = format("%d %d %d ? * *", rnd.nextInt(60), rnd.nextInt(60), rnd.nextInt(3, 6));
      return new Defaults(uid, cron, null, name);
    }
  }

  @CheckForNull private final Class<? extends JobParameters> jobParameters;

  /** A job with defaults is a system job and gets spawned automatically if it does not yet exist */
  @CheckForNull private final Defaults defaults;

  JobType() {
    this(null, null);
  }

  JobType(Class<? extends JobParameters> jobParameters) {
    this(jobParameters, null);
  }

  JobType(Defaults defaults) {
    this(null, defaults);
  }

  JobType(
      @CheckForNull Class<? extends JobParameters> jobParameters, @CheckForNull Defaults defaults) {
    this.jobParameters = jobParameters;
    this.defaults = defaults;
  }

  /**
   * @return when true, the {@link JobConfiguration#getExecutedBy()} is set to the job creator on
   *     creation unless it was set explicitly. This means that a valid User is required for a Job
   *     (System User will not work).
   */
  public boolean isValidUserRequiredForJob() {
    return this == HTML_PUSH_ANALYTICS || this == AGGREGATE_DATA_EXCHANGE || this == META_DATA_SYNC;
  }

  /**
   * @implNote since 2.42 all jobs forward to the {@code Notifier} but those not included here use
   *     {@link org.hisp.dhis.system.notification.NotificationLevel#ERROR}.
   * @return true, if {@link JobProgress} events should be forwarded to the {@link
   *     org.eclipse.emf.common.notify.Notifier} API, otherwise false
   */
  public boolean isUsingNotifications() {
    return this == RESOURCE_TABLE
        || this == SEND_SCHEDULED_MESSAGE
        || this == ANALYTICS_TABLE
        || this == CONTINUOUS_ANALYTICS_TABLE
        || this == DATA_SET_NOTIFICATION
        || this == MONITORING
        || this == VALIDATION_RESULTS_NOTIFICATION
        || this == SYSTEM_VERSION_UPDATE_CHECK
        || this == DATA_SYNC
        || this == SMS_SEND
        || this == PUSH_ANALYSIS
        || this == PREDICTOR
        || this == DATAVALUE_IMPORT
        || this == COMPLETE_DATA_SET_REGISTRATION_IMPORT
        || this == METADATA_IMPORT
        || this == TRACKER_IMPORT_JOB
        || this == GEOJSON_IMPORT;
  }

  /**
   * @return true, when an error notification should be sent by email in case the job execution
   *     fails, otherwise false
   */
  public boolean isUsingErrorNotification() {
    return this == ANALYTICS_TABLE
        || this == VALIDATION_RESULTS_NOTIFICATION
        || this == DATA_SET_NOTIFICATION
        || this == SYSTEM_VERSION_UPDATE_CHECK
        || this == PROGRAM_NOTIFICATIONS
        || this == DATAVALUE_IMPORT
        || this == METADATA_IMPORT;
  }

  /**
   * @return true, if jobs of this type should try to run as soon as possible by having job
   *     scheduler workers execute all known ready jobs of the type, when false only the oldest of
   *     the ready jobs per type is attempted to start in a single loop cycle
   */
  public boolean isUsingContinuousExecution() {
    return this == METADATA_IMPORT
        || this == RESOURCE_TABLE
        || this == ANALYTICS_TABLE
        || this == TRACKER_IMPORT_JOB
        || this == DATA_INTEGRITY
        || this == DATA_INTEGRITY_DETAILS
        || this == SMS_INBOUND_PROCESSING
        || this == GEOJSON_IMPORT;
  }

  public boolean hasJobParameters() {
    return jobParameters != null;
  }

  /**
   * @return Can a user create jobs of this type?
   */
  public boolean isUserDefined() {
    return ordinal() < MOCK.ordinal();
  }

  public Map<String, String> getRelativeApiElements() {
    return switch (this) {
      case DATA_INTEGRITY, DATA_INTEGRITY_DETAILS -> Map.of("checks", "/api/dataIntegrity");
      case ANALYTICS_TABLE ->
          Map.of(
              "skipTableTypes", "/api/analytics/tableTypes",
              "skipPrograms", "/api/programs");
      case CONTINUOUS_ANALYTICS_TABLE -> Map.of("skipTableTypes", "/api/analytics/tableTypes");
      case AGGREGATE_DATA_EXCHANGE -> Map.of("dataExchangeIds", "/api/aggregateDataExchanges");
      case MONITORING ->
          Map.of(
              "relativePeriods", "/api/periodTypes/relativePeriodTypes",
              "validationRuleGroups", "/api/validationRuleGroups");
      case PUSH_ANALYSIS -> Map.of("pushAnalysis", "/api/pushAnalysis");
      case PREDICTOR ->
          Map.of(
              "predictors", "/api/predictors",
              "predictorGroups", "/api/predictorGroups");
      case HTML_PUSH_ANALYTICS ->
          Map.of("dashboard", "/api/dashboards", "receivers", "/api/userGroups");
      default -> Map.of();
    };
  }
}
