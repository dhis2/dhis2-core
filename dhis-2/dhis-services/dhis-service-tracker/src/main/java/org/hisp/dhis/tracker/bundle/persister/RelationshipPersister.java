package org.hisp.dhis.tracker.bundle.persister;

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

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleHook;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerObjectReport;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class RelationshipPersister
    extends AbstractTrackerPersister<Relationship, org.hisp.dhis.relationship.Relationship>
{
    private final TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship> relationshipConverter;

    public RelationshipPersister( List<TrackerBundleHook> bundleHooks, ReservedValueService reservedValueService,
        TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship> relationshipConverter )
    {
        super( bundleHooks, reservedValueService );
        this.relationshipConverter = relationshipConverter;
    }

    @Override
    public TrackerTypeReport persist( Session session, TrackerBundle bundle )
    {
        List<Relationship> relationships = bundle.getRelationships();
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.RELATIONSHIP );

        relationships.forEach( o -> bundleHooks.forEach( hook -> hook.preCreate( Relationship.class, o, bundle ) ) );

        for ( int idx = 0; idx < relationships.size(); idx++ )
        {
            org.hisp.dhis.relationship.Relationship relationship = relationshipConverter
                .from( bundle.getPreheat(), relationships.get( idx ) );
            Date now = new Date();
            relationship.setLastUpdated( now );
            relationship.setLastUpdatedBy( bundle.getUser() );

            TrackerObjectReport objectReport = new TrackerObjectReport( TrackerType.RELATIONSHIP, relationship.getUid(),
                idx );
            typeReport.addObjectReport( objectReport );

            if ( relationship.getId() == 0 )
            {
                typeReport.getStats().incCreated();
            }
            else
            {
                typeReport.getStats().incUpdated();
            }

            session.persist( relationship );

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }
        }

        session.flush();

        relationships.forEach( o -> bundleHooks.forEach( hook -> hook.postCreate( Relationship.class, o, bundle ) ) );

        return typeReport;
    }

    @Override
    protected void runPreCreateHooks( TrackerBundle bundle )
    {
        bundle.getRelationships()
            .forEach( o -> bundleHooks.forEach( hook -> hook.preCreate( Relationship.class, o, bundle ) ) );
    }

    @Override
    protected org.hisp.dhis.relationship.Relationship convert( TrackerBundle bundle, Relationship trackerDto )
    {
        return relationshipConverter.from( bundle.getPreheat(), trackerDto );
    }

    @Override
    protected void persistComments( org.hisp.dhis.relationship.Relationship entity )
    {
        // NOTHING TO DO
    }

    @Override
    protected void updateEntityValues( Session session, TrackerPreheat preheat, Relationship trackerDto,
        org.hisp.dhis.relationship.Relationship hibernateEntity )
    {
        // NOTHING TO DO
    }

    @Override
    protected void updatePreheat( TrackerPreheat preheat, org.hisp.dhis.relationship.Relationship convertedDto )
    {
        // NOTHING TO DO
    }

    @Override
    protected boolean isNew( TrackerPreheat preheat, String uid )
    {
        return preheat.getRelationship( TrackerIdScheme.UID, uid ) == null;
    }

    @Override
    protected TrackerSideEffectDataBundle handleSideEffects( TrackerBundle bundle,
        org.hisp.dhis.relationship.Relationship entity )
    {
        return TrackerSideEffectDataBundle.builder().build();
    }

    @Override
    protected TrackerType getType()
    {
        return TrackerType.RELATIONSHIP;
    }

    @Override
    protected void runPostCreateHooks( TrackerBundle bundle )
    {
        bundle.getRelationships()
            .forEach( o -> bundleHooks.forEach( hook -> hook.postCreate( Relationship.class, o, bundle ) ) );
    }
}
