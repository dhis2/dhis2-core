package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.analytics.table.scheduling.AnalyticsTableTask;
import org.hisp.dhis.resourcetable.scheduling.ResourceTableTask;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.validation.scheduling.MonitoringTask;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = ResourceTableController.RESOURCE_PATH )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class ResourceTableController
{
    public static final String RESOURCE_PATH = "/resourceTables";

    @Resource( name = "analyticsAllTask" )
    private AnalyticsTableTask analyticsTableTask;

    @Autowired
    private ResourceTableTask resourceTableTask;

    @Autowired
    private MonitoringTask monitoringTask;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private WebMessageService webMessageService;

    //TODO make tasks prototypes to avoid potential concurrency issues?

    @RequestMapping( value = "/analytics", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATA_MART_ADMIN')" )
    public void analytics(
        @RequestParam( required = false ) boolean skipResourceTables,
        @RequestParam( required = false ) boolean skipAggregate,
        @RequestParam( required = false ) boolean skipEvents,
        @RequestParam( required = false ) Integer lastYears,
        HttpServletResponse response, HttpServletRequest request )
    {
        analyticsTableTask.setSkipResourceTables( skipResourceTables );
        analyticsTableTask.setSkipAggregate( skipAggregate );
        analyticsTableTask.setSkipEvents( skipEvents );
        analyticsTableTask.setLastYears( lastYears );
        analyticsTableTask.setTaskId( new TaskId( TaskCategory.ANALYTICSTABLE_UPDATE, currentUserService.getCurrentUser() ) );

        scheduler.executeTask( analyticsTableTask );

        webMessageService.send( WebMessageUtils.ok( "Initiated analytics table update" ), response, request );
    }

    @RequestMapping( method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void resourceTables( HttpServletResponse response, HttpServletRequest request )
    {
        resourceTableTask.setTaskId( new TaskId( TaskCategory.RESOURCETABLE_UPDATE, currentUserService.getCurrentUser() ) );

        scheduler.executeTask( resourceTableTask );

        webMessageService.send( WebMessageUtils.ok( "Initiated resource table update" ), response, request );
    }

    @RequestMapping( value = "/monitoring", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void monitoring( HttpServletResponse response, HttpServletRequest request )
    {
        monitoringTask.setTaskId( new TaskId( TaskCategory.MONITORING, currentUserService.getCurrentUser() ) );

        scheduler.executeTask( monitoringTask );

        webMessageService.send( WebMessageUtils.ok( "Initiated data monitoring" ), response, request );
    }
}
