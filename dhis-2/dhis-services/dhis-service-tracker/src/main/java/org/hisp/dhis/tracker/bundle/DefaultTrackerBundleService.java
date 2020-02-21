package org.hisp.dhis.tracker.bundle;

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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.sideeffect.SideEffectHandlerService;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerObjectReport;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
public class DefaultTrackerBundleService implements TrackerBundleService
{
    private final TrackerPreheatService trackerPreheatService;
    private final TrackerConverterService<TrackedEntity, org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityTrackerConverterService;
    private final TrackerConverterService<Enrollment, ProgramInstance> enrollmentTrackerConverterService;
    private final TrackerConverterService<Event, ProgramStageInstance> eventTrackerConverterService;
    private final CurrentUserService currentUserService;
    private final IdentifiableObjectManager manager;
    private final SessionFactory sessionFactory;
    private final HibernateCacheManager cacheManager;
    private final DbmsManager dbmsManager;

    private List<TrackerBundleHook> bundleHooks = new ArrayList<>();
    private List<SideEffectHandlerService> sideEffectHandlers = new ArrayList<>();

    @Autowired( required = false )
    public void setBundleHooks( List<TrackerBundleHook> bundleHooks )
    {
        this.bundleHooks = bundleHooks;
    }

    @Autowired( required = false )
    public void setSideEffectHandlers( List<SideEffectHandlerService> sideEffectHandlers )
    {
        this.sideEffectHandlers = sideEffectHandlers;
    }

    public DefaultTrackerBundleService(
        TrackerPreheatService trackerPreheatService,
        TrackerConverterService<TrackedEntity, org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityTrackerConverterService,
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
    @Transactional( readOnly = true )
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
    @Transactional
    public TrackerBundleReport commit( TrackerBundle bundle )
    {
        TrackerBundleReport bundleReport = new TrackerBundleReport();

        if ( TrackerBundleMode.VALIDATE == bundle.getImportMode() )
        {
            return bundleReport;
        }

        Session session = sessionFactory.getCurrentSession();

        bundleHooks.forEach( hook -> hook.preCommit( bundle ) );

        TrackerTypeReport trackedEntityReport = handleTrackedEntities( session, bundle );
        TrackerTypeReport enrollmentReport = handleEnrollments( session, bundle );
        TrackerTypeReport eventReport = handleEvents( session, bundle );

        bundleReport.getTypeReportMap().put( TrackerType.TRACKED_ENTITY, trackedEntityReport );
        bundleReport.getTypeReportMap().put( TrackerType.ENROLLMENT, enrollmentReport );
        bundleReport.getTypeReportMap().put( TrackerType.EVENT, eventReport );

        bundleHooks.forEach( hook -> hook.postCommit( bundle ) );

        dbmsManager.clearSession();
        cacheManager.clearCache();

        return bundleReport;
    }

    private TrackerTypeReport handleTrackedEntities( Session session, TrackerBundle bundle )
    {
        List<TrackedEntity> trackedEntities = bundle.getTrackedEntities();
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.TRACKED_ENTITY );

        trackedEntities.forEach( o -> bundleHooks.forEach( hook -> hook.preCreate( TrackedEntity.class, o, bundle ) ) );
        session.flush();

        Date now = new Date();

        for ( int idx = 0; idx < trackedEntities.size(); idx++ )
        {
            TrackedEntity trackedEntity = trackedEntities.get( idx );
            org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance = trackedEntityTrackerConverterService.from(
                bundle.getPreheat(), trackedEntity );

            TrackerObjectReport objectReport = new TrackerObjectReport( TrackerType.TRACKED_ENTITY, trackedEntityInstance.getUid(), idx );
            typeReport.addObjectReport( objectReport );

            if ( bundle.getImportStrategy().isCreate() )
            {
                trackedEntityInstance.setCreated( now );
                trackedEntityInstance.setCreatedAtClient( now );
            }

            trackedEntityInstance.setLastUpdated( now );
            trackedEntityInstance.setLastUpdatedAtClient( now );
            trackedEntityInstance.setLastUpdatedBy( bundle.getUser() );

            session.persist( trackedEntityInstance );

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }
        }

        session.flush();
        trackedEntities.forEach( o -> bundleHooks.forEach( hook -> hook.postCreate( TrackedEntity.class, o, bundle ) ) );

        return typeReport;
    }

    private TrackerTypeReport handleEnrollments( Session session, TrackerBundle bundle )
    {
        List<Enrollment> enrollments = bundle.getEnrollments();
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.ENROLLMENT );

        enrollments.forEach( o -> bundleHooks.forEach( hook -> hook.preCreate( Enrollment.class, o, bundle ) ) );
        session.flush();

        Date now = new Date();

        for ( int idx = 0; idx < enrollments.size(); idx++ )
        {
            Enrollment enrollment = enrollments.get( idx );
            ProgramInstance programInstance = enrollmentTrackerConverterService.from( bundle.getPreheat(), enrollment );

            TrackerObjectReport objectReport = new TrackerObjectReport( TrackerType.ENROLLMENT, programInstance.getUid(), idx );
            typeReport.addObjectReport( objectReport );

            if ( bundle.getImportStrategy().isCreate() )
            {
                programInstance.setCreated( now );
                programInstance.setCreatedAtClient( now );
            }

            programInstance.setLastUpdated( now );
            programInstance.setLastUpdatedAtClient( now );
            programInstance.setLastUpdatedBy( bundle.getUser() );

            session.persist( programInstance );

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }

            sideEffectHandlers.forEach( handler -> handler.handleSideEffect( TrackerSideEffectDataBundle.builder()
                .klass( ProgramInstance.class )
                .enrollmentRuleEffects( new HashMap<>() ) // TODO this need to be fetched from TrackerBundle
                .eventRuleEffects( new HashMap<>() )      // TODO this need to be fetched from TrackerBundle
                .object( programInstance )
                .importStrategy( bundle.getImportStrategy() )
                .accessedBy( bundle.getUsername() )
                .build() ) );
        }

        session.flush();
        enrollments.forEach( o -> bundleHooks.forEach( hook -> hook.postCreate( Enrollment.class, o, bundle ) ) );

        return typeReport;
    }

    private TrackerTypeReport handleEvents( Session session, TrackerBundle bundle )
    {
        List<Event> events = bundle.getEvents();
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.EVENT );

        events.forEach( o -> bundleHooks.forEach( hook -> hook.preCreate( Event.class, o, bundle ) ) );
        session.flush();

        for ( int idx = 0; idx < events.size(); idx++ )
        {
            Event event = events.get( idx );
            ProgramStageInstance programStageInstance = eventTrackerConverterService.from( bundle.getPreheat(), event );

            TrackerObjectReport objectReport = new TrackerObjectReport( TrackerType.EVENT, programStageInstance.getUid(), idx );
            typeReport.addObjectReport( objectReport );

            Date now = new Date();

            if ( bundle.getImportStrategy().isCreate() )
            {
                programStageInstance.setCreated( now );
                programStageInstance.setCreatedAtClient( now );
            }

            programStageInstance.setLastUpdated( now );
            programStageInstance.setLastUpdatedAtClient( now );
            programStageInstance.setLastUpdatedBy( bundle.getUser() );

            session.persist( programStageInstance );
            typeReport.getStats().incCreated();

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }

            sideEffectHandlers.forEach( handler -> handler.handleSideEffect( TrackerSideEffectDataBundle.builder()
                .klass( ProgramInstance.class )
                .enrollmentRuleEffects( new HashMap<>() ) // TODO this need to be fetched from TrackerBundle
                .eventRuleEffects( new HashMap<>() )      // TODO this need to be fetched from TrackerBundle
                .object( programStageInstance )
                .importStrategy( bundle.getImportStrategy() )
                .accessedBy( bundle.getUsername() )
                .build() ) );
        }

        session.flush();
        events.forEach( o -> bundleHooks.forEach( hook -> hook.postCreate( Event.class, o, bundle ) ) );

        return typeReport;
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
