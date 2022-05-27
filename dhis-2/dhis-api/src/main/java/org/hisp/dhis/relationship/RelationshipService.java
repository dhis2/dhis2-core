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

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;

/**
 * @author Abyot Asalefew
 */
public interface RelationshipService
{
    String ID = RelationshipService.class.getName();

    boolean relationshipExists( String uid );

    /**
     * Adds an {@link Relationship}
     *
     * @param relationship the relationship.
     * @return id of the added relationship.
     */
    long addRelationship( Relationship relationship );

    /**
     * Returns a {@link Relationship}.
     *
     * @param relationship the relationship.
     */
    void deleteRelationship( Relationship relationship );

    /**
     * Updates a {@link Relationship}.
     *
     * @param relationship the relationship.
     */
    void updateRelationship( Relationship relationship );

    /**
     * Returns a {@link Relationship}.
     *
     * @param id the id of the relationship to return.
     * @return the relationship with the given identifier.
     */
    Relationship getRelationship( long id );

    /**
     * Checks if relationship for given UID exists (including deleted
     * relationships).
     *
     * @param uid Relationship UID to check for.
     * @return return true if relationship exists, false otherwise.
     */
    boolean relationshipExistsIncludingDeleted( String uid );

    /**
     * Fetches a {@link Relationship} based on a relationship identifying
     * attributes:
     *
     * - relationship type - from - to
     *
     * @param relationship A valid Relationship
     * @return an Optional Relationship
     */
    Optional<Relationship> getRelationshipByRelationship( Relationship relationship );

    Relationship getRelationship( String uid );

    Relationship getRelationshipIncludeDeleted( String uid );

    List<Relationship> getRelationships( List<String> uids );

    default List<Relationship> getRelationshipsByTrackedEntityInstance( TrackedEntityInstance tei,
        boolean skipAccessValidation )
    {
        return getRelationshipsByTrackedEntityInstance( tei, null, skipAccessValidation );
    }

    List<Relationship> getRelationshipsByTrackedEntityInstance( TrackedEntityInstance tei,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
        boolean skipAccessValidation );

    default List<Relationship> getRelationshipsByProgramInstance( ProgramInstance pi, boolean skipAccessValidation )
    {
        return getRelationshipsByProgramInstance( pi, null, skipAccessValidation );
    }

    List<Relationship> getRelationshipsByProgramInstance( ProgramInstance pi,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter, boolean skipAccessValidation );

    default List<Relationship> getRelationshipsByProgramStageInstance( ProgramStageInstance psi,
        boolean skipAccessValidation )
    {
        return getRelationshipsByProgramStageInstance( psi, null, skipAccessValidation );
    }

    List<Relationship> getRelationshipsByProgramStageInstance( ProgramStageInstance psi,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter, boolean skipAccessValidation );

    List<Relationship> getRelationshipsByRelationshipType( RelationshipType relationshipType );
}
