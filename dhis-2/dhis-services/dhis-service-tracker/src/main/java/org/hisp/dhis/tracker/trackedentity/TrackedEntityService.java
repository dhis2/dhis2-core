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
package org.hisp.dhis.tracker.trackedentity;

import java.util.List;

import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;

public interface TrackedEntityService
{
    /**
     * Fetches {@see TrackedEntityInstance}s based on the specified parameters.
     *
     * @param queryParams a {@see TrackedEntityInstanceQueryParams} instance
     *        with the query parameters
     * @param params a {@see TrackedEntityParams} instance containing the
     *        directives for how much data should be fetched (e.g. Enrollments,
     *        Events, Relationships)
     * @return {@see TrackedEntityInstance}s
     */
    List<TrackedEntityInstance> getTrackedEntities( TrackedEntityInstanceQueryParams queryParams,
        TrackedEntityParams params )
        throws ForbiddenException,
        NotFoundException;

    int getTrackedEntityCount( TrackedEntityInstanceQueryParams params, boolean skipAccessValidation,
        boolean skipSearchScopeValidation );

    TrackedEntityInstance getTrackedEntity( String uid, TrackedEntityParams params )
        throws NotFoundException,
        ForbiddenException;

    TrackedEntityInstance getTrackedEntity( TrackedEntityInstance trackedEntity, TrackedEntityParams params )
        throws NotFoundException,
        ForbiddenException;

    TrackedEntityInstance getTrackedEntity( String uid, String programIdentifier, TrackedEntityParams params )
        throws NotFoundException,
        ForbiddenException;
}
