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

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
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
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.hisp.dhis.trackedentity.*;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

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

    protected boolean checkCanCreateGeo( ValidationErrorReporter errorReporter, TrackedEntity te )
    {
        if ( te.getCoordinates() != null && FeatureType.NONE != te.getFeatureType() )
        {
            try
            {
                GeoUtils.getGeometryFromCoordinatesAndType( te.getFeatureType(), te.getCoordinates() );
            }
            catch ( IOException e )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1013 )
                    .addArg( te.getCoordinates() )
                    .addArg( e.getMessage() ) );
                return false;
            }
        }

        return true;
    }

    protected void validateOrganisationUnit( ValidationErrorReporter errorReporter, TrackerBundle bundle,
        TrackedEntity te )
    {
        if ( bundle.getImportStrategy().isCreate() )
        {
            if ( StringUtils.isEmpty( te.getOrgUnit() ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1010 ) );
                return;
            }

            OrganisationUnit organisationUnit = PreheatHelper
                .getOrganisationUnit( bundle, te.getOrgUnit() );

            if ( organisationUnit == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1011 )
                    .addArg( te.getOrgUnit() ) );
            }

        }
        else if ( bundle.getImportStrategy().isUpdate() )
        {
            TrackedEntityInstance tei = PreheatHelper.getTrackedEntityInstance( bundle, te.getTrackedEntity() );
            if ( tei.getOrganisationUnit() == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1011 )
                    .addArg( tei ) );
            }
        }
    }

    protected boolean textPatternValueIsValid( TrackedEntityAttribute attribute, String value, String oldValue )
    {
        return Objects.equals( value, oldValue ) ||
            TextPatternValidationUtils.validateTextPatternValue( attribute.getTextPattern(), value ) ||
            reservedValueService.isReserved( attribute.getTextPattern(), value );
    }

    protected boolean validateTextPattern( ValidationErrorReporter errorReporter, Attribute attr,
        TrackedEntityAttribute teAttr,
        TrackedEntityAttributeValue teiAttributeValue )
    {
        if ( teAttr.getTextPattern() != null && teAttr.isGenerated() )
        //&& ??? !importOptions.isSkipPatternValidation()
        // MortenO: How should we deal with this in the new importer?
        {

            String oldValue = teiAttributeValue != null ? teiAttributeValue.getValue() : null;

            if ( !textPatternValueIsValid( teAttr, attr.getValue(), oldValue ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1008 )
                    .addArg( attr.getValue() ) );
                return false;
            }
        }

        return true;
    }

    protected boolean validateFileNotAlreadyAssigned( ValidationErrorReporter errorReporter, Attribute attr,
        TrackedEntityInstance tei )
    {
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
                return false;
            }
        }

        return true;
    }

    protected boolean validateAttrValueType( ValidationErrorReporter errorReporter, Attribute attr,
        TrackedEntityAttribute teAttr )
    {
        String error = teAttrService.validateValueType( teAttr, attr.getValue() );
        if ( error != null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1007 )
                .addArg( error ) );
            return false;
        }

        return true;
    }

    protected boolean validateAttrUnique( ValidationErrorReporter errorReporter, String value,
        TrackedEntityAttribute teAttr, String uid, OrganisationUnit trackedEntityOu )
    {
        if ( Boolean.TRUE.equals( teAttr.isUnique() ) )
        {
            String error = teAttrService.validateAttributeUniquenessWithinScope(
                teAttr,
                value,
                uid,
                trackedEntityOu );

            if ( error != null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1064 ).addArg( error ) );
                return false;
            }
        }

        return true;
    }

    protected Map<String, TrackedEntityAttributeValue> getTeiAttributeValueMap(
        List<TrackedEntityAttributeValue> values )
    {
        return values.stream().collect( Collectors.toMap( v -> v.getAttribute().getUid(), v -> v ) );
    }

    protected boolean validateGeo( ValidationErrorReporter errorReporter, TrackedEntity te, FeatureType featureType )
    {
        if ( te.getGeometry() != null )
        {
            FeatureType typeFromName = FeatureType.getTypeFromName( te.getGeometry().getGeometryType() );

            if ( FeatureType.NONE == featureType || featureType != typeFromName )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1012 )
                    .addArg( featureType.name() ) );
                return false;
            }
        }
        else
        {
            return checkCanCreateGeo( errorReporter, te );
        }

        return true;
    }

    protected void validateTrackedEntityType( ValidationErrorReporter errorReporter, TrackerBundle bundle,
        TrackedEntity te )
    {
        if ( bundle.getImportStrategy().isCreate() )
        {
            if ( te.getTrackedEntityType() == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1004 ) );
                return;
            }

            TrackedEntityType entityType = PreheatHelper.getTrackedEntityType( bundle, te.getTrackedEntityType() );
            if ( entityType == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1005 )
                    .addArg( te.getTrackedEntityType() ) );
            }
        }
    }

    protected OrganisationUnit getOrganisationUnit( TrackerBundle bundle, TrackedEntity te )
    {
        return bundle.getImportStrategy().isCreate()
            ? PreheatHelper.getOrganisationUnit( bundle, te.getOrgUnit() )
            : PreheatHelper.getTrackedEntityInstance( bundle, te.getTrackedEntity() ).getOrganisationUnit();
    }

    protected TrackedEntityType getTrackedEntityType( TrackerBundle bundle, TrackedEntity te )
    {
        return bundle.getImportStrategy().isCreate()
            ? PreheatHelper.getTrackedEntityType( bundle, te.getTrackedEntityType() )
            : PreheatHelper.getTrackedEntityInstance( bundle, te.getTrackedEntity() ).getTrackedEntityType();
    }
}
