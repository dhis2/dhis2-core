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
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.createWebMessage;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.feedback.Status.ERROR;
import static org.hisp.dhis.scheduling.JobStatus.FAILED;
import static org.hisp.dhis.scheduling.JobType.ANALYTICS_TABLE;
import static org.hisp.dhis.scheduling.JobType.MONITORING;
import static org.hisp.dhis.scheduling.JobType.RESOURCE_TABLE;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;

import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.scheduling.JobConfigurationWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Tags( "analytics" )
@Controller
@RequestMapping( value = ResourceTableController.RESOURCE_PATH )
@ApiVersion( { DEFAULT, ALL } )
@AllArgsConstructor
public class ResourceTableController
{
    public static final String RESOURCE_PATH = "/resourceTables";

    private final SchedulingManager schedulingManager;

    private final CurrentUserService currentUserService;

    @RequestMapping( value = "/analytics", method = { PUT, POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseBody
    public WebMessage analytics(
        @RequestParam( required = false ) boolean skipResourceTables,
        @RequestParam( required = false ) boolean skipAggregate,
        @RequestParam( required = false ) boolean skipEvents,
        @RequestParam( required = false ) boolean skipEnrollment,
        @RequestParam( required = false ) boolean executeTei,
        @RequestParam( required = false ) boolean skipOrgUnitOwnership,
        @RequestParam( required = false ) Integer lastYears )
    {
        Set<AnalyticsTableType> skipTableTypes = new HashSet<>();
        Set<String> skipPrograms = new HashSet<>();

        if ( skipAggregate )
        {
            skipTableTypes.add( DATA_VALUE );
            skipTableTypes.add( COMPLETENESS );
            skipTableTypes.add( COMPLETENESS_TARGET );
        }

        if ( skipEvents )
        {
            skipTableTypes.add( EVENT );
        }

        if ( skipEnrollment )
        {
            skipTableTypes.add( ENROLLMENT );
        }

        if ( skipOrgUnitOwnership )
        {
            skipTableTypes.add( OWNERSHIP );
        }

        if ( !executeTei )
        {
            skipTableTypes.add( TRACKED_ENTITY_INSTANCE );
            skipTableTypes.add( TRACKED_ENTITY_INSTANCE_EVENTS );
            skipTableTypes.add( TRACKED_ENTITY_INSTANCE_ENROLLMENTS );
        }
        else
        {
            // Ensure these tables are never skipped, as they are needed for the TEI tables/queries.
            skipTableTypes.remove( EVENT );
            skipTableTypes.remove( ENROLLMENT );
        }

        AnalyticsJobParameters analyticsJobParameters = new AnalyticsJobParameters( lastYears, skipTableTypes,
            skipPrograms, skipResourceTables );

        JobConfiguration analyticsTableJob = new JobConfiguration( "inMemoryAnalyticsJob", ANALYTICS_TABLE, "",
            analyticsJobParameters, true, true );
        analyticsTableJob.setUserUid( currentUserService.getCurrentUser().getUid() );

        return execute( analyticsTableJob );
    }

    @RequestMapping( method = { PUT, POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseBody
    public WebMessage resourceTables()
    {
        JobConfiguration resourceTableJob = new JobConfiguration( "inMemoryResourceTableJob",
            RESOURCE_TABLE, currentUserService.getCurrentUser().getUid(), true );

        return execute( resourceTableJob );
    }

    @RequestMapping( value = "/monitoring", method = { PUT, POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseBody
    public WebMessage monitoring()
    {
        JobConfiguration monitoringJob = new JobConfiguration( "inMemoryMonitoringJob", MONITORING, "",
            new MonitoringJobParameters(), true, true );

        return execute( monitoringJob );
    }

    private WebMessage execute( JobConfiguration configuration )
    {
        boolean success = schedulingManager.executeNow( configuration );
        if ( !success )
        {
            configuration.setJobStatus( FAILED );
            return createWebMessage( "Job of type " + configuration.getJobType() + " is already running", ERROR,
                CONFLICT ).setResponse( new JobConfigurationWebMessageResponse( configuration ) );
        }
        return jobConfigurationReport( configuration );
    }
}
