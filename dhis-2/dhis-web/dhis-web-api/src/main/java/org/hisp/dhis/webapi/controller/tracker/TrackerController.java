/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.webapi.controller.tracker;

import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.Notification;
import org.hisp.dhis.system.notification.Notifier;
<<<<<<< HEAD
import org.hisp.dhis.tracker.TrackerImportParams;
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.hisp.dhis.tracker.TrackerImportService;
<<<<<<< HEAD
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.job.TrackerJobWebMessageResponse;
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.hisp.dhis.tracker.job.TrackerMessageManager;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.service.ContextService;
<<<<<<< HEAD
import org.hisp.dhis.webapi.utils.ContextUtils;
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
<<<<<<< HEAD

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

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
    private final TrackerMessageManager trackerMessageManager;
    private final Notifier notifier;

    private final TrackerMessageManager trackerMessageManager;

    private final Notifier notifier;

    public TrackerController(
        TrackerImportService trackerImportService,
        RenderService renderService,
        ContextService contextService,
        TrackerMessageManager trackerMessageManager,
        Notifier notifier )
    {
        this.trackerImportService = trackerImportService;
        this.renderService = renderService;
        this.contextService = contextService;
        this.trackerMessageManager = trackerMessageManager;
        this.notifier = notifier;
    }

    @PostMapping( value = "", consumes = MediaType.APPLICATION_JSON_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKER_IMPORTER_EXPERIMENTAL')" )
<<<<<<< HEAD
    public void postJsonTracker( HttpServletRequest request, HttpServletResponse response, User currentUser ) throws IOException
=======
    public void postJsonTracker()
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
        /**
         * Returns NOT_IMPLEMENTED for 2.35, as the importer itself is not in a
         * complete state, and should not be used.
         */
        throw new HttpClientErrorException( HttpStatus.NOT_IMPLEMENTED );
    }

<<<<<<< HEAD
        TrackerBundle trackerBundle = renderService.fromJson( request.getInputStream(), TrackerBundleParams.class ).toTrackerBundle();
        params.setTrackedEntities( trackerBundle.getTrackedEntities() );
        params.setEnrollments( trackerBundle.getEnrollments() );
        params.setEvents( trackerBundle.getEvents() );
        params.setRelationships( trackerBundle.getRelationships() );
        params.setUser( currentUser );
=======
    @GetMapping( value = "/jobs/{uid}", produces = MediaType.APPLICATION_JSON_VALUE )
    public List<Notification> getJob( @PathVariable String uid, HttpServletResponse response )
        throws HttpStatusCodeException
    {
        List<Notification> notifications = notifier.getNotificationsByJobId( JobType.TRACKER_IMPORT_JOB, uid );
        setNoStore( response );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

<<<<<<< HEAD
        String jobId = trackerMessageManager.addJob( params );
=======
        return notifications;
    }
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

<<<<<<< HEAD
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

    @GetMapping( value = "/jobs/{uid}", produces = MediaType.APPLICATION_JSON_VALUE )
    public List<Notification> getJob( @PathVariable String uid, HttpServletResponse response ) throws HttpStatusCodeException
    {
        List<Notification> notifications = notifier.getNotificationsByJobId( JobType.TRACKER_IMPORT_JOB, uid );
        setNoStore( response );

        return notifications;
    }

    @GetMapping( value = "/jobs/{uid}/report", produces = MediaType.APPLICATION_JSON_VALUE )
    public TrackerImportReport getJobReport( @PathVariable String uid, HttpServletResponse response ) throws HttpStatusCodeException
=======
    @GetMapping( value = "/jobs/{uid}/report", produces = MediaType.APPLICATION_JSON_VALUE )
    public TrackerImportReport getJobReport( @PathVariable String uid, HttpServletResponse response )
        throws HttpStatusCodeException
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
        Object importReport = notifier.getJobSummaryByJobId( JobType.TRACKER_IMPORT_JOB, uid );
        setNoStore( response );

        if ( importReport != null )
        {
            return (TrackerImportReport) importReport;
        }

        throw new HttpClientErrorException( HttpStatus.NOT_FOUND );
    }
}
