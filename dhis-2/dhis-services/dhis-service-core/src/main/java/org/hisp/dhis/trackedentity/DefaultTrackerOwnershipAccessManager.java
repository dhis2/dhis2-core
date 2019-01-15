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

    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    public void setTrackedEntityInstanceService( TrackedEntityInstanceService trackedEntityInstanceService )
    {
        this.trackedEntityInstanceService = trackedEntityInstanceService;
    }

    private CurrentUserService currentUserService;

    @Autowired
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

    @Autowired
    public void setTrackedEntityProgramOwnerService( TrackedEntityProgramOwnerService trackedEntityProgramOwnerService )
    {
        this.trackedEntityProgramOwnerService = trackedEntityProgramOwnerService;
    }

    private CacheProvider cacheProvider;

    @Autowired
    public void setCacheProvider( CacheProvider cacheProvider )
    {
        this.cacheProvider = cacheProvider;
    }

    private ProgramService programService;

    @Autowired
    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    private ProgramTempOwnershipAuditService programTempOwnershipAuditService;

    @Autowired
    public void setProgramTempOwnershipAuditService( ProgramTempOwnershipAuditService programTempOwnershipAuditService )
    {
        this.programTempOwnershipAuditService = programTempOwnershipAuditService;
    }

    private ProgramOwnershipHistoryService programOwnershipHistoryService;

    @Autowired
    public void setProgramOwnershipHistoryService( ProgramOwnershipHistoryService programOwnershipHistoryService )
    {
        this.programOwnershipHistoryService = programOwnershipHistoryService;
    }

    private OrganisationUnitService organisationUnitService;

    @Autowired
    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    public void setTemporaryTrackerOwnershipCache( Cache<Boolean> temporaryTrackerOwnershipCache )
    {
        this.temporaryTrackerOwnershipCache = temporaryTrackerOwnershipCache;
    }
   
    /**
     * Cache for storing temporary ownership grants.
     */
    private Cache<Boolean> temporaryTrackerOwnershipCache;

    @PostConstruct
    public void init()
    {
        temporaryTrackerOwnershipCache = cacheProvider.newCacheBuilder( Boolean.class )
            .forRegion( "tempTrackerOwnership" )
            .withDefaultValue( false )
            .expireAfterWrite( TEMPORARY_OWNERSHIP_VALIDITY_IN_HOURS, TimeUnit.HOURS )
            .withMaximumSize( 100000 )
            .build();
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public void transferOwnership( String teiUid, String programUid, String orgUnitUid, boolean skipAccessValidation, boolean createIfNotExists )
    {
        Program program = programService.getProgram( programUid );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitUid );
        transferOwnership( entityInstance, program, orgUnit, skipAccessValidation, createIfNotExists );
    }

    @Override
    public void transferOwnership( TrackedEntityInstance entityInstance, Program program, OrganisationUnit orgUnit, boolean skipAccessValidation,
        boolean createIfNotExists )
    {
        if ( entityInstance == null || program == null | orgUnit == null )
        {
            return;
        }
        if ( hasAccess( currentUserService.getCurrentUser(), entityInstance, program ) || skipAccessValidation )
        {
            TrackedEntityProgramOwner teProgramOwner = trackedEntityProgramOwnerService.getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
            if ( teProgramOwner != null )
            {
                if ( !teProgramOwner.getOrganisationUnit().equals( orgUnit ) )
                {
                    ProgramOwnershipHistory programOwnershipHistory = new ProgramOwnershipHistory( program, entityInstance, teProgramOwner.getLastUpdated(),
                        teProgramOwner.getCreatedBy() );
                    programOwnershipHistoryService.addProgramOwnershipHistory( programOwnershipHistory );
                    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner( entityInstance, program, orgUnit );
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
            throw new AccessDeniedException( "User does not have access to change ownership for the entity-program combination" );
        }
    }

    @Override
    public void assignOwnership( String teiUid, String programUid, String orgUnitUid, boolean skipAccessValidation, boolean overwriteIfExists )
    {
        Program program = programService.getProgram( programUid );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgUnitUid );
        assignOwnership( entityInstance, program, orgUnit, skipAccessValidation, overwriteIfExists );
    }

    @Override
    public void assignOwnership( TrackedEntityInstance entityInstance, Program program, OrganisationUnit organisationUnit, boolean skipAccessValidation,
        boolean overwriteIfExists )
    {
        if ( entityInstance == null || program == null || organisationUnit == null )
        {
            return;
        }
        if ( hasAccess( currentUserService.getCurrentUser(), entityInstance, program ) || skipAccessValidation )
        {
            TrackedEntityProgramOwner teProgramOwner = trackedEntityProgramOwnerService.getTrackedEntityProgramOwner( entityInstance.getId(), program.getId() );
            if ( teProgramOwner != null )
            {
                if ( overwriteIfExists && !teProgramOwner.getOrganisationUnit().equals( organisationUnit ) )
                {
                    ProgramOwnershipHistory programOwnershipHistory = new ProgramOwnershipHistory( program, entityInstance, teProgramOwner.getLastUpdated(),
                        teProgramOwner.getCreatedBy() );
                    programOwnershipHistoryService.addProgramOwnershipHistory( programOwnershipHistory );
                    trackedEntityProgramOwnerService.updateTrackedEntityProgramOwner( entityInstance, program, organisationUnit );
                }
            }
            else
            {
                trackedEntityProgramOwnerService.createTrackedEntityProgramOwner( entityInstance, program, organisationUnit );
            }
        }
        else
        {
            log.error( "Unauthorized attempt to assign ownership" );
            throw new AccessDeniedException( "User does not have access to assign ownership for the entity-program combination" );
        }
    }

    @Override
    public void grantTemporaryOwnership( String teiUid, String programUid, User user, String reason )
    {
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        Program program = programService.getProgram( programUid );
        grantTemporaryOwnership( entityInstance, program, user, reason );
    }

    @Override
    public void grantTemporaryOwnership( TrackedEntityInstance entityInstance, Program program, User user, String reason )
    {
        if ( canSkipOwnershipCheck( user, program ) || entityInstance == null )
        {
            return;
        }
        if ( user != null && program.isProtected() )
        {
            programTempOwnershipAuditService
                .addProgramTempOwnershipAudit( new ProgramTempOwnershipAudit( program, entityInstance, reason, user.getUsername() ) );
            temporaryTrackerOwnershipCache.put( tempAccessKey( entityInstance.getUid(), program.getUid(), user.getUsername() ), true );
        }
    }

    @Override
    public boolean hasAccess( User user, TrackedEntityInstance entityInstance, Program program )
    {
        if ( canSkipOwnershipCheck( user, program ) || entityInstance == null )
        {
            return true;
        }

        OrganisationUnit ou = getOwner( entityInstance, program );
        if ( program.isOpen() || program.isAudited() )
        {
            return isInHierarchy( ou, user.getTeiSearchOrganisationUnitsWithFallback() );
        }
        else
        {
            return isInHierarchy( ou, user.getOrganisationUnits() ) || hasTemporaryAccess( entityInstance, program, user );
        }
    }

    @Override
    public boolean hasAccess( User user, String teiUid, String programUid )
    {
        Program program = programService.getProgram( programUid );
        TrackedEntityInstance entityInstance = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );
        return hasAccess( user, entityInstance, program );
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
        return new StringBuilder().append( username ).append( COLON ).append( programUid ).append( COLON ).append( teiUid ).toString();
    }

    /**
     * Get the current owner of this tei-program combination. Fallbacks to the
     * registered OU if no owner explicitly exists for the program
     * 
     * @param entityInstance The tei
     * @param program The program
     * @return The owning Organisation unit.
     */
    private OrganisationUnit getOwner( TrackedEntityInstance entityInstance, Program program )
    {
        OrganisationUnit ou = null;
        TrackedEntityProgramOwner trackedEntityProgramOwner = trackedEntityProgramOwnerService.getTrackedEntityProgramOwner( entityInstance.getId(),
            program.getId() );
        if ( trackedEntityProgramOwner == null )
        {
            ou = entityInstance.getOrganisationUnit();
        }
        else
        {
            ou = trackedEntityProgramOwner.getOrganisationUnit();
        }
        return ou;
    }

    /**
     * Check if the user has temporary access for a specific tei-program
     * combination
     * 
     * @param entityInstance The tracked entity instance object
     * @param program The program object
     * @param user The user object against which the check has to be performed
     * @return true if the user has temporary access, false otherwise
     */
    private boolean hasTemporaryAccess( TrackedEntityInstance entityInstance, Program program, User user )
    {
        if ( canSkipOwnershipCheck( user, program ) || entityInstance == null )
        {
            return true;
        }
        return temporaryTrackerOwnershipCache.get( tempAccessKey( entityInstance.getUid(), program.getUid(), user.getUsername() ) ).orElse( false );
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
        return organisationUnit != null && organisationUnits != null && organisationUnit.isDescendant( organisationUnits );
    }

    /**
     * Ownership check can be skipped if the user is super user or if the
     * program is without registration.
     * 
     * @param user
     * @param program
     * @return true if ownership check can be skipped
     */
    private boolean canSkipOwnershipCheck( User user, Program program )
    {
        return user == null || user.isSuper() || program == null || program.isWithoutRegistration();
    }
}
