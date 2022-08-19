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
package org.hisp.dhis.trackedentityattributevalue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service( "org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService" )
public class DefaultTrackedEntityAttributeValueAuditService
    implements TrackedEntityAttributeValueAuditService
{
    private final TrackedEntityAttributeValueAuditStore trackedEntityAttributeValueAuditStore;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final CurrentUserService currentUserService;

    public DefaultTrackedEntityAttributeValueAuditService(
        TrackedEntityAttributeValueAuditStore trackedEntityAttributeValueAuditStore,
        TrackedEntityAttributeService trackedEntityAttributeService,
        CurrentUserService currentUserService )
    {
        checkNotNull( trackedEntityAttributeValueAuditStore );
        checkNotNull( trackedEntityAttributeService );
        checkNotNull( currentUserService );

        this.trackedEntityAttributeValueAuditStore = trackedEntityAttributeValueAuditStore;
        this.trackedEntityAttributeService = trackedEntityAttributeService;
        this.currentUserService = currentUserService;
    }

    @Override
    public void addTrackedEntityAttributeValueAudit( TrackedEntityAttributeValueAudit trackedEntityAttributeValueAudit )
    {
        trackedEntityAttributeValueAuditStore.addTrackedEntityAttributeValueAudit( trackedEntityAttributeValueAudit );
    }

    @Override
    public List<TrackedEntityAttributeValueAudit> getTrackedEntityAttributeValueAudits(
        TrackedEntityAttributeValueAuditQueryParams params )
    {
        return aclFilter( trackedEntityAttributeValueAuditStore
            .getTrackedEntityAttributeValueAudits( params ) );
    }

    private List<TrackedEntityAttributeValueAudit> aclFilter(
        List<TrackedEntityAttributeValueAudit> trackedEntityAttributeValueAudits )
    {
        // Fetch all the Tracked Entity Instance Attributes this user has access
        // to (only store UIDs). Not a very efficient solution, but at the
        // moment
        // we do not have ACL API to check TEI attributes.

        Set<String> allUserReadableTrackedEntityAttributes = trackedEntityAttributeService
            .getAllUserReadableTrackedEntityAttributes( currentUserService.getCurrentUser() ).stream()
            .map( BaseIdentifiableObject::getUid ).collect( Collectors.toSet() );

        return trackedEntityAttributeValueAudits.stream()
            .filter( audit -> allUserReadableTrackedEntityAttributes.contains( audit.getAttribute().getUid() ) )
            .collect( Collectors.toList() );
    }

    @Override
    public int countTrackedEntityAttributeValueAudits( TrackedEntityAttributeValueAuditQueryParams params )
    {
        return trackedEntityAttributeValueAuditStore.countTrackedEntityAttributeValueAudits( params );
    }

    @Override
    public void deleteTrackedEntityAttributeValueAudits( TrackedEntityInstance trackedEntityInstance )
    {
        trackedEntityAttributeValueAuditStore.deleteTrackedEntityAttributeValueAudits( trackedEntityInstance );
    }
}
