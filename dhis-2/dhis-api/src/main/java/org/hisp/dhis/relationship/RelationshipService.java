package org.hisp.dhis.relationship;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.List;

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

    Relationship getRelationship( String uid );

    List<Relationship> getRelationshipsByTrackedEntityInstance( TrackedEntityInstance tei, boolean skipAccessValidation );

    List<Relationship> getRelationshipsByProgramInstance( ProgramInstance pi, boolean skipAccessValidation );

    List<Relationship> getRelationshipsByProgramStageInstance( ProgramStageInstance psi, boolean skipAccessValidation );
}
