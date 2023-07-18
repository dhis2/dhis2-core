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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.List;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.ContinuousAnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.DataSynchronizationJobParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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

  @Mock private JobService jobService;

  @Mock private Job job;

  @InjectMocks private JobConfigurationObjectBundleHook hook;

  private JobConfiguration analyticsTableJobConfig;

  @BeforeEach
  public void setUp() {
    analyticsTableJobConfig = new JobConfiguration();
    analyticsTableJobConfig.setJobType(JobType.ANALYTICSTABLE_UPDATE);
    analyticsTableJobConfig.setEnabled(true);
  }

  @Test
  void validateInternalNonConfigurableChangeError() {
    Mockito.when(jobConfigurationService.getJobConfigurationByUid(Mockito.eq("jsdhJSJHD")))
        .thenReturn(analyticsTableJobConfig);
    Mockito.when(jobService.getJob(Mockito.eq(JobType.ANALYTICSTABLE_UPDATE))).thenReturn(job);

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid("jsdhJSJHD");
    jobConfiguration.setJobType(JobType.ANALYTICSTABLE_UPDATE);
    jobConfiguration.setCronExpression(CRON_HOURLY);
    jobConfiguration.setEnabled(false);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    Assertions.assertEquals(1, errorReports.size());
    Assertions.assertEquals(ErrorCode.E7003, errorReports.get(0).getErrorCode());
  }

  @Test
  void validateInternalNonConfigurableChange() {
    Mockito.when(jobConfigurationService.getJobConfigurationByUid(Mockito.eq("jsdhJSJHD")))
        .thenReturn(analyticsTableJobConfig);
    Mockito.when(jobService.getJob(Mockito.eq(JobType.ANALYTICSTABLE_UPDATE))).thenReturn(job);

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid("jsdhJSJHD");
    jobConfiguration.setJobType(JobType.ANALYTICSTABLE_UPDATE);
    jobConfiguration.setCronExpression(CRON_HOURLY);
    jobConfiguration.setEnabled(true);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    Assertions.assertEquals(0, errorReports.size());
  }

  @Test
  void validateInternalNonConfigurableShownValidationErrorNonE7010() {
    Mockito.when(jobConfigurationService.getJobConfigurationByUid(Mockito.eq("jsdhJSJHD")))
        .thenReturn(analyticsTableJobConfig);
    Mockito.when(jobService.getJob(Mockito.eq(JobType.ANALYTICSTABLE_UPDATE))).thenReturn(job);
    Mockito.when(job.validate()).thenReturn(new ErrorReport(Class.class, ErrorCode.E7000));

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid("jsdhJSJHD");
    jobConfiguration.setJobType(JobType.ANALYTICSTABLE_UPDATE);
    jobConfiguration.setCronExpression(CRON_HOURLY);
    jobConfiguration.setEnabled(true);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    Assertions.assertEquals(1, errorReports.size());
    Assertions.assertEquals(ErrorCode.E7000, errorReports.get(0).getErrorCode());
  }

  @Test
  void validateInternalNonConfigurableShownValidationErrorE7010Configurable() {
    Mockito.when(jobConfigurationService.getJobConfigurationByUid(Mockito.eq("jsdhJSJHD")))
        .thenReturn(analyticsTableJobConfig);
    Mockito.when(jobService.getJob(Mockito.eq(JobType.DATA_SYNC))).thenReturn(job);
    Mockito.when(job.validate()).thenReturn(new ErrorReport(Class.class, ErrorCode.E7010));

    analyticsTableJobConfig.setJobType(JobType.DATA_SYNC);
    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid("jsdhJSJHD");
    jobConfiguration.setJobType(JobType.DATA_SYNC);
    jobConfiguration.setCronExpression(CRON_HOURLY);
    jobConfiguration.setEnabled(true);

    DataSynchronizationJobParameters jobParameters = new DataSynchronizationJobParameters();
    jobParameters.setPageSize(200);
    jobConfiguration.setJobParameters(jobParameters);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    Assertions.assertEquals(1, errorReports.size());
    Assertions.assertEquals(ErrorCode.E7010, errorReports.get(0).getErrorCode());
  }

  @Test
  void validateInternalNonConfigurableShownValidationErrorE7010NoPrevious() {
    Mockito.when(jobConfigurationService.getJobConfigurationByUid(Mockito.eq("jsdhJSJHD")))
        .thenReturn(null);
    Mockito.when(jobService.getJob(Mockito.eq(JobType.ANALYTICSTABLE_UPDATE))).thenReturn(job);
    Mockito.when(job.validate()).thenReturn(new ErrorReport(Class.class, ErrorCode.E7010));

    analyticsTableJobConfig.setJobType(JobType.ANALYTICSTABLE_UPDATE);
    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid("jsdhJSJHD");
    jobConfiguration.setJobType(JobType.ANALYTICSTABLE_UPDATE);
    jobConfiguration.setCronExpression(CRON_HOURLY);
    jobConfiguration.setEnabled(true);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    Assertions.assertEquals(1, errorReports.size());
    Assertions.assertEquals(ErrorCode.E7010, errorReports.get(0).getErrorCode());
  }

  @Test
  void validateInternalNonConfigurableIgnoredValidationErrorE7010() {
    Mockito.when(jobConfigurationService.getJobConfigurationByUid(Mockito.eq("jsdhJSJHD")))
        .thenReturn(analyticsTableJobConfig);
    Mockito.when(jobService.getJob(Mockito.eq(JobType.ANALYTICSTABLE_UPDATE))).thenReturn(job);
    Mockito.when(job.validate()).thenReturn(new ErrorReport(Class.class, ErrorCode.E7010));

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid("jsdhJSJHD");
    jobConfiguration.setJobType(JobType.ANALYTICSTABLE_UPDATE);
    jobConfiguration.setCronExpression(CRON_HOURLY);
    jobConfiguration.setEnabled(true);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    Assertions.assertEquals(0, errorReports.size());
  }

  @Test
  void validateCronExpressionForCronTypeJobs() {
    String jobConfigUid = "jsdhJSJHD";
    Mockito.when(jobConfigurationService.getJobConfigurationByUid(Mockito.eq(jobConfigUid)))
        .thenReturn(analyticsTableJobConfig);
    Mockito.when(jobService.getJob(Mockito.eq(JobType.ANALYTICSTABLE_UPDATE))).thenReturn(job);

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid(jobConfigUid);
    jobConfiguration.setJobType(JobType.ANALYTICSTABLE_UPDATE);
    jobConfiguration.setEnabled(true);

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    Assertions.assertEquals(1, errorReports.size());
    Assertions.assertEquals(ErrorCode.E7004, errorReports.get(0).getErrorCode());
  }

  @Test
  void validateDelayForFixedIntervalTypeJobs() {
    String jobConfigUid = "o8kG3Qk3nG3";
    JobConfiguration contAnalyticsTableJobConfig = new JobConfiguration();
    contAnalyticsTableJobConfig.setUid(jobConfigUid);
    contAnalyticsTableJobConfig.setJobType(JobType.CONTINUOUS_ANALYTICS_TABLE);

    Mockito.when(jobConfigurationService.getJobConfigurationByUid(Mockito.eq(jobConfigUid)))
        .thenReturn(contAnalyticsTableJobConfig);
    Mockito.when(jobService.getJob(Mockito.eq(JobType.CONTINUOUS_ANALYTICS_TABLE))).thenReturn(job);

    JobConfiguration jobConfiguration = new JobConfiguration();
    jobConfiguration.setUid(jobConfigUid);
    jobConfiguration.setJobType(JobType.CONTINUOUS_ANALYTICS_TABLE);
    jobConfiguration.setJobParameters(new ContinuousAnalyticsJobParameters(1, null, null));

    List<ErrorReport> errorReports = hook.validate(jobConfiguration, null);
    Assertions.assertEquals(1, errorReports.size());
    Assertions.assertEquals(ErrorCode.E7007, errorReports.get(0).getErrorCode());
  }
}
