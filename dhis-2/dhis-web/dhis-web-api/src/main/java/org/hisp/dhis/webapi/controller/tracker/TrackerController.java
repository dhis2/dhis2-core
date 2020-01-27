package org.hisp.dhis.webapi.controller.tracker;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.job.TrackerJobStatus;
import org.hisp.dhis.tracker.job.TrackerJobWebMessageResponse;
import org.hisp.dhis.tracker.job.TrackerManager;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RestController
@RequestMapping( value = TrackerController.RESOURCE_PATH )
public class TrackerController
{
    public static final String RESOURCE_PATH = "/tracker";

    private final TrackerImportService trackerImportService;
    private final RenderService renderService;
    private final ContextService contextService;
    private final TrackerManager trackerManager;

    public TrackerController(
        TrackerImportService trackerImportService,
        RenderService renderService,
        ContextService contextService,
        TrackerManager trackerManager )
    {
        this.trackerImportService = trackerImportService;
        this.renderService = renderService;
        this.contextService = contextService;
        this.trackerManager = trackerManager;
    }

    @PostMapping( value = "", consumes = MediaType.APPLICATION_JSON_VALUE )
    public void postJsonTracker( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        TrackerImportParams params = trackerImportService.getParamsFromMap( contextService.getParameterValuesMap() );

        TrackerBundle trackerBundle = renderService.fromJson( request.getInputStream(), TrackerBundleParams.class ).toTrackerBundle();
        params.setTrackedEntities( trackerBundle.getTrackedEntities() );
        params.setEnrollments( trackerBundle.getEnrollments() );
        params.setEvents( trackerBundle.getEvents() );

        String jobId = trackerManager.addJob( params );

        String location = ContextUtils.getRootPath( request ) + "/tracker/jobs/" + jobId;
        response.setHeader( "Location", location );
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        renderService.toJson( response.getOutputStream(), new WebMessage()
            .setMessage( "Tracker job added" )
            .setResponse(
                TrackerJobWebMessageResponse.builder()
                    .id( jobId ).location( location )
                    .build()
            ) );
    }

    @GetMapping( "/jobs/{uid}" )
    public TrackerJobStatus getJob( @PathVariable String uid ) throws HttpStatusCodeException
    {
        throw new HttpClientErrorException( HttpStatus.NOT_FOUND );
    }

    @GetMapping( "/jobs/{uid}/status" )
    public TrackerJobStatus getJobStatus( @PathVariable String uid )
    {
        return TrackerJobStatus.PENDING;
    }
}
