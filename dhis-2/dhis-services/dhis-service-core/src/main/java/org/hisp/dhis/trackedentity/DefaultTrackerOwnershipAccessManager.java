package org.hisp.dhis.trackedentity;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Set;

/**
 * @author Ameen Mohamed
 */
public class DefaultTrackerOwnershipAccessManager implements TrackerOwnershipAccessManager
{
    private static final Log log = LogFactory.getLog( DefaultTrackerOwnershipAccessManager.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

    @Override
    public boolean isOwner( User user, TrackedEntityInstance entityInstance, Program program )
    {
        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() )
        {
            return true;
        }
        OrganisationUnit ou = null;

        TrackedEntityProgramOwner trackedEntityProgramOwner = trackedEntityProgramOwnerService
            .getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
        if ( trackedEntityProgramOwner == null )
        {
            ou = entityInstance.getOrganisationUnit();
        }
        else
        {
            ou = trackedEntityProgramOwner.getOrganisationUnit();
        }

        if ( ou != null )
        { // ou should never be null, but needs to be checked for legacy reasons
            return isInHierarchy( ou, user.getOrganisationUnits() );
        }

        return false;
    }

    @Override
    public boolean isOwner( User user, int teiId, int programId )
    {
        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() )
        {
            return true;
        }
        OrganisationUnit ou = null;

        TrackedEntityProgramOwner trackedEntityProgramOwner = trackedEntityProgramOwnerService
            .getTrackedEntityProgramOwner( teiId, programId );
        if ( trackedEntityProgramOwner == null )
        {
            TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiId );
            if ( entityInstance == null )
            {
                return false;
            }
            ou = entityInstance.getOrganisationUnit();
        }
        else
        {
            ou = trackedEntityProgramOwner.getOrganisationUnit();
        }

        if ( ou != null )
        { // ou should never be null, but needs to be checked for legacy reasons
            return isInHierarchy( ou, user.getOrganisationUnits() );
        }

        return false;
    }

    @Override
    public boolean isOwner( User user, String teiUid, String programUid )
    {
        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() )
        {
            return true;
        }
        OrganisationUnit ou = null;

        TrackedEntityProgramOwner trackedEntityProgramOwner = trackedEntityProgramOwnerService
            .getTrackedEntityProgramOwner( teiUid, programUid );
        if ( trackedEntityProgramOwner == null )
        {
            TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
            if ( entityInstance == null )
            {
                return false;
            }
            ou = entityInstance.getOrganisationUnit();
        }
        else
        {
            ou = trackedEntityProgramOwner.getOrganisationUnit();
        }

        if ( ou != null )
        { // ou should never be null, but needs to be checked for legacy reasons
            return isInHierarchy( ou, user.getOrganisationUnits() );
        }

        return false;
    }

    private boolean isInHierarchy( OrganisationUnit organisationUnit, Set<OrganisationUnit> organisationUnits )
    {
        return organisationUnit != null && organisationUnits != null
            && organisationUnit.isDescendant( organisationUnits );
    }

    @Override
    public void changeOwnership( String teiUid, String programUid, String orgUnitUid, boolean skipAccessValidation )
    {
        if ( isOwner( currentUserService.getCurrentUser(), teiUid, programUid ) || skipAccessValidation )
        {
            trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner( teiUid, programUid, orgUnitUid );
        }
    }

    @Override
    public void changeOwnership( int teiId, int programId, int orgUnitId, boolean skipAccessValidation )
    {
        if ( isOwner( currentUserService.getCurrentUser(), teiId, programId ) || skipAccessValidation )
        {
            trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner( teiId, programId, orgUnitId );
        }
    }
}
