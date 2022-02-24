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
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1009;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1076;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1084;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1303;
import static org.hisp.dhis.tracker.validation.hooks.ValidationUtils.needsToValidateDataValues;
import static org.hisp.dhis.tracker.validation.hooks.ValidationUtils.validateMandatoryDataValue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Enrico Colasante
 */
@Component
public class EventDataValuesValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );

        checkNotNull( programStage, TrackerImporterAssertErrors.PROGRAM_STAGE_CANT_BE_NULL );

        for ( DataValue dataValue : event.getDataValues() )
        {
            // event dates (createdAt, updatedAt) are ignored and set by the
            // system
            DataElement dataElement = context.getDataElement( dataValue.getDataElement() );

            if ( dataElement == null )
            {
                reporter.addError( event, TrackerErrorCode.E1304, dataValue.getDataElement() );
                continue;
            }

            validateDataElement( reporter, dataElement, dataValue, programStage, event );
            if ( dataValue.getValue() != null )
            {
                validateOptionSet( reporter, event, dataElement, dataValue.getValue() );
            }
        }

        validateMandatoryDataValues( event, context, reporter );
        validateDataValueDataElementIsConnectedToProgramStage( reporter, event, programStage );
    }

    private void validateMandatoryDataValues( Event event, TrackerImportValidationContext context,
        ValidationErrorReporter reporter )
    {
        if ( StringUtils.isNotEmpty( event.getProgramStage() ) )
        {
            ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
            final List<String> mandatoryDataElements = programStage.getProgramStageDataElements()
                .stream()
                .filter( ProgramStageDataElement::isCompulsory )
                .map( de -> context.getIdentifiers().getDataElementIdScheme()
                    .getIdentifier( de.getDataElement() ) )
                .collect( Collectors.toList() );
            List<String> missingDataValue = validateMandatoryDataValue( programStage, event,
                mandatoryDataElements );
            missingDataValue
                .forEach( de -> reporter.addError( event, E1303, de ) );
        }
    }

    private void validateDataElement( ValidationErrorReporter reporter, DataElement dataElement,
        DataValue dataValue, ProgramStage programStage, Event event )
    {
        final String status = ValidationUtils.dataValueIsValid( dataValue.getValue(), dataElement );

        if ( status != null )
        {
            reporter.addError( event, TrackerErrorCode.E1302, dataElement.getUid(),
                status );
        }
        else
        {
            validateNullDataValues( reporter, dataElement, programStage, dataValue, event );
            validateFileNotAlreadyAssigned( reporter, event, dataValue, dataElement );
        }
    }

    private void validateNullDataValues( ValidationErrorReporter reporter, DataElement dataElement,
        ProgramStage programStage, DataValue dataValue, Event event )
    {
        if ( dataValue.getValue() != null || !needsToValidateDataValues( event, programStage ) )
        {
            return;
        }

        Optional<ProgramStageDataElement> optionalPsde = Optional.of( programStage )
            .map( ps -> ps.getProgramStageDataElements().stream() ).flatMap( psdes -> psdes
                .filter(
                    psde -> psde.getDataElement().getUid().equals( dataElement.getUid() ) && psde.isCompulsory() )
                .findFirst() );

        if ( optionalPsde.isPresent() )
        {
            reporter.addError( event, E1076, DataElement.class.getSimpleName(),
                dataElement.getUid() );
        }
    }

    private void validateDataValueDataElementIsConnectedToProgramStage( ValidationErrorReporter reporter, Event event,
        ProgramStage programStage )
    {
        final Set<String> dataElements = programStage.getProgramStageDataElements()
            .stream()
            .map( de -> reporter.getValidationContext().getIdentifiers().getDataElementIdScheme()
                .getIdentifier( de.getDataElement() ) )
            .collect( Collectors.toSet() );

        Set<String> payloadDataElements = event.getDataValues().stream()
            .map( DataValue::getDataElement )
            .collect( Collectors.toSet() );

        for ( String payloadDataElement : payloadDataElements )
        {
            if ( !dataElements.contains( payloadDataElement ) )
            {
                reporter.addError( event, TrackerErrorCode.E1305, payloadDataElement,
                    programStage.getUid() );
            }
        }
    }

    private void validateFileNotAlreadyAssigned( ValidationErrorReporter reporter, Event event, DataValue dataValue,
        DataElement dataElement )
    {
        if ( dataValue == null || dataValue.getValue() == null )
        {
            return;
        }

        boolean isFile = dataElement.getValueType() != null && dataElement.getValueType().isFile();
        if ( !isFile )
        {
            return;
        }

        FileResource fileResource = reporter.getValidationContext().getFileResource( dataValue.getValue() );

        reporter.addErrorIfNull( fileResource, event, E1084, dataValue.getValue() );
        reporter.addErrorIf( () -> fileResource != null && fileResource.isAssigned(), event,
            E1009, dataValue.getValue() );
    }
}
