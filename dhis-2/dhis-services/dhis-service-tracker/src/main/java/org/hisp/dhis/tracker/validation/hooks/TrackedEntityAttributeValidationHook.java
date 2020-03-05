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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class TrackedEntityAttributeValidationHook
    extends AbstractTrackerValidationHook
{
    @Autowired
    protected FileResourceService fileResourceService;

    @Autowired
    protected TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    protected ReservedValueService reservedValueService;

    @Override
    public int getOrder()
    {
        return 3;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        if ( bundle.getImportStrategy().isDelete() )
        {
            return Collections.emptyList();
        }

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );

        for ( TrackedEntity trackedEntity : bundle.getTrackedEntities() )
        {
            reporter.increment( trackedEntity );

            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, trackedEntity.getTrackedEntity() );

            OrganisationUnit orgUnit = getOrganisationUnit( bundle, trackedEntity );

            validateAttributes( reporter, bundle, trackedEntity, trackedEntityInstance, orgUnit );
        }

        return reporter.getReportList();
    }

    protected void validateAttributes( ValidationErrorReporter errorReporter, TrackerBundle bundle,
        TrackedEntity trackedEntity, TrackedEntityInstance trackedEntityInstance, OrganisationUnit orgUnit )
    {
        Objects.requireNonNull( trackedEntity, Constants.TRACKED_ENTITY_CANT_BE_NULL );

        Map<String, TrackedEntityAttributeValue> valueMap = Collections.EMPTY_MAP;
        if ( trackedEntityInstance != null )
        {
            valueMap = trackedEntityInstance.getTrackedEntityAttributeValues()
                .stream()
                .collect( Collectors.toMap( v -> v.getAttribute().getUid(), v -> v ) );
        }


        for ( Attribute attribute : trackedEntity.getAttributes() )
        {
            TrackedEntityAttribute trackedEntityAttribute = PreheatHelper
                .getTrackedEntityAttribute( bundle, attribute.getAttribute() );
            if ( trackedEntityAttribute == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1006 )
                    .addArg( attribute.getAttribute() ) );
                continue;
            }

            // Should this be an error instead maybe? if value is NULL empty -> delete
            if ( StringUtils.isEmpty( attribute.getValue() ) )
            {
                continue;
            }

            // look up in the preheater?
            TrackedEntityAttributeValue trackedEntityAttributeValue = valueMap.get( trackedEntityAttribute.getUid() );

            if ( trackedEntityAttributeValue == null )
            {
                trackedEntityAttributeValue = new TrackedEntityAttributeValue();
                trackedEntityAttributeValue.setEntityInstance( trackedEntityInstance );
                trackedEntityAttributeValue.setValue( attribute.getValue() );
                trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
            }
            validateAttributeValue( errorReporter, trackedEntityAttributeValue );

            validateTextPattern( errorReporter, bundle, attribute, trackedEntityAttribute,
                trackedEntityAttributeValue );

            validateAttrValueType( errorReporter, attribute, trackedEntityAttribute );

            // NOTE: This is "THE" potential performance killer...
            // "Error validating attribute, not unique; Error `{0}`"
            validateAttributeUniqueness( errorReporter,
                attribute.getValue(),
                trackedEntityAttribute,
                trackedEntityInstance,
                orgUnit );

            validateFileNotAlreadyAssigned( errorReporter, bundle, attribute, trackedEntityInstance, valueMap );
        }
    }

    protected void validateFileNotAlreadyAssigned( ValidationErrorReporter errorReporter, TrackerBundle bundle,
        Attribute attr, TrackedEntityInstance tei, Map<String, TrackedEntityAttributeValue> valueMap )
    {
        Objects.requireNonNull( attr, "Attribute can't be null" );

        boolean attrIsFile = attr.getValueType() != null && attr.getValueType().isFile();

        if ( tei != null && attrIsFile )
        {
//            List<String> existingValues = new ArrayList<>();
//            tei.getTrackedEntityAttributeValues().stream()
//                .filter( attrVal -> attrVal.getAttribute().getValueType().isFile() )
//                .filter( attrVal -> attrVal.getAttribute().getUid()
//                    .equals( attr.getAttribute() ) ) // << Unsure about this, this differs from the original "old" code.
//                .forEach( attrVal -> existingValues.add( attrVal.getValue() ) );

            TrackedEntityAttributeValue trackedEntityAttributeValue = valueMap.get( attr.getAttribute() );
            boolean isFile = trackedEntityAttributeValue.getAttribute().getValueType().isFile();
            if ( !isFile )
            {
                return;
            }

            FileResource fileResource = bundle.getPreheat()
                .get( TrackerIdScheme.UID, FileResource.class, attr.getValue() );
            if ( fileResource == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1084 )
                    .addArg( attr.getValue() ) );
            }

            if ( fileResource != null && fileResource.isAssigned() )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1009 )
                    .addArg( attr.getValue() ) );
            }
        }
    }

    // For å ikke å blokke eksisterende reservete verdier så tilater man derfor  Objects.equals( value, oldValue )
    protected boolean validateReservedValues( TrackedEntityAttribute attribute, String value, String oldValue )
    {
        Objects.requireNonNull( attribute, "TrackedEntityAttribute can't be null" );
        // optimize to
        return Objects.equals( value, oldValue ) ||
            TextPatternValidationUtils.validateTextPatternValue( attribute.getTextPattern(), value ) ||
            reservedValueService.isReserved( attribute.getTextPattern(), value );
    }

    protected void validateTextPattern( ValidationErrorReporter errorReporter, TrackerBundle bundle,
        Attribute attr, TrackedEntityAttribute teAttr, TrackedEntityAttributeValue teiAttributeValue )
    {
        Objects.requireNonNull( attr, "Attribute can't be null" );
        Objects.requireNonNull( teAttr, "TrackedEntityAttribute can't be null" );

        // Should we check the text pattern even if its not generated?
        // TextPatternValidationUtils.validateTextPatternValue( attribute.getTextPattern(), value )

        // Should we fail of there is no pattern and its generated?

        if ( teAttr.getTextPattern() != null && teAttr.isGenerated() && !bundle.isSkipTextPatternValidation() )
        {
            String oldValue = teiAttributeValue != null ? teiAttributeValue.getValue() : null;


            if ( !validateReservedValues( teAttr, attr.getValue(), oldValue ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1008 )
                    .addArg( attr.getValue() ) );
            }
        }
    }

    public void validateAttributeValue( ValidationErrorReporter errorReporter,
        TrackedEntityAttributeValue attributeValue )
    {
        Objects.requireNonNull( attributeValue, TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );

        if ( attributeValue.getAttribute().getValueType() == null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1078 )
                .addArg( attributeValue.getAttribute().getValueType() ) );
        }

        boolean confidentialBool = attributeValue.getAttribute().isConfidentialBool();


        // NOTE: Need some input on the encryption check here
//        if ( attributeValue.getAttribute().isConfidentialBool() &&
//            !dhisConfigurationProvider.getEncryptionStatus().isOk() )
//        {
        //
        // This straightfowarad just check config....
//            throw new IllegalStateException( "Unable to encrypt data, encryption is not correctly configured" );
//        }

        String result = dataValueIsValid( attributeValue.getValue(), attributeValue.getAttribute().getValueType() );
        if ( result != null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1085 )
                .addArg( attributeValue.getAttribute() )
                .addArg( result ) );
        }
    }
}
