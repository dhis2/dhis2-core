/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.bundle.persister;

import org.hibernate.Session;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.converter.TrackerConverterService;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class RelationshipPersister
    extends AbstractTrackerPersister<Relationship, org.hisp.dhis.relationship.Relationship>
{
    private final TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship> relationshipConverter;

    public RelationshipPersister( ReservedValueService reservedValueService,
        TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship> relationshipConverter,
        TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService )

    {
        super( reservedValueService, trackedEntityAttributeValueAuditService );
        this.relationshipConverter = relationshipConverter;
    }

    @Override
    protected org.hisp.dhis.relationship.Relationship convert( TrackerBundle bundle, Relationship trackerDto )
    {
        return relationshipConverter.from( bundle.getPreheat(), trackerDto );
    }

    @Override
    protected void persistComments( TrackerPreheat preheat, org.hisp.dhis.relationship.Relationship entity )
    {
        // NOTHING TO DO
    }

    @Override
    protected void updateAttributes( Session session, TrackerPreheat preheat, Relationship trackerDto,
        org.hisp.dhis.relationship.Relationship hibernateEntity )
    {
        // NOTHING TO DO
    }

    @Override
    protected void updateDataValues( Session session, TrackerPreheat preheat, Relationship trackerDto,
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
    protected boolean isUpdatable()
    {
        // We don't want to update relationships. Only CREATE/DELETE is
        // supported
        // so this method will inform AbstractTrackerPersister to not proceed
        // with merge.
        return false;
    }

    @Override
    protected boolean isNew( TrackerPreheat preheat, Relationship trackerDto )
    {
        return preheat.getRelationship( trackerDto ) == null;
    }

    @Override
    protected boolean isNew( TrackerPreheat preheat, String uid )
    {
        // Normally this method is never invoked, since for Relationships
        // isNew( TrackerPreheat, Relationship ) is invoked instead
        throw new UnsupportedOperationException( "use isNew(TrackerPreheat preheat, Relationship trackerDto) instead" );
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
    protected void persistOwnership( TrackerPreheat preheat, org.hisp.dhis.relationship.Relationship entity )
    {
        // NOTHING TO DO

    }

    @Override
    protected String getUpdatedTrackedEntity( org.hisp.dhis.relationship.Relationship entity )
    {
        return null;
    }
}
