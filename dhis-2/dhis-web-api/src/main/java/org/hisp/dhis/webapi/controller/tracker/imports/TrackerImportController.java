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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hisp.dhis.webapi.controller.tracker.TrackerControllerSupport.RESOURCE_PATH;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.util.Deque;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.Notification;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.job.TrackerJobWebMessageResponse;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.controller.tracker.TrackerBundleParams;
import org.hisp.dhis.webapi.controller.tracker.TrackerImportReportRequest;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.strategy.tracker.imports.TrackerImportStrategyHandler;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RestController
@RequestMapping( value = RESOURCE_PATH )
@RequiredArgsConstructor
public class TrackerImportController
{
    static final String TRACKER_JOB_ADDED = "Tracker job added";

    private final TrackerImportStrategyHandler trackerImportStrategy;

    private final TrackerImportService trackerImportService;

    private final RenderService renderService;

    private final ContextService contextService;

    private final Notifier notifier;

    @PostMapping( value = "", consumes = APPLICATION_JSON_VALUE )
    public void asyncPostJsonTracker( HttpServletRequest request, HttpServletResponse response, User currentUser,
        @RequestBody TrackerBundleParams trackerBundleParams )
        throws IOException
    {
        String jobId = CodeGenerator.generateUid();

        TrackerImportReportRequest trackerImportReportRequest = TrackerImportReportRequest.builder()
            .trackerBundleParams( trackerBundleParams ).contextService( contextService ).userUid( currentUser.getUid() )
            .isAsync( true ).uid( jobId )
            .build();

        trackerImportStrategy
            .importReport( trackerImportReportRequest );

        String location = ContextUtils.getRootPath( request ) + "/tracker/jobs/" + jobId;
        response.setHeader( "Location", location );
        response.setContentType( APPLICATION_JSON_VALUE );

        renderService.toJson( response.getOutputStream(), new WebMessage()
            .setMessage( TRACKER_JOB_ADDED )
            .setResponse(
                TrackerJobWebMessageResponse.builder()
                    .id( jobId ).location( location )
                    .build() ) );
    }

    @PostMapping( value = "", consumes = APPLICATION_JSON_VALUE, params = { "async=false" } )
    public ResponseEntity<TrackerImportReport> syncPostJsonTracker(
        @RequestParam( defaultValue = "errors", required = false ) String reportMode, User currentUser,
        @RequestBody TrackerBundleParams trackerBundleParams )
    {
        TrackerImportReportRequest trackerImportReportRequest = TrackerImportReportRequest.builder()
            .trackerBundleParams( trackerBundleParams ).contextService( contextService ).userUid( currentUser.getUid() )
            .trackerBundleReportMode( TrackerBundleReportMode
                .getTrackerBundleReportMode( reportMode ) )
            .uid( CodeGenerator.generateUid() )
            .build();

        TrackerImportReport trackerImportReport = trackerImportStrategy
            .importReport( trackerImportReportRequest );

        ResponseEntity.BodyBuilder builder = trackerImportReport.getStatus() == TrackerStatus.ERROR
            ? ResponseEntity.status( HttpStatus.CONFLICT )
            : ResponseEntity.ok();

        return builder.body( trackerImportReport );
    }

    @GetMapping( value = "/jobs/{uid}", produces = APPLICATION_JSON_VALUE )
    public Deque<Notification> getJob( @PathVariable String uid, HttpServletResponse response )
        throws HttpStatusCodeException
    {
        setNoStore( response );
        return notifier.getNotificationsByJobId( JobType.TRACKER_IMPORT_JOB, uid );
    }

    @GetMapping( value = "/jobs/{uid}/report", produces = APPLICATION_JSON_VALUE )
    public TrackerImportReport getJobReport( @PathVariable String uid,
        @RequestParam( defaultValue = "errors", required = false ) String reportMode,
        HttpServletResponse response )
        throws HttpStatusCodeException,
        NotFoundException
    {
        TrackerBundleReportMode trackerBundleReportMode = TrackerBundleReportMode
            .getTrackerBundleReportMode( reportMode );

        setNoStore( response );

        return Optional.ofNullable( notifier
            .getJobSummaryByJobId( JobType.TRACKER_IMPORT_JOB, uid ) )
            .map( report -> trackerImportService.buildImportReport( (TrackerImportReport) report,
                trackerBundleReportMode ) )
            .orElseThrow( () -> NotFoundException.notFoundUid( uid ) );
    }
}
