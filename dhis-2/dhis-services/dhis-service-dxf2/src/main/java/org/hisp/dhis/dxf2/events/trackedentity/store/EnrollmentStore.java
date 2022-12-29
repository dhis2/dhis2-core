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
package org.hisp.dhis.dxf2.events.trackedentity.store;

import java.util.List;

import org.hisp.dhis.dxf2.events.aggregates.AggregateContext;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;

import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
public interface EnrollmentStore
{
    /**
     *
     * @param ids a list of {@see TrackedEntityInstance} Primary Keys
     * @return a MultiMap where key is a {@see TrackedEntityInstance} uid and
     *         the key a List of {@see Enrollment} objects
     */
    Multimap<String, Enrollment> getEnrollmentsByTrackedEntityInstanceIds( List<Long> ids, AggregateContext ctx );

    /**
     *
     * @param ids a list of {@see Enrollment} Primary Keys
     * @return a MultiMap where key is a {@see Enrollment} uid and the key a
     *         List of {@see Note} objects
     */
    Multimap<String, Note> getNotes( List<Long> ids );

    /**
     * Fetches all the relationships having the Program Instance id specified in
     * the arg as "left" or "right" relationship
     *
     * @param ids a list of {@see Enrollment} Primary Keys
     * @return a MultiMap where key is a {@see Enrollment} uid and the key a
     *         List of {@see Relationship} objects
     */
    Multimap<String, Relationship> getRelationships( List<Long> ids, AggregateContext ctx );

    /**
     * Fetches all the attributes
     *
     * @param ids a list of enrollment ids
     * @return a MultiMap where key is a {@see Enrollment} uid and the key a
     *         List of {@see Attribute} objects
     */
    Multimap<String, Attribute> getAttributes( List<Long> ids, AggregateContext ctx );
}
