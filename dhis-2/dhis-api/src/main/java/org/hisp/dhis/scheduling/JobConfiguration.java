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
package org.hisp.dhis.scheduling;

import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.SecondaryMetadataObject;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.scheduling.parameters.AggregateDataExchangeJobParameters;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.ContinuousAnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters;
import org.hisp.dhis.scheduling.parameters.DataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.DisableInactiveUsersJobParameters;
import org.hisp.dhis.scheduling.parameters.EventProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.LockExceptionCleanupJobParameters;
import org.hisp.dhis.scheduling.parameters.MetadataSyncJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.scheduling.parameters.SmsJobParameters;
import org.hisp.dhis.scheduling.parameters.SqlViewUpdateParameters;
import org.hisp.dhis.scheduling.parameters.TestJobParameters;
import org.hisp.dhis.scheduling.parameters.TrackerProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.TrackerTrigramIndexJobParameters;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

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
        name == null || name.isEmpty()
            ? "%s (%d)".formatted(type.name(), Instant.now().toEpochMilli())
            : name;
    this.jobType = type;
    this.executedBy = executedBy;
    setAutoFields();
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
    if (jobStatus != null) return jobStatus;
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
        @JsonSubTypes.Type(value = SmsJobParameters.class, name = "SMS_SEND"),
        @JsonSubTypes.Type(value = MetadataSyncJobParameters.class, name = "META_DATA_SYNC"),
        @JsonSubTypes.Type(
            value = EventProgramsDataSynchronizationJobParameters.class,
            name = "EVENT_PROGRAMS_DATA_SYNC"),
        @JsonSubTypes.Type(
            value = TrackerProgramsDataSynchronizationJobParameters.class,
            name = "TRACKER_PROGRAMS_DATA_SYNC"),
        @JsonSubTypes.Type(value = DataSynchronizationJobParameters.class, name = "DATA_SYNC"),
        @JsonSubTypes.Type(
            value = DisableInactiveUsersJobParameters.class,
            name = "DISABLE_INACTIVE_USERS"),
        @JsonSubTypes.Type(
            value = TrackerTrigramIndexJobParameters.class,
            name = "TRACKER_SEARCH_OPTIMIZATION"),
        @JsonSubTypes.Type(value = DataIntegrityJobParameters.class, name = "DATA_INTEGRITY"),
        @JsonSubTypes.Type(
            value = AggregateDataExchangeJobParameters.class,
            name = "AGGREGATE_DATA_EXCHANGE"),
        @JsonSubTypes.Type(value = SqlViewUpdateParameters.class, name = "SQL_VIEW_UPDATE"),
        @JsonSubTypes.Type(
            value = LockExceptionCleanupJobParameters.class,
            name = "LOCK_EXCEPTION_CLEANUP"),
        @JsonSubTypes.Type(value = TestJobParameters.class, name = "TEST")
      })
  public JobParameters getJobParameters() {
    return jobParameters;
  }

  /** Kept for backwards compatibility of the REST API */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public Date getNextExecutionTime() {
    Instant next = nextExecutionTime(Instant.now(), Duration.ofDays(1));
    return next == null ? null : Date.from(next);
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
    return cronExpression == null && delay == null && queueName == null;
  }

  public boolean isDueBetween(
      @Nonnull Instant now, @Nonnull Instant then, @Nonnull Duration maxCronDelay) {
    Instant dueTime = nextExecutionTime(now, maxCronDelay);
    return dueTime != null && dueTime.isBefore(then);
  }

  /**
   * @param now current timestamp, ideally without milliseconds
   * @param maxCronDelay the maximum duration a CRON based job will trigger on the same day after
   *     its intended time during the day. If more time has passed already the execution for that
   *     day is skipped and the next day will be the target
   * @return the next time this job should run based on the {@link #getLastExecuted()} time
   */
  public Instant nextExecutionTime(@Nonnull Instant now, @Nonnull Duration maxCronDelay) {
    // for good measure we offset the last time by 1 second
    Instant since = lastExecuted == null ? now : lastExecuted.toInstant().plusSeconds(1);
    if (isUsedInQueue() && getQueuePosition() > 0) return null;
    return switch (getSchedulingType()) {
      case ONCE_ASAP -> nextOnceExecutionTime(since);
      case FIXED_DELAY -> nextDelayExecutionTime(since);
      case CRON -> nextCronExecutionTime(since, now, maxCronDelay);
    };
  }

  private Instant nextCronExecutionTime(
      @Nonnull Instant since, Instant now, @Nonnull Duration maxDelay) {
    if (isUndefinedCronExpression(cronExpression)) return null;
    SimpleTriggerContext context =
        new SimpleTriggerContext(Clock.fixed(since, ZoneId.systemDefault()));
    Date next = new CronTrigger(cronExpression).nextExecutionTime(context);
    if (next == null) return null;
    if (now.isAfter(next.toInstant().plus(maxDelay))) {
      context =
          new SimpleTriggerContext(
              Clock.fixed(next.toInstant().plusSeconds(1), ZoneId.systemDefault()));
      next = new CronTrigger(cronExpression).nextExecutionTime(context);
    }
    return next == null ? null : next.toInstant();
  }

  private Instant nextDelayExecutionTime(@Nonnull Instant since) {
    if (delay == null || delay <= 0) return null;
    // always want to run delay after last start, right away when never started
    return lastExecuted == null
        ? since
        : lastExecuted.toInstant().plusSeconds(delay).truncatedTo(ChronoUnit.SECONDS);
  }

  private Instant nextOnceExecutionTime(@Nonnull Instant since) {
    return since;
  }

  private static boolean isUndefinedCronExpression(String cronExpression) {
    return cronExpression == null
        || cronExpression.isEmpty()
        || cronExpression.equals("* * * * * ?");
  }
}
