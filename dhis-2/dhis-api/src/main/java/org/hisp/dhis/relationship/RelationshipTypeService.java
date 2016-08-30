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

import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * @author Abyot Asalefew
 * @version $Id$
 */
public interface RelationshipTypeService
{
    String ID = RelationshipTypeService.class.getName();

    /**
     * Adds an {@link RelationshipType}
     * 
     * @param relationshipType The to RelationshipType add.
     * 
     * @return A generated unique id of the added {@link RelationshipType}.
     */
    int addRelationshipType( RelationshipType relationshipType );

    /**
     * Deletes a {@link RelationshipType}.
     * 
     * @param relationshipType the RelationshipType to delete.
     */
    void deleteRelationshipType( RelationshipType relationshipType );

    /**
     * Updates an {@link RelationshipType}.
     * 
     * @param relationshipType the RelationshipType to update.
     */
    void updateRelationshipType( RelationshipType relationshipType );

    /**
     * Returns a {@link RelationshipType}.
     * 
     * @param id the id of the RelationshipType to return.
     * 
     * @return the RelationshipType with the given id
     */
    RelationshipType getRelationshipType( int id );

    /**
     * Returns a {@link RelationshipType}.
     * 
     * @param uid the uid of the RelationshipType to return.
     * 
     * @return the RelationshipType with the given id
     */
    RelationshipType getRelationshipType( String uid );

    /**
     * Retrieve a relationship
     * 
     * @param aIsToB The A side
     * @param bIsToA The B side
     * 
     * @return RelationshipType
     */
    RelationshipType getRelationshipType( String aIsToB, String bIsToA );

    /**
     * Returns all {@link RelationshipType}
     * 
     * @return a collection of all RelationshipType, or an empty collection if
     *         there are no RelationshipTypes.
     */
    List<RelationshipType> getAllRelationshipTypes();

    /**
     * Returns The number of RelationshipTypes with the key searched
     * 
     * @param name Keyword for searching by name
     * 
     * @return A number
     * 
     */
    Integer getRelationshipTypeCountByName( String name );

    /**
     * Returns {@link TrackedEntityAttribute} list with paging
     * 
     * @param name Keyword for searching by name
     * @param min
     * @param max
     * @return a collection of all TrackedEntityAttribute, or an empty
     *         collection if there are no TrackedEntityAttributes.
     */
    List<RelationshipType> getRelationshipTypesBetweenByName( String name,
        int min, int max );

    /**
     * Returns The number of all TrackedEntityAttribute available
     * 
     */
    Integer getRelationshipTypeCount();

    /**
     * Returns {@link TrackedEntityAttribute} list with paging
     * 
     * @param min
     * @param max
     * @return a List of all TrackedEntityAttribute, or an empty
     *         List if there are no TrackedEntityAttributes.
     */
    List<RelationshipType> getRelationshipTypesBetween( int min, int max );
}
