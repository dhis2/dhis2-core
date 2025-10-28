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

import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.SecondaryMetadataObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
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
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.scheduling.parameters.SmsJobParameters;
import org.hisp.dhis.scheduling.parameters.SqlViewUpdateParameters;
import org.hisp.dhis.scheduling.parameters.TestJobParameters;
import org.hisp.dhis.scheduling.parameters.TrackerTrigramIndexJobParameters;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

/**
 * This class defines configuration for a job in the system. The job is defined with general
 * identifiers, as well as job specific, such as jobType {@link JobType}.
 *
 * <p>All system jobs should be included in {@link JobType} enum and can be scheduled/executed with
 * {@link JobSchedulerService}.
 *
 * <p>The class uses a custom deserializer to handle several potential {@link JobParameters}.
 *
 * @author Henning Håkonsen
 */
@Getter
@Setter
@ToString
public class JobConfiguration extends BaseIdentifiableObject implements SecondaryMetadataObject {

  /**
   * A CRON based job may trigger on the same day after it has missed its execution. If time has
   * passed past this point the execution is skipped, and it will trigger on the intended execution
   * after that. This is the default value for the setting giving a 4h window to succeed with the
   * execution for each occurrence.
   */
  public static final int MAX_CRON_DELAY_HOURS = 4;

  /** The type of job. */
  @JsonProperty(required = true)
  private JobType jobType;

  @JsonProperty private SchedulingType schedulingType;

  /**
   * The cron expression used for scheduling the job. Relevant for {@link #schedulingType} {@link
   * SchedulingType#CRON}.
   */
  @JsonProperty private String cronExpression;

  /**
   * The delay in seconds between the completion of one job execution and the start of the next.
   * Relevant for {@link #schedulingType} {@link SchedulingType#FIXED_DELAY}.
   */
  @JsonProperty private Integer delay;

  /**
   * Parameters of the job. Jobs can use their own implementation of the {@link JobParameters}
   * class.
   */
  private JobParameters jobParameters;

  /** Indicates whether this job is currently enabled or disabled. */
  @JsonProperty private boolean enabled = true;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private JobStatus jobStatus;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private JobStatus lastExecutedStatus = JobStatus.NOT_STARTED;

  /**
   * When the job execution started last (only null when a job did never run). The name is not ideal
   * but kept for backwards compatibility.
   */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Date lastExecuted;

  /**
   * When the job execution finished most recently (only null when a job never finished running
   * yet). Can be before {@link #lastExecuted} while a new run is still in progress.
   */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Date lastFinished;

  /**
   * When the job was last observed as making progress during the execution (null while a job is
   * scheduled)
   */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Date lastAlive;

  /**
   * Optional UID of the user that executes the job. (The user's authentication is set in the
   * security context for the execution scope)
   */
  @JsonProperty private String executedBy;

  /**
   * The name of the queue this job belongs to or null if it is a stand-alone job. If set the {@link
   * #queuePosition} is also set to 0 or more.
   *
   * <p>This property is read-only for the {@link JobConfiguration} API as it is only changed using
   * {@link JobQueueService}.
   */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String queueName;

  /**
   * Position of this job in the queue named by {@link #queueName} starting from zero.
   *
   * <p>This property is read-only for the {@link JobConfiguration} API as it is only changed using
   * {@link JobQueueService}.
   */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Integer queuePosition;

  @CheckForNull
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String errorCodes;

  public JobConfiguration() {}

  /**
   * Constructor to use for any type of {@link SchedulingType#ONCE_ASAP} execution.
   *
   * <p>This will make sure the name of the job configuration is unique (within reason).
   *
   * @param type of the job to run once
   */
  public JobConfiguration(@Nonnull JobType type) {
    this(null, type);
  }

  public JobConfiguration(@CheckForNull String name, @Nonnull JobType type) {
    this(name, type, null);
  }

  /** The largest timestamp values that has already been used */
  private static final AtomicLong mostRecentUsedTimeMilli = new AtomicLong(0L);

  /**
   * Constructor to use for any type of {@link SchedulingType#ONCE_ASAP} execution.
   *
   * @param name unique name for the job
   * @param type of the job to run once
   * @param executedBy UID of the user running the job or null for run as admin
   */
  public JobConfiguration(
      @CheckForNull String name, @Nonnull JobType type, @CheckForNull String executedBy) {
    this.name =
        name == null || name.isEmpty() ? "%s (%d)".formatted(type.name(), nowUnique()) : name;
    this.jobType = type;
    this.executedBy = executedBy;
    setAutoFields();
  }

  static long nowUnique() {
    long now = System.currentTimeMillis();
    return mostRecentUsedTimeMilli.updateAndGet(val -> Math.max(val + 1, now));
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public SchedulingType getSchedulingType() {
    if (schedulingType != null) return schedulingType;
    if (cronExpression != null) return SchedulingType.CRON;
    if (delay != null) return SchedulingType.FIXED_DELAY;
    return SchedulingType.ONCE_ASAP;
  }

  public JobStatus getJobStatus() {
    if (jobStatus != null)
      return enabled && jobStatus == JobStatus.DISABLED ? JobStatus.SCHEDULED : jobStatus;
    if (getSchedulingType() == SchedulingType.ONCE_ASAP) return JobStatus.NOT_STARTED;
    return JobStatus.SCHEDULED;
  }

  /**
   * Checks if this job has changes compared to the specified job configuration that are only
   * allowed for configurable jobs.
   *
   * @param other the job configuration that should be checked.
   * @return <code>true</code> if this job configuration has changes in fields that are only allowed
   *     for configurable jobs, <code>false</code> otherwise.
   */
  public boolean hasNonConfigurableJobChanges(@Nonnull JobConfiguration other) {
    return this.jobType != other.getJobType()
        || this.getJobStatus() != other.getJobStatus()
        || this.jobParameters != other.getJobParameters()
        || this.enabled != other.isEnabled();
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public boolean isConfigurable() {
    return jobType.isUserDefined();
  }

  public boolean hasCronExpression() {
    return cronExpression != null && !cronExpression.isEmpty();
  }

  /**
   * The sub type names refer to the {@link JobType} enumeration. Defaults to null for unmapped job
   * types.
   */
  @JsonProperty
  @Property(value = PropertyType.COMPLEX, required = FALSE)
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      property = "jobType",
      defaultImpl = java.lang.Void.class)
  @JsonSubTypes(
      value = {
        @JsonSubTypes.Type(value = MetadataImportParams.class, name = "METADATA_IMPORT"),
        @JsonSubTypes.Type(value = ImportOptions.class, name = "DATAVALUE_IMPORT"),
        @JsonSubTypes.Type(value = AnalyticsJobParameters.class, name = "ANALYTICS_TABLE"),
        @JsonSubTypes.Type(
            value = ContinuousAnalyticsJobParameters.class,
            name = "CONTINUOUS_ANALYTICS_TABLE"),
        @JsonSubTypes.Type(value = MonitoringJobParameters.class, name = "MONITORING"),
        @JsonSubTypes.Type(value = PredictorJobParameters.class, name = "PREDICTOR"),
        @JsonSubTypes.Type(value = PushAnalysisJobParameters.class, name = "PUSH_ANALYSIS"),
        @JsonSubTypes.Type(
            value = HtmlPushAnalyticsJobParameters.class,
            name = "HTML_PUSH_ANALYTICS"),
        @JsonSubTypes.Type(value = SmsJobParameters.class, name = "SMS_SEND"),
        @JsonSubTypes.Type(value = MetadataSyncJobParameters.class, name = "META_DATA_SYNC"),
        @JsonSubTypes.Type(value = DataSynchronizationJobParameters.class, name = "DATA_SYNC"),
        @JsonSubTypes.Type(
            value = DisableInactiveUsersJobParameters.class,
            name = "DISABLE_INACTIVE_USERS"),
        @JsonSubTypes.Type(
            value = TrackerTrigramIndexJobParameters.class,
            name = "TRACKER_TRIGRAM_INDEX_MAINTENANCE"),
        @JsonSubTypes.Type(value = DataIntegrityJobParameters.class, name = "DATA_INTEGRITY"),
        @JsonSubTypes.Type(
            value = DataIntegrityDetailsJobParameters.class,
            name = "DATA_INTEGRITY_DETAILS"),
        @JsonSubTypes.Type(
            value = AggregateDataExchangeJobParameters.class,
            name = "AGGREGATE_DATA_EXCHANGE"),
        @JsonSubTypes.Type(
            value = SqlViewUpdateParameters.class,
            name = "MATERIALIZED_SQL_VIEW_UPDATE"),
        @JsonSubTypes.Type(
            value = LockExceptionCleanupJobParameters.class,
            name = "LOCK_EXCEPTION_CLEANUP"),
        @JsonSubTypes.Type(value = TestJobParameters.class, name = "TEST"),
        @JsonSubTypes.Type(
            value = ImportOptions.class,
            name = "COMPLETE_DATA_SET_REGISTRATION_IMPORT"),
        @JsonSubTypes.Type(value = GeoJsonImportJobParams.class, name = "GEO_JSON_IMPORT")
      })
  public JobParameters getJobParameters() {
    return jobParameters;
  }

  /** Kept for backwards compatibility of the REST API */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public Date getNextExecutionTime() {
    // this is a "best guess" because the 4h max delay could have been changed in the settings
    Instant next = nextExecutionTime(Instant.now(), Duration.ofHours(MAX_CRON_DELAY_HOURS));
    return next == null ? null : Date.from(next);
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public Date getMaxDelayedExecutionTime() {
    Duration maxCronDelay = Duration.ofHours(MAX_CRON_DELAY_HOURS);
    Instant nextExecutionTime = nextExecutionTime(Instant.now(), maxCronDelay);
    if (nextExecutionTime == null) return null;
    Instant instant = maxDelayedExecutionTime(this, maxCronDelay, nextExecutionTime);
    return instant == null ? null : Date.from(instant);
  }

  /** Kept for backwards compatibility of the REST API */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getLastRuntimeExecution() {
    return lastExecuted == null || lastFinished == null || lastFinished.before(lastExecuted)
        ? null
        : DurationFormatUtils.formatDurationHMS(lastFinished.getTime() - lastExecuted.getTime());
  }

  /** Kept for backwards compatibility of the REST API */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public boolean isLeaderOnlyJob() {
    return true;
  }

  /**
   * Kept for backwards compatibility of the REST API
   *
   * @see #getExecutedBy()
   */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public String getUserUid() {
    return executedBy;
  }

  public String getQueueIdentifier() {
    return getQueueName() == null ? getUid() : getQueueName();
  }

  /**
   * @return true if this configuration is part of a queue, false otherwise
   */
  public boolean isUsedInQueue() {
    return getQueueName() != null;
  }

  public boolean isRunOnce() {
    return schedulingType == SchedulingType.ONCE_ASAP
        && cronExpression == null
        && delay == null
        && queueName == null;
  }

  public boolean isReadyToRun() {
    return schedulingType == SchedulingType.ONCE_ASAP
        || schedulingType == SchedulingType.CRON && cronExpression != null
        || schedulingType == SchedulingType.FIXED_DELAY && delay != null
        || queueName != null && queuePosition > 0;
  }

  @Nonnull
  public JobKey toKey() {
    return new JobKey(UID.of(uid), jobType);
  }

  /**
   * @param now current timestamp, ideally without milliseconds
   * @param maxCronDelay the maximum duration a CRON based job will trigger on the same day after
   *     its intended time during the day. If more time has passed already the execution for that
   *     day is skipped and the next day will be the target
   * @return the next time this job should run based on the {@link #getLastExecuted()} time
   */
  public Instant nextExecutionTime(@Nonnull Instant now, @Nonnull Duration maxCronDelay) {
    return nextExecutionTime(ZoneId.systemDefault(), now, maxCronDelay);
  }

  Instant nextExecutionTime(
      @Nonnull ZoneId zone, @Nonnull Instant now, @Nonnull Duration maxCronDelay) {
    if (isUsedInQueue() && getQueuePosition() > 0) return null;
    JobTrigger trigger = new JobTrigger(getSchedulingType(), lastExecuted, cronExpression, delay);
    return trigger.nextExecutionTime(zone, now, maxCronDelay);
  }

  @CheckForNull
  public static Instant maxDelayedExecutionTime(
      JobConfiguration config, Duration maxCronDelay, Instant nextExecutionTime) {
    return nextExecutionTime == null || config.getSchedulingType() != SchedulingType.CRON
        ? null
        : nextExecutionTime.plus(maxCronDelay);
  }
}
