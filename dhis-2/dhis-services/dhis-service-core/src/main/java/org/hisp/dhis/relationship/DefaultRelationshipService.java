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
package org.hisp.dhis.relationship;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@Service( "org.hisp.dhis.relationship.RelationshipService" )
public class DefaultRelationshipService
    implements RelationshipService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final RelationshipStore relationshipStore;

    public DefaultRelationshipService( RelationshipStore relationshipStore )
    {
        checkNotNull( relationshipStore );

        this.relationshipStore = relationshipStore;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteRelationship( Relationship relationship )
    {
        relationshipStore.delete( relationship );
    }

    @Override
    @Transactional( readOnly = true )
    public Relationship getRelationship( long id )
    {
        return relationshipStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean relationshipExists( String uid )
    {
        return relationshipStore.getByUid( uid ) != null;
    }

    @Override
    @Transactional
    public long addRelationship( Relationship relationship )
    {
        relationship.getFrom().setRelationship( relationship );
        relationship.getTo().setRelationship( relationship );
        relationshipStore.save( relationship );

        return relationship.getId();
    }

    @Override
    @Transactional
    public void updateRelationship( Relationship relationship )
    {
        // TODO: Do we need next 2 lines? relationship never changes during
        // update
        relationship.getFrom().setRelationship( relationship );
        relationship.getTo().setRelationship( relationship );
        relationshipStore.update( relationship );
    }

    @Override
    @Transactional( readOnly = true )
    public Relationship getRelationship( String uid )
    {
        return relationshipStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Relationship> getRelationshipsByTrackedEntityInstance( TrackedEntityInstance tei,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
        boolean skipAccessValidation )
    {
        return relationshipStore.getByTrackedEntityInstance( tei, pagingAndSortingCriteriaAdapter );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Relationship> getRelationshipsByProgramInstance( ProgramInstance pi,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
        boolean skipAccessValidation )
    {
        return relationshipStore.getByProgramInstance( pi, pagingAndSortingCriteriaAdapter );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Relationship> getRelationshipsByProgramStageInstance( ProgramStageInstance psi,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
        boolean skipAccessValidation )
    {
        return relationshipStore.getByProgramStageInstance( psi, pagingAndSortingCriteriaAdapter );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Relationship> getRelationshipsByRelationshipType( RelationshipType relationshipType )
    {
        return relationshipStore.getByRelationshipType( relationshipType );
    }

    @Override
    @Transactional( readOnly = true )
    public Optional<Relationship> getRelationshipByRelationship( Relationship relationship )
    {
        checkNotNull( relationship.getFrom() );
        checkNotNull( relationship.getTo() );
        checkNotNull( relationship.getRelationshipType() );

        return Optional.ofNullable( relationshipStore.getByRelationship( relationship ) );
    }
}
