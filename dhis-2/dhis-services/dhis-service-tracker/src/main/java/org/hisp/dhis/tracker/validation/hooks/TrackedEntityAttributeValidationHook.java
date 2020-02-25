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
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class TrackedEntityAttributeValidationHook
    extends AbstractTrackerValidationHook
{

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
        Objects.requireNonNull( errorReporter, "ValidationErrorReporter can't be null" );
        Objects.requireNonNull( bundle, "TrackerBundle can't be null" );
        Objects.requireNonNull( trackedEntity, "TrackedEntity can't be null" );

        // For looking up existing trackedEntityInstance attr. ie. if it is an update. Could/should this be done in the preheater instead?
        Map<String, TrackedEntityAttributeValue> valueMap = getTeiAttributeValueMap(
            trackedEntityAttributeValueService.getTrackedEntityAttributeValues( trackedEntityInstance ) );

        for ( Attribute attribute : trackedEntity.getAttributes() )
        {
            // Should this be an error instead maybe?
            if ( StringUtils.isEmpty( attribute.getValue() ) )
            {
                continue;
            }

            TrackedEntityAttribute trackedEntityAttribute = PreheatHelper
                .getTrackedEntityAttribute( bundle, attribute.getAttribute() );
            if ( trackedEntityAttribute == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1006 )
                    .addArg( attribute.getAttribute() ) );
                continue;
            }

            // look up in the preheater
            TrackedEntityAttributeValue trackedEntityAttributeValue = valueMap.get( trackedEntityAttribute.getUid() );

            if ( trackedEntityAttributeValue == null )
            {
                trackedEntityAttributeValue = new TrackedEntityAttributeValue();
                trackedEntityAttributeValue.setEntityInstance( trackedEntityInstance );
                trackedEntityAttributeValue.setValue( attribute.getValue() );
                trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
            }
            validateAttributeValue( errorReporter, trackedEntityAttributeValue );

            validateTextPattern( errorReporter, bundle, attribute, trackedEntityAttribute, trackedEntityAttributeValue );

            validateAttrValueType( errorReporter, attribute, trackedEntityAttribute );

            // NOTE: This is "THE" potential performance killer...
            // "Error validating attribute, not unique; Error `{0}`"
            validateAttributeUniqueness( errorReporter,
                attribute.getValue(),
                trackedEntityAttribute,
                trackedEntityInstance,
                orgUnit );

            validateFileNotAlreadyAssigned( errorReporter, attribute, trackedEntityInstance );
        }
    }

    public void validateAttributeValue( ValidationErrorReporter errorReporter,
        TrackedEntityAttributeValue attributeValue )
    {
        Objects.requireNonNull( errorReporter, "ValidationErrorReporter can't be null" );
        Objects.requireNonNull( attributeValue, "TrackedEntityAttributeValue can't be null" );

        if ( attributeValue.getAttribute().getValueType() == null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1078 )
                .addArg( attributeValue.getAttribute().getValueType() ) );
        }

//        if ( attributeValue.getAttribute().isConfidentialBool() &&
//            !dhisConfigurationProvider.getEncryptionStatus().isOk() )
//        {
//            throw new IllegalStateException( "Unable to encrypt data, encryption is not correctly configured" );
//        }

        String result = dataValueIsValid( attributeValue.getValue(), attributeValue.getAttribute().getValueType() );
        if ( result != null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1078 )
                .addArg( attributeValue.getAttribute() )
                .addArg( result ) );
        }

//        if ( attributeValue.getAttribute().getValueType().isFile() && !addFileValue( attributeValue ) )
//        {
//            throw new IllegalQueryException(
//                String.format( "FileResource with id '%s' not found", attributeValue.getValue() ) );
//        }

//        if ( attributeValue.getValue() != null )
//        {
//            attributeValueStore.saveVoid( attributeValue );
//
//            if ( attributeValue.getAttribute().isGenerated() && attributeValue.getAttribute().getTextPattern() != null )
//            {
//                reservedValueService
//                    .useReservedValue( attributeValue.getAttribute().getTextPattern(), attributeValue.getValue() );
//            }
//        }
    }

}
