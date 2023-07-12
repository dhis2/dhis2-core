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
package org.hisp.dhis.webapi.controller.scheduling;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobQueueService;
import org.hisp.dhis.scheduling.SchedulingType;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API for scheduler list and named queues (sequences).
 *
 * @author Jan Bernitt
 */
@OpenApi.Tags("system")
@RestController
@RequestMapping(value = "/scheduler")
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class JobSchedulerController {
  private final JobConfigurationService jobConfigurationService;

  private final JobQueueService jobQueueService;

  @GetMapping
  public List<SchedulerEntry> getSchedulerEntries(@RequestParam(required = false) String order) {
    Map<String, List<JobConfiguration>> configsByQueueNameOrUid =
        jobConfigurationService.getAllJobConfigurations().stream()
            .collect(groupingBy(JobConfiguration::getQueueIdentifier));
    Comparator<SchedulerEntry> sortBy =
        "name".equals(order)
            ? comparing(SchedulerEntry::getName)
            : comparing(SchedulerEntry::getNextExecutionTime, nullsLast(naturalOrder()));
    return configsByQueueNameOrUid.values().stream()
        .map(SchedulerEntry::of)
        .sorted(sortBy)
        .collect(toList());
  }

  @GetMapping("/queueable")
  public List<SchedulerEntry> getQueueableJobs(@RequestParam(required = false) String name) {
    Predicate<JobConfiguration> nameFilter =
        name == null || name.isEmpty()
            ? config -> true
            : config -> !name.equals(config.getQueueName());
    return jobConfigurationService.getAllJobConfigurations().stream()
        .filter(JobConfiguration::isConfigurable)
        .filter(not(JobConfiguration::isLeaderOnlyJob))
        .filter(config -> config.getSchedulingType() != SchedulingType.FIXED_DELAY)
        .filter(config -> !config.isUsedInQueue())
        .filter(nameFilter)
        .map(SchedulerEntry::of)
        .sorted(comparing(SchedulerEntry::getName))
        .collect(toList());
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @OpenApi.Property
  static class SchedulerQueue {
    @JsonProperty String name;

    @JsonProperty(required = true)
    String cronExpression;

    @JsonProperty(required = true)
    @OpenApi.Property({UID[].class, JobConfiguration.class})
    List<String> sequence = new ArrayList<>();
  }

  @GetMapping("/queues")
  public Set<String> getQueueNames() {
    return jobQueueService.getQueueNames();
  }

  @GetMapping("/queues/{name}")
  public SchedulerQueue getQueue(@PathVariable String name) throws NotFoundException {
    List<JobConfiguration> sequence = jobQueueService.getQueue(name);
    JobConfiguration trigger = sequence.get(0);
    return new SchedulerQueue(
        trigger.getQueueName(),
        trigger.getCronExpression(),
        sequence.stream().map(IdentifiableObject::getUid).collect(toList()));
  }

  @PostMapping("/queues/{name}")
  @ResponseStatus(HttpStatus.CREATED)
  public void createQueue(@PathVariable String name, @RequestBody SchedulerQueue queue)
      throws NotFoundException, ConflictException {
    jobQueueService.createQueue(name, queue.getCronExpression(), queue.getSequence());
  }

  @PutMapping("/queues/{name}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateQueue(@PathVariable String name, @RequestBody SchedulerQueue queue)
      throws NotFoundException, ConflictException {
    jobQueueService.updateQueue(
        name, queue.getName(), queue.getCronExpression(), queue.getSequence());
  }

  @DeleteMapping("/queues/{name}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteQueue(@PathVariable String name) throws NotFoundException {
    jobQueueService.deleteQueue(name);
  }
}
