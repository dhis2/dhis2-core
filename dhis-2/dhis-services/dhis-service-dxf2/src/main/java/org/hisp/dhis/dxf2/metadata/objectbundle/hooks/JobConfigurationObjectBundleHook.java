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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingType;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/**
 * @author Henning Håkonsen
 */
@Slf4j
@Component
@AllArgsConstructor
public class JobConfigurationObjectBundleHook extends AbstractObjectBundleHook<JobConfiguration> {

  private final JobConfigurationService jobConfigurationService;

  @Override
  public void validate(
      JobConfiguration jobConfiguration, ObjectBundle bundle, Consumer<ErrorReport> addReports) {

    @SuppressWarnings("unchecked")
    List<ErrorReport>[] box = new List[1];
    validateNoErrors(
        jobConfiguration,
        error -> {
          List<ErrorReport> list = box[0];
          if (list == null) {
            list = new ArrayList<>();
            box[0] = list;
          }
          addReports.accept(error);
        });
    List<ErrorReport> errorReports = box[0];
    if (errorReports == null || errorReports.isEmpty()) {
      log.info("Validation succeeded for job configuration: '{}'", jobConfiguration.getName());
    } else {
      log.info("Validation failed for job configuration: '{}'", jobConfiguration.getName());
      log.info(errorReports.toString());
    }
  }

  @Override
  public void preCreate(JobConfiguration config, ObjectBundle bundle) {
    if (isEmpty(config.getExecutedBy()) && config.getJobType().isValidUserRequiredForJob()) {
      UserDetails creator = CurrentUserUtil.getCurrentUserDetails();
      config.setExecutedBy(creator == null ? null : creator.getUid());
    }
    setDefaultJobParameters(config);
  }

  @Override
  public void preUpdate(
      JobConfiguration newObject, JobConfiguration persObject, ObjectBundle bundle) {

    setDefaultJobParameters(newObject);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Validates that there are no other jobs of the same job type which are scheduled with the same
   * cron expression.
   */
  private void validateUniqueCronTrigger(
      Consumer<ErrorReport> addReports, JobConfiguration jobConfiguration) {
    Set<JobConfiguration> jobConfigs =
        jobConfigurationService.getAllJobConfigurations().stream()
            .filter(
                jobConfig ->
                    jobConfig.getJobType().equals(jobConfiguration.getJobType())
                        && !Objects.equals(jobConfig.getUid(), jobConfiguration.getUid()))
            .collect(Collectors.toSet());

    for (JobConfiguration jobConfig : jobConfigs) {
      if (jobConfig.hasCronExpression()
          && jobConfig.getCronExpression().equals(jobConfiguration.getCronExpression())) {
        addReports.accept(
            new ErrorReport(
                JobConfiguration.class, ErrorCode.E7000, jobConfig.getCronExpression()));
      }
    }
  }

  private void validateNoErrors(
      final JobConfiguration jobConfiguration, Consumer<ErrorReport> addReports) {
    // Check whether jobConfiguration already exists

    JobConfiguration persistedJobConfiguration =
        jobConfigurationService.getJobConfigurationByUid(jobConfiguration.getUid());

    final JobConfiguration tempJobConfiguration =
        validatePersistedAndPrepareTempJobConfiguration(
            addReports, jobConfiguration, persistedJobConfiguration);

    setDefaultJobParameters(tempJobConfiguration);
    validateValidCronConfiguration(addReports, tempJobConfiguration);
    validateValidFixedDelayConfiguration(addReports, tempJobConfiguration);
    validateUniqueCronTrigger(addReports, tempJobConfiguration);

    // Validate parameters

    if (tempJobConfiguration.getJobParameters() != null) {
      tempJobConfiguration.getJobParameters().validate().ifPresent(addReports);
    } else {
      // Report error if JobType requires JobParameters, but it does not
      // exist in JobConfiguration

      if (tempJobConfiguration.getJobType().hasJobParameters()) {
        addReports.accept(
            new ErrorReport(this.getClass(), ErrorCode.E4029, tempJobConfiguration.getJobType()));
      }
    }
  }

  private JobConfiguration validatePersistedAndPrepareTempJobConfiguration(
      Consumer<ErrorReport> addReports,
      JobConfiguration jobConfiguration,
      JobConfiguration persistedJobConfiguration) {
    if (persistedJobConfiguration != null && !persistedJobConfiguration.isConfigurable()) {
      if (persistedJobConfiguration.hasNonConfigurableJobChanges(jobConfiguration)) {
        addReports.accept(
            new ErrorReport(
                JobConfiguration.class, ErrorCode.E7003, jobConfiguration.getJobType()));
      } else {
        persistedJobConfiguration.setCronExpression(jobConfiguration.getCronExpression());
        return persistedJobConfiguration;
      }
    }
    if (persistedJobConfiguration != null) {
      jobConfiguration.setJobType(persistedJobConfiguration.getJobType());

      if (executedByShouldBeKept(jobConfiguration, persistedJobConfiguration)) {
        jobConfiguration.setExecutedBy(persistedJobConfiguration.getExecutedBy());
      }
    }
    return jobConfiguration;
  }

  private void validateValidCronConfiguration(
      Consumer<ErrorReport> addReports, JobConfiguration jobConfiguration) {
    if (jobConfiguration.getSchedulingType() == SchedulingType.CRON) {
      if (jobConfiguration.getCronExpression() == null) {
        addReports.accept(
            new ErrorReport(JobConfiguration.class, ErrorCode.E7004, jobConfiguration.getUid()));
      } else if (!CronExpression.isValidExpression(jobConfiguration.getCronExpression())) {
        addReports.accept(new ErrorReport(JobConfiguration.class, ErrorCode.E7005));
      }
    }
  }

  private void validateValidFixedDelayConfiguration(
      Consumer<ErrorReport> addReports, JobConfiguration jobConfiguration) {

    if (jobConfiguration.getSchedulingType() == SchedulingType.FIXED_DELAY
        && jobConfiguration.getDelay() == null) {
      addReports.accept(
          new ErrorReport(JobConfiguration.class, ErrorCode.E7007, jobConfiguration.getUid()));
    }
  }

  /**
   * Sets default job parameters on the given job configuration if no parameters exist.
   *
   * @param jobConfiguration the {@link JobConfiguration}.
   */
  private void setDefaultJobParameters(JobConfiguration jobConfiguration) {
    if (jobConfiguration.getJobParameters() == null) {
      jobConfiguration.setJobParameters(getDefaultJobParameters(jobConfiguration));
    }
  }

  private JobParameters getDefaultJobParameters(JobConfiguration jobConfiguration) {
    if (jobConfiguration.getJobType().getJobParameters() == null) {
      return null;
    }

    try {
      return jobConfiguration.getJobType().getJobParameters().newInstance();
    } catch (InstantiationException | IllegalAccessException ex) {
      log.error("Failed to instantiate job configuration", DebugUtils.getStackTrace(ex));
    }

    return null;
  }

  /**
   * If no executedBy value is supplied in an updated job & the persisted job has an executedBy
   * value, then indicate that we should keep the existing value. This allows {@link
   * JobType#isValidUserRequiredForJob()} jobs to keep their executedBy value when updated,
   * otherwise the value is lost and jobs fail.
   *
   * @param updatedJobConfiguration updated job
   * @param persistedJobConfiguration persisted job
   * @return whether the executedBy value should be kept
   */
  private boolean executedByShouldBeKept(
      JobConfiguration updatedJobConfiguration, JobConfiguration persistedJobConfiguration) {
    JobType jobType = persistedJobConfiguration.getJobType();
    if (jobType == null) return false;

    return persistedJobConfiguration.getExecutedBy() != null
        && updatedJobConfiguration.getExecutedBy() == null
        && jobType.isValidUserRequiredForJob();
  }
}
