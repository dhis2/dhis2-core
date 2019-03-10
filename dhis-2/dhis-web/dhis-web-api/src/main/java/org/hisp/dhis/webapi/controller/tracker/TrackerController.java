package org.hisp.dhis.webapi.controller.tracker;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = TrackerController.RESOURCE_PATH )
public class TrackerController
{
    public static final String RESOURCE_PATH = "/tracker";

    private final TrackerImportService trackerImportService;
    private final RenderService renderService;
    private final ContextService contextService;

    public TrackerController( TrackerImportService trackerImportService, RenderService renderService, ContextService contextService )
    {
        this.trackerImportService = trackerImportService;
        this.renderService = renderService;
        this.contextService = contextService;
    }

    @PostMapping( value = "", consumes = MediaType.APPLICATION_JSON_VALUE )
    public void postJsonTracker( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        TrackerImportParams params = trackerImportService.getParamsFromMap( contextService.getParameterValuesMap() );

        TrackerBundle trackerBundle = renderService.fromJson( request.getInputStream(), TrackerBundleParams.class ).toTrackerBundle();
        params.setTrackedEntities( trackerBundle.getTrackedEntities() );
        params.setEnrollments( trackerBundle.getEnrollments() );
        params.setEvents( trackerBundle.getEvents() );

        response.setContentType( MediaType.APPLICATION_JSON_UTF8_VALUE );

        TrackerImportReport importReport = trackerImportService.importTracker( params );
        renderService.toJson( response.getOutputStream(), importReport );
    }
}
