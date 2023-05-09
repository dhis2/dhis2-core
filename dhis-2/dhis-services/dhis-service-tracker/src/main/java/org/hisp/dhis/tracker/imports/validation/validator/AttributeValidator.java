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
package org.hisp.dhis.tracker.imports.validation.validator;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.system.util.ValidationUtils.valueIsValid;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1077;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1085;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1112;
import static org.hisp.dhis.tracker.imports.validation.validator.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.imports.validation.validator.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.imports.validation.validator.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL;

import java.util.List;
import java.util.Objects;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.UniqueAttributeValue;
import org.hisp.dhis.tracker.imports.util.Constant;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.service.attribute.TrackedAttributeValidationService;

/**
 * @author Luciano Fiandesio
 */
public abstract class AttributeValidator
{

    private final TrackedAttributeValidationService teAttrService;

    private final DhisConfigurationProvider dhisConfigurationProvider;

    protected AttributeValidator( TrackedAttributeValidationService teAttrService,
        DhisConfigurationProvider dhisConfigurationProvider )
    {
        this.teAttrService = teAttrService;
        this.dhisConfigurationProvider = dhisConfigurationProvider;
    }

    protected void validateAttrValueType( Reporter reporter, TrackerPreheat preheat, TrackerDto dto,
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
            reporter.addError( dto, ValidationCode.E1007, valueType, error );
        }
    }

    public void validateAttributeValue( Reporter reporter, TrackerDto trackerDto,
        TrackedEntityAttribute tea,
        String value )
    {
        checkNotNull( tea, TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );
        checkNotNull( value, TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );

        // Validate value (string) don't exceed the max length
        reporter.addErrorIf( () -> value.length() > Constant.MAX_ATTR_VALUE_LENGTH, trackerDto,
            E1077, value,
            Constant.MAX_ATTR_VALUE_LENGTH );

        // Validate if that encryption is configured properly if someone sets
        // value to (confidential)
        boolean isConfidential = tea.isConfidentialBool();
        EncryptionStatus encryptionStatus = dhisConfigurationProvider.getEncryptionStatus();
        reporter.addErrorIf( () -> isConfidential && !encryptionStatus.isOk(), trackerDto, E1112,
            value, encryptionStatus.getKey() );

        // Uses ValidationUtils to check that the data value corresponds to the
        // data value type set on the attribute
        final String result = valueIsValid( value, tea.getValueType() );
        reporter.addErrorIf( () -> result != null, trackerDto, E1085, tea, result );
    }

    protected void validateAttributeUniqueness( Reporter reporter,
        TrackerPreheat preheat, TrackerDto dto,
        String value,
        TrackedEntityAttribute trackedEntityAttribute,
        TrackedEntity trackedEntity,
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
            boolean isNotSameTei = trackedEntity == null
                || !Objects.equals( trackedEntity.getUid(),
                    uniqueAttributeValue.getTeiUid() );

            if ( isTeaUniqueInOrgUnitScope
                && isTheSameTea
                && hasTheSameValue
                && isNotSameTei )
            {
                reporter.addError( dto, ValidationCode.E1064, value, trackedEntityAttribute );
                return;
            }
        }
    }
}
