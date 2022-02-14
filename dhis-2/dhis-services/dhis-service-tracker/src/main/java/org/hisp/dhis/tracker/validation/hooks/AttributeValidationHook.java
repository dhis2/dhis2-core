/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.validation.hooks;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL;

import java.util.List;
import java.util.Objects;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypeValidationService;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.UniqueAttributeValue;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;

/**
 * @author Luciano Fiandesio
 */
public abstract class AttributeValidationHook extends AbstractTrackerDtoValidationHook
{

    private final ValueTypeValidationService valueTypeValidationService;

    protected AttributeValidationHook( ValueTypeValidationService valueTypeValidationService )
    {
        checkNotNull( valueTypeValidationService );
        this.valueTypeValidationService = valueTypeValidationService;
    }

    protected void validateAttrValueType( ValidationErrorReporter errorReporter, TrackerDto dto, Attribute attr,
        TrackedEntityAttribute teAttr )
    {
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );
        checkNotNull( teAttr, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        ValueType valueType = teAttr.getValueType();

        TrackerImportValidationContext context = errorReporter.getValidationContext();

        ErrorMessage errorMessage = valueTypeValidationService.dataValueIsValid( teAttr, attr.getValue() );

        if ( errorMessage != null )
        {
            TrackerBundle bundle = context.getBundle();
            TrackerErrorReport err = TrackerErrorReport.builder()
                .uid( dto.getUid() )
                .trackerType( dto.getTrackerType() )
                .errorCode( getTrackerErrorCode( errorMessage ) )
                .addArg( valueType.toString() )
                .addArg( errorMessage.getMessage() )
                .build( bundle );
            errorReporter.addError( err );
        }
    }

    protected void validateAttributeUniqueness( ValidationErrorReporter errorReporter,
        TrackerDto dto,
        String value,
        TrackedEntityAttribute trackedEntityAttribute,
        TrackedEntityInstance trackedEntityInstance,
        OrganisationUnit organisationUnit )
    {
        checkNotNull( trackedEntityAttribute, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        if ( Boolean.FALSE.equals( trackedEntityAttribute.isUnique() ) )
            return;

        List<UniqueAttributeValue> uniqueAttributeValues = errorReporter
            .getValidationContext().getBundle().getPreheat().getUniqueAttributeValues();

        for ( UniqueAttributeValue uniqueAttributeValue : uniqueAttributeValues )
        {
            boolean isTeaUniqueInOrgUnitScope = !trackedEntityAttribute.getOrgunitScope()
                || Objects.equals( organisationUnit.getUid(), uniqueAttributeValue.getOrgUnitId() );

            boolean isTheSameTea = Objects.equals( uniqueAttributeValue.getAttributeUid(),
                trackedEntityAttribute.getUid() );
            boolean hasTheSameValue = Objects.equals( uniqueAttributeValue.getValue(), value );
            boolean isNotSameTei = trackedEntityInstance == null
                || !Objects.equals( trackedEntityInstance.getUid(),
                    uniqueAttributeValue.getTeiUid() );

            if ( isTeaUniqueInOrgUnitScope
                && isTheSameTea
                && hasTheSameValue
                && isNotSameTei )
            {
                TrackerBundle bundle = errorReporter.getValidationContext().getBundle();
                TrackerErrorReport err = TrackerErrorReport.builder()
                    .uid( dto.getUid() )
                    .trackerType( dto.getTrackerType() )
                    .errorCode( TrackerErrorCode.E1064 )
                    .addArg( value )
                    .addArg( trackedEntityAttribute.getUid() )
                    .build( bundle );
                errorReporter.addError( err );
                return;
            }
        }
    }

    private TrackerErrorCode getTrackerErrorCode( ErrorMessage errorMesage )
    {
        switch ( errorMesage.getErrorCode() )
        {
        case E2027:
            return TrackerErrorCode.E1084;
        case E2029:
            return TrackerErrorCode.E1125;
        case E2030:
            return TrackerErrorCode.E1007;
        case E2040:
            return TrackerErrorCode.E1101;
        case E2041:
            return TrackerErrorCode.E1105;
        case E2042:
            return TrackerErrorCode.E1106;
        case E2043:
            return TrackerErrorCode.E1302;
        case E2026:
            return TrackerErrorCode.E1009;
        default:
            return null;
        }
    }

}
