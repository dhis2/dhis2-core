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

import org.hisp.dhis.common.CodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tests the data integrity checks in the "Job scheduler" section. Job configurations are inserted
 * with SQL because the states these checks detect cannot be created through the API.
 */
class DataIntegrityJobConfigurationsControllerTest extends AbstractDataIntegrityIntegrationTest {

  private static final String DETAILS_ID_TYPE = "jobConfigurations";

  private static final String ENABLED_NOT_SCHEDULED = "job_configurations_enabled_not_scheduled";
  private static final String STUCK_RUNNING = "job_configurations_stuck_running";
  private static final String NEVER_STARTED = "job_configurations_never_started";
  private static final String RECURRING_MARKED_ONCE = "job_configurations_recurring_marked_once";

  private static final String DAILY = "0 0 2 ? * *";

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void removeExistingJobs() {
    jdbcTemplate.update("delete from jobconfiguration");
  }

  // -------------------------------------------------------------------------
  // job_configurations_enabled_not_scheduled
  // -------------------------------------------------------------------------

  @Test
  void testEnabledRecurringJobStoredAsDisabled() {
    String stuck = job("Analytics").cron(DAILY).enabled(true).status("DISABLED").insert();
    job("Healthy").cron(DAILY).enabled(true).status("SCHEDULED").insert();
    // one-off jobs are out of scope: "Run now" would re-run an old import
    job("Old import").type("DATAVALUE_IMPORT").enabled(true).status("DISABLED").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, ENABLED_NOT_SCHEDULED, 50, stuck, "Analytics", null, true);
  }

  @Test
  void testRecurringJobsCorrectlyScheduledOrDisabled() {
    job("Enabled").cron(DAILY).enabled(true).status("SCHEDULED").insert();
    job("Disabled").cron(DAILY).enabled(false).status("DISABLED").insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, ENABLED_NOT_SCHEDULED, true);
  }

  @Test
  void testEnabledNotScheduledNoJobs() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, ENABLED_NOT_SCHEDULED, false);
  }

  // -------------------------------------------------------------------------
  // job_configurations_stuck_running
  // -------------------------------------------------------------------------

  @Test
  void testJobRunningPastStaleTimeout() {
    String stuck =
        job("Import").type("DATAVALUE_IMPORT").status("RUNNING").lastAlive("2 hours").insert();
    // analytics jobs use the longer analytics timeout (default 4 hours)
    job("Analytics").cron(DAILY).status("RUNNING").lastAlive("2 hours").insert();
    job("Busy import").type("DATAVALUE_IMPORT").status("RUNNING").lastAlive("1 minute").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, STUCK_RUNNING, null, stuck, "Import", "DATAVALUE_IMPORT", false);
  }

  @Test
  void testJobsRunningWithRecentProgress() {
    job("Import").type("DATAVALUE_IMPORT").status("RUNNING").lastAlive("1 minute").insert();
    job("Analytics").cron(DAILY).status("RUNNING").lastAlive("3 hours").insert();
    job("Finished").type("DATAVALUE_IMPORT").status("SCHEDULED").lastAlive("2 days").insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, STUCK_RUNNING, false);
  }

  @Test
  void testStuckRunningNoJobs() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, STUCK_RUNNING, false);
  }

  // -------------------------------------------------------------------------
  // job_configurations_never_started
  // -------------------------------------------------------------------------

  @Test
  void testOneOffJobNeverStarted() {
    String neverStarted =
        job("Old import").type("DATAVALUE_IMPORT").enabled(true).created("2 days").insert();
    job("New import").type("DATAVALUE_IMPORT").enabled(true).created("1 hour").insert();
    job("Done import")
        .type("DATAVALUE_IMPORT")
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
        "DATAVALUE_IMPORT",
        false);
  }

  @Test
  void testOneOffJobsStartedOrRecent() {
    job("New import").type("DATAVALUE_IMPORT").enabled(true).created("1 hour").insert();
    job("Done import")
        .type("DATAVALUE_IMPORT")
        .created("2 days")
        .lastExecuted("2 days")
        .lastFinished("2 days")
        .insert();
    // recurring jobs that have not run yet are not one-off jobs
    job("Weekly").cron(DAILY).enabled(true).created("2 days").insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, NEVER_STARTED, false);
  }

  @Test
  void testNeverStartedNoJobs() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, NEVER_STARTED, false);
  }

  // -------------------------------------------------------------------------
  // job_configurations_recurring_marked_once
  // -------------------------------------------------------------------------

  @Test
  void testRecurringJobMarkedAsOneOff() {
    String marked = job("Old cron").cron(DAILY).scheduling("ONCE_ASAP").status("DISABLED").insert();
    // "Run now" on a recurring job sets ONCE_ASAP + SCHEDULED until the run finishes
    job("Run now").cron(DAILY).scheduling("ONCE_ASAP").enabled(true).status("SCHEDULED").insert();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE, RECURRING_MARKED_ONCE, 50, marked, "Old cron", null, true);
  }

  @Test
  void testRecurringJobsWithRecurringType() {
    job("Cron").cron(DAILY).enabled(false).status("DISABLED").insert();
    job("Delay").delay(20).enabled(true).status("SCHEDULED").insert();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, RECURRING_MARKED_ONCE, true);
  }

  @Test
  void testRecurringMarkedOnceNoJobs() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, RECURRING_MARKED_ONCE, false);
  }

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------

  private JobRow job(String name) {
    return new JobRow(name);
  }

  /**
   * A job configuration row. Times are given as an age ("2 hours") relative to the database clock,
   * so the fixture does not depend on the JVM time zone.
   */
  private final class JobRow {
    private final String uid = CodeGenerator.generateUid();
    private final String name;
    private String type = "ANALYTICS_TABLE";
    private String scheduling = null;
    private String cron = null;
    private Integer delay = null;
    private boolean enabled = false;
    private String status = "SCHEDULED";
    private String created = "1 minute";
    private String lastExecuted = null;
    private String lastFinished = null;
    private String lastAlive = null;

    private JobRow(String name) {
      this.name = name;
    }

    JobRow type(String type) {
      this.type = type;
      return this;
    }

    JobRow scheduling(String scheduling) {
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

    JobRow status(String status) {
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

    JobRow lastAlive(String age) {
      this.lastAlive = age;
      return this;
    }

    String insert() {
      String schedulingType =
          scheduling != null
              ? scheduling
              : cron != null ? "CRON" : delay != null ? "FIXED_DELAY" : "ONCE_ASAP";
      jdbcTemplate.update(
          """
          insert into jobconfiguration (jobconfigurationid, uid, name, created, lastupdated,
            jobtype, schedulingtype, cronexpression, delay, enabled, jobstatus, lastexecutedstatus,
            lastexecuted, lastfinished, lastalive)
          values (nextval('hibernate_sequence'), ?, ?,
            now() - ?::interval, now() - ?::interval,
            ?, ?, ?, ?, ?, ?, 'NOT_STARTED',
            now() - ?::interval, now() - ?::interval, now() - ?::interval)
          """,
          uid,
          name,
          created,
          created,
          type,
          schedulingType,
          cron,
          delay,
          enabled,
          status,
          lastExecuted,
          lastFinished,
          lastAlive);
      return uid;
    }
  }
}
