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
package org.hisp.dhis.webapi.controller.scheduling;

import static org.hisp.dhis.security.Authorities.F_JOB_LOG_READ;
import static org.hisp.dhis.security.Authorities.F_PERFORM_MAINTENANCE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobEntry;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobProgress.Progress;
import org.hisp.dhis.scheduling.JobRunErrors;
import org.hisp.dhis.scheduling.JobRunErrorsParams;
import org.hisp.dhis.scheduling.JobSchedulerService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.JobTypes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple controller for API endpoints
 *
 * @author Henning Håkonsen
 */
@RestController
@RequestMapping("/api/jobConfigurations")
@RequiredArgsConstructor
@OpenApi.Document(classifiers = {"team:platform", "purpose:support"})
public class JobConfigurationController
    extends AbstractCrudController<JobConfiguration, GetObjectListParams> {

  private final JobConfigurationService jobConfigurationService;
  private final JobSchedulerService jobSchedulerService;

  @Maturity.Beta
  @RequiresAuthority(anyOf = F_JOB_LOG_READ)
  @GetMapping("/errors")
  public List<JobRunErrors> getJobRunErrors(JobRunErrorsParams params) {
    return jobConfigurationService.findJobRunErrors(params);
  }

  @Maturity.Beta
  @GetMapping("{uid}/errors")
  public JobRunErrors getJobRunErrors(
      @PathVariable("uid") @OpenApi.Param({UID.class, JobConfiguration.class}) UID uid)
      throws NotFoundException, ForbiddenException {
    checkExecutingUserOrAdmin(uid, true);
    List<JobRunErrors> errors =
        jobConfigurationService.findJobRunErrors(new JobRunErrorsParams().setJob(uid));
    return errors.isEmpty() ? JsonMixed.of("{}").as(JobRunErrors.class) : errors.get(0);
  }

  @Maturity.Beta
  @GetMapping("/due")
  public List<JobEntry> getDueJobConfigurations(
      @RequestParam int seconds,
      @RequestParam(required = false, defaultValue = "false") boolean includeWaiting) {
    return jobConfigurationService.getDueJobConfigurations(seconds, includeWaiting);
  }

  @Maturity.Beta
  @GetMapping("/stale")
  public List<JobEntry> getStaleJobConfigurations(@RequestParam int seconds) {
    return jobConfigurationService.getStaleConfigurations(seconds);
  }

  @GetMapping(
      value = "/jobTypesExtended",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public @ResponseBody Map<String, Map<String, Property>> getJobTypesExtended() {
    return jobConfigurationService.getJobParametersSchema();
  }

  @GetMapping(value = "/jobTypes", produces = APPLICATION_JSON_VALUE)
  public JobTypes getJobTypeInfo() {
    return new JobTypes(jobConfigurationService.getJobTypeInfo());
  }

  @PostMapping(
      value = "{uid}/execute",
      produces = {APPLICATION_JSON_VALUE, "application/javascript"})
  public ObjectReport executeNow(@PathVariable("uid") UID uid)
      throws NotFoundException, ConflictException {

    jobSchedulerService.executeNow(uid);

    // OBS! This response is kept for better backwards compatibility
    return new ObjectReport(JobConfiguration.class, 0);
  }

  @PostMapping("{uid}/cancel")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelExecution(@PathVariable("uid") UID uid)
      throws NotFoundException, ForbiddenException {
    checkExecutingUserOrAdmin(uid, false);
    jobSchedulerService.requestCancel(uid);
  }

  @PostMapping("{uid}/revert")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revertExecution(@PathVariable("uid") UID uid)
      throws NotFoundException, ForbiddenException, ConflictException {
    jobSchedulerService.revertNow(uid);
  }

  @GetMapping("{uid}/progress")
  public Progress getProgress(@PathVariable("uid") UID uid)
      throws ForbiddenException, NotFoundException {
    checkExecutingUserOrAdmin(uid, true);
    return jobSchedulerService.getProgress(uid);
  }

  @Maturity.Beta
  @GetMapping("{uid}/progress/errors")
  public List<JobProgress.Error> getErrors(@PathVariable("uid") UID uid)
      throws ForbiddenException, NotFoundException {
    checkExecutingUserOrAdmin(uid, true);
    return jobSchedulerService.getErrors(uid);
  }

  @Maturity.Beta
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  @PostMapping("clean")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteDoneJobs(@RequestParam int minutes) {
    jobConfigurationService.deleteFinishedJobs(minutes);
  }

  @PostMapping("{uid}/enable")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void enable(@PathVariable("uid") UID uid) throws NotFoundException, ConflictException {
    JobConfiguration obj = jobConfigurationService.getJobConfigurationByUid(uid.getValue());
    if (obj == null) throw new NotFoundException(JobConfiguration.class, uid.getValue());
    checkModifiable(obj, "Job %s is a system job that cannot be modified.");
    if (!obj.isEnabled()) {
      if (!obj.isReadyToRun())
        throw new ConflictException("Job requires schedule setup before it can be enabled.");
      obj.setEnabled(true);
      jobConfigurationService.updateJobConfiguration(obj);
    }
  }

  @PostMapping("{uid}/disable")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disable(@PathVariable("uid") UID uid) throws NotFoundException, ConflictException {
    JobConfiguration obj = jobConfigurationService.getJobConfigurationByUid(uid.getValue());
    if (obj == null) throw new NotFoundException(JobConfiguration.class, uid.getValue());
    checkModifiable(obj, "Job %s is a system job that cannot be modified.");
    if (obj.isEnabled()) {
      obj.setEnabled(false);
      jobConfigurationService.updateJobConfiguration(obj);
    }
  }

  @Override
  protected void preCreateEntity(JobConfiguration jobConfiguration) throws ConflictException {
    checkModifiable(jobConfiguration, "Job %s must be configurable but was not.");
  }

  @Override
  protected void preUpdateEntity(JobConfiguration before, JobConfiguration after)
      throws ConflictException {
    checkModifiable(before, "Job %s is a system job that cannot be modified.");
    checkModifiable(after, "Job %s can not be changed into a system job.");
    after.setEnabled(before.isEnabled());
  }

  @Override
  protected void preDeleteEntity(JobConfiguration jobConfiguration) throws ConflictException {
    checkModifiable(jobConfiguration, "Job %s is a system job that cannot be deleted.");
  }

  @Override
  protected void preUpdateItems(JobConfiguration jobConfiguration, IdentifiableObjects items)
      throws ConflictException {
    checkModifiable(jobConfiguration, "Job %s is a system job that cannot be modified.");
  }

  private void checkModifiable(JobConfiguration configuration, String message)
      throws ConflictException {
    if (!configuration.isConfigurable()) {
      String identifier = configuration.getUid();
      if (identifier == null) {
        identifier = configuration.getName();
      }
      throw new ConflictException(String.format(message, identifier));
    }
  }

  private void checkExecutingUserOrAdmin(UID uid, boolean read)
      throws NotFoundException, ForbiddenException {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    JobConfiguration obj = jobConfigurationService.getJobConfigurationByUid(uid.getValue());
    if (obj == null) throw new NotFoundException(JobConfiguration.class, uid.getValue());
    boolean isAuthorized =
        currentUser.isSuper()
            || !read && currentUser.isAuthorized("F_PERFORM_MAINTENANCE")
            || read && currentUser.isAuthorized(F_JOB_LOG_READ.toString())
            || currentUser.getUid().equals(obj.getExecutedBy());
    if (!isAuthorized) throw new ForbiddenException(JobConfiguration.class, obj.getUid());
  }
}
