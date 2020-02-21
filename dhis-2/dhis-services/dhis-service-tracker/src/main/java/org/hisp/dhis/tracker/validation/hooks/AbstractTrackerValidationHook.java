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
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.*;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.hisp.dhis.trackedentity.*;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractTrackerValidationHook
    implements TrackerValidationHook

{
    @Autowired
    protected ProgramStageInstanceService programStageInstanceService;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    protected EventService eventService;

    @Autowired
    protected FileResourceService fileResourceService;

    @Autowired
    protected TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    protected TrackedEntityAttributeService teAttrService;

    @Autowired
    protected ReservedValueService reservedValueService;

    @Autowired
    protected TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

    @Autowired
    protected TrackerOwnershipManager trackerOwnershipManager;

    @Autowired
    protected TrackedEntityCommentService commentService;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    protected I18nManager i18nManager;

    @Autowired
    protected TrackerAccessManager trackerAccessManager;

    @Autowired
    protected TrackedEntityInstanceStore trackedEntityInstanceStore;

    @Autowired
    protected OrganisationUnitService organisationUnitService;

    @Autowired
    protected TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    protected AclService aclService;

    @Autowired
    protected ProgramInstanceService programInstanceService;

    @Autowired
    protected RelationshipService relationshipService;

    protected void validateGeometryFromCoordinates( ValidationErrorReporter errorReporter, String coordinates,
        FeatureType featureType )
    {
        Objects.requireNonNull( errorReporter, "ValidationErrorReporter can't be null" );
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

    protected boolean textPatternValueIsValid( TrackedEntityAttribute attribute, String value, String oldValue )
    {
        Objects.requireNonNull( attribute, "TrackedEntityAttribute can't be null" );

        return Objects.equals( value, oldValue ) ||
            TextPatternValidationUtils.validateTextPatternValue( attribute.getTextPattern(), value ) ||
            reservedValueService.isReserved( attribute.getTextPattern(), value );
    }

    protected void validateTextPattern( ValidationErrorReporter errorReporter, Attribute attr,
        TrackedEntityAttribute teAttr,
        TrackedEntityAttributeValue teiAttributeValue )
    {
        Objects.requireNonNull( errorReporter, "ValidationErrorReporter can't be null" );
        Objects.requireNonNull( attr, "Attribute can't be null" );
        Objects.requireNonNull( teAttr, "TrackedEntityAttribute can't be null" );

        if ( teAttr.getTextPattern() != null && teAttr.isGenerated() )
        //&& ??? !importOptions.isSkipPatternValidation()
        // MortenO: How should we deal with this in the new importer?
        {

            String oldValue = teiAttributeValue != null ? teiAttributeValue.getValue() : null;

            if ( !textPatternValueIsValid( teAttr, attr.getValue(), oldValue ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1008 )
                    .addArg( attr.getValue() ) );
            }
        }
    }

    protected void validateFileNotAlreadyAssigned( ValidationErrorReporter errorReporter, Attribute attr,
        TrackedEntityInstance tei )
    {
        Objects.requireNonNull( errorReporter, "ValidationErrorReporter can't be null" );
        Objects.requireNonNull( attr, "Attribute can't be null" );

        boolean attrIsFile = attr.getValueType() != null && attr.getValueType().isFile();

        if ( tei != null && attrIsFile )
        {
            List<String> existingValues = new ArrayList<>();

            tei.getTrackedEntityAttributeValues().stream()
                .filter( attrVal -> attrVal.getAttribute().getValueType().isFile() )
                .filter( attrVal -> attrVal.getAttribute().getUid()
                    .equals( attr.getAttribute() ) ) // << Unsure about this, this differs from the original "old" code.
                .forEach( attrVal -> existingValues.add( attrVal.getValue() ) );

            FileResource fileResource = fileResourceService.getFileResource( attr.getValue() );
            if ( fileResource != null && fileResource.isAssigned() && !existingValues.contains( attr.getValue() ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1009 )
                    .addArg( attr.getValue() ) );
            }
        }
    }

    protected void validateAttrValueType( ValidationErrorReporter errorReporter, Attribute attr,
        TrackedEntityAttribute teAttr )
    {
        Objects.requireNonNull( errorReporter, "ValidationErrorReporter can't be null" );
        Objects.requireNonNull( attr, "Attribute can't be null" );
        Objects.requireNonNull( teAttr, "TrackedEntityAttribute can't be null" );

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
        Objects.requireNonNull( errorReporter, "ValidationErrorReporter can't be null" );
        Objects.requireNonNull( trackedEntityAttribute, "TrackedEntityAttribute can't be null" );

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

    protected Map<String, TrackedEntityAttributeValue> getTeiAttributeValueMap(
        List<TrackedEntityAttributeValue> values )
    {
        Objects.requireNonNull( values, "Map can't be null" );

        return values.stream().collect( Collectors.toMap( v -> v.getAttribute().getUid(), v -> v ) );
    }

    protected void validateGeo( ValidationErrorReporter errorReporter, Geometry geometry,
        String coordinates, FeatureType featureType )
    {
        Objects.requireNonNull( errorReporter, "ValidationErrorReporter can't be null" );

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
        Objects.requireNonNull( bundle, "TrackerBundle can't be null" );
        Objects.requireNonNull( te, "TrackedEntity can't be null" );

        TrackedEntityInstance trackedEntityInstance = PreheatHelper
            .getTrackedEntityInstance( bundle, te.getTrackedEntity() );
        return bundle.getImportStrategy().isCreate()
            ? PreheatHelper.getOrganisationUnit( bundle, te.getOrgUnit() )
            : trackedEntityInstance != null ? trackedEntityInstance.getOrganisationUnit() : null;
    }

    protected TrackedEntityType getTrackedEntityType( TrackerBundle bundle, TrackedEntity te )
    {
        Objects.requireNonNull( bundle, "TrackerBundle can't be null" );
        Objects.requireNonNull( te, "TrackedEntity can't be null" );

        TrackedEntityInstance trackedEntityInstance = PreheatHelper
            .getTrackedEntityInstance( bundle, te.getTrackedEntity() );
        return bundle.getImportStrategy().isCreate()
            ? PreheatHelper.getTrackedEntityType( bundle, te.getTrackedEntityType() )
            : trackedEntityInstance != null ? trackedEntityInstance.getTrackedEntityType() : null;
    }

    protected boolean isValidId( TrackerIdentifier identifier, String value )
    {
        Objects.requireNonNull( identifier, "TrackerIdentifier can't be null" );

        if ( TrackerIdentifier.UID == identifier )
        {
            return !StringUtils.isEmpty( value ) && !CodeGenerator.isValidUid( value );
        }
        else
        {
            throw new IllegalArgumentException( "Only UID ids are implemented for now!" );
        }
    }

    protected ProgramInstance getProgramInstance( User actingUser, ProgramInstance programInstance,
        TrackedEntityInstance trackedEntityInstance, Program program )
    {
        Objects.requireNonNull( program, "Program can't be null" );
        Objects.requireNonNull( actingUser, "User can't be null" );

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
