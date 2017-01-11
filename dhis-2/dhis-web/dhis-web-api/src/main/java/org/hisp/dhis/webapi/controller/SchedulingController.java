package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.SchedulingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hisp.dhis.scheduling.SchedulingManager.*;
import static org.hisp.dhis.system.scheduling.Scheduler.*;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = "/scheduling" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class SchedulingController
{
    private static final String STRATEGY_ALL_DAILY = "allDaily";
    private static final String STRATEGY_ALL_15_MIN = "allEvery15Min";
    private static final String STRATEGY_LAST_3_YEARS_DAILY = "last3YearsDaily";
    private static final String STRATEGY_ENABLED = "enabled";

    @Autowired
    private SchedulingManager schedulingManager;

    @Autowired
    private RenderService renderService;

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SCHEDULING_ADMIN')" )
    @RequestMapping( method = { RequestMethod.POST, RequestMethod.PUT }, consumes = { ContextUtils.CONTENT_TYPE_JSON } )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void schedule( HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        SchedulingStrategy strategy = renderService.fromJson( request.getInputStream(), SchedulingStrategy.class );
        ListMap<String, String> cronKeyMap = new ListMap<>();

        // -------------------------------------------------------------
        // Resource tables
        // -------------------------------------------------------------

        if ( STRATEGY_ALL_DAILY.equals( strategy.getResourceTableStrategy() ) )
        {
            cronKeyMap.putValue( CRON_DAILY_0AM, TASK_RESOURCE_TABLE );
        }
        else if ( STRATEGY_ALL_15_MIN.equals( strategy.getResourceTableStrategy() ) )
        {
            cronKeyMap.putValue( CRON_EVERY_15MIN, TASK_RESOURCE_TABLE_15_MINS );
        }

        // -------------------------------------------------------------
        // Analytics
        // -------------------------------------------------------------

        if ( STRATEGY_ALL_DAILY.equals( strategy.getAnalyticsStrategy() ) )
        {
            cronKeyMap.putValue( CRON_DAILY_0AM, TASK_ANALYTICS_ALL );
        }
        else if ( STRATEGY_LAST_3_YEARS_DAILY.equals( strategy.getAnalyticsStrategy() ) )
        {
            cronKeyMap.putValue( CRON_DAILY_0AM, TASK_ANALYTICS_LAST_3_YEARS );
        }

        // -------------------------------------------------------------
        // Data mart
        // -------------------------------------------------------------

        if ( STRATEGY_ALL_DAILY.equals( strategy.getDataMartStrategy() ) )
        {
            cronKeyMap.putValue( CRON_DAILY_0AM, TASK_DATAMART_LAST_YEAR );
        }

        // -------------------------------------------------------------
        // Monitoring
        // -------------------------------------------------------------

        if ( STRATEGY_ALL_DAILY.equals( strategy.getMonitoringStrategy() ) )
        {
            cronKeyMap.putValue( CRON_DAILY_0AM, TASK_MONITORING_LAST_DAY );
        }

        // -------------------------------------------------------------
        // Data synch
        // -------------------------------------------------------------

        if ( STRATEGY_ENABLED.equals( strategy.getDataSynchStrategy() ) )
        {
            cronKeyMap.putValue( CRON_EVERY_MIN, TASK_DATA_SYNCH );
        }

        schedulingManager.scheduleTasks( cronKeyMap );
    }
}
