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
package org.hisp.dhis.tracker.bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.tracker.ParamsConverter;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerProgramRuleService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.persister.CommitService;
import org.hisp.dhis.tracker.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.hisp.dhis.tracker.sideeffect.SideEffectHandlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableMap;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@RequiredArgsConstructor
public class DefaultTrackerBundleService
    implements TrackerBundleService
{
    private final TrackerPreheatService trackerPreheatService;

    private final SessionFactory sessionFactory;

    private final CommitService commitService;

    private final TrackerProgramRuleService trackerProgramRuleService;

    private final TrackerObjectDeletionService deletionService;

    private List<SideEffectHandlerService> sideEffectHandlers = new ArrayList<>();

    @Autowired( required = false )
    public void setSideEffectHandlers( List<SideEffectHandlerService> sideEffectHandlers )
    {
        this.sideEffectHandlers = sideEffectHandlers;
    }

    private ImmutableMap<TrackerType, Function<TrackerBundle, TrackerTypeReport>> DELETION_MAPPER;

    private ImmutableMap<TrackerType, BiFunction<Session, TrackerBundle, TrackerTypeReport>> COMMIT_MAPPER;

    @PostConstruct
    public void initMaps()
    {

        COMMIT_MAPPER = new ImmutableMap.Builder<TrackerType, BiFunction<Session, TrackerBundle, TrackerTypeReport>>()
            .put( TrackerType.ENROLLMENT,
                (( session, bundle ) -> commitService.getEnrollmentPersister().persist( session, bundle )) )
            .put( TrackerType.EVENT,
                (( session, bundle ) -> commitService.getEventPersister().persist( session, bundle )) )
            .put( TrackerType.TRACKED_ENTITY,
                (( session, bundle ) -> commitService.getTrackerPersister().persist( session, bundle )) )
            .put( TrackerType.RELATIONSHIP,
                (( session, bundle ) -> commitService.getRelationshipPersister().persist( session, bundle )) )
            .build();

        DELETION_MAPPER = new ImmutableMap.Builder<TrackerType, Function<TrackerBundle, TrackerTypeReport>>()
            .put( TrackerType.ENROLLMENT, deletionService::deleteEnrollments )
            .put( TrackerType.EVENT, deletionService::deleteEvents )
            .put( TrackerType.TRACKED_ENTITY, deletionService::deleteTrackedEntityInstances )
            .put( TrackerType.RELATIONSHIP, deletionService::deleteRelationShips )
            .build();
    }

    @Override
    public TrackerBundle create( TrackerImportParams params )
    {
        TrackerBundle trackerBundle = ParamsConverter.convert( params );
        TrackerPreheat preheat = trackerPreheatService.preheat( params );
        trackerBundle.setPreheat( preheat );

        return trackerBundle;
    }

    @Override
    public TrackerBundle runRuleEngine( TrackerBundle trackerBundle )
    {
        List<RuleEffects> ruleEffects = trackerProgramRuleService
            .calculateRuleEffects( trackerBundle );
        trackerBundle.setRuleEffects( ruleEffects );

        return trackerBundle;
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

        Stream.of( TrackerType.values() )
            .forEach( t -> bundleReport.getTypeReportMap().put( t, COMMIT_MAPPER.get( t )
                .apply( session, bundle ) ) );

        return bundleReport;
    }

    @Override
    public void handleTrackerSideEffects( List<TrackerSideEffectDataBundle> bundles )
    {
        sideEffectHandlers.forEach( handler -> handler.handleSideEffects( bundles ) );
    }

    @Override
    @Transactional
    public TrackerBundleReport delete( TrackerBundle bundle )
    {
        TrackerBundleReport bundleReport = new TrackerBundleReport();

        if ( TrackerBundleMode.VALIDATE == bundle.getImportMode() )
        {
            return bundleReport;
        }

        Stream.of( TrackerType.values() ).sorted( Collections.reverseOrder() )
            .forEach( t -> bundleReport.getTypeReportMap().put( t, DELETION_MAPPER.get( t )
                .apply( bundle ) ) );

        return bundleReport;
    }
}
