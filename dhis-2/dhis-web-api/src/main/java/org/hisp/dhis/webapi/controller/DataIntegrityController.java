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
package org.hisp.dhis.webapi.controller;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataintegrity.DataIntegrityCheck;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails;
import org.hisp.dhis.dataintegrity.DataIntegrityService;
import org.hisp.dhis.dataintegrity.DataIntegritySummary;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters.DataIntegrityReportType;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
@OpenApi.Tags("data")
@Controller
@RequestMapping("/dataIntegrity")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@AllArgsConstructor
public class DataIntegrityController {
  private final SchedulingManager schedulingManager;

  private final DataIntegrityService dataIntegrityService;

  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @PostMapping
  @ResponseBody
  public WebMessage runDataIntegrity(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @CheckForNull @RequestBody(required = false) Set<String> checksBody,
      @CurrentUser User currentUser) {
    Set<String> names = getCheckNames(checksBody, checks);
    return runDataIntegrityAsync(
            names, currentUser, "runDataIntegrity", DataIntegrityReportType.REPORT)
        .setLocation("/dataIntegrity/details?checks=" + toChecksList(names));
  }

  private WebMessage runDataIntegrityAsync(
      @Nonnull Set<String> checks,
      User currentUser,
      String description,
      DataIntegrityReportType type) {
    DataIntegrityJobParameters params = new DataIntegrityJobParameters();
    params.setChecks(checks);
    params.setType(type);
    JobConfiguration config =
        new JobConfiguration(description, JobType.DATA_INTEGRITY, null, params, true, true);
    config.setUserUid(currentUser.getUid());
    config.setAutoFields();

    if (!schedulingManager.executeNow(config)) {
      return conflict("Data integrity check is already running");
    }
    return jobConfigurationReport(config);
  }

  @GetMapping
  @ResponseBody
  public Collection<DataIntegrityCheck> getAvailableChecks(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @CheckForNull @RequestParam(required = false) String section) {
    Collection<DataIntegrityCheck> matches =
        dataIntegrityService.getDataIntegrityChecks(getCheckNames(checks));
    return section == null || section.isBlank()
        ? matches
        : matches.stream().filter(check -> section.equals(check.getSection())).collect(toList());
  }

  @GetMapping("/summary/running")
  @ResponseBody
  public Set<String> getRunningSummaryChecks() {
    return dataIntegrityService.getRunningSummaryChecks();
  }

  @GetMapping("/summary/completed")
  @ResponseBody
  public Set<String> getCompletedSummaryChecks() {
    return dataIntegrityService.getCompletedSummaryChecks();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @GetMapping("/summary")
  @ResponseBody
  public Map<String, DataIntegritySummary> getSummaries(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @RequestParam(required = false, defaultValue = "0") long timeout) {
    return dataIntegrityService.getSummaries(getCheckNames(checks), timeout);
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @PostMapping("/summary")
  @ResponseBody
  public WebMessage runSummariesCheck(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @CheckForNull @RequestBody(required = false) Set<String> checksBody,
      @CurrentUser User currentUser) {
    Set<String> names = getCheckNames(checksBody, checks);
    return runDataIntegrityAsync(
            names, currentUser, "runSummariesCheck", DataIntegrityReportType.SUMMARY)
        .setLocation("/dataIntegrity/summary?checks=" + toChecksList(names));
  }

  @GetMapping("/details/running")
  @ResponseBody
  public Set<String> getRunningDetailsChecks() {
    return dataIntegrityService.getRunningDetailsChecks();
  }

  @GetMapping("/details/completed")
  @ResponseBody
  public Set<String> getCompletedDetailsChecks() {
    return dataIntegrityService.getCompletedDetailsChecks();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @GetMapping("/details")
  @ResponseBody
  public Map<String, DataIntegrityDetails> getDetails(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @RequestParam(required = false, defaultValue = "0") long timeout) {
    return dataIntegrityService.getDetails(getCheckNames(checks), timeout);
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @PostMapping("/details")
  @ResponseBody
  public WebMessage runDetailsCheck(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @RequestBody(required = false) Set<String> checksBody,
      @CurrentUser User currentUser) {
    Set<String> names = getCheckNames(checksBody, checks);
    return runDataIntegrityAsync(
            names, currentUser, "runDetailsCheck", DataIntegrityReportType.DETAILS)
        .setLocation("/dataIntegrity/details?checks=" + toChecksList(names));
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @GetMapping("/{check}/summary")
  @ResponseBody
  public DataIntegritySummary getSummary(
      @PathVariable String check,
      @RequestParam(required = false, defaultValue = "0") long timeout) {
    Collection<DataIntegritySummary> summaries =
        dataIntegrityService.getSummaries(Set.of(check), timeout).values();
    return summaries.isEmpty() ? null : summaries.iterator().next();
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @GetMapping("/{check}/details")
  @ResponseBody
  public DataIntegrityDetails getDetails(
      @PathVariable String check,
      @RequestParam(required = false, defaultValue = "0") long timeout) {
    Collection<DataIntegrityDetails> details =
        dataIntegrityService.getDetails(Set.of(check), timeout).values();
    return details.isEmpty() ? null : details.iterator().next();
  }

  @SafeVarargs
  @Nonnull
  private static Set<String> getCheckNames(Set<String>... checks) {
    for (Set<String> names : checks) {
      if (!isEmpty(names)) {
        return names;
      }
    }
    return Set.of();
  }

  private String toChecksList(Collection<String> checks) {
    return checks == null || checks.isEmpty() ? "" : String.join(",", checks);
  }
}
