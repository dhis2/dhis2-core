package org.hisp.dhis.dxf2.events;

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

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerOwnershipAccessManager;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultTrackerAccessManager implements TrackerAccessManager
{
    private final AclService aclService;
    private final TrackerOwnershipAccessManager ownershipAccessManager;

    public DefaultTrackerAccessManager( AclService aclService, TrackerOwnershipAccessManager ownershipAccessManager )
    {
        this.aclService = aclService;
        this.ownershipAccessManager = ownershipAccessManager;
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
            if ( !isInHierarchy( ou, user.getTeiSearchOrganisationUnitsWithFallback() ) )
            {
                errors.add( "User has no read access to organisation unit: " + ou.getUid() );
            }
        }

        TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

        if ( !aclService.canDataRead( user, trackedEntityType ) )
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
            if ( !isInHierarchy( ou, user.getOrganisationUnits() ) )
            {
                errors.add( "User has no write access to organisation unit: " + ou.getUid() );
            }
        }

        TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

        if ( !aclService.canDataWrite( user, trackedEntityType ) )
        {
            errors.add( "User has no data write access to tracked entity: " + trackedEntityType.getUid() );
        }

        return errors;
    }

    @Override
    public List<String> canRead( User user, TrackedEntityInstance trackedEntityInstance, Program program )
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
            if ( !isInHierarchy( ou, user.getTeiSearchOrganisationUnitsWithFallback() ) )
            {
                errors.add( "User has no read access to organisation unit: " + ou.getUid() );
            }
        }

        TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

        if ( !aclService.canDataRead( user, trackedEntityType ) )
        {
            errors.add( "User has no data read access to tracked entity: " + trackedEntityType.getUid() );
        }

        // check for ownership access
        if ( !ownershipAccessManager.hasAccess( user, trackedEntityInstance, program ) )
        {
            errors.add( TrackerOwnershipAccessManager.OWNERSHIP_ACCESS_DENIED );
        }

        return errors;
    }

    @Override
    public List<String> canWrite( User user, TrackedEntityInstance trackedEntityInstance, Program program )
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
            if ( !isInHierarchy( ou, user.getOrganisationUnits() ) )
            {
                errors.add( "User has no write access to organisation unit: " + ou.getUid() );
            }
        }

        TrackedEntityType trackedEntityType = trackedEntityInstance.getTrackedEntityType();

        if ( !aclService.canDataWrite( user, trackedEntityType ) )
        {
            errors.add( "User has no data write access to tracked entity: " + trackedEntityType.getUid() );
        }

        // check for ownership access
        if ( !ownershipAccessManager.hasAccess( user, trackedEntityInstance, program ) )
        {
            errors.add( TrackerOwnershipAccessManager.OWNERSHIP_ACCESS_DENIED );
        }

        return errors;
    }

    @Override
    public List<String> canRead( User user, ProgramInstance programInstance )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programInstance == null )
        {
            return errors;
        }

        OrganisationUnit ou = programInstance.getOrganisationUnit();

        if ( ou != null )
        { // ou should never be null, but needs to be checked for legacy reasons
            if ( !isInHierarchy( ou, user.getTeiSearchOrganisationUnitsWithFallback() ) )
            {
                errors.add( "User has no read access to organisation unit: " + ou.getUid() );
            }
        }

        Program program = programInstance.getProgram();

        if ( !aclService.canDataRead( user, program ) )
        {
            errors.add( "User has no data read access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
            {
                errors.add( "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            // check for ownership access
            if ( !ownershipAccessManager.hasAccess( user, programInstance.getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipAccessManager.OWNERSHIP_ACCESS_DENIED );
            }
        }

        return errors;
    }

    @Override
    public List<String> canWrite( User user, ProgramInstance programInstance )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programInstance == null )
        {
            return errors;
        }

        OrganisationUnit ou = programInstance.getOrganisationUnit();

        if ( ou != null )
        { // ou should never be null, but needs to be checked for legacy reasons
            if ( !isInHierarchy( ou, user.getOrganisationUnits() ) )
            {
                errors.add( "User has no write access to organisation unit: " + ou.getUid() );
            }
        }

        Program program = programInstance.getProgram();

        if ( !aclService.canDataWrite( user, program ) )
        {
            errors.add( "User has no data write access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
            {
                errors.add( "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            // check for ownership access
            if ( !ownershipAccessManager.hasAccess( user, programInstance.getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipAccessManager.OWNERSHIP_ACCESS_DENIED );
            }
        }

        return errors;
    }

    @Override
    public List<String> canRead( User user, ProgramStageInstance programStageInstance )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programStageInstance == null )
        {
            return errors;
        }

        OrganisationUnit ou = programStageInstance.getOrganisationUnit();

        if ( ou != null )
        { // ou should never be null, but needs to be checked for legacy reasons
            if ( !isInHierarchy( ou, user.getTeiSearchOrganisationUnitsWithFallback() ) )
            {
                errors.add( "User has no read access to organisation unit: " + ou.getUid() );
            }
        }

        ProgramStage programStage = programStageInstance.getProgramStage();

        if ( isNull( programStage ) )
        {
            return errors;
        }

        Program program = programStage.getProgram();

        if ( !aclService.canDataRead( user, program ) )
        {
            errors.add( "User has no data read access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            if ( !aclService.canDataRead( user, programStage ) )
            {
                errors.add( "User has no data read access to program stage: " + programStage.getUid() );
            }

            if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
            {
                errors.add( "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            // check for ownership access
            if ( !ownershipAccessManager.hasAccess( user, programStageInstance.getProgramInstance().getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipAccessManager.OWNERSHIP_ACCESS_DENIED );
            }
        }

        errors.addAll( canRead( user, programStageInstance.getAttributeOptionCombo() ) );

        return errors;
    }

    @Override
    public List<String> canWrite( User user, ProgramStageInstance programStageInstance )
    {
        List<String> errors = new ArrayList<>();

        // always allow if user == null (internal process) or user is superuser
        if ( user == null || user.isSuper() || programStageInstance == null )
        {
            return errors;
        }

        OrganisationUnit ou = programStageInstance.getOrganisationUnit();

        if ( ou != null )
        { // ou should never be null, but needs to be checked for legacy reasons
            if ( !isInHierarchy( ou, user.getOrganisationUnits() ) )
            {
                errors.add( "User has no write access to organisation unit: " + ou.getUid() );
            }
        }

        ProgramStage programStage = programStageInstance.getProgramStage();

        if ( isNull( programStage ) )
        {
            return errors;
        }

        Program program = programStage.getProgram();

        if ( program.isWithoutRegistration() )
        {
            if ( !aclService.canDataWrite( user, program ) )
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

            if ( !aclService.canDataRead( user, program ) )
            {
                errors.add( "User has no data read access to program: " + program.getUid() );
            }

            if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
            {
                errors.add( "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            // check for ownership access
            if ( !ownershipAccessManager.hasAccess( user, programStageInstance.getProgramInstance().getEntityInstance(), program ) )
            {
                errors.add( TrackerOwnershipAccessManager.OWNERSHIP_ACCESS_DENIED );
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
        errors.addAll( canRead( user, from.getProgramInstance() ) );
        errors.addAll( canRead( user, from.getProgramStageInstance() ) );

        errors.addAll( canRead( user, to.getTrackedEntityInstance() ) );
        errors.addAll( canRead( user, to.getProgramInstance() ) );
        errors.addAll( canRead( user, to.getProgramStageInstance() ) );

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
        errors.addAll( canWrite( user, from.getProgramInstance() ) );
        errors.addAll( canWrite( user, from.getProgramStageInstance() ) );

        errors.addAll( canRead( user, to.getTrackedEntityInstance() ) );
        errors.addAll( canRead( user, to.getProgramInstance() ) );
        errors.addAll( canRead( user, to.getProgramStageInstance() ) );

        return errors;
    }

    @Override
    public List<String> canRead( User user, TrackedEntityDataValue dataValue )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() || dataValue == null )
        {
            return errors;
        }

        errors.addAll( canRead( user, dataValue.getProgramStageInstance() ) );

        DataElement dataElement = dataValue.getDataElement();

        if ( !aclService.canRead( user, dataElement ) )
        {
            errors.add( "User has no read access to data element: " + dataElement.getUid() );
        }

        return errors;
    }

    @Override
    public List<String> canWrite( User user, TrackedEntityDataValue dataValue )
    {
        List<String> errors = new ArrayList<>();

        if ( user == null || user.isSuper() || dataValue == null )
        {
            return errors;
        }

        errors.addAll( canWrite( user, dataValue.getProgramStageInstance() ) );

        DataElement dataElement = dataValue.getDataElement();

        if ( !aclService.canRead( user, dataElement ) )
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

        for ( CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions() )
        {
            if ( !aclService.canDataRead( user, categoryOption ) )
            {
                errors.add( "User has no read access to category option: " + categoryOption.getUid() );
            }
        }

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

        for ( CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions() )
        {
            if ( !aclService.canDataWrite( user, categoryOption ) )
            {
                errors.add( "User has no write access to category option: " + categoryOption.getUid() );
            }
        }

        return errors;
    }

    private boolean isInHierarchy( OrganisationUnit organisationUnit, Set<OrganisationUnit> organisationUnits )
    {
        return organisationUnit != null && organisationUnits != null && organisationUnit.isDescendant( organisationUnits );
    }

    private boolean isNull( ProgramStage programStage )
    {
        return programStage == null || programStage.getProgram() == null;
    }
}
