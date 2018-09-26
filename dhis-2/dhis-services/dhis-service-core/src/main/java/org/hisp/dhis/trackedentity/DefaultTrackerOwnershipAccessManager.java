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
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramOwnershipHistory;
import org.hisp.dhis.program.ProgramOwnershipHistoryService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTempOwnershipAudit;
import org.hisp.dhis.program.ProgramTempOwnershipAuditService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

/**
 * @author Ameen Mohamed
 */
public class DefaultTrackerOwnershipAccessManager implements TrackerOwnershipAccessManager
{
    private static final String COLON = ":";
    
    private static final int TEMPORARY_OWNERSHIP_VALIDITY_IN_HOURS = 3;

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

    @Autowired
    private CacheProvider cacheProvider;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramTempOwnershipAuditService programTempOwnershipAuditService;

    @Autowired
    private ProgramOwnershipHistoryService programOwnershipHistoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;
    
    /**
     * Cache for storing temporary ownership grants.
     */
    private Cache<Boolean> temporaryTrackerOwnershipCache;

    @PostConstruct
    public void init()
    {
        temporaryTrackerOwnershipCache = cacheProvider.newCacheBuilder( Boolean.class )
            .forRegion( "tempTrackerOwnership" ).withDefaultValue( false )
            .expireAfterWrite( TEMPORARY_OWNERSHIP_VALIDITY_IN_HOURS, TimeUnit.HOURS ).withMaximumSize( 100000 ).build();
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------
    @Override
    public boolean isOwner( User user, TrackedEntityInstance entityInstance, Program program )
    {
        if ( skipOwnershipValidation( user, program ) || entityInstance == null )
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
        Program program = programService.getProgram( programId );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiId );
        return isOwner( user, entityInstance, program );
    }

    @Override
    public boolean isOwner( User user, String teiUid, String programUid )
    {
        Program program = programService.getProgram( programUid );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        return isOwner( user, entityInstance, program );
    }

    @Override
    public void transferOwnership( String teiUid, String programUid, String orgUnitUid, boolean skipAccessValidation,
        boolean createIfNotExists )
    {
        Program program = programService.getProgram( programUid );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitUid );
        transferOwnership( entityInstance, program, orgUnit, skipAccessValidation, createIfNotExists );
    }

    @Override
    public void transferOwnership( int teiId, int programId, int orgUnitId, boolean skipAccessValidation,
        boolean createIfNotExists )
    {
        Program program = programService.getProgram( teiId );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( programId );
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitId );
        transferOwnership( entityInstance, program, orgUnit, skipAccessValidation, createIfNotExists );
    }

    @Override
    public void transferOwnership( TrackedEntityInstance entityInstance, Program program, OrganisationUnit orgUnit,
        boolean skipAccessValidation, boolean createIfNotExists )
    {
        if ( entityInstance == null || program == null | orgUnit == null )
        {
            return;
        }
        if ( hasAccess( currentUserService.getCurrentUser(), entityInstance, program ) || skipAccessValidation )
        {
            TrackedEntityProgramOwner teProgramOwner = trackedEntityProgramOwnerService
                .getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
            if ( teProgramOwner != null )
            {
                if ( !teProgramOwner.getOrganisationUnit().equals( orgUnit ) )
                {
                    ProgramOwnershipHistory programOwnershipHistory = new ProgramOwnershipHistory( program,
                        entityInstance, teProgramOwner.getLastUpdated(), teProgramOwner.getCreatedBy() );
                    programOwnershipHistoryService.addProgramOwnershipHistory( programOwnershipHistory );
                    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner( entityInstance, program,
                        orgUnit );
                }
            }
            else if ( createIfNotExists )
            {
                trackedEntityProgramOwnerService.createTrackedEntityProgramOwner( entityInstance, program, orgUnit );
            }
        }
        else
        {
            log.error( "Unauthorized attempt to change ownership" );
            throw new AccessDeniedException(
                "User does not have access to change ownership for the entity program combination" );
        }
    }

    @Override
    public void assignOwnership( String teiUid, String programUid, String orgUnitUid, boolean skipAccessValidation,
        boolean overwriteIfExists )
    {
        Program program = programService.getProgram( programUid );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitUid );
        assignOwnership( entityInstance, program, orgUnit, skipAccessValidation, overwriteIfExists );
    }

    @Override
    public void assignOwnership( TrackedEntityInstance entityInstance, Program program,
        OrganisationUnit organisationUnit, boolean skipAccessValidation, boolean overwriteIfExists )
    {
        if ( entityInstance == null || program == null || organisationUnit == null )
        {
            return;
        }
        if ( hasAccess( currentUserService.getCurrentUser(), entityInstance, program ) || skipAccessValidation )
        {
            TrackedEntityProgramOwner teProgramOwner = trackedEntityProgramOwnerService
                .getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
            if ( teProgramOwner != null )
            {
                if ( overwriteIfExists && !teProgramOwner.getOrganisationUnit().equals( organisationUnit ) )
                {
                    ProgramOwnershipHistory programOwnershipHistory = new ProgramOwnershipHistory( program,
                        entityInstance, teProgramOwner.getLastUpdated(), teProgramOwner.getCreatedBy() );
                    programOwnershipHistoryService.addProgramOwnershipHistory( programOwnershipHistory );
                    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner( entityInstance, program,
                        organisationUnit );
                }
            }
            else
            {
                trackedEntityProgramOwnerService.createTrackedEntityProgramOwner( entityInstance, program,
                    organisationUnit );
            }
        }
        else
        {
            log.error( "Unauthorized attempt to change ownership" );
            throw new AccessDeniedException(
                "User does not have access to change ownership for the entity program combination" );
        }
    }

    @Override
    public void assignOwnership( int teiId, int programId, int orgUnitId, boolean skipAccessValidation,
        boolean overwriteIfExists )
    {
        Program program = programService.getProgram( teiId );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( programId );
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitId );
        assignOwnership( entityInstance, program, orgUnit, skipAccessValidation, overwriteIfExists );
    }

    @Override
    public void grantTemporaryOwnership( int teiId, int programId, User user, String reason )
    {
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiId );
        Program program = programService.getProgram( programId );
        grantTemporaryOwnership( entityInstance, program, user, reason );
    }

    @Override
    public void grantTemporaryOwnership( String teiUid, String programUid, User user, String reason )
    {
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        Program program = programService.getProgram( programUid );
        grantTemporaryOwnership( entityInstance, program, user, reason );
    }

    @Override
    public void grantTemporaryOwnership( TrackedEntityInstance entityInstance, Program program, User user,
        String reason )
    {
        if ( skipOwnershipValidation( user, program ) )
        {
            return;
        }
        if ( user != null && program.getAccessLevel() != AccessLevel.CLOSED )
        {
            programTempOwnershipAuditService.addProgramTempOwnershipAudit(
                new ProgramTempOwnershipAudit( program, entityInstance, reason, user.getUsername() ) );
            temporaryTrackerOwnershipCache
                .put( tempAccessKey( entityInstance.getUid(), program.getUid(), user.getUsername() ), true );
        }
    }

    @Override
    public boolean hasTemporaryAccess( String teiUid, String programUid, User user )
    {
        Program program = programService.getProgram( programUid );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        return hasTemporaryAccess( entityInstance, program, user );
    }

    @Override
    public boolean hasTemporaryAccess( TrackedEntityInstance entityInstance, Program program, User user )
    {
        if ( skipOwnershipValidation( user, program ) || entityInstance == null )
        {
            return true;
        }
        return temporaryTrackerOwnershipCache
            .get( tempAccessKey( entityInstance.getUid(), program.getUid(), user.getUsername() ) ).orElse( false );
    }

    @Override
    public boolean hasTemporaryAccess( int teiId, int programId, User user )
    {
        Program program = programService.getProgram( programId );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiId );
        return hasTemporaryAccess( entityInstance, program, user );
    }

    @Override
    public boolean hasAccess( User user, TrackedEntityInstance entityInstance, Program program )
    {
        return isOwner( user, entityInstance, program ) || hasTemporaryAccess( entityInstance, program, user );
    }

    @Override
    public boolean hasAccess( User user, String teiUid, String programUid )
    {
        return isOwner( user, teiUid, programUid ) || hasTemporaryAccess( teiUid, programUid, user );
    }

    @Override
    public boolean hasAccess( User user, int teiId, int programId )
    {
        return isOwner( user, teiId, programId ) || hasTemporaryAccess( teiId, programId, user );
    }

    // -------------------------------------------------------------------------
    // Private Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Generates a unique key for the tei-program-user combination to be put
     * into cache.
     * 
     * @param teiUid
     * @param programUid
     * @param username
     * @return A unique cache key
     */
    private String tempAccessKey( String teiUid, String programUid, String username )
    {
        return new StringBuilder().append( username ).append( COLON ).append( programUid ).append( COLON )
            .append( teiUid ).toString();
    }

    /**
     * Check whether the specified organisation unit is part of any descendants
     * of the given set of org units.
     * 
     * @param organisationUnit The OU to be searched in the hierarchy.
     * @param organisationUnits The set of candidate ous which represents the
     *        hierarchy,
     * @return true if the ou is in the hierarchy, false otherwise.
     */
    private boolean isInHierarchy( OrganisationUnit organisationUnit, Set<OrganisationUnit> organisationUnits )
    {
        return organisationUnit != null && organisationUnits != null
            && organisationUnit.isDescendant( organisationUnits );
    }

    /**
     * Checks if the user is super user or if the program requires ownership
     * validation or not.
     * 
     * @param user
     * @param program
     * @return true if validation can be skipped because user is super user or
     *         program is not protected.
     */
    private boolean skipOwnershipValidation( User user, Program program )
    {
        // allow if user == null (internal process) or user is superuser or
        // program is null
        if ( user == null || user.isSuper() || program == null )
        {
            return true;
        }

        // allow if the program is OPEN or AUDITED or without registration
        return (program.isWithoutRegistration() || program.getAccessLevel() == AccessLevel.OPEN
            || program.getAccessLevel() == AccessLevel.AUDITED);
    }
}
