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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_TRACKER;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.*;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Ameen Mohamed
 */
@Slf4j
@Service( "org.hisp.dhis.trackedentity.TrackerOwnershipManager" )
public class DefaultTrackerOwnershipManager implements TrackerOwnershipManager
{
    private static final String COLON = ":";

    private static final int TEMPORARY_OWNERSHIP_VALIDITY_IN_HOURS = 3;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CurrentUserService currentUserService;

    private final TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

    private final CacheProvider cacheProvider;

    private final ProgramTempOwnershipAuditService programTempOwnershipAuditService;

    private final ProgramOwnershipHistoryService programOwnershipHistoryService;

    private final OrganisationUnitService organisationUnitService;

    private final DhisConfigurationProvider config;

    private final Environment env;

    public DefaultTrackerOwnershipManager( CurrentUserService currentUserService,
        TrackedEntityProgramOwnerService trackedEntityProgramOwnerService, CacheProvider cacheProvider,
        ProgramTempOwnershipAuditService programTempOwnershipAuditService,
        ProgramOwnershipHistoryService programOwnershipHistoryService,
        OrganisationUnitService organisationUnitService, DhisConfigurationProvider config, Environment env )
    {
        checkNotNull( currentUserService );
        checkNotNull( trackedEntityProgramOwnerService );
        checkNotNull( cacheProvider );
        checkNotNull( programTempOwnershipAuditService );
        checkNotNull( programOwnershipHistoryService );
        checkNotNull( organisationUnitService );
        checkNotNull( config );
        checkNotNull( env );

        this.currentUserService = currentUserService;
        this.trackedEntityProgramOwnerService = trackedEntityProgramOwnerService;
        this.cacheProvider = cacheProvider;
        this.programTempOwnershipAuditService = programTempOwnershipAuditService;
        this.programOwnershipHistoryService = programOwnershipHistoryService;
        this.organisationUnitService = organisationUnitService;
        this.config = config;
        this.env = env;
    }

    /**
     * Used only by test harness. Remove after test refactor.
     *
     */
    @Deprecated
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    /**
     * Cache for storing temporary ownership grants.
     */
    private Cache<Boolean> temporaryTrackerOwnershipCache;

    /**
     * Cache for storing recent ownership checks
     */
    private Cache<OrganisationUnit> ownerCache;

    @PostConstruct
    public void init()
    {
        //TODO proper solution for unit tests, where the cache must not survive between tests

        temporaryTrackerOwnershipCache = cacheProvider.newCacheBuilder( Boolean.class )
            .forRegion( "tempTrackerOwnership" )
            .withDefaultValue( false )
            .expireAfterWrite( TEMPORARY_OWNERSHIP_VALIDITY_IN_HOURS, TimeUnit.HOURS )
            .withMaximumSize( 100000 )
            .build();

        ownerCache = cacheProvider.newCacheBuilder( OrganisationUnit.class )
            .forRegion( "OrganisationUnitOwner" )
            .expireAfterWrite( 5, TimeUnit.MINUTES )
            .withMaximumSize( SystemUtils.isTestRun( env.getActiveProfiles() ) ? 0 : 1000 )
            .build();
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void transferOwnership( TrackedEntityInstance entityInstance, Program program, OrganisationUnit orgUnit,
        boolean skipAccessValidation, boolean createIfNotExists )
    {
        if ( entityInstance == null || program == null || orgUnit == null )
        {
            return;
        }

        if ( hasAccess( currentUserService.getCurrentUser(), entityInstance, program ) || skipAccessValidation )
        {
            TrackedEntityProgramOwner teProgramOwner = trackedEntityProgramOwnerService.getTrackedEntityProgramOwner(
                entityInstance.getId(), program.getId() );

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
    @Transactional
    public void assignOwnership( TrackedEntityInstance entityInstance, Program program, OrganisationUnit organisationUnit,
        boolean skipAccessValidation, boolean overwriteIfExists )
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
    @Transactional
    public void grantTemporaryOwnership( TrackedEntityInstance entityInstance, Program program, User user, String reason )
    {
        if ( canSkipOwnershipCheck( user, program ) || entityInstance == null )
        {
            return;
        }

        if ( program.isProtected() )
        {
            if ( config.isEnabled( CHANGELOG_TRACKER ) )
            {
                programTempOwnershipAuditService.addProgramTempOwnershipAudit(
                    new ProgramTempOwnershipAudit( program, entityInstance, reason, user.getUsername() ) );
            }

            temporaryTrackerOwnershipCache.put( tempAccessKey( entityInstance.getUid(), program.getUid(), user.getUsername() ), true );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccess( User user, TrackedEntityInstance entityInstance, Program program )
    {
        if ( canSkipOwnershipCheck( user, program ) || entityInstance == null )
        {
            return true;
        }

        OrganisationUnit ou = getOwner( entityInstance, program );

        if ( program.isOpen() || program.isAudited() )
        {
            return organisationUnitService.isInUserSearchHierarchyCached( user, ou );
        }
        else
        {
            return organisationUnitService.isInUserHierarchyCached( user, ou ) || hasTemporaryAccess( entityInstance, program, user );
        }
    }

    // -------------------------------------------------------------------------
    // Private Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Generates a unique key for the tei-program-user combination to be put
     * into cache.
     *
     * @return A unique cache key
     */
    private String tempAccessKey( String teiUid, String programUid, String username )
    {
        return username + COLON + programUid + COLON + teiUid;
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
        return ownerCache.get( getOwnershipCacheKey( entityInstance, program ), s -> {
            OrganisationUnit ou;
            TrackedEntityProgramOwner trackedEntityProgramOwner = trackedEntityProgramOwnerService.getTrackedEntityProgramOwner(
                entityInstance.getId(), program.getId() );

            if ( trackedEntityProgramOwner == null )
            {
                ou = entityInstance.getOrganisationUnit();
            }
            else
            {
                ou = trackedEntityProgramOwner.getOrganisationUnit();
            }
            return ou;
        } ).get();
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
     * Ownership check can be skipped if the user is super user or if the
     * program is without registration.
     *
     * @return true if ownership check can be skipped
     */
    private boolean canSkipOwnershipCheck( User user, Program program )
    {
        return user == null || user.isSuper() || program == null || program.isWithoutRegistration();
    }

    /**
     * Returns key used to store and retrieve cached records for ownership
     * @param trackedEntityInstance
     * @param program
     * @return a String representing a record of ownership
     */
    private String getOwnershipCacheKey( TrackedEntityInstance trackedEntityInstance, Program program )
    {
        return trackedEntityInstance.getUid() + "_" + program.getUid();
    }
}
