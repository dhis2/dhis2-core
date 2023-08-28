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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.hisp.dhis.scheduling.parameters.MockJobParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JobConfiguration}.
 *
 * @author Volker Schmidt
 */
class JobConfigurationTest {

  private JobParameters jobParameters;

  private JobConfiguration jobConfiguration;

  @BeforeEach
  void setUp() {
    jobParameters = new MockJobParameters();
    jobConfiguration = new JobConfiguration();
    jobConfiguration.setJobType(JobType.ANALYTICS_TABLE);
    jobConfiguration.setJobStatus(JobStatus.COMPLETED);
    jobConfiguration.setJobParameters(jobParameters);
    jobConfiguration.setEnabled(true);
    jobConfiguration.setCronExpression("0 0 6 * * ?");
  }

  @Test
  void hasNonConfigurableJobChangesFalse() {
    final JobConfiguration jc = new JobConfiguration();
    jc.setJobType(JobType.ANALYTICS_TABLE);
    jc.setJobStatus(JobStatus.COMPLETED);
    jc.setJobParameters(jobParameters);
    jc.setEnabled(true);
    assertFalse(jobConfiguration.hasNonConfigurableJobChanges(jc));
  }

  @Test
  void hasNonConfigurableJobChangesCron() {
    final JobConfiguration jc = new JobConfiguration();
    jc.setJobType(JobType.ANALYTICS_TABLE);
    jc.setJobStatus(JobStatus.COMPLETED);
    jc.setJobParameters(jobParameters);
    jc.setEnabled(true);
    jc.setCronExpression("0 0 12 * * ?");
    assertFalse(jobConfiguration.hasNonConfigurableJobChanges(jc));
  }

  @Test
  void hasNonConfigurableEnabled() {
    final JobConfiguration jc = new JobConfiguration();
    jc.setJobType(JobType.ANALYTICS_TABLE);
    jc.setJobStatus(JobStatus.COMPLETED);
    jc.setJobParameters(jobParameters);
    jc.setEnabled(false);
    assertTrue(jobConfiguration.hasNonConfigurableJobChanges(jc));
  }

  @Test
  void hasNonConfigurableJobChangesJobType() {
    final JobConfiguration jc = new JobConfiguration();
    jc.setJobType(JobType.DATA_INTEGRITY);
    jc.setJobStatus(JobStatus.COMPLETED);
    jc.setJobParameters(jobParameters);
    jc.setEnabled(true);
    assertTrue(jobConfiguration.hasNonConfigurableJobChanges(jc));
  }

  @Test
  void hasNonConfigurableJobChangesJobStatus() {
    final JobConfiguration jc = new JobConfiguration();
    jc.setJobType(JobType.ANALYTICS_TABLE);
    jc.setJobStatus(JobStatus.STOPPED);
    jc.setJobParameters(jobParameters);
    jc.setEnabled(true);
    assertTrue(jobConfiguration.hasNonConfigurableJobChanges(jc));
  }

  @Test
  void hasNonConfigurableJobChangesJobParameters() {
    final JobConfiguration jc = new JobConfiguration();
    jc.setJobType(JobType.ANALYTICS_TABLE);
    jc.setJobStatus(JobStatus.COMPLETED);
    jc.setJobParameters(new MockJobParameters());
    jc.setEnabled(true);
    assertTrue(jobConfiguration.hasNonConfigurableJobChanges(jc));
  }

  @Test
  void cronNextExecutionTimeWithDelay() {
    JobConfiguration config = new JobConfiguration(JobType.DATA_INTEGRITY);
    config.setCronExpression("0 40 8 ? * *"); // daily 8:40am

    ZonedDateTime todayMidnight = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault());
    ZonedDateTime today8am = todayMidnight.withHour(8);
    Duration maxCronDelay = Duration.ofHours(2);
    ZonedDateTime today8_40am = todayMidnight.withHour(8).withMinute(40);
    assertEquals(
        today8_40am.toInstant(), config.nextExecutionTime(today8am.toInstant(), maxCronDelay));
    ZonedDateTime today10am = todayMidnight.withHour(10);
    ZonedDateTime tomorrow8_40am = today8_40am.plusDays(1);

    // when the job never executed the next execution is on the next day the intended time
    // if now is already after the intended time
    assertEquals(
        tomorrow8_40am.toInstant(), config.nextExecutionTime(today10am.toInstant(), maxCronDelay));

    // when the job did execute last yesterday the intended time,
    // and we are still in the 2h window after 8:40am at 10am
    // the job still wants to run today 8:40am (immediately as that time has passed)
    config.setLastExecuted(Date.from(today8_40am.minusDays(1).toInstant()));
    assertEquals(
        today8_40am.toInstant(), config.nextExecutionTime(today10am.toInstant(), maxCronDelay));

    // if however, time has passed beyond the 2h window, today's execution is skipped
    // and the next execution will be tomorrow at the intended time
    ZonedDateTime today10_41am = todayMidnight.withHour(10).withMinute(41);
    assertEquals(
        tomorrow8_40am.toInstant(),
        config.nextExecutionTime(today10_41am.toInstant(), maxCronDelay));
  }
}
