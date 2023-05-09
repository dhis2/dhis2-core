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
package org.hisp.dhis.relationship;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.relationship.RelationshipService" )
public class DefaultRelationshipService
    implements RelationshipService
{
    private final RelationshipStore relationshipStore;

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
    @Transactional( readOnly = true )
    public boolean relationshipExistsIncludingDeleted( String uid )
    {
        return relationshipStore.existsIncludingDeleted( uid );
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
    public Relationship getRelationshipIncludeDeleted( String uid )
    {
        return relationshipStore.getByUidsIncludeDeleted( List.of( uid ) )
            .stream()
            .findAny()
            .orElse( null );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Relationship> getRelationships( @Nonnull List<String> uids )
    {
        return relationshipStore.getByUid( uids );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Relationship> getRelationshipsByTrackedEntityInstance( TrackedEntity tei,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
        boolean skipAccessValidation )
    {
        return relationshipStore.getByTrackedEntityInstance( tei, pagingAndSortingCriteriaAdapter );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Relationship> getRelationshipsByEnrollment( Enrollment enrollment,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
        boolean skipAccessValidation )
    {
        return relationshipStore.getByEnrollment( enrollment, pagingAndSortingCriteriaAdapter );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Relationship> getRelationshipsByEvent( Event event,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
        boolean skipAccessValidation )
    {
        return relationshipStore.getByEvent( event, pagingAndSortingCriteriaAdapter );
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
