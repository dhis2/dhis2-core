/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = ResourceTableController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class ResourceTableController
{
    public static final String RESOURCE_PATH = "/resourceTables";

    @Autowired
    private SchedulingManager schedulingManager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private WebMessageService webMessageService;

    @RequestMapping( value = "/analytics", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void analytics(
        @RequestParam( required = false ) boolean skipResourceTables,
        @RequestParam( required = false ) boolean skipAggregate,
        @RequestParam( required = false ) boolean skipEvents,
        @RequestParam( required = false ) boolean skipEnrollment,
        @RequestParam( required = false ) Integer lastYears,
        HttpServletResponse response, HttpServletRequest request )
    {
        Set<AnalyticsTableType> skipTableTypes = new HashSet<>();
        Set<String> skipPrograms = new HashSet<>();

        if ( skipAggregate )
        {
            skipTableTypes.add( AnalyticsTableType.DATA_VALUE );
            skipTableTypes.add( AnalyticsTableType.COMPLETENESS );
            skipTableTypes.add( AnalyticsTableType.COMPLETENESS_TARGET );
        }

        if ( skipEvents )
        {
            skipTableTypes.add( AnalyticsTableType.EVENT );
        }

        if ( skipEnrollment )
        {
            skipTableTypes.add( AnalyticsTableType.ENROLLMENT );
        }

        AnalyticsJobParameters analyticsJobParameters = new AnalyticsJobParameters( lastYears, skipTableTypes,
            skipPrograms,
            skipResourceTables );

        JobConfiguration analyticsTableJob = new JobConfiguration( "inMemoryAnalyticsJob", JobType.ANALYTICS_TABLE, "",
            analyticsJobParameters, true, true );
        analyticsTableJob.setUserUid( currentUserService.getCurrentUser().getUid() );

        schedulingManager.executeNow( analyticsTableJob );

        webMessageService.send( jobConfigurationReport( analyticsTableJob ), response, request );
    }

    @RequestMapping( method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void resourceTables( HttpServletResponse response, HttpServletRequest request )
    {
        JobConfiguration resourceTableJob = new JobConfiguration( "inMemoryResourceTableJob",
            JobType.RESOURCE_TABLE, currentUserService.getCurrentUser().getUid(), true );

        schedulingManager.executeNow( resourceTableJob );

        webMessageService.send( jobConfigurationReport( resourceTableJob ), response, request );
    }

    @RequestMapping( value = "/monitoring", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void monitoring( HttpServletResponse response, HttpServletRequest request )
    {
        JobConfiguration monitoringJob = new JobConfiguration( "inMemoryMonitoringJob", JobType.MONITORING, "",
            new MonitoringJobParameters(), true, true );

        schedulingManager.executeNow( monitoringJob );

        webMessageService.send( jobConfigurationReport( monitoringJob ), response, request );
    }
}
