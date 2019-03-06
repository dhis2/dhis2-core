package org.hisp.dhis.tracker.bundle;

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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.metadata.FlushMode;
import org.hisp.dhis.logging.LoggingManager;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Transactional
public class DefaultTrackerBundleService implements TrackerBundleService
{
    private static final LoggingManager.Logger log = LoggingManager.createLogger( DefaultTrackerBundleService.class );

    private final TrackerPreheatService trackerPreheatService;
    private final TrackerConverterService<TrackedEntityInstance, org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityTrackerConverterService;
    private final TrackerConverterService<Enrollment, ProgramInstance> enrollmentTrackerConverterService;
    private final TrackerConverterService<Event, ProgramStageInstance> eventTrackerConverterService;
    private final CurrentUserService currentUserService;
    private final IdentifiableObjectManager manager;
    private final SessionFactory sessionFactory;
    private final HibernateCacheManager cacheManager;
    private final DbmsManager dbmsManager;


    private List<TrackerBundleHook> bundleHooks = new ArrayList<>();

    @Autowired( required = false )
    public void setBundleHooks( List<TrackerBundleHook> bundleHooks )
    {
        this.bundleHooks = bundleHooks;
    }

    public DefaultTrackerBundleService(
        TrackerPreheatService trackerPreheatService,
        TrackerConverterService<TrackedEntityInstance, org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityTrackerConverterService,
        TrackerConverterService<Enrollment, ProgramInstance> enrollmentTrackerConverterService,
        TrackerConverterService<Event, ProgramStageInstance> eventTrackerConverterService,
        CurrentUserService currentUserService,
        IdentifiableObjectManager manager,
        SessionFactory sessionFactory,
        HibernateCacheManager cacheManager,
        DbmsManager dbmsManager )
    {
        this.trackerPreheatService = trackerPreheatService;
        this.trackedEntityTrackerConverterService = trackedEntityTrackerConverterService;
        this.enrollmentTrackerConverterService = enrollmentTrackerConverterService;
        this.eventTrackerConverterService = eventTrackerConverterService;
        this.currentUserService = currentUserService;
        this.manager = manager;
        this.sessionFactory = sessionFactory;
        this.cacheManager = cacheManager;
        this.dbmsManager = dbmsManager;
    }

    @Override
    public List<TrackerBundle> create( TrackerBundleParams params )
    {
        TrackerBundle trackerBundle = params.toTrackerBundle();
        TrackerPreheatParams preheatParams = params.toTrackerPreheatParams();
        preheatParams.setUser( getUser( preheatParams.getUser(), preheatParams.getUserId() ) );

        TrackerPreheat preheat = trackerPreheatService.preheat( preheatParams );
        trackerBundle.setPreheat( preheat );

        return Collections.singletonList( trackerBundle ); // for now we don't split the bundles
    }

    @Override
    public TrackerBundleReport commit( TrackerBundle bundle )
    {
        TrackerBundleReport bundleReport = new TrackerBundleReport();

        if ( TrackerBundleMode.VALIDATE == bundle.getImportMode() )
        {
            return bundleReport;
        }

        Session session = sessionFactory.getCurrentSession();

        bundleHooks.forEach( hook -> hook.preCommit( bundle ) );

        handleTrackedEntities( session, bundle );
        handleEnrollments( session, bundle );
        handleEvents( session, bundle );

        bundleHooks.forEach( hook -> hook.postCommit( bundle ) );

        dbmsManager.clearSession();
        cacheManager.clearCache();

        return bundleReport;
    }

    private void handleTrackedEntities( Session session, TrackerBundle bundle )
    {
        List<TrackedEntityInstance> trackedEntities = bundle.getTrackedEntities();

        trackedEntities.forEach( o -> bundleHooks.forEach( hook -> hook.preTrackedEntityCreate( o, bundle ) ) );
        session.flush();

        for ( int idx = 0; idx < trackedEntities.size(); idx++ )
        {
            TrackedEntityInstance trackedEntity = trackedEntities.get( idx );

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }
        }

        session.flush();
        trackedEntities.forEach( o -> bundleHooks.forEach( hook -> hook.postTrackedEntityCreate( o, bundle ) ) );
    }

    private void handleEnrollments( Session session, TrackerBundle bundle )
    {
        List<Enrollment> enrollments = bundle.getEnrollments();

        enrollments.forEach( o -> bundleHooks.forEach( hook -> hook.preEnrollmentCreate( o, bundle ) ) );
        session.flush();

        for ( int idx = 0; idx < enrollments.size(); idx++ )
        {
            Enrollment enrollment = enrollments.get( idx );

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }
        }

        session.flush();
        enrollments.forEach( o -> bundleHooks.forEach( hook -> hook.postEnrollmentCreate( o, bundle ) ) );
    }

    private void handleEvents( Session session, TrackerBundle bundle )
    {
        List<Event> events = bundle.getEvents();

        events.forEach( o -> bundleHooks.forEach( hook -> hook.preEventCreate( o, bundle ) ) );
        session.flush();

        for ( int idx = 0; idx < events.size(); idx++ )
        {
            Event event = events.get( idx );

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }
        }

        session.flush();
        events.forEach( o -> bundleHooks.forEach( hook -> hook.postEventCreate( o, bundle ) ) );
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    private User getUser( User user, String userUid )
    {
        if ( user != null ) // ıf user already set, reload the user to make sure its loaded in the current tx
        {
            return manager.get( User.class, user.getUid() );
        }

        if ( !StringUtils.isEmpty( userUid ) )
        {
            user = manager.get( User.class, userUid );
        }

        if ( user == null )
        {
            user = currentUserService.getCurrentUser();
        }

        return user;
    }
}
