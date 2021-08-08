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
package org.hisp.dhis.tracker.validation.hooks;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1006;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1009;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1076;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1077;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1084;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1085;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1090;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1112;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL;

import java.util.*;
import java.util.stream.Collectors;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.util.Constant;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.service.attribute.TrackedAttributeValidationService;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class TrackedEntityAttributeValidationHook extends AttributeValidationHook
{
    private final DhisConfigurationProvider dhisConfigurationProvider;

    public TrackedEntityAttributeValidationHook( TrackedAttributeValidationService teAttrService,
        DhisConfigurationProvider dhisConfigurationProvider )
    {
        super( teAttrService );
        checkNotNull( dhisConfigurationProvider );
        this.dhisConfigurationProvider = dhisConfigurationProvider;
    }

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity trackedEntity )
    {
        TrackedEntityType trackedEntityType = reporter.getValidationContext()
            .getTrackedEntityType( trackedEntity.getTrackedEntityType() );

        TrackerImportValidationContext context = reporter.getValidationContext();

        TrackedEntityInstance tei = context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() );
        OrganisationUnit organisationUnit = context.getOrganisationUnit( trackedEntity.getOrgUnit() );

        validateMandatoryAttributes( reporter, trackedEntity, trackedEntityType );
        validateAttributes( reporter, trackedEntity, tei, organisationUnit, trackedEntityType );
    }

    private void validateMandatoryAttributes( ValidationErrorReporter reporter, TrackedEntity trackedEntity,
        TrackedEntityType trackedEntityType )
    {
        if ( trackedEntityType != null )
        {
            Set<String> trackedEntityAttributes = trackedEntity.getAttributes()
                .stream()
                .map( Attribute::getAttribute )
                .collect( Collectors.toSet() );

            trackedEntityType.getTrackedEntityTypeAttributes()
                .stream()
                .filter( trackedEntityTypeAttribute -> Boolean.TRUE.equals( trackedEntityTypeAttribute.isMandatory() ) )
                .map( TrackedEntityTypeAttribute::getTrackedEntityAttribute )
                .map( BaseIdentifiableObject::getUid )
                .filter( mandatoryAttributeUid -> !trackedEntityAttributes.contains( mandatoryAttributeUid ) )
                .forEach(
                    attribute -> addError( reporter, E1090, attribute, trackedEntityType.getUid(),
                        trackedEntity.getTrackedEntity() ) );
        }
    }

    protected void validateAttributes( ValidationErrorReporter reporter,
        TrackedEntity trackedEntity, TrackedEntityInstance tei, OrganisationUnit orgUnit,
        TrackedEntityType trackedEntityType )
    {
        checkNotNull( trackedEntity, TrackerImporterAssertErrors.TRACKED_ENTITY_CANT_BE_NULL );
        checkNotNull( trackedEntityType, TrackerImporterAssertErrors.TRACKED_ENTITY_TYPE_CANT_BE_NULL );

        Map<String, TrackedEntityAttributeValue> valueMap = new HashMap<>();
        if ( tei != null )
        {
            valueMap = tei.getTrackedEntityAttributeValues()
                .stream()
                .collect( Collectors.toMap( v -> v.getAttribute().getUid(), v -> v ) );
        }

        for ( Attribute attribute : trackedEntity.getAttributes() )
        {
            TrackedEntityAttribute tea = reporter.getValidationContext()
                .getTrackedEntityAttribute( attribute.getAttribute() );

            if ( tea == null )
            {
                addError( reporter, E1006, attribute.getAttribute() );
                continue;
            }

            if ( attribute.getValue() == null )
            {
                Optional<TrackedEntityTypeAttribute> optionalTea = Optional.of( trackedEntityType )
                    .map( tet -> tet.getTrackedEntityTypeAttributes().stream() )
                    .flatMap( tetAtts -> tetAtts.filter(
                        teaAtt -> teaAtt.getTrackedEntityAttribute().getUid().equals( attribute.getAttribute() )
                            && teaAtt.isMandatory() != null && teaAtt.isMandatory() )
                        .findFirst() );

                if ( optionalTea.isPresent() )
                    addError( reporter, E1076, TrackedEntityAttribute.class.getSimpleName(), attribute.getAttribute() );

                continue;
            }

            validateAttributeValue( reporter, tea, attribute.getValue() );
            validateAttrValueType( reporter, attribute, tea );
            validateOptionSet( reporter, tea, attribute.getValue() );

            validateAttributeUniqueness( reporter, attribute.getValue(), tea, tei, orgUnit );

            validateFileNotAlreadyAssigned( reporter, attribute, valueMap );
        }
    }

    public void validateAttributeValue( ValidationErrorReporter reporter, TrackedEntityAttribute tea, String value )
    {
        checkNotNull( tea, TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );
        checkNotNull( value, TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );

        // Validate value (string) don't exceed the max length
        addErrorIf( () -> value.length() > Constant.MAX_ATTR_VALUE_LENGTH, reporter, E1077, value,
            Constant.MAX_ATTR_VALUE_LENGTH );

        // Validate if that encryption is configured properly if someone sets
        // value to (confidential)
        boolean isConfidential = tea.isConfidentialBool();
        boolean encryptionStatusOk = dhisConfigurationProvider.getEncryptionStatus().isOk();
        addErrorIf( () -> isConfidential && !encryptionStatusOk, reporter, E1112, value );

        // Uses ValidationUtils to check that the data value corresponds to the
        // data value type set on the attribute
        final String result = dataValueIsValid( value, tea.getValueType() );
        addErrorIf( () -> result != null, reporter, E1085, tea, result );
    }

    protected void validateFileNotAlreadyAssigned( ValidationErrorReporter reporter,
        Attribute attr, Map<String, TrackedEntityAttributeValue> valueMap )
    {
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );

        boolean attrIsFile = attr.getValueType() != null && attr.getValueType().isFile();
        if ( !attrIsFile )
        {
            return;
        }

        TrackedEntityAttributeValue trackedEntityAttributeValue = valueMap.get( attr.getAttribute() );

        // Todo: how can this be possible? is this acceptable?
        if ( trackedEntityAttributeValue != null &&
            !trackedEntityAttributeValue.getAttribute().getValueType().isFile() )
        {
            return;
        }

        FileResource fileResource = reporter.getValidationContext().getFileResource( attr.getValue() );

        addErrorIfNull( fileResource, reporter, E1084, attr.getValue() );
        addErrorIf( () -> fileResource != null && fileResource.isAssigned(), reporter, E1009, attr.getValue() );
    }
}
