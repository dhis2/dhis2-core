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
package org.hisp.dhis.trackedentity;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.audit.payloads.TrackedEntityInstanceAudit;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 *
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditService" )
public class DefaultTrackedEntityInstanceAuditService
    implements TrackedEntityInstanceAuditService
{
    private final TrackedEntityInstanceAuditStore trackedEntityInstanceAuditStore;

    private final TrackedEntityInstanceStore trackedEntityInstanceStore;

    private final TrackerAccessManager trackerAccessManager;

    private final CurrentUserService currentUserService;

    // -------------------------------------------------------------------------
    // TrackedEntityInstanceAuditService implementation
    // -------------------------------------------------------------------------

    @Override
    @Async
    @Transactional
    public void addTrackedEntityInstanceAudit( TrackedEntityInstanceAudit trackedEntityInstanceAudit )
    {
        trackedEntityInstanceAuditStore.addTrackedEntityInstanceAudit( trackedEntityInstanceAudit );
    }

    @Override
    @Async
    @Transactional
    public void addTrackedEntityInstanceAudit( List<TrackedEntityInstanceAudit> trackedEntityInstanceAudits )
    {
        trackedEntityInstanceAuditStore.addTrackedEntityInstanceAudit( trackedEntityInstanceAudits );
    }

    @Override
    @Transactional
    public void deleteTrackedEntityInstanceAudit( TrackedEntity trackedEntity )
    {
        trackedEntityInstanceAuditStore.deleteTrackedEntityInstanceAudit( trackedEntity );
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityInstanceAudit> getTrackedEntityInstanceAudits(
        TrackedEntityInstanceAuditQueryParams params )
    {
        return trackedEntityInstanceAuditStore.getTrackedEntityInstanceAudits( params ).stream()
            .filter( a -> trackerAccessManager.canRead( currentUserService.getCurrentUser(),
                trackedEntityInstanceStore.getByUid( a.getTrackedEntity() ) ).isEmpty() )
            .collect( Collectors.toList() );
    }

    @Override
    @Transactional( readOnly = true )
    public int getTrackedEntityInstanceAuditsCount( TrackedEntityInstanceAuditQueryParams params )
    {
        return trackedEntityInstanceAuditStore.getTrackedEntityInstanceAuditsCount( params );
    }
}