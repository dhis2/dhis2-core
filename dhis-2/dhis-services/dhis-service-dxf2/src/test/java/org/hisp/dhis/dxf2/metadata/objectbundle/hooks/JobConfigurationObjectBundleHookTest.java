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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingType;
import org.hisp.dhis.scheduling.parameters.ContinuousAnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JobConfigurationObjectBundleHook}.
 *
 * @author Volker Schmidt
 */
@ExtendWith(MockitoExtension.class)
class JobConfigurationObjectBundleHookTest {
  private static final String CRON_HOURLY = "0 0 * ? * *";

  @Mock private JobConfigurationService jobConfigurationService;

  @Mock private Job job;

  @InjectMocks private JobConfigurationObjectBundleHook hook;

  private JobConfiguration persistedConfig;

  @BeforeEach
  public void setUp() {
    persistedConfig = new JobConfiguration();
    persistedConfig.setJobType(JobType.FILE_RESOURCE_CLEANUP);
    persistedConfig.setJobStatus(JobStatus.SCHEDULED);
    persistedConfig.setSchedulingType(SchedulingType.CRON);
    persistedConfig.setEnabled(true);
  }

  @Test
  void validateInternalNonConfigurableChangeError() {
    when(jobConfigurationService.getJobConfigurationByUid("jsdhJSJHD")).thenReturn(persistedConfig);

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid("jsdhJSJHD");
    jobConfiguration.setJobType(JobType.FILE_RESOURCE_CLEANUP);
    jobConfiguration.setCronExpression(CRON_HOURLY);
    jobConfiguration.setEnabled(false);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    assertEquals(1, errorReports.size());
    assertEquals(ErrorCode.E7003, errorReports.get(0).getErrorCode());
  }

  @Test
  void validateInternalNonConfigurableChange() {
    when(jobConfigurationService.getJobConfigurationByUid("jsdhJSJHD")).thenReturn(persistedConfig);

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid("jsdhJSJHD");
    jobConfiguration.setJobType(JobType.FILE_RESOURCE_CLEANUP);
    jobConfiguration.setCronExpression(CRON_HOURLY);
    jobConfiguration.setEnabled(true);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    assertEquals(0, errorReports.size());
  }

  @Test
  void validateInternalNonConfigurableIgnoredValidationErrorE7010() {
    when(jobConfigurationService.getJobConfigurationByUid("jsdhJSJHD")).thenReturn(persistedConfig);

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid("jsdhJSJHD");
    jobConfiguration.setJobType(JobType.FILE_RESOURCE_CLEANUP);
    jobConfiguration.setCronExpression(CRON_HOURLY);
    jobConfiguration.setEnabled(true);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    assertEquals(0, errorReports.size());
  }

  @Test
  void validateCronExpressionForCronTypeJobs() {
    String jobConfigUid = "jsdhJSJHD";
    when(jobConfigurationService.getJobConfigurationByUid(jobConfigUid))
        .thenReturn(persistedConfig);

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid(jobConfigUid);
    jobConfiguration.setJobType(JobType.FILE_RESOURCE_CLEANUP);
    jobConfiguration.setJobStatus(JobStatus.SCHEDULED);
    jobConfiguration.setSchedulingType(SchedulingType.CRON);
    jobConfiguration.setEnabled(true);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    assertEquals(1, errorReports.size());
    assertEquals(ErrorCode.E7004, errorReports.get(0).getErrorCode());
  }

  @Test
  void validateDelayForFixedIntervalTypeJobs() {
    String jobConfigUid = "o8kG3Qk3nG3";
    JobConfiguration contAnalyticsTableJobConfig = new JobConfiguration();
    contAnalyticsTableJobConfig.setUid(jobConfigUid);
    contAnalyticsTableJobConfig.setJobType(JobType.CONTINUOUS_ANALYTICS_TABLE);
    contAnalyticsTableJobConfig.setSchedulingType(SchedulingType.FIXED_DELAY);

    when(jobConfigurationService.getJobConfigurationByUid(jobConfigUid))
        .thenReturn(contAnalyticsTableJobConfig);

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid(jobConfigUid);
    jobConfiguration.setJobType(JobType.CONTINUOUS_ANALYTICS_TABLE);
    jobConfiguration.setSchedulingType(SchedulingType.FIXED_DELAY);
    jobConfiguration.setJobParameters(new ContinuousAnalyticsJobParameters(1, null, null, true));

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    assertEquals(1, errorReports.size());
    assertEquals(ErrorCode.E7007, errorReports.get(0).getErrorCode());
  }

  private static JobConfiguration monitoringJob(boolean replaceResults, String executedBy) {
    MonitoringJobParameters parameters = new MonitoringJobParameters();
    parameters.setPersistResults(!replaceResults);
    parameters.setReplaceResults(replaceResults);
    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid("monitorJob1");
    jobConfiguration.setJobType(JobType.MONITORING);
    jobConfiguration.setJobStatus(JobStatus.SCHEDULED);
    jobConfiguration.setSchedulingType(SchedulingType.CRON);
    jobConfiguration.setCronExpression("0 0 3 ? * *");
    jobConfiguration.setEnabled(true);
    jobConfiguration.setJobParameters(parameters);
    jobConfiguration.setExecutedBy(executedBy);
    return jobConfiguration;
  }

  @Test
  void validateReplacingMonitoringJobRejectsExecutedBy() {
    List<ErrorReport> errorReports = hook.validate(monitoringJob(true, "userUid0001"), null);
    assertEquals(1, errorReports.size());
    assertEquals(ErrorCode.E7008, errorReports.get(0).getErrorCode());
  }

  @Test
  void validateReplacingMonitoringJobUpdateRejectsExecutedBy() {
    when(jobConfigurationService.getJobConfigurationByUid("monitorJob1"))
        .thenReturn(monitoringJob(true, null));

    List<ErrorReport> errorReports = hook.validate(monitoringJob(true, "userUid0001"), null);
    assertEquals(1, errorReports.size());
    assertEquals(ErrorCode.E7008, errorReports.get(0).getErrorCode());
  }

  @Test
  void validateMonitoringJobRejectsPersistAndReplace() {
    JobConfiguration jobConfiguration = monitoringJob(true, null);
    ((MonitoringJobParameters) jobConfiguration.getJobParameters()).setPersistResults(true);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    assertEquals(1, errorReports.size());
    assertEquals(ErrorCode.E4025, errorReports.get(0).getErrorCode());
  }

  @Test
  void validateReplacingMonitoringJobWithoutExecutedBy() {
    assertEquals(List.of(), hook.validate(monitoringJob(true, null), null));
  }

  @Test
  void validateNonReplacingMonitoringJobWithExecutedBy() {
    assertEquals(List.of(), hook.validate(monitoringJob(false, "userUid0001"), null));
  }

  /**
   * A replacing run deletes persisted results it does not find again, so it must evaluate as the
   * system user: a user's dimension constraints or analytics access would hide violations, which
   * would then be deleted. Monitoring jobs run as the system user because the job type does not
   * record its creator as executing user; E7008 rejects an explicit one.
   */
  @Test
  void testMonitoringJobsRunAsSystemUser() {
    assertFalse(JobType.MONITORING.isValidUserRequiredForJob());
  }
}
