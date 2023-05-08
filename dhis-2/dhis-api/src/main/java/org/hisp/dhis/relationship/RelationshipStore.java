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

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;

/**
 * @author Abyot Asalefew
 */
public interface RelationshipStore
    extends IdentifiableObjectStore<Relationship>
{
    String ID = RelationshipStore.class.getName();

    default List<Relationship> getByTrackedEntityInstance( TrackedEntity tei )
    {
        return getByTrackedEntityInstance( tei, null );
    }

    List<Relationship> getByTrackedEntityInstance( TrackedEntity tei,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter );

    default List<Relationship> getByEnrollment( Enrollment enrollment )
    {
        return getByEnrollment( enrollment, null );
    }

    List<Relationship> getByEnrollment( Enrollment enrollment,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter );

    default List<Relationship> getByEvent( Event event )
    {
        return getByEvent( event, null );
    }

    List<Relationship> getByEvent( Event event,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter );

    List<Relationship> getByRelationshipType( RelationshipType relationshipType );

    /**
     * Fetches a {@link Relationship} based on a relationship identifying
     * attributes: - relationship type - from - to
     *
     * @param relationship A valid Relationship
     *
     * @return a {@link Relationship} or null if no Relationship is found
     *         matching the identifying criterias
     */
    Relationship getByRelationship( Relationship relationship );

    /**
     * Checks if relationship for given UID exists (including deleted
     * relationships).
     *
     * @param uid Relationship UID to check for.
     * @return return true if relationship exists, false otherwise.
     */
    boolean existsIncludingDeleted( String uid );

    List<String> getUidsByRelationshipKeys( List<String> relationshipKeyList );

    List<Relationship> getByUidsIncludeDeleted( List<String> uids );
}
