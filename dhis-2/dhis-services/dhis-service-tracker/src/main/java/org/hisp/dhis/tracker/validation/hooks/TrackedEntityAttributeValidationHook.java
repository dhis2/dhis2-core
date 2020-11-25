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

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1006;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1008;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1009;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1076;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1077;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1084;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1085;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1112;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class TrackedEntityAttributeValidationHook extends AttributeValidationHook
{
    private static final int MAX_ATTR_VALUE_LENGTH = 1200;

    private final ReservedValueService reservedValueService;
    
    private final DhisConfigurationProvider dhisConfigurationProvider;
    
    public TrackedEntityAttributeValidationHook( TrackedEntityAttributeService teAttrService,
        ReservedValueService reservedValueService, DhisConfigurationProvider dhisConfigurationProvider )
    {
        super( teAttrService );
        checkNotNull( reservedValueService );
        checkNotNull( dhisConfigurationProvider );
        this.reservedValueService = reservedValueService;
        this.dhisConfigurationProvider = dhisConfigurationProvider;
    }

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity trackedEntity )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        TrackedEntityInstance tei = context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() );
        OrganisationUnit organisationUnit = context.getOrganisationUnit( trackedEntity.getOrgUnit() );

        validateAttributes( reporter, trackedEntity, tei, organisationUnit );
    }

    protected void validateAttributes( ValidationErrorReporter reporter,
        TrackedEntity trackedEntity, TrackedEntityInstance tei, OrganisationUnit orgUnit )
    {
        checkNotNull( trackedEntity, TrackerImporterAssertErrors.TRACKED_ENTITY_CANT_BE_NULL );

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

//            if ( StringUtils.isEmpty( attribute.getValue() ) )
            if ( attribute.getValue() == null )
            {
                //continue; ??? Just continue on empty and null?
                // TODO: Is this really correct? This check was not here originally
                //  Enrollment attr check fails on null so why not here too?
                addError( reporter, E1076, attribute );

                continue;
            }

            validateAttributeValue( reporter, tea, attribute.getValue() );
            validateTextPattern( reporter, attribute, tea, valueMap.get( tea.getUid() ) );
            validateAttrValueType( reporter, attribute, tea );

            validateAttributeUniqueness( reporter, attribute.getValue(), tea, tei, orgUnit );

            validateFileNotAlreadyAssigned( reporter, attribute, valueMap );
        }
    }

    public void validateAttributeValue( ValidationErrorReporter reporter, TrackedEntityAttribute tea, String value )
    {
        checkNotNull( tea, TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );
        checkNotNull( value, TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );

        // Validate value (string) don't exceed the max length
        addErrorIf( () -> value.length() > MAX_ATTR_VALUE_LENGTH, reporter, E1077, value, MAX_ATTR_VALUE_LENGTH );
        
        // Validate if that encryption is configured properly if someone sets value to (confidential)
        boolean isConfidential = tea.isConfidentialBool();
        boolean encryptionStatusOk = dhisConfigurationProvider.getEncryptionStatus().isOk();
        addErrorIf( () -> isConfidential && !encryptionStatusOk, reporter, E1112, value );

        // Uses ValidationUtils to check that the data value corresponds to the data value type set on the attribute
        final String result = dataValueIsValid( value, tea.getValueType() );
        addErrorIf( () -> result != null, reporter, E1085, tea, result );
    }

    protected void validateTextPattern( ValidationErrorReporter reporter,
        Attribute attribute, TrackedEntityAttribute tea, TrackedEntityAttributeValue existingValue )
    {
        TrackerBundle bundle = reporter.getValidationContext().getBundle();
        checkNotNull( attribute, ATTRIBUTE_CANT_BE_NULL );
        checkNotNull( tea, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        if ( !tea.isGenerated() )
        {
            return;
        }

        // TODO: Should we check the text pattern even if its not generated?
        // TextPatternValidationUtils.validateTextPatternValue( attribute.getTextPattern(), value )

        //TODO: Can't provoke this error since metadata importer won't allow null, empty or invalid patterns.
        if ( tea.getTextPattern() == null && !bundle.isSkipTextPatternValidation() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1111 )
                .addArg( attribute ) );
        }

        if ( tea.getTextPattern() != null && !bundle.isSkipTextPatternValidation() )
        {
            String oldValue = existingValue != null ? existingValue.getValue() : null;

            // We basically ignore the pattern validation if the value is reserved or already
            // assigned i.e. input eq. already persisted value.
            boolean isReservedOrAlreadyAssigned = Objects.equals( attribute.getValue(), oldValue ) ||
                reservedValueService.isReserved( tea.getTextPattern(), attribute.getValue() );

            boolean isValidPattern = TextPatternValidationUtils
                .validateTextPatternValue( tea.getTextPattern(), attribute.getValue() );

            addErrorIf( () -> !isReservedOrAlreadyAssigned && !isValidPattern, reporter, E1008, attribute.getValue(),
                tea.getTextPattern() );
        }
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

        FileResource fileResource = reporter.getValidationContext().getBundle().getPreheat()
            .get( TrackerIdScheme.UID, FileResource.class, attr.getValue() );
        
        addErrorIfNull( fileResource, reporter, E1084, attr.getValue() );
        addErrorIf( () -> fileResource != null && fileResource.isAssigned(), reporter, E1009, attr.getValue() );
    }
}
