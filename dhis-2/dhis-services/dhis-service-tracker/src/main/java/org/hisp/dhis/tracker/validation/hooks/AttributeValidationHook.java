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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL;

import java.util.List;
import java.util.Objects;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.UniqueAttributeValue;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.service.attribute.TrackedAttributeValidationService;

/**
 * @author Luciano Fiandesio
 */
public abstract class AttributeValidationHook extends AbstractTrackerDtoValidationHook
{

    private final TrackedAttributeValidationService teAttrService;

    protected AttributeValidationHook( TrackedAttributeValidationService teAttrService )
    {
        this.teAttrService = teAttrService;
    }

    protected void validateAttrValueType( ValidationErrorReporter reporter, TrackerPreheat preheat, TrackerDto dto,
        Attribute attr,
        TrackedEntityAttribute teAttr )
    {
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );
        checkNotNull( teAttr, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        ValueType valueType = teAttr.getValueType();

        String error;

        if ( valueType.equals( ValueType.ORGANISATION_UNIT ) )
        {
            error = preheat.getOrganisationUnit( attr.getValue() ) == null
                ? " Value " + attr.getValue() + " is not a valid org unit value"
                : null;
        }
        else if ( valueType.equals( ValueType.USERNAME ) )
        {
            error = preheat.getUserByUsername( attr.getValue() ).isPresent() ? null
                : " Value " + attr.getValue() + " is not a valid username value";
        }
        else
        {
            // We need to do try/catch here since validateValueType() since
            // validateValueType can cast IllegalArgumentException e.g.
            // on at
            // org.joda.time.format.DateTimeFormatter.parseDateTime(DateTimeFormatter.java:945)
            try
            {
                error = teAttrService.validateValueType( teAttr, attr.getValue() );
            }
            catch ( Exception e )
            {
                error = e.getMessage();
            }
        }

        if ( error != null )
        {
            reporter.addError( dto, TrackerErrorCode.E1007, valueType, error );
        }
    }

    protected void validateAttributeUniqueness( ValidationErrorReporter reporter,
        TrackerPreheat preheat, TrackerDto dto,
        String value,
        TrackedEntityAttribute trackedEntityAttribute,
        TrackedEntityInstance trackedEntityInstance,
        OrganisationUnit organisationUnit )
    {
        checkNotNull( trackedEntityAttribute, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        if ( Boolean.FALSE.equals( trackedEntityAttribute.isUnique() ) )
            return;

        List<UniqueAttributeValue> uniqueAttributeValues = preheat.getUniqueAttributeValues();

        for ( UniqueAttributeValue uniqueAttributeValue : uniqueAttributeValues )
        {
            boolean isTeaUniqueInOrgUnitScope = !trackedEntityAttribute.getOrgunitScope()
                || uniqueAttributeValue.getOrgUnit().isEqualTo( organisationUnit );

            boolean isTheSameTea = uniqueAttributeValue.getAttribute().isEqualTo( trackedEntityAttribute );
            boolean hasTheSameValue = Objects.equals( uniqueAttributeValue.getValue(), value );
            boolean isNotSameTei = trackedEntityInstance == null
                || !Objects.equals( trackedEntityInstance.getUid(),
                    uniqueAttributeValue.getTeiUid() );

            if ( isTeaUniqueInOrgUnitScope
                && isTheSameTea
                && hasTheSameValue
                && isNotSameTei )
            {
                reporter.addError( dto, TrackerErrorCode.E1064, value, trackedEntityAttribute );
                return;
            }
        }
    }
}
