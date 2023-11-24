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

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.hisp.dhis.analytics.AnalyticsTableType.COMPLETENESS;
import static org.hisp.dhis.analytics.AnalyticsTableType.COMPLETENESS_TARGET;
import static org.hisp.dhis.analytics.AnalyticsTableType.DATA_VALUE;
import static org.hisp.dhis.analytics.AnalyticsTableType.ENROLLMENT;
import static org.hisp.dhis.analytics.AnalyticsTableType.EVENT;
import static org.hisp.dhis.analytics.AnalyticsTableType.OWNERSHIP;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_ENROLLMENTS;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_EVENTS;
import static org.hisp.dhis.common.DhisApiVersion.ALL;
import static org.hisp.dhis.common.DhisApiVersion.DEFAULT;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.ANALYTICS_TABLE;
import static org.hisp.dhis.scheduling.JobType.MONITORING;
import static org.hisp.dhis.scheduling.JobType.RESOURCE_TABLE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.JobSchedulerService;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland. This is the AnalyticsExportController
 */
@OpenApi.Tags("analytics")
@Controller
@RequestMapping(value = "/resourceTables")
@ApiVersion({DEFAULT, ALL})
@RequiredArgsConstructor
public class ResourceTableController {

  private final JobConfigurationService jobConfigurationService;
  private final JobSchedulerService jobSchedulerService;

  @RequestMapping(
      value = "/analytics",
      method = {PUT, POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseBody
  public WebMessage analytics(
      @RequestParam(defaultValue = "false") Boolean skipResourceTables,
      @RequestParam(defaultValue = "false") Boolean skipAggregate,
      @RequestParam(defaultValue = "false") Boolean skipEvents,
      @RequestParam(defaultValue = "false") Boolean skipEnrollment,
      // STILL EXPERIMENTAL: to export TEIs, FE needs to set this to "false" explicitly
      @RequestParam(defaultValue = "true") Boolean skipTrackedEntities,
      @RequestParam(defaultValue = "false") Boolean skipOrgUnitOwnership,
      @RequestParam(required = false) Integer lastYears)
      throws ConflictException, @OpenApi.Ignore NotFoundException {
    Set<AnalyticsTableType> skipTableTypes = new HashSet<>();
    Set<String> skipPrograms = new HashSet<>();

    if (isTrue(skipAggregate)) {
      skipTableTypes.add(DATA_VALUE);
      skipTableTypes.add(COMPLETENESS);
      skipTableTypes.add(COMPLETENESS_TARGET);
    }

    if (isTrue(skipEvents)) {
      skipTableTypes.add(EVENT);
    }

    if (isTrue(skipEnrollment)) {
      skipTableTypes.add(ENROLLMENT);
    }

    if (isTrue(skipOrgUnitOwnership)) {
      skipTableTypes.add(OWNERSHIP);
    }

    if (isTrue(skipTrackedEntities)) {
      skipTableTypes.add(TRACKED_ENTITY_INSTANCE);
      skipTableTypes.add(TRACKED_ENTITY_INSTANCE_EVENTS);
      skipTableTypes.add(TRACKED_ENTITY_INSTANCE_ENROLLMENTS);
    }

    JobConfiguration config = new JobConfiguration(ANALYTICS_TABLE);
    config.setExecutedBy(CurrentUserUtil.getCurrentUserDetails().getUid());
    config.setJobParameters(
        new AnalyticsJobParameters(lastYears, skipTableTypes, skipPrograms, skipResourceTables));

    return execute(config);
  }

  @RequestMapping(method = {PUT, POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseBody
  public WebMessage resourceTables(@CurrentUser User currentUser)
      throws ConflictException, @OpenApi.Ignore NotFoundException {
    JobConfiguration config = new JobConfiguration(RESOURCE_TABLE);
    config.setExecutedBy(currentUser.getUid());
    return execute(config);
  }

  @RequestMapping(
      value = "/monitoring",
      method = {PUT, POST})
  @PreAuthorize("hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')")
  @ResponseBody
  public WebMessage monitoring() throws ConflictException, @OpenApi.Ignore NotFoundException {
    JobConfiguration config = new JobConfiguration(MONITORING);
    config.setJobParameters(new MonitoringJobParameters());
    return execute(config);
  }

  private WebMessage execute(JobConfiguration configuration)
      throws ConflictException, NotFoundException {
    jobSchedulerService.executeNow(jobConfigurationService.create(configuration));

    return jobConfigurationReport(configuration);
  }
}
