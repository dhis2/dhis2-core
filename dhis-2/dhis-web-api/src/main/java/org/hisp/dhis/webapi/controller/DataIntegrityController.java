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
package org.hisp.dhis.webapi.controller;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.common.collection.CollectionUtils.isEmpty;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.security.Authorities.F_PERFORM_MAINTENANCE;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobExecutionService;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.DataIntegrityDetailsJobParameters;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters.DataIntegrityReportType;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.PrometheusTextBuilder;
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
@OpenApi.Document(
    entity = DataIntegrityCheck.class,
    classifiers = {"team:platform", "purpose:support"})
@Controller
@RequestMapping("/api/dataIntegrity")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@AllArgsConstructor
public class DataIntegrityController {

  private final DataIntegrityService dataIntegrityService;

  private final JobExecutionService jobExecutionService;

  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  @PostMapping
  @ResponseBody
  public WebMessage runDataIntegrity(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @CheckForNull @RequestBody(required = false) Set<String> checksBody,
      @CurrentUser UserDetails currentUser)
      throws ConflictException {
    Set<String> names = getCheckNames(checksBody, checks);
    return runDataIntegrityAsync(names, currentUser, DataIntegrityReportType.SUMMARY)
        .setLocation("/dataIntegrity/details?checks=" + toChecksList(names));
  }

  private WebMessage runDataIntegrityAsync(
      @Nonnull Set<String> checks, UserDetails currentUser, DataIntegrityReportType type)
      throws ConflictException {
    JobType jobType =
        type == DataIntegrityReportType.DETAILS
            ? JobType.DATA_INTEGRITY_DETAILS
            : JobType.DATA_INTEGRITY;
    JobConfiguration config = new JobConfiguration(jobType);
    config.setExecutedBy(currentUser.getUid());
    JobParameters parameters =
        type == DataIntegrityReportType.DETAILS
            ? new DataIntegrityDetailsJobParameters(checks)
            : new DataIntegrityJobParameters(type, checks);
    config.setJobParameters(parameters);

    jobExecutionService.executeOnceNow(config);

    return jobConfigurationReport(config);
  }

  @GetMapping
  @ResponseBody
  public Collection<DataIntegrityCheck> getAvailableChecks(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @CheckForNull @RequestParam(required = false) String section,
      @CheckForNull @RequestParam(required = false) Boolean slow,
      @CheckForNull @RequestParam(required = false) Boolean programmatic) {
    Collection<DataIntegrityCheck> matches =
        dataIntegrityService.getDataIntegrityChecks(getCheckNames(checks));
    Predicate<DataIntegrityCheck> filter = check -> true;
    if (section != null && !section.isBlank()) filter = check -> section.equals(check.getSection());
    if (slow != null) filter = filter.and(check -> check.isSlow() == slow);
    if (programmatic != null) filter = filter.and(check -> check.isProgrammatic() == programmatic);
    return matches.stream().filter(filter).collect(toList());
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

  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  @GetMapping("/summary")
  @ResponseBody
  public Map<String, DataIntegritySummary> getSummaries(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @RequestParam(required = false, defaultValue = "0") long timeout) {
    return dataIntegrityService.getSummaries(getCheckNames(checks), timeout);
  }

  /**
   * Handles the GET request to retrieve data integrity check metrics in Prometheus format. All
   * checks present in the cache are returned.
   *
   * @return A string containing the metrics in Prometheus format.
   */
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  @GetMapping(value = "/metrics", produces = TEXT_PLAIN_VALUE)
  @ResponseBody
  public String getSummariesMetrics() {
    // Get everything which is in the cache
    Map<String, DataIntegritySummary> summaries = dataIntegrityService.getSummaries(Set.of(), 0);
    PrometheusTextBuilder metrics = new PrometheusTextBuilder();

    Map<String, Integer> countMetrics = new HashMap<>();
    Map<String, Double> percentMetrics = new HashMap<>();
    Map<String, Long> durationMetrics = new HashMap<>();
    Map<String, String> metricLabels = new HashMap<>();

    Collection<DataIntegrityCheck> checks = dataIntegrityService.getDataIntegrityChecks(Set.of());

    summaries.forEach(
        (key, summary) -> {
          DataIntegrityCheck check =
              checks.stream().filter(c -> c.getName().equals(key)).findFirst().orElse(null);
          if (check != null) {
            String severity = check.getSeverity().name();
            String objectType = check.getIssuesIdType();
            StringBuilder metricLabel = new StringBuilder();
            metricLabel.append(
                "check=\"%s\",severity=\"%s\",object_type=\"%s\""
                    .formatted(key, severity, objectType));
            countMetrics.put(key, summary.getCount());
            metricLabels.put(key, metricLabel.toString());
            // Percentage might not be present
            if (summary.getPercentage() != null) {
              percentMetrics.put(key, summary.getPercentage());
            }
            long duration = summary.getFinishedTime().getTime() - summary.getStartTime().getTime();
            durationMetrics.put(key, duration);
          }
        });
    metrics.updateMetricsWithLabelsMap(
        countMetrics,
        "dhis_data_integrity_issues_count_total",
        metricLabels,
        "Data integrity check counts",
        "gauge");

    metrics.updateMetricsWithLabelsMap(
        percentMetrics,
        "dhis_data_integrity_issues_percentage",
        metricLabels,
        "Data integrity check percentages",
        "gauge");

    metrics.updateMetricsWithLabelsMap(
        durationMetrics,
        "dhis_data_integrity_check_duration_milliseconds",
        metricLabels,
        "Data integrity check durations",
        "gauge");

    return metrics.getMetrics();
  }

  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  @PostMapping("/summary")
  @ResponseBody
  public WebMessage runSummariesCheck(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @CheckForNull @RequestBody(required = false) Set<String> checksBody,
      @CurrentUser UserDetails currentUser)
      throws ConflictException {
    Set<String> names = getCheckNames(checksBody, checks);
    return runDataIntegrityAsync(names, currentUser, DataIntegrityReportType.SUMMARY)
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

  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  @GetMapping("/details")
  @ResponseBody
  public Map<String, DataIntegrityDetails> getDetails(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @RequestParam(required = false, defaultValue = "0") long timeout) {
    return dataIntegrityService.getDetails(getCheckNames(checks), timeout);
  }

  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  @PostMapping("/details")
  @ResponseBody
  public WebMessage runDetailsCheck(
      @CheckForNull @RequestParam(required = false) Set<String> checks,
      @RequestBody(required = false) Set<String> checksBody,
      @CurrentUser UserDetails currentUser)
      throws ConflictException {
    Set<String> names = getCheckNames(checksBody, checks);
    return runDataIntegrityAsync(names, currentUser, DataIntegrityReportType.DETAILS)
        .setLocation("/dataIntegrity/details?checks=" + toChecksList(names));
  }

  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  @GetMapping("/{check}/summary")
  @ResponseBody
  public DataIntegritySummary getSummary(
      @PathVariable String check,
      @RequestParam(required = false, defaultValue = "0") long timeout) {
    Collection<DataIntegritySummary> summaries =
        dataIntegrityService.getSummaries(Set.of(check), timeout).values();
    return summaries.isEmpty() ? null : summaries.iterator().next();
  }

  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
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
