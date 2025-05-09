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

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress.Process;
import org.hisp.dhis.scheduling.JobProgress.Progress;
import org.hisp.dhis.scheduling.JobProgress.Stage;
import org.hisp.dhis.scheduling.JobProgress.Status;
import org.hisp.dhis.scheduling.JobSchedulerService;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for status information on the the scheduling of jobs.
 *
 * @author Jan Bernitt
 */
@OpenApi.Document(
    entity = JobConfiguration.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/scheduling")
@AllArgsConstructor
public class SchedulingController {

  private final JobSchedulerService jobSchedulerService;

  @GetMapping(
      value = {"/running/types", "/running/types/"},
      produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Collection<JobType> getRunningProgressTypesOnly() {
    return jobSchedulerService.getRunningTypes();
  }

  @GetMapping(
      value = {"/running", "/running/"},
      produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Map<JobType, Collection<ProcessInfo>> getRunningProgressTypes() {
    Function<JobType, Progress> running = jobSchedulerService::getRunningProgress;
    return jobSchedulerService.getRunningTypes().stream()
        .collect(toMap(Function.identity(), type -> flatten(running.apply(type))));
  }

  @GetMapping(
      value = {"/completed", "/completed/"},
      produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Map<JobType, Collection<ProcessInfo>> getCompletedProgressTypes() {
    Function<JobType, Progress> completed = jobSchedulerService::getCompletedProgress;
    return jobSchedulerService.getCompletedTypes().stream()
        .collect(toMap(Function.identity(), type -> flatten(completed.apply(type))));
  }

  @GetMapping(value = "/running/{type}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Progress getRunningProgress(@PathVariable("type") String type) {
    return jobSchedulerService.getRunningProgress(JobType.valueOf(type.toUpperCase()));
  }

  @GetMapping(value = "/completed/{type}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Progress getCompletedProgress(@PathVariable("type") String type) {
    return jobSchedulerService.getCompletedProgress(JobType.valueOf(type.toUpperCase()));
  }

  @PostMapping("/cancel/{type}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void requestCancellation(@PathVariable("type") String type) {
    jobSchedulerService.requestCancel(JobType.valueOf(type.toUpperCase()));
  }

  private static Collection<ProcessInfo> flatten(Progress progress) {
    return progress == null
        ? List.of()
        : progress.getSequence().stream().map(ProcessInfo::new).toList();
  }

  @Getter
  public static final class ProcessInfo {

    @JsonProperty private final String jobId;
    @JsonProperty private final Status status;
    @JsonProperty private final String description;
    @JsonProperty private final Date startedTime;
    @JsonProperty private final Date completedTime;
    @JsonProperty private final Date cancelledTime;
    @JsonProperty private final String summary;
    @JsonProperty private final String error;
    @JsonProperty private final List<String> stages;

    ProcessInfo(Process process) {
      this.jobId = process.getJobId();
      this.status = process.getStatus();
      this.startedTime = process.getStartedTime();
      this.completedTime = process.getCompletedTime();
      this.cancelledTime = process.getCancelledTime();
      this.description = process.getDescription();
      this.summary = process.getSummary();
      this.error = process.getError();
      this.stages = process.getStages().stream().map(ProcessInfo::getDescription).toList();
    }

    private static String getDescription(Stage stage) {
      return stage.getTotalItems() > 1
          ? stage.getDescription()
              + " ["
              + stage.getItems().size()
              + "/"
              + stage.getTotalItems()
              + "]"
          : stage.getDescription();
    }
  }
}
