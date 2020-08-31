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
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class TrackedEntityAttributeValidationHook
    extends AbstractTrackerDtoValidationHook
{
    private static final int MAX_ATTR_VALUE_LENGTH = 1200;

    public TrackedEntityAttributeValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( TrackedEntity.class, TrackerImportStrategy.CREATE_AND_UPDATE, teAttrService  );
    }

    @Autowired
    private ReservedValueService reservedValueService;

    @Autowired
    private DhisConfigurationProvider dhisConfigurationProvider;

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
                reporter.addError( newReport( TrackerErrorCode.E1006 )
                    .addArg( attribute.getAttribute() ) );
                continue;
            }

//            if ( StringUtils.isEmpty( attribute.getValue() ) )
            if ( attribute.getValue() == null )
            {
                //continue; ??? Just continue on empty and null?
                // TODO: Is this really correct? This check was not here originally
                //  Enrollment attr check fails on null so why not here too?
                reporter.addError( newReport( TrackerErrorCode.E1076 )
                    .addArg( attribute ) );
                continue;
            }

            // TODO: Should we really validate existing data? this sounds like a mix of con
//            TrackedEntityAttributeValue trackedEntityAttributeValue = valueMap.get( tea.getUid() );
//            if ( trackedEntityAttributeValue == null )
//            {
            TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
            trackedEntityAttributeValue.setEntityInstance( tei );
            trackedEntityAttributeValue.setValue( attribute.getValue() );
            trackedEntityAttributeValue.setAttribute( tea );
//            }

            validateAttributeValue( reporter, trackedEntityAttributeValue );
            validateTextPattern( reporter, attribute, tea, valueMap.get( tea.getUid() ) );
            validateAttrValueType( reporter, attribute, tea );

            // TODO: This is one "THE" potential performance killer...
            validateAttributeUniqueness( reporter, attribute.getValue(), tea, tei, orgUnit );

            validateFileNotAlreadyAssigned( reporter, attribute, valueMap );
        }
    }

    public void validateAttributeValue( ValidationErrorReporter reporter, TrackedEntityAttributeValue teav )
    {
        checkNotNull( teav, TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );
        checkNotNull( teav.getValue(), TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL );

        // Validate value (string) don't exceed the max length
        if ( teav.getValue().length() > MAX_ATTR_VALUE_LENGTH )
        {
            reporter.addError( newReport( TrackerErrorCode.E1077 )
                .addArg( teav )
                .addArg( MAX_ATTR_VALUE_LENGTH ) );
        }

        // Validate if that encryption is configured properly if someone sets value to (confidential)
        boolean isConfidential = teav.getAttribute().isConfidentialBool();
        boolean encryptionStatusOk = dhisConfigurationProvider.getEncryptionStatus().isOk();
        if ( isConfidential && !encryptionStatusOk )
        {
            reporter.addError( newReport( TrackerErrorCode.E1112 )
                .addArg( teav ) );
        }

        // Uses ValidationUtils to check that the data value corresponds to the data value type set on the attribute
        String result = dataValueIsValid( teav.getValue(), teav.getAttribute().getValueType() );
        if ( result != null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1085 )
                .addArg( teav.getAttribute() )
                .addArg( result ) );
        }
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

            if ( !isReservedOrAlreadyAssigned && !isValidPattern )
            {
                reporter.addError( newReport( TrackerErrorCode.E1008 )
                    .addArg( attribute.getValue() )
                    .addArg( tea.getTextPattern() ) );
            }
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

        if ( fileResource == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1084 )
                .addArg( attr.getValue() ) );
        }

        if ( fileResource != null && fileResource.isAssigned() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1009 )
                .addArg( attr.getValue() ) );
        }
    }
}
