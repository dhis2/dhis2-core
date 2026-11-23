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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.hisp.dhis.scheduling.JobStatus.DISABLED;
import static org.hisp.dhis.scheduling.JobStatus.NOT_STARTED;
import static org.hisp.dhis.scheduling.JobStatus.RUNNING;
import static org.hisp.dhis.scheduling.JobStatus.SCHEDULED;
import static org.hisp.dhis.scheduling.JobType.ANALYTICS_TABLE;
import static org.hisp.dhis.scheduling.JobType.CONTINUOUS_ANALYTICS_TABLE;
import static org.hisp.dhis.scheduling.JobType.DATAVALUE_IMPORT;
import static org.hisp.dhis.scheduling.JobType.HOUSEKEEPING;
import static org.hisp.dhis.scheduling.SchedulingType.CRON;
import static org.hisp.dhis.scheduling.SchedulingType.FIXED_DELAY;
import static org.hisp.dhis.scheduling.SchedulingType.ONCE_ASAP;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests the data integrity checks in the "Job scheduler" section. Job configurations are inserted
 * with SQL because the states these checks detect cannot be created through the API.
 */
class DataIntegrityJobConfigurationsControllerTest extends AbstractDataIntegrityIntegrationTest {

  private static final String DETAILS_ID_TYPE = "jobConfigurations";

  private static final String HOUSEKEEPING_NOT_RUNNING =
      "job_configurations_housekeeping_not_running";

  private static final String ENABLED_NOT_SCHEDULED = "job_configurations_enabled_not_scheduled";
  private static final String STUCK_RUNNING = "job_configurations_stuck_running";
  private static final String NEVER_STARTED = "job_configurations_never_started";
  private static final String RECURRING_MARKED_ONCE = "job_configurations_recurring_marked_once";
  private static final String DELAY_OVERDUE = "job_configurations_delay_overdue";
  private static final String RUNNING_BLOCKING_OTHERS =
      "job_configurations_running_blocking_others";

  private static final String DAILY = "0 0 2 ? * *";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private JobConfigurationService jobConfigurationService;

  @BeforeEach
  void removeExistingJobs() {
    jdbcTemplate.update("delete from jobconfiguration");
  }

  // -------------------------------------------------------------------------
  // job_configurations_housekeeping_not_running
  // -------------------------------------------------------------------------

  @Test
  void testHousekeepingDisabled() {
    String uid = housekeeping().enabled(false).lastFinished("3 hours").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, HOUSEKEEPING_NOT_RUNNING, null, uid, "Housekeeping", "Disabled", false);
  }

  @Test
  void testHousekeepingEnabledButStoredAsDisabled() {
    String uid = housekeeping().status(DISABLED).lastFinished("3 hours").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        HOUSEKEEPING_NOT_RUNNING,
        null,
        uid,
        "Housekeeping",
        "never started",
        false);
  }

  @Test
  void testHousekeepingNotCompletedRecently() {
    String uid = housekeeping().lastExecuted("2 hours").lastFinished("2 hours").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, HOUSEKEEPING_NOT_RUNNING, null, uid, "Housekeeping", "last hour", false);
  }

  @Test
  void testHousekeepingCrashedWhileRunning() {
    // the scheduler cancels a stale running Housekeeping within a minute, so this state means the
    // scheduler loop itself is no longer running
    String uid =
        housekeeping()
            .status(RUNNING)
            .lastExecuted("2 hours")
            .lastAlive("2 hours")
            .lastFinished("2 hours")
            .insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, HOUSEKEEPING_NOT_RUNNING, null, uid, "Housekeeping", "last hour", false);
  }

  @Test
  void testHousekeepingMissing() {
    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        HOUSEKEEPING_NOT_RUNNING,
        null,
        "DHIS2rocks1",
        "Housekeeping",
        "Missing",
        false);
  }

  @Test
  void testHousekeepingRunning() {
    housekeeping().lastExecuted("30 seconds").lastFinished("25 seconds").insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, HOUSEKEEPING_NOT_RUNNING, false);
  }

  @Test
  void testHousekeepingRunningNow() {
    housekeeping()
        .status(RUNNING)
        .lastExecuted("5 seconds")
        .lastAlive("1 second")
        .lastFinished("25 seconds")
        .insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, HOUSEKEEPING_NOT_RUNNING, false);
  }

  @Test
  void testHousekeepingNewlyCreated() {
    // a fresh installation has not completed a run yet
    housekeeping().insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, HOUSEKEEPING_NOT_RUNNING, false);
  }

  @Test
  void testHousekeepingCreatedByScheduler() {
    // the row the scheduler creates on startup, rather than the fixture's idea of it
    jobConfigurationService.createDefaultJob(HOUSEKEEPING);
    entityManager.flush();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, HOUSEKEEPING_NOT_RUNNING, false);
  }

  // -------------------------------------------------------------------------
  // job_configurations_enabled_not_scheduled
  // -------------------------------------------------------------------------

  @Test
  void testEnabledRecurringJobStoredAsDisabled() {
    String stuck = job("Analytics").cron(DAILY).enabled(true).status(DISABLED).insert();
    job("Healthy").cron(DAILY).enabled(true).status(SCHEDULED).insert();
    // one-off jobs are out of scope: "Run now" would re-run an old import
    job("Old import").type(DATAVALUE_IMPORT).enabled(true).status(DISABLED).insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, ENABLED_NOT_SCHEDULED, 50, stuck, "Analytics", null, true);
  }

  @Test
  void testRecurringJobsCorrectlyScheduledOrDisabled() {
    job("Enabled").cron(DAILY).enabled(true).status(SCHEDULED).insert();
    job("Disabled").cron(DAILY).enabled(false).status(DISABLED).insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, ENABLED_NOT_SCHEDULED, true);
  }

  @Test
  void testEnabledQueueMemberStoredAsDisabled() {
    // queue members keep their scheduling type but have no cron expression of their own
    String member =
        job("Second").scheduling(CRON).queue("Nightly", 1).enabled(true).status(DISABLED).insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        ENABLED_NOT_SCHEDULED,
        100,
        member,
        "Second",
        "queue Nightly position 1",
        true);
  }

  // -------------------------------------------------------------------------
  // job_configurations_stuck_running
  // -------------------------------------------------------------------------

  @Test
  void testJobRunningPastStaleTimeout() {
    String stuck =
        job("Import").type(DATAVALUE_IMPORT).status(RUNNING).lastAlive("2 hours").insert();
    // analytics jobs use the longer analytics timeout (default 4 hours)
    job("Analytics").cron(DAILY).status(RUNNING).lastAlive("2 hours").insert();
    job("Busy import").type(DATAVALUE_IMPORT).status(RUNNING).lastAlive("1 minute").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, STUCK_RUNNING, null, stuck, "Import", DATAVALUE_IMPORT.name(), false);
  }

  @Test
  void testJobsRunningWithRecentProgress() {
    job("Import").type(DATAVALUE_IMPORT).status(RUNNING).lastAlive("1 minute").insert();
    job("Analytics").cron(DAILY).status(RUNNING).lastAlive("3 hours").insert();
    job("Finished").type(DATAVALUE_IMPORT).status(SCHEDULED).lastAlive("2 days").insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, STUCK_RUNNING, false);
  }

  // -------------------------------------------------------------------------
  // job_configurations_never_started
  // -------------------------------------------------------------------------

  @Test
  void testOneOffJobNeverStarted() {
    String neverStarted =
        job("Old import").type(DATAVALUE_IMPORT).status(NOT_STARTED).created("2 days").insert();
    job("New import").type(DATAVALUE_IMPORT).enabled(true).created("1 hour").insert();
    // still waiting to start, e.g. behind another import that is marked as running
    job("Waiting import").type(DATAVALUE_IMPORT).enabled(true).created("2 days").insert();
    job("Done import")
        .type(DATAVALUE_IMPORT)
        .created("2 days")
        .lastExecuted("2 days")
        .lastFinished("2 days")
        .insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        NEVER_STARTED,
        null,
        neverStarted,
        "Old import",
        DATAVALUE_IMPORT.name(),
        false);
  }

  @Test
  void testOneOffJobsStartedOrRecent() {
    job("New import").type(DATAVALUE_IMPORT).enabled(true).created("1 hour").insert();
    job("Done import")
        .type(DATAVALUE_IMPORT)
        .created("2 days")
        .lastExecuted("2 days")
        .lastFinished("2 days")
        .insert();
    // recurring jobs that have not run yet are not one-off jobs
    job("Weekly").cron(DAILY).enabled(true).created("2 days").insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, NEVER_STARTED, false);
  }

  // -------------------------------------------------------------------------
  // job_configurations_recurring_marked_once
  // -------------------------------------------------------------------------

  @Test
  void testRecurringJobMarkedAsOneOff() {
    String marked = job("Old cron").cron(DAILY).scheduling(ONCE_ASAP).status(DISABLED).insert();
    // "Run now" on a recurring job sets ONCE_ASAP + SCHEDULED until the run finishes
    job("Run now").cron(DAILY).scheduling(ONCE_ASAP).enabled(true).status(SCHEDULED).insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, RECURRING_MARKED_ONCE, 50, marked, "Old cron", null, true);
  }

  @Test
  void testRecurringJobsWithRecurringType() {
    job("Cron").cron(DAILY).enabled(false).status(DISABLED).insert();
    job("Delay").delay(20).enabled(true).status(SCHEDULED).insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, RECURRING_MARKED_ONCE, true);
  }

  // -------------------------------------------------------------------------
  // job_configurations_delay_overdue
  // -------------------------------------------------------------------------

  @Test
  void testDelayJobOverdue() {
    String overdue = continuousAnalytics("Continuous").lastExecuted("2 hours").insert();
    continuousAnalytics("Healthy").lastExecuted("2 minutes").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        DELAY_OVERDUE,
        50,
        overdue,
        "Continuous",
        "not started by the scheduler",
        true);
  }

  @Test
  void testDelayJobOverdueStoredAsDisabled() {
    String overdue =
        continuousAnalytics("Continuous").status(DISABLED).lastExecuted("2 hours").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, DELAY_OVERDUE, 100, overdue, "Continuous", "stored as DISABLED", true);
  }

  @Test
  void testDelayJobOverdueBehindRunningJob() {
    String overdue = continuousAnalytics("Continuous").lastExecuted("2 hours").insert();
    String blocker =
        job("Dead run")
            .type(CONTINUOUS_ANALYTICS_TABLE)
            .status(RUNNING)
            .lastExecuted("3 hours")
            .lastAlive("3 hours")
            .insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        DELAY_OVERDUE,
        100,
        overdue,
        "Continuous",
        "waiting behind running job Dead run (" + blocker + ")",
        true);
  }

  @Test
  void testDelayJobsNotOverdue() {
    continuousAnalytics("Recent").lastExecuted("2 minutes").insert();
    continuousAnalytics("Running").status(RUNNING).lastExecuted("2 hours").insert();
    continuousAnalytics("Disabled").enabled(false).status(DISABLED).lastExecuted("2 days").insert();
    // covered by job_configurations_housekeeping_not_running
    housekeeping().lastExecuted("2 hours").lastFinished("2 hours").insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, DELAY_OVERDUE, true);
  }

  // -------------------------------------------------------------------------
  // job_configurations_running_blocking_others
  // -------------------------------------------------------------------------

  @Test
  void testRunningJobBlockingOneOffJobs() {
    // DHIS2-22569: the server restarted during an analytics run, users kept starting new runs
    String blocker =
        job("Dead analytics").status(RUNNING).lastExecuted("2 hours").lastAlive("2 hours").insert();
    job("Queued 1").enabled(true).created("1 hour").insert();
    job("Queued 2").enabled(true).created("30 minutes").insert();
    // "Run now" on a recurring analytics job
    job("Nightly").cron(DAILY).scheduling(ONCE_ASAP).enabled(true).lastExecuted("1 day").insert();
    // just started, not waiting long yet
    job("Queued 3").enabled(true).created("1 minute").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        RUNNING_BLOCKING_OTHERS,
        null,
        blocker,
        "Dead analytics",
        "jobs waiting: 3",
        false);
  }

  @Test
  void testRunningJobBlockingDelayJob() {
    String blocker =
        job("Long continuous run")
            .type(CONTINUOUS_ANALYTICS_TABLE)
            .status(RUNNING)
            .lastExecuted("1 hour")
            .lastAlive("1 minute")
            .insert();
    continuousAnalytics("Continuous").lastExecuted("1 hour").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        RUNNING_BLOCKING_OTHERS,
        null,
        blocker,
        "Long continuous run",
        "jobs waiting: 1",
        false);
  }

  @Test
  void testRunningJobsNotBlockingOthers() {
    // nothing waiting
    job("Analytics").status(RUNNING).lastExecuted("2 hours").lastAlive("1 minute").insert();
    // waiting jobs of another type, recently created, disabled or in a queue
    job("Import").type(DATAVALUE_IMPORT).enabled(true).created("1 hour").insert();
    job("New").enabled(true).created("1 minute").insert();
    job("Disabled").cron(DAILY).enabled(false).status(DISABLED).insert();
    job("Queued").scheduling(ONCE_ASAP).queue("Nightly", 1).enabled(true).insert();
    // a run that only just started holds nothing up for long yet
    job("Fresh import").type(DATAVALUE_IMPORT).status(RUNNING).lastExecuted("1 minute").insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, RUNNING_BLOCKING_OTHERS, false);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        ENABLED_NOT_SCHEDULED,
        STUCK_RUNNING,
        NEVER_STARTED,
        RECURRING_MARKED_ONCE,
        DELAY_OVERDUE,
        RUNNING_BLOCKING_OTHERS
      })
  void testNoJobs(String check) {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, check, false);
  }

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------

  private JobRow job(String name) {
    return new JobRow(name);
  }

  /** An enabled Continuous Analytics job that runs 5 minutes after its last start. */
  private JobRow continuousAnalytics(String name) {
    return job(name).type(CONTINUOUS_ANALYTICS_TABLE).delay(300).enabled(true);
  }

  /** The default Housekeeping job, as defined by {@link JobType#HOUSEKEEPING}: enabled. */
  private JobRow housekeeping() {
    JobType.Defaults defaults = HOUSEKEEPING.getDefaults();
    return job(defaults.name())
        .uid(defaults.uid().getValue())
        .type(HOUSEKEEPING)
        .delay(defaults.delay())
        .enabled(true);
  }

  /**
   * A job configuration row. Times are given as an age ("2 hours") relative to the database clock,
   * so the fixture does not depend on the JVM time zone.
   */
  private final class JobRow {
    private String uid = CodeGenerator.generateUid();
    private final String name;
    private JobType type = ANALYTICS_TABLE;
    private SchedulingType scheduling = null;
    private String cron = null;
    private Integer delay = null;
    private boolean enabled = false;
    private JobStatus status = SCHEDULED;
    private String created = "1 minute";
    private String lastExecuted = null;
    private String lastFinished = null;
    private String lastAlive = null;
    private String queueName = null;
    private Integer queuePosition = null;

    private JobRow(String name) {
      this.name = name;
    }

    JobRow uid(String uid) {
      this.uid = uid;
      return this;
    }

    JobRow type(JobType type) {
      this.type = type;
      return this;
    }

    JobRow scheduling(SchedulingType scheduling) {
      this.scheduling = scheduling;
      return this;
    }

    JobRow cron(String cron) {
      this.cron = cron;
      return this;
    }

    JobRow delay(int delay) {
      this.delay = delay;
      return this;
    }

    JobRow enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    JobRow status(JobStatus status) {
      this.status = status;
      return this;
    }

    JobRow created(String age) {
      this.created = age;
      return this;
    }

    JobRow lastExecuted(String age) {
      this.lastExecuted = age;
      return this;
    }

    JobRow lastFinished(String age) {
      this.lastFinished = age;
      return this;
    }

    JobRow queue(String name, int position) {
      this.queueName = name;
      this.queuePosition = position;
      return this;
    }

    JobRow lastAlive(String age) {
      this.lastAlive = age;
      return this;
    }

    String insert() {
      SchedulingType schedulingType =
          scheduling != null
              ? scheduling
              : cron != null ? CRON : delay != null ? FIXED_DELAY : ONCE_ASAP;
      jdbcTemplate.update(
          """
          insert into jobconfiguration (jobconfigurationid, uid, name, created, lastupdated,
            jobtype, schedulingtype, cronexpression, delay, enabled, jobstatus, lastexecutedstatus,
            lastexecuted, lastfinished, lastalive, queuename, queueposition)
          values (nextval('hibernate_sequence'), ?, ?,
            now() - ?::interval, now() - ?::interval,
            ?, ?, ?, ?, ?, ?, ?,
            now() - ?::interval, now() - ?::interval, now() - ?::interval, ?, ?)
          """,
          uid,
          name,
          created,
          created,
          type.name(),
          schedulingType.name(),
          cron,
          delay,
          enabled,
          status.name(),
          NOT_STARTED.name(),
          lastExecuted,
          lastFinished,
          lastAlive,
          queueName,
          queuePosition);
      return uid;
    }
  }
}
