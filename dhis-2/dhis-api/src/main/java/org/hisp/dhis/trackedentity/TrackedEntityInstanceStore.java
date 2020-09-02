package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.user.User;

/**
 * @author Abyot Asalefew Gizaw
 */
public interface TrackedEntityInstanceStore
    extends IdentifiableObjectStore<TrackedEntityInstance>
{
    String ID = TrackedEntityInstanceStore.class.getName();

    int countTrackedEntityInstances( TrackedEntityInstanceQueryParams params );

    List<TrackedEntityInstance> getTrackedEntityInstances( TrackedEntityInstanceQueryParams params );

    List<Long> getTrackedEntityInstanceIds( TrackedEntityInstanceQueryParams params );

    List<Map<String, String>> getTrackedEntityInstancesGrid( TrackedEntityInstanceQueryParams params );

    int getTrackedEntityInstanceCountForGrid( TrackedEntityInstanceQueryParams params );

    /**
     * Checks for the existence of a TEI by UID. Deleted TEIs are not taken into account.
     *
     * @param uid PSI UID to check for.
     * @return true/false depending on result.
     */
    boolean exists( String uid );

    /**
     * Checks for the existence of a TEI by UID. Takes into account also the deleted TEIs.
     *
     * @param uid PSI UID to check for.
     * @return true/false depending on result.
     */
    boolean existsIncludingDeleted( String uid );

    /**
     * Returns UIDs of existing TrackedEntityInstances (including deleted) from the provided UIDs
     *
     * @param uids TEI UIDs to check
     * @return Set containing UIDs of existing TEIs (including deleted)
     */
    List<String> getUidsIncludingDeleted( List<String> uids );

    /**
     * Set lastSynchronized timestamp to provided timestamp for provided TEIs
     *
     * @param trackedEntityInstanceUIDs UIDs of Tracked entity instances where the lastSynchronized flag should be updated
     * @param lastSynchronized          The date of last successful sync
     */
    void updateTrackedEntityInstancesSyncTimestamp( List<String> trackedEntityInstanceUIDs, Date lastSynchronized );

    List<TrackedEntityInstance> getTrackedEntityInstancesByUid( List<String> uids, User user );
}
