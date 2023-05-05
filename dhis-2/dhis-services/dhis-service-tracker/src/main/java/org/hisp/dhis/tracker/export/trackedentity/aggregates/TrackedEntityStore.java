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
package org.hisp.dhis.tracker.export.trackedentity.aggregates;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
public interface TrackedEntityStore
{
    /**
     * Get a Map of {@see TrackedEntity} by Primary Keys
     *
     * @param ids a list of Tracked Entity Instance Primary Keys
     * @return a Map where key is a {@see TrackedEntity} uid and the key is the
     *         corresponding {@see TrackedEntity}
     */
    Map<String, TrackedEntity> getTrackedEntityInstances( List<Long> ids, Context ctx );

    /**
     * Fetches all the relationships having the TEI id specified in the arg as
     * "left" or "right" relationship
     *
     * @param ids a list of Tracked Entity Instance Primary Keys
     * @return a MultiMap where key is a {@see TrackedEntity} uid and the key a
     *         List of {@see Relationship} objects
     */
    Multimap<String, RelationshipItem> getRelationships( List<Long> ids, Context ctx );

    /**
     *
     * @param ids @param ids a list of Tracked Entity Instance Primary Keys
     * @return a MultiMap where key is a {@see TrackedEntity} uid and the key a
     *         List of {@see Attribute} objects
     */
    Multimap<String, TrackedEntityAttributeValue> getAttributes( List<Long> ids );

    /**
     *
     * @param ids a list of Tracked Entity Instance Primary Keys
     * @return a MultiMap where key is a {@see TrackedEntity} uid and the * key
     *         a List of {@see ProgramOwner} objects
     */
    Multimap<String, TrackedEntityProgramOwner> getProgramOwners( List<Long> ids );

    /**
     * For each tei, get the list of programs for which the user has ownership.
     *
     * @param ids a list of Tracked Entinty Instance primary keys
     * @param ctx aggregate context
     * @return Tei uids mapped to a list of program uids to which user has
     *         ownership
     */
    Multimap<String, String> getOwnedTeis( List<Long> ids, Context ctx );
}
