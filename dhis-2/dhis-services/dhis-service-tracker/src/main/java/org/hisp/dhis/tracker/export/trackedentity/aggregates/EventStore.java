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

import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
public interface EventStore
{
    /**
     * Key: enrollment uid -> Value: Event
     *
     * @param enrollmentsId a List of Program Instance Primary Keys
     * @param ctx the {@see Context}
     * @return A Map, where the key is a Program Instance Primary Key, and the
     *         value is a List of {@see Event}
     */
    Multimap<String, ProgramStageInstance> getEventsByEnrollmentIds( List<Long> enrollmentsId, Context ctx );

    /**
     *
     * Key: event uid -> Value: List<DataValue>
     *
     * @param programStageInstanceId a List of Program Stage Instance Primary
     *        Keys
     * @return A Map, where the key is a Program Stage Instance Primary Key, and
     *         the value is a List of {@see DataValue}
     */
    Map<String, List<EventDataValue>> getDataValues( List<Long> programStageInstanceId );

    /**
     * Fetches all the relationships having the Program Stage Instance id
     * specified in the arg as "left" or "right" relationship
     *
     * @param ids a list of {@see Enrollment} Primary Keys
     * @return a MultiMap where key is a {@see Enrollment} uid and the value a
     *         List of {@see Relationship} objects
     */
    Multimap<String, RelationshipItem> getRelationships( List<Long> ids, Context ctx );

    Multimap<String, TrackedEntityComment> getNotes( List<Long> eventIds );
}
