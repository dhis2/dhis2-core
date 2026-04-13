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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.ALL;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingType;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.usagemetrics.SendUsageMetricsJob;
import org.hisp.dhis.usagemetrics.UsageMetricsConsent;
import org.hisp.dhis.usagemetrics.UsageMetricsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/usageMetrics")
@OpenApi.Document(classifiers = {"team:extensibility", "purpose:support"})
public class UsageMetricsController {

  private static final int EXPORT_INTERVAL_SECONDS = 604800; // weekly interval

  private final UsageMetricsService usageMetricsConsentService;
  private final JobConfigurationService jobConfigurationService;

  @PutMapping
  @RequiresAuthority(anyOf = ALL)
  @ResponseBody
  public WebMessage updateConsent(@RequestBody UsageMetricsConsent usageMetricsConsent)
      throws ConflictException {
    usageMetricsConsentService.saveConsent(usageMetricsConsent);
    JobConfiguration sendUsageMetricsJobConfig =
        jobConfigurationService.getJobConfigurationByUid(
            SendUsageMetricsJob.COLLECT_USAGE_METRICS_JOB_ID);
    if (usageMetricsConsent.isConsent() && sendUsageMetricsJobConfig == null) {
      sendUsageMetricsJobConfig = new JobConfiguration();
      sendUsageMetricsJobConfig.setName("Send Usage Metrics");
      sendUsageMetricsJobConfig.setDelay(EXPORT_INTERVAL_SECONDS);
      sendUsageMetricsJobConfig.setSchedulingType(SchedulingType.FIXED_DELAY);
      sendUsageMetricsJobConfig.setUid(SendUsageMetricsJob.COLLECT_USAGE_METRICS_JOB_ID);
      sendUsageMetricsJobConfig.setJobStatus(JobStatus.SCHEDULED);
      sendUsageMetricsJobConfig.setJobType(JobType.SEND_USAGE_METRICS);

      jobConfigurationService.create(sendUsageMetricsJobConfig);
    } else if (!usageMetricsConsent.isConsent() && sendUsageMetricsJobConfig != null) {
      jobConfigurationService.deleteJobConfiguration(sendUsageMetricsJobConfig);
    }

    return ok();
  }

  @GetMapping()
  @RequiresAuthority(anyOf = ALL)
  @ResponseBody
  public UsageMetricsConsent getConsent() {
    return usageMetricsConsentService.getConsent();
  }
}
