package org.hisp.dhis.tracker.validation.hooks;

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

import com.vividsolutions.jts.geom.Geometry;
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.*;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractTrackerValidationHook
    implements TrackerValidationHook

{
    @Autowired
    protected TrackedEntityAttributeService teAttrService;

    @Autowired
    protected TrackerAccessManager trackerAccessManager;

    @Autowired
    protected OrganisationUnitService organisationUnitService;

    @Autowired
    protected ProgramInstanceService programInstanceService;

    protected void validateGeometryFromCoordinates( ValidationErrorReporter errorReporter, String coordinates,
        FeatureType featureType )
    {
        Objects.requireNonNull( featureType, "FeatureType can't be null" );

        if ( coordinates != null && FeatureType.NONE != featureType )
        {
            try
            {
                GeoUtils.getGeometryFromCoordinatesAndType( featureType, coordinates );
            }
            catch ( IOException e )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1013 )
                    .addArg( coordinates )
                    .addArg( e.getMessage() ) );
            }
        }
    }

    protected void validateAttrValueType( ValidationErrorReporter errorReporter, Attribute attr,
        TrackedEntityAttribute teAttr )
    {
        Objects.requireNonNull( attr, Constants.ATTRIBUTE_CANT_BE_NULL );
        Objects.requireNonNull( teAttr, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        String error = teAttrService.validateValueType( teAttr, attr.getValue() );
        if ( error != null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1007 )
                .addArg( error ) );
        }
    }

    protected void validateAttributeUniqueness( ValidationErrorReporter errorReporter,
        String value,
        TrackedEntityAttribute trackedEntityAttribute,
        TrackedEntityInstance trackedEntityInstanceUid,
        OrganisationUnit organisationUnit )
    {
        Objects.requireNonNull( trackedEntityAttribute, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        if ( Boolean.TRUE.equals( trackedEntityAttribute.isUnique() ) )
        {
            String error = teAttrService.validateAttributeUniquenessWithinScope(
                trackedEntityAttribute,
                value,
                trackedEntityInstanceUid,
                organisationUnit );

            if ( error != null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1064 )
                    .addArg( error ) );
            }
        }
    }

    protected void validateGeo( ValidationErrorReporter errorReporter, Geometry geometry,
        String coordinates, FeatureType featureType )
    {
        //NOTE: Is both (coordinates && geometry) at same time possible?
        if ( coordinates != null )
        {
            validateGeometryFromCoordinates( errorReporter, coordinates, featureType );
        }

        if ( geometry != null )
        {
            if ( featureType == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1074 ) );
                return;
            }

            FeatureType typeFromName = FeatureType.getTypeFromName( geometry.getGeometryType() );

            if ( FeatureType.NONE == featureType || featureType != typeFromName )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1012 )
                    .addArg( featureType.name() ) );
            }
        }
    }

    protected OrganisationUnit getOrganisationUnit( TrackerBundle bundle, TrackedEntity te )
    {
        Objects.requireNonNull( bundle, TRACKER_BUNDLE_CANT_BE_NULL );
        Objects.requireNonNull( te, TRACKED_ENTITY_CANT_BE_NULL );

        TrackedEntityInstance trackedEntityInstance = PreheatHelper
            .getTrackedEntityInstance( bundle, te.getTrackedEntity() );

        OrganisationUnit organisationUnit =
            trackedEntityInstance != null ? trackedEntityInstance.getOrganisationUnit() : null;

        return bundle.getImportStrategy().isCreate()
            ? PreheatHelper.getOrganisationUnit( bundle, te.getOrgUnit() )
            : organisationUnit;
    }

    protected TrackedEntityType getTrackedEntityType( TrackerBundle bundle, TrackedEntity te )
    {
        Objects.requireNonNull( bundle, TRACKER_BUNDLE_CANT_BE_NULL );
        Objects.requireNonNull( te, TRACKED_ENTITY_CANT_BE_NULL );

        TrackedEntityInstance trackedEntityInstance = PreheatHelper
            .getTrackedEntityInstance( bundle, te.getTrackedEntity() );

        TrackedEntityType trackedEntityType =
            trackedEntityInstance != null ? trackedEntityInstance.getTrackedEntityType() : null;

        return bundle.getImportStrategy().isCreate()
            ? PreheatHelper.getTrackedEntityType( bundle, te.getTrackedEntityType() )
            : trackedEntityType;
    }

    protected ProgramInstance getProgramInstance( User actingUser, ProgramInstance programInstance,
        TrackedEntityInstance trackedEntityInstance, Program program )
    {
        Objects.requireNonNull( program, PROGRAM_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( actingUser, USER_CANT_BE_NULL );

        if ( program.isRegistration() )
        {
            if ( programInstance == null && trackedEntityInstance != null )
            {
                List<ProgramInstance> activeProgramInstances = new ArrayList<>( programInstanceService
                    .getProgramInstances( trackedEntityInstance, program, ProgramStatus.ACTIVE ) );

                if ( activeProgramInstances.size() == 1 )
                {
                    programInstance = activeProgramInstances.get( 0 );
                }
            }
        }
        else
        {
            // NOTE: This is cached in the prev. event importer? What do we do here?
            List<ProgramInstance> activeProgramInstances = programInstanceService
                .getProgramInstances( program, ProgramStatus.ACTIVE );

            if ( activeProgramInstances.isEmpty() )
            {
                ProgramInstance pi = new ProgramInstance();
                pi.setEnrollmentDate( new Date() );
                pi.setIncidentDate( new Date() );
                pi.setProgram( program );
                pi.setStatus( ProgramStatus.ACTIVE );
                pi.setStoredBy( actingUser.getUsername() );
                programInstance = pi;
            }
            else if ( activeProgramInstances.size() == 1 )
            {
                programInstance = activeProgramInstances.get( 0 );
            }
        }

        return programInstance;
    }

    public boolean isValidDateString( String dateString )
    {
        Objects.requireNonNull( dateString, "Date string can not be null" );

        return DateUtils.dateIsValid( dateString );
    }

    public boolean isValidDateStringAndNotNull( String dateString )
    {
        return dateString != null && DateUtils.dateIsValid( dateString );
    }

    public boolean isValidDateAndNotNull( Date date )
    {
        return date != null && DateUtils.getMediumDateString( date ) != null
            && isValidDateString( DateUtils.getMediumDateString( date ) );
    }
}
