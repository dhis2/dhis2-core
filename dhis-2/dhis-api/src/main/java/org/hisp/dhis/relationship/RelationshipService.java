package org.hisp.dhis.relationship;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.List;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Abyot Asalefew
 * @version $Id$
 */
public interface RelationshipService
{
    String ID = RelationshipService.class.getName();

    /**
     * Adds an {@link Program}
     * 
     * @param program The to Program add.
     * 
     * @return A generated unique id of the added {@link Program}.
     */
    int addRelationship( Relationship relationship );

    /**
     * Returns a {@link Program}.
     * 
     * @param id the id of the Program to return.
     * 
     * @return the Program with the given id
     */
    void deleteRelationship( Relationship relationship );

    /**
     * Updates an {@link Program}.
     * 
     * @param program the Program to update.
     */
    void updateRelationship( Relationship relationship );

    /**
     * Returns a {@link Program}.
     * 
     * @param id the id of the Program to return.
     * 
     * @return the Program with the given id
     */
    Relationship getRelationship( int id );

    /**
     * Get the relationship between two entityInstances by retrieving a
     * {@link RelationshipType}
     * 
     * @param entityInstanceA {@link TrackedEntityInstance}
     * @param entityInstanceB {@link TrackedEntityInstance}
     * @param relationshipType {@link RelationshipType}
     * 
     * @return {@link RelationshipType}
     */
    Relationship getRelationship( TrackedEntityInstance entityInstanceA, TrackedEntityInstance entityInstanceB, RelationshipType relationshipType );
    
    /**
     * Retrieve relationships of a instance
     * 
     * @param instance TrackedEntityInstance
     * 
     * @return Relationship list
     */
    List<Relationship> getRelationshipsForTrackedEntityInstance( TrackedEntityInstance instance );

    /**
     * Retrieve all relationships by relationship type of a person, for example
     * a instance might have more than one sibling
     * 
     * @param entityInstanceA TrackedEntityInstance
     * @param relationshipType RelationshipType
     * 
     * @return Relationship list
     */
    List<Relationship> getRelationships( TrackedEntityInstance entityInstanceA, RelationshipType relationshipType );
}
