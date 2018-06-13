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
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * @author Ameen Mohamed
 */
@Transactional
public class DefaultTrackedEntityProgramOwnerService implements TrackedEntityProgramOwnerService
{
    private static final Log log = LogFactory.getLog( DefaultTrackedEntityProgramOwnerService.class );
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private OrganisationUnitService orgUnitService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore;

    @Override
    public void createTrackedEntityProgramOwner( String teiUid, String programUid, String orgUnitUid )
    {
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        if ( entityInstance == null )
        {
            return;
        }
        Program program = programService.getProgram( programUid );
        if ( program == null )
        {
            return;
        }
        OrganisationUnit ou = orgUnitService.getOrganisationUnit( orgUnitUid );
        if ( ou == null )
        {
            return;
        }
        trackedEntityProgramOwnerStore.save( createTrackedEntityProgramOwner( entityInstance, program, ou ) );
    }

    private TrackedEntityProgramOwner createTrackedEntityProgramOwner( TrackedEntityInstance entityInstance,
        Program program, OrganisationUnit ou )
    {
        TrackedEntityProgramOwner teiProgramOwner = new TrackedEntityProgramOwner( entityInstance, program, ou );
        teiProgramOwner.updateDates();
        User user = currentUserService.getCurrentUser();
        if ( user != null )
        {
            teiProgramOwner.setCreatedBy( user.getUsername() );
        }
        return teiProgramOwner;
    }

    @Override
    public void createOrUpdateTrackedEntityProgramOwner( String teiUid, String programUid, String orgUnitUid )
    {
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        Program program = programService.getProgram( programUid );
        if ( entityInstance == null )
        {
            return;
        }
        TrackedEntityProgramOwner teiProgramOwner = trackedEntityProgramOwnerStore
            .getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
        OrganisationUnit ou = orgUnitService.getOrganisationUnit( orgUnitUid );
        if ( ou == null )
        {
            return;
        }

        if ( teiProgramOwner == null )
        {
            trackedEntityProgramOwnerStore.save( createTrackedEntityProgramOwner( entityInstance, program, ou ) );
        }
        else
        {
            teiProgramOwner = updateTrackedEntityProgramOwner( teiProgramOwner, ou );
            trackedEntityProgramOwnerStore.update( teiProgramOwner );
        }
    }

    @Override
    public void createOrUpdateTrackedEntityProgramOwner( int teiUid, int programUid, int orgUnitUid )
    {
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        Program program = programService.getProgram( programUid );
        if ( entityInstance == null )
        {
            return;
        }
        TrackedEntityProgramOwner teiProgramOwner = trackedEntityProgramOwnerStore
            .getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
        OrganisationUnit ou = orgUnitService.getOrganisationUnit( orgUnitUid );
        if ( ou == null )
        {
            return;
        }

        if ( teiProgramOwner == null )
        {
            trackedEntityProgramOwnerStore.save( createTrackedEntityProgramOwner( entityInstance, program, ou ) );
        }
        else
        {
            teiProgramOwner = updateTrackedEntityProgramOwner( teiProgramOwner, ou );
            trackedEntityProgramOwnerStore.update( teiProgramOwner );
        }
    }

    @Override
    public void createOrUpdateTrackedEntityProgramOwner( TrackedEntityInstance entityInstance, Program program,
        OrganisationUnit ou )
    {
        if ( entityInstance == null || program == null || ou == null )
        {
            return;
        }
        TrackedEntityProgramOwner teiProgramOwner = trackedEntityProgramOwnerStore
            .getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
        if ( teiProgramOwner == null )
        {
            trackedEntityProgramOwnerStore.save( createTrackedEntityProgramOwner( entityInstance, program, ou ) );
        }
        else
        {
            teiProgramOwner = updateTrackedEntityProgramOwner( teiProgramOwner, ou );
            trackedEntityProgramOwnerStore.update( teiProgramOwner );
        }
    }

    @Override
    public void updateTrackedEntityProgramOwner( TrackedEntityInstance entityInstance, Program program,
        OrganisationUnit ou )
    {
        if ( entityInstance == null || program == null || ou == null )
        {
            return;
        }
        TrackedEntityProgramOwner teiProgramOwner = trackedEntityProgramOwnerStore
            .getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
        if ( teiProgramOwner == null )
        {
            return;
        }
        teiProgramOwner = updateTrackedEntityProgramOwner( teiProgramOwner, ou );
        trackedEntityProgramOwnerStore.update( teiProgramOwner );
    }

    private TrackedEntityProgramOwner updateTrackedEntityProgramOwner( TrackedEntityProgramOwner teiProgramOwner,
        OrganisationUnit ou )
    {
        teiProgramOwner.setOrganisationUnit( ou );
        teiProgramOwner.updateDates();
        User user = currentUserService.getCurrentUser();
        if ( user != null )
        {
            teiProgramOwner.setCreatedBy( user.getUsername() );
        }
        return teiProgramOwner;
    }

    @Override
    public void updateTrackedEntityProgramOwner( String teiUid, String programUid, String orgUnitUid )
    {
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        if ( entityInstance == null )
        {
            return;
        }
        Program program = programService.getProgram( programUid );
        if ( program == null )
        {
            return;
        }

        TrackedEntityProgramOwner teProgramOwner = trackedEntityProgramOwnerStore
            .getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
        if ( teProgramOwner == null )
        {
            return;
        }
        OrganisationUnit ou = orgUnitService.getOrganisationUnit( orgUnitUid );
        if ( ou == null )
        {
            return;
        }
        teProgramOwner = updateTrackedEntityProgramOwner( teProgramOwner, ou );
        trackedEntityProgramOwnerStore.update( teProgramOwner );
    }

    @Override
    public void createTrackedEntityProgramOwner( int teiId, int programId, int orgUnitId )
    {
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiId );
        if ( entityInstance == null )
        {
            return;
        }
        Program program = programService.getProgram( programId );
        if ( program == null )
        {
            return;
        }
        OrganisationUnit ou = orgUnitService.getOrganisationUnit( orgUnitId );
        if ( ou == null )
        {
            return;
        }
        trackedEntityProgramOwnerStore.save( createTrackedEntityProgramOwner( entityInstance, program, ou ) );
    }

    @Override
    public void updateTrackedEntityProgramOwner( int teiId, int programId, int orgUnitId )
    {
        TrackedEntityProgramOwner teProgramOwner = trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner( teiId,
            programId );
        if ( teProgramOwner == null )
        {
            return;
        }
        OrganisationUnit ou = orgUnitService.getOrganisationUnit( orgUnitId );
        if ( ou == null )
        {
            return;
        }
        trackedEntityProgramOwnerStore.update( updateTrackedEntityProgramOwner( teProgramOwner, ou ) );
    }

    @Override
    public TrackedEntityProgramOwner getTrackedEntityProgramOwner( int teiId, int programId )
    {
        return trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner( teiId, programId );
    }

    @Override
    public TrackedEntityProgramOwner getTrackedEntityProgramOwner( String teiUid, String programUid )
    {
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        Program program = programService.getProgram( programUid );
        if ( entityInstance == null || program == null )
        {
            return null;
        }
        return trackedEntityProgramOwnerStore.getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
    }

    @Override
    public List<TrackedEntityProgramOwner> getTrackedEntityProgramOwnersUsingId( List<Integer> teiIds )
    {
        return trackedEntityProgramOwnerStore.getTrackedEntityProgramOwners( teiIds );
    }

    @Override
    public List<TrackedEntityProgramOwner> getTrackedEntityProgramOwnersUsingId( List<Integer> teiIds, Program program )
    {
        return trackedEntityProgramOwnerStore.getTrackedEntityProgramOwners( teiIds, program.getId() );
    }

}
