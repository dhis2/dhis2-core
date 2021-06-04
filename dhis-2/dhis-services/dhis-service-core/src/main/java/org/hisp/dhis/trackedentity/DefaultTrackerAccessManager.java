/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@Component( "org.hisp.dhis.dxf2.events.TrackerAccessManager" )
public class DefaultTrackerAccessManager implements TrackerAccessManager
{
    private final AclService aclService;

    private final TrackerOwnershipManager ownershipAccessManager;

    private final OrganisationUnitService organisationUnitService;

    private static final Cache<Boolean> canUserDataReadProgramCache = new SimpleCacheBuilder<Boolean>()
        .forRegion( "DTAM_canUserReadProgramCache" )
        .expireAfterAccess( 3, TimeUnit.HOURS )
        .withInitialCapacity( 10000 )
        .withMaximumSize( 50000 )
        .build();

    private static final Cache<Boolean> canUserDataWriteProgramCache = new SimpleCacheBuilder<Boolean>()
        .forRegion( "DTAM_canUserDataWriteProgramCache" )
        .expireAfterAccess( 3, TimeUnit.HOURS )
        .withInitialCapacity( 10000 )
        .withMaximumSize( 50000 )
        .build();

    private static final  Cache<Boolean> canUserDataReadProgramStageCache = new SimpleCacheBuilder<Boolean>()
        .forRegion( "DTAM_canUserReadProgramStageCache" )
        .expireAfterAccess( 3, TimeUnit.HOURS )
        .withInitialCapacity( 10000 )
        .withMaximumSize( 50000 )
        .build();

    private static final Cache<Boolean> canUserDataReadTrackedEntityTypeCache = new SimpleCacheBuilder<Boolean>()
        .forRegion( "DTAM_canUserReadTrackedEntityTypeCache" )
        .expireAfterAccess( 3, TimeUnit.HOURS )
        .withInitialCapacity( 10000 )
        .withMaximumSize( 50000 )
        .build();

    private static final Cache<Boolean> canUserReadDataElementCache = new SimpleCacheBuilder<Boolean>()
        .forRegion( "DTAM_canUserReadDataElementCache" )
        .expireAfterAccess( 3, TimeUnit.HOURS )
        .withInitialCapacity( 50000 )
        .withMaximumSize( 100000 )
        .build();

    private static final Cache<List<String>> canUserDataReadCOCCache = new SimpleCacheBuilder<List<String>>()
        .forRegion( "DTAM_canUserDataReadCOCCache" )
        .expireAfterAccess( 3, TimeUnit.HOURS )
        .withInitialCapacity( 50000 )
        .withMaximumSize( 100000 )
        .build();

    private static final Cache<List<String>> canUserDataWriteCOCCache = new SimpleCacheBuilder<List<String>>()
        .forRegion( "DTAM_canUserDataWriteCOCCache" )
        .expireAfterAccess( 3, TimeUnit.HOURS )
        .withInitialCapacity( 50000 )
        .withMaximumSize( 100000 )
        .build();

    public DefaultTrackerAccessManager( AclService aclService, TrackerOwnershipManager ownershipAccessManager,
        OrganisationUnitService organisationUnitService )
    {
        checkNotNull( aclService );
        checkNotNull( ownershipAccessManager );
        checkNotNull( organisationUnitService );

        this.aclService = aclService;
        this.ownershipAccessManager = ownershipAccessManager;
        this.organisationUnitService = organisationUnitService;
    }

    @Override
    public List<String> canRead( User user, TrackedEntityInstance trackedEntityInstance )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || trackedEntityInstance == null )
        {
            return errors;
        }

        OrganisationUnit ou = trackedEntityInstance.getOrganisationUnit();

        if ( ou != null )
        { // ou should never be null, but needs to be checked for legacy reasons
            if ( !organisationUnitService.isInUserSearchHierarchyCached( user, ou ) )
            {
                errors.add( "User has no read access to organisation unit: " + ou.getUid() );
            }
        }

        TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

        if ( !canDataReadTrackedEntityType( user, trackedEntityType ) )
        {
            errors.add( "User has no data read access to tracked entity: " + trackedEntityType.getUid() );
        }

        return errors;
    }

    @Override
    public List<String> canWrite( User user, TrackedEntityInstance trackedEntityInstance )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || trackedEntityInstance == null )
        {
            return errors;
        }

        OrganisationUnit ou = trackedEntityInstance.getOrganisationUnit();

        if ( ou != null )
        { // ou should never be null, but needs to be checked for legacy reasons
            if ( !organisationUnitService.isInUserSearchHierarchyCached( user, ou ) )
            {
                errors.add( "User has no write access to organisation unit: " + ou.getUid() );
            }
        }

        TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

        if ( !canDataReadTrackedEntityType( user, trackedEntityType ) )
        {
            errors.add( "User has no data write access to tracked entity: " + trackedEntityType.getUid() );
        }

        return errors;
    }

    @Override
    public List<String> canRead( User user, TrackedEntityInstance trackedEntityInstance, Program program,
        boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || trackedEntityInstance == null )
        {
            return errors;
        }

        if ( !canDataReadProgram( user, program ) )
        {
            errors.add( "User has no data read access to program: " + program.getUid() );
        }

        TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

        if ( !canDataReadTrackedEntityType( user, trackedEntityType ) )
        {
            errors.add( "User has no data read access to tracked entity: " + trackedEntityType.getUid() );
        }

        if ( !skipOwnershipCheck && !ownershipAccessManager.hasAccess( user, trackedEntityInstance, program ) )
        {
            errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
        }

        return errors;
    }

    @Override
    public List<String> canWrite( User user, TrackedEntityInstance trackedEntityInstance, Program program,
        boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || trackedEntityInstance == null )
        {
            return errors;
        }

        if ( !canDataWriteProgram( user, program ) )
        {
            errors.add( "User has no data write access to program: " + program.getUid() );
        }

        TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

        if ( !canDataReadTrackedEntityType( user, trackedEntityType ) )
        {
            errors.add( "User has no data write access to tracked entity: " + trackedEntityType.getUid() );
        }

        if ( !skipOwnershipCheck && !ownershipAccessManager.hasAccess( user, trackedEntityInstance, program ) )
        {
            errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
        }

        return errors;
    }

    @Override
    public List<String> canRead( User user, ProgramInstance programInstance, boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programInstance == null )
        {
            return errors;
        }

        Program program = programInstance.getProgram();

        if ( !canDataReadProgram( user, program ) )
        {
            errors.add( "User has no data read access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            if ( !canDataReadTrackedEntityType( user, program.getTrackedEntityType() ) )
            {
                errors.add(
                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            if ( !skipOwnershipCheck
                && !ownershipAccessManager.hasAccess( user, programInstance.getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }
        }
        else // this branch will only happen if coming from /events
        {
            OrganisationUnit ou = programInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserSearchHierarchyCached( user, ou ) )
                {
                    errors.add( "User has no read access to organisation unit: " + ou.getUid() );
                }
            }
        }

        return errors;
    }

    @Override
    public List<String> canCreate( User user, ProgramInstance programInstance, boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programInstance == null )
        {
            return errors;
        }

        Program program = programInstance.getProgram();

        OrganisationUnit ou = programInstance.getOrganisationUnit();
        if ( ou != null )
        {
            if ( !organisationUnitService.isInUserHierarchyCached( user, ou ) )
            {
                errors.add( "User has no create access to organisation unit: " + ou.getUid() );
            }
        }

        if ( !canDataWriteProgram( user, program ) )
        {
            errors.add( "User has no data write access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            if ( !canDataReadTrackedEntityType( user, program.getTrackedEntityType() ) )
            {
                errors.add(
                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            if ( !skipOwnershipCheck
                && !ownershipAccessManager.hasAccess( user, programInstance.getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }
        }

        return errors;
    }

    @Override
    public List<String> canUpdate( User user, ProgramInstance programInstance, boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programInstance == null )
        {
            return errors;
        }

        Program program = programInstance.getProgram();

        if ( !canDataWriteProgram( user, program ) )
        {
            errors.add( "User has no data write access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            if ( !canDataReadTrackedEntityType( user, program.getTrackedEntityType() ) )
            {
                errors.add(
                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            if ( !skipOwnershipCheck
                && !ownershipAccessManager.hasAccess( user, programInstance.getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }

        }
        else
        {
            OrganisationUnit ou = programInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserHierarchyCached( user, ou ) )
                {
                    errors.add( "User has no write access to organisation unit: " + ou.getUid() );
                }
            }
        }

        return errors;
    }

    @Override
    public List<String> canDelete( User user, ProgramInstance programInstance, boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programInstance == null )
        {
            return errors;
        }

        Program program = programInstance.getProgram();

        if ( !canDataWriteProgram( user, program ) )
        {
            errors.add( "User has no data write access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            if ( !canDataReadTrackedEntityType( user, program.getTrackedEntityType() ) )
            {
                errors.add(
                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            if ( !skipOwnershipCheck
                && !ownershipAccessManager.hasAccess( user, programInstance.getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }
        }

        else
        {
            OrganisationUnit ou = programInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserHierarchyCached( user, ou ) )
                {
                    errors.add( "User has no delete access to organisation unit: " + ou.getUid() );
                }
            }
        }

        return errors;
    }

    @Override
    public List<String> canRead( User user, ProgramStageInstance programStageInstance, boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programStageInstance == null )
        {
            return errors;
        }

        ProgramStage programStage = programStageInstance.getProgramStage();

        if ( isNull( programStage ) )
        {
            return errors;
        }

        Program program = programStage.getProgram();

        if ( !canDataReadProgram( user, program ) )
        {
            errors.add( "User has no data read access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            if ( !canDataReadProgramStage( user, programStage ) )
            {
                errors.add( "User has no data read access to program stage: " + programStage.getUid() );
            }

            if ( !canDataReadTrackedEntityType( user, program.getTrackedEntityType() ) )
            {
                errors.add(
                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            if ( !skipOwnershipCheck && !ownershipAccessManager.hasAccess( user,
                programStageInstance.getProgramInstance().getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }
        }
        else
        {
            OrganisationUnit ou = programStageInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserSearchHierarchyCached( user, ou ) )
                {
                    errors.add( "User has no read access to organisation unit: " + ou.getUid() );
                }
            }
        }

        errors.addAll( canRead( user, programStageInstance.getAttributeOptionCombo() ) );

        return errors;
    }

    @Override
    public List<String> canCreate( User user, ProgramStageInstance programStageInstance, boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programStageInstance == null )
        {
            return errors;
        }

        ProgramStage programStage = programStageInstance.getProgramStage();

        if ( isNull( programStage ) )
        {
            return errors;
        }

        Program program = programStage.getProgram();

        OrganisationUnit ou = programStageInstance.getOrganisationUnit();
        if ( ou != null )
        {
            if ( programStageInstance.isCreatableInSearchScope()
                ? !organisationUnitService.isInUserSearchHierarchyCached( user, ou )
                : !organisationUnitService.isInUserHierarchyCached( user, ou ) )
            {
                errors.add( "User has no create access to organisation unit: " + ou.getUid() );
            }
        }

        if ( program.isWithoutRegistration() )
        {
            if ( !canDataWriteProgram( user, program ) )
            {
                errors.add( "User has no data write access to program: " + program.getUid() );
            }
        }
        else
        {
            if ( !aclService.canDataWrite( user, programStage ) )
            {
                errors.add( "User has no data write access to program stage: " + programStage.getUid() );
            }

            if ( !canDataReadProgram( user, program ) )
            {
                errors.add( "User has no data read access to program: " + program.getUid() );
            }

            if ( !canDataReadTrackedEntityType( user, program.getTrackedEntityType() ) )
            {
                errors.add(
                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            if ( !skipOwnershipCheck && !ownershipAccessManager.hasAccess( user,
                programStageInstance.getProgramInstance().getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }
        }

        errors.addAll( canWrite( user, programStageInstance.getAttributeOptionCombo() ) );

        return errors;
    }

    @Override
    public List<String> canUpdate( User user, ProgramStageInstance programStageInstance, boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programStageInstance == null )
        {
            return errors;
        }

        ProgramStage programStage = programStageInstance.getProgramStage();

        if ( isNull( programStage ) )
        {
            return errors;
        }

        Program program = programStage.getProgram();

        if ( program.isWithoutRegistration() )
        {
            if ( !canDataWriteProgram( user, program ) )
            {
                errors.add( "User has no data write access to program: " + program.getUid() );
            }
        }
        else
        {
            if ( !aclService.canDataWrite( user, programStage ) )
            {
                errors.add( "User has no data write access to program stage: " + programStage.getUid() );
            }

            if ( !canDataReadProgram( user, program ) )
            {
                errors.add( "User has no data read access to program: " + program.getUid() );
            }

            if ( !canDataReadTrackedEntityType( user, program.getTrackedEntityType() ) )
            {
                errors.add(
                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            OrganisationUnit ou = programStageInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserSearchHierarchyCached( user, ou ) )
                {
                    errors.add( "User has no update access to organisation unit: " + ou.getUid() );
                }
            }

            if ( !skipOwnershipCheck && !ownershipAccessManager.hasAccess( user,
                programStageInstance.getProgramInstance().getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }
        }

        errors.addAll( canWrite( user, programStageInstance.getAttributeOptionCombo() ) );

        return errors;
    }

    @Override
    public List<String> canDelete( User user, ProgramStageInstance programStageInstance, boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programStageInstance == null )
        {
            return errors;
        }

        ProgramStage programStage = programStageInstance.getProgramStage();

        if ( isNull( programStage ) )
        {
            return errors;
        }

        Program program = programStage.getProgram();

        if ( program.isWithoutRegistration() )
        {
            OrganisationUnit ou = programStageInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserHierarchyCached( user, ou ) )
                {
                    errors.add( "User has no delete access to organisation unit: " + ou.getUid() );
                }
            }

            if ( !canDataWriteProgram( user, program ) )
            {
                errors.add( "User has no data write access to program: " + program.getUid() );
            }
        }
        else
        {
            if ( !aclService.canDataWrite( user, programStage ) )
            {
                errors.add( "User has no data write access to program stage: " + programStage.getUid() );
            }

            if ( !canDataReadProgram( user, program ) )
            {
                errors.add( "User has no data read access to program: " + program.getUid() );
            }

            if ( !canDataReadTrackedEntityType( user, program.getTrackedEntityType() ) )
            {
                errors.add(
                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            if ( !skipOwnershipCheck && !ownershipAccessManager.hasAccess( user,
                programStageInstance.getProgramInstance().getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }
        }

        errors.addAll( canWrite( user, programStageInstance.getAttributeOptionCombo() ) );

        return errors;
    }

    @Override
    public List<String> canRead( User user, Relationship relationship )
    {
        List<String> errors = new ArrayList<>();
        RelationshipType relationshipType;
        RelationshipItem from;
        RelationshipItem to;

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || relationship == null )
        {
            return errors;
        }

        relationshipType = relationship.getRelationshipType();

        if ( !aclService.canDataRead( user, relationshipType ) )
        {
            errors.add( "User has no data read access to relationshipType: " + relationshipType.getUid() );
        }

        from = relationship.getFrom();
        to = relationship.getTo();

        errors.addAll( canRead( user, from.getTrackedEntityInstance() ) );
        errors.addAll( canRead( user, from.getProgramInstance(), false ) );
        errors.addAll( canRead( user, from.getProgramStageInstance(), false ) );

        errors.addAll( canRead( user, to.getTrackedEntityInstance() ) );
        errors.addAll( canRead( user, to.getProgramInstance(), false ) );
        errors.addAll( canRead( user, to.getProgramStageInstance(), false ) );

        return errors;
    }

    @Override
    public List<String> canWrite( User user, Relationship relationship )
    {
        List<String> errors = new ArrayList<>();
        RelationshipType relationshipType;
        RelationshipItem from;
        RelationshipItem to;

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || relationship == null )
        {
            return errors;
        }

        relationshipType = relationship.getRelationshipType();

        if ( !aclService.canDataWrite( user, relationshipType ) )
        {
            errors.add( "User has no data write access to relationshipType: " + relationshipType.getUid() );
        }

        from = relationship.getFrom();
        to = relationship.getTo();

        errors.addAll( canWrite( user, from.getTrackedEntityInstance() ) );
        errors.addAll( canUpdate( user, from.getProgramInstance(), false ) );
        errors.addAll( canUpdate( user, from.getProgramStageInstance(), false ) );

        errors.addAll( canWrite( user, to.getTrackedEntityInstance() ) );
        errors.addAll( canUpdate( user, to.getProgramInstance(), false ) );
        errors.addAll( canUpdate( user, to.getProgramStageInstance(), false ) );

        return errors;
    }

    @Override
    public List<String> canRead( User user, ProgramStageInstance programStageInstance, DataElement dataElement,
        boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() )
        {
            return errors;
        }

        errors.addAll( canRead( user, programStageInstance, skipOwnershipCheck ) );

        if ( !canReadDataElement( user, dataElement ) )
        {
            errors.add( "User has no read access to data element: " + dataElement.getUid() );
        }

        return errors;
    }

    @Override
    public List<String> canWrite( User user, ProgramStageInstance programStageInstance, DataElement dataElement,
        boolean skipOwnershipCheck )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() )
        {
            return errors;
        }

        errors.addAll( canUpdate( user, programStageInstance, skipOwnershipCheck ) );

        if ( !canReadDataElement( user, dataElement ) )
        {
            errors.add( "User has no read access to data element: " + dataElement.getUid() );
        }

        return errors;
    }

    @Override
    public List<String> canRead( User user, CategoryOptionCombo categoryOptionCombo )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() || categoryOptionCombo == null )
        {
            return errors;
        }

        errors.addAll( canDataReadOptionComboCache( user, categoryOptionCombo ) );

        return errors;
    }

    @Override
    public List<String> canWrite( User user, CategoryOptionCombo categoryOptionCombo )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() || categoryOptionCombo == null )
        {
            return errors;
        }

        errors.addAll( canDataWriteOptionComboCache( user, categoryOptionCombo ) );

        return errors;
    }

    //------------------------------------------------------------------------------------
    // Private Helper & Cache methods
    //------------------------------------------------------------------------------------

    private boolean isNull( ProgramStage programStage )
    {
        return programStage == null || programStage.getProgram() == null;
    }

    private boolean canDataReadTrackedEntityType( User user, TrackedEntityType trackedEntityType )
    {
        if ( user == null || user.isSuper() || trackedEntityType == null )
        {
            return true;
        }

        return canUserDataReadTrackedEntityTypeCache
            .get( TextUtils.joinHyphen( user.getUid(), trackedEntityType.getUid() ),
                s -> aclService.canDataRead( user, trackedEntityType ) ).orElse( false );
    }

    private boolean canDataReadProgram( User user, Program program )
    {
        if ( user == null || user.isSuper() || program == null )
        {
            return true;
        }

        return canUserDataReadProgramCache
            .get( TextUtils.joinHyphen( user.getUid(), program.getUid() ),
                s -> aclService.canDataRead( user, program ) ).orElse( false );
    }

    private boolean canDataWriteProgram( User user, Program program )
    {
        if ( user == null || user.isSuper() || program == null )
        {
            return true;
        }

        return canUserDataWriteProgramCache
            .get( TextUtils.joinHyphen( user.getUid(), program.getUid() ),
                s -> aclService.canDataWrite( user, program ) ).orElse( false );
    }

    private boolean canDataReadProgramStage( User user, ProgramStage programStage )
    {
        if ( user == null || user.isSuper() || programStage == null )
        {
            return true;
        }

        return canUserDataReadProgramStageCache
            .get( TextUtils.joinHyphen(user.getUid(), programStage.getUid() ),
                s -> aclService.canDataRead( user, programStage ) ).orElse( false );
    }

    private boolean canReadDataElement( User user, DataElement dataElement )
    {
        if ( user == null || user.isSuper() || dataElement == null )
        {
            return true;
        }

        return canUserReadDataElementCache
            .get( TextUtils.joinHyphen( user.getUid(), dataElement.getUid() ),
                s -> aclService.canRead( user, dataElement ) ).orElse( false );
    }

    private List<String> canDataReadOptionCombo( User user, CategoryOptionCombo optionCombo )
    {
        List<String> errors = new ArrayList<>();

        for ( CategoryOption categoryOption : optionCombo.getCategoryOptions() )
        {
            if ( !aclService.canDataRead( user, categoryOption ) )
            {
                errors.add( "User has no read access to category option: " + categoryOption.getUid() );
            }
        }
        return errors;
    }

    private List<String> canDataReadOptionComboCache( User user, CategoryOptionCombo optionCombo )
    {
        String cacheKey = TextUtils.joinHyphen( user.getUid(), optionCombo.getUid() );

        return canUserDataReadCOCCache.get( cacheKey, key -> canDataReadOptionCombo( user, optionCombo ) ).orElse(
            Collections.EMPTY_LIST );

    }

    private List<String> canDataWriteOptionCombo( User user, CategoryOptionCombo optionCombo )
    {
        List<String> errors = new ArrayList<>();

        for ( CategoryOption categoryOption : optionCombo.getCategoryOptions() )
        {
            if ( !aclService.canDataWrite( user, categoryOption ) )
            {
                errors.add( "User has no read access to category option: " + categoryOption.getUid() );
            }
        }
        return errors;
    }

    private List<String> canDataWriteOptionComboCache( User user, CategoryOptionCombo optionCombo )
    {
        String cacheKey = TextUtils.joinHyphen( user.getUid(), optionCombo.getUid() );

        return canUserDataWriteCOCCache.get( cacheKey, key -> canDataWriteOptionCombo( user, optionCombo ) ).orElse(
            Collections.EMPTY_LIST );

    }




}
